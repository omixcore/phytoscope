package io.gitee.omixcore.phytoscope.intake;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Import-time health-check report.
 *
 * <p>Aggregates the per-file integrity results, the findings and a summary. It is
 * rendered two ways: {@link #toJson()} (machine-readable, consumable by a CI
 * gate that fails on {@code errorCount > 0}) and {@link #toMarkdown()} (human
 * readable). The report is a standalone deliverable, independent of the
 * post-import {@code AnnotationHealthService.HealthReport}.</p>
 *
 * @param files   per-file integrity results
 * @param issues  ordered list of findings
 * @param summary human-readable one-paragraph summary
 * @param pass    true when there are no {@link IntakeSeverity#ERROR} findings
 */
public record IntakeReport(List<IntakeFileResult> files, List<IntakeIssue> issues, String summary, boolean pass) {

    /** Specification version string written into the JSON and manifest v2. */
    public static final String SPEC_VERSION = "import-health-check/1.0";

    public IntakeReport {
        files = files == null ? List.of() : List.copyOf(files);
        issues = issues == null ? List.of() : List.copyOf(issues);
    }

    /** Number of {@link IntakeSeverity#ERROR} findings. */
    public int errorCount() {
        return (int) issues.stream().filter(i -> i.severity() == IntakeSeverity.ERROR).count();
    }

    /** Number of {@link IntakeSeverity#WARNING} findings. */
    public int warningCount() {
        return (int) issues.stream().filter(i -> i.severity() == IntakeSeverity.WARNING).count();
    }

    /** Number of {@link IntakeSeverity#INFO} findings. */
    public int infoCount() {
        return (int) issues.stream().filter(i -> i.severity() == IntakeSeverity.INFO).count();
    }

    /**
     * Machine-readable JSON sharing the same field names as the post-import
     * health report and the reference-provenance style: {@code specVersion},
     * {@code summary}, {@code pass}, the three counts, then {@code files} and
     * {@code issues}. A CI gate can fail on {@code errorCount > 0}.
     */
    public String toJson() {
        try {
            Map<String, Object> root = new LinkedHashMap<>();
            root.put("specVersion", SPEC_VERSION);
            root.put("summary", summary);
            root.put("pass", pass);
            root.put("errorCount", errorCount());
            root.put("warningCount", warningCount());
            root.put("infoCount", infoCount());
            root.put("files", files);
            root.put("issues", issues);
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to render intake report JSON", ex);
        }
    }

    /**
     * Human-readable Markdown: header, summary, severity counts, a file table and
     * the findings grouped by severity with their locator and suggestion.
     */
    public String toMarkdown() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Import health-check report\n\n");
        sb.append("**Specification:** ").append(SPEC_VERSION).append('\n');
        sb.append("**Pass:** ").append(pass ? "yes" : "no").append("\n\n");
        sb.append("## Summary\n\n").append(summary).append("\n\n");
        sb.append("| Severity | Count |\n|---|---|\n");
        sb.append("| ERROR | ").append(errorCount()).append(" |\n");
        sb.append("| WARNING | ").append(warningCount()).append(" |\n");
        sb.append("| INFO | ").append(infoCount()).append(" |\n\n");
        sb.append("## Files\n\n");
        sb.append("| Path | Kind | Bytes | SHA-256 | OK |\n|---|---|---|---|---|\n");
        for (IntakeFileResult f : files) {
            sb.append("| ").append(escape(f.path() == null ? "" : f.path())).append(" | ")
                    .append(f.kind() == null ? "" : f.kind()).append(" | ")
                    .append(f.bytes()).append(" | ")
                    .append(f.sha256() == null ? "" : f.sha256()).append(" | ")
                    .append(f.ok()).append(" |\n");
        }
        sb.append("\n## Issues\n\n");
        for (IntakeSeverity severity : List.of(IntakeSeverity.ERROR, IntakeSeverity.WARNING, IntakeSeverity.INFO)) {
            List<IntakeIssue> group = issues.stream().filter(i -> i.severity() == severity).toList();
            sb.append("### ").append(severity).append(" (").append(group.size()).append(")\n\n");
            if (group.isEmpty()) {
                sb.append("_none_\n\n");
                continue;
            }
            for (IntakeIssue i : group) {
                sb.append("- **[").append(i.code() == null ? "" : i.code()).append("]** ");
                if (i.file() != null) sb.append('`').append(escape(i.file())).append("` ");
                if (i.lineOrCoord() != null) sb.append("(line/coord: ").append(i.lineOrCoord()).append(") ");
                sb.append(": ").append(i.message() == null ? "" : i.message());
                if (i.suggestion() != null) sb.append("\n  - Suggestion: ").append(i.suggestion());
                sb.append('\n');
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    /** Escapes a pipe so it does not break Markdown table cells. */
    private static String escape(String text) {
        return text.replace("|", "\\|").replace("\n", " ").replace("\r", " ");
    }
}
