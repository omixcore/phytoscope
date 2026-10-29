"""Build a complete Mentha longifolia (CMEN585 v3) GFF3 from the ENA WGS record.

Source: ENA WGS set LSBG02 (assembly GCA_001642375.2, Mlong_CMEN585_v3), the
INSDC submission behind the WSU Mint Genome Resource. The flat file carries the
full Maker annotation for all twelve chromosome-scale records; this script
re-encodes it as GFF3 with an explicit gene -> mRNA -> exon/CDS hierarchy while
keeping every coordinate verbatim.

Usage:
    python tools/build_mint_full_reference.py --dat LSBG02.dat.gz --out <file.gff3>
"""
from __future__ import annotations

import argparse
import gzip
import hashlib
import re
import sys
from pathlib import Path

# Assembly accession -> chromosome used by the app's sequence selector.
CHROMOSOMES = {
    "CM038110.1": "Chr1",
    "CM038111.1": "Chr2",
    "CM038112.1": "Chr3",
    "CM038113.1": "Chr4",
    "CM038114.1": "Chr5",
    "CM038115.1": "Chr6",
    "CM038116.1": "Chr7",
    "CM038117.1": "Chr8",
    "CM038118.1": "Chr9",
    "CM038119.1": "Chr10",
    "CM038120.1": "Chr11",
    "CM038121.1": "Chr12",
}

ASSEMBLY = "Mlong_CMEN585_v3"


def parse_location(text: str):
    """Return (strand, [(start, end), ...]) for an EMBL location string."""
    text = "".join(text.split())
    strand = 1
    while text.startswith("complement(") and text.endswith(")"):
        strand *= -1
        text = text[len("complement("):-1]
    for wrapper in ("join(", "order("):
        if text.startswith(wrapper) and text.endswith(")"):
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
            segments.sort()
            return strand, segments
    text = text.replace("<", "").replace(">", "")
    if ".." not in text:
        return strand, [(int(text), int(text))]
    start, end = text.split("..", 1)
    return strand, [(int(start), int(end))]


def parse_record_ft(lines):
    """Turn the FT block of one record into feature dicts."""
    features, current = [], None
    for line in lines:
        if line.startswith("FT   ") and not line.startswith("FT                   "):
            if current:
                features.append(current)
            key, _, rest = line[5:].partition(" ")
            current = {"key": key.strip(), "lines": [rest.strip()], "quals": {}}
        elif line.startswith("FT                   ") and current:
            current["lines"].append(line[21:].rstrip())
    if current:
        features.append(current)

    out = []
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
        # Strip the quotes that wrap multi-line qualifier values.
        for match in re.finditer(r'/([A-Za-z_]+)="?([^"\n]*)"?', blob):
            feature["quals"].setdefault(match.group(1), match.group(2).strip())
        if re.search(r"/pseudo\b", blob):
            feature["quals"].setdefault("pseudo", "true")
        loc = "".join(location)
        if not loc:
            continue
        try:
            strand, segments = parse_location(loc)
        except Exception:
            continue
        feature["location"] = loc
        feature["strand"] = strand
        feature["segments"] = segments
        feature["start"] = min(s for s, _ in segments)
        feature["end"] = max(e for _, e in segments)
        out.append(feature)
    return out


def records(path: Path):
    """Yield (accession, length, features) per EMBL record."""
    opener = gzip.open if path.suffix == ".gz" else open
    acc, length, ft_lines, id_line = None, None, [], ""
    with opener(path, "rt", encoding="utf-8", errors="replace") as fh:
        for line in fh:
            if line.startswith("ID   "):
                id_line = line.rstrip()
            elif line.startswith("AC   "):
                acc = line[5:].split(";")[0].strip()
            elif line.startswith("FT   "):
                ft_lines.append(line.rstrip("\n"))
            elif line.startswith("SQ   "):
                m = re.search(r"(\d+) BP", line)
                if m:
                    length = int(m.group(1))
            elif line.startswith("//"):
                if acc and length:
                    yield acc, length, parse_record_ft(ft_lines)
                acc, length, ft_lines, id_line = None, None, [], ""


