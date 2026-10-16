"""Extract real single-assay RBCS measurements from E-MTAB-7978; no synthetic values.
Downloads are pinned by SHA-256 in the committed manifest after the first build.
Only one-assay groups with five identical summary components are accepted, so no
assumption about mean versus median of multi-assay summaries is needed.
"""
import argparse, csv, hashlib, json, urllib.request, xml.etree.ElementTree as ET
from pathlib import Path

BASE = 'https://ftp.ebi.ac.uk/pub/databases/microarray/data/atlas/experiments/E-MTAB-7978/'
GENES = ['AT1G67090', 'AT5G38410', 'AT5G38420', 'AT5G38430']
GROUPS = ['g7', 'g8', 'g20', 'g2', 'g26', 'g40']
NAMES = ['Root', 'Rosette_leaf_1', 'Rosette_leaf_8', 'Cauline_leaf', 'Dry_seed', 'Pollen']

def build(cache, output):
    cache.mkdir(parents=True, exist_ok=True); output.mkdir(parents=True, exist_ok=True)
    previous = json.loads((output/'manifest.json').read_text()) if (output/'manifest.json').exists() else None
    inputs = {}
    for suffix in ['tpms.tsv', 'configuration.xml']:
        name = 'E-MTAB-7978-' + suffix; path = cache/name
        if not path.exists():
            with urllib.request.urlopen(BASE+name, timeout=120) as response: path.write_bytes(response.read())
        digest = hashlib.sha256(path.read_bytes()).hexdigest()
        if previous and digest != previous['inputs'][name]['sha256']: raise ValueError(f'Source changed: {name}')
        inputs[name] = {'url': BASE+name, 'sha256': digest}
    config = ET.parse(cache/'E-MTAB-7978-configuration.xml')
    groups = {g.attrib['id']: g for g in config.findall('.//assay_group')}
    metadata = []
    for group, name in zip(GROUPS, NAMES):
        assays = [a.text for a in groups[group].findall('assay')]
        assert len(assays) == 1, f'{group} is not a single-assay group'
        metadata.append([name, group, groups[group].attrib['label'], assays[0], '1', 'TPM'])
    rows = {}
    raw = []
    with (cache/'E-MTAB-7978-tpms.tsv').open() as stream:
        for row in csv.DictReader(stream, delimiter='\t'):
            if row['GeneID'] not in GENES: continue
            values = []
            for group in GROUPS:
                parts = row[group].split(',')
                assert len(parts) == 5 and len(set(parts)) == 1, (row['GeneID'], group, parts)
                float(parts[0]); values.append(parts[0])
                raw.append([row['GeneID'], group, row[group]])
            rows[row['GeneID']] = values
    assert set(rows) == set(GENES)
    matrix = '# unit=TPM\n# level=gene\n# source=Expression Atlas E-MTAB-7978; single-assay groups only\n'
    matrix += '\t'.join(['GeneID']+NAMES)+'\n'
    matrix += ''.join('\t'.join([gene]+rows[gene])+'\n' for gene in GENES)
    (output/'expression.tsv').write_text(matrix)
    for name, header, content in [('samples.tsv', ['Sample','Atlas_group','Atlas_label','ENA_run','Assays_in_group','Unit'], metadata),
                                   ('source-values.tsv', ['GeneID','Atlas_group','Unmodified_source_cell'], raw)]:
        with (output/name).open('w', newline='') as f:
            writer=csv.writer(f, delimiter='\t', lineterminator='\n'); writer.writerow(header); writer.writerows(content)
    # Keep a complete, unchanged descendant closure for the four case-study genes.
    reference = Path(__file__).resolve().parents[1]/'src/main/resources/data/species/arabidopsis'
    annotation_manifest = json.loads((reference/'manifest.json').read_text())
    selected = set(GENES)
    rows = []
    for chromosome in ['Chr1', 'Chr5']:
        path = reference/(chromosome+'.gff3')
        assert hashlib.sha256(path.read_bytes()).hexdigest() == annotation_manifest['chromosomes'][chromosome]['sha256']
        # The pinned Araport11 input is parent ordered; independent validation checks the retained hierarchy.
        with path.open() as stream:
            for line in stream:
                if line.startswith('#'): continue
                f=line.rstrip('\n').split('\t')
                attrs=dict(token.split('=',1) for token in f[8].split(';') if '=' in token)
                if attrs.get('ID') in selected or any(parent in selected for parent in attrs.get('Parent','').split(',')):
                    rows.append(line.rstrip('\n'))
                    if 'ID' in attrs:selected.add(attrs['ID'])
    assert all(any(('ID='+gene+';') in row for row in rows) for gene in GENES)
    header=['##gff-version 3','##species=Arabidopsis thaliana','##assembly=TAIR10 / Araport11']
    header += [f"##sequence-region {c} 1 {annotation_manifest['chromosomes'][c]['length']}" for c in ['Chr1','Chr5']]
    (output/'annotation.gff3').write_text('\n'.join(header+rows)+'\n')
    manifest = {'accession':'E-MTAB-7978', 'title':'Arabidopsis tissue atlas',
        'publication_doi':'10.1038/s41586-020-2094-2', 'assembly':'TAIR10', 'annotation_join':'Exact AGI gene IDs to bundled Araport11; no transcript-level inference',
        'inputs':inputs, 'genes':GENES, 'groups':GROUPS,
        'extraction':'Selected single-assay groups; all five source summary components must be identical; retain that value unchanged.',
        'limitations':'Descriptive gene-level atlas values, one assay per selected condition; no differential-expression statistics or causal inference.',
        'annotation_sources':{c:annotation_manifest['chromosomes'][c]['sha256'] for c in ['Chr1','Chr5']},
        'outputs':{name: hashlib.sha256((output/name).read_bytes()).hexdigest() for name in ['expression.tsv','samples.tsv','source-values.tsv','annotation.gff3']}}
    (output/'manifest.json').write_text(json.dumps(manifest, indent=2)+'\n')
    print(f'PASS: {len(GENES)} genes x {len(GROUPS)} traceable single-assay conditions')

if __name__ == '__main__':
    parser=argparse.ArgumentParser(); parser.add_argument('--cache',type=Path,default=Path('target/atlas-cache')); parser.add_argument('--output',type=Path,default=Path('examples/arabidopsis-atlas'))
    args=parser.parse_args(); build(args.cache,args.output)
