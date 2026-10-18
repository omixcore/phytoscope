package io.gitee.omixcore.phytoscope;

import io.gitee.omixcore.phytoscope.model.*;
import io.gitee.omixcore.phytoscope.research.*;
import io.gitee.omixcore.phytoscope.service.GeneSequenceService;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ResearchAnalysisTest {
    @Test void promoterWindowHonorsStrandAndSequenceEdge() throws Exception {
        var analysis = new IntervalAnalysisService();
        Gene plus = new Gene("g1", "g1", "chr1", 4, 10, '+');
        var p = analysis.promoterInterval(plus, 5, 2);
        assertEquals(1, p.start()); assertEquals(6, p.end());
        Gene minus = new Gene("g2", "g2", "chr1", 4, 10, '-');
        var m = analysis.promoterInterval(minus, 5, 2);
        assertEquals(8, m.start()); assertEquals(15, m.end());
        GeneSequenceService fasta = new GeneSequenceService(); fasta.loadFromContent(">chr1\nAACCGGTTAACCGGTT\n", "test");
        assertEquals(6, analysis.extract(p, fasta).sequence().length());
    }

    @Test void sequenceChecksAndProteinHitsRemainDescriptive() {
        var qc = new SequenceQualityService().assessCds("tx1", "ATGGCNTAA");
        assertEquals(1, qc.ambiguousBases()); assertEquals(100.0*3/9, qc.gcPercent(), 0.001);
        var hs = new ProteinHomologyService().compare(
                List.of(new ProteinHomologyService.Protein("A","a","MKWVTF")),
                List.of(new ProteinHomologyService.Protein("B","b","MKWVTF")), 50);
        assertEquals(1, hs.size()); assertEquals(100.0, hs.getFirst().identity()); assertTrue(hs.getFirst().reciprocalBestHit());
    }

    @Test void domainAminoAcidsMapAcrossCodingExons() {
        Gene gene = new Gene("g", "g", "chr1", 100, 220, '-');
        Transcript tx = new Transcript("tx", "chr1", 100, 220, '-'); tx.setProteinId("p");
        GenomicFeature first = new GenomicFeature("cds1", "chr1", FeatureType.CDS, 200, 208, '-');
        GenomicFeature second = new GenomicFeature("cds2", "chr1", FeatureType.CDS, 100, 108, '-');
        tx.addUnassignedSegment(first); tx.addUnassignedSegment(second); gene.addTranscript(tx);
        var domain = new DomainLinkService.Domain("p", "PF00001", 2, 4, "test");
        var link = new DomainLinkService().link(domain, gene).getFirst();
        assertEquals(List.of(new DomainLinkService.GenomicBlock("chr1", 200, 205, '-'),
                new DomainLinkService.GenomicBlock("chr1", 106, 108, '-')), link.blocks());
    }
}
