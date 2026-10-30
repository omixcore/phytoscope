package io.gitee.omixcore.phytoscope;

import io.gitee.omixcore.phytoscope.research.CandidateGeneReportService;
import io.gitee.omixcore.phytoscope.research.CandidateGeneReportService.CandidateGeneReport;
import io.gitee.omixcore.phytoscope.research.CandidateGeneReportService.EvidenceCard;
import io.gitee.omixcore.phytoscope.research.CandidateGeneReportService.OverlapKind;
import io.gitee.omixcore.phytoscope.research.GenomicInterval;
import io.gitee.omixcore.phytoscope.service.ExpressionDataService;
import io.gitee.omixcore.phytoscope.service.GenomeDataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CandidateGeneReportServiceTest {

    @TempDir Path temp;

    private static final String ANNOTATION = """
            ##gff-version 3
            ##sequence-region chr1 1 10000
            chr1\tsrc\tgene\t400\t600\t.\t+\t.\tID=g_full
            chr1\tsrc\tmRNA\t400\t600\t.\t+\t.\tID=t_full;Parent=g_full
            chr1\tsrc\texon\t400\t600\t.\t+\t.\tID=e_full;Parent=t_full
            chr1\tsrc\tCDS\t400\t599\t.\t+\t0\tParent=t_full
            chr1\tsrc\tgene\t950\t1050\t.\t+\t.\tID=g_partial
            chr1\tsrc\tmRNA\t950\t1050\t.\t+\t.\tID=t_partial;Parent=g_partial
            chr1\tsrc\texon\t950\t1050\t.\t+\t.\tID=e_partial;Parent=t_partial
            chr1\tsrc\tCDS\t950\t1050\t.\t+\t0\tParent=t_partial
            chr1\tsrc\tgene\t500\t600\t.\t-\t.\tID=g_minus
            chr1\tsrc\tmRNA\t500\t600\t.\t-\t.\tID=t_minus;Parent=g_minus
            chr1\tsrc\texon\t500\t600\t.\t-\t.\tID=e_minus;Parent=t_minus
            chr1\tsrc\tCDS\t500\t599\t.\t-\t0\tParent=t_minus
            chr1\tsrc\tgene\t700\t900\t.\t+\t.\tID=g_multi
            chr1\tsrc\tmRNA\t700\t900\t.\t+\t.\tID=t_multi1;Parent=g_multi
            chr1\tsrc\texon\t700\t800\t.\t+\t.\tID=e_m1a;Parent=t_multi1
            chr1\tsrc\texon\t850\t900\t.\t+\t.\tID=e_m1b;Parent=t_multi1
            chr1\tsrc\tmRNA\t700\t900\t.\t+\t.\tID=t_multi2;Parent=g_multi
            chr1\tsrc\texon\t700\t750\t.\t+\t.\tID=e_m2a;Parent=t_multi2
            chr1\tsrc\texon\t800\t900\t.\t+\t.\tID=e_m2b;Parent=t_multi2
            chr1\tsrc\tgene\t700\t900\t.\t+\t.\tID=g_missing
            """;

    private CandidateGeneReport report() throws Exception {
        var genome = new GenomeDataService();
        genome.loadFromContent(ANNOTATION, "candidate-fixture.gff3");

        Path matrix = temp.resolve("matrix.tsv");
        Files.writeString(matrix, """
                # unit=TPM
                # level=gene
                Gene\tRoot\tLeaf
                g_full\t1.5\t0
                g_minus\t3.0\t2.0
                g_multi\t0\t0
                """);
        var expression = new ExpressionDataService();
        expression.loadFromFile(matrix);
        expression.reconcile(genome);

        GenomicInterval interval = new GenomicInterval(null, null, null, "chr1", 400, 1000, '.');
        return new CandidateGeneReportService().report(interval, genome, expression);
    }

    @Test
    void distinguishesFullFromBoundaryPartialOverlap() throws Exception {
        var report = report();
        assertEquals(5, report.cards().size());

        var full = card(report, "g_full");
        assertEquals(OverlapKind.FULL, full.overlap().kind());
        assertEquals(400, full.overlap().start());
        assertEquals(600, full.overlap().end());
        assertEquals(201, full.overlap().length());

        var partial = card(report, "g_partial");
        assertEquals(OverlapKind.PARTIAL, partial.overlap().kind());
        assertEquals(950, partial.overlap().start());
        assertEquals(1000, partial.overlap().end());
        assertEquals(51, partial.overlap().length());
    }

    @Test
    void annotatesNegativeStrandAndMultipleTranscripts() throws Exception {
        var report = report();

        var minus = card(report, "g_minus");
        assertEquals('-', minus.strand());
        assertTrue(report.toMarkdown().contains("minus strand"));

        var multi = card(report, "g_multi");
        assertEquals(2, multi.transcripts().size());
        assertEquals("t_multi1", multi.transcripts().get(0).transcriptId());
        assertEquals("t_multi2", multi.transcripts().get(1).transcriptId());
    }

    @Test
    void distinguishesMeasuredZeroFromMissingExpression() throws Exception {
        var report = report();

        var zero = card(report, "g_full");
        assertTrue(zero.expression().present());
        assertEquals("g_full", zero.expression().inputId());
        var leaf = zero.expression().values().stream()
                .filter(v -> v.sample().equals("Leaf"))
                .findFirst().orElseThrow();
        assertFalse(leaf.missing());
        assertEquals(0.0, leaf.value(), 1e-9);

        var missing = card(report, "g_missing");
        assertFalse(missing.expression().present());
        assertNull(missing.expression().inputId());
        assertTrue(missing.expression().values().stream().allMatch(v -> v.missing()));
        assertTrue(report.toMarkdown().contains("no record (NA)"));
    }

    @Test
    void rendersCsvWithTraceableSourceLines() throws Exception {
        var report = report();
        String csv = report.toCsv();
        assertTrue(csv.contains("\"GeneID\""));
        assertTrue(csv.contains("\"g_full\""));
        assertTrue(csv.contains("\"g_missing\""));
        assertTrue(report.toMarkdown().contains("Source: line"));
    }

    private static EvidenceCard card(CandidateGeneReport report, String geneId) {
        return report.cards().stream()
                .filter(c -> geneId.equals(c.geneId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("missing card for " + geneId));
    }
}
