"""Build the Mentha longifolia nuclear-chromosome example from the WSU MGR GFF.

Source: Mint Genome Resources (Lange lab, WSU), Mlong CMEN585 v3.01b Maker
annotation, http://langelabtools.wsu.edu/mgr/ (also deposited as
GCA_001642375.2). The GFF names its twelve chromosome-scale sequences
`scaffold_<N>_length_<L>`; the embedded length is the true sequence length.
417 unplaced scaffolds are excluded.

Convention matches tools/build_chromosome_fixtures.py: the first two gene
families per chromosome (file order) plus their complete descendant closure,
coordinates untouched. Sequence names are mapped to ChrN and the original
scaffold names are preserved in header comments for provenance.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
from pathlib import Path

CHROM = re.compile(r"^scaffold_(\d+)_length_(\d+)$")
SOURCE_URL = ("http://langelabtools.wsu.edu/mgr/sites/default/files/"
              "downloads/Mlong585_v3.01b-maker.gff")


def attributes(text: str) -> dict:
    return dict(x.split("=", 1) for x in text.split(";") if "=" in x)


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("source", type=Path)
    ap.add_argument("--update-example", action="store_true")
    args = ap.parse_args()

    text = args.source.read_text(encoding="utf-8")
    lines = text.splitlines()

    lengths: dict[str, int] = {}
    genes: dict[str, list[str]] = {}
    rows: list[tuple[list[str], dict]] = []
    for line in lines:
        if line.startswith("#"):
            continue
        cols = line.split("\t")
        if len(cols) != 9:
            continue
        seq = cols[0]
        if seq not in lengths:
            m = CHROM.match(seq)
            # The twelve chromosome-scale sequences are scaffold_1..scaffold_12;
            # higher-numbered entries with the same name pattern are unplaced
            # scaffolds (the assembly has 417 of them) and are excluded.
            if not m or not 1 <= int(m.group(1)) <= 12:
                continue
            lengths[seq] = int(m.group(2))
        if cols[2] == "gene" and len(genes.setdefault(seq, [])) < 2:
            genes[seq].append(attributes(cols[8])["ID"])
        rows.append((cols, attributes(cols[8])))

    if not lengths:
        raise SystemExit("no chromosome-scale sequences matched")

    # Descendant closure of the selected families (parents may follow children).
    selected = {gid for ids in genes.values() for gid in ids}
    children: dict[str, list[str]] = {}
    for _, attrs in rows:
        for parent in attrs.get("Parent", "").split(","):
            if parent:
                children.setdefault(parent, []).append(attrs.get("ID"))
    pending = list(selected)
    while pending:
        for child in children.get(pending.pop(), []):
            if child and child not in selected:
                selected.add(child)
                pending.append(child)

    kept = []
    for cols, attrs in rows:
        if attrs.get("ID") in selected or any(p in selected for p in attrs.get("Parent", "").split(",")):
            cols = list(cols)
            m = CHROM.match(cols[0])
            cols[0] = f"Chr{int(m.group(1))}"
            kept.append("\t".join(cols))

    chroms = sorted(lengths, key=lambda s: int(CHROM.match(s).group(1)))
    name_map = {s: f"Chr{int(CHROM.match(s).group(1))}" for s in chroms}
    header = [
        "##gff-version 3",
        "##species=Mentha longifolia",
        "##assembly=Mlong_CMEN585_v3.01b",
        "# Source: " + SOURCE_URL,
        "# WSU Mint Genome Resources, Mentha longifolia CMEN585 v3.01b Maker annotation",
        "# (also deposited as INSDC GCA_001642375.2). Real annotation excerpt: the first",
        "# two gene families per chromosome-scale scaffold, unmodified coordinates.",
        "# Sequence-name mapping (417 unplaced scaffolds excluded):",
    ]
    header += [f"#   {s} -> {name_map[s]}" for s in chroms]
    header += [f"##sequence-region {name_map[s]} 1 {lengths[s]}" for s in chroms]
    data = "\n".join(header + kept) + "\n"

    stats = dict(
        chromosomes={name_map[s]: lengths[s] for s in chroms},
        families={name_map[s]: genes[s] for s in chroms},
        feature_rows=len(kept),
        source_sha256=hashlib.sha256(text.encode()).hexdigest(),
        fixture_sha256=hashlib.sha256(data.encode()).hexdigest(),
    )
    print(json.dumps({k: v for k, v in stats.items() if k != "families"}, indent=2))
    for s in chroms:
        print(f"  {name_map[s]:6s} {lengths[s]:>11,d} bp  {len(genes[s])} families  {genes[s][0][:50]}...")

    if args.update_example:
        out = Path(__file__).resolve().parents[1] / "src/main/resources/data/species/mint/mentha_longifolia_chromosomes.gff3"
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_text(data, encoding="utf-8", newline="\n")
        print("written:", out)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
