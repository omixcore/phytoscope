#!/usr/bin/env python3
"""Fetch the published peptide archive and verify its frozen SHA-256."""
import argparse
import html
import json
import re
import urllib.request
from pathlib import Path
from screen_domain_isoforms import sha256


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--case', type=Path, default=Path('examples/arabidopsis-isoform-validation'))
    p.add_argument('--cache', type=Path, default=Path('target/independent-isoforms'))
    args = p.parse_args()
    source = json.loads((args.case / 'sources.json').read_text())['peptides']
    args.cache.mkdir(parents=True, exist_ok=True)
    target = args.cache / source['archive_name']
    if target.exists() and sha256(target) == source['archive_sha256']:
        print(target)
        return
    with urllib.request.urlopen(source['download_page'], timeout=60) as response:
        page = response.read().decode('utf-8')
    urls = [html.unescape(u) for u in re.findall(r'href="([^"]+)"', page)
            if source['archive_name'] in u]
    if not urls:
        raise RuntimeError('Publisher download link missing; supply the original archive manually')
    temporary = target.with_suffix('.download')
    urllib.request.urlretrieve(urls[0], temporary)
    if sha256(temporary) != source['archive_sha256']:
        temporary.unlink()
        raise RuntimeError('Source archive changed: checksum does not match the frozen case')
    temporary.replace(target)
    print(target)


if __name__ == '__main__':
    main()
