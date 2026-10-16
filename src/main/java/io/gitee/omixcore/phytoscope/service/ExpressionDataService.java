package io.gitee.omixcore.phytoscope.service;

import io.gitee.omixcore.phytoscope.model.ExpressionProfile;
import io.gitee.omixcore.phytoscope.model.TissueExpression;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads and serves sample-level expression data.
 *
 * <p>Species-independent by design: any matrix whose first column holds a gene
 * identifier and whose remaining columns hold measurements will load, whatever
 * the column names are. "Tissue" here really means "any sample group" —
 * tissues, organs, developmental stages or treatments all work, and the panel
 * label shown in the interface comes straight from the file header.</p>
 *
 * <p>Accepted layout:</p>
 * <pre>
 * # optional comment lines
 * GeneID   Root   Stem   Leaf     &lt;- header row defines the sample groups
 * AT1G01010 14.27 6.74  6.74
 * </pre>
 *
 * <p>Tab, comma and semicolon are all accepted as delimiters, and comment
 * lines starting with '#' are ignored, so provenance can live inside the file.</p>
 */
@Service
public class ExpressionDataService {

    /**
     * No expression measurements are bundled; retained as a null compatibility constant.
     *
     * @deprecated the bundled panel is described by {@link ExampleDataset};
     *             prefer {@link #loadExample(ExampleDataset)}.
     */
    @Deprecated
    public static final String SAMPLE_RESOURCE = null;

    /** Sample groups (tissues / stages / treatments), in file-header order. */
    private final List<String> tissues = new ArrayList<>();

    private final Map<String, ExpressionProfile> profiles = new HashMap<>();

    private String source = "No data loaded";

    /** Column heading of the gene-identifier column, as written in the file. */
    private String idColumnLabel = "GeneID";

    /** Unit declared in the file header, e.g. TPM or FPKM; shown in the legend. */
    private String valueUnit = "unspecified";

    // ------------------------------------------------------------------
    // Loading
    // ------------------------------------------------------------------

    /** Loads expression only when the default reference supplies measurements; otherwise clears it. */
    public void loadSampleData() throws IOException {
        loadExample(ExampleDataset.defaultDataset());
    }

