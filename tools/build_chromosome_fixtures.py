"""Extract real gene families on every nuclear chromosome from Ensembl Plants.

Usage: python tools/build_chromosome_fixtures.py --cache /tmp/phytoscope-validation
Downloads release 62 GFF3 archives when missing. No synthetic gene coordinates.
The source sequence names, sequence lengths and complete selected families are
retained. Two protein-coding gene families per chromosome keep fixtures small.
"""
import argparse
import gzip
import hashlib
import json
from pathlib import Path
import re
import urllib.request

BASE = 'https://ftp.ensemblgenomes.ebi.ac.uk/pub/plants/release-62/gff3/'
SOURCES = {
    'arabidopsis': ('Arabidopsis thaliana', 'TAIR10', 'arabidopsis_thaliana/Arabidopsis_thaliana.TAIR10.62.gff3.gz'),
    'rice': ('Oryza sativa Japonica Group', 'IRGSP-1.0', 'oryza_sativa/Oryza_sativa.IRGSP-1.0.62.gff3.gz'),
    'maize': ('Zea mays', 'Zm-B73-REFERENCE-NAM-5.0', 'zea_mays/Zea_mays.Zm-B73-REFERENCE-NAM-5.0.62.gff3.gz'),
    'soybean': ('Glycine max', 'Glycine_max_v2.1', 'glycine_max/Glycine_max.Glycine_max_v2.1.62.gff3.gz'),
}


def attributes(text):
    return dict(x.split('=', 1) for x in text.split(';') if '=' in x)


def extract(archive):
    regions, genes, rows = {}, {}, []
    # Keep all rows of the first two gene families on each numeric chromosome.
    # GFF3 descendants may precede parents; resolve the closure after reading.
    with gzip.open(archive, 'rt', encoding='utf-8') as src:
        for line in src:
            if line.startswith('##sequence-region'):
                _, seq, start, end = line.split()
                if re.fullmatch(r'[0-9]+', seq):
                    regions[seq] = (int(start), int(end))
            elif not line.startswith('#'):
                cols = line.rstrip('\n').split('\t')
                if len(cols) == 9 and cols[0] in regions:
                    attrs = attributes(cols[8])
                    if cols[2] == 'gene' and len(genes.setdefault(cols[0], [])) < 2:
                        genes[cols[0]].append(attrs['ID'])
                    rows.append((cols, attrs))
    selected = {gid for ids in genes.values() for gid in ids}
    children = {}
    for cols, attrs in rows:
        for parent in attrs.get('Parent', '').split(','):
            if parent:
                children.setdefault(parent, []).append(attrs.get('ID'))
    pending = list(selected)
    while pending:
        for child in children.get(pending.pop(), []):
            if child and child not in selected:
                selected.add(child)
                pending.append(child)
    kept = [cols for cols, attrs in rows if attrs.get('ID') in selected
            or any(p in selected for p in attrs.get('Parent', '').split(','))]
    order = {seq: i for i, seq in enumerate(sorted(regions, key=int))}
    kept.sort(key=lambda cols: order[cols[0]])  # stable within each family
    return dict(sorted(regions.items(), key=lambda item: int(item[0]))), genes, kept


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--cache', type=Path, required=True)
    parser.add_argument('--update-examples', action='store_true', help='Refresh all four version-pinned reference examples')
    args = parser.parse_args()
    args.cache.mkdir(parents=True, exist_ok=True)
    root = Path(__file__).resolve().parents[1]
    dest = root / 'src/test/resources/chromosome-validation'
    dest.mkdir(parents=True, exist_ok=True)
    manifest = {}
    for name, (species, assembly, relative) in SOURCES.items():
        archive = args.cache / (name + '.gff3.gz')
        url = BASE + relative
        if not archive.exists():
            urllib.request.urlretrieve(url, archive)
        regions, genes, rows = extract(archive)
        header = ['##gff-version 3', '##species=' + species, '##assembly=' + assembly,
                  '# Source: ' + url, '# Real annotation excerpt: two gene families per nuclear chromosome.']
        header += [f'##sequence-region {seq} {start} {end}' for seq, (start, end) in regions.items()]
        data = '\n'.join(header + ['\t'.join(row) for row in rows]) + '\n'
        output = dest / (name + '.gff3')
        output.write_text(data, encoding='utf-8')
        manifest[name] = dict(species=species, assembly=assembly, source=url,
                              archive_sha256=hashlib.sha256(archive.read_bytes()).hexdigest(),
                              fixture_sha256=hashlib.sha256(data.encode()).hexdigest(),
                              chromosome_count=len(regions),
                              lengths={seq: end for seq, (_, end) in regions.items()},
                              genes=genes, feature_count=len(rows))
        print(name, len(regions), len(rows), flush=True)
    if args.update_examples:
        for name in SOURCES:
            output = root / f'src/main/resources/data/species/{name}/{name}_chromosomes.gff3'
            output.parent.mkdir(parents=True, exist_ok=True)
            output.write_bytes((dest / (name + '.gff3')).read_bytes())
    (dest / 'expected.tsv').write_text(
        'dataset\tsequence\tstart\tend\n' + ''.join(
            f'{name}\t{seq}\t1\t{end}\n'
            for name, entry in manifest.items() for seq, end in entry['lengths'].items()),
        encoding='utf-8')
    (dest / 'manifest.json').write_text(json.dumps(manifest, indent=2) + '\n', encoding='utf-8')


if __name__ == '__main__':
    main()
