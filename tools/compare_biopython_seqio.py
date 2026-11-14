#!/usr/bin/env python3
"""Independent Biopython SeqIO sequence-reconstruction comparison against the
frozen PhytoScope non-model sequence gold.

Purpose
-------
This script performs the *scripted* half of the Phase-3 (FCS) comparison
experiment: it uses only Biopython (SeqIO + Seq) plus raw GFF3 feature rows to
reconstruct, for a fixed set of non-model plant loci, the same strand-aware
sequence products that PhytoScope exports (genomic span, spliced transcript,
CDS, translated protein and inferred 5'/3' UTRs). Every product is compared
byte-for-byte against the frozen gold FASTA files under
``src/test/resources/non-model/<species>/gold-*.fasta``. Those gold files are
the Java-verified agreement point: the repository's Java tests assert that
PhytoScope's own exports match them, and ``tools/build_nonmodel_sequence_gold.py``
regenerates them directly from source GFF3 + FASTA slices without the PhytoScope
parser.

This is a *correctness + wall-clock timing* comparison of an independent,
widely used Python library against the frozen gold. It is NOT a GUI or
usability comparison and makes no speed-superiority claim.

Coordinate convention (matches build_nonmodel_sequence_gold.py)
----------------------------------------------------------------
GFF3 is 1-based inclusive. A feature with start ``s`` and end ``e`` is sliced as
``genome[s-1:e]``. On the minus strand, the concatenated forward slices are
reverse-complemented. Protein translation uses the standard nuclear code
(table 1) and drops exactly one terminal stop codon, mirroring
``GeneSequenceService`` / ``build_nonmodel_sequence_gold.py``.
"""
import argparse
import gzip
import hashlib
import json
import platform
import statistics
import sys
import time
from pathlib import Path

from Bio import SeqIO
from Bio.Seq import Seq

# species -> (annotation_gff3, scaffold_fasta, target_locus, data_root)
# The primary data root holds the vigna archive; the additional root holds the
# two medicinal/ornamental cases. Defaults point at the already-fetched local
# copies in target/ so no network access is required at run time.
CASES = {
    "vigna-umbellata": {
        "gff3": "GCF_029865625.1_genomic.gff.gz",
        "fasta": "NC_142174.1.fna",
        "locus": "LOC124833352",
    },
    "andrographis-paniculata": {
        "gff3": "GCF_009805555.1_ASM980555v1_genomic.gff.gz",
        "fasta": "NW_026137634.1.fna",
        "locus": "LOC127243311",
    },
    "hibiscus-syriacus": {
        "gff3": "GCF_006381635.1_ASM638163v2_genomic.gff.gz",
        "fasta": "NW_024062564.1.fna",
        "locus": "LOC120212642",
    },
}

# Sequence-product keys expected in every gold FASTA, mapped to the label used
# in the Biopython reconstruction report.
GOLD_KEYS = (
    "genomic_span",
    "transcript",
    "CDS",
    "protein",
    "five_prime_UTR_inferred",
    "three_prime_UTR_inferred",
)


def parse_attrs(column9):
    """Return an attribute dict from a GFF3 column-9 string (ID/Parent only needed)."""
    result = {}
    for token in column9.split(";"):
        if "=" in token:
            key, value = token.split("=", 1)
            result[key] = value
    return result


def read_gff3_records(path, sequence_id):
    """Yield feature dicts for one sequence ID from a (possibly gzipped) GFF3."""
    opener = gzip.open if str(path).endswith(".gz") else open
    with opener(path, "rt", encoding="utf-8") as stream:
        for line in stream:
            if line.startswith("##FASTA"):
                break
            if not line.strip() or line.startswith("#"):
                continue
            fields = line.rstrip("\n").split("\t")
            if len(fields) != 9:
                raise ValueError(f"Malformed GFF3 row in {path}: {fields!r}")
            if fields[0] != sequence_id:
                continue
            yield {
                "type": fields[2].lower(),
                "start": int(fields[3]),
                "end": int(fields[4]),
                "strand": fields[6],
                "attrs": parse_attrs(fields[8]),
            }


