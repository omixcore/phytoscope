"""Rewrite an ft2gff.py GFF3 block into the GTF dialect used by the soybean sample.

This is a one-off helper for building the bundled example data. It is not part of
the application: PhytoScope itself reads both dialects natively, and the soybean
file is written in GTF precisely to prove that.

Mapping rules (GFF3 -> GTF):
  gene       -> gene        gene_id "<id>"; gene_name "<id>";
  mRNA       -> transcript  gene_id "<gid>"; transcript_id "<mid>";
  exon / CDS -> exon / CDS  gene_id "<gid>"; transcript_id "<mid>";
                            plus protein_id "<mid>.p" on CDS rows

Usage:  python gff2gtf_block.py <in.gff3> [protein-suffix]
"""
import re
import sys

ATTR = re.compile(r"(\w+)=([^;]+)")


def parse_attrs(field):
    return {m.group(1): m.group(2) for m in ATTR.finditer(field)}


def main():
    path = sys.argv[1]
    suffix = sys.argv[2] if len(sys.argv) > 2 else ".p"

    tx_gene = {}          # mRNA id -> owning gene id
    protein_of = {}       # mRNA id -> protein id, assigned on first CDS
    lines = []

    for raw in open(path, encoding="utf-8"):
        line = raw.rstrip("\n")
        if not line or line.startswith("#"):
            continue
        f = line.split("\t")
        if len(f) != 9:
            continue
        seq, src, kind, start, end, score, strand, phase, attr_f = f
        a = parse_attrs(attr_f)

        if kind == "gene":
            gid = a["ID"]
            out_kind = "gene"
            attrs = f'gene_id "{gid}"; gene_name "{gid}";'
        elif kind == "mRNA":
            mid = a["ID"]
            gid = a["Parent"]
            tx_gene[mid] = gid
            out_kind = "transcript"
            attrs = f'gene_id "{gid}"; transcript_id "{mid}";'
        else:
            mid = a.get("Parent", "")
            gid = tx_gene.get(mid, mid)
            out_kind = kind
            attrs = f'gene_id "{gid}"; transcript_id "{mid}";'
            if kind == "CDS":
                pid = protein_of.setdefault(mid, mid + suffix)
                attrs += f' protein_id "{pid}";'

        lines.append("\t".join([seq, src, out_kind, start, end, score,
                                strand, phase, attrs]))

    # GTF wants the gene row first, then its transcripts, then everything else;
    # the GFF3 block is already emitted in that order, so no re-sorting is needed.
    sys.stdout.write("\n".join(lines) + "\n")


if __name__ == "__main__":
    main()
