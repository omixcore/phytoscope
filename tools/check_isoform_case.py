#!/usr/bin/env python3
"""Reproduce the frozen case offline; fail on changed inputs or outcomes."""
import json
import subprocess
import sys
from pathlib import Path
from screen_domain_isoforms import content_sha256


def resolve(reference, name):
    """Map a manifest input name to the real file (plain or gzip form)."""
    plain = reference / name
    if plain.exists():
        return plain
    gz = reference / (name + '.gz')
    if gz.exists():
        return gz
    return plain


def main():
    root = Path(__file__).resolve().parents[1]
    case = root / 'examples/arabidopsis-isoform-validation'
    reference = root / 'src/main/resources/data/species/arabidopsis'
    out = root / 'target/independent-isoforms/reproduced'
    cache = root / 'target/independent-isoforms'
    manifest = json.loads((case / 'discovery-manifest.json').read_text())
    for name, expected in manifest['input_sha256'].items():
        path = case / name if (case / name).is_file() else resolve(reference, name)
        if content_sha256(path) != expected:
            raise ValueError(f'Frozen input changed: {name}')
    subprocess.run([sys.executable, str(root / 'tools/screen_domain_isoforms.py'),
                    '--reference', str(reference), '--uniprot', str(case / 'uniprot-domains.tsv'),
                    '--protocol', str(case / 'protocol.json'), '--out', str(out),
                    '--cache', str(cache)], check=True)
    for name in ['candidates.json', 'candidates.tsv', 'annotation.gff3', 'discovery-manifest.json']:
        if (case / name).read_bytes() != (out / name).read_bytes():
            raise ValueError(f'Screen result changed: {name}')
    subprocess.run([sys.executable, str(root / 'tools/validate_domain_isoforms.py'),
                    '--case', str(case), '--proteins', str(cache / 'specificity-proteins.json'),
                    '--out', str(out)], check=True)
    for name in ['source-peptides.tsv', 'peptide-evidence.tsv', 'validation.tsv']:
        if (case / name).read_bytes() != (out / name).read_bytes():
            raise ValueError(f'Validation result changed: {name}')
    expected = json.loads((case / 'validation-manifest.json').read_text())
    actual = json.loads((out / 'validation-manifest.json').read_text())
    # Full-archive validation and offline subset validation are distinguished.
    expected.pop('source_archive_verified_this_run')
    actual.pop('source_archive_verified_this_run')
    if actual != expected:
        raise ValueError('Validation manifest changed')
    print('PASS: fixed inputs, candidates, all peptide rows and failed outcomes reproduced')


if __name__ == '__main__':
    main()
