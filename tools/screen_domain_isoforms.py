#!/usr/bin/env python3
"""Screen Araport11 isoforms without looking at peptide validation outcomes.

Uses the bundled genomic reference and a frozen UniProt TSV snapshot. All
coordinates in output tables are 1-based, inclusive. Standard library only.
"""
import argparse
import csv
import gzip
import hashlib
import json
import re
from collections import Counter, defaultdict
from pathlib import Path
from urllib.parse import unquote


def sha256(path):
    h = hashlib.sha256()
    with Path(path).open('rb') as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b''):
            h.update(chunk)
    return h.hexdigest()


def fasta(path):
    result = {}
    opener = gzip.open if str(path).endswith('.gz') else open
    with opener(path, 'rt') as handle:
        name, parts = None, []
        for line in handle:
            if line.startswith('>'):
                if name is not None:
                    result[name] = ''.join(parts).upper()
                name, parts = line[1:].split()[0], []
            else:
                parts.append(line.strip())
        if name is not None:
            result[name] = ''.join(parts).upper()
    return result


CODONS = dict(zip(
    (a + b + c for a in 'TCAG' for b in 'TCAG' for c in 'TCAG'),
    'FFLLSSSSYY**CC*WLLLLPPPPHHQQRRRRIIIMTTTTNNKKSSRRVVVVAAAADDEEGGGG'))
COMPLEMENT = str.maketrans('ACGTN', 'TGCAN')


def translate_complete(dna):
    if len(dna) % 3 or not dna.startswith('ATG'):
        return None
    protein = ''.join(CODONS.get(dna[i:i + 3], 'X') for i in range(0, len(dna), 3))
    if not protein.endswith('*') or '*' in protein[:-1] or 'X' in protein:
        return None
    return protein[:-1]


def load_models(directory):
    genomes = fasta(directory / 'TAIR10_genomic.fna.gz')
    genes, models = {}, {}
    for chromosome in range(1, 6):
        with (directory / f'Chr{chromosome}.gff3').open() as handle:
            for line in handle:
                if line.startswith('#') or not line.strip():
                    continue
                c = line.rstrip('\n').split('\t')
                attrs = {k: unquote(v) for k, v in
                         (s.split('=', 1) for s in c[8].split(';') if '=' in s)}
                if c[2] == 'gene':
                    genes[attrs['ID']] = {'symbol': attrs.get('symbol', ''),
                                           'description': attrs.get('Note', ''),
                                           'chromosome': c[0], 'strand': c[6]}
                elif c[2] == 'mRNA':
                    models[attrs['ID']] = {'gene': attrs['Parent'], 'chromosome': c[0],
                                            'strand': c[6], 'cds': []}
                elif c[2] == 'CDS':
                    for parent in attrs.get('Parent', '').split(','):
                        if parent not in models:
                            raise ValueError(f'CDS before/unmatched mRNA: {parent}')
                        models[parent]['cds'].append([int(c[3]), int(c[4]), int(c[7])])
    complete, rejected, specificity = {}, Counter(), {}
    for tid, m in models.items():
        blocks = sorted(m['cds'], reverse=m['strand'] == '-')
        offset, parts, valid = 0, [], True
        for start, end, phase in blocks:
            if phase != (3 - offset % 3) % 3:
                valid = False
            seq = genomes[m['chromosome'].removeprefix('Chr')][start - 1:end]
            parts.append(seq.translate(COMPLEMENT)[::-1] if m['strand'] == '-' else seq)
            offset += end - start + 1
        dna = ''.join(parts)
        # Include partial CDS models in the peptide-specificity background.
        # Use all frames conservatively when annotation phases disagree.
        for frame in ([0] if valid else range(3)):
            protein_bg = ''.join(CODONS.get(dna[i:i + 3], 'X')
                                 for i in range(frame, len(dna) - 2, 3))
            specificity[f'{tid}:frame{frame}'] = {'gene': m['gene'], 'protein': protein_bg}
        if not valid:
            rejected['incomplete_or_inconsistent_phase'] += 1
            continue
        protein = translate_complete(dna)
        if protein is None:
            rejected['incomplete_or_noncanonical_cds'] += 1
            continue
        complete[tid] = dict(m, cds=blocks, protein=protein)
    return genes, complete, dict(rejected), specificity