def select_target_transcript(records, locus):
    """Resolve the target coding transcript and its exon/CDS children for a locus.

    Returns ``(gene_id, transcript_id, strand, exons, cds)`` where ``exons`` and
    ``cds`` are lists of ``{"start", "end"}`` sorted by start. This mirrors the
    gold builder's selection: find the gene ``gene-<locus>``, then its child mRNA
    whose CDS children exist.
    """
    genes = [r for r in records if r["type"] == "gene"]
    tx_parent = {}
    tx_rows = {}
    children_by_tx = {}
    for r in records:
        identifier = r["attrs"].get("ID")
        if r["type"] in {"mrna", "transcript"}:
            tx_parent[identifier] = r["attrs"].get("Parent")
            tx_rows[identifier] = r
        elif identifier is None:
            continue
    for r in records:
        parents = r["attrs"].get("Parent", "")
        for parent in parents.split(",") if parents else []:
            children_by_tx.setdefault(parent, []).append(r)

    target_gene_id = f"gene-{locus}"
    if target_gene_id not in {g["attrs"].get("ID") for g in genes}:
        raise ValueError(f"Gene {target_gene_id} not found in annotation")

    transcript_ids = [
        tid for tid, parent in tx_parent.items() if parent == target_gene_id
    ]
    chosen = None
    for tid in transcript_ids:
        children = children_by_tx.get(tid, [])
        exons = sorted((c for c in children if c["type"] == "exon"),
                       key=lambda c: c["start"])
        cds = sorted((c for c in children if c["type"] == "cds"),
                     key=lambda c: c["start"])
        if exons and cds:
            chosen = (tid, exons, cds)
            break
    if chosen is None:
        raise ValueError(f"No coding transcript with exons+CDS under {target_gene_id}")

    tid, exons, cds = chosen
    gene_strand = next(
        g["strand"] for g in genes if g["attrs"].get("ID") == target_gene_id
    )
    return target_gene_id, tid, gene_strand, exons, cds


def infer_utr_segments(exons, cds, strand, five_prime):
    """Mirror GeneSequenceService: terminal exonic bases outside the CDS."""
    min_cds = min(f["start"] for f in cds)
    max_cds = max(f["end"] for f in cds)
    segments = []
    for exon in exons:
        high_side = (strand == "-") == five_prime
        if high_side:
            start = max(exon["start"], max_cds + 1)
            end = exon["end"]
        else:
            start = exon["start"]
            end = min(exon["end"], min_cds - 1)
        if end >= start:
            segments.append({"start": start, "end": end})
    return segments


def join_slices(genome, features, strand):
    """Concatenate forward genome slices; reverse-complement on the minus strand."""
    text = "".join(genome[f["start"] - 1:f["end"]] for f in features)
    return str(Seq(text).reverse_complement()) if strand == "-" else text


def translate_cds(cds_text):
    """Translate with the standard nuclear code, dropping one terminal stop."""
    protein = str(Seq(cds_text).translate(table=1, to_stop=False))
    if protein.endswith("*"):
        protein = protein[:-1]
    return protein


def read_gold(gold_path):
    """Read a gold FASTA into ``{header: uppercase_sequence}``."""
    gold = {}
    for record in SeqIO.parse(gold_path, "fasta"):
        gold[record.id] = str(record.seq).upper()
    return gold


def reconstruct_once(genome, span_start, span_end, strand, exons, cds):
    """Reconstruct all gold products; each returned tuple is (product, wall_ms).

    ``genome`` is the full reference sequence; ``exons``/``cds`` use absolute
    (1-based) coordinates, so genomic span is sliced separately here.
    """
    products = {}
    timings = {}

    start = time.perf_counter()
    span = genome[span_start - 1:span_end]
    products["genomic_span"] = (
        str(Seq(span).reverse_complement()) if strand == "-" else span
    )
    timings["genomic_span_ms"] = (time.perf_counter() - start) * 1000

    start = time.perf_counter()
    products["transcript"] = join_slices(genome, exons, strand)
    timings["transcript_ms"] = (time.perf_counter() - start) * 1000

    start = time.perf_counter()
    products["CDS"] = join_slices(genome, cds, strand)
    timings["cds_ms"] = (time.perf_counter() - start) * 1000

    start = time.perf_counter()
    products["protein"] = translate_cds(products["CDS"])
    timings["translate_ms"] = (time.perf_counter() - start) * 1000

    start = time.perf_counter()
    five = join_slices(genome, infer_utr_segments(exons, cds, strand, True), strand)
    three = join_slices(genome, infer_utr_segments(exons, cds, strand, False), strand)
    products["five_prime_UTR_inferred"] = five
    products["three_prime_UTR_inferred"] = three
    timings["utr_ms"] = (time.perf_counter() - start) * 1000

    return products, timings


