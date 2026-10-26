"""Export each existing Word table as an editable A4 SVG and vector PDF.
Content is read from the styled Supplementary Information DOCX, not recreated.
"""
from pathlib import Path
import html,json,subprocess
from docx import Document
from PIL import ImageFont
import fitz
O=Path(__file__).resolve().parent;OUT=O/'Supporting_Tables';OUT.mkdir(exist_ok=True)
D=Document(O/'supporting-information.docx')
TITLES=[p.text for p in D.paragraphs if p.text.startswith('Table S')]
fontmm=7*25.4/72;stroke=.5*25.4/72
fp=subprocess.check_output(['fc-match','-f','%{file}','Arial']).decode()
font=ImageFont.truetype(fp,140)
def length(s):return font.getlength(s)*fontmm/140
def wrap(s,w):
 lines=[];line=''
 for word in s.split():
  while length(word)>w:
   cut=1
   while cut<len(word) and length(word[:cut+1])<=w:cut+=1
   if line:lines.append(line);line=''
   lines.append(word[:cut]);word=word[cut:]
  candidate=(line+' '+word).strip()
  if length(candidate)>w and line:lines.append(line);line=word
  else:line=candidate
 if line:lines.append(line)
 return lines or ['']
audit=[]
for i,(title,table) in enumerate(zip(TITLES,D.tables),1):
 rows=[[c.text for c in row.cells] for row in table.rows];n=len(rows[0]);width=159.6;cw=width/n;x=25.2;y=35
 out=['<svg xmlns="http://www.w3.org/2000/svg" width="210mm" height="297mm" viewBox="0 0 210 297" preserveAspectRatio="xMidYMid meet">','<rect width="210" height="297" fill="white"/>','<g id="Table_S'+str(i)+'">']
 def text(xx,yy,s):out.append(f'<text x="{xx}" y="{yy}" font-family="Arial" font-size="{fontmm}" font-weight="400" fill="#000000">{html.escape(s)}</text>')
 for j,s in enumerate(wrap(title,width)):text(x,28+j*3.3,s)
 for ri,row in enumerate(rows):
  cells=[wrap(s,cw-3) for s in row];rh=max(map(len,cells))*3.3+2
  for ci,lines in enumerate(cells):
   xx=x+ci*cw
   out.append(f'<rect x="{xx}" y="{y}" width="{cw}" height="{rh}" fill="{"#F0F2F3" if ri==0 else "white"}" stroke="#000000" stroke-width="{stroke}"/>')
   for k,s in enumerate(lines):text(xx+1.5,y+3+k*3.3,s)
  y+=rh
 assert y<=272,('table too tall',i,y)
 out.append('</g></svg>');p=OUT/f'Table_S{i}.svg';p.write_text('\n'.join(out)+'\n')
 subprocess.run(['inkscape',str(p),'--export-type=pdf',f'--export-filename={OUT/f"Table_S{i}.pdf"}'],check=True,capture_output=True)
 subprocess.run(['inkscape',str(p),'--export-type=svg','--export-text-to-path','--export-plain-svg',f'--export-filename={OUT/f"Table_S{i}_outlined.svg"}'],check=True,capture_output=True)
 pdf=fitz.open(OUT/f'Table_S{i}.pdf');page=pdf[0]
 spans=[s for b in page.get_text('dict')['blocks'] if 'lines' in b for l in b['lines'] for s in l['spans']]
 assert all(s['font']=='ArialMT' and abs(s['size']-7)<.01 and s['color']==0 for s in spans)
 for d in page.get_drawings():
  if d.get('color') is not None:assert abs(d['width']-.5)<.001 and d['color']==(0.,0.,0.)
 audit.append({'table':f'S{i}','rows':len(rows),'columns':n,'font':'ArialMT','font_pt':7,'border_pt':.5,'color':'#000000','content_bottom_mm':y,'all_checks':'PASS'})
(OUT/'tables-format-audit.json').write_text(json.dumps(audit,indent=2)+'\n')
print('Exported four editable supplementary tables and outlined copies; all checked.')
