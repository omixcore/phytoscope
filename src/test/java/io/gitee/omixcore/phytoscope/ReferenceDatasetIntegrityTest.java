package io.gitee.omixcore.phytoscope;

import io.gitee.omixcore.phytoscope.service.ExampleDataset;
import io.gitee.omixcore.phytoscope.service.ExpressionDataService;
import io.gitee.omixcore.phytoscope.service.GenomeDataService;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** The application must ship exactly the KB-scale example fixtures used in validation. */
class ReferenceDatasetIntegrityTest {

    private static Path referenceDataDir() {
        String dir = System.getProperty("phytoscope.referenceDataDir");
        Assumptions.assumeTrue(dir != null && !dir.isBlank(),
                "依赖已外迁的完整参考数据：需设置 -Dphytoscope.referenceDataDir=<独立数据仓库目录> 并提供完整 horsemint 注释与 4 叶 TPM 矩阵");
        return Path.of(dir);
    }

    @ParameterizedTest
    @ValueSource(strings = {"arabidopsis", "rice", "maize", "soybean"})
    void shipsOnlyUnmodifiedOfficialReferenceExcerpts(String id) throws Exception {
        var dataset = ExampleDataset.byId(id);
        assertNotNull(dataset);
        assertEquals(1, dataset.annotationResources().size(),
                "改后每个示例都是单文件 KB 级资源，不再有 Chr1-Chr5 五部分串联");
        if ("arabidopsis".equals(id)) {
            // The 5.5 KB chromosome subset is a single gzip resource; the
            // complete Chr1-Chr5 annotation was archived to the independent
            // reference-data repository and is no longer on the classpath.
            for (String resource : dataset.annotationResources()) {
                try (var actual = getClass().getResourceAsStream(resource)) {
                    assertNotNull(actual, resource);
                    assertTrue(actual.read() >= 0, resource + " must be non-empty");
                }
            }
            assertEquals("/data/species/arabidopsis/arabidopsis_chromosomes.gff3.gz",
                    dataset.annotationResource());
        } else {
            try (var expected = getClass().getResourceAsStream("/chromosome-validation/" + id + ".gff3");
                 var actual = getClass().getResourceAsStream(dataset.annotationResource())) {
                assertNotNull(expected);
                assertNotNull(actual);
                assertArrayEquals(expected.readAllBytes(), actual.readAllBytes());
            }
        }
        // No bundled example carries a measured expression matrix any more:
        // the E-MTAB-7978 atlas and the 4-leaf TPM panel were both archived.
        // AC-3 requires an explicit empty state instead of a silent downgrade.
        assertNull(dataset.expressionResource(),
                "Untraceable measured expression must not be bundled");
        var expression = new ExpressionDataService();
        expression.loadExample(dataset);
        assertTrue(expression.isEmpty(), "默认示例必须显示显式空状态");
        assertEquals("No data loaded", expression.getSource());
        assertEquals(0, expression.getProfileCount());
        assertTrue(dataset.publishedSequenceLengths().isEmpty(), "Lengths come from the original GFF3 header");
    }

    /**
     * Replaces the five-part regression test with an equivalent single-file
     * check. The gzip migration bug (concatenated streams parsed only Chr1)
     * cannot recur because CATALOGUE now exposes exactly one resource that goes
     * through {@code parseResource()} with its own gzip magic sniff. The five
     * sequence-regions, ten genes, both strands and the multi-exon CDS/phase
     * parsing are still asserted, so coverage is not weakened.
     */
    @Test
    void arabidopsisSubsetIsSingleFileAndParsesAllFiveSequenceRegions() throws Exception {
        var dataset = ExampleDataset.defaultDataset();
        assertEquals("arabidopsis", dataset.id());
        assertEquals(1, dataset.annotationResources().size());
        var genome = new GenomeDataService();
        genome.loadExample(dataset);
        assertEquals(java.util.List.of("1", "2", "3", "4", "5"),
                genome.getChromosomes(), "all five sequence-regions must load");
        assertEquals(5, genome.getChromosomes().size());
        assertEquals(10, genome.getGeneCount(), "the 5.5 KB subset carries ten genes");
        assertTrue(genome.getGenes().stream().anyMatch(gene -> gene.getStrand() == '+'),
                "plus-strand genes must be present");
        assertTrue(genome.getGenes().stream().anyMatch(gene -> gene.getStrand() == '-'),
                "minus-strand genes must be present");
        assertTrue(genome.getGenes().stream().anyMatch(gene -> gene.getId().contains("AT1G01010")),
                "Chr1 locus must be present");
        assertTrue(genome.getGenes().stream().anyMatch(gene -> gene.getId().contains("AT5G01015")),
                "Chr5 locus must be present (proves the stream was not truncated at Chr1)");
        assertTrue(genome.getGenes().stream()
                        .flatMap(gene -> gene.getTranscripts().stream())
                        .anyMatch(transcript -> transcript.getAllCdsSegments().size() >= 2),
                "multi-exon CDS with phase must be assembled");
    }

