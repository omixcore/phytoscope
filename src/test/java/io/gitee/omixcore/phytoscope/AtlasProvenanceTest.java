package io.gitee.omixcore.phytoscope;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The 845 KB E-MTAB-7978 expression matrix was archived in the independent
 * reference-data repository. This test now validates the retained provenance
 * metadata instead of loading that matrix, keeping the "no fabricated numbers"
 * and byte-exact SHA-256 discipline: the two small metadata files are still
 * bundled and are checked byte-for-byte, and all 24 pinned source rows keep
 * their five identical summary components.
 */
class AtlasProvenanceTest {
    private static final String BASE = "/data/case-studies/arabidopsis-atlas/";

    private byte[] read(String name) throws Exception {
        try (var in = getClass().getResourceAsStream(BASE + name)) {
            assertNotNull(in, name);
            return in.readAllBytes();
        }
    }

    @Test
    void archivedMatrixIsMarkedExternalAndPinnedSourceCellsRemainUnmodified() throws Exception {
        var manifest = new ObjectMapper().readTree(read("manifest.json"));
        assertEquals("E-MTAB-7978", manifest.get("accession").asText());
        assertEquals(32833, manifest.get("expression_matrix_gene_count").asInt());
        assertEquals(6, manifest.get("expression_matrix_sample_count").asInt());

        // The complete matrix no longer lives on the classpath; its manifest
        // entry is now an explicit external pointer (historical / migration
        // marker only, never a load path).
        JsonNode outputs = manifest.get("outputs");
        assertNotNull(outputs, "");
        JsonNode expressionEntry = outputs.get("expression.tsv");
        assertNotNull(expressionEntry, "");
        assertTrue(expressionEntry.isObject(), "");
        assertEquals("external", expressionEntry.get("storage").asText());
        assertTrue(expressionEntry.get("repository").isNull(), "");
        assertEquals("data/case-studies/arabidopsis-atlas/expression.tsv",
                expressionEntry.get("external_path").asText());
        assertTrue(expressionEntry.get("sha256").asText().matches("[0-9a-f]{64}"),
                "");
        // Check the source tree (deterministic). A stale copy in target/classes
        // is a build artefact, removed by `mvn clean package` and verified by
        // AC-6 G4/G5; it must never be treated as proof that the file is bundled.
        assertFalse(Files.exists(Path.of("src/main/resources/data/case-studies/arabidopsis-atlas/expression.tsv")),
                "");

        // Two small metadata files are still bundled: byte-for-byte SHA-256
        // checks are preserved verbatim in spirit (no assertNotNull shortcut).
        for (String file : List.of("samples.tsv", "source-values.tsv")) {
            String actual = HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(read(file)));
            assertEquals(outputs.get(file).asText(), actual, file);
        }

        // Keep the pinned-source structure: six single-assay groups and 24
        // rows whose five summary components are identical. These are structural
        // checks on retained bytes, not invented measurement values.
        Map<String, String> groups = new HashMap<>();
        for (String line : new String(read("samples.tsv"), StandardCharsets.UTF_8)
                .lines().skip(1).toList()) {
            var c = line.split("\t");
            assertEquals("1", c[4], "");
            groups.put(c[1], c[0]);
        }
        assertEquals(6, groups.size(), "");

        int checked = 0;
        for (String line : new String(read("source-values.tsv"), StandardCharsets.UTF_8)
                .lines().skip(1).toList()) {
            var c = line.split("\t");
            var components = c[2].split(",");
            assertEquals(5, components.length, "");
            for (String v : components) {
                assertEquals(components[0], v, "");
            }
            checked++;
        }
        assertEquals(24, checked, "");
    }
}
