#!/usr/bin/env python3
"""Reproduce the frozen case offline; fail on changed inputs or outcomes.

The reference genome/annotation inputs were migrated out of the main repository
into ``phytoscope-reference-data``. CI clones that archive into
``target/reference-data``; the Arabidopsis directory is therefore configurable
instead of pointing at the now-empty bundled path.
"""
import argparse
import json
import os
import subprocess
import sys
from pathlib import Path
from screen_domain_isoforms import content_sha256


# Repo-relative default; matches the CI clone target (validate.yml:31).
DEFAULT_REFERENCE = "target/reference-data/data/species/arabidopsis"


def resolve(reference, name):
    """Map a manifest input name to the real file (plain or gzip form)."""
    plain = reference / name
    if plain.exists():
        return plain
    gz = reference / (name + '.gz')
    if gz.exists():
        return gz
    return plain


def canonical_text(path: Path) -> str:
    """Read a text artifact with platform line endings normalised to LF.

    The frozen case files were committed with LF, while Python's ``write_text``
    emits CRLF on Windows. Comparing canonical text keeps the content check
    strict without making the result depend on the host OS.
    """
    return path.read_text(encoding='utf-8').replace('\r\n', '\n')


def parse_args(argv=None):
    """Parse the reference-data location, allowing an environment override."""
    parser = argparse.ArgumentParser(
        description="Reproduce the frozen isoform screen against migrated reference data.")
    parser.add_argument(
        "--reference",
        default=os.environ.get("PHYTOSCOPE_REFERENCE_DATA_DIR", DEFAULT_REFERENCE),
        help="Arabidopsis reference directory (plain or .gz inputs). "
             "Defaults to %s; PHYTOSCOPE_REFERENCE_DATA_DIR overrides the default."
             % DEFAULT_REFERENCE,
    )
    return parser.parse_args(argv)


def main(argv=None):
    args = parse_args(argv)
    root = Path(__file__).resolve().parents[1]
    case = root / 'examples/arabidopsis-isoform-validation'
    reference = Path(args.reference)
    if not reference.is_absolute():
        reference = root / reference
    out = root / 'target/independent-isoforms/reproduced'
    cache = root / 'target/independent-isoforms'

    # Fail fast before resolve(): the old bundled path was emptied when the
    # reference data was migrated, so an uncaught FileNotFoundError would be
    # misleading. Report the actionable clone command instead.
    if not reference.is_dir():
        raise SystemExit(
            f"[guard] 外迁参考数据目录不存在: {reference}；"
            "请先取回 phytoscope-reference-data"
            "（`git clone --depth 1 https://gitee.com/omixcore/phytoscope-reference-data.git "
            "target/reference-data`）"
        )

    manifest = json.loads((case / 'discovery-manifest.json').read_text())
    for name, expected in manifest['input_sha256'].items():
        path = case / name if (case / name).is_file() else resolve(reference, name)
        if content_sha256(path) != expected:
            raise ValueError(f'Frozen input changed: {name}')
    subprocess.run([sys.executable, str(root / 'tools/screen_domain_isoforms.py'),
                    '--reference', str(reference), '--uniprot', str(case / 'uniprot-domains.tsv'),
                    '--protocol', str(case / 'protocol.json'), '--out', str(out),
                    '--cache', str(cache)], check=True)
    # candidates.json is compared by JSON semantics (json.loads), so CRLF/LF
    # line endings and key order do not produce a spurious mismatch. It used to
    # be a byte comparison, which failed on Windows even though the 16 candidate
    # records were identical (B01b-2).
    expected_candidates = json.loads(canonical_text(case / 'candidates.json'))
    actual_candidates = json.loads(canonical_text(out / 'candidates.json'))
    if actual_candidates != expected_candidates:
        raise ValueError('Screen result changed: candidates.json')
    # The TSV byte comparison and the input_sha256 guard above stay unchanged.
    if (case / 'candidates.tsv').read_bytes() != (out / 'candidates.tsv').read_bytes():
        raise ValueError('Screen result changed: candidates.tsv')
    # Remaining screen artifacts are still compared strictly, but LF-normalised
    # so Windows and Linux agree on the same frozen bytes.
    for name in ['annotation.gff3', 'discovery-manifest.json']:
        if canonical_text(case / name) != canonical_text(out / name):
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
