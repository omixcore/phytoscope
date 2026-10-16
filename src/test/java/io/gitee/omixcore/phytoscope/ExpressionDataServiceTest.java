package io.gitee.omixcore.phytoscope;

import io.gitee.omixcore.phytoscope.service.ExpressionDataService;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ExpressionDataServiceTest {

    @Test
    void detectsSemicolonWhenCommaOccursInsideQuotedHeader() throws Exception {
        Path file = Files.createTempFile("phytoscope-expression-delimiter-", ".csv");
        try {
            Files.writeString(file, "Gene;\"Root, stressed\";Leaf\nA;1.5;2.5\n");
            ExpressionDataService service = new ExpressionDataService();
            service.loadFromFile(file);
            assertEquals(java.util.List.of("Root, stressed", "Leaf"), service.getTissues());
            assertEquals(1.5, service.getProfile("A").getTpm("Root, stressed"), 1e-9);
            assertEquals(2.5, service.getProfile("A").getTpm("Leaf"), 1e-9);
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void failedDelimiterAwareImportDoesNotReplaceExistingData() throws Exception {
        Path file = Files.createTempFile("phytoscope-expression-failure-", ".tsv");
        try {
            Files.writeString(file, "Gene\tRoot\nA\t2\n");
            ExpressionDataService service = new ExpressionDataService();
            service.loadFromFile(file);
            Files.writeString(file, "Gene;\"Root, stressed\";Leaf\nA;1.5\n");
            assertThrows(IOException.class, () -> service.loadFromFile(file));
            assertEquals(2.0, service.getProfile("A").getTpm("Root"), 1e-9);
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void loadsArbitrarySampleNamesAndResetsMetadata() throws Exception {
        Path file = Files.createTempFile("phytoscope-expression-", ".tsv");
        try {
            Files.writeString(file, "# unit=FPKM\nLocus\tRoot_V1\tStress\n"
                    + "geneA\t12.5\tNA\n");
            ExpressionDataService service = new ExpressionDataService();
            service.loadFromFile(file);
            assertEquals(java.util.List.of("Root_V1", "Stress"), service.getTissues());
            assertEquals("Locus", service.getIdColumnLabel());
            assertEquals("FPKM", service.getValueUnit());
            assertEquals(1, service.getProfileCount());

            Files.writeString(file, "GeneID\tLeaf\n"
                    + "geneB\t3.0\n");
            service.loadFromFile(file);
            assertEquals(java.util.List.of("Leaf"), service.getTissues());
            assertEquals("GeneID", service.getIdColumnLabel());
            assertEquals("unspecified", service.getValueUnit());
            assertNull(service.getProfile("geneA"));
            assertNotNull(service.getProfile("geneB"));
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
