"""Build the PhytoScope graphical abstract / main highlights figure.
All numbers and claims are transcribed from the manuscript evidence package.
"""
from pathlib import Path
import matplotlib.pyplot as plt
from matplotlib.patches import FancyBboxPatch, FancyArrowPatch, Circle

OUT = Path(__file__).parent
plt.rcParams.update({"font.family":"DejaVu Sans", "font.size":10,
                     "svg.fonttype":"none", "pdf.fonttype":42})
NAVY="#17324D"; BLUE="#3976A5"; TEAL="#2F8A78"; GOLD="#D49A32"
INK="#253746"; MUTED="#64747E"; PALE="#F2F6F8"; LINE="#D7E1E6"

fig = plt.figure(figsize=(13.2,7.3), facecolor="white")
ax=fig.add_axes([0,0,1,1]); ax.set_xlim(0,13.2); ax.set_ylim(0,7.3); ax.axis('off')

def rounded(x,y,w,h,fc=PALE,ec=LINE,r=.12,lw=1.2):
    p=FancyBboxPatch((x,y),w,h,boxstyle=f"round,pad=0.025,rounding_size={r}",facecolor=fc,edgecolor=ec,linewidth=lw)
    ax.add_patch(p); return p

def txt(x,y,s,size=10,color=INK,weight='normal',ha='left',va='center',**kw):
    ax.text(x,y,s,fontsize=size,color=color,fontweight=weight,ha=ha,va=va,**kw)

def arrow(x1,y1,x2,y2,color=BLUE,lw=1.7):
    ax.add_patch(FancyArrowPatch((x1,y1),(x2,y2),arrowstyle='-|>',mutation_scale=13,color=color,linewidth=lw))

# Header
txt(.55,6.88,"PhytoScope",25,NAVY,'bold')
txt(3.52,6.88,"A portable local query framework for plant genomes",16,INK,'normal')
txt(.57,6.48,"Connect public files to traceable locus-level queries—without requiring a species-specific web database",10.5,MUTED)
ax.plot([.55,12.65],[6.22,6.22],color=LINE,lw=1)

# Three zones
x1,w1=.55,3.55; x2,w2=4.38,4.35; x3,w3=9.00,3.65
y0,h=1.60,4.28
for x,w in [(x1,w1),(x2,w2),(x3,w3)]: rounded(x,y0,w,h,"#FFFFFF",LINE,.12,1.15)
# Zone 1 inputs and local framework
txt(x1+.22,5.56,"1  |  Bring local data",12,NAVY,'bold')
# file cards
for yy,label,sub,c in [(4.82,"Genome annotation","GFF3 / GTF",BLUE),(4.02,"Matching sequence","FASTA",TEAL),(3.22,"Expression + samples","Matrix + metadata",GOLD)]:
    rounded(x1+.23,yy,1.52,.58,"#F7F9FA",c,.08,1.1)
    txt(x1+.36,yy+.37,label,8.3,INK,'bold'); txt(x1+.36,yy+.16,sub,7.8,MUTED)
    arrow(x1+1.78,yy+.29,x1+2.04,3.79,color=c,lw=1.25)
rounded(x1+2.08,3.04,1.20,1.48,"#EAF2F7",NAVY,.12,1.4)
txt(x1+2.68,4.18,"Local",10,NAVY,'bold',ha='center')
txt(x1+2.68,3.91,"locus",10,NAVY,'bold',ha='center')
txt(x1+2.68,3.64,"workspace",10,NAVY,'bold',ha='center')
# small locus glyph
ax.plot([x1+2.30,x1+3.04],[3.32,3.32],color=BLUE,lw=2)
for gx in [x1+2.38,x1+2.64,x1+2.93]: ax.add_patch(plt.Rectangle((gx,3.25),.10,.14,color=TEAL))
txt(x1+.25,2.32,"Search genes · inspect transcript structures",8.5,INK,'normal')
txt(x1+.25,2.04,"Export sequences · display expression",8.5,INK,'normal')
txt(x1+.25,1.77,"Save and reopen a portable project",8.5,INK,'normal')

