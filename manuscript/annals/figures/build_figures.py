"""Build reproducible vector figures for the PhytoScope manuscript.

All plotted values are read from the version-controlled evidence reports cited in
manuscript/annals/manuscript.md and the supplementary information. No inferential
statistics are applied.
"""
from pathlib import Path
import matplotlib.pyplot as plt
from matplotlib.patches import FancyBboxPatch, FancyArrowPatch
import numpy as np

OUT = Path(__file__).parent
plt.rcParams.update({"font.family": "DejaVu Sans", "font.size": 9,
                     "axes.titlesize": 13, "axes.labelsize": 9,
                     "svg.fonttype": "none", "pdf.fonttype": 42})
NAVY = "#17324D"
BLUE = "#2F6B9A"
TEAL = "#3B8C86"
GOLD = "#D89B32"
PALE = "#EDF3F7"
GRAY = "#5E6A71"


def save(fig, stem):
    fig.savefig(OUT / f"{stem}.svg", bbox_inches="tight")
    fig.savefig(OUT / f"{stem}.pdf", bbox_inches="tight")
    fig.savefig(OUT / f"{stem}.png", bbox_inches="tight", dpi=300)
    plt.close(fig)


def box(ax, xy, w, h, label, fc=PALE, ec=BLUE, fs=9):
    x, y = xy
    p = FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.02,rounding_size=0.04",
                       facecolor=fc, edgecolor=ec, linewidth=1.35)
    ax.add_patch(p)
    ax.text(x+w/2, y+h/2, label, ha="center", va="center", color=NAVY,
            fontsize=fs, wrap=True)
    return p

# Figure 1: input-to-project workflow diagram.
fig, ax = plt.subplots(figsize=(10, 4.6))
ax.set(xlim=(0, 10), ylim=(0, 5)); ax.axis("off")
ax.text(5, 4.72, "A local, file-driven plant query workflow", ha="center",
        fontsize=15, weight="bold", color=NAVY)
for y, label in [(3.55, "Annotation\nGFF3 / GTF"), (2.55, "Matching genome\nFASTA"),
                 (1.55, "Expression matrix\n+ sample metadata")]:
    box(ax, (0.25, y), 2.1, 0.68, label, fs=9)
    ax.add_patch(FancyArrowPatch((2.4, y+0.34), (3.15, 2.7), arrowstyle="-|>",
                                 mutation_scale=12, linewidth=1.1, color=GRAY,
                                 connectionstyle="arc3,rad=0"))
box(ax, (3.2, 2.13), 2.15, 1.12, "PhytoScope local\nproject and locus model", fc="#E3EEF5", ec=NAVY, fs=10)
for y, label in [(3.55, "Search and\nstructure view"), (2.55, "Sequence export\nand QC"),
                 (1.55, "Expression display\nand grouping"), (0.55, "Selected locus-linked\nanalysis modules")]:
    ax.add_patch(FancyArrowPatch((5.4, 2.68), (6.05, y+0.34), arrowstyle="-|>",
                                 mutation_scale=12, linewidth=1.1, color=GRAY))
    box(ax, (6.1, y), 2.15, 0.68, label, fc="#F5F7F8", ec=TEAL, fs=9)
ax.add_patch(FancyArrowPatch((8.3, 2.65), (9.0, 2.65), arrowstyle="-|>",
                             mutation_scale=13, linewidth=1.3, color=GRAY))
box(ax, (9.05, 2.12), 0.8, 1.08, "Save /\nreopen", fc="#FFF4DC", ec=GOLD, fs=8)
ax.text(5, 0.12, "User files remain the source of biological content; missing information is not inferred.",
        ha="center", color=GRAY, fontsize=9)
save(fig, "figure1-workflow")

# Figure 2: tested coverage matrix. 1 = tested in this evaluation.
rows = ["A. thaliana", "O. sativa", "Z. mays", "G. max", "V. umbellata",
        "A. paniculata", "H. syriacus"]
cols = ["Full annotation\nimport", "Search / structure", "Independent\nsequence gold",
        "Expression matrix\nvalidation"]
data = np.array([[1,1,1,1],[1,1,0,0],[1,1,0,0],[1,1,0,0],
                 [1,1,1,1],[1,1,1,0],[1,1,1,0]])
fig, ax = plt.subplots(figsize=(8.5, 4.8))
from matplotlib.colors import ListedColormap
ax.imshow(data, cmap=ListedColormap(["#F0F2F3", TEAL]), vmin=0, vmax=1, aspect="auto")
ax.set_xticks(np.arange(len(cols)), cols)
ax.set_yticks(np.arange(len(rows)), rows)
ax.tick_params(length=0, labelcolor=NAVY)
ax.xaxis.tick_top(); ax.tick_params(axis="x", pad=9)
for i in range(len(rows)):
    for j in range(len(cols)):
        ax.text(j, i, "Tested" if data[i,j] else "—", ha="center", va="center",
                color="white" if data[i,j] else GRAY, fontsize=8,
                weight="bold" if data[i,j] else "normal")
for sp in ax.spines.values(): sp.set_visible(False)
ax.set_xticks(np.arange(-.5, len(cols), 1), minor=True)
ax.set_yticks(np.arange(-.5, len(rows), 1), minor=True)
ax.grid(which="minor", color="white", linewidth=2)
ax.set_title("Evaluation coverage by species and data task", loc="left", pad=19,
             color=NAVY, weight="bold")
fig.text(.01, .01, "Sequence gold: five selected coding loci per additional plant; reference species have separate structural audits.",
         fontsize=8, color=GRAY)
fig.tight_layout(rect=(0, .04, 1, .96))
save(fig, "figure2-coverage")

