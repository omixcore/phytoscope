"""Validate complete bundled Arabidopsis TAIR10/Araport11 Chr1-Chr5 resources."""
import argparse, hashlib, json
from pathlib import Path

EXPECTED = {
    "Chr1": 30427671, "Chr2": 19698289, "Chr3": 23459830,
    "Chr4": 18585056, "Chr5": 26975502,
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
    for c, length in EXPECTED.items():
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
        assert genes > 5000, (c, genes)
        assert len(rows) > 30000, (c, len(rows))
        digest = hashlib.sha256(raw).hexdigest()
        assert manifest["chromosomes"][c]["sha256"] == digest
        total_features += len(rows)
        total_genes += genes
    assert total_genes > 30000, total_genes
    assert total_features > 300000, total_features
    print(f"PASS: complete TAIR10/Araport11 Chr1-Chr5; {total_genes} gene-like features; {total_features} feature rows")

if __name__ == "__main__":
    main()