# Zone 2 tested evidence
txt(x2+.24,5.56,"2  |  Test portability and fidelity",12,NAVY,'bold')
# Big structural metric card
rounded(x2+.22,4.47,w2-.44,.82,"#EAF4F1",TEAL,.1,1.2)
txt(x2+.43,5.00,"4,641,758",21,TEAL,'bold')
txt(x2+2.15,5.01,"recognized structural tuples",9.4,INK,'bold')
txt(x2+2.15,4.73,"8 independent source inventories",8.2,MUTED)
txt(x2+.44,4.60,"0 missing  ·  0 extra",8.8,TEAL,'bold')
# species breadth chips
rounded(x2+.22,3.43,w2-.44,.78,"#F7F9FA",LINE,.1,1)
txt(x2+.40,3.98,"7 plant species evaluated",10.2,NAVY,'bold')
# 7 small species chips, 4 in row / 3 row
species=["Arabidopsis","Rice","Maize","Soybean","Rice bean","Andrographis","Hibiscus"]
positions=[(x2+.40,3.70),(x2+1.36,3.70),(x2+2.17,3.70),(x2+2.96,3.70),(x2+.40,3.50),(x2+1.56,3.50),(x2+2.80,3.50)]
# compact pill labels
for lab,(xx,yy) in zip(species,positions):
    ax.add_patch(Circle((xx,yy),.032,color=TEAL))
    txt(xx+.08,yy,lab,7.2,INK)
# Sequence gold and expression case
rounded(x2+.22,2.45,2.02,.75,"#F4F7FA",BLUE,.1,1.1)
txt(x2+.38,2.96,"Independent sequence checks",8.2,NAVY,'bold')
txt(x2+.38,2.68,"5 coding loci × 3 additional plants",8.0,INK)
rounded(x2+2.42,2.45,1.70,.75,"#FBF6EA",GOLD,.1,1.1)
txt(x2+2.56,2.96,"Expression ID join",8.2,NAVY,'bold')
txt(x2+2.56,2.68,"34,914 / 34,914 IDs",8.0,INK)
txt(x2+.25,1.94,"Evidence supports tested releases and formats—not universal compatibility.",8.2,MUTED)

# Zone 3 scope / interpretation
txt(x3+.22,5.56,"3  |  Interpret within evidence",12,NAVY,'bold')
rounded(x3+.22,4.36,w3-.44,.91,"#FBF6EA",GOLD,.1,1.15)
txt(x3+.40,5.02,"Rice-bean expression case",9.4,NAVY,'bold')
txt(x3+.40,4.73,"Reads: IC426787  |  reference: VRB3",8.3,INK)
txt(x3+.40,4.51,"Import / identifier reconciliation only",8.3,GOLD,'bold')
# Boundary list
for yy,head,detail in [(3.87,"Inputs remain authoritative","No function or orthology inferred"),
                       (3.12,"Missing values stay missing","Annotation completeness bounds outputs"),
                       (2.37,"Claims stay technical","No usability, time-saving, or discovery claim")]:
    rounded(x3+.23,yy-.26,w3-.46,.58,"#F7F9FA",LINE,.08,.9)
    ax.add_patch(Circle((x3+.44,yy),.10,facecolor="#E7F1EF",edgecolor=TEAL,lw=1))
    txt(x3+.44,yy,"✓",8,TEAL,'bold',ha='center')
    txt(x3+.64,yy+.10,head,8.4,NAVY,'bold')
    txt(x3+.64,yy-.12,detail,7.5,MUTED)

# Bottom takeaway banner
rounded(.55,.58,12.10,.69,NAVY,NAVY,.12,1)
txt(.82,.925,"TAKE-HOME",8.5,"#B9D8E7",'bold')
txt(2.02,.925,"A reusable query layer for researchers working with plant species that lack dedicated online gene portals.",11,"white",'bold')
txt(.62,.30,"PhytoScope organizes user-supplied data locally; provenance, reference matching, and annotation completeness define what can be concluded.",8.5,MUTED)

for ext in ["svg","pdf"]:
    fig.savefig(OUT/f"figure1-highlights.{ext}",bbox_inches='tight',facecolor='white')
fig.savefig(OUT/"figure1-highlights.png",bbox_inches='tight',dpi=450,facecolor='white')
plt.close(fig)
print("Wrote figure1-highlights.svg/.pdf/.png")
