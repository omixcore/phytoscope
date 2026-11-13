#!/usr/bin/env python3
"""Deterministic generator for the import-health-check test fixtures.

Produces KB-scale, self-contained, committable fixtures under
src/test/resources/intake/ covering three assembly types (chromosome-level,
fragmented scaffold, organelle), negative-strand genes, cross-format coordinate
boundaries, an alias-mismatch case, a missing-seqid case, a standalone
expression matrix, a bad FASTA and a malformed GFF3.

Run from the project root:  python3 gen_intake_fixtures.py
"""
import os
import random

ROOT = os.path.join("src", "test", "resources", "intake")
random.seed(20261027)

NUC = "ACGT"


def seq(length: int) -> str:
    """Length-bounded deterministic pseudo-genomic sequence (no newlines inside)."""
    out = []
    for _ in range(length):
        out.append(random.choice(NUC))
    return "".join(out)


def wrap(text: str, width: int = 70) -> str:
    return "\n".join(text[i:i + width] for i in range(0, len(text), width))


def write_fasta(path: str, records: list[tuple[str, str]]) -> None:
    with open(path, "w", encoding="utf-8") as fh:
        for name, bases in records:
            fh.write(">" + name + "\n")
            fh.write(wrap(bases) + "\n")


def gene_block(seqid, gene_id, start, end, strand, phase=0):
    """A complete gene (gene + mRNA + exon + CDS) on one strand."""
    lines = [
        f"{seqid}\tsource\tgene\t{start}\t{end}\t.\t{strand}\t.\tID={gene_id};Name={gene_id}",
        f"{seqid}\tsource\tmRNA\t{start}\t{end}\t.\t{strand}\t.\tID={gene_id}.t1;Parent={gene_id}",
        f"{seqid}\tsource\texon\t{start}\t{end}\t.\t{strand}\t.\tParent={gene_id}.t1",
        f"{seqid}\tsource\tCDS\t{start}\t{end}\t.\t{strand}\t{phase}\tParent={gene_id}.t1",
    ]
    return lines


def write_gff3(path: str, seq_regions: list[tuple[str, int, int]], blocks: list[list[str]]) -> None:
    with open(path, "w", encoding="utf-8") as fh:
        fh.write("##gff-version 3\n")
        for seqid, lo, hi in seq_regions:
            fh.write(f"##sequence-region {seqid} {lo} {hi}\n")
        for block in blocks:
            fh.write("\n".join(block) + "\n")


def write_tsv(path: str, gene_ids: list[str], tissues=("Root", "Leaf")) -> None:
    with open(path, "w", encoding="utf-8") as fh:
        fh.write("# unit=counts\n")
        fh.write("Gene\t" + "\t".join(tissues) + "\n")
        for gid in gene_ids:
            vals = "\t".join(str(random.randint(0, 1000)) for _ in tissues)
            fh.write(f"{gid}\t{vals}\n")


# ----------------------------------------------------------------------------
# 1. Chromosome-level assembly (happy path, exact seqid match, both strands)
# ----------------------------------------------------------------------------
CHR_LEN = 2600
chr_records = [(f"Chr{i}", seq(CHR_LEN)) for i in range(1, 4)]
write_fasta(os.path.join(ROOT, "chr_level.fa"), chr_records)

chr_blocks = []
chr_genes = []
gid = 0
for ci in range(1, 4):
    for k in range(4):
        gid += 1
        start = 200 + k * 600
        end = start + 299
        strand = "+" if (gid % 2 == 0) else "-"
        name = f"A_{gid:04d}"
        chr_blocks.append(gene_block(f"Chr{ci}", name, start, end, strand))
        chr_genes.append(name)
write_gff3(os.path.join(ROOT, "chr_level.gff3"),
           [(f"Chr{i}", 1, CHR_LEN) for i in range(1, 4)], chr_blocks)
write_tsv(os.path.join(ROOT, "chr_level.tsv"), chr_genes)

