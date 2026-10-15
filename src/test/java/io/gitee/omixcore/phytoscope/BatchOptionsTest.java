package io.gitee.omixcore.phytoscope;

import io.gitee.omixcore.phytoscope.cli.BatchOptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the batch-renderer command line, covering option parsing, defaults
 * and the validation that keeps a reproducibility script from silently doing
 * the wrong thing.
 */
class BatchOptionsTest {

    @Test
    @DisplayName("Defaults are applied when only the annotation is given")
    void shouldApplyDefaults() {
        BatchOptions options = BatchOptions.parse(new String[]{"--gff3", "genes.gff3"});

        assertEquals(Path.of("genes.gff3"), options.annotation());
        assertEquals(BatchOptions.DEFAULT_OUTPUT_DIR, options.outputDir().toString());
        assertEquals("both", options.format());
        assertTrue(options.withExpression(), "expression tracks are on by default");
        assertTrue(options.wantsPng());
        assertTrue(options.wantsSvg());
        assertTrue(options.loci().isEmpty(), "no loci means render everything");
    }

    @Test
    @DisplayName("Bundled examples can be selected instead of a file")
    void shouldAcceptExample() {
        BatchOptions options = BatchOptions.parse(new String[]{"--example", "mint"});
        assertEquals("mint", options.example());
        assertEquals(null, options.annotation());
    }

    @Test
    @DisplayName("Loci lists are split and trimmed")
    void shouldSplitLociList() {
        BatchOptions options = BatchOptions.parse(new String[]{
                "--example", "rice", "--loci", "Os01g0100100, Os01g0100300 ,Os01g0100500"});
        assertEquals(List.of("Os01g0100100", "Os01g0100300", "Os01g0100500"), options.loci());
    }

    @Test
    @DisplayName("Format selection narrows the output")
    void shouldRespectFormatSelection() {
        BatchOptions svgOnly = BatchOptions.parse(new String[]{
                "--example", "mint", "--format", "svg"});
        assertFalse(svgOnly.wantsPng());
        assertTrue(svgOnly.wantsSvg());

        BatchOptions pngOnly = BatchOptions.parse(new String[]{
                "--example", "mint", "--format", "png"});
        assertTrue(pngOnly.wantsPng());
        assertFalse(pngOnly.wantsSvg());
    }

    @Test
    @DisplayName("Canvas size and padding are configurable")
    void shouldAcceptGeometryOptions() {
        BatchOptions options = BatchOptions.parse(new String[]{
                "--example", "mint", "--width", "1600", "--height", "900", "--padding", "0.5"});
        assertEquals(1600, options.width());
        assertEquals(900, options.height());
        assertEquals(0.5, options.padding(), 1e-9);
    }

    @Test
    @DisplayName("Expression tracks can be switched off")
    void shouldAllowDisablingExpression() {
        BatchOptions options = BatchOptions.parse(new String[]{
                "--gff3", "g.gff3", "--no-expression"});
        assertFalse(options.withExpression());
    }

    @Test
    @DisplayName("A dataset source is mandatory")
    void shouldRequireADataSource() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> BatchOptions.parse(new String[]{"--out", "figures"}));
        assertTrue(ex.getMessage().contains("--gff3") || ex.getMessage().contains("--example"));
    }

    @Test
    @DisplayName("A file and a bundled example cannot be combined")
    void shouldRejectConflictingSources() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> BatchOptions.parse(new String[]{
                        "--gff3", "g.gff3", "--example", "mint"}));
        assertTrue(ex.getMessage().contains("mutually exclusive"));
    }

    @Test
    @DisplayName("An unsupported format is rejected with a helpful message")
    void shouldRejectUnknownFormat() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> BatchOptions.parse(new String[]{
                        "--example", "mint", "--format", "pdf"}));
        assertTrue(ex.getMessage().contains("png, svg, both"));
    }

    @Test
    @DisplayName("An unknown option fails fast rather than being ignored")
    void shouldRejectUnknownOption() {
        assertThrows(IllegalArgumentException.class,
                () -> BatchOptions.parse(new String[]{"--example", "mint", "--nonsense"}));
    }

    @Test
    @DisplayName("A missing option value is reported clearly")
    void shouldRejectMissingValue() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> BatchOptions.parse(new String[]{"--gff3"}));
        assertTrue(ex.getMessage().contains("requires a value"));
    }

    @Test
    @DisplayName("--help needs no dataset and does not validate the rest")
    void shouldAllowHelpAlone() {
        BatchOptions options = BatchOptions.parse(new String[]{"--help"});
        assertTrue(options.help());
    }

    @Test
    @DisplayName("Batch mode is detected from the launcher arguments")
    void shouldDetectBatchInvocation() {
        assertTrue(PhytoScopeLauncher.isBatchRequest(new String[]{"--example", "mint"}));
        assertTrue(PhytoScopeLauncher.isBatchRequest(new String[]{"--gff3", "g.gff3"}));
        assertTrue(PhytoScopeLauncher.isBatchRequest(new String[]{"--help"}));
        assertFalse(PhytoScopeLauncher.isBatchRequest(new String[]{}),
                "no arguments means start the GUI");
    }

    @Test
    @DisplayName("Tiny canvases are rejected to avoid unusable figures")
    void shouldRejectTinyCanvas() {
        assertThrows(IllegalArgumentException.class,
                () -> BatchOptions.parse(new String[]{
                        "--example", "mint", "--width", "50"}));
    }
}
