"""Package the selected one-main-figure report; run within a full checkout."""
from pathlib import Path
import re,json,hashlib,shutil,tempfile,xml.etree.ElementTree as ET
from zipfile import ZipFile,ZIP_DEFLATED
import fitz
O=Path(__file__).resolve().parent;ROOT=O.parents[2]
NAME='PhytoScope_Concise_Report_20261026'
md=(O/'manuscript.md').read_text()
def words(s):return len(re.findall(r"\b[\w]+(?:[’'–-][\w]+)*\b",s))
def section(a,b):return md.split(a,1)[1].split(b,1)[0].strip()
abstract=section('## Abstract','**Keywords:')
body=section('## Introduction','## Acknowledgments')
body='\n'.join(l for l in body.splitlines() if not l.startswith('#'))
refs=section('## References','## Figure Legend')
refids=[int(n) for n in re.findall(r'^(\d+)\. ',refs,re.M)]
seen=[]
for c in re.findall(r'\[([0-9, ]+)\]',body):
 for n in re.findall(r'\d+',c):
  n=int(n)
  if n not in seen:seen.append(n)
assert refids==seen==list(range(1,15))
audit=json.loads((O/'content-audit.json').read_text())
audit.update(abstract_words=words(abstract),body_words_excluding_headings=words(body)+words('Introduction'),total_manuscript_words_including_references_and_legend=words(md),references=len(refids))
# Include the Introduction text (the split removed only its heading).
audit['body_words_excluding_headings']=words(body)
audit['word_count_method']='Unicode word tokens; internal apostrophes and hyphens counted within a word; headings excluded from body'
assert audit['abstract_words']<=200 and audit['total_manuscript_words_including_references_and_legend']<=4000
(O/'content-audit.json').write_text(json.dumps(audit,indent=2)+'\n')
# Check actual exported artwork, including embedded font, text sizes/colors and vector strokes.
art=[]
for p in [O/'Figure_1.pdf',*sorted((O/'Supporting_Figures').glob('*.pdf')),*sorted((O/'Supporting_Tables').glob('*.pdf'))]:
 d=fitz.open(p);spans=[s for pg in d for b in pg.get_text('dict')['blocks'] if 'lines'in b for l in b['lines'] for s in l['spans'] if s['text'].strip()]
 sizes=sorted(set(round(s['size'],3) for s in spans));fonts=sorted(set(s['font'] for s in spans));colors=sorted(set(s['color'] for s in spans))
 strokes=[x for pg in d for x in pg.get_drawings() if x.get('color') is not None and x.get('width',0)>0]
 widths=sorted(set(round(x['width'],3) for x in strokes));black=all(all(abs(v)<1e-5 for v in x['color']) for x in strokes)
 assert sizes==[7.0] and colors==[0] and all('Arial' in f and 'Bold' not in f for f in fonts),(p,sizes,fonts,colors)
 assert widths==[0.5] and black,(p,widths)
 assert p.stat().st_size<10_000_000
 art.append({'file':str(p.relative_to(O)),'fonts':fonts,'sizes_pt':sizes,'text_colors':colors,'stroke_widths_pt':widths,'black_strokes':black,'bytes':p.stat().st_size})
