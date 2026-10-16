"""Same-input, same-output structure extraction baseline using gffutils 0.13.
Report index construction separately from warm queries. This is a parser/query
baseline, not a claim to compare GUI usability or all capabilities of browsers.
"""
import argparse, csv, json, time, platform, sys
try:
 import resource
except ImportError:
 resource=None
from pathlib import Path
import gffutils
from validate_structure_inventory import GENE_TYPES, TX_TYPES, UTR_TYPES
p=argparse.ArgumentParser();p.add_argument('--annotation',type=Path,required=True);p.add_argument('--loci',nargs='+',required=True);p.add_argument('--out',type=Path,required=True);a=p.parse_args()
a.out.mkdir(parents=True,exist_ok=True);dbfile=a.out/'reference.db'
start=time.perf_counter()
db=gffutils.create_db(str(a.annotation),dbfn=str(dbfile),force=True,keep_order=True,
    merge_strategy='create_unique',sort_attribute_values=True,disable_infer_genes=True,disable_infer_transcripts=True)
load_ms=(time.perf_counter()-start)*1000
results=[];timings=[]
for repeat in range(100):
 start=time.perf_counter()
 for gid in a.loci:
  gene=db[gid]
  for tx in db.children(gene,level=1):
   if tx.featuretype.lower() not in TX_TYPES:continue
   list(db.children(tx,level=1))
 timings.append((time.perf_counter()-start)*1000)
for gid in a.loci:
 gene=db[gid];results.append([gid,'',gene.seqid,gene.strand,'gene',gene.start,gene.end])
 for tx in db.children(gene,level=1):
  if tx.featuretype.lower() not in TX_TYPES:continue
  results.append([gid,tx.id,tx.seqid,tx.strand,'transcript',tx.start,tx.end])
  for f in db.children(tx,level=1):
   kind=f.featuretype.lower();kind='CDS' if kind=='cds' else 'UTR' if kind in UTR_TYPES else 'exon' if kind in {'exon','pseudogenic_exon'} else None
   if kind:results.append([gid,tx.id,f.seqid,f.strand,kind,f.start,f.end])
with (a.out/'parsed-structure.tsv').open('w') as stream:
 writer=csv.writer(stream,delimiter='\t',lineterminator='\n');writer.writerow(['GeneID','TranscriptID','Sequence','Strand','Feature','Start','End']);writer.writerows(results)
timings.sort()
report={'tool':'gffutils','version':gffutils.__version__,'python':platform.python_version(),'os':platform.platform(),'annotation':str(a.annotation),
 'index_build_ms':load_ms,'query_100_repetitions_median_ms':timings[50],'query_p95_ms':timings[94],
 'records':len(results),'max_rss_platform_units':resource.getrusage(resource.RUSAGE_SELF).ru_maxrss if resource else None,
 'rss_units':'bytes' if sys.platform=='darwin' else 'KiB' if resource else 'not measured',
 'scope':'Selected loci exact hierarchy and coordinate extraction; disk-backed index build measured separately; query queries every selected locus per repetition.'}
(a.out/'performance.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report))
