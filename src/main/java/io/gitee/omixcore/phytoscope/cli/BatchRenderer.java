package io.gitee.omixcore.phytoscope.cli;

import io.gitee.omixcore.phytoscope.model.Gene;
import io.gitee.omixcore.phytoscope.parser.AnnotationFormat;
import io.gitee.omixcore.phytoscope.service.ExampleDataset;
import io.gitee.omixcore.phytoscope.service.ExpressionDataService;
import io.gitee.omixcore.phytoscope.service.GenomeDataService;
import io.gitee.omixcore.phytoscope.ui.component.GeneTrackRenderer;
import io.gitee.omixcore.phytoscope.ui.component.ImageExporter;
import io.gitee.omixcore.phytoscope.ui.component.SvgExporter;

import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Headless, reproducible batch renderer.
 *
 * <p>Renders the same view the interactive application shows, but without a
 * display and without user interaction, so figures can be regenerated
 * identically from a script. This is what makes results reproducible for
 * publication: a figure in the paper can be traced back to one command.</p>
 *
 * <p>Usage:</p>
 * <pre>
 *   java -cp phytoscope.jar io.gitee.omixcore.phytoscope.cli.BatchRenderer \
 *        --gff3 annotation.gff3 \
 *        --expression panel.tsv \
 *        --loci AT1G01010,AT1G01040 \
 *        --out figures/ \
 *        --format both
 * </pre>
 *
 * <p>Instead of {@code --gff3}, one of the bundled examples may be named with
 * {@code --example arabidopsis|rice|maize|soybean}.</p>
 */
public final class BatchRenderer {

    /** Exit code returned on a successful run. */
    public static final int EXIT_OK = 0;

    /** Exit code returned when the command line is malformed. */
    public static final int EXIT_USAGE = 2;

    /** Exit code returned when a dataset fails to load or render. */
    public static final int EXIT_FAILED = 3;

    private BatchRenderer() {
    }

    public static void main(String[] args) {
        int code = run(args, System.out, System.err);
        if (code != EXIT_OK) {
            System.exit(code);
        }
    }

    /**
     * Runs the batch renderer.
     *
     * @param args   command-line arguments
     * @param out    stream for normal progress output
     * @param err    stream for diagnostics
     * @return process exit code
     */
    public static int run(String[] args, PrintStream out, PrintStream err) {
        BatchOptions options;
        try {
            options = BatchOptions.parse(args);
        } catch (IllegalArgumentException ex) {
            err.println("error: " + ex.getMessage());
            err.println();
            printUsage(err);
            return EXIT_USAGE;
        }

        if (options.help()) {
            printUsage(out);
            return EXIT_OK;
        }

        try {
            return renderOnFxThread(options, out, err);
        } catch (Exception ex) {
            err.println("error: " + ex.getMessage());
            return EXIT_FAILED;
        }
    }

    /**
     * Runs the rendering inside a JavaFX application thread.
     *
     * <p>{@code Canvas} may only be drawn from the FX thread, so the batch is
     * executed through a minimal {@link javafx.application.Application} whose
     * {@code start} method performs the work. JavaFX requires that class to
     * have a no-argument constructor, so the parameters travel through a
     * static hand-off slot rather than the constructor.</p>
     */
    private static int renderOnFxThread(BatchOptions options, PrintStream out, PrintStream err)
            throws Exception {
        HeadlessRenderApp.options = options;
        HeadlessRenderApp.out = out;
        HeadlessRenderApp.err = err;
        HeadlessRenderApp.exitCode = EXIT_FAILED;
        javafx.application.Application.launch(HeadlessRenderApp.class);
        return HeadlessRenderApp.exitCode;
    }

    /**
     * Minimal JavaFX application that renders the batch on the FX thread and
     * records the result, then quits. Instantiated reflectively by JavaFX, so
     * it must keep a public no-argument constructor.
     */
    public static final class HeadlessRenderApp extends javafx.application.Application {

        static volatile BatchOptions options;
        static volatile PrintStream out;
        static volatile PrintStream err;
        static volatile int exitCode = EXIT_FAILED;

        /** Required by JavaFX. */
        public HeadlessRenderApp() {
        }

        @Override
        public void start(javafx.stage.Stage stage) {
            // No window is ever shown: this is a headless render.
            try {
                exitCode = render(options, out);
            } catch (Exception ex) {
                err.println("error: " + ex.getMessage());
                exitCode = EXIT_FAILED;
            } finally {
                javafx.application.Platform.exit();
            }
        }
    }

