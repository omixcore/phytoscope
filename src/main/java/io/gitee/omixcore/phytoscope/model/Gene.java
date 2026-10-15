package io.gitee.omixcore.phytoscope.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Gene: the top-level entity of a genome annotation, containing one or more transcripts.
 */
public class Gene extends GenomicFeature {

    private final List<Transcript> transcripts = new ArrayList<>();

    /** Locus tag (GFF3 locus_tag), may be null. */
    private String locusTag;

    /** Functional description (GFF3 Note / description attribute). */
    private String description;

    /** Gene aliases (GFF3 Alias attribute). */
    private String alias;

    public Gene() {
        setType(FeatureType.GENE);
    }

    public Gene(String id, String name, String chromosome, int start, int end, char strand) {
        super(id, chromosome, FeatureType.GENE, start, end, strand);
        setName(name);
    }

    public void addTranscript(Transcript transcript) {
        transcripts.add(transcript);
    }

    public List<Transcript> getTranscripts() {
        return transcripts;
    }

    /** Returns the representative transcript: the canonical one if flagged, otherwise the one with most exons. */
    public Transcript getCanonicalTranscript() {
        return transcripts.stream()
                .filter(Transcript::isCanonical)
                .findFirst()
                .orElseGet(() -> transcripts.stream()
                        .max((a, b) -> Integer.compare(a.getExonCount(), b.getExonCount()))
                        .orElse(null));
    }

    public String getLocusTag() {
        return locusTag;
    }

    public void setLocusTag(String locusTag) {
        this.locusTag = locusTag;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }
}
