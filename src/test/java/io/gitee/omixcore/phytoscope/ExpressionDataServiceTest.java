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

    /**
     * The default example no longer bundles the E-MTAB-7978 atlas matrix.
     * AC-3 requires a visible, explicit empty state instead of the silent
     * downgrade that the removed resource would otherwise cause.
     */
    @Test
    void defaultArabidopsisExampleHasExplicitEmptyExpressionState() throws Exception {
        ExpressionDataService service = new ExpressionDataService();
        service.loadExample(ExampleDataset.defaultDataset());
        assertNull(ExampleDataset.defaultDataset().expressionResource());
        assertTrue(service.isEmpty());
        assertEquals("No data loaded", service.getSource());
        assertEquals(0, service.getProfileCount());
        assertTrue(service.getTissues().isEmpty());
    }

    /**
     * Equivalent replacement for the archived atlas matrix test: an inline
     * six-column TPM fixture keeps the same assertion structure (six sample
     * groups, declared unit, exact numeric values, per-gene profile lookup and
     * finite-value checks). No real measurement is fabricated or asserted here;
     * the numbers belong to this synthetic fixture only.
     */
    @Test
    void inlineSixTissueFixtureLoadsWithExactValuesAndProfileQueries() throws Exception {
        String fixture = "# unit=TPM\n"
                + "GeneID\tRoot\tRosette_leaf_1\tRosette_leaf_8\tCauline_leaf\tDry_seed\tPollen\n"
                + "AT1G67090\t8.0\t17.0\t17494.0\t16.0\t3.0\t2.0\n"
                + "AT5G38430\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\n"
                + "AT1G01860\t8.0\t17.0\t15.0\t16.0\t3.0\t2.0\n";
        Path file = Files.createTempFile("phytoscope-six-tissue-", ".tsv");
        try {
            Files.writeString(file, fixture);
            ExpressionDataService service = new ExpressionDataService();
            service.loadFromFile(file);
            assertEquals(java.util.List.of("Root", "Rosette_leaf_1", "Rosette_leaf_8",
                    "Cauline_leaf", "Dry_seed", "Pollen"), service.getTissues());
            assertEquals("TPM", service.getValueUnit());
            assertEquals(3, service.getProfileCount(), "one profile per fixture row");
            assertEquals(17494.0, service.getProfile("AT1G67090").getTpm("Rosette_leaf_8"));
            assertEquals(0.0, service.getProfile("AT5G38430").getTpm("Pollen"));
            assertNotNull(service.getProfile("AT1G01860"));
            assertTrue(java.util.Arrays.stream(new double[]{
                    service.getProfile("AT1G01860").getTpm("Root"),
                    service.getProfile("AT1G01860").getTpm("Rosette_leaf_1"),
                    service.getProfile("AT1G01860").getTpm("Rosette_leaf_8"),
                    service.getProfile("AT1G01860").getTpm("Cauline_leaf"),
                    service.getProfile("AT1G01860").getTpm("Dry_seed"),
                    service.getProfile("AT1G01860").getTpm("Pollen")}).allMatch(Double::isFinite));
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
