"""Keep original figure data/layout while applying the requested figure style.
Outputs A4 landscape pages with >=25 mm margins, Arial Regular 7 pt and black
0.5 pt strokes. Figures remain proportional; filled data colors are retained.
"""
from pathlib import Path
import base64,copy,io,json,math,re,shutil,subprocess,xml.etree.ElementTree as E
from PIL import Image
import fitz
O=Path(__file__).resolve().parent;OUT=O/'standardized';OUT.mkdir(exist_ok=True)
NS='http://www.w3.org/2000/svg';XL='http://www.w3.org/1999/xlink'
E.register_namespace('',NS);E.register_namespace('xlink',XL)
def T(s):return '{'+NS+'}'+s
def sty(z):
 return dict((k.strip(),v.strip()) for v0 in z.get('style','').split(';') if ':' in v0 for k,v in [v0.split(':',1)])
def setsty(z,d):z.set('style','; '.join(f'{k}: {v}' for k,v in d.items()))
S=['figure1-original-layout-A4','figure2-coverage','figure3-annotation-tuples','figure4-expression-reconciliation','figure5-module-checks']
audit=[]
for no,stem in enumerate(S,1):
 if no==1:
  for ext in ['svg','pdf','png']:shutil.copy2(O/f'{stem}.{ext}',OUT/f'Figure_{no}.{ext}')
  shutil.copy2(O/f'{stem}-outlined.svg',OUT/f'Figure_{no}_outlined.svg')
  audit.append({'figure':1,'source':stem,'source_text_retained':True,'source_layout_retained':True})
  continue
 src=E.parse(O/f'{stem}.svg').getroot();texts=[''.join(z.itertext()) for z in src.iter(T('text'))]
 _,_,w,h=map(float,src.get('viewBox').split())
 scale=min(25.4/72,247/w,160/h);xoff=(297-w*scale)/2;yoff=(210-h*scale)/2
 font=7*25.4/72/scale;sw=.5*25.4/72/scale
 root=E.Element(T('svg'),{'width':'297mm','height':'210mm','viewBox':'0 0 297 210','preserveAspectRatio':'xMidYMid meet'})
 E.SubElement(root,T('title')).text=f'PhytoScope Figure {no}'
 E.SubElement(root,T('rect'),{'width':'297','height':'210','fill':'white','stroke':'none'})
 g=E.SubElement(root,T('g'),{'id':f'Figure_{no}','transform':f'translate({xoff},{yoff}) scale({scale})'})
 for z in list(src):g.append(copy.deepcopy(z))
 # Replace the flat coverage bitmap with cell rectangles using its own pixels,
 # coordinates and original transform. This retains exact matrix geometry.
 replaced=0
 for parent in list(g.iter()):
  for z in list(parent):
   if z.tag==T('image'):
    data=z.get('{'+XL+'}href',z.get('href',''))
    if not data.startswith('data:image/png;base64,'):raise ValueError('Unexpected image source')
    with Image.open(io.BytesIO(base64.b64decode(data.split(',',1)[1]))) as im:
     im=im.convert('RGB');pix=im.load();xx=float(z.get('x'));yy=float(z.get('y'));ww=float(z.get('width'));hh=float(z.get('height'))
     gg=E.Element(T('g'),{k:v for k,v in z.attrib.items() if k in ['transform','clip-path']})
     for r in range(7):
      for c in range(4):
       rgb=pix[min(im.width-1,int((c+.5)*im.width/4)),min(im.height-1,int((r+.5)*im.height/7))]
       E.SubElement(gg,T('rect'),{'x':str(xx+c*ww/4),'y':str(yy+r*hh/7),'width':str(ww/4),'height':str(hh/7),'fill':'#%02x%02x%02x'%rgb,'stroke':'none'})
    ix=list(parent).index(z);parent.remove(z);parent.insert(ix,gg);replaced+=1
 # Matplotlib arrow patches have separate shaft/head paths. Fuse them into
 # one continuously connected path; preserve their original endpoints.
 arrows=0
 for z in list(g.iter(T('g'))):
  children=list(z)
  if len(children)==2 and all(x.tag==T('path') for x in children):
   a,b=children;sa,sb=sty(a),sty(b)
   if sa.get('fill')=='none' and sb.get('fill','none')!='none':
    nums=list(map(float,re.findall(r'-?\d+(?:\.\d+)?',b.get('d',''))))
    if len(nums)==6:
     left,tip,right=nums[:2],nums[2:4],nums[4:6]
     a.set('d',a.get('d')+f' L{left[0]},{left[1]} L{right[0]},{right[1]} L{tip[0]},{tip[1]}')
     sa['fill']='#000000';setsty(a,sa);z.remove(b);arrows+=1
 for z in g.iter():
  if z.tag==T('text'):
   d=sty(z)
   for k in list(d):
    if k.startswith('font') or k in ['fill','stroke','color','text-decoration']:d.pop(k)
   d.update({'font-family':'Arial','font-size':str(font)+'px','font-weight':'400','font-style':'normal','fill':'#000000','stroke':'none'})
   setsty(z,d)
   for k in list(z.attrib):
    if k.startswith('font-'):del z.attrib[k]
  else:
   d=sty(z)
   if d.get('stroke',z.get('stroke','none'))!='none':
    d['stroke']='#000000';d['stroke-width']=str(sw);d['stroke-linecap']='round';d['stroke-linejoin']='round'
    z.attrib.pop('stroke',None);z.attrib.pop('stroke-width',None);setsty(z,d)
 assert texts==[''.join(z.itertext()) for z in g.iter(T('text'))]
 p=OUT/f'Figure_{no}.svg';E.ElementTree(root).write(p,encoding='unicode',xml_declaration=True)
 for ext in ['pdf','png']:
  cmd=['inkscape',str(p),f'--export-type={ext}',f'--export-filename={OUT/f"Figure_{no}.{ext}"}']
  if ext=='png':cmd+=['--export-dpi=600']
  subprocess.run(cmd,check=True,capture_output=True)
 subprocess.run(['inkscape',str(p),'--export-type=svg','--export-text-to-path','--export-plain-svg',f'--export-filename={OUT/f"Figure_{no}_outlined.svg"}'],check=True,capture_output=True)
 with Image.open(OUT/f'Figure_{no}.png') as im:im.save(OUT/f'Figure_{no}.png',dpi=(600,600),optimize=True)
 audit.append({'figure':no,'source':stem,'source_text_retained':True,'source_layout_retained':True,'text_nodes':len(texts),'joined_arrows':arrows,'coverage_bitmaps_vectorized':replaced,'artwork_mm':[w*scale,h*scale],'offset_mm':[xoff,yoff]})
# Verify the exported PDFs, not just SVG declarations.
for a in audit:
 no=a['figure'];p=fitz.open(OUT/f'Figure_{no}.pdf')[0]
 spans=[s for b in p.get_text('dict')['blocks'] if 'lines' in b for l in b['lines'] for s in l['spans']]
 assert spans and all(s['font']=='ArialMT' and abs(s['size']-7)<.01 and s['color']==0 for s in spans),(no,{(s['font'],s['size'],s['color']) for s in spans})
 widths=[]
 for d in p.get_drawings():
  if d.get('color') is not None:
   assert all(abs(c)<1e-6 for c in d['color']),no
   assert abs(d['width']-.5)<.002,(no,d['width'])
   widths.append(d['width'])
 a.update({'PDF_font':'ArialMT','PDF_font_pt':7,'PDF_text_color':'#000000','PDF_stroke_color':'#000000','PDF_stroke_pt':.5,'PDF_images':len(p.get_images()),'all_checks':'PASS'})
(OUT/'format-audit.json').write_text(json.dumps(audit,indent=2)+'\n')
print(json.dumps(audit,indent=2))
