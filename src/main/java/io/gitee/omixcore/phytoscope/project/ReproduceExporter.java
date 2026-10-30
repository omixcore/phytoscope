package io.gitee.omixcore.phytoscope.project;

import io.gitee.omixcore.phytoscope.BuildInfo;
import io.gitee.omixcore.phytoscope.parser.Gff3Parser;
import io.gitee.omixcore.phytoscope.service.ExpressionDataService;
import io.gitee.omixcore.phytoscope.service.GenomeDataService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * Exports a self-contained reproducible record from the current snapshot.
 *
 * <p>The output directory contains the real annotation bytes (decompressed and
 * concatenated from the source parts), the canonical expression TSV, checksum
 * and version files, and two platform scripts ({@code reproduce.sh} /
 * {@code reproduce.bat}) that rebuild the same figures headlessly. Snapshot
 * state without a CLI equivalent is listed in {@code UNCOVERED.md} instead of
 * being silently dropped.</p>
 */
public final class ReproduceExporter {

    /** Default directory name when no explicit target is supplied. */
    public static final String DEFAULT_DIRECTORY_NAME = "phytoscope-reproduce";

    /** Default batch canvas width, used because ViewState does not persist it. */
    public static final double DEFAULT_WIDTH = 1400;

    /** Default batch canvas height, used because ViewState does not persist it. */
    public static final double DEFAULT_HEIGHT = 700;

    /** Default batch window padding ratio, used because ViewState does not persist it. */
    public static final double DEFAULT_PADDING = 0.35;

    /** Everything written by an export, so callers can display or verify the structure. */
    public record Result(Path directory, List<Path> inputs, Path scriptSh, Path scriptBat,
                         Path checksums, Path version, Path uncovered) {
    }

    /**
     * Writes the reproducible record.
     *
     * @param outDir     target directory; when null, {@code phytoscope-reproduce}
     *                   in the working directory is used
     * @param genome     source annotation (must expose source input bytes)
     * @param expression source expression matrix, may be empty
     * @param view       interactive view snapshot
     * @param research   research-workbench snapshot
     * @return the written artefacts
     * @throws IOException when the target already exists or the source bytes are unavailable
     */
    public Result export(Path outDir, GenomeDataService genome, ExpressionDataService expression,
                         ProjectStore.ViewState view, ProjectStore.ResearchState research) throws IOException {
        Path target = outDir != null ? outDir : Path.of(DEFAULT_DIRECTORY_NAME);
        if (Files.exists(target)) {
            throw new IOException("Refusing to overwrite existing directory: " + target.toAbsolutePath());
        }
        Files.createDirectories(target);
        Path inputsDir = target.resolve("inputs");
        Files.createDirectories(inputsDir);

        List<Path> inputs = new ArrayList<>();

        // Real source bytes: decompress each part independently, then concatenate,
        // mirroring GenomeDataService.loadParts in reverse.
        if (genome.getSourceInputs().isEmpty()) {
            throw new IOException("No source annotation bytes available; import a file or bundled reference before exporting");
        }
        Path annotation = inputsDir.resolve("annotation.gff3");
        try (OutputStream out = Files.newOutputStream(annotation)) {
            for (GenomeDataService.SourceInput source : genome.getSourceInputs()) {
                try (InputStream in = Gff3Parser.decompressPerPart(source.open())) {
                    in.transferTo(out);
                }
            }
        }
        inputs.add(annotation);

        boolean hasExpression = !expression.isEmpty();
        if (hasExpression) {
            Path matrix = inputsDir.resolve("expression.tsv");
            Files.writeString(matrix, expression.serializeMatrix(), StandardCharsets.UTF_8);
            inputs.add(matrix);
        }

        List<String> loci = view == null || view.comparisonGenes() == null ? List.of() : view.comparisonGenes();
        boolean comparison = !loci.isEmpty();

        Path scriptSh = target.resolve("reproduce.sh");
        Path scriptBat = target.resolve("reproduce.bat");
        String shCommand = comparison
                ? buildComparisonCommand(loci, view.alignment(), view.compressed(), view.expressionScale(), hasExpression)
                : buildBatchCommand(loci, hasExpression, DEFAULT_WIDTH, DEFAULT_HEIGHT, DEFAULT_PADDING);
        Files.writeString(scriptSh, shellScript(shCommand), StandardCharsets.UTF_8);
        Files.writeString(scriptBat, batchScript(toBatchCommand(shCommand)), StandardCharsets.UTF_8);

        Path checksums = target.resolve("SHA256SUMS.txt");
        Files.writeString(checksums, checksumsContent(inputsDir), StandardCharsets.UTF_8);

        Path version = target.resolve("VERSION.txt");
        Files.writeString(version, BuildInfo.version() + "\n", StandardCharsets.UTF_8);

        Path uncovered = target.resolve("UNCOVERED.md");
        Files.writeString(uncovered, uncoveredContent(view, research, comparison), StandardCharsets.UTF_8);

        return new Result(target, List.copyOf(inputs), scriptSh, scriptBat, checksums, version, uncovered);
    }

