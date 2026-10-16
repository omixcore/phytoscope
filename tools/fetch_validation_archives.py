"""Download the four pinned Ensembl Plants 62 validation archives without changing fixtures."""
import argparse, concurrent.futures, hashlib, json, urllib.request
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--cache',type=Path,required=True);a=p.parse_args();a.cache.mkdir(parents=True,exist_ok=True)
manifest=json.loads((Path(__file__).resolve().parents[1]/'src/test/resources/chromosome-validation/manifest.json').read_text())
def fetch(item):
 name,entry=item;dest=a.cache/(name+'.gff3.gz')
 if not dest.exists():
  with urllib.request.urlopen(entry['source'],timeout=180) as response:dest.write_bytes(response.read())
 digest=hashlib.sha256(dest.read_bytes()).hexdigest()
 if digest!=entry['archive_sha256']:raise ValueError(f'Source changed: {name}')
 print(name,'SHA-256 PASS',flush=True)
with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:list(pool.map(fetch,manifest.items()))
