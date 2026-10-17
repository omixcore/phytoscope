#!/usr/bin/env python3
"""Fetch and pin the official Ensembl Plants TAIR10 genomic DNA FASTA."""
import gzip
import hashlib
import json
import os
import shutil
import tempfile
import urllib.request
from pathlib import Path

URL = "https://ftp.ensemblgenomes.ebi.ac.uk/pub/plants/release-62/fasta/arabidopsis_thaliana/dna/Arabidopsis_thaliana.TAIR10.dna.toplevel.fa.gz"
DEST = Path("src/main/resources/data/species/arabidopsis/TAIR10_genomic.fna.gz")
MANIFEST = Path("src/main/resources/data/species/arabidopsis/genome-sequence-provenance.json")


def main():
    DEST.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.NamedTemporaryFile(delete=False) as temp:
        temp_path = Path(temp.name)
    try:
        request = urllib.request.Request(URL, headers={"User-Agent": "PhytoScope genome reference validator"})
        with urllib.request.urlopen(request, timeout=180) as response, temp_path.open("wb") as output:
            shutil.copyfileobj(response, output)
        digest = hashlib.sha256(temp_path.read_bytes()).hexdigest()
        record_ids = []
        record_lengths = {}
        current = None
        total = 0
        with gzip.open(temp_path, "rt", encoding="ascii") as source:
            for line in source:
                if line.startswith(">"):
                    current = line[1:].split()[0]
                    if current in record_lengths:
                        raise SystemExit(f"Duplicate FASTA record: {current}")
                    record_ids.append(current)
                    record_lengths[current] = 0
                elif current is not None:
                    bases = "".join(line.split()).upper()
                    if bases and any(base not in "ACGTNRYWSKMBDHV" for base in bases):
                        raise SystemExit(f"Invalid nucleotide in FASTA record {current}")
                    record_lengths[current] += len(bases)
                    total += len(bases)
        if not record_ids or total < 100_000_000:
            raise SystemExit(f"Unexpected genome FASTA size: {total} nt / {len(record_ids)} records")
        required = {"1", "2", "3", "4", "5"}
        found = set()
        for record_id in record_ids:
            if record_id in required:
                found.add(record_id)
        if found != required:
            raise SystemExit(f"TAIR10 primary chromosome records missing: {sorted(required - found)}")
        shutil.move(str(temp_path), DEST)
        manifest = {
            "species": "Arabidopsis thaliana",
            "assembly": "TAIR10",
            "source": "Ensembl Plants release 62",
            "url": URL,
            "filename": DEST.name,
            "sha256": digest,
            "compressed_bytes": DEST.stat().st_size,
            "sequence_records": len(record_ids),
            "total_nucleotides": total,
            "record_lengths": record_lengths,
            "primary_chromosomes": {key: record_lengths[key] for key in sorted(required)},
            "retrieval": "Downloaded from the version-pinned Ensembl Plants FTP source and gzip/FASTA validated; not modified.",
        }
        MANIFEST.write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
        print(json.dumps({k: manifest[k] for k in ["assembly", "url", "sha256", "compressed_bytes", "sequence_records", "total_nucleotides", "primary_chromosomes"]}, indent=2))
    finally:
        temp_path.unlink(missing_ok=True)


if __name__ == "__main__":
    main()
