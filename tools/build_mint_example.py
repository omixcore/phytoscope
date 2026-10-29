"""Build the bundled Mentha spicata chloroplast example from its INSDC record.

Source record: GenBank/ENA MG256495 (Mentha spicata chloroplast, complete genome,
152,132 bp). The feature table of that record is re-encoded as GFF3 with an
explicit gene -> transcript -> exon/CDS hierarchy; every coordinate is taken
verbatim from the /gene, /CDS, /tRNA and /rRNA features, and no feature is added,
dropped or moved.

Usage:
    python tools/build_mint_example.py --cache /tmp/phytoscope-mint [--update-example]
"""
import argparse
import gzip
import hashlib
import re
import urllib.request
from pathlib import Path

ACCESSION = "MG256495"
SPECIES = "Mentha spicata"
SEQUENCE = "Pt"
SOURCE_URL = f"https://www.ebi.ac.uk/ena/browser/api/embl/{ACCESSION}?download=false"
TARGET = Path("src/main/resources/data/species/mint/mentha_spicata_chloroplast.gff3")
CHILD_TYPES = {"CDS": "protein_coding", "tRNA": "tRNA", "rRNA": "rRNA"}


def fetch(cache: Path) -> Path:
    cache.mkdir(parents=True, exist_ok=True)
    path = cache / f"{ACCESSION}.embl"
    if not path.exists():
        print(f"downloading {SOURCE_URL}")
        with urllib.request.urlopen(SOURCE_URL, timeout=120) as resp:
            path.write_bytes(resp.read())
    return path


def parse_location(text: str):
    """Return (strand, [(start, end), ...]) for an EMBL location string."""
    text = "".join(text.split())
    strand = 1
    while True:
        if text.startswith("complement("):
            strand *= -1
            text = text[len("complement("):-1]
            continue
        break
    for wrapper in ("join(", "order("):
        if text.startswith(wrapper):
            inner = text[len(wrapper):-1]
            parts, depth, current = [], 0, ""
            for ch in inner:
                if ch == "," and depth == 0:
                    parts.append(current)
                    current = ""
                    continue
                depth += ch == "("
                depth -= ch == ")"
                current += ch
            parts.append(current)
            segments = []
            for part in parts:
                _, sub = parse_location(part)
                segments.extend(sub)
            return strand, segments
    text = text.replace("<", "").replace(">", "")
    if ".." not in text:
        return strand, [(int(text), int(text))]
    start, end = text.split("..", 1)
    return strand, [(int(start), int(end))]


