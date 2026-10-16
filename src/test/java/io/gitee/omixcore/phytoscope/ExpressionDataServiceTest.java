package io.gitee.omixcore.phytoscope;

import io.gitee.omixcore.phytoscope.service.ExpressionDataService;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ExpressionDataServiceTest {

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
            assertEquals("TPM", service.getValueUnit());
            assertNull(service.getProfile("geneA"));
            assertNotNull(service.getProfile("geneB"));
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
