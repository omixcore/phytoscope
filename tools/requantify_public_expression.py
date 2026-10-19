#!/usr/bin/env python3
"""Re-quantify public rice-bean RNA-seq reads against the exact RefSeq release.

Requires Salmon 2.8.0, the GCF_029865625.1 RNA FASTA/GFF3, and the six
MD5-verified paired FASTQ files listed below. No third-party Python packages.
"""
import argparse
import csv
import gzip
import hashlib
import json
import os
import subprocess
import urllib.request
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path


RUNS = [
    ("SRR16122599", "IC426787_5DPA_rep1", 1_992_605_096, "0001bb88323341d7e30e6eb08ace0834",
     2_263_549_691, "870c5890367c7c3bab55e9a34711b236"),
    ("SRR16122608", "IC426787_5DPA_rep2", 1_347_654_696, "fa6a9371a993af899502e3fbb50dbd4b",
     1_545_764_414, "b813a2c1baa1d82d5a650560fc1ab40f"),
    ("SRR16122607", "IC426787_5DPA_rep3", 2_042_811_388, "13da9b2813373eb1357587bbc58bcf3f",
     2_327_301_235, "15e29c932df5c3c29c53416b46e0aa28"),
]
ENA_RUN_DIRS = {"SRR16122599": "099", "SRR16122608": "008", "SRR16122607": "007"}


def digest(path, algorithm):
    h = hashlib.new(algorithm)
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(8 * 1024 * 1024), b""):
            h.update(block)
    return h.hexdigest()


def download_fastq(url, path, expected_size, expected_md5):
    """Resume via HTTP byte ranges; stream chunks to disk without holding a FASTQ in RAM."""
    if path.exists() and path.stat().st_size == expected_size:
        if digest(path, "md5") == expected_md5:
            return
        path.write_bytes(b"")
    path.parent.mkdir(parents=True, exist_ok=True)
    start = path.stat().st_size if path.exists() else 0
    remaining = expected_size - start
    if remaining < 0:
        path.write_bytes(b"")
        start = 0
        remaining = expected_size
    fd = os.open(path, os.O_CREAT | os.O_RDWR, 0o644)
    os.ftruncate(fd, expected_size)
    chunks = []
    for i in range(4):
        first = start + remaining * i // 4
        last = start + remaining * (i + 1) // 4 - 1
        if last >= first:
            chunks.append((first, last))

    def fetch(first, last):
        request = urllib.request.Request(url, headers={"Range": f"bytes={first}-{last}"})
        chunk = path.with_name(f".{path.name}.{first}.{last}.part")
        count = 0
        with urllib.request.urlopen(request, timeout=180) as response:
            if response.status != 206:
                raise IOError(f"ENA did not honor byte range {first}-{last} for {path.name}")
            expected_range = f"bytes {first}-{last}/"
            if not response.headers.get("Content-Range", "").startswith(expected_range):
                raise IOError(f"ENA returned an unexpected Content-Range for {path.name}: {response.headers.get('Content-Range')}")
            with chunk.open("wb") as target:
                while True:
                    block = response.read(8 * 1024 * 1024)
                    if not block:
                        break
                    target.write(block)
                    count += len(block)
        if count != last - first + 1:
            raise IOError(f"short ENA byte range for {path.name}: {count} bytes")
        return first, chunk

    try:
        with ThreadPoolExecutor(max_workers=4) as pool:
            futures = [pool.submit(fetch, first, last) for first, last in chunks]
            for future in as_completed(futures):
                offset, chunk = future.result()
                with chunk.open("rb") as source:
                    position = offset
                    while True:
                        block = source.read(8 * 1024 * 1024)
                        if not block:
                            break
                        written = os.pwrite(fd, block, position)
                        if written != len(block):
                            raise IOError(f"short write for {path.name}")
                        position += written
                chunk.unlink()
    finally:
        os.close(fd)
    if path.stat().st_size != expected_size or digest(path, "md5") != expected_md5:
        raise ValueError(f"ENA size/MD5 verification failed for {path}")


