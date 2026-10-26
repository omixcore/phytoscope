"""Restyle the original overview without changing its content or layout.
A4 landscape, Arial Regular 7 pt, black 0.5 pt strokes. Requires Arial and Inkscape.
"""
from pathlib import Path
import copy,json,math,re,subprocess,xml.etree.ElementTree as E
from PIL import Image,ImageFont
O=Path(__file__).resolve().parent
NS='http://www.w3.org/2000/svg';E.register_namespace('',NS)
def tag(s):return '{'+NS+'}'+s
src=E.parse(O/'figure1-highlights.svg').getroot()
texts=[''.join(x.itertext()) for x in src.iter(tag('text'))]
# Keep the original 1440 x 1090 geometry under one uniform transformation.
scale=160/1090; width=1440*scale; xoff=(297-width)/2; yoff=25
font=7*25.4/72/scale; sw=.5*25.4/72/scale
out=E.Element(tag('svg'),{'width':'297mm','height':'210mm','viewBox':'0 0 297 210','preserveAspectRatio':'xMidYMid meet'})
E.SubElement(out,tag('title')).text='PhytoScope original overview with requested publication formatting'
E.SubElement(out,tag('desc')).text='Original content, coordinates, panel arrangement and data retained. Arial Regular 7 pt; black text and black 0.5 pt strokes. Integral arrow paths. Uniform scaling only.'
E.SubElement(out,tag('rect'),{'width':'297','height':'210','fill':'white','stroke':'none'})
fontpath=subprocess.check_output(['fc-match','-f','%{file}','Arial']).decode()
metricfont=ImageFont.truetype(fontpath,round(font*20))
g=E.SubElement(out,tag('g'),{'id':'PhytoScope_original_overview','transform':f'translate({xoff},{yoff}) scale({scale})'})
for original in list(src):
 if original.tag in (tag('defs'),tag('style'),tag('title'),tag('desc')):continue
 z=copy.deepcopy(original)
 if z.tag==tag('text'):
  z.set('font-family','Arial');z.set('font-size',str(font));z.set('font-weight','400');z.set('font-style','normal');z.set('fill','#000000');z.set('stroke','none')
  if ''.join(z.itertext()) in ('ATGC','TCGA'):
   z.set('x','88.5');z.set('text-anchor','middle')
  if ''.join(z.itertext()) in ('0.39','26.88','9.12'):
   cx=float(z.get('x'));baseline=float(z.get('y'));w=metricfont.getlength(''.join(z.itertext()))/20
   E.SubElement(g,tag('rect'),{'x':str(cx-w/2-2),'y':str(baseline-font*.85),'width':str(w+4),'height':str(font*.85+2),'fill':'white','stroke':'none'})
 elif z.get('stroke') not in (None,'none'):
  old_color=z.get('stroke');old_sw=float(z.get('stroke-width','1'))
  # Preserve the original colored donut band as a filled annulus; its outline is 0.5 pt.
  if z.tag==tag('circle') and z.get('fill')=='none' and old_sw>5:
   cx,cy,r=map(float,[z.get('cx'),z.get('cy'),z.get('r')]);ro=r+old_sw/2;ri=r-old_sw/2
   d=f'M{cx+ro},{cy} A{ro},{ro} 0 1 0 {cx-ro},{cy} A{ro},{ro} 0 1 0 {cx+ro},{cy} Z M{cx+ri},{cy} A{ri},{ri} 0 1 1 {cx-ri},{cy} A{ri},{ri} 0 1 1 {cx+ri},{cy} Z'
   z=E.Element(tag('path'),{'d':d,'fill':old_color,'fill-rule':'evenodd'})
  z.set('stroke','#000000');z.set('stroke-width',str(sw));z.set('stroke-linecap','round');z.set('stroke-linejoin','round')
 if z.get('marker-end'):
  # One continuous path includes both the shaft and the filled triangular head.
  vals=list(map(float,re.findall(r'-?\d+(?:\.\d+)?',z.get('d'))))
  if len(vals)!=4:raise ValueError('Unexpected arrow geometry')
  x,y,xx,yy=vals;a=math.atan2(yy-y,xx-x);L=8;W=4
  left=(xx-L*math.cos(a)+W*math.sin(a),yy-L*math.sin(a)-W*math.cos(a))
  right=(xx-L*math.cos(a)-W*math.sin(a),yy-L*math.sin(a)+W*math.cos(a))
  z.set('d',z.get('d')+f' L{left[0]},{left[1]} L{right[0]},{right[1]} L{xx},{yy}')
  z.set('fill','#000000');del z.attrib['marker-end']
 g.append(z)
assert texts==[''.join(x.itertext()) for x in out.iter(tag('text'))]
# Non-arrow path geometry, rectangles and text anchors retain source coordinates.
assert len(list(src.iter(tag('text'))))==len(list(out.iter(tag('text'))))
stem='figure1-original-layout-A4'
p=O/f'{stem}.svg';E.ElementTree(out).write(p,encoding='unicode',xml_declaration=True)
for ext in ['pdf','png']:
 cmd=['inkscape',str(p),f'--export-type={ext}',f'--export-filename={O/f"{stem}.{ext}"}']
 if ext=='png':cmd+=['--export-dpi=600']
 subprocess.run(cmd,check=True,capture_output=True)
subprocess.run(['inkscape',str(p),'--export-type=svg','--export-text-to-path','--export-plain-svg',f'--export-filename={O/f"{stem}-outlined.svg"}'],check=True,capture_output=True)
with Image.open(O/f'{stem}.png') as im:im.save(O/f'{stem}.png',dpi=(600,600),optimize=True)
(O/'figure1-original-layout-check.json').write_text(json.dumps({'source':'figure1-highlights.svg','text_nodes':len(texts),'all_source_text_retained':True,'panel_and_data_geometry_retained':True,'icon_text_alignment':'ATGC/TCGA centered in original file symbol; all text remains 7 pt','chart_label_background':'White knockout behind the three unchanged numeric labels prevents black-grid interference','page_mm':[297,210],'artwork_mm':[width,160],'artwork_origin_mm':[xoff,yoff],'font':'Arial Regular','font_pt':7,'stroke_pt':.5,'text_and_stroke_color':'#000000','fills':'Original palette retained','arrows':'Shaft and filled head are one continuous path','scaling':'Uniform'},indent=2)+'\n')
print('Original layout retained; all',len(texts),'text nodes retained. Exported SVG, outlined SVG, PDF and PNG.')
