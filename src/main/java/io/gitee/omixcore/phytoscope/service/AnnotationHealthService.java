package io.gitee.omixcore.phytoscope.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gitee.omixcore.phytoscope.model.Exon;
import io.gitee.omixcore.phytoscope.model.Gene;
import io.gitee.omixcore.phytoscope.model.GenomicFeature;
import io.gitee.omixcore.phytoscope.model.SequenceRegion;
import io.gitee.omixcore.phytoscope.model.Transcript;
import io.gitee.omixcore.phytoscope.parser.ParseDiagnostics;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Consistency health check over an annotation and its paired expression matrix.
 *
 * <p>The report is a single ordered list of {@link HealthIssue} items bucketed
 * into four severities. Every orphan, skipped row and coordinate/CDS/phase
 * anomaly carries its source line number and raw attribute evidence where
 * available, so the report is directly traceable back to the input file.</p>
 */
public final class AnnotationHealthService {

    /** Report severity buckets. */
    public enum Severity {
        ERROR,
        WARNING,
        NOTICE,
        NOT_CHECKED
    }

    /** Stable machine-readable issue codes. */
    public enum IssueCode {
        ORPHAN_PARENT,
        COORDINATE_INVERTED,
        COORDINATE_OUT_OF_BOUNDS,
        CDS_INCOMPLETE,
        CDS_ANNOTATION_CONVENTION,
        CDS_TRUE_ANOMALY,
        INVALID_PHASE,
        EXPRESSION_ID_MISMATCH,
        ASSEMBLY_MATCH_UNVERIFIED,
        /** Parser input quality: malformed rows skipped, or detail lists truncated. */
        SKIPPED_ROW
    }

    /** One health finding with its traceability evidence. */
    public record HealthIssue(Severity severity, IssueCode code, String message,
                              Integer lineNumber, String rawEvidence, String geneId,
                              String transcriptId) {
    }

    /** Immutable ordered report; counts are derived from the issues. */
    public record HealthReport(List<HealthIssue> issues) {
        public HealthReport {
            issues = issues == null ? List.of() : List.copyOf(issues);
        }

        public int errorCount() {
            return count(Severity.ERROR);
        }

        public int warningCount() {
            return count(Severity.WARNING);
        }

        public int noticeCount() {
            return count(Severity.NOTICE);
        }

        public int notCheckedCount() {
            return count(Severity.NOT_CHECKED);
        }

        private int count(Severity severity) {
            return (int) issues.stream().filter(issue -> issue.severity() == severity).count();
        }

        /** Human-readable report with one section per severity bucket. */
        public String toText() {
            StringBuilder sb = new StringBuilder();
            sb.append("Annotation health report\n");
            sb.append("=======================\n\n");
            section(sb, "Errors", Severity.ERROR);
            section(sb, "Warnings", Severity.WARNING);
            section(sb, "Notices", Severity.NOTICE);
            section(sb, "Not checked", Severity.NOT_CHECKED);
            sb.append("Summary: ").append(errorCount()).append(" error(s), ")
                    .append(warningCount()).append(" warning(s), ")
                    .append(noticeCount()).append(" notice(s), ")
                    .append(notCheckedCount()).append(" not checked.\n");
            return sb.toString();
        }

        /** Machine-readable JSON sharing the same issue list as {@link #toText()}. */
        public String toJson() {
            try {
                Map<String, Object> root = new LinkedHashMap<>();
                root.put("errorCount", errorCount());
                root.put("warningCount", warningCount());
                root.put("noticeCount", noticeCount());
                root.put("notCheckedCount", notCheckedCount());
                root.put("issues", issues);
                return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(root);
            } catch (JsonProcessingException ex) {
                throw new IllegalStateException(ex);
            }
        }

        private void section(StringBuilder sb, String title, Severity severity) {
            List<HealthIssue> matches = issues.stream()
                    .filter(issue -> issue.severity() == severity)
                    .toList();
            sb.append(title).append(" (").append(matches.size()).append("):\n");
            if (matches.isEmpty()) {
                sb.append("  none\n");
            }
            for (HealthIssue issue : matches) {
                sb.append("  [").append(issue.code()).append(']');
                if (issue.lineNumber() != null) {
                    sb.append(" line ").append(issue.lineNumber());
                }
                if (issue.geneId() != null) {
                    sb.append(" gene=").append(issue.geneId());
                }
                if (issue.transcriptId() != null) {
                    sb.append(" transcript=").append(issue.transcriptId());
                }
                sb.append(": ").append(issue.message()).append('\n');
                if (issue.rawEvidence() != null && !issue.rawEvidence().isBlank()) {
                    sb.append("      evidence: ").append(issue.rawEvidence()).append('\n');
                }
            }
            sb.append('\n');
        }
    }

