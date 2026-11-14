#!/usr/bin/env python3
"""Render PhytoScope FCS paper main figures (Figure 1-5) from real, frozen data.

Self-contained: this script reads the machine-readable evidence files that are
already committed in the repository and renders PNG (300 dpi) + SVG for each
main figure. No numbers are invented here; values transcribed from the Markdown
evidence documents carry an inline source comment and are only used where a
machine-readable file does not exist for that specific quantity.

Data sources (all relative to the repository root):
  - Figure 1: schematic (workflow); class names from src/main/java/...
  - Figure 2: src/test/resources/non-model/vigna-umbellata/manifest.json (real),
              docs/input-format-specification.md (coordinate conventions, real).
  - Figure 3: docs/research-module-validation.md (real CI results),
              src/test/resources/non-model/vigna-umbellata/manifest.json.
  - Figure 4: docs/chromosome-validation.md (four model Ensembl r62 counts),
              src/test/resources/non-model/additional-nonmodel-manifest.json,
              src/test/resources/non-model/vigna-umbellata/manifest.json,
              src/test/resources/non-model/multi-locus-sequence-gold.tsv.
  - Figure 5: target/phase3-comparison/gffutils/summary.json (real measured),
              target/phase3-comparison/biopython/summary.json (real measured),
              docs/fcs-phase3-comparison-plan.md (qualitative matrix, marked).

Run from the repository root:  python docs/paper-figures/make_paper_figures.py
"""

from __future__ import annotations

import csv
import json
import os

import matplotlib

matplotlib.use("Agg")  # headless rendering, no display needed

import matplotlib.pyplot as plt
import numpy as np
from matplotlib.patches import FancyArrowPatch, FancyBboxPatch, Patch, Rectangle

# --------------------------------------------------------------------------- #
# Paths and constants
# --------------------------------------------------------------------------- #

REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT = os.path.join(REPO, "docs", "paper-figures")
os.makedirs(OUT, exist_ok=True)

VIGNA_MANIFEST = os.path.join(
    REPO, "src", "test", "resources", "non-model", "vigna-umbellata", "manifest.json"
)
ADDITIONAL_MANIFEST = os.path.join(
    REPO, "src", "test", "resources", "non-model", "additional-nonmodel-manifest.json"
)
MULTILOCUS_GOLD = os.path.join(
    REPO, "src", "test", "resources", "non-model", "multi-locus-sequence-gold.tsv"
)
GFFUTILS_SUMMARY = os.path.join(
    REPO, "target", "phase3-comparison", "gffutils", "summary.json"
)
BIOPYTHON_SUMMARY = os.path.join(
    REPO, "target", "phase3-comparison", "biopython", "summary.json"
)

# Academic palette (no finance red/green semantics).
BLUE = "#2C7BB6"
TEAL = "#008080"
ORANGE = "#E69F00"
PURPLE = "#7570B3"
GREEN = "#4DAF4A"   # used only as a "match / agreement" check colour
GRAY = "#9E9E9E"
DARK = "#333333"
LIGHT = "#F5F5F5"
AMBER = "#FDB863"

plt.rcParams.update(
    {
        "font.family": "DejaVu Sans",
        "font.size": 10,
        "axes.edgecolor": DARK,
        "axes.labelcolor": DARK,
        "text.color": DARK,
        "xtick.color": DARK,
        "ytick.color": DARK,
        "figure.facecolor": "white",
        "axes.facecolor": "white",
        "svg.fonttype": "none",  # keep text as text in SVG
    }
)


def save(fig, name: str) -> None:
    """Save a figure as PNG (print dpi) and SVG, returning output paths."""
    png = os.path.join(OUT, name + ".png")
    svg = os.path.join(OUT, name + ".svg")
    fig.savefig(png, dpi=300, bbox_inches="tight", facecolor="white")
    fig.savefig(svg, bbox_inches="tight", facecolor="white")
    plt.close(fig)
    print(f"  wrote {os.path.relpath(png, REPO)}")
    print(f"  wrote {os.path.relpath(svg, REPO)}")


# --------------------------------------------------------------------------- #
# Loaders for the real, frozen evidence files
# --------------------------------------------------------------------------- #

def load_json(path: str):
    with open(path, "r", encoding="utf-8") as fh:
        return json.load(fh)


def load_vigna_manifest():
    return load_json(VIGNA_MANIFEST)


def load_additional_manifest():
    return load_json(ADDITIONAL_MANIFEST)


def load_multilocus_gold():
    rows = []
    with open(MULTILOCUS_GOLD, "r", encoding="utf-8") as fh:
        reader = csv.DictReader(fh, delimiter="\t")
        for row in reader:
            row["start"] = int(row["start"])
            row["end"] = int(row["end"])
            row["exon_count"] = int(row["exon_count"])
            row["cds_segments"] = int(row["cds_segments"])
            row["cds_length"] = int(row["cds_length"])
            row["protein_aa"] = int(row["protein_aa"])
            rows.append(row)
    return rows


def load_gffutils_summary():
    return load_json(GFFUTILS_SUMMARY)


def load_biopython_summary():
    return load_json(BIOPYTHON_SUMMARY)


# --------------------------------------------------------------------------- #
# Figure 1 — software architecture & workflow (schematic)
# --------------------------------------------------------------------------- #