(O/'artwork-audit.json').write_text(json.dumps(art,indent=2)+'\n')
legend=md.split('## Figure Legend',1)[1].strip()
(O/'Figure_1_legend.md').write_text('# Figure 1 legend\n\n'+legend+'\n')
with tempfile.TemporaryDirectory(prefix='phyto-concise-') as td:
 out=Path(td)/NAME;out.mkdir()
 def copy(src,dest):
  target=out/dest;target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(src,target)
 for n in ['manuscript.docx','manuscript.pdf','manuscript.md']:copy(O/n,'01_Manuscript/'+n)
 for n in ['Figure_1.pdf','Figure_1.svg','Figure_1.png','Figure_1_outlined.svg','Figure_1_legend.md']:copy(O/n,'02_Main_Figure/'+n)
 for n in ['supporting-information.docx','supporting-information.pdf','supporting-information.md']:copy(O/n,'03_Supporting_Information/'+n)
 for folder in ['Supporting_Figures','Supporting_Tables']:
  for p in sorted((O/folder).iterdir()):
   if p.is_file():copy(p,'03_Supporting_Information/'+folder+'/'+p.name)
 for n in ['cover-letter.docx','cover-letter.pdf','cover-letter.md','journal-requirements.md']:copy(O/n,'04_Submission_Documents/'+n)
 for n in ['content-audit.json','artwork-audit.json','reference-metadata-audit.json','references-verified.json']:copy(O/n,'06_Checks/'+n)
 for n in ['build_report.py','build_table_assets.py','build_package.py']:copy(O/n,'05_Reproducibility/Report_sources/'+n)
 for n in ['multi-locus-sequence-gold.tsv','vigna-umbellata/manifest.json','vigna-umbellata/public-expression-manifest.json','vigna-umbellata/public-expression.tsv','vigna-umbellata/gold-LOC124833352.fasta','vigna-umbellata/XP_087956893.1.faa']:copy(ROOT/'src/test/resources/non-model'/n,'05_Reproducibility/Public_data/'+n)
 for p in sorted((ROOT/'benchmarks/results/2026-10-16').glob('*/structure-validation.json')):copy(p,'05_Reproducibility/Evidence/'+p.parent.name+'_structure-validation.json')
 for n in ['non-model-portability-evidence.md','validation-and-benchmarks.md']:copy(ROOT/'docs'/n,'05_Reproducibility/Evidence/'+n)
 copy(ROOT/'benchmarks/results/2026-10-18/cross-species-extraction.md','05_Reproducibility/Evidence/cross-species-extraction.md')
 (out/'README_FIRST.txt').write_text(f'''PhytoScope: Concise Report for Annals of the New York Academy of Sciences

Selected main artwork: ONE Figure 1, also reused for the graphical abstract.
Four supporting figures and four editable supporting tables accompany the report.
The main figure preserves the approved overview geometry, with uppercase A-D labels.
All figure/table artwork: Arial Regular 7 pt; black text and black 0.5 pt strokes.

Body: {audit['body_words_excluding_headings']} words. Abstract: {audit['abstract_words']} words. References: 14.
Current journal instructions checked 26 October 2026; see 04_Submission_Documents.

01_Manuscript: editable Word, review PDF, Markdown.
02_Main_Figure: the sole main figure, PDF/vector SVG/600-dpi PNG, outlined SVG and legend.
03_Supporting_Information: extended methods, S1-S4 figures and separate editable S1-S4 tables.
04_Submission_Documents: cover letter and preparation/author-review record.
05_Reproducibility: selected public data, gold records, manifests, evidence and report source scripts.
06_Checks: word counts, reference metadata, actual PDF font/stroke audit, SHA256 inventory.

Author names, affiliations, ORCIDs, corresponding-author details, CRediT roles,
funding, conflicts, author approval/originality statements and archival DOI remain
explicit author-controlled fields. This draft has not been submitted.

Source/evidence snapshot: https://github.com/omixcore/phytoscope/tree/159ae0f79bd3681afc6c914db282c515190a3888
The manuscript-formatting changes postdate that snapshot and are versioned on dev.
To reproduce, check out the current dev report together with the pinned evidence;
follow the commands in Supporting Information and docs/non-model-portability-evidence.md.
The archive includes selected inputs, not full genomes/FASTQ archives. Public fetch
scripts verify source checksums. Arial font files are not redistributed.
Packaging checked document structure, all rendered Word pages, figure styles,
reference order and archive integrity; it did not rerun application/biological analyses.
Expression evidence concerns exact-release IDs from IC426787 reads against VRB3,
not genotype-matched expression biology or measured gains in user efficiency.
''')
 pdfs={}
 for p in out.rglob('*'):
  if p.suffix=='.pdf':pdfs[str(p.relative_to(out))]=len(fitz.open(p))
  elif p.suffix=='.docx':
   with ZipFile(p) as z:assert z.testzip() is None;ET.fromstring(z.read('word/document.xml'))
 assert len(list((out/'02_Main_Figure').glob('*.pdf')))==1
 check={'main_figures':1,'supporting_figures':4,'supporting_tables':4,'references':14,'pdf_page_counts':pdfs,'docx_integrity':'PASS','rendered_word_pages_visually_checked':15,'application_tests_rerun':False,'author_fields_complete':False}
 (out/'06_Checks/package-check.json').write_text(json.dumps(check,indent=2)+'\n')
 inventory=[{'path':str(p.relative_to(out)),'bytes':p.stat().st_size,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()} for p in sorted(out.rglob('*')) if p.is_file()]
 (out/'06_Checks/file-inventory.json').write_text(json.dumps(inventory,indent=2)+'\n')
 (out/'06_Checks/SHA256SUMS.txt').write_text('\n'.join(x['sha256']+'  '+x['path'] for x in inventory)+'\n')
 archive=O/(NAME+'.zip')
 with ZipFile(archive,'w',ZIP_DEFLATED) as z:
  for p in sorted(out.rglob('*')):
   if p.is_file():z.write(p,str(Path(NAME)/p.relative_to(out)))
 with ZipFile(archive) as z:
  assert z.testzip() is None
  for x in inventory:assert hashlib.sha256(z.read(NAME+'/'+x['path'])).hexdigest()==x['sha256']
 assert archive.stat().st_size<500_000_000
 print(json.dumps({'archive':str(archive),'bytes':archive.stat().st_size,'files':len(inventory)+2,'counts':audit},indent=2))
