"""Build an editable evidence-based PhytoScope Figure 1.

Geometry and statistics are loaded from repository evidence. Framework symbols
are schematic; locus coordinates and example TPM values are source-derived.
Requires Python 3 and Inkscape for PDF/PNG rendering. No external artwork.
"""
from pathlib import Path
import csv, json, math, html, subprocess
from PIL import Image

OUT=Path(__file__).resolve().parent
ROOT=OUT.parents[2]
FIX=ROOT/'src/test/resources/non-model'
manifest=json.loads((FIX/'vigna-umbellata/manifest.json').read_text())
locus=manifest['validated_locus']
gold=list(csv.DictReader((FIX/'multi-locus-sequence-gold.tsv').open(),delimiter='\t'))
with (FIX/'vigna-umbellata/public-expression.tsv').open() as f:
    matrix=list(csv.DictReader((l for l in f if not l.startswith('#')),delimiter='\t'))
expr=next(r for r in matrix if r['GeneID']=='gene-LOC148191553')
samples=[k for k in expr if k!='GeneID']
tpm=[float(expr[k]) for k in samples]
seqs={}; key=None
for line in (FIX/'vigna-umbellata/gold-LOC124833352.fasta').read_text().splitlines():
    if line.startswith('>'): key=line[1:];seqs[key]=''
    else: seqs[key]+=line
inventory_sources=[('Arabidopsis · bundled','arabidopsis'),('Arabidopsis · full','arabidopsis-full'),('Maize · excerpt','maize'),('Maize · full','maize-full'),('Rice · excerpt','rice'),('Rice · full','rice-full'),('Soybean · excerpt','soybean'),('Soybean · full','soybean-full')]
inventories=[]
for name,folder in inventory_sources:
    report=json.loads((ROOT/f'benchmarks/results/2026-10-16/{folder}/structure-validation.json').read_text())
    assert report['missing_records']==report['extra_records']==0
    assert report['expected_records']==report['parsed_records']
    inventories.append((name,report['parsed_records']))
expression_manifest=json.loads((FIX/'vigna-umbellata/public-expression-manifest.json').read_text())
assert expression_manifest['matrix']['matched_annotation_gene_ids']==len(matrix)
assert expression_manifest['matrix']['unmatched_gene_ids']==0
assert sum(n for _,n in inventories)==4641758
assert len(matrix)==34914 and len(gold)==25