def make_figure1():
    """Pipeline schematic: import -> validate -> model -> browse/query ->
    seven research modules -> export/project. Representative schematic."""
    fig, ax = plt.subplots(figsize=(10.5, 4.6))
    ax.set_xlim(0, 100)
    ax.set_ylim(0, 46)
    ax.axis("off")

    def box(x, y, w, h, text, fc, sub=None, fs=9.5):
        p = FancyBboxPatch(
            (x, y), w, h,
            boxstyle="round,pad=0.35,rounding_size=0.9",
            linewidth=1.1, edgecolor=DARK, facecolor=fc, zorder=2,
        )
        ax.add_patch(p)
        ax.text(x + w / 2, y + h / 2 + (1.4 if sub else 0), text,
                ha="center", va="center", fontsize=fs, weight="bold", zorder=3)
        if sub:
            ax.text(x + w / 2, y + h / 2 - 1.7, sub,
                    ha="center", va="center", fontsize=7.2, color="#555555", zorder=3)

    def arrow(x1, y1, x2, y2):
        a = FancyArrowPatch((x1, y1), (x2, y2), arrowstyle="-|>",
                            mutation_scale=13, linewidth=1.2, color=DARK, zorder=1)
        ax.add_patch(a)

    # Top pipeline row (five stages).
    stages = [
        ("Import", "FASTA · GFF3/GTF ·\nexpression matrix", LIGHT),
        ("Health-check", "validate + JSON/MD/TSV\nreport", LIGHT),
        ("Unified model", "genes · transcripts ·\nfeatures · expression", "#EAF2F8"),
        ("Browse & query", "locus search ·\nstrand-aware sequence", "#EAF2F8"),
        ("Export & project", "FASTA · PNG/SVG/TSV ·\n.phytoscope", LIGHT),
    ]
    x, y, w, h, gap = 1.5, 27, 17.2, 11, 1.4
    centers = []
    for text, sub, fc in stages:
        box(x, y, w, h, text, fc, sub)
        centers.append((x + w / 2, y))
        x += w + gap

    for i in range(len(centers) - 1):
        x1 = centers[i][0] + w / 2 + 0.4
        arrow(x1, y + h / 2, centers[i + 1][0] - w / 2 - 0.4, y + h / 2)

    # Feedback loop: project save/reopen -> back to model.
    ax.annotate(
        "", xy=(centers[2][0] - w / 2 + 0.5, y + h + 1.4),
        xytext=(centers[4][0], y + h + 1.4),
        arrowprops=dict(arrowstyle="-|>", mutation_scale=13, linewidth=1.1,
                        color=GRAY, linestyle="--"),
    )
    ax.text((centers[2][0] + centers[4][0]) / 2, y + h + 3.1,
            "reopen .phytoscope (SHA-256 re-verified)",
            ha="center", va="center", fontsize=7.6, color="#555555")

    # Bottom: seven research modules.
    ax.text(1.5, 14.5, "Research modules (shared pipeline, zero species-specific code)",
            fontsize=9, weight="bold", va="center")
    modules = [
        ("Promoter", "IntervalAnalysisService"),
        ("Sequence QC", "SequenceQualityService"),
        ("Homology", "ProteinHomologyService"),
        ("Domain", "DomainLinkService"),
        ("Expression", "ExpressionDataService\n+ SampleGroupingService"),
        ("Comparison", "ComparisonFigure"),
        ("Project", "ProjectStore"),
    ]
    mw, mh, mgap, mx = 12.9, 8, 0.5, 1.6
    for name, cls in modules:
        p = FancyBboxPatch(
            (mx, 2.5), mw, mh,
            boxstyle="round,pad=0.3,rounding_size=0.7",
            linewidth=1.0, edgecolor=PURPLE, facecolor="#F2EEF9", zorder=2,
        )
        ax.add_patch(p)
        ax.text(mx + mw / 2, 2.5 + mh / 2 + 1.35, name,
                ha="center", va="center", fontsize=8.8, weight="bold",
                color=PURPLE, zorder=3)
        ax.text(mx + mw / 2, 2.5 + mh / 2 - 1.35, cls,
                ha="center", va="center", fontsize=5.1, family="monospace",
                color="#555555", zorder=3)
        mx += mw + mgap

    ax.text(50, 0.4, "schematic (representative workflow; not a measured pipeline)",
            ha="center", va="center", fontsize=7.2, color=GRAY, style="italic")
    fig.text(0.5, 0.995,
             "PhytoScope: one local pipeline from import to export",
             ha="center", fontsize=11, weight="bold")
    return fig


# --------------------------------------------------------------------------- #
# Figure 2 — unified data model & input-format compatibility
# --------------------------------------------------------------------------- #

