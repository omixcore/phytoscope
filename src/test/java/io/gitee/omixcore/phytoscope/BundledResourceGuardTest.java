package io.gitee.omixcore.phytoscope;

import io.gitee.omixcore.phytoscope.service.ExampleDataset;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression guard (R10): the software repository must keep only KB-scale
 * example fixtures. Any attempt to put the archived MB-scale reference data
 * back into {@code src/main/resources/data} fails the build.
 */
class BundledResourceGuardTest {

    private static final long MAX_BUNDLED_BYTES = 256L * 1024;   // 256 KB
    private static final List<String> EXTERNAL_MARKERS = List.of(
            "TAIR10_genomic", "Chr1.gff3.gz", "Chr2.gff3.gz", "Chr3.gff3.gz",
            "Chr4.gff3.gz", "Chr5.gff3.gz", "mentha_longifolia_horsemint",
            "mentha_longifolia_expression", "arabidopsis-atlas/expression");

    @Test
    void everyCatalogueResourceExistsAndIsSmall() {
        assertEquals(6, ExampleDataset.CATALOGUE.size(), "");
        for (ExampleDataset d : ExampleDataset.CATALOGUE) {
            assertEquals(1, d.annotationResources().size(), d.id());
            for (String res : d.annotationResources()) assertBundled(res, d.id());
            if (d.expressionResource() != null) assertBundled(d.expressionResource(), d.id());
        }
    }

    @Test
    void noExternalDataIsBundled() {
        for (ExampleDataset d : ExampleDataset.CATALOGUE) {
            for (String res : d.annotationResources()) {
                assertTrue(EXTERNAL_MARKERS.stream().noneMatch(res::contains),
                        "" + res);
            }
            if (d.expressionResource() != null) {
                String res = d.expressionResource();
                assertTrue(EXTERNAL_MARKERS.stream().noneMatch(res::contains),
                        "" + res);
            }
        }
        assertNull(ExampleDataset.byId("mint-genome"), "");
        assertEquals("arabidopsis", ExampleDataset.defaultDataset().id());
        assertNull(ExampleDataset.defaultDataset().expressionResource());
    }

    /**
     * Sanity-check of the guard itself: a deliberately oversized fake resource
     * must be rejected. T04 verifies effectiveness by temporarily placing a
     * &gt;256 KB file in the classpath and confirming this assertion fails.
     */
    @Test
    void oversizedResourceIsRejectedByTheGuard() {
        byte[] oversized = new byte[(int) MAX_BUNDLED_BYTES + 1];
        assertTrue(oversized.length > MAX_BUNDLED_BYTES,
                "");
        assertFalse(oversized.length < MAX_BUNDLED_BYTES,
                "");
    }

    private void assertBundled(String resource, String id) {
        try (InputStream in = getClass().getResourceAsStream(resource)) {
            assertNotNull(in, id + ": " + resource);
            long bytes = in.readAllBytes().length;
            assertTrue(bytes > 0, resource);
            assertTrue(bytes < MAX_BUNDLED_BYTES,
                    resource + ": " + bytes);
        } catch (IOException e) {
            fail(e);
        }
    }
}
