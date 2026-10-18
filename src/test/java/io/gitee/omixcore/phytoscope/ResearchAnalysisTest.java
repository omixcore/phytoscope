package io.gitee.omixcore.phytoscope;

import io.gitee.omixcore.phytoscope.model.*;
import io.gitee.omixcore.phytoscope.research.*;
import io.gitee.omixcore.phytoscope.service.GeneSequenceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ResearchAnalysisTest {
    @TempDir Path temp;
    @Test void promoterWindowHonorsStrandAndSequenceEdge() throws Exception {
        var analysis = new IntervalAnalysisService();
        Gene plus = new Gene("g1", "g1", "chr1", 4, 10, '+');
        var p = analysis.promoterInterval(plus, 5, 2, 16);
        assertEquals(1, p.start()); assertEquals(5, p.end());
        var interior = analysis.promoterInterval(new Gene("g1b", "g1b", "chr1", 8, 12, '+'), 5, 2, 30);
        assertEquals(7, interior.end() - interior.start() + 1);
        Gene minus = new Gene("g2", "g2", "chr1", 4, 10, '-');
        var m = analysis.promoterInterval(minus, 5, 2, 16);
        assertEquals(9, m.start()); assertEquals(15, m.end());
        GeneSequenceService fasta = new GeneSequenceService(); fasta.loadFromContent(">chr1\nAACCGGTTAACCGGTT\n", "test");
        assertEquals(5, analysis.extract(p, fasta).sequence().length());
        Gene nearEnd = new Gene("g3", "g3", "chr1", 12, 16, '-');
        var clipped = analysis.promoterInterval(nearEnd, 5, 2, 16);
        assertEquals(15, clipped.start()); assertEquals(16, clipped.end());
        assertEquals(2, fasta.extractPromoter(nearEnd, 5, 2).length());
    }

    @Test void sequenceChecksAndProteinHitsRemainDescriptive() {
        var qc = new SequenceQualityService().assessCds("tx1", "ATGGCNTAA");
        assertEquals(1, qc.ambiguousBases()); assertEquals(100.0*3/9, qc.gcPercent(), 0.001);
        var hs = new ProteinHomologyService().compare(
                List.of(new ProteinHomologyService.Protein("A","a","MKWVTF")),
                List.of(new ProteinHomologyService.Protein("B","b","MKWVTF")), 50);
        assertEquals(1, hs.size()); assertEquals(100.0, hs.getFirst().identity()); assertTrue(hs.getFirst().reciprocalBestHit());
    }

    @Test void sampleGroupingKeepsMeasuredZeroAndExcludesMissing() throws Exception {
        Path annotation = temp.resolve("annotation.gff3");
        Files.writeString(annotation, "c\ts\tgene\t1\t20\t.\t+\t.\tID=A\n");
        Path matrix = temp.resolve("expression.tsv");
        Files.writeString(matrix, "Gene\tRoot\tLeaf\tSeed\nA\t0\tNA\t9\n");
        var genome = new io.gitee.omixcore.phytoscope.service.GenomeDataService(); genome.loadFromFile(annotation);
        var expression = new io.gitee.omixcore.phytoscope.service.ExpressionDataService();
        expression.loadFromFile(matrix); expression.reconcile(genome);
        var groups = new SampleGroupingService(); groups.assign("Root", "vegetative"); groups.assign("Leaf", "vegetative");
        var summary = groups.summarize(expression, "A", SampleGroupingService.Statistic.MEAN);
        assertEquals(2, summary.size());
        assertEquals(1, summary.getFirst().n());
        assertEquals(0.0, summary.getFirst().value());
        assertEquals("Seed", summary.getLast().group());
        assertEquals(9.0, summary.getLast().value());
    }

    @Test void analysisExportIsAValidSingleRecordTsv() {
        assertEquals("Analysis\tResult\nQC output\tlength=4;\\nGC=50%\\tflag\n",
                io.gitee.omixcore.phytoscope.ui.ResearchAnalysisPane.formatTsv("QC\toutput\nlength=4;\nGC=50%\tflag"));
    }

    @Test void proteinComparisonUsesSpeciesQualifiedIdsAndCleanedCoverage() {
        var service = new ProteinHomologyService();
        var hits = service.compare(List.of(
                        new ProteinHomologyService.Protein("A", "same", "AAAA"),
                        new ProteinHomologyService.Protein("B", "same", "M-K")),
                List.of(new ProteinHomologyService.Protein("T", "t", "MK")), 50);
        assertEquals(1, hits.size());
        assertEquals("B", hits.getFirst().querySpecies());
        assertEquals(1.0, hits.getFirst().queryCoverage());
        assertEquals(1.0, hits.getFirst().targetCoverage());
        assertTrue(hits.getFirst().reciprocalBestHit());
        assertThrows(IllegalArgumentException.class, () -> service.compare(
                List.of(new ProteinHomologyService.Protein("A", "long", "M".repeat(2_000))),
                List.of(new ProteinHomologyService.Protein("B", "other", "M".repeat(2_000))), 0));
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

        tx.setStrand('.');
        var unknown = new DomainLinkService().link(domain, gene).getFirst();
        assertTrue(unknown.blocks().isEmpty());
        assertTrue(unknown.note().contains("strand is unknown"));
    }
}
