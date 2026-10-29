"""Convert the published horsemint (Mentha longifolia) GTF into GFF3 for PhytoScope.

Input : Long3_HAP_v2.gtf.gz  (Braker3/AUGUSTUS models, figshare 10.6084/m9.figshare.28457522)
Output: gene -> mRNA -> exon/CDS hierarchy, one seqid per chromosome.

The GTF carries two haplotypes (Chr01_A_Long3HAP ... Chr12_B_Long3HAP). By
default only the A haplotype is written, renamed Chr1..Chr12 so the app's
chromosome selector reads naturally; --haplotypes AB writes both.

Usage:
    python tools/build_mint_horsemint_gff3.py --gtf Long3_HAP_v2.gtf.gz \
        --out mentha_longifolia_horsemint.gff3 [--haplotypes AB] [--anno tsv.gz]
"""
from __future__ import annotations

import argparse
import gzip
import re
from pathlib import Path

SEQID = re.compile(r"^Chr(\d{2})_([AB])_Long3HAP$")


def read_anno(path: Path | None) -> dict[str, str]:
    if not path or not path.exists():
        return {}
    out: dict[str, str] = {}
    opener = gzip.open if path.suffix == ".gz" else open
    with opener(path, "rt", encoding="utf-8", errors="replace") as fh:
        for line in fh:
            parts = line.rstrip("\n").split("\t")
            # columns: flag, tags, transcript id, description, ...
            if len(parts) < 4:
                continue
            key = parts[2].strip()
            desc = parts[3].strip()
            if key and desc and key.lower() != "seqname" and not desc.startswith("---"):
                out.setdefault(key, desc[:200])
    return out


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--gtf", type=Path, required=True)
    ap.add_argument("--out", type=Path, required=True)
    ap.add_argument("--haplotypes", default="A")
    ap.add_argument("--anno", type=Path, default=None)
    ap.add_argument("--fasta", type=Path, default=None,
                    help="genome FASTA(.gz) used for the true sequence lengths")
    args = ap.parse_args()

    wanted = set(args.haplotypes.upper())
    anno = read_anno(args.anno)
    print(f"functional annotation entries: {len(anno):,}")

    genes: dict[tuple[str, str], dict] = {}
    transcripts: dict[tuple[str, str], dict] = {}
    order: list[tuple[str, str]] = []
    seq_lengths: dict[str, int] = {}
    skipped = 0

    with gzip.open(args.gtf, "rt", encoding="utf-8", errors="replace") as fh:
        for line in fh:
            if line.startswith("#"):
                continue
            parts = line.rstrip("\n").split("\t")
            if len(parts) < 9:
                continue
            seqid_raw, _, kind, start, end, _, strand, _, attrs = parts[:9]
            m = SEQID.match(seqid_raw)
            if not m or m.group(2) not in wanted:
                skipped += 1
                continue
            seqid = f"Chr{int(m.group(1))}" + ("" if m.group(2) == "A" else "B")
            start_i, end_i = int(start), int(end)
            seq_lengths[seqid] = max(seq_lengths.get(seqid, 0), end_i)

            gene_id = re.search(r'gene_id "([^"]+)"', attrs)
            gene_id = gene_id.group(1) if gene_id else None
            t_id = re.search(r'transcript_id "([^"]+)"', attrs)
            t_id = t_id.group(1) if t_id else None
            # This GTF writes bare identifiers on the gene / transcript rows
            # ("g1", "g1.t1") and only uses key/value pairs on exon-ish rows.
            if gene_id is None and t_id is None and " " not in attrs.strip():
                bare = attrs.strip()
                if kind == "gene":
                    gene_id = bare
                elif kind == "transcript":
                    t_id = bare
                    gene_id = bare.rsplit(".t", 1)[0]
            if gene_id is None and t_id is not None:
                gene_id = t_id.rsplit(".t", 1)[0]
            if gene_id is None:
                continue

            if kind == "gene":
                if (seqid, gene_id) not in genes:
                    genes[(seqid, gene_id)] = {"start": start_i, "end": end_i, "strand": strand,
                                               "tx": []}
                    order.append((seqid, gene_id))
                continue
            if kind == "transcript":
                if not t_id:
                    continue
                transcripts[(seqid, t_id)] = {"gene": gene_id, "start": start_i, "end": end_i,
                                              "strand": strand, "exons": [], "cds": []}
                if (seqid, gene_id) in genes:
                    genes[(seqid, gene_id)]["tx"].append(t_id)
                continue
            if not t_id:
                continue
            rec = transcripts.get((seqid, t_id))
            if rec is None:
                rec = {"gene": gene_id, "start": start_i, "end": end_i, "strand": strand,
                       "exons": [], "cds": []}
                transcripts[(seqid, t_id)] = rec
                if (seqid, gene_id) in genes:
                    genes[(seqid, gene_id)]["tx"].append(t_id)
            if kind == "exon":
                rec["exons"].append((start_i, end_i))
            elif kind == "CDS":
                rec["cds"].append((start_i, end_i))

    print(f"genes={len(genes):,} transcripts={len(transcripts):,} skipped_rows={skipped:,}")

    if args.fasta and args.fasta.exists():
        opener = gzip.open if args.fasta.suffix == ".gz" else open
        real: dict[str, int] = {}
        cur = None
        with opener(args.fasta, "rt", errors="replace") as fh:
            for line in fh:
                if line.startswith(">"):
                    cur = line[1:].split()[0]
                    real[cur] = 0
                elif cur:
                    real[cur] += len(line.strip())
        fixed = 0
        for seqid in list(seq_lengths):
            raw = "Chr%02d_%s_Long3HAP" % (int(seqid[3:].rstrip("B")), "B" if seqid.endswith("B") else "A")
            if raw in real and real[raw] >= seq_lengths[seqid]:
                seq_lengths[seqid] = real[raw]
                fixed += 1
        print(f"sequence lengths taken from {args.fasta.name}: {fixed} replaced")

    out_lines: list[str] = [
        "##gff-version 3",
        "##species=Mentha longifolia",
        "##assembly=Long3_HAP_v2 (diploid unphased, haplotype A)",
        "# Source: Lieberman, Henry & Comai 2025, figshare 10.6084/m9.figshare.28457522",
        "# Braker3/AUGUSTUS gene models, 189,242 genes / 205,350 transcripts across both haplotypes.",
        "# Coordinates are verbatim; this file only re-encodes GTF attributes as GFF3.",
    ]
    for seqid in sorted(seq_lengths):
        out_lines.append(f"##sequence-region {seqid} 1 {seq_lengths[seqid]}")

    rows = 0
    for key in order:
        gene = genes[key]
        seqid, gene_id = key
        gid = f"gene:{gene_id}"
        note = anno.get(gene_id) or next(
            (anno[t] for t in gene["tx"] if t in anno), None)
        biotype = "protein_coding"
        attrs = f"ID={gid};Name={gene_id};gene={gene_id};biotype={biotype}"
        if note:
            attrs += f";Note={note}"
        out_lines.append("\t".join([seqid, "AUGUSTUS", "gene", str(gene["start"]), str(gene["end"]),
                                    ".", gene["strand"], ".", attrs]))
        rows += 1
        for t_id in gene["tx"]:
            rec = transcripts.get((seqid, t_id))
            if not rec:
                continue
            tid = f"transcript:{t_id}"
            out_lines.append("\t".join([seqid, "AUGUSTUS", "mRNA", str(rec["start"]), str(rec["end"]),
                                        ".", rec["strand"], ".",
                                        f"ID={tid};Parent={gid};Name={t_id};biotype={biotype}"]))
            rows += 1
            exons = sorted(set(rec["exons"]))
            for i, (s, e) in enumerate(exons, start=1):
                out_lines.append("\t".join([seqid, "AUGUSTUS", "exon", str(s), str(e), ".",
                                            rec["strand"], ".",
                                            f"ID=exon:{t_id}.e{i};Parent={tid}"]))
                rows += 1
            for i, (s, e) in enumerate(sorted(set(rec["cds"])), start=1):
                out_lines.append("\t".join([seqid, "AUGUSTUS", "CDS", str(s), str(e), ".",
                                            rec["strand"], ".",
                                            f"ID=cds:{t_id}.c{i};Parent={tid}"]))
                rows += 1

    args.out.parent.mkdir(parents=True, exist_ok=True)
    with open(args.out, "w", encoding="utf-8", newline="\n") as fh:
        fh.write("\n".join(out_lines) + "\n")
    print(f"wrote {args.out}  sequences={len(seq_lengths)}  rows={rows:,}  "
          f"bytes={args.out.stat().st_size:,}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
