"""A4 portrait Figure 1: Arial 7 pt, black 0.5 pt strokes, 25 mm margins.
Run after build_graphical_abstract.py has generated figure1-source-data.json.
Requires installed Arial Regular, Inkscape, and Pillow. Fonts are not bundled.
"""
from pathlib import Path
import html,json,subprocess,math
from PIL import Image
O=Path(__file__).resolve().parent
D=json.loads((O/'figure1-source-data.json').read_text())
P=[]; FS=7*25.4/72; SW=.5*25.4/72
C='#000000';BLUE='#E8F0F7';GREEN='#EAF2ED';GOLD='#FBF1E3';PURPLE='#F0ECF5'
def add(s):P.append(s)
def text(x,y,s,anchor='start'):
 add(f'<text x="{x}" y="{y}" font-family="Arial" font-size="{FS}" font-weight="400" font-style="normal" fill="#000000" stroke="none" text-anchor="{anchor}">{html.escape(str(s))}</text>')
def box(x,y,w,h,fill='white',r=1):
 add(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{r}" fill="{fill}" stroke="#000000" stroke-width="{SW}"/>')
def path(d):add(f'<path d="{d}" fill="none" stroke="#000000" stroke-width="{SW}" stroke-linecap="round" stroke-linejoin="round"/>')
def line(x,y,xx,yy):path(f'M{x},{y} L{xx},{yy}')
def arrow(points):
 # Shaft and open arrowhead are a single path object, with a continuous join.
 (x,y),(xx,yy)=points[-2:];dx=xx-x;dy=yy-y;a=math.atan2(dy,dx);L=1.25;W=.55
 ax=xx-L*math.cos(a)+W*math.sin(a);ay=yy-L*math.sin(a)-W*math.cos(a)
 bx=xx-L*math.cos(a)-W*math.sin(a);by=yy-L*math.sin(a)+W*math.cos(a)
 d='M'+ ' L'.join(f'{a},{b}' for a,b in points)
 path(d+f' L{ax},{ay} L{xx},{yy} L{bx},{by}')
def tick(x,y):path(f'M{x-0.7},{y} l0.5,0.5 l1.2,-1.4')
add('<svg xmlns="http://www.w3.org/2000/svg" width="210mm" height="297mm" viewBox="0 0 210 297" preserveAspectRatio="xMidYMid meet">')
add('<title>PhytoScope Figure 1 A4 portrait</title>')
add('<desc>All text Arial Regular 7 pt; text and strokes pure black; strokes 0.5 pt; content within 25 mm page margins. All arrows and shafts are single path objects.</desc>')
add('<g id="PhytoScope_Figure1">')
text(25,28,'PhytoScope: a portable local framework for plant genome queries')
text(25,33,'Public data, a shared locus model, and source-derived validation')

# a: inputs
text(25,41,'a  Compatible public inputs')
inputs=[(25,48.5,'Annotation','GFF3 / GTF',BLUE),(81.5,105,'Genome sequence','Matching FASTA',GREEN),(138,161.5,'Expression','Matrix and sample labels',PURPLE)]
for x,c,t,s,f in inputs:
 box(x,45,47,22,f)
 # file glyph
 path(f'M{x+3},49 h4 l1.5,1.5 v10 h-5.5 z M{x+7},49 v1.5 h1.5')
 for yy in [53,55,57]:line(x+4,yy,x+7.5,yy)
 text(x+11,53,t);text(x+11,58,s)
 text(x+11,63,'User-supplied files')
line(48.5,67,48.5,73);line(161.5,67,161.5,73)
line(48.5,73,161.5,73)
arrow([(105,67),(105,80.5)])

# b: model
text(25,80,'b  Shared local model')
box(25,83,160,37,GREEN)
for x,w,t,s in [(31,30,'Sequence IDs','Coordinates'),(69,30,'Gene / locus','Searchable records'),(107,30,'Transcripts','Parent relationships'),(145,33,'Exon / CDS / UTR','Source feature labels')]:
 box(x,90,w,13)
 text(x+w/2,95,t,'middle');text(x+w/2,99.5,s,'middle')
for x,xx in [(61,68),(99,106),(137,144)]:arrow([(x,96.5),(xx,96.5)])
text(31,111,'One import and query code path across species')
text(31,116,'Source features and inferred intervals remain labelled')
box(111,107,67,9,'white')
text(144.5,112.7,'Save / reopen inputs, locus state and results','middle')
arrow([(105,120),(105,127)])

# c: genuine examples
text(25,130,'c  Locus-centered queries and outputs')
box(25,134,160,36,BLUE)
text(30,140,'Structure: V. umbellata / LOC124833352')
text(180,140,'NC_142174.1 / chromosome 10','end')
locus=D['locus'];lo=locus['start_1_based_inclusive'];hi=locus['end_1_based_inclusive']
def sx(v):return 31+(v-lo)/(hi-lo)*148
arrow([(179,146),(31,146)])
line(31,151,179,151)
for a,b in sorted(locus['exon_intervals']):box(sx(a),149,max(.1,sx(b)-sx(a)),4,'#F5DFB8',0)
for a,b in sorted(locus['cds_intervals']):box(sx(a),149,max(.1,sx(b)-sx(a)),4,'#AAC4DE',0)
for v,anchor in [(lo,'start'),(20000,'middle'),(hi,'end')]:
 line(sx(v),155,sx(v),156);text(sx(v),160,f'{v:,}',anchor)
text(30,166,'XM_088100767.1 / minus strand')
box(119,163.3,3,2,'#AAC4DE',0);text(124,165.5,'CDS')
box(140,163.3,3,2,'#F5DFB8',0);text(145,165.5,'Inferred UTR')

box(25,176,77,35,'white')
text(30,182,'Sequence export / LOC124833352')
for y,k,value in [(190,'Transcript','2,060 nt'),(197,'GFF3 CDS','1,983 nt'),(204,'Protein','660 aa')]:
 text(30,y,k);text(97,y,value,'end')
box(108,176,77,35,'white')
text(113,182,'Expression / LOC148191553 / gene TPM')
base=204;top=188;left=122;right=180
for t in [0,15,30]:
 yy=base-t/30*(base-top)
 line(left,yy,right if t==0 else left+1,yy);text(119.5,yy+.7,t,'end')
line(left,top,left,base)
for i,val in enumerate(D['TPM']):
 x=128+i*20;h=val/30*(base-top)
 box(x,base-h,6,h, ['#D8E3EE','#AAC4DE','#C2D5E6'][i],0)
 text(x+3,base-h-1,f'{val:.2f}','middle');text(x+3,208.5,f'R{i+1}','middle')
text(25,216,'Expression example: three individual IC426787 biological replicates, 5 days post-anthesis.')

# d: concise overview evidence; detailed inventory counts remain in Figure 3.
text(25,224,'d  Independent public-data validation / seven plant species evaluated')
for x,fill,rows in [
 (25,BLUE,['Structural fidelity','4,641,758 recognized tuples','8 source inventories','0 missing / 0 extra']),
 (80,GREEN,['Sequence reconstruction','15 loci / 25 coding transcripts','5 coding loci in each of 3 plants','Span, spliced RNA and CDS hashes']),
 (135,PURPLE,['Expression ID reconciliation','34,914 / 34,914 gene IDs','0 unmatched IDs','Pinned RefSeq annotation release'])]:
 box(x,228,50,27,fill)
 for y,s in zip([234,240,246,252],rows):text(x+3,y,s)
text(25,263,'Expression reads: IC426787; reference cultivar: VRB3. This supports import and ID compatibility.')
text(25,268,'Structure and sequence examples use LOC124833352; expression display uses LOC148191553.')
add('</g></svg>')
stem='figure1-portrait-A4'
p=O/f'{stem}.svg';p.write_text('\n'.join(P)+'\n')
for ext in ['pdf','png']:
 cmd=['inkscape',str(p),f'--export-type={ext}',f'--export-filename={O/f"{stem}.{ext}"}']
 if ext=='png':cmd+=['--export-dpi=600']
 subprocess.run(cmd,check=True,capture_output=True)
subprocess.run(['inkscape',str(p),'--export-type=svg','--export-text-to-path','--export-plain-svg',f'--export-filename={O/f"{stem}-outlined.svg"}'],check=True,capture_output=True)
with Image.open(O/f'{stem}.png') as im:im.save(O/f'{stem}.png',dpi=(600,600),optimize=True)
print('Created A4 portrait SVG, outlined SVG, PDF and 600 dpi PNG')
