#!/usr/bin/env python3
"""Compare PhytoScope promoter sequences with bedtools getfasta -s."""
from __future__ import annotations

import argparse
import gzip
import hashlib
import json
import re
import shutil
import subprocess
import tempfile
import time
from pathlib import Path


def open_text(path: Path):
    return gzip.open(path, "rt", encoding="ascii") if path.suffix == ".gz" else path.open("rt", encoding="ascii")


def fasta_records(path: Path):
    header = None
    chunks: list[str] = []
    with open_text(path) as handle:
        for raw in handle:
            line = raw.strip()
            if not line:
                continue
            if line.startswith(">"):
                if header is not None:
                    yield header, "".join(chunks).upper()
                header = line[1:].strip()
                chunks = []
            else:
                if header is None:
                    raise ValueError("FASTA sequence appears before the first header")
                chunks.append(re.sub(r"\s+", "", line))
    if header is not None:
        yield header, "".join(chunks).upper()


def normalize_name(value: str) -> str:
    normalized = re.sub(r"[^a-z0-9]", "", value.lower())
    return normalized[3:] if normalized.startswith("chr") and len(normalized) > 3 else normalized


def sha256(sequence: str) -> str:
    return hashlib.sha256(sequence.encode("ascii")).hexdigest()


def parse_fasta(path: Path):
    records = []
    header = None
    chunks: list[str] = []
    with path.open("rt", encoding="ascii") as handle:
        for raw in handle:
            line = raw.strip()
            if not line:
                continue
            if line.startswith(">"):
                if header is not None:
                    records.append((header, "".join(chunks).upper()))
                header = line[1:].strip()
                chunks = []
            else:
                if header is None:
                    raise ValueError("Output FASTA sequence appears before a header")
                chunks.append(re.sub(r"\s+", "", line))
    if header is not None:
        records.append((header, "".join(chunks).upper()))
    return records


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--report", type=Path, required=True,
                        help="research-module-gold-set.json emitted by ResearchModuleGoldSetTest")
    parser.add_argument("--genome", type=Path, required=True,
                        help="Pinned TAIR10 genomic FASTA (gzip-compressed)")
    parser.add_argument("--out", type=Path, required=True)
    args = parser.parse_args()

    bedtools = shutil.which("bedtools")
    if not bedtools:
        raise SystemExit("bedtools is required; install the bedtools package before running this comparison")

    report = json.loads(args.report.read_text(encoding="utf-8"))
    rows = report["promoter_extraction"]["records"]
    if len(rows) != 8:
        raise SystemExit(f"Expected the pinned 8-locus promoter set, found {len(rows)}")
    if any("sequence" not in row or "chromosome" not in row for row in rows):
        raise SystemExit("The research-module report must include promoter sequences and chromosome IDs")

    target_name = normalize_name(rows[0]["chromosome"])
    if any(normalize_name(row["chromosome"]) != target_name for row in rows):
        raise SystemExit("This comparator currently expects a single chromosome per report")
    genome_sequence = None
    for header, sequence in fasta_records(args.genome):
        identifier = header.split()[0]
        if normalize_name(identifier) == target_name:
            genome_sequence = sequence
            break
    if genome_sequence is None:
        raise SystemExit(f"Could not resolve chromosome {rows[0]['chromosome']} in {args.genome}")

    for row in rows:
        if sha256(row["sequence"]) != row["sequence_sha256"]:
            raise SystemExit(f"PhytoScope sequence/hash mismatch for {row['gene_id']}")
        start = int(row["start_1based_closed"])
        end = int(row["end_1based_closed"])
        if start < 1 or end < start or end > len(genome_sequence):
            raise SystemExit(f"Invalid 1-based closed interval for {row['gene_id']}")
        if end - start + 1 != int(row["length_bp"]):
            raise SystemExit(f"Interval length mismatch for {row['gene_id']}")

    with tempfile.TemporaryDirectory(prefix="phytoscope-bedtools-") as temporary:
        temp = Path(temporary)
        reference = temp / "chr1.fa"
        bed = temp / "promoters.bed"
        output = temp / "bedtools.fa"
        with reference.open("wt", encoding="ascii") as handle:
            handle.write(">Chr1\n")
            for offset in range(0, len(genome_sequence), 60):
                handle.write(genome_sequence[offset:offset + 60] + "\n")
        with bed.open("wt", encoding="ascii") as handle:
            for row in rows:
                # Convert GFF-style 1-based closed [start, end] to BED 0-based
                # half-open [start-1, end); -s asks bedtools to orient by strand.
                start0 = int(row["start_1based_closed"]) - 1
                end0 = int(row["end_1based_closed"])
                handle.write(f"Chr1\t{start0}\t{end0}\t{row['gene_id']}\t0\t{row['strand']}\n")
        version = subprocess.run([bedtools, "--version"], check=True, capture_output=True, text=True).stdout.strip()
        started = time.perf_counter()
        subprocess.run([bedtools, "getfasta", "-fi", str(reference), "-bed", str(bed),
                        "-s", "-fo", str(output)], check=True, capture_output=True, text=True)
        elapsed = time.perf_counter() - started
        extracted = parse_fasta(output)

    if len(extracted) != len(rows):
        raise SystemExit(f"bedtools returned {len(extracted)} records for {len(rows)} intervals")

    comparisons = []
    mismatches = []
    for row, (header, sequence) in zip(rows, extracted, strict=True):
        matched = sequence == row["sequence"].upper()
        record = {
            "gene_id": row["gene_id"],
            "strand": row["strand"],
            "start_1based_closed": row["start_1based_closed"],
            "end_1based_closed": row["end_1based_closed"],
            "length_bp": len(sequence),
            "exact_sequence_match": matched,
            "phytoscope_sha256": row["sequence_sha256"],
            "bedtools_sha256": sha256(sequence),
            "bedtools_header": header,
        }
        comparisons.append(record)
        if not matched:
            mismatches.append(record)

    result = {
        "tool": "bedtools getfasta -s",
        "tool_version": version,
        "reference": "Arabidopsis thaliana TAIR10 chromosome 1",
        "phytoScope_report": str(args.report),
        "genome_source": str(args.genome),
        "coordinate_conversion": "BED start = 1-based start - 1; BED end = 1-based closed end",
        "interval_count": len(rows),
        "exact_sequence_matches": len(rows) - len(mismatches),
        "mismatch_count": len(mismatches),
        "elapsed_seconds_for_bedtools_getfasta": round(elapsed, 6),
        "scope_limit": "Eight selected loci on one chromosome; correctness comparison only, not a performance ranking.",
        "records": comparisons,
    }
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({k: result[k] for k in (
        "tool", "tool_version", "interval_count", "exact_sequence_matches",
        "mismatch_count", "elapsed_seconds_for_bedtools_getfasta")}, indent=2))
    if mismatches:
        print(json.dumps({"mismatches": mismatches}, indent=2))
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
