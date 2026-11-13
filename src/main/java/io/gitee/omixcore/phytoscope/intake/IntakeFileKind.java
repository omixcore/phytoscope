package io.gitee.omixcore.phytoscope.intake;

/**
 * The kind of input file checked by the import health-check.
 *
 * <p>Serialised verbatim ({@code FASTA} / {@code GFF3} / {@code EXPRESSION}) in
 * the report and manifest, so the value matches the authoritative input
 * specification contract.</p>
 */
public enum IntakeFileKind {
    /** Genome DNA FASTA (references the annotation seqids). */
    FASTA,
    /** Gene annotation in GFF3 / GTF. */
    GFF3,
    /** Expression matrix (TSV/CSV/TXT). */
    EXPRESSION
}
