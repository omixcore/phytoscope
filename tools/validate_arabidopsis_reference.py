"""Validate complete bundled Arabidopsis TAIR10/Araport11 Chr1-Chr5 resources."""
import argparse, hashlib, json
from pathlib import Path

EXPECTED = {
    "Chr1": {"length": 30427671, "features": 207497, "gene_like_features": 9019},
    "Chr2": {"length": 19698289, "features": 125500, "gene_like_features": 5486},
    "Chr3": {"length": 23459830, "features": 154731, "gene_like_features": 6747},
    "Chr4": {"length": 18585056, "features": 121971, "gene_like_features": 5131},
    "Chr5": {"length": 26975502, "features": 178800, "gene_like_features": 7614},
}

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--directory", type=Path, required=True)
    args = ap.parse_args()
    manifest = json.loads((args.directory / "manifest.json").read_text())
    assert manifest["assembly"] == "TAIR10"
    assert manifest["annotation"] == "Araport11"
    assert manifest["scope"] == "complete nuclear Chr1-Chr5"
    total_features = total_genes = 0
    for c, expected in EXPECTED.items():
        length = expected["length"]
        p = args.directory / f"{c}.gff3"
        assert p.exists(), p
        raw = p.read_bytes()
        lines = raw.decode("utf-8").splitlines()
        assert lines[0] == "##gff-version 3"
        assert any(x == f"##sequence-region {c} 1 {length}" for x in lines)
        rows = [x for x in lines if x and not x.startswith("#")]
        assert rows
        assert all(len(x.split("\t")) == 9 for x in rows)
        assert all(x.split("\t", 1)[0] == c for x in rows)
        ids = set()
        parents = []
        genes = 0
        for row in rows:
            f = row.split("\t")
            attrs = f[8]
            for token in attrs.split(";"):
                if token.startswith("ID="):
                    ids.add(token[3:])
                elif token.startswith("Parent="):
                    parents.extend(token[7:].split(","))
            if f[2] in ("gene", "pseudogene"):
                genes += 1
        missing = [p for p in parents if p and p not in ids]
        assert not missing[:10], f"{c}: missing parents, first={missing[:10]}"
        assert genes == expected["gene_like_features"], (
            c, genes, expected["gene_like_features"]
        )
        assert len(rows) == expected["features"], (
            c, len(rows), expected["features"]
        )
        digest = hashlib.sha256(raw).hexdigest()
        assert manifest["chromosomes"][c]["sha256"] == digest
        assert manifest["chromosomes"][c]["length"] == length
        assert manifest["chromosomes"][c]["features"] == expected["features"]
        assert manifest["chromosomes"][c]["gene_like_features"] == expected["gene_like_features"]
        total_features += len(rows)
        total_genes += genes
    assert total_genes == 33997, total_genes
    assert total_features == 788499, total_features
    assert manifest["total_features"] == total_features
    assert manifest["total_gene_like_features"] == total_genes
    print(f"PASS: complete TAIR10/Araport11 Chr1-Chr5; {total_genes} gene-like features; {total_features} feature rows")

if __name__ == "__main__":
    main()
