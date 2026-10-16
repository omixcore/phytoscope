package io.gitee.omixcore.phytoscope;

import io.gitee.omixcore.phytoscope.service.ExampleDataset;
import io.gitee.omixcore.phytoscope.service.ExpressionDataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/** The application must ship exactly the row-verified references used in validation. */
class ReferenceDatasetIntegrityTest {
    @ParameterizedTest
    @ValueSource(strings = {"arabidopsis", "rice", "maize", "soybean"})
    void shipsOnlyUnmodifiedOfficialReferenceExcerpts(String id) throws Exception {
        var dataset = ExampleDataset.byId(id);
        assertNotNull(dataset);
        if ("arabidopsis".equals(id)) {
            for (String resource : dataset.annotationResources()) {
                try (var actual = getClass().getResourceAsStream(resource)) {
                    assertNotNull(actual, resource);
                    assertTrue(actual.read() >= 0, resource + " must be non-empty");
                }
            }
            assertEquals(5, dataset.annotationResources().size());
        } else {
            try (var expected = getClass().getResourceAsStream("/chromosome-validation/" + id + ".gff3");
                 var actual = getClass().getResourceAsStream(dataset.annotationResource())) {
                assertNotNull(expected);
                assertNotNull(actual);
                assertArrayEquals(expected.readAllBytes(), actual.readAllBytes());
            }
        }
        assertNull(dataset.expressionResource(), "Untraceable measured expression must not be bundled");
        assertTrue(dataset.publishedSequenceLengths().isEmpty(), "Lengths come from the original GFF3 header");
        var expression = new ExpressionDataService();
        expression.loadExample(dataset);
        assertTrue(expression.isEmpty());
    }

    @Test
    void catalogueContainsOnlyFourReviewedReferences() {
        assertEquals(4, ExampleDataset.CATALOGUE.size());
        assertNull(ExampleDataset.byId("mint"));
        assertSame(ExampleDataset.byId("soybean"), ExampleDataset.byId("soybean-v2"));
        assertNotNull(getClass().getResource("/data/reference-provenance.json"));
        assertNull(getClass().getResource("/data/species/mint/mentha_spicata_plastid.gff3"));
    }
}