def build_record(acc: str, seqid: str, length: int, features, source: str):
    genes = [f for f in features if f["key"] in ("gene", "pseudogene", "ncRNA_gene")]
    transcripts = [f for f in features if f["key"] in ("mRNA", "tRNA", "rRNA", "ncRNA", "transcript")]
    exons = [f for f in features if f["key"] == "exon"]
    cds = [f for f in features if f["key"] == "CDS"]

    # Genes without an explicit gene row: promote the transcript itself.
    if not genes:
        for t in transcripts:
            t["key"] = "gene"
        genes, transcripts = transcripts, []

    def name_of(f, prefix):
        q = f["quals"]
        return (q.get("gene") or q.get("locus_tag") or q.get("Name")
                or q.get("standard_name") or f"{prefix}{f['start']}")

    def biotype(f):
        q = f["quals"]
        if q.get("pseudo") or q.get("pseudogene"):
            return "pseudogene"
        key = f["key"]
        if key == "tRNA":
            return "tRNA"
        if key == "rRNA":
            return "rRNA"
        if key in ("ncRNA", "ncRNA_gene"):
            return "ncRNA"
        if q.get("transcript_biotype", "").startswith("protein_coding"):
            return "protein_coding"
        return "protein_coding"

    used = {}

    def unique(base: str) -> str:
        used[base] = used.get(base, 0) + 1
        return base if used[base] == 1 else f"{base}.{used[base]}"

    # transcript -> gene by qualifier, else smallest containing gene.
    assigned = {id(g): [] for g in genes}
    orphans = []
    for t in transcripts:
        q = t["quals"]
        tag = q.get("gene") or q.get("locus_tag")
        holders = [g for g in genes
                   if g["start"] <= t["start"] and g["end"] >= t["end"]
                   and (g["strand"] == t["strand"] or (g["start"], g["end"]) == (t["start"], t["end"]))]
        target = None
        if tag:
            named = [g for g in holders if tag in (g["quals"].get("gene"), g["quals"].get("locus_tag"))]
            if named:
                target = min(named, key=lambda g: g["end"] - g["start"])
        if target is None and holders:
            target = min(holders, key=lambda g: (g["end"] - g["start"], len(assigned[id(g)])))
        if target is None:
            orphans.append(t)
        else:
            assigned[id(target)].append(t)

    # A gene with no transcript row but with CDS children still needs a node.
    rows = []
    for g in genes:
        gname = name_of(g, "MLOC_")
        gid = "gene:" + unique(gname)
        g["gid"], g["gname"] = gid, gname
        rows.append([g["start"], 0, "\t".join([
            seqid, source, "gene", str(g["start"]), str(g["end"]), ".",
            "+" if g["strand"] > 0 else "-", ".",
            f"ID={gid};Name={gname};gene={gname};biotype={biotype(g)}"
            + (f";Note={g['quals']['product']}" if g["quals"].get("product") else "")])])

        children = assigned[id(g)]
        if not children:
            # Use CDS rows that fall inside this gene as one implicit transcript.
            inner_cds = [c for c in cds if g["start"] <= c["start"] and g["end"] >= c["end"]]
            inner_exons = [e for e in exons if g["start"] <= e["start"] and g["end"] >= e["end"]]
            if not inner_cds and not inner_exons:
                continue
            children = [{"quals": {"locus_tag": gname}, "strand": g["strand"],
                         "start": g["start"], "end": g["end"], "key": "mRNA",
                         "segments": [(g["start"], g["end"])],
                         "implicit": True,
                         "_cds": inner_cds, "_exons": inner_exons}]

        for index, t in enumerate(children, start=1):
            tid_base = gname if len(children) == 1 else f"{gname}.t{index}"
            tid = "transcript:" + tid_base
            ttype = "mRNA" if t["key"] in ("mRNA", "transcript") else t["key"]
            tstart, tend = t["start"], t["end"]
            strand = "+" if t["strand"] > 0 else "-"
            tb = biotype(t) if not t.get("implicit") else "protein_coding"
            rows.append([tstart, 1, "\t".join([
                seqid, source, ttype, str(tstart), str(tend), ".", strand, ".",
                f"ID={tid};Parent={g['gid']};Name={tid_base};biotype={tb}"
                + (f";Note={t['quals']['product']}" if t["quals"].get("product") else "")])])

            if t.get("implicit"):
                cds_parts = [(c["start"], c["end"]) for c in t["_cds"]]
                exon_parts = [(e["start"], e["end"]) for e in t["_exons"]] or cds_parts
            else:
                cds_parts = [(c["start"], c["end"]) for c in cds
                             if tstart <= c["start"] and tend >= c["end"] and c["strand"] == t["strand"]]
                exon_parts = [(e["start"], e["end"]) for e in exons
                              if tstart <= e["start"] and tend >= e["end"] and e["strand"] == t["strand"]]
                if not exon_parts:
                    exon_parts = cds_parts

            for i, (s, e) in enumerate(sorted(exon_parts), start=1):
                rows.append([s, 2, "\t".join([
                    seqid, source, "exon", str(s), str(e), ".", strand, ".",
                    f"ID=exon:{tid_base}.e{i};Parent={tid}"] )])
            for i, (s, e) in enumerate(sorted(cds_parts), start=1):
                rows.append([s, 3, "\t".join([
                    seqid, source, "CDS", str(s), str(e), ".", strand, ".",
                    f"ID=cds:{tid_base}.c{i};Parent={tid}"])])

    rows.sort(key=lambda item: (item[0], item[1]))
    return rows, len(genes)


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--dat", type=Path, required=True)
    ap.add_argument("--out", type=Path, required=True)
    ap.add_argument("--include-scaffolds", action="store_true",
                    help="also emit unplaced scaffolds (seqid Scaffold_N)")
    args = ap.parse_args()

    digest = hashlib.sha256()
    with open(args.dat, "rb") as fh:
        for block in iter(lambda: fh.read(1 << 20), b""):
            digest.update(block)

    header = [
        "##gff-version 3",
        "##species=Mentha longifolia",
        f"##assembly={ASSEMBLY} (INSDC GCA_001642375.2)",
        "# Source: ENA WGS set LSBG02 (Mlong_CMEN585_v3, Oregon State University / WSU Mint Genome Resource)",
        f"# Source file: LSBG02.dat.gz  SHA-256 {digest.hexdigest()}",
        "# Complete Maker annotation: every gene row of the INSDC submission is preserved;",
        "# coordinates are verbatim and this file only adds the gene/transcript/exon hierarchy.",
    ]
    body, seq_regions = [], []
    total_genes, total_rows, seen = 0, 0, []
    for acc, length, features in records(args.dat):
        if acc in CHROMOSOMES:
            seqid = CHROMOSOMES[acc]
        elif args.include_scaffolds:
            seqid = f"Scaffold_{acc}"
        else:
            continue
        rows, genes = build_record(acc, seqid, length, features, ASSEMBLY)
        seq_regions.append(f"##sequence-region {seqid} 1 {length}")
        body.extend(row[2] for row in rows)
        total_genes += genes
        total_rows += len(rows)
        seen.append((seqid, acc, length, genes))
        print(f"{seqid:12s} {acc:12s} {length:>12,} bp  {genes:>6,} genes  {len(rows):>7,} rows", flush=True)

    args.out.parent.mkdir(parents=True, exist_ok=True)
    with open(args.out, "w", encoding="utf-8", newline="\n") as fh:
        fh.write("\n".join(header + seq_regions + body) + "\n")
    print(f"\nwrote {args.out}  sequences={len(seen)}  genes={total_genes:,}  rows={total_rows:,}  "
          f"bytes={args.out.stat().st_size:,}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
