#!/usr/bin/env python3
"""Validate frozen domain-deletion candidates against published peptide tables.

Reads the source XLSX without modifying it. Does not re-search mass spectra or
estimate new FDRs. Peptide-level evidence and all failed candidates are retained.
"""
import argparse
import csv
import io
import json
import posixpath
import re
import zipfile
from collections import defaultdict
from pathlib import Path
from xml.etree import ElementTree as ET
from screen_domain_isoforms import sha256, write_tsv

NS = {'s': 'http://schemas.openxmlformats.org/spreadsheetml/2006/main'}
REL = '{http://schemas.openxmlformats.org/officeDocument/2006/relationships}id'
NEW_GROUPS = {'ap', 'wt', 'B1', 'B2', 'B3'}
ATLAS_GROUPS = set('CC10 CC3 CIM CLLF CP CT CTSAM EB FL FLPD HY IND LFD LFP LFPT ND P PT RKD2 RT RTTP RTUZ SCLF SD SDIMB SP SQ SQSP SQV ST'.split())


def xlsx_rows(data, sheet_name):
    """Read cached values from a named sheet; shared/inline strings supported."""
    with zipfile.ZipFile(io.BytesIO(data)) as z:
        strings = []
        if 'xl/sharedStrings.xml' in z.namelist():
            strings = [''.join(e.itertext()) for e in ET.fromstring(z.read('xl/sharedStrings.xml'))]
        rels = {e.attrib['Id']: e.attrib['Target']
                for e in ET.fromstring(z.read('xl/_rels/workbook.xml.rels'))}
        sheets = ET.fromstring(z.read('xl/workbook.xml')).find('s:sheets', NS)
        matches = [e for e in sheets if e.attrib['name'] == sheet_name]
        if len(matches) != 1:
            raise ValueError(f'Sheet not found: {sheet_name}')
        target = rels[matches[0].attrib[REL]]
        path = target.lstrip('/') if target.startswith('/') else posixpath.normpath('xl/' + target)
        for event, row in ET.iterparse(io.BytesIO(z.read(path)), events=('end',)):
            if row.tag != '{' + NS['s'] + '}row':
                continue
            values = {}
            for cell in row.findall('s:c', NS):
                col = 0
                for letter in re.match(r'[A-Z]+', cell.attrib['r'])[0]:
                    col = col * 26 + ord(letter) - 64
                node = cell.find('s:v', NS)
                value = '' if node is None else node.text or ''
                if cell.attrib.get('t') == 's':
                    value = strings[int(value)]
                elif cell.attrib.get('t') == 'inlineStr':
                    value = ''.join(cell.find('s:is', NS).itertext())
                values[col - 1] = value
            yield [values.get(i, '') for i in range(max(values, default=-1) + 1)]
            row.clear()


def peptide_rows(archive):
    with zipfile.ZipFile(archive) as z:
        filename = 'Supplemental Table 2 - Isoforms_v6.xlsx'
        data = z.read(filename)
    rows = xlsx_rows(data, '2 Isoform specific peptides')
    header = next(rows)
    for row_number, values in enumerate(rows, 2):
        record = dict(zip(header, values))
        record['source_excel_row'] = row_number
        yield record


def indistinguishable(sequence):
    return sequence.upper().replace('I', 'L')


