package io.gitee.omixcore.phytoscope;

import io.gitee.omixcore.phytoscope.parser.Gff3Parser;
import io.gitee.omixcore.phytoscope.parser.ParseDiagnostics;
import io.gitee.omixcore.phytoscope.research.CandidateGeneReportService;
import io.gitee.omixcore.phytoscope.research.GenomicInterval;
import io.gitee.omixcore.phytoscope.service.AnnotationHealthService;
import io.gitee.omixcore.phytoscope.service.AnnotationHealthService.IssueCode;
import io.gitee.omixcore.phytoscope.service.AnnotationHealthService.Severity;
import io.gitee.omixcore.phytoscope.service.ExpressionDataService;
import io.gitee.omixcore.phytoscope.service.GenomeDataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression coverage for annotation and expression edge cases.
 */
class QaEdgeCaseTest {

    @TempDir Path temp;

    // ------------------------------------------------------------------

    // ------------------------------------------------------------------

    @Test
    void cdsMissingStartCodonWithStopCodonIsIncompleteNotTrueAnomaly() throws Exception {
        var genome = new GenomeDataService();
        genome.loadFromContent("""
                ##gff-version 3
                ##sequence-region chr1 1 1000
                chr1\tsrc\tgene\t10\t30\t.\t+\t.\tID=g1
                chr1\tsrc\tmRNA\t10\t30\t.\t+\t.\tID=t1;Parent=g1
                chr1\tsrc\texon\t10\t30\t.\t+\t.\tID=e1;Parent=t1
                chr1\tsrc\tCDS\t10\t17\t.\t+\t0\tParent=t1
                chr1\tsrc\tstop_codon\t15\t17\t.\t+\t0\tParent=t1
                """, "missing-atg.gff3");

        var report = new AnnotationHealthService().assess(genome, new ExpressionDataService());

        assertTrue(report.issues().stream().anyMatch(issue ->
                        issue.code() == IssueCode.CDS_INCOMPLETE
                                && issue.severity() == Severity.WARNING
                                && "g1".equals(issue.geneId())),
                "");
        assertFalse(report.issues().stream().anyMatch(issue ->
                        issue.code() == IssueCode.CDS_TRUE_ANOMALY && "g1".equals(issue.geneId())),
                "");
    }

    // ------------------------------------------------------------------

    // ------------------------------------------------------------------

    @Test
    void dotPhaseIsLegalUnknownButInvalidPhasesAreWarned() throws Exception {
        var genome = new GenomeDataService();
        genome.loadFromContent("""
                ##gff-version 3
                ##sequence-region chr1 1 1000
                chr1\tsrc\tgene\t10\t200\t.\t+\t.\tID=g_dot
                chr1\tsrc\tmRNA\t10\t200\t.\t+\t.\tID=t_dot;Parent=g_dot
                chr1\tsrc\texon\t10\t200\t.\t+\t.\tID=e_dot;Parent=t_dot
                chr1\tsrc\tCDS\t10\t99\t.\t+\t.\tParent=t_dot
                chr1\tsrc\tgene\t10\t200\t.\t+\t.\tID=g_bad
                chr1\tsrc\tmRNA\t10\t200\t.\t+\t.\tID=t_bad;Parent=g_bad
                chr1\tsrc\texon\t10\t200\t.\t+\t.\tID=e_bad;Parent=t_bad
                chr1\tsrc\tCDS\t10\t99\t.\t+\t3\tParent=t_bad
                chr1\tsrc\tCDS\t10\t99\t.\t+\t-1\tParent=t_bad
                chr1\tsrc\tCDS\t10\t99\t.\t+\tX\tParent=t_bad
                """, "phase.gff3");

        var report = new AnnotationHealthService().assess(genome, new ExpressionDataService());

        assertFalse(report.issues().stream().anyMatch(issue ->
                        issue.code() == IssueCode.INVALID_PHASE && "g_dot".equals(issue.geneId())),
                "");

        long badPhaseCount = report.issues().stream()
                .filter(issue -> issue.code() == IssueCode.INVALID_PHASE && "g_bad".equals(issue.geneId()))
                .count();
        assertEquals(3, badPhaseCount, "");
    }

