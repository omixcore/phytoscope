package io.gitee.omixcore.phytoscope;

import io.gitee.omixcore.phytoscope.model.Gene;
import io.gitee.omixcore.phytoscope.service.GenomeDataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.*;

/** Full-source GFF3 checks for two unrelated medicinal/ornamental non-model plants. */
@EnabledIfSystemProperty(named = "phytoscope.additionalReferenceDir", matches = ".+")
class AdditionalNonModelReferenceTest {
    private static final Path DATA = Path.of(System.getProperty("phytoscope.additionalReferenceDir"));
    private static final Set<String> GENE_TYPES = Set.of("gene", "protein_coding_gene", "pseudogene", "ncrna_gene",
            "lncrna_gene", "mirna_gene", "snrna_gene", "snorna_gene", "rrna_gene", "transposable_element_gene");

    @Test void importsAndQueriesAndrographisPaniculataRefSeq() throws Exception {
        validate("GCF_009805555.1_ASM980555v1_genomic.gff.gz", 27_712,
                "LOC127243311", "NW_026137634.1", 2_929_092, 2_934_822, null, 11);
    }

    @Test void importsAndQueriesHibiscusSyriacusRefSeq() throws Exception {
        validate("GCF_006381635.1_ASM638163v2_genomic.gff.gz", 106_303,
                "LOC120212642", "NW_024062564.1", 758_339, 760_358, '-', -1);
    }

    private static void validate(String file, int expectedGenes, String targetId,
                                  String targetSequence, int start, int end,
                                  Character strand, int expectedExons) throws Exception {
        Path source = DATA.resolve(file);
        Set<String> sourceSequences = new HashSet<>();
        Set<String> sourceGeneIds = new HashSet<>();
        try (var raw = new GZIPInputStream(java.nio.file.Files.newInputStream(source));
             var reader = new BufferedReader(new InputStreamReader(raw, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("##sequence-region")) {
                    sourceSequences.add(line.trim().split("\\s+")[1]);
                } else if (!line.isBlank() && !line.startsWith("#")) {
                    String[] fields = line.split("\\t", 9);
                    if (fields.length != 9) continue;
                    sourceSequences.add(fields[0]);
                    if (GENE_TYPES.contains(fields[2].toLowerCase())) {
                        for (String attribute : fields[8].split(";")) {
                            if (attribute.startsWith("ID=")) sourceGeneIds.add(attribute.substring(3));
                        }
                    }
                }
            }
        }
        assertEquals(expectedGenes, sourceGeneIds.size(), "independent source gene inventory");

        var genome = new GenomeDataService();
        genome.loadFromFile(source);
        assertEquals(expectedGenes, genome.getGeneCount());
        assertEquals(sourceSequences, new HashSet<>(genome.getChromosomes()));
        assertTrue(genome.getChromosomes().contains(targetSequence));
        var hits = genome.search(targetId);
        Gene gene = genome.findGene("gene-" + targetId);
        if (gene == null && !hits.isEmpty()) gene = hits.stream().filter(g -> targetId.equals(g.getName())).findFirst().orElse(null);
        assertNotNull(gene, "gene search resolves public NCBI locus " + targetId);
        String resolvedGeneId = gene.getId();
        assertTrue(hits.stream().anyMatch(hit -> hit.getId().equals(resolvedGeneId)));
        assertEquals(targetSequence, gene.getChromosome());
        assertEquals(start, gene.getStart());
        assertEquals(end, gene.getEnd());
        if (strand != null) assertEquals(strand.charValue(), gene.getStrand());
        assertFalse(gene.getTranscripts().isEmpty());
        if (expectedExons > 0) {
            assertTrue(gene.getTranscripts().stream().anyMatch(tx -> tx.getExonCount() == expectedExons),
                    "at least one transcript matches the NCBI Gene exon-count summary");
        }
    }
}
