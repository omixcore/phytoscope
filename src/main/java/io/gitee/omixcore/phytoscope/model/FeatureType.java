package io.gitee.omixcore.phytoscope.model;

/**
 * Feature types found in gene structure annotations, following the
 * Sequence Ontology terms commonly used in GFF3 files.
 */
public enum FeatureType {
    GENE("gene", "Gene"),
    MRNA("mRNA", "Transcript"),
    TRANSCRIPT("transcript", "Transcript"),
    EXON("exon", "Exon"),
    CDS("CDS", "Coding sequence"),
    FIVE_UTR("five_prime_UTR", "5' UTR"),
    THREE_UTR("three_prime_UTR", "3' UTR"),
    INTRON("intron", "Intron"),
    UNKNOWN("unknown", "Unknown feature");

    private final String soTerm;
    private final String label;

    FeatureType(String soTerm, String label) {
        this.soTerm = soTerm;
        this.label = label;
    }

    public String getSoTerm() {
        return soTerm;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Resolves a feature type from the GFF3 {@code type} column, case-insensitive.
     */
    public static FeatureType fromSoTerm(String term) {
        if (term == null || term.isBlank()) {
            return UNKNOWN;
        }
        String normalized = term.trim().toLowerCase();
        for (FeatureType type : values()) {
            if (type.soTerm.equalsIgnoreCase(normalized)) {
                return type;
            }
        }
        // Fallback for common aliases
        return switch (normalized) {
            case "transcript" -> TRANSCRIPT;
            case "five_prime_utr", "5'utr", "5utr" -> FIVE_UTR;
            case "three_prime_utr", "3'utr", "3utr" -> THREE_UTR;
            default -> UNKNOWN;
        };
    }
}
