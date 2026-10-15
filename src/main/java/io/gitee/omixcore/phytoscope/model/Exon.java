package io.gitee.omixcore.phytoscope.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Exon: a segment of a transcript, which may belong to both UTR and CDS.
 */
public class Exon extends GenomicFeature {

    /** CDS segments contained in this exon (may be empty, e.g. a pure UTR exon). */
    private final List<GenomicFeature> cdsSegments = new ArrayList<>();

    /** UTR segments contained in this exon. */
    private final List<GenomicFeature> utrSegments = new ArrayList<>();

    /** Exon rank (1-based, counted along the transcription direction). */
    private int rank;

    public Exon() {
        setType(FeatureType.EXON);
    }

    public Exon(String id, String chromosome, int start, int end, char strand) {
        super(id, chromosome, FeatureType.EXON, start, end, strand);
    }

    public void addCds(GenomicFeature cds) {
        cdsSegments.add(cds);
    }

    public void addUtr(GenomicFeature utr) {
        utrSegments.add(utr);
    }

    public List<GenomicFeature> getCdsSegments() {
        return cdsSegments;
    }

    public List<GenomicFeature> getUtrSegments() {
        return utrSegments;
    }

    /** Whether this exon contains coding sequence. */
    public boolean isCoding() {
        return !cdsSegments.isEmpty();
    }

    /**
     * Sorts exons by transcription order (reversed on the minus strand).
     */
    public static void sortByTranscriptionOrder(List<Exon> exons, char strand) {
        exons.sort(strand == '-'
                ? Comparator.comparingInt(GenomicFeature::getStart).reversed()
                : Comparator.comparingInt(GenomicFeature::getStart));
        for (int i = 0; i < exons.size(); i++) {
            exons.get(i).setRank(i + 1);
        }
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }
}
