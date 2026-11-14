#!/usr/bin/env python3
"""Build frozen multi-locus sequence hashes from public GFF3 and matching FASTA.

This deliberately uses only source feature coordinates and reference FASTA slices;
it does not call PhytoScope or its parser. Run after the public-data fetch scripts.

In addition to the genomic-span / transcript / CDS hashes, each coding transcript
row now freezes the translated protein (terminal stop codon excluded), the protein
length in amino acids, and the inferred 5'/3' UTR hashes. UTR inference mirrors
PhytoScope's GeneSequenceService: when an annotation has exons and CDS but no
explicit UTR rows, terminal exonic bases outside the CDS boundaries are labelled
inferred. Protein translation uses the standard nuclear genetic code.
"""
import argparse
import csv
import gzip
import hashlib
from pathlib import Path

CASES = [
    ("vigna-umbellata", "GCF_029865625.1_genomic.gff.gz", "NC_142174.1", "LOC124833352"),
    ("andrographis-paniculata", "GCF_009805555.1_ASM980555v1_genomic.gff.gz", "NW_026137634.1", "LOC127243311"),
    ("hibiscus-syriacus", "GCF_006381635.1_ASM638163v2_genomic.gff.gz", "NW_024062564.1", "LOC120212642"),
]
HEADER = ["species", "annotation", "sequence", "gene_id", "transcript_id", "start", "end",
          "strand", "exon_count", "cds_segments", "cds_length", "span_sha256",
          "transcript_sha256", "cds_sha256", "protein_sha256", "protein_aa",
          "five_prime_utr_sha256", "three_prime_utr_sha256"]

CODONS = (
    "TTT TTC TTA TTG TCT TCC TCA TCG TAT TAC TAA TAG TGT TGC TGA TGG "
    "CTT CTC CTA CTG CCT CCC CCA CCG CAT CAC CAA CAG CGT CGC CGA CGG "
    "ATT ATC ATA ATG ACT ACC ACA ACG AAT AAC AAA AAG AGT AGC AGA AGG "
    "GTT GTC GTA GTG GCT GCC GCA GCG GAT GAC GAA GAG GGT GGC GGA GGG")
AMINO_ACIDS = "FFLLSSSSYY**CC*WLLLLPPPPHHQQRRRRIIIMTTTTNNKKSSRRVVVVAAAADDEEGGGG"
CODON_TABLE = dict(zip(CODONS.split(), AMINO_ACIDS))


def attrs(text):
    return dict(part.split("=", 1) for part in text.split(";") if "=" in part)


def digest(text):
    return hashlib.sha256(text.encode("ascii")).hexdigest()


def rc(text):
    return text.translate(str.maketrans("ACGTN", "TGCAN"))[::-1]


def translate(cds):
    """Translate a CDS with the standard nuclear code; exclude a terminal stop."""
    protein = "".join(CODON_TABLE.get(cds[i:i + 3], "X") for i in range(0, len(cds) - 2, 3))
    if protein.endswith("*"):
        protein = protein[:-1]
    return protein


def read_records(annotation, sequence):
    records = []
    with gzip.open(annotation, "rt", encoding="utf-8") as source:
        for line in source:
            if line.startswith("#"):
                continue
            fields = line.rstrip("\n").split("\t")
            if len(fields) != 9 or fields[0] != sequence:
                continue
            records.append({"type": fields[2].lower(), "start": int(fields[3]),
                            "end": int(fields[4]), "strand": fields[6], "attrs": attrs(fields[8])})
    return records


def infer_utr_segments(exons, cds, strand, five_prime):
    """Mirror GeneSequenceService.inferTerminalUtr: exonic bases outside the CDS."""
    min_cds = min(feature["start"] for feature in cds)
    max_cds = max(feature["end"] for feature in cds)
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


def build(data_root, additional_root):
    rows = []
    for species, annotation_name, sequence, target in CASES:
        directory = data_root / "vigna-umbellata" if species == "vigna-umbellata" else additional_root
        annotation = directory / annotation_name
        records = read_records(annotation, sequence)
        fasta = directory / f"{sequence}.fna"
        genome = "".join(line.strip() for line in fasta.read_text(encoding="ascii").splitlines()
                         if not line.startswith(">")).upper()
        tx_parent, tx_rows = {}, {}
        genes = []
        for feature in records:
            identifier = feature["attrs"].get("ID")
            if feature["type"] == "gene":
                genes.append(feature)
            elif feature["type"] in {"mrna", "transcript"}:
                tx_parent[identifier] = feature["attrs"].get("Parent")
                tx_rows[identifier] = feature
        coding_tx = {feature["attrs"].get("Parent") for feature in records
                     if feature["type"] == "cds"}
        coding_genes = {tx_parent[tx] for tx in coding_tx if tx in tx_parent}
        candidates = [gene for gene in genes if gene["attrs"].get("ID") in coding_genes]
        target_gene = next(gene for gene in candidates
                           if gene["attrs"].get("ID") == f"gene-{target}")
        selected = [target_gene] + [gene for gene in candidates if gene is not target_gene][:4]
        for gene in selected:
            gene_id = gene["attrs"]["ID"]
            for tx_id, parent in tx_parent.items():
                if parent != gene_id:
                    continue
                children = [feature for feature in records
                            if tx_id in feature["attrs"].get("Parent", "").split(",")]
                exons = sorted((f for f in children if f["type"] == "exon"), key=lambda f: f["start"])
                cds = sorted((f for f in children if f["type"] == "cds"), key=lambda f: f["start"])
                if not exons or not cds:
                    continue
                strand = gene["strand"]
                span = genome[gene["start"] - 1:gene["end"]]
                if strand == "-":
                    span = rc(span)

                def join(features):
                    sequence_text = "".join(genome[f["start"] - 1:f["end"]] for f in features)
                    return rc(sequence_text) if strand == "-" else sequence_text

                transcript, coding = join(exons), join(cds)
                protein = translate(coding)
                five_prime = join(infer_utr_segments(exons, cds, strand, True))
                three_prime = join(infer_utr_segments(exons, cds, strand, False))
                rows.append([species, annotation_name, sequence, gene_id, tx_id,
                             gene["start"], gene["end"], strand, len(exons), len(cds), len(coding),
                             digest(span), digest(transcript), digest(coding),
                             digest(protein), len(protein),
                             digest(five_prime), digest(three_prime)])
    return rows


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--data-root", type=Path, default=Path("target/non-model"))
    parser.add_argument("--additional-root", type=Path, default=Path("target/additional-nonmodel"))
    parser.add_argument("--output", type=Path,
                        default=Path("src/test/resources/non-model/multi-locus-sequence-gold.tsv"))
    args = parser.parse_args()
    rows = build(args.data_root, args.additional_root)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with args.output.open("w", encoding="utf-8", newline="") as output:
        writer = csv.writer(output, delimiter="\t", lineterminator="\n")
        writer.writerow(HEADER)
        writer.writerows(rows)
    print(f"Wrote {len(rows)} source-derived transcript sequence records to {args.output}")


if __name__ == "__main__":
    main()