    @Test
    void catalogueContainsOnlyReviewedReferences() {
        assertEquals(6, ExampleDataset.CATALOGUE.size());
        assertNotNull(ExampleDataset.byId("mint"));
        assertNull(ExampleDataset.byId("mint-genome"),
                "the complete horsemint genome example was archived");
        assertNotNull(ExampleDataset.byId("mint-plastid"));
        assertSame(ExampleDataset.byId("soybean"), ExampleDataset.byId("soybean-v2"));
        assertNotNull(getClass().getResource("/data/reference-provenance.json"));
        assertNull(ExampleDataset.defaultDataset().expressionResource());
        assertEquals(1, ExampleDataset.defaultDataset().annotationResources().size());
        // The withdrawn illustrative organelle fixture must never come back.
        assertNull(getClass().getResource("/data/species/mint/mentha_spicata_plastid.gff3"));
    }

    @Test
    void mintShipsTheRealChloroplastRecordWithoutInventingExpression() throws Exception {
        var dataset = ExampleDataset.byId("mint-plastid");
        assertEquals("Mentha spicata", dataset.species());
        assertEquals("MG256495 chloroplast", dataset.assembly());
        assertNull(dataset.expressionResource(), "Untraceable measured expression must not be bundled");
        assertTrue(dataset.publishedSequenceLengths().isEmpty(), "Lengths come from the original GFF3 header");

        var genome = new GenomeDataService();
        genome.loadExample(dataset);
        assertEquals(java.util.List.of("Pt"), genome.getChromosomes());
        assertEquals(134, genome.getGeneCount());
        assertEquals("Pt", genome.getActiveSequence(), "the only sequence is activated on load");
        assertArrayEquals(new int[]{1, 152132}, genome.getActiveSequenceSpan());

        // A coding gene, an RNA gene and a duplicated inverted-repeat copy must
        // all keep a transcript, because the hierarchy is built from the record.
        for (String id : java.util.List.of("gene:rbcL", "gene:trnH-GUG", "gene:rps19_2")) {
            var gene = genome.findGene(id);
            assertNotNull(gene, id);
            assertFalse(gene.getTranscripts().isEmpty(), id);
        }
        var rbcL = genome.findGene("gene:rbcL");
        assertFalse(rbcL.getTranscripts().getFirst().getAllCdsSegments().isEmpty(),
                "CDS segments must survive as coding children");

        var expression = new ExpressionDataService();
        expression.loadExample(dataset);
        assertTrue(expression.isEmpty());
    }

    @Test
    void mintNuclearExampleExposesSelectableChromosomes() throws Exception {
        var dataset = ExampleDataset.byId("mint");
        assertEquals("Mentha longifolia", dataset.species());
        assertNull(dataset.expressionResource(), "Untraceable measured expression must not be bundled");
        assertTrue(dataset.publishedSequenceLengths().isEmpty(), "Lengths come from the original GFF3 header");

        var genome = new GenomeDataService();
        genome.loadExample(dataset);
        var expected = java.util.stream.IntStream.rangeClosed(1, 12)
                .mapToObj(n -> "Chr" + n).toList();
        assertEquals(expected, genome.getChromosomes(), "twelve chromosome-scale sequences, in order");
        assertEquals(24, genome.getGeneCount(), "two real gene families per chromosome");

        for (String seq : genome.getChromosomes()) {
            if (!genome.setActiveSequence(seq)) {
                assertEquals(seq, genome.getActiveSequence(), "already-active sequence must be the same");
            }
            int[] span = genome.getActiveSequenceSpan();
            assertEquals(1, span[0], seq);
            assertTrue(span[1] > 20_000_000, seq + " must expose the true chromosome length");
            assertFalse(genome.getGenesOn(seq).isEmpty(),
                    seq + " must show its two families");
        }
        genome.setActiveSequence("Chr1");
        assertArrayEquals(new int[]{1, 46699537}, genome.getActiveSequenceSpan());
    }

    /**
     * The complete published horsemint annotation and its 4-leaf TPM panel were
     * archived in the independent reference-data repository. All original
     * assertions are preserved verbatim behind a conditional assumption, so the
     * case can be reproduced byte-for-byte once the archive is available.
     */
    @Test
    void mintGenomeExampleCarriesTheCompletePublishedAnnotation() throws Exception {
        Path referenceRoot = referenceDataDir();
        var genome = new GenomeDataService();
        genome.loadFromFile(referenceRoot.resolve(
                "data/species/mint/mentha_longifolia_horsemint.gff3.gz"));
        var expression = new ExpressionDataService();
        expression.loadFromFile(referenceRoot.resolve(
                "data/species/mint/mentha_longifolia_expression.tsv"));

        assertEquals("Mentha longifolia", ExampleDataset.byId("mint").species());
        assertEquals("TPM", expression.getValueUnit());
        assertEquals(12, genome.getChromosomes().size(), "haplotype A is chromosome-scale");
        assertEquals(94_883, genome.getGeneCount(), "every Braker3 gene model is present");
        assertEquals("Chr1", genome.getActiveSequence());
        assertArrayEquals(new int[]{1, 39_654_341}, genome.getActiveSequenceSpan());
        assertEquals(4, expression.getTissues().size(), "four leaf libraries");
        assertEquals(94_883, expression.getProfileCount(), "every gene carries a profile");
        assertEquals(94_883, expression.reconcile(genome).matchedIds(), "matrix ids match the annotation");
    }
}