def occurrences(peptide, protein):
    start = protein.find(peptide)
    while start >= 0:
        yield start
        start = protein.find(peptide, start + 1)


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--case', type=Path, required=True)
    p.add_argument('--archive', type=Path, help='Optional original archive; otherwise use the frozen all-candidate subset')
    p.add_argument('--proteins', type=Path, required=True)
    p.add_argument('--out', type=Path, help='Output directory; defaults to --case')
    args = p.parse_args()
    out = args.out or args.case
    out.mkdir(parents=True, exist_ok=True)
    sources = json.loads((args.case / 'sources.json').read_text())
    if sha256(args.proteins) != sources['specificity_background']['sha256']:
        raise ValueError('Specificity background differs from frozen reference reconstruction')
    manifest = json.loads((args.case / 'discovery-manifest.json').read_text())
    if sha256(args.case / 'candidates.json') != manifest['candidate_sha256']:
        raise ValueError('Candidate list differs from frozen discovery manifest')
    protocol = json.loads((args.case / 'protocol.json').read_text())
    if sha256(args.case / 'protocol.json') != manifest['input_sha256']['protocol.json']:
        raise ValueError('Protocol changed after screening')
    candidates = json.loads((args.case / 'candidates.json').read_text())
    gene_set = {c['gene'] for c in candidates}
    if args.archive:
        if sha256(args.archive) != sources['peptides']['archive_sha256']:
            raise ValueError('Peptide archive checksum differs from frozen source')
        selected = [r for r in peptide_rows(args.archive) if r['Gene'] in gene_set]
    else:
        subset = args.case / sources['peptides']['subset']
        if sha256(subset) != sources['peptides']['subset_sha256']:
            raise ValueError('Frozen peptide subset checksum mismatch')
        with subset.open() as handle:
            selected = list(csv.DictReader(handle, delimiter='\t'))
        for r in selected:
            r['source_excel_row'] = int(r['source_excel_row'])
    evidence = []
    for c in candidates:
        short, long = map(indistinguishable, (c['short_sequence'], c['canonical_sequence']))
        boundary = c['deletion_start_aa'] - 1
        for r in selected:
            if r['Gene'] != c['gene']:
                continue
            peptide = r['Peptide']
            if not re.fullmatch('[ACDEFGHIKLMNPQRSTVWY]+', peptide):
                raise ValueError(f'Unexpected peptide notation at row {r["source_excel_row"]}')
            normalized = indistinguishable(peptide)
            positions = list(occurrences(normalized, short))
            junctions = [i for i in positions if i + 2 <= boundary <= i + len(peptide) - 2]
            if not junctions:
                continue
            groups = set(r['Groups Detected In'].split(','))
            unknown = groups - NEW_GROUPS - ATLAS_GROUPS
            evidence.append(dict(candidate_id=c['candidate_id'], gene=c['gene'], peptide=peptide,
                                 source_excel_row=r['source_excel_row'], search=r['Search'],
                                 source_mapped_isoforms=r['Mapped Isoforms'], source_groups=r['Groups Detected In'],
                                 short_start_aa=junctions[0] + 1, short_end_aa=junctions[0] + len(peptide),
                                 short_position_count=len(positions), canonical_match=normalized in long,
                                 length_pass=len(peptide) >= protocol['validation_rules']['minimum_peptide_length_aa'],
                                 atlas_groups=sorted(groups & ATLAS_GROUPS), new_groups=sorted(groups & NEW_GROUPS),
                                 unresolved_groups=sorted(unknown)))
    # Compare each normalized peptide against every CDS model, including partial
    # translations. Shared peptides do not count as locus-specific evidence.
    background = json.loads(args.proteins.read_text())
    peptide_genes = {}
    for pep in {indistinguishable(e['peptide']) for e in evidence}:
        peptide_genes[pep] = sorted({m['gene'] for m in background.values()
                                     if pep in indistinguishable(m['protein'])})
    for e in evidence:
        genes = peptide_genes[indistinguishable(e['peptide'])]
        e['matching_genes'] = genes
        e['eligible'] = bool(e['length_pass'] and not e['canonical_match'] and
                             genes == [e['gene']] and not e['unresolved_groups'])
    paper_ids = args.case / sources['source_article']['gene_ids']
    if sha256(paper_ids) != sources['source_article']['gene_ids_sha256']:
        raise ValueError('Source article gene-ID snapshot mismatch')
    paper_genes = set(re.findall(r'AT[1-5]G\d{5}', paper_ids.read_text()))
    outcomes = []
    for c in candidates:
        eligible = [e for e in evidence if e['candidate_id'] == c['candidate_id'] and e['eligible']]
        distinct = {indistinguishable(e['peptide']) for e in eligible}
        atlas = sorted({e['peptide'] for e in eligible if e['atlas_groups']})
        new = sorted({e['peptide'] for e in eligible if e['new_groups']})
        passed = bool(atlas and new and len(distinct) >= protocol['validation_rules']['minimum_distinct_junction_peptides_total'])
        outcomes.append(dict(candidate_id=c['candidate_id'], gene=c['gene'], symbol=c['symbol'],
                             short_transcript=c['short_transcript'], domain=c['domain'],
                             eligible_distinct_peptides=len(distinct), atlas_peptides=atlas,
                             new_experiment_peptides=new, independent_support=passed,
                             discussed_in_source_article=c['gene'] in paper_genes,
                             status='independently_supported' if passed else 'insufficient_independent_evidence'))
    write_tsv(out / 'peptide-evidence.tsv', evidence,
              ['candidate_id', 'gene', 'peptide', 'source_excel_row', 'source_groups', 'search',
               'source_mapped_isoforms', 'short_start_aa', 'short_end_aa', 'canonical_match',
               'length_pass', 'matching_genes', 'atlas_groups', 'new_groups', 'unresolved_groups', 'eligible'])
    write_tsv(out / 'validation.tsv', outcomes,
              ['candidate_id', 'gene', 'symbol', 'short_transcript', 'domain', 'eligible_distinct_peptides',
               'atlas_peptides', 'new_experiment_peptides', 'independent_support',
               'discussed_in_source_article', 'status'])
    write_tsv(out / 'source-peptides.tsv', selected, list(selected[0]) if selected else ['Gene'])
    report = {'candidate_rows': len(candidates), 'candidate_genes': len(gene_set),
              'source_rows_for_candidate_genes': len(selected), 'junction_evidence_rows': len(evidence),
              'independently_supported_rows': sum(r['independent_support'] for r in outcomes),
              'independently_supported_genes': sorted({r['gene'] for r in outcomes if r['independent_support']}),
              'source_archive_sha256': sources['peptides']['archive_sha256'],
              'source_archive_verified_this_run': bool(args.archive),
              'source_article_gene_ids_sha256': sha256(paper_ids),
              'candidate_sha256': manifest['candidate_sha256'],
              'background_sha256': sha256(args.proteins), 'background_models': len(background),
              'novel_biological_discovery_established': False,
              'limitations': ['Published processed peptide identifications, not a new raw-spectrum search',
                              'Original biological datasets are independent; reprocessing is shared',
                              'Missing peptide detection is not evidence of absent isoform',
                              'A peptide supports a sequence fragment, not the entire intact isoform',
                              'Domain overlap predicts a structural change, not loss of measured function']}
    (out / 'validation-manifest.json').write_text(json.dumps(report, indent=2) + '\n')
    print(json.dumps(report, indent=2))


if __name__ == '__main__':
    main()
