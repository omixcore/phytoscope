"""Convert an NCBI feature table into a minimal GFF3 block.

Used once, offline, to build the bundled PhytoScope example annotations from
public RefSeq records. The output is a flat GFF3 (gene / mRNA / CDS) laid out
exactly like the hand-curated chromosome 1 and 2 blocks, so a single parser
path reads every sequence in the file.

NCBI feature tables carry no parent pointers: a gene row is followed by its
transcripts and CDSs, and the only thread linking them is coordinate
containment. Children are therefore matched to a gene when their envelope sits
inside the gene's envelope.

Usage:  python ft2gff.py <chrom-id> <feature-table.txt> \
            [max-end] [max-genes] [source-column] [emit-exons]

  max-end       skip genes ending past this coordinate (0 = no limit)
  max-genes     keep only the first N loci (0 = all)
  source-column value written to GFF3 column 2, e.g. IRGSP-1.0, TAIR10
  emit-exons    "1" to also write exon rows beside each CDS
  skip-trans    "1" to drop trans-spliced loci (mitochondrial nad1/nad2/nad5),
                whose exons sit tens of kb apart and swamp a whole-genome view
"""

import re
import sys


GENE_KINDS = ("gene", "pseudogene")


def parse_feature_table(path):
    """Read the table into a flat, ordered list of feature rows."""
    rows = []
    cur = None
    for raw in open(path, encoding="utf-8", errors="replace"):
        line = raw.rstrip("\n")
        if line.startswith(">"):
            continue

        m = re.match(r"^([<>]?)(\d+)\t([<>]?)(\d+)\t(\S+)$", line)
        if m:
            a, b = int(m.group(2)), int(m.group(4))
            cur = {
                "kind": m.group(5),
                "rev": a > b,
                "loc": [(min(a, b), max(a, b))],
                "q": {},
            }
            cur["span"] = cur["loc"][0]
            for part in cur["loc"]:
                cur["span"] = (
                    min(cur["span"][0], part[0]),
                    max(cur["span"][1], part[1]),
                )
            rows.append(cur)
            continue

        # Extra segments of a multi-part location (a spliced CDS, a join()).
        # NCBI indents qualifier lines with tabs, but continuation coordinates
        # start at column 0, so accept both shapes.
        m = re.match(r"^\t*(\d+)\t(\d+)$", line)
        if m and cur is not None:
            a, b = int(m.group(1)), int(m.group(2))
            part = (min(a, b), max(a, b))
            cur["loc"].append(part)
            cur["span"] = (
                min(cur["span"][0], part[0]),
                max(cur["span"][1], part[1]),
            )
            continue

        m = re.match(r"^\t+([A-Za-z_][\w./-]*)\t(.*)$", line)
        if m and cur is not None:
            cur["q"].setdefault(m.group(1), []).append(m.group(2).strip())

    return rows


def choose_genes(rows, max_end, max_genes, require_cds=False, skip_trans_spliced=False):
    """Select the leading loci, keeping each gene with the features it owns."""
    genes = []
    for row in rows:
        if row["kind"] not in GENE_KINDS:
            continue
        start, end = row["span"]
        if max_end and end > max_end:
            continue
        if skip_trans_spliced and any(
                "trans" in v.lower() for v in row["q"].get("exception", [])):
            continue
        genes.append(row)

    if max_genes:
        genes = genes[:max_genes]

    for gene in genes:
        start, end = gene["span"]
        members = []
        for row in rows:
            if row is gene or row["kind"] in GENE_KINDS:
                continue
            if row["kind"] not in ("mRNA", "tRNA", "rRNA", "ncRNA", "CDS"):
                continue
            rs, re_ = row["span"]
            if rs >= start and re_ <= end:
                members.append(row)
        gene["members"] = members
    if require_cds:
        genes = [g for g in genes
                 if any(m["kind"] == "CDS" for m in g.get("members", []))]
    return genes


def first_qualifier(feature, key, fallback=""):
    values = feature["q"].get(key)
    return values[0] if values else fallback


def phytozome_name(gene):
    """Return the Phytozome symbol carried by the record, if any.

    The NCBI Wm82-ISU-01 soybean record carries both an internal locus_tag
    (AAZV13_02G000100) and the Phytozome id (GmISU01.02G000100). The Phytozome
    id maps onto the classic Glyma.02G... naming a soybean reader recognises,
    so it is the better display name.
    """
    for value in gene["q"].get("db_xref", []):
        if value.startswith("Phytozome:"):
            return value.split(":", 1)[1]
    return None


