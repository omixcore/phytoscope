package io.gitee.omixcore.phytoscope.parser;

/**
 * Annotation file formats recognised by the parsers.
 *
 * <p>The formats differ only in the layout of column 9 (attributes) and in how
 * the header is written, so a single parser core can serve all of them once the
 * dialect is known.</p>
 */
public enum AnnotationFormat {

    /**
     * GFF3: {@code key=value} pairs separated by semicolons, hierarchical
     * {@code ID}/{@code Parent} attributes, {@code ##} directives in the header.
     */
    GFF3("GFF3", "*.gff3", "*.gff", "*.gff3.gz"),

    /**
     * GTF/GFF2: {@code key "value"} pairs separated by semicolons, with
     * {@code gene_id} / {@code transcript_id} instead of {@code ID}/{@code Parent}.
     * Widely used by Ensembl, GENCODE and many plant genome portals.
     */
    GTF("GTF", "*.gtf", "*.gtf.gz", "*.gff2");

    private final String label;
    private final String[] extensions;

    AnnotationFormat(String label, String... extensions) {
        this.label = label;
        this.extensions = extensions;
    }

    public String getLabel() {
        return label;
    }

    /** Glob patterns accepted by a file chooser for this format. */
    public String[] getExtensions() {
        return extensions.clone();
    }

    /**
     * Guesses the format from a file name, defaulting to {@link #GFF3}.
     * The content-based detector in the parser takes precedence over this.
     */
    public static AnnotationFormat fromFileName(String fileName) {
        if (fileName == null) {
            return GFF3;
        }
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".gtf") || lower.endsWith(".gtf.gz") || lower.endsWith(".gff2")) {
            return GTF;
        }
        return GFF3;
    }

    /** A file-chooser filter entry covering both supported formats. */
    public static String[] allExtensions() {
        return new String[]{"*.gff3", "*.gff", "*.gtf", "*.gff2", "*.gff3.gz", "*.gtf.gz"};
    }
}