def deletion_positions(long, short, flank=10):
    """Return all exact internal single-deletion placements (zero-based)."""
    if len(long) <= len(short):
        return []
    prefix = 0
    while prefix < len(short) and long[prefix] == short[prefix]:
        prefix += 1
    suffix = 0
    while suffix < len(short) and long[-suffix - 1] == short[-suffix - 1]:
        suffix += 1
    delta = len(long) - len(short)
    return [i for i in range(max(flank, len(short) - suffix),
                            min(prefix, len(short) - flank) + 1)
            if long[:i] + long[i + delta:] == short]


def amino_blocks(model, start_aa, end_aa):
    lo, hi, offset, out = (start_aa - 1) * 3, end_aa * 3, 0, []
    for start, end, _ in model['cds']:
        length = end - start + 1
        a, b = max(lo, offset), min(hi, offset + length)
        if a < b:
            if model['strand'] == '-':
                out.append([end - (b - offset) + 1, end - (a - offset)])
            else:
                out.append([start + a - offset, start + b - offset - 1])
        offset += length
    if sum(b - a + 1 for a, b in out) != (end_aa - start_aa + 1) * 3:
        raise ValueError('Domain/deletion beyond CDS')
    return out


def write_tsv(path, rows, fields):
    with path.open('w', newline='') as handle:
        writer = csv.DictWriter(handle, fieldnames=fields, delimiter='\t', extrasaction='ignore')
        writer.writeheader()
        for row in rows:
            writer.writerow({k: json.dumps(v, separators=(',', ':')) if isinstance(v, (list, dict))
                             else v for k, v in row.items() if k in fields})


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--reference', type=Path, required=True)
    parser.add_argument('--uniprot', type=Path, required=True)
    parser.add_argument('--protocol', type=Path, required=True)
    parser.add_argument('--out', type=Path, required=True)
    parser.add_argument('--cache', type=Path, required=True)
    args = parser.parse_args()
    rule = json.loads(args.protocol.read_text())['discovery_rules']
    genes, models, rejected, specificity = load_models(args.reference)
    grouped = defaultdict(list)
    for tid, model in models.items():
        grouped[model['gene']].append(tid)
    candidates, counts = [], Counter()
    with args.uniprot.open() as handle:
        for record in csv.DictReader(handle, delimiter='\t'):
            counts['uniprot_records'] += 1
            agis = set(x.upper() for x in re.findall(r'AT[1-5]G\d{5}', record['Gene Names'], re.I))
            if len(agis) != 1:
                continue
            gene = next(iter(agis))
            if gene not in genes or len(grouped[gene]) < 2:
                continue
            if re.search(r'glycine (?:cleavage|decarboxylase)', genes[gene]['description'], re.I):
                continue
            raw_sequence = record.get('Sequence')
            sequence_digest = (hashlib.sha256(raw_sequence.encode()).hexdigest() if raw_sequence
                               else record['Sequence SHA256'])
            canonical = sorted(t for t in grouped[gene]
                               if hashlib.sha256(models[t]['protein'].encode()).hexdigest() == sequence_digest)
            if not canonical:
                continue
            long = models[canonical[0]]['protein']
            if len(long) < rule['minimum_canonical_length_aa']:
                continue
            counts['canonical_matched_genes'] += 1
            domains = list(re.finditer(r'DOMAIN (\d+)\.\.(\d+); /note="([^"]+)"', record['Domain [FT]']))
            for tid in sorted(grouped[gene]):
                short = models[tid]['protein']
                delta = len(long) - len(short)
                if len(short) < rule['minimum_short_length_aa'] or delta < rule['minimum_deleted_aa']:
                    continue
                positions = deletion_positions(long, short, rule['minimum_identical_flank_aa'])
                if len(positions) > 1:
                    counts['ambiguous_deletion_pairs_rejected'] += 1
                if len(positions) != 1:
                    continue
                start, end = positions[0] + 1, positions[0] + delta
                for d in domains:
                    a, b, name = int(d[1]), int(d[2]), d[3]
                    overlap = max(0, min(end, b) - max(start, a) + 1)
                    fraction = overlap / (b - a + 1)
                    if overlap < rule['minimum_domain_overlap_aa'] or fraction < rule['minimum_domain_fraction_removed']:
                        continue
                    candidates.append(dict(
                        gene=gene, symbol=genes[gene]['symbol'], description=genes[gene]['description'],
                        accession=record['Entry'], canonical_transcript=canonical[0],
                        equivalent_canonical_transcripts=canonical, short_transcript=tid,
                        chromosome=models[tid]['chromosome'], strand=models[tid]['strand'],
                        canonical_length_aa=len(long), short_length_aa=len(short),
                        deletion_start_aa=start, deletion_end_aa=end, deletion_length_aa=delta,
                        domain=name, domain_start_aa=a, domain_end_aa=b,
                        domain_overlap_aa=overlap, domain_fraction_removed=fraction,
                        deletion_genomic_blocks=amino_blocks(models[canonical[0]], start, end),
                        domain_genomic_blocks=amino_blocks(models[canonical[0]], a, b),
                        uniprot_domain_features=record['Domain [FT]'],
                        canonical_sequence=long, short_sequence=short))
    candidates.sort(key=lambda r: (-r['domain_fraction_removed'], -r['domain_overlap_aa'],
                                  r['gene'], r['short_transcript'], r['domain_start_aa']))
    for i, row in enumerate(candidates, 1):
        row['candidate_id'] = f'DI{i:04d}'
    args.out.mkdir(parents=True, exist_ok=True)
    args.cache.mkdir(parents=True, exist_ok=True)
    (args.cache / 'annotated-proteins.json').write_text(json.dumps(models))
    (args.cache / 'specificity-proteins.json').write_text(json.dumps(specificity))
    (args.out / 'candidates.json').write_text(json.dumps(candidates, indent=2) + '\n')
    # Preserve complete, unmodified source rows for all candidate loci so that
    # the case can be opened in the desktop and tested without a full reimport.
    selected_genes = {c['gene'] for c in candidates}
    retained_ids = set(selected_genes)
    excerpt = ['##gff-version 3\n', '##species=Arabidopsis thaliana\n',
               '##assembly=TAIR10 / Araport11\n']
    for chromosome in range(1, 6):
        with (args.reference / f'Chr{chromosome}.gff3').open() as handle:
            for line in handle:
                if line.startswith('#'):
                    continue
                c = line.rstrip('\n').split('\t')
                if len(c) != 9:
                    continue
                attrs = dict(s.split('=', 1) for s in c[8].split(';') if '=' in s)
                if attrs.get('ID') in selected_genes or retained_ids.intersection(attrs.get('Parent', '').split(',')):
                    excerpt.append(line)
                    if 'ID' in attrs:
                        retained_ids.add(attrs['ID'])
    (args.out / 'annotation.gff3').write_text(''.join(excerpt))
    fields = ['candidate_id', 'gene', 'symbol', 'accession', 'canonical_transcript', 'short_transcript',
              'canonical_length_aa', 'short_length_aa', 'deletion_start_aa', 'deletion_end_aa',
              'domain', 'domain_start_aa', 'domain_end_aa', 'domain_overlap_aa', 'domain_fraction_removed',
              'chromosome', 'strand', 'deletion_genomic_blocks', 'domain_genomic_blocks']
    write_tsv(args.out / 'candidates.tsv', candidates, fields)
    inputs = [args.protocol, args.uniprot, args.reference / 'TAIR10_genomic.fna.gz']
    inputs.extend(args.reference / f'Chr{i}.gff3' for i in range(1, 6))
    report = {'protocol_version': 1, 'complete_cds_models': len(models),
              'rejected_cds': rejected, 'specificity_background_models': len(specificity),
              'screen_counts': dict(counts),
              'candidate_rows': len(candidates), 'candidate_genes': len({c['gene'] for c in candidates}),
              'input_sha256': {p.name: sha256(p) for p in inputs},
              'candidate_sha256': sha256(args.out / 'candidates.json'),
              'validation_peptides_used_for_screening': False}
    (args.out / 'discovery-manifest.json').write_text(json.dumps(report, indent=2) + '\n')
    print(json.dumps(report, indent=2))


if __name__ == '__main__':
    main()