def make_figure2():
    """(a) three inputs -> one unified model; (b) coordinate/strand/missing
    conventions; (c) real rice-bean locus strip showing the GFF3-vs-GTF
    terminal-stop-codon boundary. Mix of schematic + real data."""
    vigna = load_vigna_manifest()
    loc = vigna["validated_locus"]

    fig = plt.figure(figsize=(10.5, 6.0))
    gs = fig.add_gridspec(2, 2, height_ratios=[1.0, 1.25], width_ratios=[1.15, 1.0],
                          left=0.06, right=0.97, top=0.88, bottom=0.07,
                          hspace=0.38, wspace=0.16)
    axA = fig.add_subplot(gs[0, 0])   # unified model
    axB = fig.add_subplot(gs[0, 1])   # conventions table
    axC = fig.add_subplot(gs[1, :])   # locus strip

    # ---- (a) unified model (horizontal: inputs -> one model) --------------
    axA.axis("off")
    axA.set_title("(a) One unified data model from three input formats",
                  fontsize=9.5, weight="bold", loc="left")

    # Right: unified model box.
    mx, my, mw, mh = 0.55, 0.10, 0.43, 0.74
    axA.add_patch(FancyBboxPatch((mx, my), mw, mh, boxstyle="round,pad=0.03",
                                 linewidth=1.1, edgecolor=BLUE, facecolor="#EAF2F8"))
    axA.text(mx + mw / 2, my + mh - 0.13, "Unified in-memory\nmodel",
             ha="center", va="top", fontsize=9, weight="bold")
    axA.text(mx + mw / 2, my + mh / 2 - 0.12,
             "SequenceRegion · Gene ·\nTranscript · GenomicFeature ·\nExpressionProfile",
             ha="center", va="center", fontsize=7.2, color="#333333")

    inputs = [
        ("FASTA", "sequence records (id, length)"),
        ("GFF3 / GTF", "gene → transcript → exon/CDS/UTR"),
        ("Expression TSV/CSV", "sample × feature matrix (+unit/level)"),
    ]
    ix, iw, ih = 0.0, 0.46, 0.18
    for i, (title, sub) in enumerate(inputs):
        y = 0.66 - i * 0.28
        axA.add_patch(FancyBboxPatch((ix, y), iw, ih, boxstyle="round,pad=0.02",
                                     linewidth=1.0, edgecolor=GRAY, facecolor=LIGHT))
        axA.text(ix + iw / 2, y + ih - 0.055, title, ha="center", va="center",
                 fontsize=8.2, weight="bold")
        axA.text(ix + iw / 2, y + ih / 2 - 0.055, sub, ha="center", va="center",
                 fontsize=7.0, color="#555555")
        axA.annotate("", xy=(mx + 0.01, y + ih / 2), xytext=(ix + iw + 0.02, y + ih / 2),
                     arrowprops=dict(arrowstyle="-|>", mutation_scale=11,
                                     linewidth=1.0, color=GRAY))

    axA.text(mx + mw / 2, my - 0.14, "species-agnostic: IDs/coordinates come from the files",
             ha="center", fontsize=7.2, style="italic", color=GRAY)

    # ---- (b) conventions table --------------------------------------------
    axB.axis("off")
    axB.set_title("(b) Coordinate & value conventions",
                  fontsize=9.5, weight="bold", loc="left")
    rows = [
        ("GFF3", "1-based, closed [start, end]"),
        ("GTF", "1-based, closed (GFF2 via same importer)"),
        ("BED (deferred)", "0-based, half-open [start, end)"),
        ("Strand", "+ / − · minus strand reverse-complemented"),
        ("Expression", "measured 0 ≠ missing value"),
        ("Duplicate IDs", "rejected & reported"),
    ]
    y = 0.96
    for i, (k, v) in enumerate(rows):
        yy = y - i * 0.145
        axB.text(0.02, yy, k, fontsize=7.8, weight="bold", color=BLUE, va="top")
        axB.text(0.02, yy - 0.052, v, fontsize=7.0, va="top", color="#333333")
    axB.text(0.02, 0.06, "BED/bedGraph deferred (v2 target); standard nuclear code only",
             fontsize=6.8, style="italic", color=GRAY)

    # ---- (c) rice-bean locus strip ----------------------------------------
    axC.set_title("(c) Real format boundary — rice bean LOC124833352 "
                  "(NC_142174.1, minus strand)",
                  fontsize=9.5, weight="bold", loc="left")
    start, end = loc["start_1_based_inclusive"], loc["end_1_based_inclusive"]
    exons = loc["exon_intervals"]
    cds = loc["cds_intervals"]

    axC.set_xlim(start - 2500, end + 2500)
    axC.set_ylim(0, 10)
    axC.set_yticks([])
    axC.set_xlabel("genomic position on NC_142174.1 (bp, 1-based)")
    for spine in ("top", "right", "left"):
        axC.spines[spine].set_visible(False)

    # scale bar
    axC.plot([start, start + 2000], [0.6, 0.6], color=DARK, lw=1.2)
    axC.text(start + 1000, 1.0, "2 kb", ha="center", fontsize=7)

    def draw_track(ybase, intervals, color, h, alpha=1.0, fill=True):
        for a, b in intervals:
            if fill:
                axC.add_patch(Rectangle((a, ybase), b - a + 1, h,
                                        facecolor=color, edgecolor="none",
                                        alpha=alpha, zorder=2))
            else:
                axC.add_patch(Rectangle((a, ybase), b - a + 1, h,
                                        facecolor="none", edgecolor=color,
                                        lw=1.1, zorder=2))

    # Gene backbone (intron line) + exons.
    axC.plot([start, end], [5.0, 5.0], color=DARK, lw=1.0, zorder=1)
    draw_track(5.0 - 1.1, exons, "#7FB3D5", 2.2)
    # CDS overlays.
    draw_track(5.0 - 0.55, cds, "#2C7BB6", 1.1)
    # UTR regions (5' at high coords on minus strand, 3' at low coords).
    axC.add_patch(Rectangle((23792, 5.0 - 1.1), 30, 2.2, facecolor=AMBER,
                            edgecolor="none", zorder=2))
    axC.add_patch(Rectangle((16647, 5.0 - 1.1), 47, 2.2, facecolor=AMBER,
                            edgecolor="none", zorder=2))
    axC.text(23821 + 90, 5.0, "5′ UTR (30 nt, inferred)", fontsize=6.8,
             va="center", color="#B5651D")
    axC.text(16647 - 140, 5.0, "3′ UTR (47 nt, inferred)", fontsize=6.8,
             va="center", ha="right", color="#B5651D")

    # strand arrow
    axC.annotate("", xy=(start - 500, 8.6), xytext=(end + 500, 8.6),
                 arrowprops=dict(arrowstyle="-|>", mutation_scale=11,
                                 linewidth=1.1, color=DARK))
    axC.text((start + end) / 2, 9.15, "minus (−) strand: transcription "
             "right → left", ha="center", fontsize=7.4)

    # GFF3 vs GTF stop-codon annotation.
    note = ("GFF3 CDS = 1,983 nt (includes terminal stop codon)  ·  "
            "GTF CDS = 1,980 nt + separate 3-nt stop_codon  ·  "
            "both translate to RefSeq XP_087956893.1 (660 aa)")
    axC.text((start + end) / 2, 1.9, note, ha="center", fontsize=7.6,
             bbox=dict(boxstyle="round,pad=0.4", facecolor="#FFF7E6",
                       edgecolor=ORANGE, linewidth=0.9))

    # legend
    from matplotlib.lines import Line2D
    handles = [
        Rectangle((0, 0), 1, 1, facecolor="#7FB3D5", edgecolor="none", label="exon"),
        Rectangle((0, 0), 1, 1, facecolor="#2C7BB6", edgecolor="none", label="CDS"),
        Rectangle((0, 0), 1, 1, facecolor=AMBER, edgecolor="none", label="inferred UTR"),
    ]
    axC.legend(handles=handles, loc="lower right", fontsize=7, frameon=False,
               ncol=3, bbox_to_anchor=(0.99, -0.02))

    fig.text(0.5, 0.995,
             "Standard data model and input-format compatibility",
             ha="center", fontsize=11, weight="bold")
    return fig


