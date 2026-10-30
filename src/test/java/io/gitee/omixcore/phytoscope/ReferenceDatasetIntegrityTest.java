package io.gitee.omixcore.phytoscope;

import io.gitee.omixcore.phytoscope.service.ExampleDataset;
import io.gitee.omixcore.phytoscope.service.ExpressionDataService;
import io.gitee.omixcore.phytoscope.service.GenomeDataService;
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
        if ("arabidopsis".equals(id)) {
            assertEquals("/data/case-studies/arabidopsis-atlas/expression.tsv", dataset.expressionResource());
            var expression = new ExpressionDataService();
            expression.loadExample(dataset);
            assertTrue(expression.getProfileCount() >= 20000);
            assertEquals(6, expression.getTissues().size());
            assertEquals("TPM", expression.getValueUnit());
        } else {
            assertNull(dataset.expressionResource(), "Untraceable measured expression must not be bundled");
            var expression = new ExpressionDataService();
            expression.loadExample(dataset);
            assertTrue(expression.isEmpty());
        }
        assertTrue(dataset.publishedSequenceLengths().isEmpty(), "Lengths come from the original GFF3 header");
    }

    @Test
    void arabidopsisMultiPartAnnotationLoadsAllFiveChromosomes() throws Exception {
        // Regression: the gzip migration split Arabidopsis into five
        // chromosome-sized .gff3.gz parts. Loading must decompress each part
        // independently and surface all five sequences — an earlier bug
        // concatenated the raw streams, so only Chr1 was parsed and every
        // gene on Chr2-Chr5 (e.g. AT5G38410) vanished.
        var genome = new GenomeDataService();
        genome.loadExample(ExampleDataset.byId("arabidopsis"));
        assertEquals(java.util.List.of("Chr1", "Chr2", "Chr3", "Chr4", "Chr5"),
                genome.getChromosomes(), "all five chromosomes must load");
        assertNotNull(genome.findGene("AT1G67090"), "Chr1 locus must be present");
        assertNotNull(genome.findGene("AT5G38410"), "Chr5 locus must be present");
        assertNotNull(genome.findGene("AT5G38420"), "Chr5 locus must be present");
        assertNotNull(genome.findGene("AT5G38430"), "Chr5 locus must be present");
        assertEquals("Chr5", genome.findGene("AT5G38410").getChromosome());
    }

    @Test
    void catalogueContainsOnlyReviewedReferences() {
        assertEquals(7, ExampleDataset.CATALOGUE.size());
        assertNotNull(ExampleDataset.byId("mint"));
        assertNotNull(ExampleDataset.byId("mint-genome"));
        assertNotNull(ExampleDataset.byId("mint-plastid"));
        assertSame(ExampleDataset.byId("soybean"), ExampleDataset.byId("soybean-v2"));
        assertNotNull(getClass().getResource("/data/reference-provenance.json"));
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

    @Test
    void mintGenomeExampleCarriesTheCompletePublishedAnnotation() throws Exception {
        var dataset = ExampleDataset.byId("mint-genome");
        assertEquals("Mentha longifolia", dataset.species());
        assertEquals("Long3_HAP_v2 (haplotype A)", dataset.assembly());
        assertEquals("/data/species/mint/mentha_longifolia_expression.tsv", dataset.expressionResource(),
                "the leaf RNA-seq panel measured on this very assembly is bundled");

        var genome = new GenomeDataService();
        genome.loadExample(dataset);
        assertEquals(12, genome.getChromosomes().size(), "haplotype A is chromosome-scale");
        assertEquals(94_883, genome.getGeneCount(), "every Braker3 gene model is present");
        assertEquals("Chr1", genome.getActiveSequence());
        assertArrayEquals(new int[]{1, 39_654_341}, genome.getActiveSequenceSpan());

        var expression = new ExpressionDataService();
        expression.loadExample(dataset);
        assertEquals("TPM", expression.getValueUnit());
        assertEquals(4, expression.getTissues().size(), "four leaf libraries");
        assertEquals(94_883, expression.getProfileCount(), "every gene carries a profile");
        assertEquals(94_883, expression.reconcile(genome).matchedIds(), "matrix ids match the annotation");
    }
}