    /** Builds the comparison-path command (shell form) for the current snapshot. */
    public static String buildComparisonCommand(List<String> loci, String alignment, boolean compressed,
                                                String scale, boolean withExpression) {
        StringBuilder sb = new StringBuilder("java -jar \"$JAR\" --compare");
        sb.append(" --gff3 inputs/annotation.gff3");
        if (withExpression) {
            sb.append(" --expression inputs/expression.tsv");
        }
        if (loci != null && !loci.isEmpty()) {
            sb.append(" --loci ").append(String.join(",", loci));
        }
        sb.append(" --out comparison-output");
        if (alignment != null && !alignment.isBlank()) {
            sb.append(" --alignment ").append(alignment);
        }
        if (compressed) {
            sb.append(" --compress-introns");
        }
        if (scale != null && !scale.isBlank()) {
            sb.append(" --scale ").append(scale);
        }
        return sb.toString();
    }

    /** Builds the batch-path command (shell form) for the current snapshot. */
    public static String buildBatchCommand(List<String> loci, boolean withExpression,
                                           double width, double height, double padding) {
        StringBuilder sb = new StringBuilder("java -jar \"$JAR\"");
        sb.append(" --gff3 inputs/annotation.gff3");
        if (withExpression) {
            sb.append(" --expression inputs/expression.tsv");
        }
        if (loci != null && !loci.isEmpty()) {
            sb.append(" --loci ").append(String.join(",", loci));
        }
        sb.append(" --out batch-output --format both");
        sb.append(" --width ").append((long) width);
        sb.append(" --height ").append((long) height);
        sb.append(" --padding ").append(padding);
        return sb.toString();
    }