# --------------------------------------------------------------------------- #
# Figure 3 — seven core research modules and their golden-standard agreement
# --------------------------------------------------------------------------- #

def make_figure3():
    vigna = load_vigna_manifest()
    n_genes = vigna["expression"]["gene_rows"]
    n_match = vigna["expression"]["matched_annotation_gene_ids"]

    modules = [
        ("Promoter", "IntervalAnalysisService",
         "8/8 loci (4 plus, 4 minus strand)\n"
         "2,000 bp upstream + 200 bp downstream\n"
         "matched direct FASTA slice + reverse-complement",
         "baseline: bedtools 2.31.1 (CI)", True),
        ("Sequence QC", "SequenceQualityService",
         "4/4 RBCS CDS (411 / 561 / 546 / 546 nt)\n"
         "all in frame · 0 ambiguous bases\n"
         "no QC flags raised",
         "test-side GC / frame / start-stop reference", True),
        ("Homology", "ProteinHomologyService",
         "RBCS (high conservation): 20/20 agreement\n"
         "CYP71A (medium, ~51–60% id): 20/20 agreement\n"
         "12 in-family + 8 controls per family",
         "baseline: BLASTP 2.12.0+ / 2.17.0+", True),
        ("Domain", "DomainLinkService",
         "16/16 domain / deletion records\n"
         "across 13 genes\n"
         "(incl. a multi-exon junction)",
         "independent Python coordinate reference", True),
        ("Expression", "ExpressionDataService\n+ SampleGroupingService",
         "AT1G01860 leaf: mean = median = 16.0 TPM (n = 3)\n"
         "measured zeros preserved (n = 2)\n"
         "rice-bean: %d / %d genes reconciled (0 unmatched)"
         % (n_match, n_genes),
         "Salmon 2.8.0 re-quantification", True),
        ("Comparison", "ComparisonFigure",
         "structure / expression alignment with\n"
         "correct units & shared axes\n"
         "(regression-tested layout)",
         "no numeric gold — representative", False),
        ("Project", "ProjectStore",
         "manifest v2 (schemaVersion 4)\n"
         "schema 1–4 backward-compatible\n"
         "SHA-256 provenance · save / reopen round-trip",
         "checksum-mismatch abort regression", True),
    ]

    fig, axes = plt.subplots(4, 2, figsize=(10.5, 7.4))
    axes = axes.ravel()
    for ax, (title, cls, result, baseline, measured) in zip(axes, modules):
        ax.axis("off")
        colour = GREEN if measured else GRAY
        ax.add_patch(FancyBboxPatch((0.0, 0.04), 0.98, 0.90,
                                    boxstyle="round,pad=0.02,rounding_size=0.04",
                                    linewidth=1.1, edgecolor=colour,
                                    facecolor="#F4FBF4" if measured else "#F7F7F7"))
        ax.text(0.03, 0.89, title, fontsize=10, weight="bold", va="top")
        ax.text(0.03, 0.76, cls, fontsize=6.2, family="monospace",
                color="#555555", va="top")
        ax.text(0.03, 0.47, result, fontsize=7.8, va="center", color="#222222")
        ax.text(0.03, 0.10, baseline, fontsize=6.8, color="#555555",
                va="top", style="italic")
        tag = "measured" if measured else "representative"
        ax.text(0.97, 0.89, tag, fontsize=6.6, ha="right", va="top",
                color=colour, weight="bold")

    # Empty 8th cell -> summary note (framed for visual consistency).
    ax = axes[-1]
    ax.axis("off")
    ax.add_patch(FancyBboxPatch((0.0, 0.04), 0.98, 0.90,
                                boxstyle="round,pad=0.02,rounding_size=0.04",
                                linewidth=1.1, edgecolor=GRAY,
                                facecolor="#F7F7F7"))
    ax.text(0.05, 0.85,
            "All seven modules share the\nsame imported model; each is\n"
            "judged on correctness against\nan independent baseline, not on\nalgorithm novelty.",
            fontsize=8.2, va="top", color="#222222")
    ax.text(0.05, 0.16,
            "Comparison = regression-tested layout\n(no numeric gold).",
            fontsize=7.2, va="top", style="italic", color="#555555")

    fig.text(0.5, 0.995,
             "Core research modules and their golden-standard agreement",
             ha="center", fontsize=11, weight="bold")
    fig.tight_layout(rect=[0, 0, 1, 0.985])
    return fig


# --------------------------------------------------------------------------- #
# Figure 4 — multi-species validation & correctness
# --------------------------------------------------------------------------- #

