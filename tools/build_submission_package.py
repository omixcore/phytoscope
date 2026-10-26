"""Assemble current manuscript materials without inventing author declarations.
Run from any directory. Source files are copied, not regenerated or rewritten.
"""
from pathlib import Path
from zipfile import ZipFile,ZIP_DEFLATED
from datetime import datetime
from zoneinfo import ZoneInfo
import hashlib,json,re,shutil,subprocess,xml.etree.ElementTree as E
from pypdf import PdfReader,PdfWriter

ROOT=Path(__file__).resolve().parents[1]
BASE=ROOT/'manuscript/annals'
DAY=datetime.now(ZoneInfo('Asia/Shanghai')).strftime('%Y%m%d')
NAME=f'PhytoScope_Submission_Package_{DAY}'
OUT=BASE/'submission'/NAME
OUT.mkdir(parents=True,exist_ok=True)
SHA=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip()
files=[]
def copy(src,dest):
 s=ROOT/src;t=OUT/dest;t.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(s,t);files.append(t)
def write(dest,content):
 t=OUT/dest;t.parent.mkdir(parents=True,exist_ok=True);t.write_text(content,encoding='utf-8');files.append(t)
for s,d in [('manuscript.docx','Manuscript.docx'),('manuscript-review.pdf','Manuscript_review.pdf'),('manuscript.md','Manuscript_source.md')]:
 copy(f'manuscript/annals/{s}',f'01_Manuscript/{d}')
stems=['figure1-original-layout-A4','figure2-coverage','figure3-annotation-tuples','figure4-expression-reconciliation','figure5-module-checks']
for i,stem in enumerate(stems,1):
 for ext in ['pdf','svg','png']:copy(f'manuscript/annals/figures/standardized/Figure_{i}.{ext}',f'02_Main_Figures/Figure_{i}.{ext}')
for i in range(1,6):
 copy(f'manuscript/annals/figures/standardized/Figure_{i}_outlined.svg',f'02_Main_Figures/Figure_{i}_outlined.svg')
copy('manuscript/annals/figures/figure1-legend.md','02_Main_Figures/Figure_1_legend_EN_ZH.md')
copy('manuscript/annals/figures/figure1-original-layout-format.md','02_Main_Figures/Figure_1_format.md')
writer=PdfWriter()
for i,stem in enumerate(stems,1):writer.append(str(BASE/f'figures/standardized/Figure_{i}.pdf'),outline_item=f'Figure {i}')
proof=OUT/'02_Main_Figures/All_Figures_review.pdf';writer.write(proof);writer.close();files.append(proof)
for s,d in [('supplementary-information.docx','Supplementary_Information.docx'),('supplementary-information.pdf','Supplementary_Information.pdf'),('supplementary-information.md','Supplementary_Information_source.md')]:
 copy(f'manuscript/annals/{s}',f'03_Supplementary/{d}')
for i in range(1,5):
 for ext in ['pdf','svg']:
  copy(f'manuscript/annals/figures/standardized/Table_S{i}.{ext}',f'03_Supplementary/Tables/Table_S{i}.{ext}')
 copy(f'manuscript/annals/figures/standardized/Table_S{i}_outlined.svg',f'03_Supplementary/Tables/Table_S{i}_outlined.svg')
combined=PdfWriter()
for i in range(1,6):combined.append(str(BASE/f'figures/standardized/Figure_{i}.pdf'),outline_item=f'Figure {i}')
for i in range(1,5):combined.append(str(BASE/f'figures/standardized/Table_S{i}.pdf'),outline_item=f'Table S{i}')
review=OUT/'Figures_and_Tables_review.pdf';combined.write(review);combined.close();files.append(review)
for name in ['format-audit.json','tables-format-audit.json','supplementary-word-tables-audit.json']:
 copy('manuscript/annals/figures/standardized/'+name,'06_Package_checks/'+name)