def transcript_gene_map(fasta, gff, output):
    transcripts = set()
    with fasta.open(encoding="ascii") as stream:
        for line in stream:
            if line.startswith(">"):
                transcripts.add(line[1:].split()[0])
    mapping = {}
    opener = gzip.open if gff.suffix == ".gz" else open
    with opener(gff, "rt", encoding="utf-8") as stream:
        for line in stream:
            if line.startswith("#"):
                continue
            fields = line.rstrip("\n").split("\t")
            if len(fields) != 9 or fields[2] in {
                "gene", "exon", "CDS", "start_codon", "stop_codon",
                "five_prime_UTR", "three_prime_UTR",
            }:
                continue
            attrs = {}
            for item in fields[8].split(";"):
                if "=" in item:
                    key, value = item.split("=", 1)
                    attrs[key] = value
            tx = attrs.get("transcript_id", attrs.get("ID", "").removeprefix("rna-"))
            parent = attrs.get("Parent")
            if tx in transcripts and parent:
                if tx in mapping and mapping[tx] != parent:
                    raise ValueError(f"ambiguous transcript-to-gene mapping for {tx}")
                mapping[tx] = parent
    if transcripts != mapping.keys():
        missing = sorted(transcripts - mapping.keys())
        raise ValueError(f"{len(missing)} FASTA transcript IDs lack an exact GFF3 parent: {missing[:5]}")
    with output.open("w", encoding="ascii", newline="") as stream:
        for tx in sorted(mapping):
            stream.write(f"{tx}\t{mapping[tx]}\n")
    return len(transcripts), len(set(mapping.values()))