# Figure 3: tuple counts by inventory.
names = ["Arabidopsis\nbundled", "Arabidopsis\nrelease 62", "Maize\nexcerpt", "Maize\nfull",
         "Rice\nexcerpt", "Rice\nfull", "Soybean\nexcerpt", "Soybean\nfull"]
counts = np.array([811827,791557,377,1161691,347,550921,839,1324199])
fig, ax = plt.subplots(figsize=(9, 4.7))
colors = [BLUE, BLUE, "#91B7C9", BLUE, "#91B7C9", BLUE, "#91B7C9", BLUE]
bars = ax.bar(np.arange(len(names)), counts/1e6, color=colors, width=.68)
ax.set_xticks(np.arange(len(names)), names)
ax.set_ylabel("Recognized tuples (millions)")
ax.set_ylim(0, 1.72)
ax.spines[["top","right"]].set_visible(False)
ax.spines[["left","bottom"]].set_color("#AAB4BA")
ax.tick_params(colors=GRAY)
for bar, v in zip(bars, counts):
    ax.text(bar.get_x()+bar.get_width()/2, bar.get_height()+.035,
            f"{v:,}", ha="center", va="bottom", fontsize=7.5, color=NAVY, rotation=0)
ax.set_title("Source-to-model structural tuple agreement", loc="left", color=NAVY, weight="bold")
ax.text(.99, .99, "4,641,758 total\n0 missing · 0 extra", transform=ax.transAxes,
        ha="right", va="top", color=TEAL, fontsize=11, weight="bold",
        bbox=dict(boxstyle="round,pad=.4", facecolor="#EAF4F2", edgecolor=TEAL))
fig.tight_layout()
save(fig, "figure3-annotation-tuples")

# Figure 4: expression pipeline; only run-level assigned-fragment count and result IDs.
run_ids = ["SRR16122599\nrep 1", "SRR16122608\nrep 2", "SRR16122607\nrep 3"]
fragments = np.array([21999811,16000136,23225998]) / 1e6
fig, (ax1, ax2) = plt.subplots(1,2,figsize=(9,4.4), gridspec_kw={"width_ratios":[1.1,1]})
bars=ax1.bar(np.arange(3),fragments,color=[BLUE,TEAL,GOLD],width=.62)
ax1.set_xticks(np.arange(3),run_ids); ax1.set_ylabel("Assigned fragments (millions)")
ax1.set_ylim(0,27); ax1.spines[["top","right"]].set_visible(False)
for b,v in zip(bars,[21999811,16000136,23225998]):
    ax1.text(b.get_x()+b.get_width()/2,b.get_height()+.5,f"{v:,}",ha="center",fontsize=8,color=NAVY)
ax1.set_title("Salmon 2.8.0",loc="left",color=NAVY,weight="bold")
ax2.axis("off")
steps=[("Public paired reads", "IC426787 · 5 days post-anthesis"),
       ("Exact RefSeq transcript set", "GCF_029865625.1 · 54,178 RNAs"),
       ("Gene TPM matrix", "34,914 rows · 0 invalid/missing cells"),
       ("Annotation reconciliation", "34,914 / 34,914 IDs matched")]
ys=[.84,.62,.40,.18]
for i,((head,sub),y) in enumerate(zip(steps,ys)):
    box(ax2,(.06,y-.08),.88,.15,head+"\n"+sub,fc="#EDF3F7" if y!=.18 else "#E6F2EE",ec=BLUE if y!=.18 else TEAL,fs=8)
    if i < len(steps)-1:
        next_y = ys[i+1]
        ax2.annotate("",xy=(.5,next_y+.078),xytext=(.5,y-.082),arrowprops=dict(arrowstyle="-|>",color=GRAY,lw=1))
fig.suptitle("Public rice-bean reads: quantification and identifier reconciliation",x=.03,ha="left",color=NAVY,weight="bold")
fig.text(.03,.015,"Reference cultivar VRB3 differs from source genotype IC426787; this is an import/ID check, not matched-genotype biology.",fontsize=8,color=GRAY)
fig.tight_layout(rect=(0,.06,1,.93))
save(fig,"figure4-expression-reconciliation")

# Figure 5: task-specific outcomes shown as separate cards, not normalized as a score.
checks=[
    ("Promoter sequences", "8 / 8 exact", "Direct slicing + bedtools; 2,200 bp, both strands", TEAL),
    ("Domain coordinates", "16 / 16 matched", "Independent Python reconstruction across 13 genes", BLUE),
    ("Similarity classifications", "20 / 20 agreed", "Small RBCS family panels; not an orthology test", GOLD),
    ("Expression identifiers", "34,914 / 34,914", "Exact RefSeq gene IDs in the rice-bean matrix", TEAL),
]
fig,ax=plt.subplots(figsize=(9,4.8));ax.set(xlim=(0,10),ylim=(0,5));ax.axis("off")
ax.text(0,4.68,"Selected independent module checks",fontsize=14,weight="bold",color=NAVY)
positions=[(.2,2.65),(5.1,2.65),(.2,.75),(5.1,.75)]
for (title,value,detail,color),(x,y) in zip(checks,positions):
    box(ax,(x,y),4.55,1.55,"",fc="#F7F9FA",ec=color)
    ax.text(x+.22,y+1.17,title,ha="left",va="center",fontsize=10,weight="bold",color=NAVY)
    ax.text(x+.22,y+.72,value,ha="left",va="center",fontsize=15,weight="bold",color=color)
    ax.text(x+.22,y+.28,detail,ha="left",va="center",fontsize=8,color=GRAY)
ax.text(.2,.25,"Separate bounded panels; denominators are not comparable accuracy estimates. Protein similarity checks are a one-family smoke test.",fontsize=8,color=GRAY)
save(fig,"figure5-module-checks")

print("Wrote five SVG and PDF figure pairs to", OUT)
