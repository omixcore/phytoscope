package io.gitee.omixcore.phytoscope.research;

import io.gitee.omixcore.phytoscope.model.ExpressionProfile;
import io.gitee.omixcore.phytoscope.model.Gene;
import io.gitee.omixcore.phytoscope.model.Transcript;
import io.gitee.omixcore.phytoscope.service.ExpressionDataService;
import io.gitee.omixcore.phytoscope.service.GenomeDataService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Summarises the genes overlapping a genomic interval as evidence cards.
 *
 * <p>The report deliberately aggregates without ranking: every overlapping gene
 * is listed in coordinate order with its overlap relation, transcript
 * structure, three-state expression evidence (measured value / measured zero /
 * no record) and traceable source. Negative strands, multi-transcript genes and
 * missing expression are annotated explicitly.</p>
 */
public final class CandidateGeneReportService {

    /** Whether the feature interval lies fully inside the query window. */
    public enum OverlapKind {
        FULL,
        PARTIAL
    }

    /** Overlap relation between a query interval and a feature interval. */
    public record Overlap(OverlapKind kind, int start, int end, int length) {

        /** True when the two closed intervals actually intersect. */
        public boolean overlaps() {
            return length > 0;
        }

        /** Computes the 1-based closed overlap; {@code length} is 0 when disjoint. */
        public static Overlap compute(int queryStart, int queryEnd, int featureStart, int featureEnd) {
            int start = Math.max(queryStart, featureStart);
            int end = Math.min(queryEnd, featureEnd);
            int length = end - start + 1;
            if (length <= 0) {
                return new Overlap(OverlapKind.PARTIAL, start, end, 0);
            }
            OverlapKind kind = (featureStart >= queryStart && featureEnd <= queryEnd)
                    ? OverlapKind.FULL : OverlapKind.PARTIAL;
            return new Overlap(kind, start, end, length);
        }
    }

    /** Structural summary of one transcript, together with its overlap to the query. */
    public record TranscriptEvidence(String transcriptId, int exonCount, int cdsLength,
                                     boolean hasUtr, boolean hasStartCodon, boolean hasStopCodon,
                                     Overlap overlap) {
    }

    /** One expression measurement; {@code missing} distinguishes NA from a measured 0. */
    public record SampleValue(String sample, Double value, boolean missing) {
    }

    /** Expression evidence for one gene, preserving the three-state semantics. */
    public record ExpressionEvidence(boolean present, String inputId, String unit,
                                     List<SampleValue> values) {
    }

    /** Where a card's data came from, for independent verification. */
    public record SourceEvidence(int geneLineNumber, String annotationAttributes,
                                 String expressionInputId) {
    }

    /** One candidate gene and everything the report knows about it. */
    public record EvidenceCard(String geneId, String displayName, String chromosome, char strand,
                               Overlap overlap, List<TranscriptEvidence> transcripts,
                               ExpressionEvidence expression, SourceEvidence source) {
    }

    /** Ordered, unranked collection of evidence cards for a query interval. */
    public record CandidateGeneReport(GenomicInterval query, List<EvidenceCard> cards) {

        public CandidateGeneReport {
            cards = cards == null ? List.of() : List.copyOf(cards);
        }

        /** Markdown rendering of the whole report, suitable for export. */
        public String toMarkdown() {
            StringBuilder sb = new StringBuilder();
            sb.append("# Candidate genes in ").append(query.label()).append('\n');
            sb.append("\nQuery: ").append(query.sequence()).append(':')
                    .append(query.start()).append('-').append(query.end())
                    .append(" (").append(query.strand()).append(")\n");
            sb.append("\nFound ").append(cards.size()).append(" overlapping gene")
                    .append(cards.size() == 1 ? "" : "s").append(" (summarised, not ranked).\n");
            for (EvidenceCard card : cards) {
                sb.append("\n## ").append(card.displayName())
                        .append(" (`").append(card.geneId()).append("`)\n");
                sb.append("- Locus: ").append(card.chromosome()).append(':')
                        .append(card.overlap().start()).append('-').append(card.overlap().end())
                        .append(" strand ").append(card.strand());
                if (card.strand() == '-') {
                    sb.append(" (minus strand)");
                }
                sb.append('\n');
                sb.append("- Overlap: ").append(card.overlap().kind()).append(" ")
                        .append(card.overlap().start()).append('-').append(card.overlap().end())
                        .append(" (").append(card.overlap().length()).append(" bp)\n");

                if (card.transcripts().isEmpty()) {
                    sb.append("- Transcripts: none annotated\n");
                } else {
                    sb.append("- Transcripts (").append(card.transcripts().size()).append("):\n");
                    for (TranscriptEvidence tx : card.transcripts()) {
                        sb.append("  - ").append(tx.transcriptId()).append(": ")
                                .append(tx.exonCount()).append(" exon(s), CDS ")
                                .append(tx.cdsLength()).append(" bp, UTR ")
                                .append(tx.hasUtr() ? "yes" : "no")
                                .append(", start codon ").append(tx.hasStartCodon() ? "yes" : "no")
                                .append(", stop codon ").append(tx.hasStopCodon() ? "yes" : "no")
                                .append(", overlap ").append(tx.overlap().kind()).append(' ')
                                .append(tx.overlap().start()).append('-').append(tx.overlap().end())
                                .append('\n');
                    }
                }

                sb.append("- Expression: ");
                if (!card.expression().present()) {
                    sb.append("no record (NA)\n");
                } else {
                    sb.append("input id `").append(card.expression().inputId()).append("`, unit ")
                            .append(card.expression().unit()).append('\n');
                    for (SampleValue value : card.expression().values()) {
                        sb.append("  - ").append(value.sample()).append(": ");
                        if (value.missing()) {
                            sb.append("无表达 (NA)\n");
                        } else {
                            sb.append(formatValue(value.value())).append('\n');
                        }
                    }
                }

                sb.append("- Source: line ").append(card.source().geneLineNumber());
                if (card.source().expressionInputId() != null) {
                    sb.append(", expression input `").append(card.source().expressionInputId()).append('`');
                }
                sb.append('\n');
            }
            return sb.toString();
        }