    /**
     * Loads an expression matrix from a classpath resource.
     *
     * @param resourcePath classpath path of the matrix
     * @param label        human-readable origin recorded in {@link #getSource()}
     */
    public void loadFromResource(String resourcePath, String label) throws IOException {
        try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("Classpath resource not found: " + resourcePath);
            }
            parse(in);
            this.source = label == null ? resourcePath : label;
        }
    }

    /** Loads an external expression matrix supplied by the user. */
    public void loadFromFile(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            parse(in);
            this.source = file.getFileName().toString();
        }
    }

    /** Loads the expression matrix of a bundled example dataset. */
    public void loadExample(ExampleDataset dataset) throws IOException {
        if (dataset == null || dataset.expressionResource() == null) {
            clear();
            return;
        }
        loadFromResource(dataset.expressionResource(),
                "Bundled panel (" + dataset.species() + ")");
    }

    /** Clears all loaded expression data, used when switching datasets. */
    public void clear() {
        tissues.clear();
        profiles.clear();
        source = "No data loaded";
        idColumnLabel = "GeneID";
        valueUnit = "unspecified";
        level = "gene";
        annotationIds.clear();
        report = ImportReport.empty();
    }

    private ImportReport report = ImportReport.empty();
    private String level = "gene";
    private final Map<String, String> annotationIds = new HashMap<>();

    public record ImportReport(int rows, int profiles, int missingCells, int invalidCells,
                               int matchedIds, int annotationIds, List<String> unmatchedIds,
                               List<String> warnings) {
        static ImportReport empty() { return new ImportReport(0, 0, 0, 0, 0, 0, List.of(), List.of()); }
        public String summary() {
            return "Read " + rows + " rows; " + profiles + " profiles; " + matchedIds
                    + " matched of " + profiles + " input IDs against " + annotationIds
                    + " annotation IDs; " + missingCells + " missing cells; " + invalidCells
                    + " invalid cells.\n" + String.join("\n", warnings)
                    + (unmatchedIds.isEmpty() ? "" : "\nUnmatched IDs: " + String.join(", ", unmatchedIds));
        }
    }

    public ImportReport getImportReport() { return report; }
    public void restoreImportAudit(ImportReport original, String originalSource) {
        if (original != null && original.profiles() == profiles.size()) {
            List<String> warnings = new ArrayList<>(original.warnings());
            warnings.add("Restored canonical matrix snapshot; original import diagnostics retained.");
            report = new ImportReport(original.rows(), profiles.size(), original.missingCells(), original.invalidCells(),
                    report.matchedIds(), report.annotationIds(), report.unmatchedIds(), List.copyOf(warnings));
        }
        source = originalSource;
    }
    public String getLevel() { return level; }
    public List<String> getProfileIds() { return profiles.keySet().stream().sorted().toList(); }

    /** Exact IDs or an unambiguous annotation gene:/transcript: prefix only; no version stripping. */
    public ImportReport reconcile(GenomeDataService genome) {
        annotationIds.clear();
        Map<String, List<String>> candidates = new HashMap<>();
        List<String> ids = new ArrayList<>();
        for (var gene : genome.getGenes()) {
            if (level.equals("transcript")) {
                for (var tx : gene.getTranscripts()) ids.add(tx.getId());
            } else ids.add(gene.getId());
        }
        for (String id : ids) {
            if (id == null) continue;
            candidates.computeIfAbsent(id, key -> new ArrayList<>()).add(id);
            String shortId = id.replaceFirst("^(gene|transcript):", "");
            if (!shortId.equals(id)) candidates.computeIfAbsent(shortId, key -> new ArrayList<>()).add(id);
        }
        List<String> unmatched = new ArrayList<>();
        Map<String, List<String>> targetInputs = new HashMap<>();
        for (String inputId : getProfileIds()) {
            List<String> matches = candidates.getOrDefault(inputId, List.of()).stream().distinct().toList();
            if (matches.size() != 1) unmatched.add(inputId);
            else targetInputs.computeIfAbsent(matches.getFirst(), key -> new ArrayList<>()).add(inputId);
        }
        for (var target : targetInputs.entrySet()) {
            if (target.getValue().size() == 1) annotationIds.put(target.getKey(), target.getValue().getFirst());
            else unmatched.addAll(target.getValue());
        }
        unmatched.sort(String::compareTo);
        int matched = annotationIds.size();
        report = new ImportReport(report.rows(), profiles.size(), report.missingCells(),
                report.invalidCells(), matched, ids.size(), List.copyOf(unmatched), report.warnings());
        return report;
    }

    /** Parse into a temporary object so failed imports never erase existing measurements. */
    private void parse(InputStream in) throws IOException {
        ExpressionDataService next = new ExpressionDataService();
        next.readMatrix(in);
        replaceWith(next);
    }

    public void replaceWith(ExpressionDataService next) {
        tissues.clear(); tissues.addAll(next.tissues);
        profiles.clear(); profiles.putAll(next.profiles);
        annotationIds.clear(); annotationIds.putAll(next.annotationIds);
        source = next.source; idColumnLabel = next.idColumnLabel;
        valueUnit = next.valueUnit; level = next.level; report = next.report;
    }

    private void readMatrix(InputStream in) throws IOException {
        int rows = 0, missing = 0, invalid = 0, lineNo = 0;
        boolean headerSeen = false;
        char delimiter = '\t';
        List<String> warnings = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (lineNo == 1 && line.startsWith("\uFEFF")) line = line.substring(1);
                if (line.isBlank()) continue;
                if (line.stripLeading().startsWith("#")) {
                    var meta = java.util.regex.Pattern.compile("^#\\s*(unit|level)\\s*[:=]\\s*(.+)$",
                            java.util.regex.Pattern.CASE_INSENSITIVE).matcher(line.trim());
                    if (meta.matches()) {
                        if (headerSeen) throw new IOException("Metadata must precede the header (line " + lineNo + ")");
                        if (meta.group(1).equalsIgnoreCase("unit")) valueUnit = meta.group(2).trim();
                        else level = meta.group(2).trim().toLowerCase(java.util.Locale.ROOT);
                    }
                    continue;
                }
                if (!headerSeen) delimiter = detectDelimiter(line);
                List<String> cols = splitRow(line, delimiter);
                if (!headerSeen) {
                    if (cols.size() < 2) throw new IOException("Expected an identifier and at least one sample column");
                    idColumnLabel = cols.getFirst().trim();
                    if (idColumnLabel.isBlank()) throw new IOException("Identifier heading is blank");
                    for (int i = 1; i < cols.size(); i++) {
                        String name = cols.get(i).trim();
                        if (name.isBlank() || tissues.contains(name)) throw new IOException("Blank or duplicate sample heading at column " + (i + 1));
                        tissues.add(name);
                    }
                    if (!List.of("gene", "transcript").contains(level)) throw new IOException("level must be gene or transcript");
                    headerSeen = true;
                    continue;
                }
                rows++;
                if (cols.size() != tissues.size() + 1) throw new IOException("Column count mismatch at line " + lineNo);
                String id = cols.getFirst().trim();
                if (id.isBlank()) throw new IOException("Blank identifier at line " + lineNo);
                if (profiles.containsKey(id)) throw new IOException("Duplicate identifier '" + id + "' at line " + lineNo + "; resolve duplicates explicitly");
                ExpressionProfile profile = new ExpressionProfile(id);
                for (int i = 1; i < cols.size(); i++) {
                    String raw = cols.get(i).trim();
                    if (raw.isEmpty() || List.of("NA", "N/A", ".", "NULL").contains(raw.toUpperCase(java.util.Locale.ROOT))) { missing++; continue; }
                    try {
                        double value = Double.parseDouble(raw);
                        if (!Double.isFinite(value)) throw new NumberFormatException();
                        if (value < 0 && List.of("TPM", "FPKM", "COUNTS", "COUNT").contains(valueUnit.toUpperCase(java.util.Locale.ROOT))) throw new NumberFormatException();
                        profile.put(tissues.get(i - 1), value, 0);
                    } catch (NumberFormatException ex) {
                        invalid++;
                        if (warnings.size() < 20) warnings.add("Invalid value at line " + lineNo + ", column " + (i + 1) + ": " + raw);
                    }
                }
                profiles.put(id, profile);
            }
        }
        if (!headerSeen || profiles.isEmpty()) throw new IOException("No expression records found");
        if (valueUnit.equals("unspecified")) warnings.add("Unit not declared; values are shown as unspecified.");
        warnings.add("Measurement level: " + level + "; no aggregation or ID version conversion applied.");
        report = new ImportReport(rows, profiles.size(), missing, invalid, 0, 0, List.of(), List.copyOf(warnings));
    }

    /** Detects the matrix delimiter using only characters outside quoted fields. */
    private static char detectDelimiter(String line) {
        int tabs = 0, commas = 0, semicolons = 0;
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    i++;
                } else {
                    quoted = !quoted;
                }
                continue;
            }
            if (!quoted) {
                switch (c) {
                    case '\t' -> tabs++;
                    case ',' -> commas++;
                    case ';' -> semicolons++;
                    default -> { }
                }
            }
        }
        char delimiter = '\t';
        int best = tabs;
        if (commas > best) { delimiter = ','; best = commas; }
        if (semicolons > best) { delimiter = ';'; }
        return best == 0 ? '\t' : delimiter;
    }

    /** Single-line RFC-style quoted fields, fixed delimiter chosen from the header. */
    private static List<String> splitRow(String line, char delimiter) throws IOException {
        List<String> result = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false, closed = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') { cell.append('"'); i++; }
                else if (c == '"') { quoted = false; closed = true; }
                else cell.append(c);
            } else if (c == delimiter) {
                result.add(cell.toString()); cell.setLength(0); closed = false;
            } else if (c == '"' && cell.isEmpty() && !closed) quoted = true;
            else if (closed) throw new IOException("Unexpected characters after a quoted field");
            else cell.append(c);
        }
        if (quoted) throw new IOException("Unclosed quoted field; multiline fields are not supported");
        result.add(cell.toString());
        return result;
    }

    // ------------------------------------------------------------------
    // Queries
    // ------------------------------------------------------------------

    public List<String> getTissues() {
        return List.copyOf(tissues);
    }

    public ExpressionProfile getRawProfile(String inputId) { return profiles.get(inputId); }
    /** Views use only successfully reconciled annotation IDs, never an ambiguous raw-ID fallback. */
    public ExpressionProfile getProfileForAnnotation(String annotationId) {
        String input = annotationIds.get(annotationId);
        return input == null ? null : profiles.get(input);
    }

    public ExpressionProfile getProfile(String geneId) {
        return profiles.get(annotationIds.getOrDefault(geneId, geneId));
    }

    public boolean hasProfile(String geneId) {
        return getProfile(geneId) != null;
    }

    public int getProfileCount() {
        return profiles.size();
    }

    public String getSource() {
        return source;
    }

    /** Heading of the identifier column as written in the data file. */
    public String getIdColumnLabel() {
        return idColumnLabel;
    }

    /** Measurement unit declared in the file, used for axis and legend labels. */
    public String getValueUnit() {
        return valueUnit;
    }

    public boolean isEmpty() {
        return profiles.isEmpty();
    }

    /**
     * Highest value observed anywhere in the dataset, used to scale heat-map colour.
     */
    public double globalMaxTpm() {
        return profiles.values().stream().flatMap(p -> p.getValues().stream())
                .mapToDouble(v -> Math.abs(v.tpm())).max().orElse(1.0);
    }

    /**
     * Summary of the most highly expressed sample group for one gene.
     */
    public String describe(String geneId) {
        ExpressionProfile profile = getProfile(geneId);
        if (profile == null || profile.isEmpty()) {
            return "no expression data";
        }
        TissueExpression top = profile.topTissue();
        return top == null ? "no expression data"
                : String.format("highest in %s (%.1f %s)", top.tissue(), top.tpm(), valueUnit);
    }
}
