"""Materialize the complete Arabidopsis TAIR10/Araport11 nuclear annotation.

The source is the archived Araport11 GFF3 annotation mapped to the TAIR10
assembly. The file is streamed and split into five chromosome resources so
each Git object stays comfortably below GitHub's 100 MB single-file limit.
No gene-family sampling or feature filtering is performed within Chr1-Chr5.
"""
import argparse, gzip, hashlib, json, subprocess
from pathlib import Path

OFFICIAL = "https://www.arabidopsis.org/download_files/Genes/Araport11_genome_release/Araport11_GFF3_genes_transposons.201606.gff.gz"
MIRROR = "https://plantgarden.jp/en/download/t3702/t3702.G001/Araport11_GFF3_genes_transposons.201606.gff.gz"
CHRS = {f"Chr{i}": i for i in range(1, 6)}
EXPECTED_ARCHIVE_SHA256 = "22d8c42ec2ca667fc3f78105f568bf295ee10d2145b2bd76dcd290a469a27b99"
LENGTHS = {
    "Chr1": 30427671, "Chr2": 19698289, "Chr3": 23459830,
    "Chr4": 18585056, "Chr5": 26975502,
}

def download(archive):
    if archive.exists():
        return "local"
    for url in (OFFICIAL, MIRROR):
        try:
            subprocess.run(["curl", "-fL", "--retry", "3", "--connect-timeout", "30",
                            "-o", str(archive), url], check=True)
            return url
        except subprocess.CalledProcessError:
            archive.unlink(missing_ok=True)
    raise RuntimeError("Unable to download the pinned Araport11 annotation from the official source or mirror")

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--archive", type=Path, required=True)
    ap.add_argument("--output", type=Path, required=True)
    args = ap.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    source = download(args.archive)
    archive_sha256 = hashlib.sha256(args.archive.read_bytes()).hexdigest()
    if archive_sha256 != EXPECTED_ARCHIVE_SHA256:
        raise RuntimeError(
            f"Pinned Araport11 archive checksum mismatch: expected {EXPECTED_ARCHIVE_SHA256}, "
            f"observed {archive_sha256}"
        )

    handles = {}
    feature_counts = {c: 0 for c in CHRS}
    gene_counts = {c: 0 for c in CHRS}
    sequence_lengths = {}
    try:
        for c, length in LENGTHS.items():
            h = (args.output / f"{c}.gff3").open("w", encoding="utf-8", newline="\n")
            h.write("##gff-version 3\n")
            h.write("##species=Arabidopsis thaliana\n")
            h.write("##assembly=TAIR10 / Araport11\n")
            h.write(f"##sequence-region {c} 1 {length}\n")
            handles[c] = h

        with gzip.open(args.archive, "rt", encoding="utf-8", errors="strict") as fh:
            for line in fh:
                if line.startswith("##sequence-region"):
                    p = line.rstrip("\n").split()
                    if len(p) >= 4:
                        raw = p[1]
                        c = raw if raw.startswith("Chr") else f"Chr{raw}" if raw in {"1","2","3","4","5"} else None
                        if c in CHRS:
                            sequence_lengths[c] = int(p[3])
                    continue
                if line.startswith("#"):
                    continue
                cols = line.rstrip("\n").split("\t", 9)
                if len(cols) != 9:
                    continue
                raw = cols[0]
                c = raw if raw.startswith("Chr") else f"Chr{raw}" if raw in {"1","2","3","4","5"} else None
                if c not in CHRS:
                    continue
                cols[0] = c
                row = "\t".join(cols) + "\n"
                handles[c].write(row)
                feature_counts[c] += 1
                if cols[2] in ("gene", "pseudogene"):
                    gene_counts[c] += 1
    finally:
        for h in handles.values():
            h.close()

    manifest = {
        "source": OFFICIAL,
        "downloaded_from": source,
        "archive_sha256": archive_sha256,
        "source_provider": "The Arabidopsis Information Resource (TAIR)",
        "source_release": "Araport11 original release, June 2016",
        "assembly": "TAIR10",
        "annotation": "Araport11",
        "scope": "complete nuclear Chr1-Chr5",
        "chromosomes": {},
        "total_features": 0,
        "total_gene_like_features": 0,
    }
    for c in CHRS:
        text = (args.output / f"{c}.gff3").read_bytes()
        manifest["chromosomes"][c] = {
            "length": LENGTHS[c],
            "source_sequence_length": sequence_lengths.get(c, LENGTHS[c]),
            "features": feature_counts[c],
            "gene_like_features": gene_counts[c],
            "sha256": hashlib.sha256(text).hexdigest(),
        }
        manifest["total_features"] += feature_counts[c]
        manifest["total_gene_like_features"] += gene_counts[c]
        assert sequence_lengths.get(c, LENGTHS[c]) == LENGTHS[c], (c, sequence_lengths.get(c))

    (args.output / "manifest.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(manifest, indent=2))

if __name__ == "__main__":
    main()