def compare_case(case, gff3_path, fasta_path, gold_path, trials):
    """Run one species case: load FASTA, reconstruct, compare against gold."""
    records = list(read_gff3_records(gff3_path, fasta_path.stem))
    gene_id, tx_id, strand, exons, cds = select_target_transcript(
        records, case["locus"]
    )
    gold = read_gold(gold_path)

    # The target gene span determines the reverse-complemented genomic span; the
    # gold 'genomic_span' is the whole-gene span for the selected transcript.
    gene = next(r for r in records if r["type"] == "gene" and r["attrs"].get("ID") == gene_id)
    span_start, span_end = gene["start"], gene["end"]

    trial_results = []
    load_ms = []
    for _ in range(trials):
        start = time.perf_counter()
        record = SeqIO.read(fasta_path, "fasta")
        genome = str(record.seq).upper()
        load_ms.append((time.perf_counter() - start) * 1000)

        products, timings = reconstruct_once(
            genome, span_start, span_end, strand, exons, cds
        )
        trial_results.append((products, timings))

    median_timings = {
        key: statistics.median([t[key] for _, t in trial_results])
        for key in trial_results[0][1]
    }
    median_timings["fasta_load_ms"] = statistics.median(load_ms)

    comparisons = {}
    all_match = True
    for key in GOLD_KEYS:
        expected = gold.get(key)
        actual = trial_results[0][0][key]
        if expected is None:
            comparisons[key] = {"status": "SKIPPED", "reason": "not in gold FASTA"}
            continue
        matches = expected == actual
        if matches:
            comparisons[key] = {"status": "MATCH", "length": len(actual)}
        else:
            all_match = False
            # Locate the first divergence for diagnostics.
            first_diff = next(
                (i for i, (a, b) in enumerate(zip(expected, actual)) if a != b),
                min(len(expected), len(actual)),
            )
            comparisons[key] = {
                "status": "MISMATCH",
                "expected_length": len(expected),
                "actual_length": len(actual),
                "first_diff_index": first_diff,
                "expected_prefix": expected[first_diff:first_diff + 20],
                "actual_prefix": actual[first_diff:first_diff + 20],
            }

    sha = hashlib.sha256(trial_results[0][0]["transcript"].encode("ascii")).hexdigest()
    return {
        "species": case["species"],
        "assembly": case["gff3"].split("_genomic")[0],
        "sequence": fasta_path.stem,
        "gene_id": gene_id,
        "transcript_id": tx_id,
        "strand": strand,
        "span": [span_start, span_end],
        "exon_count": len(exons),
        "cds_segments": len(cds),
        "cds_length": len(trial_results[0][0]["CDS"]),
        "protein_aa": len(trial_results[0][0]["protein"]),
        "all_gold_products_match": all_match,
        "products": comparisons,
        "transcript_sha256": sha,
        "timing_median_ms": median_timings,
        "trials": trials,
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--data-root", type=Path,
                        default=Path("target/non-model"),
                        help="root holding the vigna archive (default target/non-model)")
    parser.add_argument("--additional-root", type=Path,
                        default=Path("target/additional-nonmodel"),
                        help="root holding andrographis/hibiscus archives")
    parser.add_argument("--gold-root", type=Path,
                        default=Path("src/test/resources/non-model"))
    parser.add_argument("--out", type=Path,
                        default=Path("target/phase3-comparison/biopython"))
    parser.add_argument("--trials", type=int, default=3)
    parser.add_argument("--species", nargs="+",
                        choices=list(CASES) + ["all"],
                        default=["all"])
    args = parser.parse_args()
    if args.trials < 3:
        parser.error("--trials must be at least 3")

    species = list(CASES) if args.species == ["all"] else args.species
    args.out.mkdir(parents=True, exist_ok=True)
    cases = []

    for name in species:
        case = dict(CASES[name])
        case["species"] = name
        directory = (
            args.data_root / "vigna-umbellata"
            if name == "vigna-umbellata"
            else args.additional_root
        )
        gff3 = directory / case["gff3"]
        fasta = directory / case["fasta"]
        gold = args.gold_root / name / f"gold-{case['locus']}.fasta"
        for path, label in ((gff3, "GFF3"), (fasta, "FASTA"), (gold, "gold FASTA")):
            if not path.is_file():
                raise FileNotFoundError(f"{label} not found: {path}")
        result = compare_case(case, gff3, fasta, gold, args.trials)
        cases.append(result)
        (args.out / f"{name}.json").write_text(
            json.dumps(result, indent=2) + "\n", encoding="utf-8"
        )

    report = {
        "status": "PASS" if all(c["all_gold_products_match"] for c in cases) else "FAIL",
        "comparison": (
            "Biopython SeqIO strand-aware sequence reconstruction vs frozen "
            "PhytoScope non-model sequence gold"
        ),
        "python": platform.python_version(),
        "biopython_version": __import__("Bio").__version__,
        "platform": platform.platform(),
        "trials_per_case": args.trials,
        "cases": cases,
        "interpretation": (
            "Byte-for-byte agreement of genomic span, spliced transcript, CDS, "
            "translated protein and inferred UTRs against the Java-verified gold "
            "files. Timings are per-product wall-clock medians from Biopython-only "
            "in-process trials and are not a GUI or speed-superiority comparison."
        ),
    }
    (args.out / "summary.json").write_text(
        json.dumps(report, indent=2) + "\n", encoding="utf-8"
    )
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
