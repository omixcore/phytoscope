package io.gitee.omixcore.phytoscope;

import io.gitee.omixcore.phytoscope.parser.Gff3Parser;
import io.gitee.omixcore.phytoscope.parser.ParseDiagnostics;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class Gff3ParserDiagnosticsTest {

    private static Gff3Parser.ParseResult parse(String content) throws Exception {
        return new Gff3Parser().parse(
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), null, "diagnostics.gff3");
    }

    @Test
    void capturesScoreAndPhaseAsRawStrings() throws Exception {
        var result = parse("""
                ##gff-version 3
                ##sequence-region chr1 1 1000
                chr1\tsrc\tgene\t10\t100\t.\t+\t.\tID=g1
                chr1\tsrc\tmRNA\t10\t100\t.\t+\t.\tID=t1;Parent=g1
                chr1\tsrc\texon\t10\t99\t.\t+\t.\tID=e1;Parent=t1
                chr1\tsrc\tCDS\t10\t99\t7.5\t+\t2\tParent=t1
                """);
        var gene = result.genes().getFirst();
        assertEquals(".", gene.getScore());
        assertEquals(".", gene.getPhase());
        assertEquals(3, gene.getLineNumber());

        var cds = gene.getTranscripts().getFirst().getAllCdsSegments().getFirst();
        assertEquals("7.5", cds.getScore());
        assertEquals("2", cds.getPhase());
        assertEquals(6, cds.getLineNumber());
    }

    @Test
    void retainsInvalidPhaseStringsForLaterDiagnosis() throws Exception {
        var result = parse("""
                ##gff-version 3
                ##sequence-region chr1 1 1000
                chr1\tsrc\tgene\t10\t200\t.\t+\t.\tID=g1
                chr1\tsrc\tmRNA\t10\t200\t.\t+\t.\tID=t1;Parent=g1
                chr1\tsrc\texon\t10\t200\t.\t+\t.\tID=e1;Parent=t1
                chr1\tsrc\tCDS\t10\t99\t.\t+\t0\tParent=t1
                chr1\tsrc\tCDS\t10\t99\t.\t+\t3\tParent=t1
                chr1\tsrc\tCDS\t10\t99\t.\t+\t-1\tParent=t1
                chr1\tsrc\tCDS\t10\t99\t.\t+\tX\tParent=t1
                """);
        var phases = result.genes().getFirst().getTranscripts().getFirst().getAllCdsSegments().stream()
                .map(io.gitee.omixcore.phytoscope.model.GenomicFeature::getPhase)
                .toList();
        assertTrue(phases.contains("0"));
        assertTrue(phases.contains("3"));
        assertTrue(phases.contains("-1"));
        assertTrue(phases.contains("X"));
    }

    @Test
    void recordsStructuredSkippedRowsWithLineNumbersAndReasons() throws Exception {
        var parser = new Gff3Parser();
        var result = parser.parse(new ByteArrayInputStream("""
                ##gff-version 3
                broken-row-without-tabs
                chr1\tsrc\tgene\tnotanumber\t100\t.\t+\t.\tID=g1
                chr1\tsrc\tgene\t1\t100\t.\t+\t.\tID=g2
                """.getBytes(StandardCharsets.UTF_8)), null, "skips.gff3");

        assertEquals(2, parser.getLastSkippedRows(), "int accessor returns the uncapped total");
        assertEquals(2, result.diagnostics().skippedRowTotal());
        assertEquals(2, result.diagnostics().skippedRows().size());

        var first = result.diagnostics().skippedRows().get(0);
        assertEquals(2, first.lineNumber());
        assertEquals(ParseDiagnostics.SkipReason.FEWER_THAN_NINE_COLUMNS, first.reason());

        var second = result.diagnostics().skippedRows().get(1);
        assertEquals(3, second.lineNumber());
        assertEquals(ParseDiagnostics.SkipReason.UNUSABLE_COORDINATES_OR_ATTRIBUTES, second.reason());

        assertEquals(1, result.genes().size());
    }

    @Test
    void recordsTranscriptLevelOrphans() throws Exception {
        var result = parse("""
                ##gff-version 3
                ##sequence-region chr1 1 1000
                chr1\tsrc\tgene\t1\t100\t.\t+\t.\tID=g1
                chr1\tsrc\tCDS\t10\t99\t.\t+\t0\tID=cds1;Parent=missing_tx
                """);
        assertEquals(1, result.diagnostics().orphans().size());
        var orphan = result.diagnostics().orphans().getFirst();
        assertEquals(ParseDiagnostics.OrphanTarget.TRANSCRIPT, orphan.target());
        assertEquals("cds1", orphan.featureId());
        assertEquals("missing_tx", orphan.missingParentId());
        assertEquals(4, orphan.lineNumber());
    }

    @Test
    void recordsGeneLevelOrphans() throws Exception {
        var result = parse("""
                ##gff-version 3
                ##sequence-region chr1 1 1000
                chr1\tsrc\tmRNA\t10\t100\t.\t+\t.\tID=t1;Parent=g_missing
                """);
        assertEquals(1, result.diagnostics().orphans().size());
        var orphan = result.diagnostics().orphans().getFirst();
        assertEquals(ParseDiagnostics.OrphanTarget.GENE, orphan.target());
        assertEquals("t1", orphan.featureId());
        assertEquals("g_missing", orphan.missingParentId());
    }

    @Test
    void capsDetailedSkippedRowsButKeepsUncappedTotal() throws Exception {
        StringBuilder content = new StringBuilder("##gff-version 3\n");
        content.append("broken\n".repeat(ParseDiagnostics.MAX_RECORDS + 1));
        var parser = new Gff3Parser();
        var result = parser.parse(new ByteArrayInputStream(
                content.toString().getBytes(StandardCharsets.UTF_8)), null, "cap.gff3");

        assertEquals(ParseDiagnostics.MAX_RECORDS, result.diagnostics().skippedRows().size());
        assertEquals(ParseDiagnostics.MAX_RECORDS + 1, result.diagnostics().skippedRowTotal());
        assertEquals(ParseDiagnostics.MAX_RECORDS + 1, parser.getLastSkippedRows());
        assertTrue(result.diagnostics().skippedDetailsTruncated());
    }
}
