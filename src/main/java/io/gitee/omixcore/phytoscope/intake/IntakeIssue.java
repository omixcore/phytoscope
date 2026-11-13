package io.gitee.omixcore.phytoscope.intake;

/**
 * One finding of the import-time health-check.
 *
 * <p>Mirrors the traceability contract of {@code AnnotationHealthService.HealthIssue}
 * but uses the independent {@link IntakeSeverity} and a free-text
 * {@code lineOrCoord} locator so that GFF3 gene line numbers and other positions
 * can be reported without coupling to the four-level post-import model.</p>
 *
 * @param severity    {@link IntakeSeverity} of the finding
 * @param code        stable machine-readable code, e.g. {@code SEQID_NOT_IN_FASTA}
 * @param message     human-readable description
 * @param file        originating file name (FASTA / GFF3 / TSV), nullable
 * @param lineOrCoord source locator: GFF3 gene line number, a coordinate, or null
 * @param suggestion  optional remediation hint, nullable
 */
public record IntakeIssue(IntakeSeverity severity, String code, String message,
                         String file, String lineOrCoord, String suggestion) {
}
