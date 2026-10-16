package io.gitee.omixcore.phytoscope;

import io.gitee.omixcore.phytoscope.service.ExpressionDataService;
import io.gitee.omixcore.phytoscope.service.ExampleDataset;
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
            assertEquals("unspecified", service.getValueUnit());
            assertNull(service.getProfile("geneA"));
            assertNotNull(service.getProfile("geneB"));
        } finally {
            Files.deleteIfExists(file);
        }
    }
    @Test
    void defaultArabidopsisExampleLoadsBundledTranscriptome() throws Exception {
        ExpressionDataService service = new ExpressionDataService();
        service.loadExample(ExampleDataset.defaultDataset());
        assertFalse(service.isEmpty());
        assertEquals(4, service.getProfileCount());
        assertEquals("TPM", service.getValueUnit());
        assertEquals("Bundled panel (Arabidopsis thaliana)", service.getSource());
    }

    @Test
    void bundledArabidopsisTranscriptomeLoadsWithSixTissueGroups() throws Exception {
        ExpressionDataService service = new ExpressionDataService();
        service.loadFromResource("/data/case-studies/arabidopsis-atlas/expression.tsv", "Expression Atlas E-MTAB-7978");
        assertEquals(java.util.List.of("Root", "Rosette_leaf_1", "Rosette_leaf_8", "Cauline_leaf", "Dry_seed", "Pollen"), service.getTissues());
        assertEquals("TPM", service.getValueUnit());
        assertEquals(4, service.getProfileCount());
        assertEquals(17494.0, service.getProfile("AT1G67090").getTpm("Rosette_leaf_8"));
        assertEquals(0.0, service.getProfile("AT5G38430").getTpm("Pollen"));
    }

}