# ----------------------------------------------------------------------------
# 2. Fragmented scaffold assembly
# ----------------------------------------------------------------------------
SCAFF_LEN = 1700
scaff_records = [(f"scaffold_{i}", seq(SCAFF_LEN)) for i in range(1, 6)]
write_fasta(os.path.join(ROOT, "scaffold_fragmented.fa"), scaff_records)

scaff_blocks = []
scaff_genes = []
gid = 0
for si in range(1, 6):
    for k in range(3):
        gid += 1
        start = 150 + k * 500
        end = start + 249
        strand = "+" if (gid % 3 != 0) else "-"
        name = f"S_{gid:04d}"
        scaff_blocks.append(gene_block(f"scaffold_{si}", name, start, end, strand))
        scaff_genes.append(name)
write_gff3(os.path.join(ROOT, "scaffold_fragmented.gff3"),
           [(f"scaffold_{i}", 1, SCAFF_LEN) for i in range(1, 6)], scaff_blocks)
write_tsv(os.path.join(ROOT, "scaffold_fragmented.tsv"), scaff_genes)

# ----------------------------------------------------------------------------
# 3. Organelle assembly (mitochondrion + chloroplast)
# ----------------------------------------------------------------------------
ORG_LEN = 1300
org_records = [("chrM", seq(ORG_LEN)), ("chrC", seq(ORG_LEN))]
write_fasta(os.path.join(ROOT, "organelle.fa"), org_records)

org_blocks = []
org_genes = []
gid = 0
for sid in ("chrM", "chrC"):
    for k in range(3):
        gid += 1
        start = 120 + k * 400
        end = start + 199
        strand = "+" if (gid % 2 == 1) else "-"
        name = f"O_{gid:04d}"
        org_blocks.append(gene_block(sid, name, start, end, strand))
        org_genes.append(name)
write_gff3(os.path.join(ROOT, "organelle.gff3"),
           [("chrM", 1, ORG_LEN), ("chrC", 1, ORG_LEN)], org_blocks)
write_tsv(os.path.join(ROOT, "organelle.tsv"), org_genes)

# ----------------------------------------------------------------------------
# 4. Alias-mismatch assembly (FASTA Chr1/Chr2, GFF3 uses 1/2)
# ----------------------------------------------------------------------------
ALIAS_FA_LEN = 2000
write_fasta(os.path.join(ROOT, "alias_mismatch.fa"),
            [("Chr1", seq(ALIAS_FA_LEN)), ("Chr2", seq(ALIAS_FA_LEN))])

alias_blocks = []
alias_genes = []
for idx, (fa_id, gff_id) in enumerate((("Chr1", "1"), ("Chr2", "2"))):
    start = 200 + idx * 700
    end = start + 299
    name = f"M_{idx + 1:04d}"
    alias_blocks.append(gene_block(gff_id, name, start, end, "+"))
    alias_genes.append(name)
write_gff3(os.path.join(ROOT, "alias_mismatch.gff3"),
           [("1", 1, ALIAS_FA_LEN), ("2", 1, ALIAS_FA_LEN)], alias_blocks)
write_tsv(os.path.join(ROOT, "alias_mismatch.tsv"), alias_genes)

# ----------------------------------------------------------------------------
# 5. Missing-seqid assembly (FASTA has only Chr1; GFF3 also references Chr2)
# ----------------------------------------------------------------------------
write_fasta(os.path.join(ROOT, "missing_seqid.fa"), [("Chr1", seq(2200))])
miss_blocks = []
miss_genes = []
# gene on Chr1 (present)
miss_blocks.append(gene_block("Chr1", "P_0001", 200, 499, "+"))
miss_genes.append("P_0001")
# gene on Chr2 (absent from FASTA -> ERROR)
miss_blocks.append(gene_block("Chr2", "P_0002", 200, 499, "-"))
miss_genes.append("P_0002")
write_gff3(os.path.join(ROOT, "missing_seqid.gff3"),
           [("Chr1", 1, 2200), ("Chr2", 1, 2200)], miss_blocks)