copy('manuscript/annals/cover-letter.md','04_Submission_Documents/Cover_letter_source.md')
cover=(BASE/'cover-letter.md').read_text().replace('[DATE]',datetime.now(ZoneInfo('Asia/Shanghai')).strftime('%d %B %Y'))
write('04_Submission_Documents/Cover_letter.txt',cover.replace('*',''))
copy('manuscript/annals/submission-checklist.md','04_Submission_Documents/Submission_checklist.md')
# Record author-controlled gaps explicitly; do not infer people, declarations or approvals.
md=(BASE/'manuscript.md').read_text()
refs=md.split('## References',1)[1].split('## Figure legends',1)[0]
nrefs=len(re.findall(r'^\d+\. ',refs,re.M))
placeholders=sorted(set(re.findall(r'\[(?:AUTHOR[^\]]*|AFFILIATIONS|NAME, EMAIL, POSTAL ADDRESS|ORCID FOR EACH AUTHOR)\]',md)))
write('04_Submission_Documents/Items_to_complete.md',f'''# Author review items

This archive contains the current source materials, not a confirmation of journal acceptance or final submission readiness.

- Fill the author list, affiliations, corresponding author details and ORCIDs.
- Complete contributions, funding, acknowledgments and competing-interest declarations using author-verified information.
- Complete the originality, concurrent-submission and author-approval declarations in the cover letter.
- The manuscript currently has {nrefs} references. The earlier requested 50–60-reference expansion is not completed in this archive. No new reference verification was performed during packaging.
- All five figures and four supplementary tables use Arial Regular 7 pt, black text and black 0.5 pt strokes. Figure 1 retains the approved original overview layout. Original data and panel layouts are retained; every arrow has a connected shaft and head.
- Check the manuscript's legacy Figure 1 caption together with the fuller panel-specific legend included beside the selected figure.
- Provide a permanent archival DOI and confirm source-data licensing as required.
- The checklist's journal-guideline snapshot is dated 2026-10-19; current instructions have not been rechecked during packaging.
- Expression validation supports exact-release identifier reconciliation for the tested rice-bean dataset; IC426787 reads and the VRB3 reference are different genotypes.

Current manuscript placeholders:

'''+ '\n'.join('- '+p for p in placeholders)+'\n')
write('04_Submission_Documents/Figure_legends.md',md.split('## Figure legends',1)[1].strip()+'\n\nThe selected Figure 1 has an additional panel-specific English and Chinese legend in `02_Main_Figures/Figure_1_legend_EN_ZH.md`.\n')
for n in ['build_figures.py','build_graphical_abstract.py','build_reference_style_figure.py','figure1-highlights.svg','figure1-source-data.json','figure1-original-layout-check.json','standardize_figures.py','build_supplementary_tables.py']:
 copy(f'manuscript/annals/figures/{n}',f'05_Reproducibility/Figure_sources/{n}')
copy('manuscript/annals/build_docx.py','05_Reproducibility/Document_sources/build_docx.py')
for p in sorted((ROOT/'benchmarks/results/2026-10-16').glob('*/structure-validation.json')):
 copy(str(p.relative_to(ROOT)),f'05_Reproducibility/Evidence/{p.parent.name}_structure-validation.json')
for n in ['multi-locus-sequence-gold.tsv','vigna-umbellata/manifest.json','vigna-umbellata/public-expression-manifest.json','vigna-umbellata/public-expression.tsv','vigna-umbellata/gold-LOC124833352.fasta','vigna-umbellata/XP_087956893.1.faa']:
 copy(f'src/test/resources/non-model/{n}',f'05_Reproducibility/Public_data/{n}')
for n in ['non-model-portability-evidence.md','validation-and-benchmarks.md','annals-evidence.md']:
 copy(f'docs/{n}',f'05_Reproducibility/Evidence/{n}')