def load_gene_tpm(path):
    result = {}
    with path.open(encoding="utf-8", newline="") as stream:
        reader = csv.DictReader(stream, delimiter="\t")
        if not {"Name", "TPM"}.issubset(reader.fieldnames or []):
            raise ValueError(f"unexpected Salmon gene quantification header in {path}")
        for row in reader:
            result[row["Name"]] = float(row["TPM"])
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--rna-fasta", type=Path, required=True,
                        help="GCF_029865625.1_Vumbellata_1.0_rna.fna")
    parser.add_argument("--gff", type=Path, required=True,
                        help="matching GCF_029865625.1 genomic GFF3, plain or gzip")
    parser.add_argument("--reads-dir", type=Path, required=True,
                        help="directory containing the six ENA FASTQ files")
    parser.add_argument("--salmon", type=Path, required=True, help="Salmon 2.8.0 executable")
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--threads", type=int, default=4)
    parser.add_argument("--fetch-reads", action="store_true",
                        help="download any missing ENA FASTQs and verify their size/MD5")
    parser.add_argument("--temporary-reads", action="store_true",
                        help="delete each verified read pair after it has been quantified")
    args = parser.parse_args()
    if args.threads < 1:
        parser.error("--threads must be positive")
    version = subprocess.check_output([args.salmon, "--version"], text=True).strip()
    if version != "salmon 2.8.0":
        raise SystemExit(f"expected salmon 2.8.0, found {version}")

    index = args.output / "index"
    quant_root = args.output / "quant"
    args.output.mkdir(parents=True, exist_ok=True)
    quant_root.mkdir(exist_ok=True)
    gene_map = args.output / "transcript-to-gene.tsv"
    transcript_count, gene_count = transcript_gene_map(args.rna_fasta, args.gff, gene_map)
    subprocess.run([str(args.salmon), "index", "-t", str(args.rna_fasta), "-i", str(index),
                    "-k", "31", "-p", str(args.threads)], check=True)

    source_records = []
    args.reads_dir.mkdir(parents=True, exist_ok=True)
    for run, label, size1, md51, size2, md52 in RUNS:
        reads = [args.reads_dir / f"{run}_1.fastq.gz", args.reads_dir / f"{run}_2.fastq.gz"]
        expected = ((size1, md51), (size2, md52))
        destination = quant_root / run
        quant_genes = destination / "quant.genes.sf"
        if not quant_genes.exists():
            def download_missing(item):
                mate, path = item
                url = f"https://ftp.sra.ebi.ac.uk/vol1/fastq/SRR161/{ENA_RUN_DIRS[run]}/{run}/{run}_{mate}.fastq.gz"
                if args.fetch_reads:
                    download_fastq(url, path, expected[mate - 1][0], expected[mate - 1][1])
                elif not path.exists():
                    raise FileNotFoundError(path)

            with ThreadPoolExecutor(max_workers=2) as pool:
                list(pool.map(download_missing, enumerate(reads, start=1)))
            for path, (expected_size, expected_md5) in zip(reads, expected):
                if path.stat().st_size != expected_size or digest(path, "md5") != expected_md5:
                    raise ValueError(f"ENA size/MD5 verification failed for {path}")
            subprocess.run([
                str(args.salmon), "quant", "-i", str(index), "-l", "A",
                "-1", str(reads[0]), "-2", str(reads[1]),
                "--geneMap", str(gene_map), "-p", str(args.threads), "-o", str(destination),
            ], check=True)
            if args.temporary_reads:
                for path in reads:
                    path.unlink()
        library_report = json.loads((destination / "lib_format_counts.json").read_text(encoding="utf-8"))
        source_records.append({
            "run_accession": run, "sample_label": label,
            "fastq_urls": [
                f"https://ftp.sra.ebi.ac.uk/vol1/fastq/SRR161/{ENA_RUN_DIRS[run]}/{run}/{run}_1.fastq.gz",
                f"https://ftp.sra.ebi.ac.uk/vol1/fastq/SRR161/{ENA_RUN_DIRS[run]}/{run}/{run}_2.fastq.gz",
            ],
            "fastq_1": {"bytes": size1, "md5": md51},
            "fastq_2": {"bytes": size2, "md5": md52},
            "salmon_expected_library_format": library_report["expected_format"],
            "compatible_fragment_ratio": library_report["compatible_fragment_ratio"],
            "assigned_fragments": library_report["num_assigned_fragments"],
            "quant_genes_sha256": digest(destination / "quant.genes.sf", "sha256"),
        })

    sample_values = [load_gene_tpm(quant_root / row[0] / "quant.genes.sf") for row in RUNS]
    ids = sorted(set().union(*(values.keys() for values in sample_values)))
    matrix = args.output / "public-expression.tsv"
    with matrix.open("w", encoding="utf-8", newline="") as stream:
        stream.write("#unit: TPM\n#level: gene\n")
        writer = csv.writer(stream, delimiter="\t", lineterminator="\n")
        writer.writerow(["GeneID", *[row[1] for row in RUNS]])
        for gene in ids:
            writer.writerow([gene, *[format(values.get(gene, 0.0), ".10g") for values in sample_values]])
    manifest = {
        "study": "PRJNA765494", "species": "Vigna umbellata",
        "study_url": "https://www.ncbi.nlm.nih.gov/bioproject/765494/",
        "source_genotype": "IC426787", "developmental_stage": "5 days post anthesis",
        "assembly_accession": "GCF_029865625.1", "assembly_name": "Vumbellata_1.0",
        "cultivar": "VRB3", "annotation_release": "GCF_029865625.1-RS_2026_09",
        "annotation_url": "https://www.ncbi.nlm.nih.gov/refseq/annotation_euk/Vigna_umbellata/GCF_029865625.1-RS_2026_09/",
        "reference_inputs": {
            "rna_fasta_archive": {
                "file": "GCF_029865625.1_Vumbellata_1.0_rna.fna.gz",
                "url": "https://ftp.ncbi.nlm.nih.gov/genomes/all/GCF/029/865/625/GCF_029865625.1_Vumbellata_1.0/GCF_029865625.1_Vumbellata_1.0_rna.fna.gz",
                "ncbi_md5": "b39ade0ed73ce453276f0a5c41a7cd9a",
            },
            "gff3_archive": {
                "file": "GCF_029865625.1_genomic.gff.gz",
                "url": "https://ftp.ncbi.nlm.nih.gov/genomes/all/GCF/029/865/625/GCF_029865625.1_Vumbellata_1.0/GCF_029865625.1_genomic.gff.gz",
                "ncbi_md5": "f051f69254aabf92d935d06e086f74a0",
            },
        },
        "quantifier": version, "library_type": "Salmon auto-detected paired-end",
        "mapping_mode": "selective alignment (Salmon 2.8.0 default)",
        "gene_tpm_method": "Salmon --geneMap aggregates transcript estimates by exact RefSeq GFF3 parent gene ID; not length-corrected by tximport and not used for differential-expression inference.",
        "index_kmer_length": 31,
        "transcript_fasta_sha256": digest(args.rna_fasta, "sha256"),
        "gff3_sha256": digest(args.gff, "sha256"),
        "transcript_count": transcript_count, "gene_map_count": gene_count,
        "samples": source_records,
        "matrix": {"file": matrix.name, "genes": len(ids), "sha256": digest(matrix, "sha256")},
        "limitation": "Source genotype IC426787 differs from reference cultivar VRB3; this validates data import and exact-ID reconciliation, not genotype-matched biology or differential expression.",
    }
    with (args.output / "manifest.json").open("w", encoding="utf-8") as stream:
        json.dump(manifest, stream, indent=2)
        stream.write("\n")
    print(json.dumps(manifest, indent=2))


if __name__ == "__main__":
    main()
