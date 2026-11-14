package io.gitee.omixcore.phytoscope;

import io.gitee.omixcore.phytoscope.intake.IntakeHealthCheck;
import io.gitee.omixcore.phytoscope.intake.IntakeIssue;
import io.gitee.omixcore.phytoscope.intake.IntakeReport;
import io.gitee.omixcore.phytoscope.intake.IntakeSeverity;
import io.gitee.omixcore.phytoscope.service.GeneSequenceService;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Logic tests for the import-time health-check coordinator. These run without a
 * JavaFX toolkit, exercising {@link IntakeHealthCheck} against the committed
 * fixtures under {@code src/test/resources/intake/}.
 */
class IntakeHealthCheckTest {

    private static Path fixture(String name) {
        return Path.of("src/test/resources/intake", name);
    }

    private static boolean hasCode(IntakeReport report, String code) {
        return report.issues().stream().anyMatch(i -> code.equals(i.code()));
    }

    private static boolean hasCodeWithSeverity(IntakeReport report, String code, IntakeSeverity severity) {
        return report.issues().stream()
                .anyMatch(i -> code.equals(i.code()) && i.severity() == severity);
    }

    // ---- AC-1: a clean chromosome-level import passes ----------------------
    @Test
    void chromosomeLevelImportPasses() {
        IntakeReport report = IntakeHealthCheck.check(
                fixture("chr_level.fa"), fixture("chr_level.gff3"), fixture("chr_level.tsv"));
        assertTrue(report.pass(), "chr_level must pass: " + report.summary());
        assertEquals(0, report.errorCount(), "no errors expected: " + report.issues());
        assertFalse(hasCode(report, "SEQID_NOT_IN_FASTA"));
    }

    // ---- AC-1: fragmented scaffold assembly passes ------------------------
    @Test
    void scaffoldFragmentedImportPasses() {
        IntakeReport report = IntakeHealthCheck.check(
                fixture("scaffold_fragmented.fa"), fixture("scaffold_fragmented.gff3"),
                fixture("scaffold_fragmented.tsv"));
        assertTrue(report.pass(), "scaffold_fragmented must pass: " + report.summary());
        assertEquals(0, report.errorCount());
    }

    // ---- AC-1: organelle assembly passes ----------------------------------
    @Test
    void organelleImportPasses() {
        IntakeReport report = IntakeHealthCheck.check(
                fixture("organelle.fa"), fixture("organelle.gff3"), fixture("organelle.tsv"));
        assertTrue(report.pass(), "organelle must pass: " + report.summary());
        assertEquals(0, report.errorCount());
    }

    // ---- coordinate-boundary genes (start=1, end=length) parse cleanly ----
    @Test
    void coordinateBoundaryGenesParseWithoutError() {
        IntakeReport report = IntakeHealthCheck.check(
                fixture("coord_boundary.fa"), fixture("coord_boundary.gff3"),
                fixture("coord_boundary.tsv"));
        assertTrue(report.pass(), "coord_boundary must pass: " + report.summary());
        assertEquals(0, report.errorCount());
    }

    // ---- negative-strand genes are parsed and counted ----------------------
    @Test
    void negativeStrandGenesAreParsed() throws Exception {
        IntakeReport report = IntakeHealthCheck.check(
                fixture("negative_strand.fa"), fixture("negative_strand.gff3"),
                fixture("negative_strand.tsv"));
        assertTrue(report.pass(), "negative_strand must pass: " + report.summary());
    }

    // ---- AC-3: missing seqid reports an ERROR with a line locator ---------
    @Test
    void missingSeqidReportsErrorWithLineLocator() {
        IntakeReport report = IntakeHealthCheck.check(
                fixture("missing_seqid.fa"), fixture("missing_seqid.gff3"),
                fixture("missing_seqid.tsv"));
        assertFalse(report.pass(), "missing_seqid must block the import");
        assertTrue(hasCodeWithSeverity(report, "SEQID_NOT_IN_FASTA", IntakeSeverity.ERROR),
                "expected a SEQID_NOT_IN_FASTA error: " + report.issues());
        List<IntakeIssue> errors = report.issues().stream()
                .filter(i -> "SEQID_NOT_IN_FASTA".equals(i.code())).toList();
        assertTrue(errors.stream().anyMatch(i -> i.lineOrCoord() != null && !i.lineOrCoord().isBlank()),
                "at least one missing-seqid error must point at the GFF3 gene line number (AC-3): "
                        + report.issues());
    }

    // ---- AC-4: an annotation/FASTA name mismatch yields an alias suggestion
    @Test
    void aliasMismatchSuggestsAliasViaNormalisation() {
        IntakeReport report = IntakeHealthCheck.check(
                fixture("alias_mismatch.fa"), fixture("alias_mismatch.gff3"),
                fixture("alias_mismatch.tsv"));
        assertTrue(report.pass(),
                "alias mismatch must NOT be an error once normalisation matches: " + report.summary());
        assertTrue(hasCodeWithSeverity(report, "SEQUENCE_ALIAS_SUGGESTED", IntakeSeverity.INFO),
                "expected an alias suggestion: " + report.issues());
        boolean mapsOneToChr1 = report.issues().stream()
                .anyMatch(i -> "SEQUENCE_ALIAS_SUGGESTED".equals(i.code())
                        && i.message().contains("'1'") && i.message().contains("Chr1"));
        assertTrue(mapsOneToChr1, "alias suggestion should map annotation '1' to FASTA id 'Chr1': "
                + report.issues());
    }