copy('benchmarks/results/2026-10-18/cross-species-extraction.md','05_Reproducibility/Evidence/cross-species-extraction.md')
write('05_Reproducibility/README.md',f'''# Reproducibility

Source repository: https://github.com/omixcore/phytoscope
Pinned source commit: {SHA}
Source commit URL: https://github.com/omixcore/phytoscope/tree/{SHA}

This archive includes selected source manifests, the public expression matrix, sequence gold records, frozen structural validation reports, figure source files and scripts. Full genomes, full annotations and raw FASTQ reads are obtained with the source repository's fetch scripts and verified against the manifests. They are not duplicated in this manuscript archive.

Reproduce the application checks in a checkout of the pinned repository with Java 21. Follow `docs/non-model-portability-evidence.md` and the source Supplementary Information for the data-fetch, Salmon and test commands. To reproduce Figure 1, install Arial Regular, Inkscape and Pillow, then run `python manuscript/annals/figures/build_reference_style_figure.py` inside that checkout. The formatting scripts in this archive can postdate the application/evidence commit. Copy the supplied Figure_sources scripts into the pinned checkout at `manuscript/annals/figures/`, then run `build_figures.py`, `standardize_figures.py` and `build_supplementary_tables.py` there. The Word table formatter is included separately in Document_sources; retain the supplied styled Supplementary Information DOCX when exporting table-only figures. Run the scripts in the full checkout because their evidence paths are repository-relative. The file inventory pins all supplied formatting sources by SHA-256. Arial font files are not redistributed.

Packaging did not rerun biological analyses or the application test suite. It checked PDF and DOCX readability, selected figure numbering, file checksums and ZIP integrity.
''')
write('README_FIRST.txt',f'''PhytoScope manuscript materials
Target: Annals of the New York Academy of Sciences
Archive date: {DAY}
Source commit: {SHA}

01_Manuscript: manuscript Word file, review PDF and editable Markdown source.
02_Main_Figures: five individually numbered main figures in PDF/SVG/PNG; Figure 1 is the approved original-layout version. All figures include outlined SVG copies. Also includes the Figure 1 bilingual legend, formatting notes and a five-page combined review PDF.
03_Supplementary: Supplementary Information in Word, PDF and Markdown, plus four individual tables in PDF, editable SVG and outlined SVG.
04_Submission_Documents: cover letter, figure legends, checklist and explicit remaining author-review items.
05_Reproducibility: pinned source version, figure scripts/data, validation snapshots, public expression data and provenance manifests.
06_Package_checks: file inventory, SHA-256 checksums, actual-PDF font/line audits and packaging validation results.
Figures_and_Tables_review.pdf: combined nine-page review of all figures and tables.

Read 04_Submission_Documents/Items_to_complete.md before uploading. Author metadata/declarations remain placeholders; the manuscript has {nrefs} references, so the previously requested 50–60-reference expansion is not complete. This is a packaged current draft, not an assertion that every submission requirement has been met.

Use only Figure_1 through Figure_5 as the selected main figures. Earlier alternate Figure 1 designs are not mixed into the upload figure set. The original overview SVG in the reproducibility folder is a build input.
''')
# Integrity checks on the actual copied files.
pdfs={}
for p in files:
 if p.suffix=='.pdf':
  reader=PdfReader(p);assert len(reader.pages)>0;pdfs[str(p.relative_to(OUT))]=len(reader.pages)
 if p.suffix=='.docx':
  with ZipFile(p) as z:assert z.testzip() is None;E.fromstring(z.read('word/document.xml'))
assert pdfs['02_Main_Figures/All_Figures_review.pdf']==5
checkdir=OUT/'06_Package_checks';checkdir.mkdir(exist_ok=True)
inventory=[{'path':str(p.relative_to(OUT)),'bytes':p.stat().st_size,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()} for p in sorted(files)]
(checkdir/'file_inventory.json').write_text(json.dumps(inventory,indent=2)+'\n')
(checkdir/'SHA256SUMS.txt').write_text('\n'.join(x['sha256']+'  '+x['path'] for x in inventory)+'\n')
(checkdir/'packaging_check.json').write_text(json.dumps({'source_commit':SHA,'reference_count':nrefs,'selected_main_figures':5,'combined_figure_pages':5,'combined_figure_table_pages':9,'standardized_tables':4,'figure_table_font':'Arial Regular 7 pt','figure_table_strokes':'black 0.5 pt','pdf_page_counts':pdfs,'docx_zip_xml_integrity':'PASS','author_placeholders_present':bool(placeholders),'reference_expansion_complete':False,'application_tests_rerun':False},indent=2)+'\n')
archive=OUT.parent/(NAME+'.zip')
with ZipFile(archive,'w',ZIP_DEFLATED) as z:
 for p in sorted(OUT.rglob('*')):
  if p.is_file():z.write(p,str(Path(NAME)/p.relative_to(OUT)))
with ZipFile(archive) as z:
 assert z.testzip() is None
 for e in inventory:
  assert hashlib.sha256(z.read(str(Path(NAME)/e['path']))).hexdigest()==e['sha256']
print(json.dumps({'archive':str(archive),'bytes':archive.stat().st_size,'files':len(inventory)+3,'reference_count':nrefs,'pdfs':pdfs},indent=2))
