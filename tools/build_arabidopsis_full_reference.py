"""Build complete TAIR10/Araport11 Chr1-Chr5 resources from pinned Ensembl Plants 62.

Only chromosome feature rows are retained; no gene-family sampling occurs.
The generated files use the source chromosome IDs (1..5) rewritten to the
canonical UI names Chr1..Chr5. All other columns and attributes are preserved.
"""
import argparse, gzip, hashlib, json, urllib.request
from pathlib import Path

URL = "https://ftp.ensemblgenomes.ebi.ac.uk/pub/plants/release-62/gff3/arabidopsis_thaliana/Arabidopsis_thaliana.TAIR10.62.gff3.gz"
CHRS = [str(i) for i in range(1, 6)]

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--archive", type=Path, required=True)
    ap.add_argument("--output", type=Path, required=True)
    args = ap.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    if not args.archive.exists():
        urllib.request.urlretrieve(URL, args.archive)

    archive_sha256 = hashlib.sha256(args.archive.read_bytes()).hexdigest()
    regions, rows = {}, {c: [] for c in CHRS}
    with gzip.open(args.archive, "rt", encoding="utf-8") as fh:
        for line in fh:
            if line.startswith("##sequence-region"):
                p = line.rstrip().split()
                if len(p) >= 4 and p[1] in CHRS:
                    regions[p[1]] = int(p[3])
            elif not line.startswith("#"):
                p = line.rstrip("\n").split("\t")
                if len(p) == 9 and p[0] in CHRS:
                    rows[p[0]].append(line.rstrip("\n"))

    manifest = {"source": URL, "archive_sha256": archive_sha256,
                "assembly": "TAIR10", "chromosomes": {},
                "total_features": 0, "total_genes": 0}
    for c in CHRS:
        name = f"Chr{c}"
        converted = []
        for row in rows[c]:
            cols = row.split("\t", 1)
            converted.append(name + "\t" + cols[1])
        text = "\n".join([
            "##gff-version 3",
            "##species=Arabidopsis thaliana",
            "##assembly=TAIR10",
            f"##sequence-region {name} 1 {regions[c]}",
            *converted]) + "\n"
        out = args.output / f"{name}.gff3"
        out.write_text(text, encoding="utf-8")
        genes = sum(1 for r in converted if "\tgene\t" in r)
        manifest["chromosomes"][name] = {
            "length": regions[c], "features": len(converted), "genes": genes,
            "sha256": hashlib.sha256(text.encode()).hexdigest()
        }
        manifest["total_features"] += len(converted)
        manifest["total_genes"] += genes

    (args.output / "manifest.json").write_text(
        json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(manifest, indent=2))

if __name__ == "__main__":
    main()
