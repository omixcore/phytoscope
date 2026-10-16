"""Independent source-to-model hierarchy/coordinate check, without importing Java parser code.
Compares multisets of gene/transcript/exon/CDS/UTR tuples, including each explicit
Parent edge. Unknown feature types are counted and reported, never silently treated
as validation of unsupported biology. No coordinate or identifier normalization.
"""
import argparse, collections, csv, gzip, json
from pathlib import Path
from urllib.parse import unquote
GENE_TYPES = {'gene','pseudogene','protein_coding_gene','ncrna_gene','lncrna_gene','mirna_gene','snrna_gene','snorna_gene','rrna_gene','transposable_element_gene'}
TX_TYPES = {'mrna','transcript','primary_transcript','ncrna','lncrna','lnc_rna','snrna','snorna','rrna','mirna_primary_transcript','mirna_primary_transcript_region','pseudogenic_transcript','trna','pseudogenic_trna','antisense_lncrna','antisense_rna','transcript_region','mirna','pre_mirna','srp_rna','rnase_mrp_rna','rnase_p_rna'}
UTR_TYPES = {'five_prime_utr','three_prime_utr','five_prime_utr_region','three_prime_utr_region',"5'utr","3'utr",'5utr','3utr','5_prime_utr','3_prime_utr'}

def inventory(sources):
    genes, transcripts, children, ignored = {}, {}, [], collections.Counter()
    for source in sources:
        opener = gzip.open if source.suffix == '.gz' else open
        with opener(source,'rt') as stream:
            for line in stream:
                if line.startswith('##FASTA'): break
                if not line.strip() or line.startswith('#'): continue
                f=line.rstrip('\n').split('\t')
                if len(f)!=9: raise ValueError(f'Malformed source row in {source}')
                feature=(f[0],f[6],int(f[3]),int(f[4])); kind=f[2].lower()
                if kind not in GENE_TYPES | TX_TYPES | UTR_TYPES | {'exon','pseudogenic_exon','cds'}:
                    ignored[f[2]]+=1; continue
                # Coordinates and hierarchy require only ID/Parent, not long prose attributes.
                attrs={}
                for token in f[8].split(';'):
                    if token.startswith('ID='): attrs['ID']=unquote(token[3:])
                    elif token.startswith('Parent='): attrs['Parent']=token[7:]
                parents=[unquote(parent) for parent in attrs.get('Parent','').split(',')]
                if kind in GENE_TYPES: genes[attrs['ID']]=feature
                elif kind in TX_TYPES: transcripts[attrs['ID']]=(parents,feature,kind)
                elif kind in {'exon','pseudogenic_exon','cds'}|UTR_TYPES:
                    children.append((parents, 'CDS' if kind=='cds' else 'UTR' if kind in UTR_TYPES else 'exon', feature))
                else: ignored[f[2]]+=1
    expected=collections.Counter()
    for gid,(seq,strand,start,end) in genes.items(): expected[(gid,'',seq,strand,'gene',start,end)]+=1
    tx_to_genes={}
    for tid,(parents,feature,kind) in transcripts.items():
        linked=[p for p in parents if p in genes]
        if not linked and kind=='mirna': ignored['miRNA']+=1; continue
        if not linked: raise ValueError(f'Transcript without explicit source gene: {tid}')
        tx_to_genes[tid]=linked
        for gid in linked:
            seq,strand,start,end=feature; expected[(gid,tid,seq,strand,'transcript',start,end)]+=1
    for parents,kind,(seq,strand,start,end) in children:
        for tid in parents:
            if tid not in tx_to_genes: raise ValueError(f'Unresolved source feature parent: {tid}')
            for gid in tx_to_genes[tid]: expected[(gid,tid,seq,strand,kind,start,end)]+=1
    return expected,ignored

def validate(sources, parsed):
    expected,ignored=inventory(sources)
    actual=collections.Counter()
    with parsed.open() as stream:
        for row in csv.DictReader(stream,delimiter='\t'):
            actual[(row['GeneID'],row['TranscriptID'],row['Sequence'],row['Strand'],row['Feature'],int(row['Start']),int(row['End']))]+=1
    missing,extra=expected-actual,actual-expected
    result={'status':'PASS' if not missing and not extra else 'FAIL','expected_records':expected.total(),'parsed_records':actual.total(),
        'missing_records':missing.total(),'extra_records':extra.total(),'missing_examples':list(missing.items())[:10],'extra_examples':list(extra.items())[:10],
        'unsupported_source_types':dict(ignored),'scope':'Explicit GFF3 parent edges and exact 1-based inclusive coordinates; unsupported types excluded and reported.'}
    return result

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--source',type=Path,nargs='+',required=True);parser.add_argument('--parsed',type=Path,required=True);parser.add_argument('--out',type=Path,required=True)
    a=parser.parse_args(); result=validate(a.source,a.parsed);a.out.parent.mkdir(parents=True,exist_ok=True);a.out.write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result));raise SystemExit(result['status']!='PASS')
