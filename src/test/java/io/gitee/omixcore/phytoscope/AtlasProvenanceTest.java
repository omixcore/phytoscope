package io.gitee.omixcore.phytoscope;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gitee.omixcore.phytoscope.service.ExpressionDataService;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AtlasProvenanceTest {
    private static final String BASE = "/data/case-studies/arabidopsis-atlas/";
    private byte[] read(String name) throws Exception {
        try (var in = getClass().getResourceAsStream(BASE + name)) { assertNotNull(in); return in.readAllBytes(); }
    }
    @Test void allTwentyFourMeasurementsMatchPinnedUnmodifiedSourceCells() throws Exception {
        var manifest = new ObjectMapper().readTree(read("manifest.json"));
        assertEquals("E-MTAB-7978", manifest.get("accession").asText());
        for (String file : List.of("expression.tsv", "samples.tsv", "source-values.tsv")) {
            String actual = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(read(file)));
            assertEquals(manifest.get("outputs").get(file).asText(), actual, file);
        }
        var service = new ExpressionDataService(); service.loadFromResource(BASE+"expression.tsv", "E-MTAB-7978");
        Map<String,String> groups = new HashMap<>();
        for (String line : new String(read("samples.tsv"),StandardCharsets.UTF_8).lines().skip(1).toList()) {
            var c = line.split("\t"); assertEquals("1",c[4]); groups.put(c[1],c[0]);
        }
        int checked = 0;
        for (String line : new String(read("source-values.tsv"),StandardCharsets.UTF_8).lines().skip(1).toList()) {
            var c=line.split("\t"); var components=c[2].split(",");
            assertEquals(5,components.length);
            for (String v:components) assertEquals(components[0],v);
            assertEquals(Double.parseDouble(components[0]),service.getProfile(c[0]).getTpm(groups.get(c[1]))); checked++;
        }
        assertEquals(24, checked);
    }
}
