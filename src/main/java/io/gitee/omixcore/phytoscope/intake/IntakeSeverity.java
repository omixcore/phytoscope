package io.gitee.omixcore.phytoscope.intake;

/**
 * Severity model for the import-time health-check report.
 *
 * <p>This is an <em>independent</em> three-level model ({@code ERROR / WARNING /
 * INFO}) deliberately distinct from {@code AnnotationHealthService.Severity}
 * (which uses {@code ERROR / WARNING / NOTICE / NOT_CHECKED}). The two reports run
 * at different stages (import-time vs import-after) and must never mix their
 * severities in the same artifact; see the incremental design &sect;1.4.</p>
 */
public enum IntakeSeverity {
    /** Blocks the import: e.g. an unparseable file or a reference to a missing sequence. */
    ERROR,
    /** Usable but needs human review: e.g. a sequence-name alias needs confirmation. */
    WARNING,
    /** Informational, non-blocking: e.g. a FASTA sequence with no annotation. */
    INFO
}
