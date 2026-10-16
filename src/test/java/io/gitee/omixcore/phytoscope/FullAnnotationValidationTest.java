package io.gitee.omixcore.phytoscope;

import io.gitee.omixcore.phytoscope.service.GenomeDataService;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.*;

/** Optional local validation against full downloaded source archives; never downloads in tests. */
@EnabledIfSystemProperty(named = "phytoscope.fullAnnotationDir", matches = ".+")
class FullAnnotationValidationTest {
    @ParameterizedTest
    @CsvSource({"arabidopsis,5", "rice,12", "maize,10", "soybean,20"})
    void validatesFullArchive(String species, int nuclearCount) throws Exception {
        Path archive = Path.of(System.getProperty("phytoscope.fullAnnotationDir"), species + ".gff3.gz");
        Path annotation = Files.createTempFile("phytoscope-" + species, ".gff3");
        try {
            try (var input = new GZIPInputStream(Files.newInputStream(archive));
                 var output = Files.newOutputStream(annotation)) {
                input.transferTo(output);
            }
            // Independent streaming inventory, not based on the application parser.
            var sequences = new LinkedHashSet<String>();
            var geneIds = new HashSet<String>();
            Map<String, Integer> genesPerSequence = new HashMap<>();
            try (BufferedReader reader = Files.newBufferedReader(annotation, StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("##sequence-region")) {
                        sequences.add(line.trim().split("\\s+")[1]);
                    } else if (!line.startsWith("#")) {
                        String[] c = line.split("\t");
                        if (c.length != 9) continue;
                        sequences.add(c[0]);
                        if (c[2].equals("gene")) {
                            for (String attribute : c[8].split(";")) {
                                if (attribute.startsWith("ID=") && geneIds.add(attribute.substring(3))) {
                                    genesPerSequence.merge(c[0], 1, Integer::sum);
                                }
                            }
                        }
                    }
                }
            }
            var service = new GenomeDataService();
            service.loadFromFile(annotation);
            assertEquals(sequences, new LinkedHashSet<>(service.getChromosomes()));
            assertEquals(sequences.size(), service.getMetadata().getSequenceCount());
            var expectedNuclear = new LinkedHashSet<String>();
            for (int i = 1; i <= nuclearCount; i++) {
                expectedNuclear.add("arabidopsis".equals(species) ? "Chr" + i : Integer.toString(i));
            }
            assertEquals(expectedNuclear,
                    service.getChromosomes().stream()
                            .filter(expectedNuclear::contains)
                            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new)));
            assertEquals(geneIds.size(), service.getGeneCount());
            for (String seq : service.getChromosomes()) {
                service.setActiveSequence(seq);
                assertEquals(genesPerSequence.getOrDefault(seq, 0).intValue(), service.getGenesOnActiveSequence().size());
                assertTrue(service.getGenesOnActiveSequence().stream().allMatch(g -> seq.equals(g.getChromosome())));
                var span = service.getActiveSequenceSpan();
                assertNotNull(span);
                assertTrue(span[1] >= service.getGenesOnActiveSequence().stream().mapToInt(g -> g.getEnd()).max().orElse(0));
            }
            System.out.printf("FULL_ANNOTATION %s nuclear=%d total_sequences=%d genes=%d features=%d%n",
                    species, nuclearCount, sequences.size(), service.getGeneCount(), service.getRawFeatureCount());
        } finally {
            Files.deleteIfExists(annotation);
        }
    }
}