    // ---- AC-5: a bad FASTA surfaces as a parse ERROR ----------------------
    @Test
    void badFastaSurfacesAsParseError() {
        IntakeReport report = IntakeHealthCheck.check(
                fixture("bad_fasta.fa"), fixture("chr_level.gff3"), null);
        assertTrue(hasCodeWithSeverity(report, "FASTA_PARSE_ERROR", IntakeSeverity.ERROR),
                "expected FASTA_PARSE_ERROR: " + report.issues());
        assertFalse(report.pass());
    }

    // ---- AC-5: a malformed GFF3 surfaces skipped-row diagnostics ----------
    @Test
    void malformedGff3ReportsSkippedRows() {
        IntakeReport report = IntakeHealthCheck.check(
                fixture("chr_level.fa"), fixture("bad_gff3.gff3"), null);
        assertTrue(hasCodeWithSeverity(report, "SKIPPED_ROWS", IntakeSeverity.WARNING),
                "expected SKIPPED_ROWS warning: " + report.issues());
    }

    // ---- AC-5: a standalone expression matrix is informational ------------
    @Test
    void standaloneExpressionIsInformational() {
        IntakeReport report = IntakeHealthCheck.check(
                null, null, fixture("standalone_expression.tsv"));
        assertTrue(hasCodeWithSeverity(report, "EXPRESSION_STANDALONE", IntakeSeverity.INFO),
                "expected EXPRESSION_STANDALONE info: " + report.issues());
    }

    // ---- guard: no inputs at all is an ERROR ------------------------------
    @Test
    void noInputsIsAnError() {
        IntakeReport report = IntakeHealthCheck.check(null, null, null);
        assertTrue(hasCodeWithSeverity(report, "NO_INPUT_FILES", IntakeSeverity.ERROR));
        assertFalse(report.pass());
    }

    // ---- AC-8: the report renders to valid JSON and Markdown --------------
    @Test
    void reportRendersToJsonAndMarkdown() {
        IntakeReport report = IntakeHealthCheck.check(
                fixture("chr_level.fa"), fixture("chr_level.gff3"), fixture("chr_level.tsv"));
        String json = report.toJson();
        assertTrue(json.contains("\"specVersion\""), "JSON must carry specVersion");
        assertTrue(json.contains("\"pass\""), "JSON must carry pass");
        assertTrue(json.contains("\"issues\""), "JSON must carry issues");
        String md = report.toMarkdown();
        assertTrue(md.contains("# Import health-check report"),
                "Markdown must have the report heading");
        assertTrue(md.contains("## Files"), "Markdown must list files");
        assertTrue(md.contains("## Issues"), "Markdown must list issues");
    }

    // ---- D1: a name that normalises to more than one FASTA id is ambiguous --
    @Test
    void aliasCollisionIsReportedAsAmbiguous() {
        IntakeReport report = IntakeHealthCheck.check(
                fixture("alias_collision.fa"), fixture("alias_collision.gff3"), null);
        assertTrue(hasCodeWithSeverity(report, "ALIAS_AMBIGUOUS", IntakeSeverity.WARNING),
                "a chromosome matching multiple FASTA ids must be a WARNING, not auto-resolved: "
                        + report.issues());
        // A genuinely unique normalisation (annotation '2' -> FASTA 'Chr2') must
        // still be suggested as an auto-normalise candidate.
        assertTrue(hasCodeWithSeverity(report, "SEQUENCE_ALIAS_SUGGESTED", IntakeSeverity.INFO),
                "the unique '2' -> 'Chr2' mapping should still be suggested: " + report.issues());
    }

    // ---- D1: a user-confirmed alias must overwrite any auto-discovered one ---
    @Test
    void confirmedAliasOverridesAutoAlias() throws Exception {
        GeneSequenceService fasta = IntakeHealthCheck.parseFasta(fixture("alias_mismatch.fa"));
        // Simulate an auto-discovered (or prior) alias '1' -> 'Chr2'.
        fasta.addAliases(Map.of("1", "Chr2"));
        assertEquals("Chr2", fasta.resolveSequenceName("1"),
                "precondition: the initial alias should resolve '1' to Chr2");
        // The user explicitly maps '1' -> 'Chr1'; the override must win.
        fasta.addAliases(Map.of("1", "Chr1"));
        assertEquals("Chr1", fasta.resolveSequenceName("1"),
                "a user-confirmed alias must overwrite the auto alias (D1)");
    }

    // ---- D6a: a FASTA header split on a TAB keeps only the id --------------
    @Test
    void fastaHeaderTabSplitKeepsIdOnly() throws Exception {
        GeneSequenceService fasta = IntakeHealthCheck.parseFasta(fixture("fasta_tab_header.fa"));
        assertTrue(fasta.getSequenceNames().contains("seq1"),
                "tab-split FASTA header must keep only the id 'seq1': " + fasta.getSequenceNames());
        assertFalse(fasta.getSequenceNames().stream().anyMatch(n -> n.contains("\t") || n.contains(" ")),
                "no sequence name should contain the description or a tab");
    }

    // ---- D6b: a feature past the FASTA contig end is an ERROR ---------------
    @Test
    void coordinateOutOfBoundsReportedAsError() {
        IntakeReport report = IntakeHealthCheck.check(
                fixture("coord_oob.fa"), fixture("coord_oob.gff3"), null);
        assertFalse(report.pass(), "coord_oob must be blocked: " + report.summary());
        assertTrue(hasCodeWithSeverity(report, "COORDINATE_OUT_OF_BOUNDS", IntakeSeverity.ERROR),
                "a feature exceeding the FASTA contig length must be an ERROR: " + report.issues());
    }
}