def make_figure4():
    vigna = load_vigna_manifest()
    extra = load_additional_manifest()
    gold_rows = load_multilocus_gold()

    # (Transcribed verbatim from docs/chromosome-validation.md "Full public
    # annotation validation" — four Ensembl Plants release 62 imports.)
    model_species = [
        ("Arabidopsis thaliana", "TAIR10", 32833, 791564),
        ("Oryza sativa", "IRGSP-1.0", 38993, 551530),
        ("Zea mays", "Zm-B73-NAM-5.0", 44303, 1162376),
        ("Glycine max", "Glycine_max_v2.1", 57147, 1328989),
    ]

    # Non-model gene/pseudogene counts (RefSeq).
    nonmodel = [
        ("Vigna umbellata", "GCF_029865625.1",
         vigna["independent_source_inventory"]["gff3_gene_and_pseudogene_features"],
         vigna["independent_source_inventory"]["gff3_feature_rows"]),
        ("Andrographis paniculata", "GCF_009805555.1",
         extra["datasets"]["andrographis-paniculata"]["gene_count"], None),
        ("Hibiscus syriacus", "GCF_006381635.1",
         extra["datasets"]["hibiscus-syriacus"]["gene_count"], None),
    ]

    # 25 coding transcripts summary.
    by_species = {}
    for r in gold_rows:
        by_species.setdefault(r["species"], set()).add(r["transcript_id"])
    loci = set((r["species"], r["gene_id"]) for r in gold_rows)
    n_transcripts = sum(len(v) for v in by_species.values())

    fig = plt.figure(figsize=(10.5, 6.2))
    gs = fig.add_gridspec(1, 3, width_ratios=[1.35, 1.0, 1.0],
                          left=0.06, right=0.97, top=0.85, bottom=0.16, wspace=0.32)
    axA = fig.add_subplot(gs[0, 0])
    axB = fig.add_subplot(gs[0, 1])
    axC = fig.add_subplot(gs[0, 2])

    # ---- (a) import scale (genes + features) ------------------------------
    species = [m[0] for m in model_species] + [n[0] for n in nonmodel]
    genes = [m[2] for m in model_species] + [n[2] for n in nonmodel]
    colors = [BLUE] * len(model_species) + [ORANGE] * len(nonmodel)

    y = range(len(species))[::-1]
    axA.barh(y, genes, color=colors, edgecolor="none", height=0.62)
    axA.set_yticks(list(y))
    axA.set_yticklabels(species, fontsize=7.8)
    axA.set_xlabel("imported gene-like loci / gene records", fontsize=8)
    axA.tick_params(axis="x", labelsize=7.5)
    for yi, g in zip(y, genes):
        axA.text(g + 800, yi, f"{g:,}", va="center", fontsize=7, color=DARK)
    axA.spines["top"].set_visible(False)
    axA.spines["right"].set_visible(False)
    axA.set_title("(a) Imported gene counts\n(7 assemblies)", fontsize=9.5,
                  weight="bold", loc="left")
    axA.legend(handles=[Patch(facecolor=BLUE, label="model (Ensembl r62)"),
                        Patch(facecolor=ORANGE, label="non-model (RefSeq)")],
               fontsize=7, loc="upper right", frameon=False)

    # ---- (b) CDS length + protein aa for 3 full-gold loci -----------------
    gold_loci = [
        ("V. umbellata", 1983, 660),
        ("A. paniculata", 3372, 1123),
        ("H. syriacus", 624, 207),
    ]
    names = [g[0] for g in gold_loci]
    cds_len = [g[1] for g in gold_loci]
    aa = [g[2] for g in gold_loci]

    yy = np.arange(len(names))[::-1]
    axB.barh(yy - 0.18, cds_len, height=0.34, color=TEAL, label="CDS length (nt)")
    axB.barh(yy + 0.18, aa, height=0.34, color=PURPLE, label="protein (aa)")
    axB.set_yticks(list(yy))
    axB.set_yticklabels(names, fontsize=7.6)
    axB.set_xlabel("nucleotides / amino acids", fontsize=8)
    axB.tick_params(axis="x", labelsize=7.5)
    for yi, c, a in zip(yy, cds_len, aa):
        axB.text(c + 60, yi - 0.18, f"{c:,}", va="center", fontsize=7, color=TEAL)
        axB.text(a + 40, yi + 0.18, f"{a:,}", va="center", fontsize=7, color=PURPLE)
    axB.spines["top"].set_visible(False)
    axB.spines["right"].set_visible(False)
    axB.set_title("(b) Full-gold loci\n(CDS nt vs protein aa)",
                  fontsize=9.5, weight="bold", loc="left")
    axB.legend(fontsize=7, frameon=False, loc="lower right")

    # ---- (c) byte-exact agreement grid (products × species) ---------------
    axC.axis("off")
    axC.set_xlim(-0.20, 1.04)
    axC.set_ylim(-0.04, 1.08)
    axC.set_title("(c) Byte-exact agreement\n(%d coding transcripts, 5 loci × 3 species)"
                  % n_transcripts, fontsize=9.5, weight="bold", loc="left")
    products = ["span", "transcript", "CDS", "protein"]
    species = ["Vigna", "Andrographis", "Hibiscus"]
    col_x = [0.30, 0.56, 0.84]
    for i, s in enumerate(species):
        axC.text(col_x[i], 0.90, s, ha="center", va="center",
                 fontsize=5.6, weight="bold")
    for j, prod in enumerate(products):
        yy = 0.68 - j * 0.20
        axC.text(0.12, yy, prod, ha="right", va="center", fontsize=7.0)
        for i in range(3):
            axC.add_patch(Rectangle((col_x[i] - 0.09, yy - 0.06), 0.18, 0.12,
                                    facecolor=GREEN, edgecolor="none"))
            axC.text(col_x[i], yy, "MATCH", ha="center", va="center",
                     fontsize=5.2, color="white", weight="bold")
    axC.text(0.12, 0.02, "MATCH = byte-for-byte equal to independent gold /\n"
             "RefSeq protein (span = genomic span); all 3 loci × 4 products.",
             fontsize=6.4, color="#333333", va="top")

    fig.text(0.5, 0.975, "Multi-species data validation and correctness",
             ha="center", fontsize=11, weight="bold")
    fig.text(0.5, 0.015,
             "Gene counts are per-assembly reported loci: model = Ensembl r62 gene-like "
             "loci (incl. ncRNA/pseudogene); non-model = RefSeq gene/pseudogene records — "
             "different quantities, shown for import-scale context only.\n"
             "Full-gold loci: LOC124833352 (V. umbellata, −, 3 exons) · "
             "LOC127243311 (A. paniculata, +, 11 exons) · "
             "LOC120212642 (H. syriacus, −, 2 exons).",
             ha="center", fontsize=6.8, color=GRAY, style="italic")
    return fig


