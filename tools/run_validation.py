"""Run real reference imports, measured benchmarks and independent structure checks."""
import argparse, json, subprocess, sys
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--jar',type=Path,required=True);p.add_argument('--java',default='java');p.add_argument('--out',type=Path,default=Path('target/research-validation'));p.add_argument('--full-dir',type=Path);a=p.parse_args()
root=Path(__file__).resolve().parents[1]
for name in ['arabidopsis','rice','maize','soybean']:
 out=a.out/name
 subprocess.run([a.java,'-Xmx3g','-jar',str(a.jar),'--benchmark','--example',name,'--out',str(out)],check=True)
 if name=='arabidopsis':
  sources= sorted((root/'src/main/resources/data/species/arabidopsis').glob('Chr*.gff3*'))
 else:
  base=root/f'src/main/resources/data/species/{name}/{name}_chromosomes.gff3'
  sources=[base] if base.exists() else [base.with_suffix(base.suffix+'.gz')]
 subprocess.run([sys.executable,str(root/'tools/validate_structure_inventory.py'),'--source',*map(str,sources),'--parsed',str(out/'parsed-structure.tsv'),'--out',str(out/'structure-validation.json')],check=True)
 if a.full_dir:
  source=a.full_dir/f'{name}.gff3.gz';full=a.out/(name+'-full')
  subprocess.run([a.java,'-Xmx3g','-jar',str(a.jar),'--benchmark','--annotation',str(source),'--out',str(full)],check=True)
  subprocess.run([sys.executable,str(root/'tools/validate_structure_inventory.py'),'--source',str(source),'--parsed',str(full/'parsed-structure.tsv'),'--out',str(full/'structure-validation.json')],check=True)
print('PASS: all requested source-to-model structure checks')