    /**
     * Assesses the loaded annotation and expression matrix.
     *
     * @param genome     annotation model and parser diagnostics
     * @param expression expression matrix (may be empty)
     * @return a severity-bucketed health report
     */
    public HealthReport assess(GenomeDataService genome, ExpressionDataService expression) {
        List<HealthIssue> issues = new ArrayList<>();
        ParseDiagnostics diagnostics = genome.getDiagnostics() == null
                ? ParseDiagnostics.empty() : genome.getDiagnostics();

        // 1. Orphan Parent references -> ERROR.
        for (ParseDiagnostics.OrphanFeature orphan : diagnostics.orphans()) {
            String parentKind = orphan.target() == ParseDiagnostics.OrphanTarget.GENE ? "gene" : "transcript";
            issues.add(new HealthIssue(Severity.ERROR, IssueCode.ORPHAN_PARENT,
                    "Feature " + describe(orphan.featureId(), orphan.featureType())
                            + " references missing " + parentKind + " parent '"
                            + orphan.missingParentId() + "'",
                    orphan.lineNumber(), null, orphan.featureId(), null));
        }

        // 2. Structured skipped rows -> NOTICE summary (with truncation note when applicable).
        if (diagnostics.skippedRowTotal() > 0) {
            String message = diagnostics.skippedRowTotal()
                    + " malformed annotation rows were skipped during parsing.";
            if (diagnostics.skippedDetailsTruncated()) {
                message += " Detailed skip list truncated at " + ParseDiagnostics.MAX_RECORDS + " records.";
            }
            issues.add(new HealthIssue(Severity.NOTICE, IssueCode.SKIPPED_ROW, message, null, null, null, null));
        }
        if (diagnostics.orphans().size() >= ParseDiagnostics.MAX_RECORDS) {
            issues.add(new HealthIssue(Severity.NOTICE, IssueCode.SKIPPED_ROW,
                    "Orphan feature detail list truncated at " + ParseDiagnostics.MAX_RECORDS + " records.",
                    null, null, null, null));
        }

        // 3. Model walk: coordinates, phase and CDS frame.
        for (Gene gene : genome.getGenes()) {
            SequenceRegion declared = genome.getMetadata().findDeclaredRegion(gene.getChromosome());
            checkCoordinates(gene, gene.getId(), null, declared, issues);
            for (Transcript transcript : gene.getTranscripts()) {
                checkCoordinates(transcript, gene.getId(), transcript.getId(), declared, issues);
                for (Exon exon : transcript.getExons()) {
                    checkCoordinates(exon, gene.getId(), transcript.getId(), declared, issues);
                }
                for (GenomicFeature segment : transcript.getAllCdsSegments()) {
                    checkCoordinates(segment, gene.getId(), transcript.getId(), declared, issues);
                    checkPhase(segment, gene.getId(), transcript.getId(), issues);
                }
                checkCdsFrame(transcript, gene.getId(), issues);
            }
        }

        // 4. Expression identifier reconciliation -> WARNING.
        if (!expression.isEmpty()) {
            ExpressionDataService.ImportReport report = expression.getImportReport();
            if (report != null && report.unmatchedIds() != null && !report.unmatchedIds().isEmpty()) {
                List<String> preview = report.unmatchedIds().stream().limit(10).toList();
                String message = report.unmatchedIds().size()
                        + " expression ID(s) did not match any annotation ID: "
                        + String.join(", ", preview)
                        + (report.unmatchedIds().size() > preview.size() ? ", …" : "");
                issues.add(new HealthIssue(Severity.WARNING, IssueCode.EXPRESSION_ID_MISMATCH,
                        message, null, null, null, null));
            }
        }

        // 5. Assembly match is never verifiable from coordinates alone -> NOT_CHECKED.
        if (!genome.isEmpty()) {
            issues.add(new HealthIssue(Severity.NOT_CHECKED, IssueCode.ASSEMBLY_MATCH_UNVERIFIED,
                    "Annotation assembly compatibility with a reference sequence cannot be verified "
                            + "from coordinates alone.",
                    null, null, null, null));
        }

        return new HealthReport(issues);
    }

