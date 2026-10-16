package io.gitee.omixcore.phytoscope.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Transcript: a gene may produce several transcripts through alternative splicing, each composed of exons.
 */
public class Transcript extends GenomicFeature {

    private final List<Exon> exons = new ArrayList<>();
    private final List<GenomicFeature> unassignedSegments = new ArrayList<>();
    /** Source CDS/UTR segments without an overlapping annotated exon; never fabricate exons. */
    public List<GenomicFeature> getUnassignedSegments() { return List.copyOf(unassignedSegments); }
    public void addUnassignedSegment(GenomicFeature segment) { unassignedSegments.add(segment); }
    public List<GenomicFeature> getAllCdsSegments() {
        return java.util.stream.Stream.concat(exons.stream().flatMap(e -> e.getCdsSegments().stream()),
                unassignedSegments.stream().filter(s -> s.getType() == FeatureType.CDS)).toList();
    }

    /** Corresponding protein product ID (protein_id on CDS features), may be null. */
    private String proteinId;

    /** Whether this is the canonical (representative) transcript. */
    private boolean canonical;

    public Transcript() {
        setType(FeatureType.MRNA);
    }

    public Transcript(String id, String chromosome, int start, int end, char strand) {
        super(id, chromosome, FeatureType.MRNA, start, end, strand);
    }

    public void addExon(Exon exon) {
        exons.add(exon);
    }

    public List<Exon> getExons() {
        return exons;
    }

    /** Returns a copy of the exons sorted by ascending genomic coordinate, for drawing. */
    public List<Exon> getExonsByCoordinate() {
        List<Exon> copy = new ArrayList<>(exons);
        copy.sort(Comparator.comparingInt(GenomicFeature::getStart));
        return copy;
    }

    /**
     * Computes intron intervals between adjacent exons in genomic coordinate order.
     */
    public List<GenomicFeature> computeIntrons() {
        List<Exon> sorted = getExonsByCoordinate();
        List<GenomicFeature> introns = new ArrayList<>();
        for (int i = 0; i < sorted.size() - 1; i++) {
            int intronStart = sorted.get(i).getEnd() + 1;
            int intronEnd = sorted.get(i + 1).getStart() - 1;
            if (intronEnd >= intronStart) {
                GenomicFeature intron = new GenomicFeature(
                        getId() + ".intron" + (i + 1),
                        getChromosome(),
                        FeatureType.INTRON,
                        intronStart,
                        intronEnd,
                        getStrand());
                introns.add(intron);
            }
        }
        return introns;
    }

    /** Total exonic length in bases. */
    public int getExonicLength() {
        return exons.stream().mapToInt(GenomicFeature::length).sum();
    }

    /** Total CDS length in bases, i.e. the coding region length. */
    public int getCdsLength() {
        return getAllCdsSegments().stream().mapToInt(GenomicFeature::length).sum();
    }

    /** Total number of introns. */
    public int getIntronCount() {
        return computeIntrons().size();
    }

    /** Number of exons. */
    public int getExonCount() {
        return exons.size();
    }

    public String getProteinId() {
        return proteinId;
    }

    public void setProteinId(String proteinId) {
        this.proteinId = proteinId;
    }

    public boolean isCanonical() {
        return canonical;
    }

    public void setCanonical(boolean canonical) {
        this.canonical = canonical;
    }
}