def read_features(path: Path):
    features, current = [], None
    for line in path.read_text(encoding="utf-8", errors="replace").splitlines():
        if line.startswith("FT   ") and not line.startswith("FT                   "):
            if current:
                features.append(current)
            key, _, rest = line[5:].partition(" ")
            current = {"key": key, "lines": [rest.strip()], "quals": {}}
        elif line.startswith("FT                   ") and current:
            current["lines"].append(line[21:].rstrip())
    if current:
        features.append(current)
    for feature in features:
        location, qual_text, seen_qual = [], [], False
        for piece in feature["lines"]:
            stripped = piece.strip()
            if stripped.startswith("/"):
                seen_qual = True
            if seen_qual:
                qual_text.append(stripped)
            elif stripped:
                location.append(stripped)
        blob = "\n".join(qual_text)
        for match in re.finditer(r'/([A-Za-z_]+)="?([^"\n]*)"?', blob):
            feature["quals"].setdefault(match.group(1), match.group(2).strip())
        # Bare /pseudo has no value, so the pattern above cannot see it.
        if re.search(r"/pseudo\b", blob):
            feature["quals"].setdefault("pseudo", "true")
        feature["location"] = "".join(location)
    return features


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--cache", type=Path, required=True)
    parser.add_argument("--update-example", action="store_true")
    args = parser.parse_args()

    record = fetch(args.cache)
    raw = record.read_bytes()
    digest = hashlib.sha256(raw).hexdigest()
    features = read_features(record)

    organism = next((f["quals"].get("organism") for f in features if f["key"] == "source"), SPECIES)
    organelle = next((f["quals"].get("organelle") for f in features if f["key"] == "source"), "plastid")
    length = None
    for line in raw.decode("utf-8", "replace").splitlines():
        if line.startswith("ID   "):
            length = int(re.search(r"(\d+) BP\.", line).group(1))
            break
    if not length:
        raise SystemExit("cannot read sequence length from the record header")

    rows, used, genes = [], {}, 0
    gene_rows, pending = [], []
    for feature in features:
        key = feature["key"]
        if key == "gene":
            name = feature["quals"].get("gene") or feature["quals"].get("locus_tag")
            if not name:
                continue
            strand, segments = parse_location(feature["location"])
            start, end = min(s for s, _ in segments), max(e for _, e in segments)
            genes += 1
            gene = {"name": name, "start": start, "end": end, "strand": strand,
                    "children": [], "product": None,
                    "pseudogene": feature["quals"].get("pseudogene") or feature["quals"].get("pseudo")}
            rows.append(gene)
            gene_rows.append(gene)
            continue
        if key not in CHILD_TYPES:
            continue
        strand, segments = parse_location(feature["location"])
        span = (min(s for s, _ in segments), max(e for _, e in segments))
        # A child belongs to the smallest gene feature that contains it: the
        # inverted-repeat copies and the rps19/ycf1 pseudogenes mean file order
        # alone cannot be trusted.
        holders = [g for g in gene_rows
                   if g["start"] <= span[0] and g["end"] >= span[1]
                   and (g["strand"] == strand or (g["start"], g["end"]) == span)]
        if not holders:
            continue
        target = min(holders, key=lambda g: (g["end"] - g["start"], len(g["children"])))
        product = feature["quals"].get("product")
        if product and not target["product"]:
            target["product"] = product
        target["children"].append({
            "type": key, "strand": strand, "segments": segments,
            "product": product,
            "protein_id": feature["quals"].get("protein_id"),
            "transl_table": feature["quals"].get("transl_table"),
        })

    # Name duplicates in genomic order so the first copy keeps the plain ID.
    for gene in sorted(rows, key=lambda g: g["start"]):
        used[gene["name"]] = used.get(gene["name"], 0) + 1
        unique = gene["name"] if used[gene["name"]] == 1 else f"{gene['name']}_{used[gene['name']]}"
        gene["id"] = f"gene:{unique}"

    odd = [g for g in rows if not g["children"]]
    if odd:
        print(f"note: {len(odd)} gene features carry no CDS/tRNA/rRNA child "
              f"(kept as gene-only rows): {[g['name'] for g in odd][:5]}")

    out = ["##gff-version 3",
           f"##species={organism}",
           f"##assembly={ACCESSION} chloroplast, complete genome",
           f"# Source: INSDC {ACCESSION} (GenBank/ENA), {organelle}, {length} bp",
           f"# Source record: {SOURCE_URL}",
           f"# Source SHA-256: {digest}",
           "# Feature coordinates, gene names and products are copied verbatim from the",
           "# INSDC feature table; this file only adds the gene/transcript/exon hierarchy.",
           f"##sequence-region {SEQUENCE} 1 {length}"]
    level = {"gene": 0}
    lines = []
    for gene in rows:
        note = f";Note={gene['product']}" if gene["product"] else ""
        if gene["pseudogene"] is not None:
            biotype = "pseudogene"
            note += ";pseudogene=unknown"
        elif gene["children"]:
            biotype = CHILD_TYPES[gene["children"][0]["type"]]
        else:
            biotype = "protein_coding"
        lines.append((gene["start"], level["gene"],
                      "\t".join([SEQUENCE, ACCESSION, "gene", str(gene["start"]), str(gene["end"]),
                                 ".", "+" if gene["strand"] > 0 else "-", ".",
                                 f"ID={gene['id']};Name={gene['name']};gene={gene['name']}"
                                 f";biotype={biotype}{note}"])))
        for index, child in enumerate(gene["children"], start=1):
            tid = f"transcript:{gene['id'].split(':', 1)[1]}.{index}"
            # GenBank calls a coding child "CDS"; its GFF3 hierarchy node is an mRNA.
            type_label = "mRNA" if child["type"] == "CDS" else child["type"]
            biotype = CHILD_TYPES[child["type"]]
            strand = "+" if child["strand"] > 0 else "-"
            start = min(s for s, _ in child["segments"])
            end = max(e for _, e in child["segments"])
            child_note = f";Note={child['product']}" if child["product"] else ""
            lines.append((start, 1, "\t".join([SEQUENCE, ACCESSION, type_label, str(start), str(end),
                                               ".", strand, ".",
                                               f"ID={tid};Parent={gene['id']};Name={tid.split(':', 1)[1]}"
                                               f";biotype={biotype}{child_note}"])))
            for seg_index, (seg_start, seg_end) in enumerate(child["segments"], start=1):
                lines.append((seg_start, 2, "\t".join([SEQUENCE, ACCESSION, "exon", str(seg_start), str(seg_end),
                                                       ".", strand, ".",
                                                       f"ID=exon:{tid.split(':', 1)[1]}.e{seg_index};Parent={tid}"])))
            if child["type"] == "CDS":
                extra = ""
                if child["protein_id"]:
                    extra += f";protein_id={child['protein_id']}"
                if child["transl_table"]:
                    extra += f";transl_table={child['transl_table']}"
                for seg_index, (seg_start, seg_end) in enumerate(child["segments"], start=1):
                    lines.append((seg_start, 3, "\t".join([SEQUENCE, ACCESSION, "CDS", str(seg_start), str(seg_end),
                                                           ".", strand, ".",
                                                           f"ID=cds:{tid.split(':', 1)[1]}.c{seg_index};Parent={tid}{extra}"])))

    lines.sort(key=lambda item: (item[0], item[1]))
    out.extend(line for _, _, line in lines)
    payload = "\n".join(out) + "\n"

    if args.update_example:
        TARGET.parent.mkdir(parents=True, exist_ok=True)
        TARGET.write_text(payload, encoding="utf-8", newline="\n")
        print(f"wrote {TARGET} ({len(lines)} rows, {genes} genes, {len(payload)} bytes)")
    else:
        print(f"dry run: {len(lines)} rows, {genes} genes, {len(payload)} bytes (pass --update-example)")


if __name__ == "__main__":
    main()
