#!/usr/bin/env python3
"""Build frozen multi-locus sequence hashes from public GFF3 and matching FASTA.

This deliberately uses only source feature coordinates and reference FASTA slices;
it does not call PhytoScope or its parser. Run after the public-data fetch scripts.
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
          "transcript_sha256", "cds_sha256"]


def attrs(text):
    return dict(part.split("=", 1) for part in text.split(";") if "=" in part)


def digest(text):
    return hashlib.sha256(text.encode("ascii")).hexdigest()


def rc(text):
    return text.translate(str.maketrans("ACGTN", "TGCAN"))[::-1]


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


def build(data_root):
    rows = []
    for species, annotation_name, sequence, target in CASES:
        directory = data_root / ("vigna-umbellata" if species == "vigna-umbellata" else "additional")
        annotation = directory / annotation_name
        records = read_records(annotation, sequence)
        fasta = directory / f"{sequence}.fna"
        genome = "".join(line.strip() for line in fasta.read_text(encoding="ascii").splitlines()
                         if not line.startswith(">" )).upper()
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
                rows.append([species, annotation_name, sequence, gene_id, tx_id,
                             gene["start"], gene["end"], strand, len(exons), len(cds), len(coding),
                             digest(span), digest(transcript), digest(coding)])
    return rows


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--data-root", type=Path, default=Path("target/non-model"))
    parser.add_argument("--output", type=Path,
                        default=Path("src/test/resources/non-model/multi-locus-sequence-gold.tsv"))
    args = parser.parse_args()
    rows = build(args.data_root)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with args.output.open("w", encoding="utf-8", newline="") as output:
        writer = csv.writer(output, delimiter="\t", lineterminator="\n")
        writer.writerow(HEADER)
        writer.writerows(rows)
    print(f"Wrote {len(rows)} source-derived transcript sequence records to {args.output}")


if __name__ == "__main__":
    main()
