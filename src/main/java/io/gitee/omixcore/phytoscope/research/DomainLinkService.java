package io.gitee.omixcore.phytoscope.research;

import io.gitee.omixcore.phytoscope.model.*;
import java.util.*;

/** Links imported protein-domain coordinates to CDS genomic blocks. Coordinates are 1-based closed. */
public final class DomainLinkService {
    public record Domain(String proteinId, String name, int startAa, int endAa, String source) {
        public Domain { if (proteinId == null || proteinId.isBlank() || startAa < 1 || endAa < startAa) throw new IllegalArgumentException("Invalid domain record"); }
    }
    public record GenomicBlock(String sequence, int start, int end, char strand) {}
    public record Link(Domain domain, String transcriptId, List<GenomicBlock> blocks, String note) {
        public Link { blocks = List.copyOf(blocks); }
    }
    public List<Link> link(Domain d, Gene gene) {
        List<Link> out = new ArrayList<>();
        for (Transcript tx : gene.getTranscripts()) {
            if (!Objects.equals(d.proteinId(), tx.getProteinId())) continue;
            if (tx.getStrand() != '+' && tx.getStrand() != '-') {
                out.add(new Link(d, tx.getId(), List.of(), "Cannot map domain coordinates because transcript strand is unknown."));
                continue;
            }
            List<GenomicFeature> cds = new ArrayList<>(tx.getAllCdsSegments());
            cds.sort(tx.getStrand() == '-' ? Comparator.comparingInt(GenomicFeature::getStart).reversed() : Comparator.comparingInt(GenomicFeature::getStart));
            int from=(d.startAa()-1)*3+1, to=d.endAa()*3, offset=0;
            List<GenomicBlock> blocks=new ArrayList<>();
            for(GenomicFeature seg:cds){int len=seg.length(), lo=Math.max(from,offset+1), hi=Math.min(to,offset+len); if(hi>=lo){int a=lo-offset-1,b=hi-offset-1; int s,e; if(tx.getStrand()=='-'){s=seg.getEnd()-b;e=seg.getEnd()-a;}else{s=seg.getStart()+a;e=seg.getStart()+b;} blocks.add(new GenomicBlock(seg.getChromosome(),s,e,tx.getStrand()));} offset+=len;}
            String note=offset<to ? "Domain extends beyond annotated CDS; mapping is partial." : "Mapped by CDS nucleotide order; confirm protein isoform and domain coordinates.";
            out.add(new Link(d,tx.getId(),blocks,note));
        }
        return List.copyOf(out);
    }
}