# --------------------------------------------------------------------------- #
# Figure 5 — fair comparison with existing tools (honest)
# --------------------------------------------------------------------------- #

def make_figureS1():
    """Supplementary figure: fair comparison with existing tools
    (gffutils/Biopython measured; IGV/JBrowse2/TBtools qualitative)."""
    gff = load_gffutils_summary()
    bio = load_biopython_summary()

    fig = plt.figure(figsize=(10.5, 7.2))
    gs = fig.add_gridspec(2, 2, height_ratios=[1.0, 1.15], width_ratios=[1.2, 1.0],
                          left=0.06, right=0.97, top=0.86, bottom=0.06,
                          hspace=0.42, wspace=0.22)
    axA = fig.add_subplot(gs[0, :])   # gffutils
    axB = fig.add_subplot(gs[1, 0])   # biopython
    axC = fig.add_subplot(gs[1, 1])   # qualitative matrix

    # ---- (a) gffutils vs PhytoScope ---------------------------------------
    axA.axis("off")
    axA.set_title("(a) gffutils 0.13 vs PhytoScope — same frozen rice-bean input",
                  fontsize=9.5, weight="bold", loc="left")

    trial_count = len(gff["trials"])
    tuple_count = gff["source_gold_tuple_count"]
    agree = all(t["exact_tuple_agreement_with_each_other_and_source"]
                for t in gff["trials"])

    # Correctness headline (the primary result — not a speed claim).
    axA.text(0.0, 0.78, "Shared-task correctness (primary result):",
             fontsize=9, weight="bold")
    axA.text(0.0, 0.60,
             "PhytoScope == gffutils == independent source-derived tuples",
             fontsize=9.5, color=GREEN, weight="bold")
    axA.text(0.0, 0.44,
             "%d/%d structure tuples identical · %d/%d alternating-order trials"
             % (tuple_count, tuple_count, trial_count, trial_count),
             fontsize=9)

    # Timing reported separately, prominently annotated.
    p_imp = gff["median_phytoscope_full_import_ms"]
    g_idx = gff["median_gffutils_full_index_build_ms"]
    p_q = gff["trials"][0]["phytoscope"]["selected_gene_search_median_ms"]
    g_q = gff["trials"][0]["gffutils"]["selected_gene_query_100_median_ms"]

    timing = (
        "Cold setup (reported separately — different storage designs):\n"
        "  PhytoScope in-memory import    %6.0f ms   (3 alternating trials)\n"
        "  gffutils on-disk SQLite index  %6.0f ms   (3 alternating trials)\n"
        "Frozen-locus lookup (100 queries, median):\n"
        "  PhytoScope %4.2f ms · gffutils %4.2f ms"
        % (p_imp, g_idx, p_q, g_q)
    )
    axA.text(0.0, 0.12, timing, fontsize=8.2, family="monospace",
             va="top",
             bbox=dict(boxstyle="round,pad=0.45", facecolor="#FFF7E6",
                       edgecolor=ORANGE, linewidth=0.9))
    axA.text(0.66, 0.02,
             "No speed/usability superiority is claimed: the two tools use "
             "in-memory vs\non-disk indexes, so cold import and index-build "
             "times are reported\nseparately (source summary.json interpretation).",
             fontsize=7.2, color="#555555", style="italic", va="bottom")

    # ---- (b) Biopython SeqIO ----------------------------------------------
    axB.axis("off")
    axB.set_xlim(-0.02, 1.04)
    axB.set_ylim(-0.04, 1.08)
    axB.set_title("(b) Biopython 1.88 SeqIO — byte-exact vs frozen gold",
                  fontsize=9.5, weight="bold", loc="left")
    cases = bio["cases"]
    products = ["span", "transcript", "CDS", "protein", "5′ UTR", "3′ UTR"]
    species_short = {
        "vigna-umbellata": "Vigna",
        "andrographis-paniculata": "Andrographis",
        "hibiscus-syriacus": "Hibiscus",
    }
    col_x = [0.34 + i * 0.116 for i in range(6)]  # 0.34 .. 0.92
    for i, prod in enumerate(products):
        axB.text(col_x[i], 0.985, prod, ha="center", va="center",
                 fontsize=6.0, weight="bold")
    for j, case in enumerate(cases):
        yy = 0.76 - j * 0.27
        axB.text(0.02, yy, species_short[case["species"]], ha="left",
                 va="center", fontsize=7.8, weight="bold")
        axB.text(0.02, yy - 0.085,
                 case["gene_id"].replace("gene-", "") +
                 " (%s, %d exons)" % (case["strand"], case["exon_count"]),
                 fontsize=6.0, va="center", color="#555555")
        for i, prod in enumerate(products):
            ok = case["products"][
                "genomic_span" if prod == "span" else
                "transcript" if prod == "transcript" else
                "CDS" if prod == "CDS" else
                "protein" if prod == "protein" else
                "five_prime_UTR_inferred" if prod == "5′ UTR" else
                "three_prime_UTR_inferred"
            ]["status"] == "MATCH"
            axB.add_patch(Rectangle((col_x[i] - 0.05, yy - 0.055), 0.10, 0.11,
                                    facecolor=GREEN if ok else GRAY,
                                    edgecolor="none"))
            axB.text(col_x[i], yy, "✓" if ok else "·", ha="center",
                     va="center", fontsize=7.5, color="white", weight="bold")
    axB.text(0.02, 0.0,
             "All three loci (1 plus, 2 minus strand): every product "
             "byte-exact\n(%s; all_gold_products_match = true)"
             % bio["biopython_version"],
             fontsize=6.8, color="#333333", va="top")

    # ---- (c) qualitative capability matrix --------------------------------
    axC.axis("off")
    axC.set_xlim(-0.02, 1.02)
    axC.set_ylim(-0.02, 1.06)
    axC.set_title("(c) Qualitative capability matrix (not measured)",
                  fontsize=9.5, weight="bold", loc="left")

    # From docs/fcs-phase3-comparison-plan.md §4.1 (qualitative, documentation-
    # based). 1 = native, 0.5 = needs config/external tool, 0 = not native.
    tasks = [
        ("Locus lookup", [1, 1, 1, 0.5]),
        ("Isoform structure", [1, 1, 1, 1]),
        ("Linked expression", [1, 0.5, 0.5, 1]),
        ("Comparison figure", [1, 0.5, 0.5, 0.5]),
        ("Restore analysis", [1, 1, 1, 0.5]),
        ("Portal-free import", [1, 1, 1, 1]),
        ("Reproducible export", [1, 0.5, 0.5, 0.5]),
    ]
    tools = ["PhytoScope", "IGV", "JBrowse2", "TBtools"]
    col_x = [0.42, 0.60, 0.78, 0.96]
    for i, t in enumerate(tools):
        axC.text(col_x[i], 0.94, t, ha="center", fontsize=7.6, weight="bold")
    for j, (task, vals) in enumerate(tasks):
        yy = 0.80 - j * 0.115
        axC.text(0.0, yy, task, ha="left", va="center", fontsize=6.6)
        for i, v in enumerate(vals):
            if v == 1:
                sym, col = "✓", GREEN
            elif v == 0.5:
                sym, col = "△", AMBER
            else:
                sym, col = "—", GRAY
            axC.text(col_x[i], yy, sym, ha="center", va="center", fontsize=8.5,
                     color=col, weight="bold")
    axC.text(0.0, 0.02,
             "✓ native · △ needs config / external tool · — not native\n"
             "Qualitative, documentation-based (§4.1) — no timings, no\n"
             "usability / time-saving / superiority claim. Tasks abbreviated;\n"
             "see Supplementary Table 3.",
             fontsize=6.6, color="#333333", va="top")

    fig.text(0.5, 0.975,
             "Supplementary figure: fair comparison with existing tools — "
             "shared-task agreement and design differences",
             ha="center", fontsize=11, weight="bold")
    return fig


