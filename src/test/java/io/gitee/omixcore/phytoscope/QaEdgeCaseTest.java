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
 * QA 补充的边角/错误路径测试，独立于工程师自测，覆盖主理人指定的验收易错点。
 */
class QaEdgeCaseTest {

    @TempDir Path temp;

    // ------------------------------------------------------------------
    // 验收2 补充：CDS 三分类的"缺 ATG 但含终止密码子"仍判"不完整"
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
                "缺起始密码子但含终止密码子的 CDS 应判为 CDS_INCOMPLETE(警告)");
        assertFalse(report.issues().stream().anyMatch(issue ->
                        issue.code() == IssueCode.CDS_TRUE_ANOMALY && "g1".equals(issue.geneId())),
                "缺起始密码子的 CDS 不得误判为真实异常");
    }

    // ------------------------------------------------------------------
    // 验收2 补充：phase "." 合法未知不告警；3/-1/非数字才告警
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
                "phase='.' 是合法未知值，不应告警");

        long badPhaseCount = report.issues().stream()
                .filter(issue -> issue.code() == IssueCode.INVALID_PHASE && "g_bad".equals(issue.geneId()))
                .count();
        assertEquals(3, badPhaseCount, "phase=3/-1/X 应各产生一条 INVALID_PHASE 警告");
    }

    // ------------------------------------------------------------------
    // 验收3 补充：负链基因坐标 + 精确行号追溯 + 缺失表达 NA 而非 0
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

        // 表达矩阵里没有 g_minus，也没有任何 ID → 该基因无表达记录。
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
        assertEquals('-', card.strand(), "负链基因应显式标注 strand='-'");
        // GFF3 坐标是基因组坐标（5'->3'），负链也不镜像。
        assertEquals(500, card.overlap().start());
        assertEquals(600, card.overlap().end());
        assertEquals(101, card.overlap().length());

        // 来源追溯：行号必须精确指向该基因所在行。
        int geneLine = lineNumber(annotation, "ID=g_minus");
        assertEquals(geneLine, card.source().geneLineNumber(), "证据卡应追溯到基因的原始行号");

        // 缺失表达：present=false，样本全部 missing（NA），不得补 0 或猜值。
        assertFalse(card.expression().present(), "矩阵缺失该基因时应标记为无表达记录");
        assertNull(card.expression().inputId());
        assertTrue(card.expression().values().stream().allMatch(v -> v.missing()));

        String csv = report.toCsv();
        assertTrue(csv.contains("NA"), "CSV 对缺失表达应写 NA 而非 0");
        assertTrue(csv.contains("-"), "CSV 应保留负链 strand 列");
    }

    // ------------------------------------------------------------------
    // 特别核实点3 补充：getSkippedRows() 与 getDiagnostics() 在 replaceWith 后一致性
    // ------------------------------------------------------------------

    @Test
    void skippedRowsStaysConsistentAfterReplaceWith() throws Exception {
        var withSkips = new GenomeDataService();
        withSkips.loadFromContent("##gff-version 3\nbroken\nbroken\n", "skips");
        assertTrue(withSkips.getSkippedRows() > 0, "含畸形行时 getSkippedRows 应 > 0");

        var clean = new GenomeDataService();
        clean.loadFromContent("##gff-version 3\n##sequence-region chr1 1 10\n"
                + "chr1\ts\tgene\t1\t10\t.\t+\t.\tID=g1\n", "clean");
        assertEquals(0, clean.getSkippedRows());

        clean.replaceWith(withSkips);

        assertEquals(withSkips.getDiagnostics().skippedRowTotal(),
                clean.getDiagnostics().skippedRowTotal(),
                "replaceWith 应携带结构化诊断总量");
        assertEquals(withSkips.getSkippedRows(), clean.getSkippedRows(),
                "replaceWith 后 getSkippedRows() 必须与 getDiagnostics() 一致（同为最近一次加载）");
    }

    // ------------------------------------------------------------------
    // 特别核实点4 补充：ParseResult 4 参便捷构造向后兼容
    // ------------------------------------------------------------------

    @Test
    void parseResultFourArgConstructorDefaultsToEmptyDiagnostics() {
        var result = new Gff3Parser.ParseResult(
                java.util.List.of(), 0, io.gitee.omixcore.phytoscope.parser.AnnotationFormat.GFF3,
                io.gitee.omixcore.phytoscope.model.GenomeMetadata.fromAnnotation(
                        null, null, "", "empty", java.util.List.of()));

        assertNotNull(result.diagnostics(), "4 参构造不应产生 null diagnostics");
        assertEquals(ParseDiagnostics.empty(), result.diagnostics(), "4 参构造应默认空诊断");
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
                return i + 1; // 1-based, 与解析器 lineNumber 语义一致
            }
        }
        throw new IllegalArgumentException("needle not found: " + needle);
    }
}
