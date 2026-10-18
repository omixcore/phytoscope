package io.gitee.omixcore.phytoscope.research;

/** A named, 1-based closed genomic interval. */
public record GenomicInterval(String label, String species, String assembly, String sequence,
                              int start, int end, char strand) {
    public GenomicInterval {
        if (sequence == null || sequence.isBlank()) throw new IllegalArgumentException("Sequence name is required");
        if (start < 1 || end < start) throw new IllegalArgumentException("Expected a non-empty 1-based closed interval");
        if (strand != '+' && strand != '-' && strand != '.') throw new IllegalArgumentException("Strand must be +, - or .");
        label = label == null || label.isBlank() ? sequence + ":" + start + "-" + end : label;
    }
    public int length() { return end - start + 1; }
}
