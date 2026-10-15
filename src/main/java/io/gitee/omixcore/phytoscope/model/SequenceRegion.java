package io.gitee.omixcore.phytoscope.model;

/**
 * Metadata describing one sequence region (chromosome, scaffold or contig)
 * declared in an annotation file.
 *
 * <p>GFF3 files declare these with {@code ##sequence-region <id> <start> <end>}
 * directives. The information is used to drive the coordinate axis, to offer a
 * chromosome selector and to validate the visible window, all without any
 * species-specific assumptions.</p>
 *
 * @param id    sequence name as it appears in column 1 of the annotation
 * @param start first coordinate of the region, usually 1
 * @param end   last coordinate of the region
 */
public record SequenceRegion(String id, int start, int end) {

    /** Region length in base pairs. */
    public int length() {
        return end - start + 1;
    }

    /** True when the declared span is complete enough to be used as an axis bound. */
    public boolean isUsable() {
        return id != null && !id.isBlank() && end > start;
    }

    @Override
    public String toString() {
        return id + ":" + start + "-" + end;
    }
}
