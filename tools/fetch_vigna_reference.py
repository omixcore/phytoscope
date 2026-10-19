#!/usr/bin/env python3
"""Fetch and checksum the public Vigna reference inputs used by Java validation."""

import argparse
import hashlib
import os
import tempfile
import urllib.request
from pathlib import Path


FILES = {
    "GCF_029865625.1_genomic.gff.gz": (
        "https://ftp.ncbi.nlm.nih.gov/genomes/all/GCF/029/865/625/GCF_029865625.1_Vumbellata_1.0/GCF_029865625.1_Vumbellata_1.0_genomic.gff.gz",
        "5c50d02f221c5e39d5193ed07904f2fa23e2f410e54a581f59db74929a57e46e",
    ),
    "GCF_029865625.1_genomic.gtf.gz": (
        "https://ftp.ncbi.nlm.nih.gov/genomes/all/GCF/029/865/625/GCF_029865625.1_Vumbellata_1.0/GCF_029865625.1_Vumbellata_1.0_genomic.gtf.gz",
        "2315735a3dabee035e173cc290f51748aa04f8351209895dee357706802a7414",
    ),
    "NC_142174.1.fna": (
        "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=nuccore&id=NC_142174.1&rettype=fasta&retmode=text",
        "c726f9ad50684d573622669b34ae6e48e12f41d737106d68070e6e2ac9c41805",
    ),
}


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--directory", type=Path, required=True)
    args = parser.parse_args()
    args.directory.mkdir(parents=True, exist_ok=True)
    for filename, (url, expected) in FILES.items():
        destination = args.directory / filename
        if destination.exists() and sha256(destination) == expected:
            print(f"VERIFIED cached {filename}")
            continue
        with tempfile.NamedTemporaryFile(dir=args.directory, delete=False) as temp:
            temporary = Path(temp.name)
            try:
                request = urllib.request.Request(url, headers={"User-Agent": "PhytoScope-validation/1.0"})
                with urllib.request.urlopen(request, timeout=120) as response:
                    while chunk := response.read(1024 * 1024):
                        temp.write(chunk)
            except Exception:
                temporary.unlink(missing_ok=True)
                raise
        try:
            actual = sha256(temporary)
            if actual != expected:
                raise RuntimeError(f"SHA-256 mismatch for {filename}: {actual} != {expected}")
            os.replace(temporary, destination)
        finally:
            temporary.unlink(missing_ok=True)
        print(f"VERIFIED downloaded {filename} sha256={expected}")


if __name__ == "__main__":
    main()
