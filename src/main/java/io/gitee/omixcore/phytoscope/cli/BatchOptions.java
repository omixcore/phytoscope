package io.gitee.omixcore.phytoscope.cli;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Parsed command-line options for {@link BatchRenderer}.
 *
 * <p>Kept separate from the renderer so that argument handling can be unit
 * tested without touching the graphics stack, and so the usage text stays in
 * one place.</p>
 *
 * @param annotation   annotation file, or {@code null} when an example is used
 * @param expression   expression matrix, or {@code null}
 * @param example      bundled example id, or {@code null} when a file is used
 * @param loci         loci to render; empty means every gene
 * @param outputDir    directory for the rendered figures
 * @param format       one of png, svg or both
 * @param width        canvas width in pixels
 * @param height       canvas height in pixels
 * @param padding      window padding ratio around each gene
 * @param withExpression whether to draw the expression heat-map tracks
 * @param help         whether usage was requested
 */
public record BatchOptions(Path annotation,
                           Path expression,
                           String example,
                           List<String> loci,
                           Path outputDir,
                           String format,
                           double width,
                           double height,
                           double padding,
                           boolean withExpression,
                           boolean help) {

    /** Default output directory used when none is supplied. */
    public static final String DEFAULT_OUTPUT_DIR = "phytoscope-exports";

    /**
     * Parses command-line arguments.
     *
     * @throws IllegalArgumentException when an option is unknown or malformed
     */
    public static BatchOptions parse(String[] args) {
        Path annotation = null;
        Path expression = null;
        String example = null;
        List<String> loci = new ArrayList<>();
        Path outputDir = Path.of(DEFAULT_OUTPUT_DIR);
        String format = "both";
        double width = 1400;
        double height = 700;
        double padding = 0.35;
        boolean withExpression = true;
        boolean help = false;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "--gff3", "--annotation" -> annotation = Path.of(requireValue(args, ++i, arg));
                case "--expression" -> expression = Path.of(requireValue(args, ++i, arg));
                case "--example" -> example = requireValue(args, ++i, arg);
                case "--loci" -> loci.addAll(splitList(requireValue(args, ++i, arg)));
                case "--out", "--outdir" -> outputDir = Path.of(requireValue(args, ++i, arg));
                case "--format" -> format = requireValue(args, ++i, arg).toLowerCase();
                case "--width" -> width = parseDouble(requireValue(args, ++i, arg), arg);
                case "--height" -> height = parseDouble(requireValue(args, ++i, arg), arg);
                case "--padding" -> padding = parseDouble(requireValue(args, ++i, arg), arg);
                case "--no-expression" -> withExpression = false;
                case "-h", "--help" -> help = true;
                default -> {
                    // Tolerate a leading "--" separator from Maven/Spring Boot.
                    if (arg.equals("--") && i == 0) {
                        continue;
                    }
                    throw new IllegalArgumentException("unknown option: " + arg);
                }
            }
        }

        if (!help) {
            if (annotation == null && example == null) {
                throw new IllegalArgumentException(
                        "either --gff3 <file> or --example <id> is required");
            }
            if (annotation != null && example != null) {
                throw new IllegalArgumentException(
                        "--gff3 and --example are mutually exclusive");
            }
            if (!List.of("png", "svg", "both").contains(format)) {
                throw new IllegalArgumentException(
                        "--format must be one of png, svg, both (got " + format + ")");
            }
            double exportWidth = width * io.gitee.omixcore.phytoscope.ui.component.ImageExporter.DEFAULT_SCALE;
            double exportHeight = height * io.gitee.omixcore.phytoscope.ui.component.ImageExporter.DEFAULT_SCALE;
            if (!Double.isFinite(width) || !Double.isFinite(height) || !Double.isFinite(padding)
                    || !Double.isFinite(exportWidth) || !Double.isFinite(exportHeight)
                    || exportWidth > 16000 || exportHeight > 16000 || exportWidth * exportHeight > 32_000_000
                    || padding < 0 || padding > 10)
                throw new IllegalArgumentException("Dimensions must be finite and at most 16000 px / 32 million pixels after PNG export scaling; padding must be 0–10");
            if (width < 200 || height < 200) {
                throw new IllegalArgumentException("--width and --height must be at least 200");
            }
        }

        return new BatchOptions(annotation, expression, example, loci, outputDir, format,
                width, height, padding, withExpression, help);
    }

    private static String requireValue(String[] args, int index, String option) {
        if (index >= args.length) {
            throw new IllegalArgumentException("option " + option + " requires a value");
        }
        return args[index];
    }

    private static double parseDouble(String value, String option) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(option + " expects a number, got: " + value);
        }
    }

    private static List<String> splitList(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public boolean wantsPng() {
        return "png".equals(format) || "both".equals(format);
    }

    public boolean wantsSvg() {
        return "svg".equals(format) || "both".equals(format);
    }
}