    private static void checkCoordinates(GenomicFeature feature, String geneId, String transcriptId,
                                         SequenceRegion declared, List<HealthIssue> issues) {
        if (feature.getStart() > feature.getEnd()) {
            issues.add(new HealthIssue(Severity.ERROR, IssueCode.COORDINATE_INVERTED,
                    "Inverted coordinates " + feature.getStart() + " > " + feature.getEnd()
                            + " for " + feature.getDisplayName(),
                    feature.getLineNumber(), feature.getAttributes(), geneId, transcriptId));
        }
        if (declared != null && declared.isUsable()
                && (feature.getStart() < 1 || feature.getEnd() > declared.end())) {
            issues.add(new HealthIssue(Severity.ERROR, IssueCode.COORDINATE_OUT_OF_BOUNDS,
                    "Coordinates " + feature.getStart() + "-" + feature.getEnd()
                            + " exceed declared sequence-region " + declared.id()
                            + " (1-" + declared.end() + ")",
                    feature.getLineNumber(), feature.getAttributes(), geneId, transcriptId));
        }
    }

    private static void checkPhase(GenomicFeature segment, String geneId, String transcriptId,
                                   List<HealthIssue> issues) {
        String phase = segment.getPhase();
        if (isInvalidPhase(phase)) {
            issues.add(new HealthIssue(Severity.WARNING, IssueCode.INVALID_PHASE,
                    "Invalid CDS phase '" + phase + "' (expected 0, 1, 2 or '.')",
                    segment.getLineNumber(), segment.getAttributes(), geneId, transcriptId));
        }
    }

    private static boolean isInvalidPhase(String phase) {
        if (phase == null || phase.isBlank() || phase.equals(".")) {
            return false;
        }
        return !(phase.equals("0") || phase.equals("1") || phase.equals("2"));
    }

    private static void checkCdsFrame(Transcript transcript, String geneId, List<HealthIssue> issues) {
        int cdsLength = transcript.getCdsLength();
        if (cdsLength <= 0 || cdsLength % 3 == 0) {
            return;
        }
        if (isExplicitPartial(transcript)) {
            issues.add(new HealthIssue(Severity.NOTICE, IssueCode.CDS_ANNOTATION_CONVENTION,
                    "CDS length " + cdsLength + " is not a multiple of 3; the transcript is "
                            + "explicitly marked partial=true/1 (annotation convention).",
                    transcript.getLineNumber(), transcript.getAttributes(), geneId, transcript.getId()));
        } else if (transcript.getStartCodonSegments().isEmpty() || transcript.getStopCodonSegments().isEmpty()) {
            issues.add(new HealthIssue(Severity.WARNING, IssueCode.CDS_INCOMPLETE,
                    "CDS length " + cdsLength + " is not a multiple of 3 and the transcript lacks "
                            + "a start or stop codon (incomplete boundary model).",
                    transcript.getLineNumber(), transcript.getAttributes(), geneId, transcript.getId()));
        } else {
            issues.add(new HealthIssue(Severity.WARNING, IssueCode.CDS_TRUE_ANOMALY,
                    "CDS length " + cdsLength + " is not a multiple of 3 despite complete "
                            + "start/stop codons (true anomaly, review manually).",
                    transcript.getLineNumber(), transcript.getAttributes(), geneId, transcript.getId()));
        }
    }

    private static boolean isExplicitPartial(Transcript transcript) {
        if (isExplicitPartial(transcript.getAttributes())) {
            return true;
        }
        for (GenomicFeature cds : transcript.getAllCdsSegments()) {
            if (isExplicitPartial(cds.getAttributes())) {
                return true;
            }
        }
        return false;
    }

    /** Detects an explicit {@code partial=true} / {@code partial=1} marker in either dialect. */
    private static boolean isExplicitPartial(String attributes) {
        if (attributes == null || attributes.isBlank()) {
            return false;
        }
        String lower = attributes.toLowerCase(Locale.ROOT);
        int idx = lower.indexOf("partial");
        while (idx >= 0) {
            int i = idx + "partial".length();
            while (i < lower.length() && (Character.isWhitespace(lower.charAt(i))
                    || lower.charAt(i) == '=' || lower.charAt(i) == '"')) {
                i++;
            }
            if (i < lower.length()) {
                if (lower.startsWith("true", i)) {
                    return true;
                }
                if (lower.charAt(i) == '1'
                        && (i + 1 >= lower.length() || !Character.isLetterOrDigit(lower.charAt(i + 1)))) {
                    return true;
                }
            }
            idx = lower.indexOf("partial", idx + 1);
        }
        return false;
    }

    private static String describe(String featureId, String featureType) {
        if (featureId != null && !featureId.isBlank()) {
            return "'" + featureId + "'";
        }
        return featureType == null || featureType.isBlank() ? "(unnamed)" : "of type '" + featureType + "'";
    }
}
