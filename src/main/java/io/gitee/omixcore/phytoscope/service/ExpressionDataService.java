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
    private String valueUnit = "TPM";

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
        valueUnit = "TPM";
    }

    private void parse(InputStream in) throws IOException {
        tissues.clear();
        profiles.clear();
        idColumnLabel = "GeneID";
        valueUnit = "TPM";

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            boolean headerSeen = false;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                // Strip a UTF-8 BOM if the writer left one on the first line,
                // otherwise the leading '#' is masked and the header is misread.
                if (firstLine) {
                    firstLine = false;
                    if (!line.isEmpty() && line.charAt(0) == '\uFEFF') {
                        line = line.substring(1);
                    }
                }
                if (line.isBlank()) {
                    continue;
                }
                if (line.startsWith("#")) {
                    harvestHeaderMetadata(line);
                    continue;
                }
                String[] cols = splitRow(line);
                if (cols.length < 2) {
                    continue;
                }

                if (!headerSeen) {
                    // Header row: first column names the identifier column,
                    // the rest name the sample groups.
                    idColumnLabel = cols[0].trim();
                    for (int i = 1; i < cols.length; i++) {
                        String name = cols[i].trim();
                        if (!name.isEmpty()) {
                            tissues.add(name);
                        }
                    }
                    headerSeen = true;
                    continue;
                }

                String geneId = cols[0].trim();
                if (geneId.isEmpty()) {
                    continue;
                }
                ExpressionProfile profile = new ExpressionProfile(geneId);
                for (int i = 1; i < cols.length && i <= tissues.size(); i++) {
                    String raw = cols[i].trim();
                    if (raw.isEmpty() || raw.equalsIgnoreCase("NA") || raw.equals(".")) {
                        continue;
                    }
                    try {
                        double value = Double.parseDouble(raw);
                        profile.put(tissues.get(i - 1), value, 0);
                    } catch (NumberFormatException ignored) {
                        // Skip malformed cells rather than failing the whole load
                    }
                }
                profiles.put(geneId, profile);
            }
        }
    }

    /** Detects the delimiter from a data row, so TSV, CSV and semicolon files all load. */
    private static String[] splitRow(String line) {
        if (line.indexOf('\t') >= 0) {
            return line.split("\t", -1);
        }
        if (line.indexOf(',') >= 0) {
            return line.split(",", -1);
        }
        if (line.indexOf(';') >= 0) {
            return line.split(";", -1);
        }
        return line.split("\\s+", -1);
    }

    /** Reads the declared unit (TPM/FPKM/…) out of a comment line when present. */
    private void harvestHeaderMetadata(String line) {
        String lower = line.toLowerCase();
        if (lower.contains("unit")) {
            int eq = line.indexOf('=');
            String value = eq >= 0 ? line.substring(eq + 1) : line.substring(line.indexOf(':') + 1);
            String cleaned = value.replaceAll("[^A-Za-z0-9/]", " ").trim();
            if (!cleaned.isEmpty()) {
                String first = cleaned.split("\\s+")[0];
                if (first.length() <= 12 && first.matches("[A-Za-z0-9/]+")) {
                    valueUnit = first.toUpperCase();
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Queries
    // ------------------------------------------------------------------

    public List<String> getTissues() {
        return List.copyOf(tissues);
    }

    public ExpressionProfile getProfile(String geneId) {
        return profiles.get(geneId);
    }

    public boolean hasProfile(String geneId) {
        return profiles.containsKey(geneId);
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
        return profiles.values().stream()
                .mapToDouble(ExpressionProfile::maxTpm)
                .max()
                .orElse(1.0);
    }

    /**
     * Summary of the most highly expressed sample group for one gene.
     */
    public String describe(String geneId) {
        ExpressionProfile profile = profiles.get(geneId);
        if (profile == null || profile.isEmpty()) {
            return "no expression data";
        }
        TissueExpression top = profile.topTissue();
        return top == null ? "no expression data"
                : String.format("highest in %s (%.1f %s)", top.tissue(), top.tpm(), valueUnit);
    }
}