# --------------------------------------------------------------------------- #
# Figure 5 — reproducibility (provenance + self-contained projects)
# --------------------------------------------------------------------------- #

def make_figure5():
    """(a) manifest v2 schema (schematic); (b) save -> tamper -> checksum-abort
    -> reopen round-trip (schematic); (c) independent reference-data repository
    (real: 9 files / 68,518,847 B / dual mirror / sha256sum -c 9/9 OK)."""
    fig = plt.figure(figsize=(10.5, 6.6))
    gs = fig.add_gridspec(2, 2, height_ratios=[1.0, 1.15], width_ratios=[1.0, 1.0],
                          left=0.06, right=0.97, top=0.86, bottom=0.06,
                          hspace=0.42, wspace=0.18)
    axA = fig.add_subplot(gs[0, 0])   # manifest schema
    axB = fig.add_subplot(gs[0, 1])   # round-trip
    axC = fig.add_subplot(gs[1, :])   # reference-data

    # ---- (a) manifest schema ----------------------------------------------
    axA.axis("off")
    axA.set_xlim(0, 1)
    axA.set_ylim(0, 1)
    axA.set_title("(a) Manifest v2 (schemaVersion 4)", fontsize=9.5,
                  weight="bold", loc="left")
    axA.add_patch(FancyBboxPatch((0.02, 0.06), 0.96, 0.86,
                                 boxstyle="round,pad=0.02,rounding_size=0.03",
                                 linewidth=1.1, edgecolor=BLUE, facecolor="#EAF2F8"))
    axA.text(0.04, 0.86, ".phytoscope project manifest", fontsize=8.6,
             weight="bold", va="top")
    fields = [
        "schemaVersion (4) · applicationVersion",
        "assembly identity",
        "annotation metadata (format / version / source)",
        "per-file integrity — SHA-256 of exact bytes (incl. compression)",
        "user-confirmed sequence-name alias map",
        "provenance — source label + full import report",
    ]
    for i, f in enumerate(fields):
        axA.text(0.04, 0.74 - i * 0.11, "•  " + f, fontsize=7.6, va="top",
                 color="#222222")
    axA.text(0.04, 0.10, "schema 1–4 backward-compatible · missing fields = "
             "\"unknown\" (never invented)", fontsize=6.8, va="top",
             style="italic", color="#555555")

    # ---- (b) round-trip ---------------------------------------------------
    axB.axis("off")
    axB.set_xlim(0, 1)
    axB.set_ylim(-0.08, 1)
    axB.set_title("(b) Self-contained project round-trip", fontsize=9.5,
                  weight="bold", loc="left")

    def flow_box(x, y, w, h, text, fc, sub=None):
        axB.add_patch(FancyBboxPatch((x, y), w, h,
                                     boxstyle="round,pad=0.02,rounding_size=0.03",
                                     linewidth=1.0, edgecolor=DARK, facecolor=fc))
        axB.text(x + w / 2, y + h / 2 + (0.055 if sub else 0), text,
                 ha="center", va="center", fontsize=7.8, weight="bold")
        if sub:
            axB.text(x + w / 2, y + h / 2 - 0.06, sub, ha="center", va="center",
                     fontsize=6.2, color="#555555")

    def flow_arrow(x1, y1, x2, y2, label=None, colour=DARK):
        axB.annotate("", xy=(x2, y2), xytext=(x1, y1),
                     arrowprops=dict(arrowstyle="-|>", mutation_scale=12,
                                     linewidth=1.1, color=colour))
        if label:
            axB.text((x1 + x2) / 2, (y1 + y2) / 2 + 0.03, label, ha="center",
                     va="bottom", fontsize=6.0, color=colour, weight="bold")

    # Left vertical chain: save -> reopen -> verify.
    flow_box(0.02, 0.74, 0.40, 0.20, "save .phytoscope", "#F4FBF4",
             "SHA-256 per file")
    flow_box(0.02, 0.46, 0.40, 0.20, "reopen project", "#EAF2F8")
    flow_box(0.02, 0.14, 0.40, 0.20, "verify stored hash", "#FFF7E6")
    flow_arrow(0.22, 0.74, 0.22, 0.66)
    flow_arrow(0.22, 0.46, 0.22, 0.34)

    # Right outcomes.
    flow_box(0.56, 0.56, 0.42, 0.32, "restore state", "#F4FBF4",
             "structure · expression ·\nview state · parameters")
    flow_box(0.56, 0.08, 0.42, 0.20, "\u201cchecksum mismatch\u201d → abort",
             "#FDE0E0", "explicit user choice")

    # Branch arrows.
    flow_arrow(0.42, 0.28, 0.56, 0.60, "match", GREEN)
    flow_arrow(0.42, 0.20, 0.56, 0.18, "differs", "#C0392B")

    axB.text(0.5, -0.055, "implementation: project/ProjectStore.java",
             ha="center", va="center", fontsize=6.2, family="monospace",
             color="#555555")

    # ---- (c) reference-data repository ------------------------------------
    axC.axis("off")
    axC.set_xlim(0, 1)
    axC.set_ylim(0, 1)
    axC.set_title("(c) Independent reference-data repository (dual mirror)",
                  fontsize=9.5, weight="bold", loc="left")

    # mirror boxes
    axC.add_patch(FancyBboxPatch((0.02, 0.82), 0.12, 0.13,
                                 boxstyle="round,pad=0.02", linewidth=1.0,
                                 edgecolor=PURPLE, facecolor="#F2EEF9"))
    axC.text(0.08, 0.885, "gitee", ha="center", va="center",
             fontsize=7.4, weight="bold", color=PURPLE)
    axC.add_patch(FancyBboxPatch((0.17, 0.82), 0.12, 0.13,
                                 boxstyle="round,pad=0.02", linewidth=1.0,
                                 edgecolor=PURPLE, facecolor="#F2EEF9"))
    axC.text(0.23, 0.885, "github", ha="center", va="center",
             fontsize=7.4, weight="bold", color=PURPLE)
    axC.text(0.145, 0.885, "⇄", ha="center", va="center", fontsize=9,
             color=GRAY)
    axC.text(0.02, 0.77, "commit acac2cf", fontsize=6.0, color="#555555",
             va="top")

    # 9-file summary + badge
    axC.add_patch(FancyBboxPatch((0.34, 0.80), 0.64, 0.17,
                                 boxstyle="round,pad=0.02", linewidth=1.0,
                                 edgecolor=GREEN, facecolor="#F4FBF4"))
    axC.text(0.66, 0.915, "9 files · 68,518,847 B", ha="center", va="center",
             fontsize=7.6, weight="bold", color="#1B7A3D")
    axC.text(0.66, 0.845, "$ sha256sum -c SHA256SUMS.txt  →  9/9 OK  ✓",
             ha="center", va="center", fontsize=6.6, family="monospace",
             color="#1B7A3D")

    # 9-file list (real byte sizes from docs/design-slim-bundled-data.md §2.1).
    files = [
        ("TAIR10 genome FASTA (gz)", 36462703),
        ("Araport11 Chr1 GFF3 (gz)", 4491636),
        ("Araport11 Chr2 GFF3 (gz)", 2755680),
        ("Araport11 Chr3 GFF3 (gz)", 3382640),
        ("Araport11 Chr4 GFF3 (gz)", 2620794),
        ("Araport11 Chr5 GFF3 (gz)", 3820663),
        ("horsemint GFF3 (gz)", 10744463),
        ("horsemint 4-leaf TPM matrix", 3394837),
        ("E-MTAB-7978 expression matrix", 845431),
    ]
    axC.text(0.02, 0.72, "MANIFEST.json: per-file bytes · SHA-256 · source URL · "
             "release · license", fontsize=6.8, color="#555555", va="top")
    col_x = [0.02, 0.36, 0.70]
    for i, (name, nbytes) in enumerate(files):
        row = i % 3
        col = i // 3
        xx = col_x[col]
        yy = 0.55 - row * 0.17
        axC.text(xx, yy, name, fontsize=5.8, va="top", color="#222222")
        axC.text(xx + 0.32, yy, f"{nbytes/1e6:,.2f} MB", fontsize=5.8,
                 va="top", color="#555555", ha="right")
    axC.text(0.02, 0.02,
             "LICENSES.md: 1 file confirmed CC BY 4.0 (horsemint) · 8 pending "
             "author confirmation (incl. one mint TPM matrix with unresolved source)",
             fontsize=6.8, color="#555555", style="italic", va="top")

    fig.text(0.5, 0.975,
             "Reproducibility: provenance and self-contained projects",
             ha="center", fontsize=11, weight="bold")
    return fig


# --------------------------------------------------------------------------- #
# Main
# --------------------------------------------------------------------------- #

def main():
    print("Rendering PhytoScope paper figures into", os.path.relpath(OUT, REPO))
    save(make_figure1(), "Figure1")
    save(make_figure2(), "Figure2")
    save(make_figure3(), "Figure3")
    save(make_figure4(), "Figure4")
    save(make_figure5(), "Figure5")      # reproducibility (new)
    save(make_figureS1(), "FigureS1")    # tool comparison -> supplementary
    print("Done. Figures:", [f for f in sorted(os.listdir(OUT))])


if __name__ == "__main__":
    main()
