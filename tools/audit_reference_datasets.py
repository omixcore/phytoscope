"""Audit pinned reference archives, provider checksums and every bundled GFF3 row.

Run after build_chromosome_fixtures.py:
python tools/audit_reference_datasets.py --cache /path/to/annotation-cache
No manifest values are refreshed here: any source or fixture change fails audit.
"""
import argparse
import datetime
import gzip
import hashlib
import json
from pathlib import Path
import urllib.request

ACCESSIONS = {
    'arabidopsis': 'GCA_000001735.1',
    'rice': 'GCA_001433935.1',
    'maize': 'GCA_902167145.1',
    'soybean': 'GCA_000004515.4',
}
EXPECTED_NUCLEAR = {'arabidopsis': 5, 'rice': 12, 'maize': 10, 'soybean': 20}


def bsd_sum(data):
    value = 0
    for byte in data:
        value = ((value >> 1) | ((value & 1) << 15))
        value = (value + byte) & 0xffff
    return value, (len(data) + 1023) // 1024


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--cache', type=Path, required=True)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    fixtures = root / 'src/test/resources/chromosome-validation'
    manifest = json.loads((fixtures / 'manifest.json').read_text())
    reports = {}
    for name, meta in manifest.items():
        archive = args.cache / (name + '.gff3.gz')
        compressed = archive.read_bytes()
        assert hashlib.sha256(compressed).hexdigest() == meta['archive_sha256'], name + ': archive SHA-256 mismatch'
        checksums_url = meta['source'].rsplit('/', 1)[0] + '/CHECKSUMS'
        provider = urllib.request.urlopen(checksums_url, timeout=60).read().decode()
        record = next(l.split() for l in provider.splitlines() if l.split()[-1] == meta['source'].split('/')[-1])
        assert bsd_sum(compressed) == (int(record[0]), int(record[1])), name + ': provider checksum mismatch'
        fixture = fixtures / (name + '.gff3')
        bundled = root / f'src/main/resources/data/species/{name}/{name}_chromosomes.gff3'
        raw = fixture.read_bytes()
        assert hashlib.sha256(raw).hexdigest() == meta['fixture_sha256'], name + ': fixture SHA-256 mismatch'
        assert raw == bundled.read_bytes(), name + ': bundled data differ from verified fixture'
        remaining = {l for l in raw.decode().splitlines() if l and not l.startswith('#')}
        row_count = len(remaining)
        regions, accession, assembly = {}, None, None
        with gzip.open(archive, 'rt', encoding='utf-8') as source:
            for line in source:
                if line.startswith('##sequence-region'):
                    _, seq, start, end = line.split()
                    if seq.isdigit():
                        regions[seq] = (int(start), int(end))
                elif line.startswith('#!genome-build-accession'):
                    accession = line.split()[-1]
                elif line.startswith('#!genome-version'):
                    assembly = line.split()[-1]
                else:
                    remaining.discard(line.rstrip('\n'))
        assert not remaining, name + ': example rows absent from original archive'
        assert accession == ACCESSIONS[name], name + ': assembly accession mismatch'
        assert assembly == meta['assembly'], name + ': assembly version mismatch'
        assert len(regions) == EXPECTED_NUCLEAR[name] == meta['chromosome_count'], name + ': chromosome count mismatch'
        assert {seq: end for seq, (_, end) in regions.items()} == meta['lengths'], name + ': chromosome length mismatch'
        reports[name] = dict(
            species=meta['species'], assembly=assembly, assembly_accession=accession,
            assembly_record='https://www.ncbi.nlm.nih.gov/datasets/genome/' + accession + '/',
            source=meta['source'], release=62, chromosome_count=len(regions),
            provider_checksum_url=checksums_url, provider_checksum=' '.join(record),
            archive_sha256=meta['archive_sha256'], fixture_sha256=meta['fixture_sha256'],
            verified_original_feature_rows=row_count, status='PASS')
        print(name, accession, len(regions), row_count, 'PASS', flush=True)
    output = root / 'src/main/resources/data/reference-provenance.json'
    output.write_text(json.dumps(dict(audit_date=datetime.date.today().isoformat(),
                                     scope='Reference structure annotations only; no measured expression data bundled',
                                     datasets=reports), indent=2) + '\n')


if __name__ == '__main__':
    main()