def emit(chrom, genes, out, source="TAIR10", emit_exons=False):
    for gene in genes:
        gs, ge = gene["span"]
        strand = "-" if gene["rev"] else "+"
        # RefSeq records label nuclear genes by locus_tag, but organellar and
        # older records may only carry gene / gene_syn; fall back so the same
        # builder serves every source. When a short RAP-DB / TAIR-style symbol
        # is present in the "gene" qualifier and the locus_tag is a long
        # internal id, prefer the symbol — that is the name a reader recognises.
        gid = phytozome_name(gene) or first_qualifier(gene, "locus_tag")
        symbol = first_qualifier(gene, "gene")
        if symbol and gid and len(symbol) < len(gid) and symbol[:2].isalpha():
            gid = symbol
        if not gid:
            gid = (first_qualifier(gene, "gene_syn") or symbol)
        if not gid:
            continue
        note = first_qualifier(gene, "note") or first_qualifier(gene, "gene")
        if not note:
            for member in gene.get("members", []):
                product = (first_qualifier(member, "product")
                           or first_qualifier(member, "prot_desc"))
                if product:
                    note = product
                    break
        if not note:
            note = "uncharacterized protein"
        # RefSeq protein descriptions read "Os02t0100050-00: Thioredoxin ...";
        # keep only the human-readable part.
        if ": " in note:
            note = note.split(": ", 1)[1]

        out.append(
            "%s\t%s\tgene\t%d\t%d\t.\t%s\t.\t"
            "ID=%s;Name=%s;Note=%s;locus_tag=%s"
            % (chrom, source, gs, ge, strand, gid, gid, note, gid)
        )

        rnas = [m for m in gene.get("members", [])
                if m["kind"] in ("mRNA", "tRNA", "rRNA", "ncRNA")]
        cds = [m for m in gene.get("members", []) if m["kind"] == "CDS"]
        if not rnas:
            # Some RefSeq prokaryote-style and older plant records attach the
            # CDS straight to the gene with no transcript layer. Synthesise one
            # so every dataset exposes the same gene -> transcript -> exon
            # hierarchy, which is what the renderer and detail pane expect. The
            # transcript spans the whole gene, and each CDS segment becomes an
            # exon of its own.
            if not cds:
                continue
            span = (min(m["span"][0] for m in cds), max(m["span"][1] for m in cds))
            merged = []
            for m in cds:
                merged.extend(m["loc"])
            rnas = [{"kind": "mRNA", "span": gene["span"], "loc": sorted(merged),
                     "q": {}, "rev": gene["rev"]}]

        for index, rna in enumerate(rnas, 1):
            tid = "%s.%d" % (gid, index)
            rs, re_ = rna["span"]
            rtype = "mRNA" if rna["kind"] == "mRNA" else rna["kind"]
            out.append(
                "%s\t%s\t%s\t%d\t%d\t.\t%s\t.\tID=%s;Parent=%s;Name=%s"
                % (chrom, source, rtype, rs, re_, strand, tid, gid, tid)
            )
            for c in cds:
                # With no transcript layer the synthetic RNA owns every CDS of
                # the gene, so accept any CDS inside the gene envelope.
                if rna["loc"]:
                    if c["span"] != rna["span"] and not (
                        c["span"][0] >= rs and c["span"][1] <= re_
                    ):
                        continue
                elif not (c["span"][0] >= gs and c["span"][1] <= ge):
                    continue
                for (a, b) in c["loc"]:
                    if emit_exons:
                        out.append(
                            "%s\t%s\texon\t%d\t%d\t.\t%s\t.\tParent=%s"
                            % (chrom, source, a, b, strand, tid)
                        )
                    out.append(
                        "%s\t%s\tCDS\t%d\t%d\t.\t%s\t0\tParent=%s"
                        % (chrom, source, a, b, strand, tid)
                    )
                if rna["loc"]:
                    break


def main():
    if len(sys.argv) < 3:
        sys.stderr.write(__doc__)
        raise SystemExit(2)
    chrom = sys.argv[1]
    path = sys.argv[2]
    max_end = int(sys.argv[3]) if len(sys.argv) > 3 else 0
    max_genes = int(sys.argv[4]) if len(sys.argv) > 4 else 0
    source = sys.argv[5] if len(sys.argv) > 5 else "TAIR10"
    emit_exons = len(sys.argv) > 6 and sys.argv[6].lower() in ("1", "true", "yes")
    skip_ts = len(sys.argv) > 7 and sys.argv[7].lower() in ("1", "true", "yes")

    rows = parse_feature_table(path)
    genes = choose_genes(rows, max_end, max_genes, require_cds=True,
                         skip_trans_spliced=skip_ts)
    out = []
    emit(chrom, genes, out, source=source, emit_exons=emit_exons)
    sys.stdout.write("\n".join(out) + "\n")
    sys.stderr.write(
        "chrom=%s genes=%d rows=%d features=%d\n"
        % (chrom, len(genes), len(out), len(rows))
    )


main()
