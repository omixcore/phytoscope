package io.gitee.omixcore.phytoscope.intake;

/**
 * Annotation version / format / source recorded in manifest v2.
 *
 * @param format  annotation dialect name (e.g. {@code GFF3})
 * @param version annotation version / build label, nullable
 * @param source  annotation source label (file name), nullable
 */
public record AnnotationMeta(String format, String version, String source) {
}
