"""Three fresh-process, alternating-order extraction trials on the same real loci.
Index construction and warm query times are reported separately. No GUI-usability
or overall speed superiority is inferred from the different storage designs.
"""
import argparse, collections, csv, hashlib, json, statistics, subprocess, sys
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--jar',type=Path,required=True);p.add_argument('--java',default='java');p.add_argument('--out',type=Path,default=Path('target/extraction-comparison'));a=p.parse_args()
root=Path(__file__).resolve().parents[1];annotation=root/'examples/arabidopsis-atlas/annotation.gff3'
ids=['AT1G67090','AT5G38410','AT5G38420','AT5G38430'];a.out.mkdir(parents=True,exist_ok=True)
def records(path):
 with path.open() as stream:
  reader=csv.reader(stream,delimiter='\t');next(reader);return collections.Counter(tuple(row) for row in reader)
trials=[]
for trial in range(3):
 java=a.out/f'java-{trial+1}';baseline=a.out/f'gffutils-{trial+1}'
 commands=[ [a.java,'-Xmx3g','-jar',str(a.jar),'--benchmark','--annotation',str(annotation),'--out',str(java)],
            [sys.executable,str(root/'tools/compare_gffutils.py'),'--annotation',str(annotation),'--loci',*ids,'--out',str(baseline)] ]
 for command in commands if trial%2==0 else commands[::-1]:subprocess.run(command,check=True,stdout=subprocess.DEVNULL)
 assert records(java/'parsed-structure.tsv')==records(baseline/'parsed-structure.tsv'),'Extraction disagreement'
 for directory in [java,baseline]:
  subprocess.run([sys.executable,str(root/'tools/validate_structure_inventory.py'),'--source',str(annotation),'--parsed',str(directory/'parsed-structure.tsv'),'--out',str(directory/'correctness.json')],check=True,stdout=subprocess.DEVNULL)
 trials.append({'trial':trial+1,'first_tool':'java' if trial%2==0 else 'gffutils','phytoscope':json.loads((java/'performance.json').read_text()),'gffutils':json.loads((baseline/'performance.json').read_text()),'record_agreement':True,'records':sum(records(java/'parsed-structure.tsv').values())})
summary={'input_sha256':hashlib.sha256(annotation.read_bytes()).hexdigest(),'loci':ids,'trials':trials,
 'median_import_or_index_ms':{'phytoscope_in_memory':statistics.median(t['phytoscope']['import_ms'] for t in trials),'gffutils_sqlite':statistics.median(t['gffutils']['index_build_ms'] for t in trials)},
 'interpretation':'Same loci and exact output agreement. Index construction differs (memory vs SQLite); timings are not a GUI comparison or proof of general superiority. Java heap pool peaks and Python RSS are different quantities.'}
(a.out/'summary.json').write_text(json.dumps(summary,indent=2)+'\n');print(json.dumps({k:v for k,v in summary.items() if k!='trials'},indent=2))