N='#20354D'; TEXT='#2F4051'; GRAY='#697786'; LINE='#D6E0E8'
B='#426DA5'; TEAL='#358B83'; PURPLE='#8875B1'; ORANGE='#CC8D47'
parts=[]
def put(s): parts.append(s)
def rect(x,y,w,h,fill='white',stroke=LINE,r=10,sw=1.5,extra=''):
    put(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{r}" fill="{fill}" stroke="{stroke}" stroke-width="{sw}" {extra}/>')
def text(x,y,s,size=19,color=TEXT,bold=False,anchor='start',italic=False,extra=''):
    put(f'<text x="{x}" y="{y}" font-size="{size}" fill="{color}" font-weight="{700 if bold else 400}" text-anchor="{anchor}" font-style="{"italic" if italic else "normal"}" {extra}>{html.escape(str(s))}</text>')
def line(x1,y1,x2,y2,c=LINE,sw=1.6,dash=None,marker=None):
    put(f'<path d="M{x1},{y1} L{x2},{y2}" fill="none" stroke="{c}" stroke-width="{sw}"'+(f' stroke-dasharray="{dash}"' if dash else '')+(f' marker-end="url(#{marker})"' if marker else '')+'/>')
def path(d,c=N,sw=1.8,fill='none',extra=''):
    put(f'<path d="{d}" fill="{fill}" stroke="{c}" stroke-width="{sw}" {extra}/>')
def circle(x,y,r,fill,stroke='none',sw=1.5): put(f'<circle cx="{x}" cy="{y}" r="{r}" fill="{fill}" stroke="{stroke}" stroke-width="{sw}"/>')
def arrow(x1,y1,x2,y2,c=B,sw=2.2):line(x1,y1,x2,y2,c,sw,marker='arrow')
def label(letter,x,y,title):
    text(x,y,letter,25,N,True);text(x+29,y,title,22,N,True)
def check(x,y,c=TEAL):
    circle(x,y,9,'#E8F3EE');path(f'M{x-4},{y} l3,3 l6,-7',c,1.9)
def leafplant(x,y,kind=0,c=TEAL):
    # Botanical symbols indicate plant inputs, not diagnostic species drawings.
    path(f'M{x},{y} Q{x+3},{y-30} {x},{y-66}',c,2.4)
    for dy,side in [(20,-1),(37,1),(53,-1)]:
        xx=x+side*26; yy=y-dy-14
        path(f'M{x},{y-dy} Q{xx},{y-dy+3} {xx},{yy} Q{x+side*5},{yy-2} {x},{y-dy}',c,1.5,fill='#DFEFE6')
    if kind==2:
        for a in range(5):
            th=2*math.pi*a/5;circle(x+math.cos(th)*8,y-69+math.sin(th)*8,6,'#EBCFD8')
        circle(x,y-69,4,ORANGE)
    elif kind==1:
        for dy in [0,9,18]: circle(x+3,y-65-dy,3,'#CBBBD9')
    else:
        path(f'M{x},{y-65} Q{x+17},{y-79} {x+9},{y-91}',c,1.7)
def fileicon(x,y,color,kind):
    path(f'M{x},{y} h36 l13,13 v48 h-49 z',color,1.8,fill='white')
    path(f'M{x+36},{y} v13 h13',color,1.5)
    if kind=='gff':
        for yy in [26,38,50]:
            line(x+8,y+yy,x+40,y+yy,color,1.3)
            rect(x+11+(yy%3)*3,y+yy-3,10,6,color,color,0,.6)
    elif kind=='seq':
        text(x+7,y+31,'ATGC',10,color,True);text(x+7,y+45,'TCGA',10,color,True)
    else:
        for i in range(3):
            for j in range(3):rect(x+8+j*10,y+24+i*9,7,6,["#D8C8EB","#B9D8E5","#D7E8D9"][(i+j)%3],color,0,.4)

put('<svg xmlns="http://www.w3.org/2000/svg" width="180mm" height="136.25mm" viewBox="0 0 1440 1090">')
put('<defs><marker id="arrow" markerWidth="8" markerHeight="8" refX="6.5" refY="4" orient="auto-start-reverse"><path d="M0,0 L8,4 L0,8 z" fill="context-stroke"/></marker></defs>')
put('<style>text{font-family:Arial,"Nimbus Sans",sans-serif} path,line{stroke-linecap:round;stroke-linejoin:round}</style>')
rect(0,0,1440,1090,'white','none',0)
text(34,43,'PhytoScope',34,N,True)
text(246,43,'One local framework for diverse plant genomes',27,N)
text(34,77,'Public files → a shared locus model → inspectable, exportable and recoverable results',20,GRAY)

# a. Diverse inputs
rect(34,108,292,486,'#F3F7FA','#B7CBDC',14)
label('a',50,139,'Species as data')
for x,kind,name in [(89,0,'Crop'),(180,1,'Medicinal'),(270,2,'Ornamental')]:
    leafplant(x,254,kind);text(x,280,name,18,GRAY,anchor='middle')
text(50,309,'Compatible public inputs',19,N,True)
for y,c,title,sub,k in [(327,B,'Annotation','GFF3 / GTF','gff'),(409,TEAL,'Genome sequence','Matching FASTA','seq'),(491,PURPLE,'Expression','Matrix + sample labels','mat')]:
    rect(50,y,260,73,'white',LINE,8)
    fileicon(64,y+6,c,k)
    text(125,y+29,title,20,N,True);text(125,y+54,sub,18,GRAY)
# connecting arrows between panels
arrow(330,350,359,350,B,2.8)

# b. shared model / state
rect(370,108,354,486,'#F1F7F4','#B7D3C3',14)
label('b',386,139,'Shared local model')
rect(391,163,312,39,'#E1EEE5','#A6C9B3',8)
text(547,189,'One import / query code path',19,TEAL,True,'middle')
# hierarchy as schematic database/model
text(547,233,'Sequence IDs and coordinates',18,GRAY,False,'middle')
for x,h in [(483,40),(510,30),(537,40),(564,34),(591,40)]:
    rect(x,244,13,h,'#B2C5D7',B,6,1)
    line(x+2,262,x+11,262,'white',2)
arrow(547,287,547,305,TEAL,1.8)
rect(459,309,176,40,'white',TEAL,7)
text(547,336,'Gene / locus',20,N,True,'middle')
line(547,350,547,363,TEAL,1.7)
line(468,363,626,363,TEAL,1.7)
arrow(468,363,468,376,TEAL,1.7);arrow(626,363,626,376,TEAL,1.7)
for x,name in [(400,'Transcript 1'),(557,'Transcript 2')]:
    rect(x,379,137,38,'white',TEAL,7)
    text(x+68.5,404,name,18,N,False,'middle')
# schematic exon relationships
for x in [409,565]:
    line(x,441,x+119,441,B,1.8)
    for off,w in [(0,28),(48,18),(88,31)]:rect(x+off,433,w,18,'#8BAED0',B,1,1)
text(547,477,'Exon • CDS • UTR relationships',18,N,False,'middle')
text(547,501,'Source features / inference labels',18,GRAY,False,'middle')
# saved state: stylized disk icon and linked source state
rect(391,521,312,53,'white','#AAC8B5',8)
rect(403,532,28,29,'#DFEBE3',TEAL,2,1.3)
rect(409,532,18,8,'white',TEAL,1,1);rect(409,549,18,10,'white',TEAL,1,1)
text(444,544,'Save ↔ reopen project',19,TEAL,True)
text(444,565,'Inputs, locus and results',18,GRAY)
arrow(727,350,753,350,B,2.8)

# c. outputs: source-derived exemplar visualization
rect(766,108,640,486,'#FBFAF6','#D8CCAE',14)
label('c',782,139,'Locus-centered queries and outputs')
rect(786,162,600,180,'white',LINE,9)
text(802,189,'Structure',21,N,True)
text(912,189,'V. umbellata · LOC124833352',19,GRAY,italic=True)
text(802,217,'NC_142174.1  |  chromosome 10  |  minus strand',18,GRAY)
lo,hi=locus['start_1_based_inclusive'],locus['end_1_based_inclusive']
def sx(v):return 813+(v-lo)/(hi-lo)*548
arrow(1361,240,813,240,B,1.6)
line(813,260,1361,260,B,1.4)
for xx in [915,1045,1170,1290]:path(f'M{xx+5},256 l-5,4 l5,4',B,1.2)
for start,end in sorted(locus['exon_intervals']):rect(sx(start),248,max(2,sx(end)-sx(start)),25,'#F5DFB8',ORANGE,1,1)
for start,end in sorted(locus['cds_intervals']):rect(sx(start),248,max(2,sx(end)-sx(start)),25,B,B,1,.7)
for v,anchor in [(lo,'start'),(20000,'middle'),(hi,'end')]:
    line(sx(v),280,sx(v),285,GRAY,1);text(sx(v),304,f'{v:,}',18,GRAY,anchor=anchor)
text(802,329,'XM_088100767.1',18,N)
rect(1061,317,18,11,B,B,1);text(1084,329,'CDS',18,GRAY)
rect(1141,317,18,11,'#F5DFB8',ORANGE,1);text(1164,329,'Inferred UTR',18,GRAY)
# sequences export card
rect(786,356,297,218,'white',LINE,9)
text(802,384,'Sequence export',21,N,True)
text(802,409,'LOC124833352',18,GRAY)
for y,name,key,unit in [(439,'Transcript','transcript','nt'),(467,'CDS','CDS','nt'),(495,'Protein','protein','aa')]:
    text(802,y,name,18,N);text(1064,y,f'{len(seqs[key]):,} {unit}',18,B,True,'end')
rect(802,515,264,40,'#EEF4F9','#D5E1EB',5)
text(815,541,'>CDS  '+seqs['CDS'][:14]+'…',18,B,extra='font-family="monospace"')
# real expression plot (not an illustrative heat map)
rect(1097,356,289,218,'white',LINE,9)
text(1113,384,'Expression display',21,N,True)
text(1113,409,'LOC148191553 · gene TPM',18,GRAY)
ybase=535
for tick in [0,15,30]:
    yy=ybase-tick/30*104
    line(1143,yy,1364,yy,'#E5EAF0',1)
    text(1136,yy+5,tick,18,GRAY,anchor='end')
for i,val in enumerate(tpm):
    xx=1162+i*74;hh=val/30*104
    rect(xx,ybase-hh,31,max(1,hh),['#86A9CB','#537FAE','#A7C3D9'][i],'none',1)
    text(xx+15.5,max(425,ybase-hh-7),f'{val:.2f}',18,N,anchor='middle')
    text(xx+15.5,555,f'R{i+1}',18,GRAY,anchor='middle')
text(1240,572,'IC426787 · 5 DPA',18,GRAY,anchor='middle')

# d. Independent evaluation band
line(547,595,547,617,PURPLE,1.8,dash='5 5')
arrow(547,617,547,638,PURPLE,1.8)
text(570,624,'Checked against independent source-derived references',18,PURPLE)
rect(34,646,1372,390,'#FAF9FC','#CBBFDE',14)
label('d',50,678,'Public-data evidence')
text(1378,678,'7 plant species evaluated',20,PURPLE,True,'end')
# first evidence group
rect(50,699,459,315,'white','#DFD8EA',10)
text(67,728,'Structural fidelity',21,N,True)
text(67,763,'4,641,758',30,B,True)
text(248,760,'recognized tuples',18,GRAY)
# 8 inventory bar plot, all counts retained
maxn=max(n for _,n in inventories)
for i,(name,n) in enumerate(inventories):
    yy=791+i*23
    text(67,yy,name,18,GRAY)
    bw=n/maxn*133
    rect(252,yy-12,bw,12,'#9BB9D2' if 'excerpt' in name else B,'none',1)
    text(491,yy,f'{n:,}',18,N,anchor='end')
text(67,994,'8 inventories  ·  0 missing  ·  0 extra',18,TEAL,True)
# second evidence group
rect(525,699,421,315,'white','#DFD8EA',10)
text(542,728,'Sequence reconstruction',21,N,True)
text(542,761,'15 loci',28,TEAL,True)
text(670,760,'25 coding transcripts',18,GRAY)
text(542,800,'Plant',18,GRAY,True)
text(706,800,'tx',18,GRAY,True,'middle')
for xx,lab in [(774,'Span'),(844,'RNA'),(914,'CDS')]:text(xx,800,lab,18,GRAY,True,'middle')
line(542,811,929,811,LINE,1)
rows=[('Vigna','vigna-umbellata'),('Andrographis','andrographis-paniculata'),('Hibiscus','hibiscus-syriacus')]
for i,(lab,species) in enumerate(rows):
    yy=844+i*43
    rr=[r for r in gold if r['species']==species]
    text(542,yy,lab,18,N,italic=True)
    text(706,yy,len(rr),18,GRAY,anchor='middle')
    for xx in [774,844,914]:check(xx,yy-6)
text(542,973,'5 selected coding loci per plant',18,TEAL,True)
text(542,996,'Strand-aware sequence SHA-256 checks',18,GRAY)
# third evidence group
rect(962,699,428,315,'white','#DFD8EA',10)
text(979,728,'Expression ID reconciliation',21,N,True)
text(979,759,'Public reads → exact RefSeq RNA → gene TPM',18,GRAY)
# donut is identifier coverage, never labelled accuracy
circle(1035,831,43,'none','#E3ECE8',11)
circle(1035,831,43,'none',TEAL,11)
text(1035,839,'100%',22,TEAL,True,'middle')
text(1100,815,'34,914 / 34,914',25,TEAL,True)
text(1100,843,'gene IDs matched',18,N)
text(1100,870,'0 unmatched IDs',18,GRAY)
rect(979,901,394,93,'#FCF4E9','#DDC09B',8)
text(994,927,'Reads IC426787 ≠ reference VRB3',18,ORANGE,True)
text(994,954,'Supports import / ID compatibility;',18,GRAY)
text(994,979,'genotype-matched biology is not tested.',18,GRAY)

# provenance footer outside the panels
text(34,1070,'Versioned public inputs',18,N,True)
arrow(257,1064,290,1064,GRAY,1.5)
text(310,1070,'Source manifests + checksums',18,N,True)
arrow(613,1064,647,1064,GRAY,1.5)
text(667,1070,'Independent checks',18,N,True)
arrow(881,1064,915,1064,GRAY,1.5)
text(935,1070,'Reproducible scripts and saved projects',18,N,True)
put('</svg>')
svg=OUT/'figure1-highlights.svg'
svg.write_text('\n'.join(parts)+'\n')
for ext in ['pdf','png']:
    cmd=['inkscape',str(svg),f'--export-type={ext}',f'--export-filename={OUT/f"figure1-highlights.{ext}"}']
    if ext=='png':cmd+=['--export-width=5400']
    subprocess.run(cmd,check=True,capture_output=True)
png=OUT/'figure1-highlights.png'
with Image.open(png) as im: im.save(png,dpi=(762,762),optimize=True)
# Auditable machine-readable plotting data, separate from schematics.
source={'locus_manifest':'src/test/resources/non-model/vigna-umbellata/manifest.json','locus':locus,
        'expression_gene':'gene-LOC148191553','expression_samples':samples,'TPM':tpm,
        'expression_source_manifest':'src/test/resources/non-model/vigna-umbellata/public-expression-manifest.json',
        'expression_runs':[x['run_accession'] for x in expression_manifest['samples']],
        'structural_report_root':'benchmarks/results/2026-10-16',
        'structural_inventories':[{'name':n,'tuples':v} for n,v in inventories],
        'sequence_gold_source':'src/test/resources/non-model/multi-locus-sequence-gold.tsv',
        'sequence_counts':{s:{'loci':len({r['gene_id'] for r in gold if r['species']==s}),
                              'coding_transcripts':len([r for r in gold if r['species']==s])} for _,s in rows},
        'expression_rows':len(matrix),'unmatched_expression_ids':expression_manifest['matrix']['unmatched_gene_ids'],
        'font_note':'Editable SVG requests Arial with Nimbus Sans fallback; PDF embeds the available fallback.',
        'schematics':'Plant symbols, input documents and shared-model hierarchy are conceptual, not source morphology or application screenshots.'}
(OUT/'figure1-source-data.json').write_text(json.dumps(source,indent=2)+'\n')
print('Built editable SVG, vector PDF, high-resolution PNG and figure1-source-data.json')