write_tsv(os.path.join(ROOT, "missing_seqid.tsv"), miss_genes)

# ----------------------------------------------------------------------------
# 6. Negative-strand focused assembly (explicit - strand, with line numbers)
# ----------------------------------------------------------------------------
write_fasta(os.path.join(ROOT, "negative_strand.fa"), [("Chr1", seq(2400))])
neg_blocks = []
neg_genes = []
neg_blocks.append(gene_block("Chr1", "N_0001", 300, 699, "-"))
neg_genes.append("N_0001")
neg_blocks.append(gene_block("Chr1", "N_0002", 1100, 1499, "+"))
neg_genes.append("N_0002")
write_gff3(os.path.join(ROOT, "negative_strand.gff3"),
           [("Chr1", 1, 2400)], neg_blocks)
write_tsv(os.path.join(ROOT, "negative_strand.tsv"), neg_genes)

# ----------------------------------------------------------------------------
# 7. Coordinate-boundary assembly (genes at start=1 and at the very end)
# ----------------------------------------------------------------------------
CB_LEN = 2000
write_fasta(os.path.join(ROOT, "coord_boundary.fa"), [("Chr1", seq(CB_LEN))])
cb_blocks = []
cb_genes = []
# gene right at the start (1-based) of the sequence
cb_blocks.append(gene_block("Chr1", "B_0001", 1, 200, "+"))
cb_genes.append("B_0001")
# gene near the end (end == sequence length)
cb_blocks.append(gene_block("Chr1", "B_0002", CB_LEN - 199, CB_LEN, "-"))
cb_genes.append("B_0002")
# an interior gene
cb_blocks.append(gene_block("Chr1", "B_0003", 900, 1199, "+"))
cb_genes.append("B_0003")
write_gff3(os.path.join(ROOT, "coord_boundary.gff3"),
           [("Chr1", 1, CB_LEN)], cb_blocks)
write_tsv(os.path.join(ROOT, "coord_boundary.tsv"), cb_genes)

# ----------------------------------------------------------------------------
# 8. Standalone expression matrix (no annotation in the wizard)
# ----------------------------------------------------------------------------
write_tsv(os.path.join(ROOT, "standalone_expression.tsv"),
          [f"X_{i:04d}" for i in range(1, 9)])

# ----------------------------------------------------------------------------
# 9. Bad FASTA (contains a non-nucleotide character -> parse error)
# ----------------------------------------------------------------------------
with open(os.path.join(ROOT, "bad_fasta.fa"), "w", encoding="utf-8") as fh:
    fh.write(">Chr1\n")
    fh.write(wrap(seq(500) + "Z" + seq(500)) + "\n")

# ----------------------------------------------------------------------------
# 10. Malformed GFF3 (valid genes + rows with fewer than 9 columns / bad coords)
# ----------------------------------------------------------------------------
with open(os.path.join(ROOT, "bad_gff3.gff3"), "w", encoding="utf-8") as fh:
    fh.write("##gff-version 3\n")
    fh.write("##sequence-region Chr1 1 2000\n")
    # a valid gene
    for line in gene_block("Chr1", "G_0001", 200, 599, "+"):
        fh.write(line + "\n")
    # malformed: fewer than 9 columns
    fh.write("Chr1\tsource\tgene\t200\t599\t.\t+\n")
    # malformed: start greater than end
    fh.write("Chr1\tsource\tgene\t900\t800\t.\t+\t.\tID=G_BAD1\n")
    # malformed: non-numeric coordinate
    fh.write("Chr1\tsource\tgene\tabc\tdef\t.\t+\t.\tID=G_BAD2\n")
    # another valid gene
    for line in gene_block("Chr1", "G_0002", 1200, 1599, "-"):
        fh.write(line + "\n")

print("Fixtures written to", ROOT)
for name in sorted(os.listdir(ROOT)):
    p = os.path.join(ROOT, name)
    print(f"  {name:28s} {os.path.getsize(p):7d} bytes")
