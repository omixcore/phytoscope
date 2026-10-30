package io.gitee.omixcore.phytoscope;

import io.gitee.omixcore.phytoscope.project.ProjectStore;
import io.gitee.omixcore.phytoscope.project.ReproduceExporter;
import io.gitee.omixcore.phytoscope.service.ExpressionDataService;
import io.gitee.omixcore.phytoscope.service.GenomeDataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReproduceExporterTest {

    @TempDir Path temp;

    private static final String GFF3 = """
            ##gff-version 3
            ##sequence-region chr1 1 1000
            chr1\tsrc\tgene\t10\t100\t.\t+\t.\tID=g1
            """;

    private static final String MATRIX = """
            # unit=TPM
            # level=gene
            Gene\tRoot\tLeaf
            g1\t1.5\t0
            """;

    private GenomeDataService genome() throws IOException {
        Path gff = temp.resolve("source.gff3");
        Files.writeString(gff, GFF3);
        var genome = new GenomeDataService();
        genome.loadFromFile(gff);
        return genome;
    }

    private ExpressionDataService expression() throws IOException {
        Path matrix = temp.resolve("matrix.tsv");
        Files.writeString(matrix, MATRIX);
        var expression = new ExpressionDataService();
        expression.loadFromFile(matrix);
        return expression;
    }

    @Test
    void exportsComparisonPathWithFullParameterMapping() throws Exception {
        var genome = genome();
        var expression = expression();
        var view = new ProjectStore.ViewState("chr1", "g1", 1, 200, List.of("g1"),
                "TRANSCRIPTION_START", true, "LOG1P", "g1");
        var research = new ProjectStore.ResearchState(1500, 250, "BED", Map.of("Root", "below"),
                "rice", 35.0, "P1", "kinase", 12, 88, "QC passed");

        Path target = temp.resolve("repro");
        var result = new ReproduceExporter().export(target, genome, expression, view, research);

        assertTrue(Files.isDirectory(target));
        assertTrue(Files.exists(target.resolve("inputs/annotation.gff3")));
        assertTrue(Files.exists(target.resolve("inputs/expression.tsv")));
        assertTrue(Files.exists(result.scriptSh()));
        assertTrue(Files.exists(result.scriptBat()));
        assertTrue(Files.exists(result.checksums()));
        assertTrue(Files.exists(result.version()));
        assertTrue(Files.exists(result.uncovered()));

        String sh = Files.readString(result.scriptSh());
        assertTrue(sh.contains("JAR=\"${PHYTOSCOPE_JAR:-phytoscope.jar}\""));
        assertTrue(sh.contains("--compare"));
        assertTrue(sh.contains("--gff3 inputs/annotation.gff3"));
        assertTrue(sh.contains("--expression inputs/expression.tsv"));
        assertTrue(sh.contains("--loci g1"));
        assertTrue(sh.contains("--alignment TRANSCRIPTION_START"));
        assertTrue(sh.contains("--compress-introns"));
        assertTrue(sh.contains("--scale LOG1P"));

        String bat = Files.readString(result.scriptBat());
        assertTrue(bat.contains("%JAR%"));
        assertTrue(bat.contains("--compare"));

        // Real source bytes are reproduced verbatim; the matrix is the canonical
        // serialized form shared with ProjectStore.save.
        assertEquals(GFF3, Files.readString(target.resolve("inputs/annotation.gff3")));
        assertEquals(expression.serializeMatrix(), Files.readString(target.resolve("inputs/expression.tsv")));

        // Checksums cover both inputs.
        String sums = Files.readString(result.checksums());
        assertTrue(sums.contains("inputs/annotation.gff3"));
        assertTrue(sums.contains("inputs/expression.tsv"));

        // Version is injected from build metadata.
        assertEquals(BuildInfo.version() + "\n", Files.readString(result.version()));

        // Uncovered items list search and research state without CLI equivalents.
        String uncovered = Files.readString(result.uncovered());
        assertTrue(uncovered.contains("g1"), "search query should be listed");
        assertTrue(uncovered.contains("rice"), "homology species should be listed");
    }

    @Test
    void exportsBatchPathWhenNoComparisonGenesAreSelected() throws Exception {
        var genome = genome();
        var view = new ProjectStore.ViewState("chr1", "g1", 1, 200, List.of(),
                "GENOMIC_START", false, "LINEAR", "");
        Path target = temp.resolve("batch-repro");
        var result = new ReproduceExporter().export(target, genome, new ExpressionDataService(),
                view, ProjectStore.ResearchState.defaults());

        String sh = Files.readString(result.scriptSh());
        assertFalse(sh.contains("--compare"));
        assertTrue(sh.contains("--out batch-output --format both"));
        assertTrue(sh.contains("--width 1400"));
        assertTrue(sh.contains("--height 700"));
        assertTrue(sh.contains("--padding 0.35"));
        assertFalse(sh.contains("--expression"), "no matrix loaded means no --expression flag");
    }

    @Test
    void refusesToOverwriteExistingDirectory() throws Exception {
        var genome = genome();
        var view = new ProjectStore.ViewState("chr1", null, 1, 200, List.of(),
                "GENOMIC_START", false, "LINEAR", "");
        Path target = temp.resolve("repro-existing");
        new ReproduceExporter().export(target, genome, new ExpressionDataService(), view,
                ProjectStore.ResearchState.defaults());
        assertThrows(IOException.class, () -> new ReproduceExporter().export(target, genome,
                new ExpressionDataService(), view, ProjectStore.ResearchState.defaults()));
    }

    @Test
    void checksumMatchesSha256OfWrittenInput() throws Exception {
        var genome = genome();
        var view = new ProjectStore.ViewState("chr1", null, 1, 200, List.of(),
                "GENOMIC_START", false, "LINEAR", "");
        Path target = temp.resolve("repro-sha");
        var result = new ReproduceExporter().export(target, genome, new ExpressionDataService(), view,
                ProjectStore.ResearchState.defaults());

        byte[] bytes = Files.readAllBytes(target.resolve("inputs/annotation.gff3"));
        String expected = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        String sums = Files.readString(result.checksums());
        assertTrue(sums.contains(expected + "  inputs/annotation.gff3"));
    }
}
