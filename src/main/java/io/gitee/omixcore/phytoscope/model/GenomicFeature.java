package io.gitee.omixcore.phytoscope.model;

/**
 * An interval feature in a gene structure; the base class for all drawable elements.
 *
 * <p>Coordinate convention: 1-based, fully-closed intervals, as defined by the GFF3 specification.
 * Both start and end are 1-based and the interval is inclusive on both ends.</p>
 */
public class GenomicFeature {

    /** Unique feature ID (the ID attribute in GFF3 column 9), may be null. */
    private String id;

    /** Feature name (the Name attribute), used for display. */
    private String name;

    /** Chromosome or contig this feature belongs to. */
    private String chromosome;

    /** Feature type. */
    private FeatureType type;

    /** Start position, 1-based, inclusive. */
    private int start;

    /** End position, 1-based, inclusive. */
    private int end;

    /** Strand: '+' forward, '-' reverse, '.' unknown. */
    private char strand = '.';

    /** Species this feature belongs to (e.g. Arabidopsis thaliana). */
    private String species;

    /** Raw GFF3 column-9 attribute string, kept for traceability. */
    private String attributes;

    /** Source-file line number (1-based) of the row that produced this feature; 0 when unknown. */
    private int lineNumber;

    /** Raw GFF3 column-5 score string; {@code "."} means unknown and is kept verbatim. */
    private String score;

    /** Raw GFF3 column-7 phase string; {@code "."} means unknown and is kept verbatim. */
    private String phase;

    public GenomicFeature() {
    }

    public GenomicFeature(String id, String chromosome, FeatureType type, int start, int end, char strand) {
        this.id = id;
        this.chromosome = chromosome;
        this.type = type;
        this.start = start;
        this.end = end;
        this.strand = strand;
    }

    /** Interval length in base pairs. */
    public int length() {
        return end - start + 1;
    }

    public boolean isForwardStrand() {
        return strand == '+';
    }

    public String getDisplayName() {
        if (name != null && !name.isBlank()) {
            return name;
        }
        if (id != null && !id.isBlank()) {
            return id;
        }
        return "(unnamed)";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getChromosome() {
        return chromosome;
    }

    public void setChromosome(String chromosome) {
        this.chromosome = chromosome;
    }

    public FeatureType getType() {
        return type;
    }

    public void setType(FeatureType type) {
        this.type = type;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public char getStrand() {
        return strand;
    }

    public void setStrand(char strand) {
        this.strand = strand;
    }

    public String getSpecies() {
        return species;
    }

    public void setSpecies(String species) {
        this.species = species;
    }

    public String getAttributes() {
        return attributes;
    }

    public void setAttributes(String attributes) {
        this.attributes = attributes;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getScore() {
        return score;
    }

    public void setScore(String score) {
        this.score = score;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    @Override
    public String toString() {
        return String.format("%s[%s %s:%d-%d(%c) len=%d]",
                type == null ? "?" : type.getSoTerm(),
                getDisplayName(), chromosome, start, end, strand, length());
    }
}
