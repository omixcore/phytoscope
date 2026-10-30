package io.gitee.omixcore.phytoscope;

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

class AnnotationHealthServiceTest {

    @TempDir Path temp;

    private static final String ANNOTATION = """
            ##gff-version 3
            ##sequence-region chr1 1 1000
            ##assembly TestAssembly
            chr1\tsrc\tgene\t1\t100\t.\t+\t.\tID=g_clean
            chr1\tsrc\tmRNA\t1\t100\t.\t+\t.\tID=t_clean;Parent=g_clean
            chr1\tsrc\texon\t1\t100\t.\t+\t.\tID=e_clean;Parent=t_clean
            chr1\tsrc\tCDS\t1\t99\t.\t+\t0\tParent=t_clean
            chr1\tsrc\texon\t200\t300\t.\t+\t.\tID=e_orphan;Parent=missing_tx
            chr1\tsrc\tgene\t1\t1500\t.\t+\t.\tID=g_oob
            chr1\tsrc\tgene\t500\t400\t.\t+\t.\tID=g_inverted
            chr1\tsrc\tgene\t20\t40\t.\t+\t.\tID=g_partial
            chr1\tsrc\tmRNA\t20\t40\t.\t+\t.\tID=t_partial;Parent=g_partial;partial=true
            chr1\tsrc\texon\t20\t40\t.\t+\t.\tID=e_partial;Parent=t_partial
            chr1\tsrc\tCDS\t20\t23\t.\t+\t0\tParent=t_partial
            chr1\tsrc\tgene\t50\t70\t.\t+\t.\tID=g_incomplete
            chr1\tsrc\tmRNA\t50\t70\t.\t+\t.\tID=t_incomplete;Parent=g_incomplete
            chr1\tsrc\texon\t50\t70\t.\t+\t.\tID=e_incomplete;Parent=t_incomplete
            chr1\tsrc\tCDS\t50\t54\t.\t+\t0\tParent=t_incomplete
            chr1\tsrc\tgene\t80\t110\t.\t+\t.\tID=g_true
            chr1\tsrc\tmRNA\t80\t110\t.\t+\t.\tID=t_true;Parent=g_true
            chr1\tsrc\texon\t80\t110\t.\t+\t.\tID=e_true;Parent=t_true
            chr1\tsrc\tCDS\t80\t86\t.\t+\t0\tParent=t_true
            chr1\tsrc\tstart_codon\t80\t82\t.\t+\t0\tParent=t_true
            chr1\tsrc\tstop_codon\t84\t86\t.\t+\t0\tParent=t_true
            chr1\tsrc\tCDS\t10\t99\t.\t+\t3\tID=cds_badphase;Parent=t_clean
            """;

    @Test
    void detectsAllPresetAnomaliesWithCorrectSeverity() throws Exception {
        var genome = new GenomeDataService();
        genome.loadFromContent(ANNOTATION, "preset-anomalies.gff3");

        Path matrix = temp.resolve("matrix.tsv");
        Files.writeString(matrix, """
                # unit=TPM
                # level=gene
                Gene\tRoot
                g_clean\t2.0
                unknown_gene\t1.0
                """);
        var expression = new ExpressionDataService();
        expression.loadFromFile(matrix);
        expression.reconcile(genome);

        var report = new AnnotationHealthService().assess(genome, expression);

        assertHas(report, IssueCode.ORPHAN_PARENT, Severity.ERROR);
        assertHas(report, IssueCode.COORDINATE_OUT_OF_BOUNDS, Severity.ERROR);
        assertHas(report, IssueCode.COORDINATE_INVERTED, Severity.ERROR);
        assertHas(report, IssueCode.INVALID_PHASE, Severity.WARNING);
        assertHas(report, IssueCode.CDS_ANNOTATION_CONVENTION, Severity.NOTICE);
        assertHas(report, IssueCode.CDS_INCOMPLETE, Severity.WARNING);
        assertHas(report, IssueCode.CDS_TRUE_ANOMALY, Severity.WARNING);
        assertHas(report, IssueCode.EXPRESSION_ID_MISMATCH, Severity.WARNING);
        assertHas(report, IssueCode.ASSEMBLY_MATCH_UNVERIFIED, Severity.NOT_CHECKED);

        // The clean, in-frame, in-phase transcript produces no CDS or phase issue.
        assertFalse(report.issues().stream().anyMatch(issue ->
                issue.code() == IssueCode.CDS_TRUE_ANOMALY && "g_clean".equals(issue.geneId())));
    }

    @Test
    void rendersTextAndJsonSharingTheSameIssues() throws Exception {
        var genome = new GenomeDataService();
        genome.loadFromContent(ANNOTATION, "preset-anomalies.gff3");
        var report = new AnnotationHealthService().assess(genome, new ExpressionDataService());

        String text = report.toText();
        assertTrue(text.contains("Errors ("));
        assertTrue(text.contains("Warnings ("));
        assertTrue(text.contains("Notices ("));
        assertTrue(text.contains("Not checked ("));

        String json = report.toJson();
        assertTrue(json.contains("\"errorCount\""));
        assertTrue(json.contains("\"issues\""));
    }

    private static void assertHas(AnnotationHealthService.HealthReport report,
                                  IssueCode code, Severity severity) {
        assertTrue(report.issues().stream().anyMatch(issue ->
                        issue.code() == code && issue.severity() == severity),
                "expected issue " + code + " with severity " + severity);
    }
}