    /** Snapshot fields with no command-line equivalent, listed in {@code UNCOVERED.md}. */
    public static List<String> uncoveredItems(ProjectStore.ViewState view, ProjectStore.ResearchState research) {
        List<String> items = new ArrayList<>();
        items.add("Interactive render dimensions (width/height/padding) are not stored in ViewState; "
                + "batch reproduction uses the defaults " + (long) DEFAULT_WIDTH + "×" + (long) DEFAULT_HEIGHT
                + " / padding " + DEFAULT_PADDING + ".");
        if (view != null && !blank(view.search())) {
            items.add("Gene search query: \"" + view.search() + "\" (no CLI equivalent).");
        }
        if (research != null) {
            if (research.upstream() != 2000 || research.downstream() != 200) {
                items.add("Promoter window upstream/downstream = " + research.upstream() + "/"
                        + research.downstream() + " (interval analysis, no CLI equivalent).");
            }
            if (research.sampleGroups() != null && !research.sampleGroups().isEmpty()) {
                items.add("Sample group assignments = " + research.sampleGroups() + " (no CLI equivalent).");
            }
            if (!blank(research.homologySpecies())) {
                items.add("Homology query species = \"" + research.homologySpecies() + "\" (no CLI equivalent).");
            }
            if (research.homologyIdentity() != 30.0) {
                items.add("Homology minimum identity = " + research.homologyIdentity() + "% (no CLI equivalent).");
            }
            if (!blank(research.domainProtein())) {
                items.add("Domain mapping protein = \"" + research.domainProtein() + "\" (no CLI equivalent).");
            }
            if (!blank(research.domainName())) {
                items.add("Domain mapping name = \"" + research.domainName() + "\" (no CLI equivalent).");
            }
            if (research.domainStartAa() != 1 || research.domainEndAa() != 1) {
                items.add("Domain amino-acid window = " + research.domainStartAa() + "-"
                        + research.domainEndAa() + " (no CLI equivalent).");
            }
            if (!blank(research.lastResult())) {
                items.add("Analysis output text is not reproducible from the command line.");
            }
            if (!blank(research.customInterval())) {
                items.add("Custom interval input = \"" + research.customInterval() + "\" (no CLI equivalent).");
            }
            if (!blank(research.qcSequence())) {
                items.add("Sequence QC input is not reproducible from the command line.");
            }
            if (!blank(research.proteinFasta())) {
                items.add("Homology FASTA input is not reproducible from the command line.");
            }
            if (!blank(research.domainInput())) {
                items.add("Domain mapping input is not reproducible from the command line.");
            }
        }
        return List.copyOf(items);
    }

    // ------------------------------------------------------------------
    // File content helpers
    // ------------------------------------------------------------------

    private static String shellScript(String command) {
        return "#!/usr/bin/env bash\n"
                + "set -euo pipefail\n"
                + "cd \"$(dirname \"$0\")\"\n"
                + "JAR=\"${PHYTOSCOPE_JAR:-phytoscope.jar}\"\n"
                + command + "\n";
    }

    private static String batchScript(String command) {
        return "@echo off\r\n"
                + "setlocal\r\n"
                + "cd /d \"%~dp0\"\r\n"
                + "if \"%PHYTOSCOPE_JAR%\"==\"\" set \"PHYTOSCOPE_JAR=phytoscope.jar\"\r\n"
                + "set \"JAR=%PHYTOSCOPE_JAR%\"\r\n"
                + command + "\r\n";
    }

    private static String toBatchCommand(String shCommand) {
        return shCommand.replace("\"$JAR\"", "\"%JAR%\"");
    }

    private static String checksumsContent(Path inputsDir) throws IOException {
        Path target = inputsDir.getParent();
        StringBuilder sb = new StringBuilder();
        try (var paths = Files.list(inputsDir)) {
            for (Path file : paths.sorted().toList()) {
                String relative = target.relativize(file).toString().replace('\\', '/');
                sb.append(sha256Hex(Files.readAllBytes(file))).append("  ").append(relative).append('\n');
            }
        }
        return sb.toString();
    }

    private static String uncoveredContent(ProjectStore.ViewState view,
                                           ProjectStore.ResearchState research, boolean comparison) {
        StringBuilder sb = new StringBuilder("# UNCOVERED — snapshot state without a CLI equivalent\n\n");
        sb.append("The generated scripts reproduce the annotation bytes, the expression matrix "
                + "and the " + (comparison ? "comparison" : "batch") + " figure settings. "
                + "The following snapshot state has no command-line equivalent and is therefore not reproduced:\n\n");
        List<String> items = uncoveredItems(view, research);
        for (String item : items) {
            sb.append("- ").append(item).append('\n');
        }
        sb.append("\nNote: `--no-expression` is intentionally omitted (a retained compatibility no-op); "
                + "when no matrix was loaded, `--expression` is simply not passed.\n");
        return sb.toString();
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
