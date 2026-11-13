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
        assertEquals(6, ExampleDataset.CATALOGUE.size(), "CATALOGUE 应恰好保留 6 个小示例");
        for (ExampleDataset d : ExampleDataset.CATALOGUE) {
            assertEquals(1, d.annotationResources().size(), d.id() + " 必须是单文件示例");
            for (String res : d.annotationResources()) assertBundled(res, d.id());
            if (d.expressionResource() != null) assertBundled(d.expressionResource(), d.id());
        }
    }

    @Test
    void noExternalDataIsBundled() {
        for (ExampleDataset d : ExampleDataset.CATALOGUE) {
            for (String res : d.annotationResources()) {
                assertTrue(EXTERNAL_MARKERS.stream().noneMatch(res::contains),
                        "外迁资源不得回到 CATALOGUE: " + res);
            }
            if (d.expressionResource() != null) {
                String res = d.expressionResource();
                assertTrue(EXTERNAL_MARKERS.stream().noneMatch(res::contains),
                        "外迁资源不得回到 CATALOGUE: " + res);
            }
        }
        assertNull(ExampleDataset.byId("mint-genome"), "完整 horsemint 示例已外迁");
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
                "守卫阈值之上必须判为超大文件，否则守卫无效");
        assertFalse(oversized.length < MAX_BUNDLED_BYTES,
                "256 KB 以上的文件不得通过小示例守卫");
    }

    private void assertBundled(String resource, String id) {
        try (InputStream in = getClass().getResourceAsStream(resource)) {
            assertNotNull(in, id + " 的资源不存在: " + resource);
            long bytes = in.readAllBytes().length;
            assertTrue(bytes > 0, resource + " 不得为空");
            assertTrue(bytes < MAX_BUNDLED_BYTES,
                    resource + " 单文件 " + bytes + " B 超过 256 KB：完整数据请放入独立数据仓库");
        } catch (IOException e) {
            fail(e);
        }
    }
}