    // ------------------------------------------------------------------

    // ------------------------------------------------------------------

    @Test
    void negativeStrandReportKeepsGenomicCoordinatesAndExactSourceLine() throws Exception {
        String annotation = """
                ##gff-version 3
                ##sequence-region chr1 1 10000
                chr1\tsrc\tgene\t500\t600\t.\t-\t.\tID=g_minus
                chr1\tsrc\tmRNA\t500\t600\t.\t-\t.\tID=t_minus;Parent=g_minus
                chr1\tsrc\texon\t500\t600\t.\t-\t.\tID=e_minus;Parent=t_minus
                chr1\tsrc\tCDS\t500\t599\t.\t-\t0\tParent=t_minus
                """;
        var genome = new GenomeDataService();
        genome.loadFromContent(annotation, "minus.gff3");


        var expression = new ExpressionDataService();
        expression.loadFromFile(writeMatrix("""
                # unit=TPM
                # level=gene
                Gene\tRoot
                g_other\t2.0
                """));
        expression.reconcile(genome);

        var report = new CandidateGeneReportService().report(
                new GenomicInterval(null, null, null, "chr1", 400, 1000, '.'),
                genome, expression);

        assertEquals(1, report.cards().size());
        var card = report.cards().getFirst();
        assertEquals('-', card.strand(), "");

        assertEquals(500, card.overlap().start());
        assertEquals(600, card.overlap().end());
        assertEquals(101, card.overlap().length());


        int geneLine = lineNumber(annotation, "ID=g_minus");
        assertEquals(geneLine, card.source().geneLineNumber(), "");


        assertFalse(card.expression().present(), "");
        assertNull(card.expression().inputId());
        assertTrue(card.expression().values().stream().allMatch(v -> v.missing()));

        String csv = report.toCsv();
        assertTrue(csv.contains("NA"), "");
        assertTrue(csv.contains("-"), "");
    }

    // ------------------------------------------------------------------

    // ------------------------------------------------------------------

    @Test
    void skippedRowsStaysConsistentAfterReplaceWith() throws Exception {
        var withSkips = new GenomeDataService();
        withSkips.loadFromContent("##gff-version 3\nbroken\nbroken\n", "skips");
        assertTrue(withSkips.getSkippedRows() > 0, "");

        var clean = new GenomeDataService();
        clean.loadFromContent("##gff-version 3\n##sequence-region chr1 1 10\n"
                + "chr1\ts\tgene\t1\t10\t.\t+\t.\tID=g1\n", "clean");
        assertEquals(0, clean.getSkippedRows());

        clean.replaceWith(withSkips);

        assertEquals(withSkips.getDiagnostics().skippedRowTotal(),
                clean.getDiagnostics().skippedRowTotal(),
                "");
        assertEquals(withSkips.getSkippedRows(), clean.getSkippedRows(),
                "");
    }

    // ------------------------------------------------------------------

    // ------------------------------------------------------------------

    @Test
    void parseResultFourArgConstructorDefaultsToEmptyDiagnostics() {
        var result = new Gff3Parser.ParseResult(
                java.util.List.of(), 0, io.gitee.omixcore.phytoscope.parser.AnnotationFormat.GFF3,
                io.gitee.omixcore.phytoscope.model.GenomeMetadata.fromAnnotation(
                        null, null, "", "empty", java.util.List.of()));

        assertNotNull(result.diagnostics(), "");
        assertEquals(ParseDiagnostics.empty(), result.diagnostics(), "");
        assertEquals(0, result.diagnostics().skippedRowTotal());
        assertTrue(result.diagnostics().orphans().isEmpty());
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private Path writeMatrix(String content) throws Exception {
        Path matrix = temp.resolve("matrix-" + System.nanoTime() + ".tsv");
        Files.writeString(matrix, content);
        return matrix;
    }

    private static int lineNumber(String text, String needle) {
        String[] lines = text.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains(needle)) {
                return i + 1; // Match the parser’s 1-based line numbers.
            }
        }
        throw new IllegalArgumentException("needle not found: " + needle);
    }
}
