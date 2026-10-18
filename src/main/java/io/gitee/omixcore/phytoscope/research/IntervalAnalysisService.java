package io.gitee.omixcore.phytoscope.research;

import io.gitee.omixcore.phytoscope.model.Gene;
import io.gitee.omixcore.phytoscope.service.GeneSequenceService;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Promoter windows and custom BED/GFF-style interval extraction. */
public final class IntervalAnalysisService {
    public record SequenceResult(GenomicInterval interval, String sequence, String source) {}
    public GenomicInterval promoterInterval(Gene gene, int upstream, int downstream) {
        if (upstream < 0 || downstream < 0 || upstream + downstream == 0) throw new IllegalArgumentException("Specify a non-empty promoter window");
        int tss = gene.getStrand() == '-' ? gene.getEnd() : gene.getStart();
        int start = gene.getStrand() == '-' ? tss - downstream : tss - upstream;
        int end = gene.getStrand() == '-' ? tss + upstream : tss + downstream;
        return new GenomicInterval("promoter:" + gene.getDisplayName(), gene.getSpecies(), null, gene.getChromosome(), Math.max(1,start), Math.max(1,end), gene.getStrand());
    }
    public SequenceResult extract(GenomicInterval interval, GeneSequenceService sequenceService) throws IOException {
        return new SequenceResult(interval, sequenceService.extractInterval(interval.sequence(), interval.start(), interval.end(), interval.strand()), sequenceService.getSourceLabel());
    }
    /** BED is 0-based half-open (converted on import); GFF/custom TSV is 1-based closed. */
    public List<GenomicInterval> read(Path path, boolean bed) throws IOException {
        List<GenomicInterval> out = new ArrayList<>(); int lineNo=0;
        for(String raw:Files.readAllLines(path)){lineNo++; String line=raw.trim(); if(line.isEmpty()||line.startsWith("#")||line.startsWith("track ")||line.startsWith("browser ")) continue;
            String[] c=line.split("\\t",-1); if(c.length<3) throw new IOException("Expected at least 3 tab-separated columns at line " + lineNo);
            try { int s=Integer.parseInt(c[1]), e=Integer.parseInt(c[2]); if(bed){if(s<0||e<=s)throw new IllegalArgumentException();s++;} char strand=c.length>5&&!c[5].isBlank()?c[5].charAt(0):'.';
                out.add(new GenomicInterval(c.length>3?c[3]:null,null,null,c[0],s,e,strand));
            } catch(RuntimeException ex){throw new IOException("Invalid interval at line " + lineNo + " ("+(bed?"BED 0-based half-open":"1-based closed")+")",ex);}
        }
        return List.copyOf(out);
    }
}
