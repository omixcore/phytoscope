package io.gitee.omixcore.phytoscope;

import io.gitee.omixcore.phytoscope.model.GenomeMetadata;
import io.gitee.omixcore.phytoscope.model.SequenceRegion;
import io.gitee.omixcore.phytoscope.service.ExampleDataset;
import io.gitee.omixcore.phytoscope.service.GenomeDataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Real Ensembl Plants 62 annotations, with synthetic edge cases labelled below. */
class AdaptiveChromosomeTest {
    @ParameterizedTest
    @CsvSource({"arabidopsis,5", "rice,12", "maize,10", "soybean,20"})
    void detectsEveryNuclearChromosomeFromRealAnnotations(String species, int expected) throws Exception {
        var service = new GenomeDataService();
        service.loadFromResource("/chromosome-validation/" + species + ".gff3", null);
        assertEquals(expected, service.getChromosomes().size());
        assertEquals(expected, service.getPopulatedChromosomes().size());
        assertEquals(expected * 2, service.getGeneCount());
        // Importing an annotation does not consult a species catalogue.
        assertEquals(java.util.stream.IntStream.rangeClosed(1, expected)
                .mapToObj(Integer::toString).toList(), service.getChromosomes());
        try (var reader = new BufferedReader(new InputStreamReader(
                getClass().getResourceAsStream("/chromosome-validation/expected.tsv"), StandardCharsets.UTF_8))) {
            for (String line : reader.lines().skip(1).toList()) {
                String[] c = line.split("\t");
                if (!species.equals(c[0])) continue;
                service.setActiveSequence(c[1]);
                assertEquals(c[1], service.getActiveSequence());
                assertArrayEquals(new int[]{Integer.parseInt(c[2]), Integer.parseInt(c[3])},
                        service.getActiveSequenceSpan());
                var genes = service.getGenesOnActiveSequence();
                assertEquals(2, genes.size());
                for (var gene : genes) {
                    assertEquals(c[1], gene.getChromosome());
                    assertTrue(gene.getStart() >= 1);
                    assertTrue(gene.getEnd() <= Integer.parseInt(c[3]));
                    assertFalse(gene.getTranscripts().isEmpty(), "Real transcript hierarchy must survive");
                    assertTrue(gene.getTranscripts().stream().anyMatch(t -> !t.getExons().isEmpty()));
                    assertTrue(service.search(gene.getId()).contains(gene));
                }
            }
        }
    }

    @Test
    void switchesBetweenDifferentGenomeSizesWithoutStaleSequences() throws Exception {
        var service = new GenomeDataService();
        for (String species : List.of("soybean", "rice", "maize", "arabidopsis", "soybean")) {
            service.loadFromResource("/chromosome-validation/" + species + ".gff3", null);
            assertEquals("1", service.getActiveSequence());
            String last = service.getChromosomes().getLast();
            assertTrue(service.setActiveSequence(last));
            assertEquals(2, service.getGenesOnActiveSequence().size());
            assertFalse(service.setActiveSequence("not-in-this-assembly"));
            assertEquals(last, service.getActiveSequence());
        }
        service.loadFromContent("##gff-version 3\n");
        assertTrue(service.getChromosomes().isEmpty());
        assertNull(service.getActiveSequence());
        assertNull(service.getActiveSequenceSpan());
        assertNull(service.findGene("gene:AT1G01010"));
    }

    @Test
    void allBundledDatasetsExposeOnlyTheirOwnSequences() throws Exception {
        var service = new GenomeDataService();
        for (var dataset : ExampleDataset.CATALOGUE) {
            service.loadExample(dataset);
            assertFalse(service.getChromosomes().isEmpty());
            assertEquals(service.getChromosomes().size(), service.getMetadata().getSequenceCount());
            assertEquals(service.getChromosomes(), service.getPopulatedChromosomes());
            for (String seq : service.getChromosomes()) {
                service.setActiveSequence(seq);
                assertTrue(service.getGenesOnActiveSequence().stream().allMatch(g -> seq.equals(g.getChromosome())));
                assertTrue(service.getActiveSequenceSpan()[1] >= service.getGenesOnActiveSequence()
                        .stream().mapToInt(g -> g.getEnd()).max().orElse(0));
            }
        }
    }

    @Test
    void mergesDuplicateAndIncompleteHeadersAndStartsOnAPopulatedSequence() throws Exception {
        // Synthetic regression case: mixed, repeated, invalid and incomplete declarations.
        var service = new GenomeDataService();
        service.loadFromContent("""
                ##gff-version 3
                ##sequence-region Pt 1 1000
                ##sequence-region chrA 1 100
                ##sequence-region chrA 1 200
                ##sequence-region invalid 500 1
                chrA\tsrc\tgene\t150\t300\t.\t+\t.\tID=a
                scaffold42\tsrc\tgene\t20\t70\t.\t+\t.\tID=b
                """);
        assertEquals(List.of("Pt", "chrA", "scaffold42"), service.getChromosomes());
        assertEquals("chrA", service.getActiveSequence());
        assertArrayEquals(new int[]{1, 300}, service.getActiveSequenceSpan());
        assertTrue(service.setActiveSequence("Pt"));
        assertTrue(service.getGenesOnActiveSequence().isEmpty());
        assertArrayEquals(new int[]{1, 1000}, service.getActiveSequenceSpan());
    }

    @Test
    void metadataCannotCountDuplicateIdsOrShrinkAnObservedEnd() {
        var metadata = GenomeMetadata.fromAnnotation(null, null, "", "synthetic", List.of(
                new SequenceRegion("contig", 4200, 35200), new SequenceRegion("contig", 1, 100)));
        assertEquals(1, metadata.getSequenceCount());
        var untouched = metadata.withSequenceLengths(Map.of("contig", 33000));
        assertEquals(35200, untouched.findRegion("contig").end());
        var inferred = GenomeMetadata.fromAnnotation(null, null, "", "synthetic", List.of(
                new SequenceRegion("contig", 4200, 35200)));
        assertEquals(35200, inferred.withSequenceLengths(Map.of("contig", 33000)).findRegion("contig").end());
        assertEquals(1, inferred.withSequenceLengths(Map.of("contig", 35200)).findRegion("contig").start());
    }
}