    private static int render(BatchOptions options, PrintStream out) throws Exception {
        GenomeDataService genomes = new GenomeDataService();
        ExpressionDataService expression = new ExpressionDataService();

        // 1. Load the annotation, from an example id or from a file.
        if (options.example() != null) {
            ExampleDataset dataset = ExampleDataset.byId(options.example());
            if (dataset == null) {
                throw new IllegalArgumentException("unknown example: " + options.example()
                        + " (expected one of " + ExampleDataset.CATALOGUE.stream()
                        .map(ExampleDataset::id).toList() + ")");
            }
            genomes.loadExample(dataset);
            expression.loadExample(dataset);
            out.println("dataset  : " + dataset.species() + " (" + dataset.assembly() + ")");
        } else {
            genomes.loadFromFile(options.annotation());
            if (options.expression() != null) {
                expression.loadFromFile(options.expression());
            }
            out.println("dataset  : " + genomes.getMetadata().summary());
        }

        out.println("format   : " + genomes.getFormat().getLabel());
        out.println("genes    : " + genomes.getGeneCount());
        out.println("features : " + genomes.getRawFeatureCount());
        if (!expression.isEmpty()) {
            out.println("samples  : " + expression.getTissues().size()
                    + " " + expression.getTissues());
        }

        // 2. Resolve the loci to render. With none given, every gene is rendered.
        List<Gene> targets = resolveLoci(genomes, options.loci());
        if (targets.isEmpty()) {
            throw new IllegalArgumentException("no gene matched the requested loci");
        }
        out.println("targets  : " + targets.size() + " gene(s)");

        // 3. Prepare the output directory.
        Path outputDir = options.outputDir();
        Files.createDirectories(outputDir);

        // 4. Render each locus.
        int written = 0;
        for (Gene gene : targets) {
            RenderContext context = prepareRenderer(genomes, gene, options);
            String stem = fileStem(gene);
            if (options.wantsPng()) {
                Path png = outputDir.resolve(stem + ".png");
                ImageExporter.exportPng(context.renderer().getCanvas(), png.toFile(),
                        ImageExporter.DEFAULT_SCALE);
                out.println("  wrote " + png);
                written++;
            }
            if (options.wantsSvg()) {
                Path svg = outputDir.resolve(stem + ".svg");
                try (Writer writer = Files.newBufferedWriter(svg, StandardCharsets.UTF_8)) {
                    new SvgExporter(context.renderer()).export(writer,
                            context.renderer().getLastRenderWidth(),
                            context.renderer().getLastRenderHeight());
                }
                out.println("  wrote " + svg);
                written++;
            }
        }

        out.println("done     : " + written + " file(s) in " + outputDir.toAbsolutePath());
        return EXIT_OK;
    }

    /**
     * Builds a renderer pre-configured for one locus: the gene is selected and
     * the window is focused on it with a little padding, mirroring what the
     * interactive "Focus selected" action does.
     */
    private static RenderContext prepareRenderer(GenomeDataService genomes,
                                                 Gene gene,
                                                 BatchOptions options) {
        GeneTrackRenderer renderer = new GeneTrackRenderer();
        renderer.setSequenceLabel(gene.getChromosome());

        // Render one gene at a time so each figure is self-contained.
        renderer.setGenes(List.of(gene));
        renderer.setSelectedGene(gene);
        renderer.focusOn(gene, options.padding());
        renderer.render(options.width(), options.height());
        return new RenderContext(renderer);
    }

    private record RenderContext(GeneTrackRenderer renderer) {
    }

    /**
     * Resolves the requested loci against the loaded annotation. Matching is
     * case-insensitive and accepts either the gene ID or its display name.
     * With no loci given, all genes are returned in their natural order.
     */
    private static List<Gene> resolveLoci(GenomeDataService genomes, List<String> loci) {
        if (loci == null || loci.isEmpty()) {
            return genomes.getGenes();
        }
        List<Gene> resolved = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (String locus : loci) {
            String key = locus.trim().toLowerCase();
            for (Gene gene : genomes.getGenes()) {
                boolean match = key.equals(lower(gene.getId()))
                        || key.equals(lower(gene.getName()))
                        || key.equals(lower(gene.getDisplayName()));
                if (match && seen.add(gene.getDisplayName())) {
                    resolved.add(gene);
                }
            }
        }
        return resolved;
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase();
    }

    /** A filesystem-safe file stem encoding the species-agnostic locus name. */
    private static String fileStem(Gene gene) {
        String name = gene.getDisplayName().replaceAll("[^A-Za-z0-9._-]", "_");
        return name + "_" + gene.getStart() + "-" + gene.getEnd();
    }

    private static void printUsage(PrintStream stream) {
        stream.println("""
                PhytoScope batch renderer - headless, reproducible figure generation

                Usage:
                  --gff3 <file>          annotation file (GFF3 or GTF)
                  --expression <file>    optional expression matrix (TSV/CSV)
                  --example <id>         use a bundled example instead of --gff3;
                                         one of: arabidopsis, rice, maize, soybean
                  --loci <id[,id...]>    genes to render; all genes when omitted
                  --out <dir>            output directory (default: phytoscope-exports)
                  --format <png|svg|both>  output format (default: both)
                  --width <px>           canvas width (default: 1400)
                  --height <px>          canvas height (default: 700)
                  --padding <ratio>      window padding around the gene (default: 0.35)
                  --no-expression        (kept for compatibility; expression is no longer drawn on tracks)
                  -h, --help             show this message

                Examples:
                  # one gene of a bundled species
                  --example maize --out figures --format svg

                  # every gene of a local annotation, vector only
                  --gff3 genes.gff3 --expression tpm.tsv --out figures --format svg

                Exit codes: 0 success, 2 usage error, 3 load or render failure.
                """);
    }
}
