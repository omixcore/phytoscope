"""Register the mint nuclear-chromosome example in reference-provenance.json.

Renames the existing 'mint' (plastid) block to 'mint-plastid' and inserts the
new 'mint' (Mentha longifolia CMEN585 v3.01b nuclear chromosomes) block.
Idempotent: re-running refreshes the nuclear block.
"""
import json
from collections import OrderedDict

PATH = 'src/main/resources/data/reference-provenance.json'

NUCLEAR = OrderedDict([
    ('species', 'Mentha longifolia'),
    ('assembly', 'Mlong_CMEN585_v3.01b chromosome-scale assembly'),
    ('assembly_accession', 'GCA_001642375.2 (CMEN585 v3)'),
    ('assembly_record', 'https://www.ebi.ac.uk/ena/browser/view/GCA_001642375'),
    ('source', 'http://langelabtools.wsu.edu/mgr/sites/default/files/downloads/Mlong585_v3.01b-maker.gff'),
    ('source_provider', 'WSU Mint Genome Resources (Lange lab); same assembly deposited at INSDC'),
    ('source_release', 'Mlong_CMEN585_v3.01b (WSU MGR download 2026-10-28)'),
    ('sequence_count', 12),
    ('sequence_lengths', OrderedDict([
        ('Chr1', 46699537), ('Chr2', 45526029), ('Chr3', 45460755),
        ('Chr4', 44284954), ('Chr5', 43231337), ('Chr6', 37537474),
        ('Chr7', 36621300), ('Chr8', 36842082), ('Chr9', 33738042),
        ('Chr10', 33088173), ('Chr11', 29943784), ('Chr12', 29660084),
    ])),
    ('archive_sha256', '1da307a82d870b1be8468fd837c1b94ebcb4c3eb3d111fec85c4c07d377837e2'),
    ('fixture_sha256', 'edd9ae3583e153d3877952615639eeb7f0d86a0fd2bf83f027596c40c5812c08'),
    ('verified_original_feature_rows', 377),
    ('verified_feature_breakdown', OrderedDict([
        ('gene', 24), ('mRNA', 24), ('exon', 157), ('CDS', 156),
        ('five_prime_UTR', 6), ('three_prime_UTR', 10),
    ])),
    ('transformation', 'Gene coordinates, IDs and hierarchy rows are copied verbatim from the Maker GFF3. '
                       'The first two gene families per chromosome-scale scaffold (file order) plus their full '
                       'descendant closure are kept. Sequence names are mapped scaffold_N_length_L -> ChrN '
                       '(N = 1..12, the embedded length is the true sequence length); the original scaffold '
                       'names are preserved in the GFF3 header. The 417 unplaced scaffolds are excluded.'),
    ('generator', 'tools/build_mint_nuclear_example.py'),
    ('annotation_scope', '12 chromosome-scale sequences (29.7-46.7 Mb), 24 Maker-predicted genes '
                         '(2 real families per chromosome)'),
    ('expression_resource', None),
    ('notes', 'No verified expression matrix is bundled; the browser shows an explicit missing-data state.'),
    ('status', 'PASS'),
])

with open(PATH, encoding='utf-8') as fh:
    doc = json.load(fh, object_pairs_hook=OrderedDict)

ds = doc['datasets']
if 'mint' in ds and 'mint-plastid' not in ds:
    plastid = ds.pop('mint')
    plastid['annotation_scope'] = 'complete plastid genome, single sequence Pt (152,132 bp)'
    ds['mint-plastid'] = plastid
ds['mint'] = NUCLEAR

with open(PATH, 'w', encoding='utf-8', newline='\n') as fh:
    json.dump(doc, fh, ensure_ascii=False, indent=2)
    fh.write('\n')
print('datasets:', list(ds.keys()))
