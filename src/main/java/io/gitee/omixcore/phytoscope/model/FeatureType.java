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
        // Fallback for common Sequence Ontology / plant annotation aliases.
        // These are semantic aliases, not species-specific rules.
        return switch (normalized) {
            case "gene",
                 "protein_coding_gene", "pseudogene",
                 "ncrna_gene", "lncrna_gene", "mirna_gene",
                 "snrna_gene", "snorna_gene", "rrna_gene",
                 "transposable_element_gene" -> GENE;
            case "mrna", "transcript", "primary_transcript",
                 "ncrna", "lncrna", "lnc_rna",
                 "snrna", "snorna", "rrna" -> TRANSCRIPT;
            // A mature miRNA product is not a gene-owned transcript in
            // Araport/TAIR-style GFF3; its primary transcript is the hierarchy node.
            case "mirna_primary_transcript", "mirna_primary_transcript_region" -> TRANSCRIPT;
            case "pseudogenic_transcript" -> TRANSCRIPT;
            case "five_prime_utr", "five_prime_utr_region",
                 "5'utr", "5utr", "5_prime_utr" -> FIVE_UTR;
            case "three_prime_utr", "three_prime_utr_region",
                 "3'utr", "3utr", "3_prime_utr" -> THREE_UTR;
            case "pseudogenic_exon" -> EXON;
            default -> UNKNOWN;
        };
    }
}