        /** CSV rendering with one row per candidate gene. */
        public String toCsv() {
            StringBuilder sb = new StringBuilder();
            sb.append("\"GeneID\",\"DisplayName\",\"Chromosome\",\"Strand\",\"Overlap\",\"Start\",\"End\",\"Length\","
                    + "\"Transcripts\",\"ExpressionPresent\",\"InputId\",\"Unit\",\"Samples\",\"SourceLine\"\n");
            for (EvidenceCard card : cards) {
                StringBuilder samples = new StringBuilder();
                for (SampleValue value : card.expression().values()) {
                    if (samples.length() > 0) {
                        samples.append(';');
                    }
                    samples.append(value.sample()).append('=')
                            .append(value.missing() ? "NA" : formatValue(value.value()));
                }
                sb.append(csv(card.geneId())).append(',')
                        .append(csv(card.displayName())).append(',')
                        .append(csv(card.chromosome())).append(',')
                        .append(csv(String.valueOf(card.strand()))).append(',')
                        .append(csv(card.overlap().kind().name())).append(',')
                        .append(card.overlap().start()).append(',')
                        .append(card.overlap().end()).append(',')
                        .append(card.overlap().length()).append(',')
                        .append(card.transcripts().size()).append(',')
                        .append(card.expression().present()).append(',')
                        .append(csv(card.expression().inputId())).append(',')
                        .append(csv(card.expression().unit())).append(',')
                        .append(csv(samples.toString())).append(',')
                        .append(card.source().geneLineNumber()).append('\n');
            }
            return sb.toString();
        }

        private static String csv(String value) {
            return "\"" + (value == null ? "" : value.replace("\"", "\"\"")) + "\"";
        }

        private static String formatValue(Double value) {
            return value == null ? "NA" : String.format(Locale.ROOT, "%.4g", value);
        }
    }

    /**
     * Builds the candidate report for one interval.
     *
     * @param interval   query interval (1-based closed)
     * @param genome     annotation model
     * @param expression expression matrix, may be empty
     * @return unranked evidence cards in coordinate order
     */
    public CandidateGeneReport report(GenomicInterval interval, GenomeDataService genome,
                                      ExpressionDataService expression) {
        List<EvidenceCard> cards = new ArrayList<>();
        for (Gene gene : genome.getGenesOn(interval.sequence())) {
            Overlap overlap = Overlap.compute(interval.start(), interval.end(), gene.getStart(), gene.getEnd());
            if (!overlap.overlaps()) {
                continue;
            }
            cards.add(card(gene, interval, expression));
        }
        return new CandidateGeneReport(interval, List.copyOf(cards));
    }

    private static EvidenceCard card(Gene gene, GenomicInterval interval, ExpressionDataService expression) {
        List<TranscriptEvidence> transcripts = new ArrayList<>();
        for (Transcript transcript : gene.getTranscripts()) {
            boolean hasUtr = transcript.getExons().stream()
                    .anyMatch(exon -> !exon.getUtrSegments().isEmpty());
            transcripts.add(new TranscriptEvidence(
                    transcript.getId(),
                    transcript.getExonCount(),
                    transcript.getCdsLength(),
                    hasUtr,
                    !transcript.getStartCodonSegments().isEmpty(),
                    !transcript.getStopCodonSegments().isEmpty(),
                    Overlap.compute(interval.start(), interval.end(), transcript.getStart(), transcript.getEnd())));
        }

        String annotationId = gene.getId();
        ExpressionProfile profile = expression.getProfileForAnnotation(annotationId);
        boolean present = profile != null;
        String inputId = expression.getInputIdForAnnotation(annotationId);
        List<SampleValue> values = new ArrayList<>();
        for (String sample : expression.getTissues()) {
            double value = profile == null ? Double.NaN : profile.getTpm(sample);
            values.add(new SampleValue(sample, value, Double.isNaN(value)));
        }

        return new EvidenceCard(
                gene.getId(),
                gene.getDisplayName(),
                gene.getChromosome(),
                gene.getStrand(),
                Overlap.compute(interval.start(), interval.end(), gene.getStart(), gene.getEnd()),
                List.copyOf(transcripts),
                new ExpressionEvidence(present, inputId, expression.getValueUnit(), List.copyOf(values)),
                new SourceEvidence(gene.getLineNumber(), gene.getAttributes(), inputId));
    }
}
