from pathlib import Path
import json,re,subprocess,html
O=Path(__file__).resolve().parent
if '## Table S1' in (O/'supporting-information.md').read_text():
 s=(O/'supporting-information.md').read_text()
 tables=[]
 for n in range(1,5):
  block=s.split(f'## Table S{n}. ',1)[1] if f'## Table S{n}. ' in s else s.split(f'## Table S{n} ',1)[1]
  title=block.splitlines()[0];rows=[]
  for l in block.splitlines()[1:]:
   if l.startswith('## '):break
   if l.startswith('|'):
    rr=[x.strip() for x in l.strip('|').split('|')]
    if not all(re.fullmatch(':?-+:?',x) for x in rr):rows.append(rr)
  tables.append({'name':f'Table S{n}','title':title,'rows':rows})
 (O/'supporting-tables-data.json').write_text(json.dumps(tables,indent=2))
# A compact vector layout, physical font/stroke dimensions unchanged.
a=['<svg xmlns="http://www.w3.org/2000/svg" width="297mm" height="210mm" viewBox="0 0 297 210">','<rect width="297" height="210" fill="white"/>']
def text(x,y,t,anchor='start'):
 a.append(f'<text x="{x}" y="{y}" text-anchor="{anchor}" font-family="Arial" font-size="2.469444444" font-weight="normal" fill="black">{html.escape(str(t))}</text>')
def rect(x,y,w,h,fill='white',stroke=True):a.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" fill="{fill}" stroke="{"black" if stroke else "none"}" stroke-width="0.176388889"/>')
def line(x,y,X,Y):a.append(f'<path d="M{x},{y} L{X},{Y}" fill="none" stroke="black" stroke-width="0.176388889"/>')
text(25,27,'Figure S1. Public-data evaluation and supporting checks')
text(25,36,'A  Evaluation coverage')
labels=['A. thaliana','O. sativa','Z. mays','G. max','V. umbellata','A. paniculata','H. syriacus']
for j,(h1,h2) in enumerate([('Full','import'),('Search /','structure'),('Sequence','gold'),('Expression','matrix')]):
 x=66+j*19;text(x,43,h1,'middle');text(x,46.5,h2,'middle')
data=[[1,1,1,1],[1,1,0,0],[1,1,0,0],[1,1,0,0],[1,1,1,1],[1,1,1,0],[1,1,1,0]]
for i,l in enumerate(labels):
 y=49+i*6.5;text(25,y+4.2,l)
 for j,v in enumerate(data[i]):
  rect(56.5+j*19,y,19,6.5,'#d8eae7' if v else '#f0f2f3');text(66+j*19,y+4.2,'Tested' if v else '—','middle')
text(25,100,'Sequence gold covers selected loci; “—” indicates not evaluated.')
text(154,36,'B  Structural tuple inventories')
names=['Arabidopsis · bundled','Arabidopsis · full','Maize · excerpt','Maize · full','Rice · excerpt','Rice · full','Soybean · excerpt','Soybean · full']
counts=[811827,791557,377,1161691,347,550921,839,1324199]
for i,(n,c) in enumerate(zip(names,counts)):
 y=43+i*6;text(154,y+3,n);rect(191,y,54*c/1400000,3.7,'#426e9e',False);text(271,y+3,f'{c:,}','end')
text(154,97,'4,641,758 recognized tuples; 0 missing; 0 extra.')
text(154,101,'Bars share a linear scale; counts are feature/relationship records.')
text(25,116,'C  Public ricebean expression and reference reconciliation')
# Bar chart, with exact counts and source run IDs.
for tick in [0,10,20,30]:
 y=156-tick;line(35,y,88,y);text(32,y+1,tick,'end')
for x,v,run in [(43,21999811,'SRR16122599'),(61,16000136,'SRR16122608'),(79,23225998,'SRR16122607')]:
 rect(x-4,156-v/1e6,8,v/1e6,'#426e9e',False);text(x,154-v/1e6,f'{v:,}','middle');text(x,161,run,'middle')
text(35,123,'Assigned fragments (millions)')
for i,lines in enumerate([['IC426787 · 5 DPA','Three paired-read libraries'],['GCF_029865625.1 · VRB3','54,178 RefSeq RNAs'],['Salmon 2.8.0 · gene TPM','34,914 / 34,914 IDs matched']]):
 y=125+i*14;rect(95,y,48,11,'#f3f6f7');text(119,y+4.2,lines[0],'middle');text(119,y+8.2,lines[1],'middle')
 if i<2:a.append(f'<path d="M119,{y+11} V{y+13} l-0.7,-1 M119,{y+13} l0.7,-1" fill="none" stroke="black" stroke-width="0.176388889"/>')
text(25,174,'0 invalid/missing cells; each TPM column sums to 1 million within rounding.')
text(25,179,'IC426787 reads differ from VRB3: technical import/ID validation only.')
text(154,116,'D  Independent module checks')
cards=[('Promoter sequences','8 / 8 exact','Direct slicing + bedtools','2,200 bp; both strands'),('Domain coordinates','16 / 16 matched','Independent reconstruction','13 genes'),('Similarity classifications','20 / 20 agreed','Small RBCS-family panels','Not an orthology test'),('Expression identifiers','34,914 / 34,914','Exact ricebean RefSeq IDs','Separate task denominator')]
for i,ls in enumerate(cards):
 x=154+(i%2)*60;y=124+(i//2)*28;rect(x,y,57,25,'#f7f9fa')
 for k,l in enumerate(ls):text(x+2,y+5+k*5,l)
text(154,183,'Task-specific denominators are retained; no pooled accuracy score.')
a.append('</svg>');p=O/'Supporting_Figures/Figure_S1.svg';p.write_text('\n'.join(a))
for ext in ['pdf','png']:
 cmd=['inkscape',str(p),f'--export-type={ext}',f'--export-filename={p.with_suffix("."+ext)}']
 if ext=='png':cmd+=['--export-dpi=600']
 subprocess.run(cmd,check=True,capture_output=True)
subprocess.run(['inkscape',str(p),'--export-type=svg','--export-text-to-path','--export-plain-svg',f'--export-filename={p.with_name("Figure_S1_outlined.svg")}'],check=True,capture_output=True)
