package io.gitee.omixcore.phytoscope.parser;

import java.util.List;

/**
 * Structured diagnostics captured while parsing an annotation file.
 *
 * <p>This replaces the previous single skipped-row counter with evidence that
 * the consistency health check can report line by line. Every detailed list is
 * capped at {@link #MAX_RECORDS} to bound memory on adversarial inputs; the
 * {@link #skippedRowTotal} count is never capped and reflects the real total.</p>
 *
 * <p>The auxiliary types ({@link SkipReason}, {@link SkippedRow},
 * {@link OrphanTarget} and {@link OrphanFeature}) live here as nested types so
 * the whole diagnostic vocabulary stays in one file while remaining accessible
 * to the health service and the test suite.</p>
 *
 * @param skippedRows     detailed skipped-row evidence (capped)
 * @param skippedRowTotal uncapped total number of skipped rows
 * @param orphans         features whose {@code Parent} reference did not resolve (capped)
 */
public record ParseDiagnostics(List<SkippedRow> skippedRows, int skippedRowTotal,
                               List<OrphanFeature> orphans) {

    /** Maximum number of detailed records retained per list; totals stay uncapped. */
    public static final int MAX_RECORDS = 10_000;

    public ParseDiagnostics {
        skippedRows = skippedRows == null ? List.of() : List.copyOf(skippedRows);
        orphans = orphans == null ? List.of() : List.copyOf(orphans);
    }

    /** A diagnostics object with no skipped rows and no orphans. */
    public static ParseDiagnostics empty() {
        return new ParseDiagnostics(List.of(), 0, List.of());
    }

    /** True when the detailed skipped-row list was truncated at {@link #MAX_RECORDS}. */
    public boolean skippedDetailsTruncated() {
        return skippedRowTotal > skippedRows.size();
    }

    /** Reason a row was skipped during parsing. */
    public enum SkipReason {
        FEWER_THAN_NINE_COLUMNS,
        UNUSABLE_COORDINATES_OR_ATTRIBUTES
    }

    /** One skipped annotation row, with its source line number and original text. */
    public record SkippedRow(int lineNumber, String rawLine, SkipReason reason) {
    }

    /** The hierarchy level whose parent reference could not be resolved. */
    public enum OrphanTarget {
        TRANSCRIPT,
        GENE
    }

    /**
     * A feature whose {@code Parent} reference did not resolve during hierarchy
     * assembly. The feature itself is retained for inspection rather than silently
     * dropped, so the health report can point at the exact source line.
     */
    public record OrphanFeature(int lineNumber, String featureId, String featureType,
                                String missingParentId, OrphanTarget target) {
    }
}
