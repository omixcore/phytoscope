package io.gitee.omixcore.phytoscope;

import io.gitee.omixcore.phytoscope.service.ExpressionDataService;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class ExpressionDelimiterTest {
    @Test
    void detectsDelimiterOutsideQuotedFields() throws Exception {
        Path p = Files.createTempFile("phytoscope-delimiter-", ".csv");
        try {
            Files.writeString(p, "Gene;\"Root, stressed\";Leaf\nA;1.5;2.5\n");
            var service = new ExpressionDataService();
            service.loadFromFile(p);
            assertEquals(java.util.List.of("Root, stressed", "Leaf"), service.getTissues());
            assertEquals(1.5, service.getProfile("A").getTpm("Root, stressed"), 1e-9);
            assertEquals(2.5, service.getProfile("A").getTpm("Leaf"), 1e-9);
        } finally {
            Files.deleteIfExists(p);
        }
    }
}
