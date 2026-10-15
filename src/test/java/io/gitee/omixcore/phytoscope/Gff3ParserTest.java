package io.gitee.omixcore.phytoscope;

import io.gitee.omixcore.phytoscope.model.Exon;
import io.gitee.omixcore.phytoscope.model.ExpressionProfile;
import io.gitee.omixcore.phytoscope.model.Gene;
import io.gitee.omixcore.phytoscope.model.TissueExpression;
import io.gitee.omixcore.phytoscope.model.Transcript;
import io.gitee.omixcore.phytoscope.parser.Gff3Parser;
import io.gitee.omixcore.phytoscope.service.ExpressionDataService;
import io.gitee.omixcore.phytoscope.service.GenomeDataService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Historical illustrative fixtures for grammar and numeric-loader regression only.
 * These values are not authoritative biological data and do not validate a reference genome.
 * Verified public-reference checks live in AdaptiveChromosomeTest,
 * FullAnnotationValidationTest and ReferenceDatasetIntegrityTest.
 */
@DisplayName("Legacy parser and numeric expression fixtures (not measured data)")
class Gff3ParserTest {

    private static Gff3Parser.ParseResult result;

    @BeforeAll
    static void setUp() throws Exception {
        result = new Gff3Parser().parseResource(LegacyAnnotationFixtures.byId("arabidopsis").annotationResource());
    }

    @Test
    @DisplayName("Parses every sample gene across all five chromosomes")
    void shouldParseAllGenes() {
        assertEquals(29, result.genes().size());
    }

    @Test
    @DisplayName("Extracts species from the annotation header")
    void shouldExtractSpecies() {
        assertEquals("Arabidopsis thaliana", result.species());
    }

    @Test
    @DisplayName("Lists chromosomes correctly, in declaration order")
    void shouldListChromosomes() {
        assertEquals(List.of("Chr1", "Chr2", "Chr3", "Chr4", "Chr5"), result.chromosomes());
    }

    @Test
    @DisplayName("AT2G01008 on chromosome 2 keeps its real NC_003071 coordinates")
    void shouldAssembleAt2g01008() {
        Gene gene = findGene("AT2G01008");
        assertNotNull(gene, "the first protein-coding gene of Chr2 must be present");
        assertEquals("Chr2", gene.getChromosome());
        assertEquals('+', gene.getStrand());
        assertEquals(1025, gene.getStart());
        assertEquals(3173, gene.getEnd());

        Transcript transcript = gene.getTranscripts().get(0);
        assertEquals(3, transcript.getExonCount(), "mRNA join has three exons");
        assertEquals(2, transcript.getIntronCount());
    }

    @Test
    @DisplayName("AT1G01010: forward strand, 6 exons with 5'/3' UTRs")
    void shouldAssembleAt1g01010() {
        Gene gene = findGene("AT1G01010");
        assertNotNull(gene);
        assertEquals('+', gene.getStrand());
        assertEquals(3631, gene.getStart());
        assertEquals(5899, gene.getEnd());
        assertEquals(1, gene.getTranscripts().size());

        Transcript transcript = gene.getTranscripts().get(0);
        assertEquals(6, transcript.getExonCount());
        assertEquals(5, transcript.getIntronCount());

        // first exon carries a 5' UTR, last exon carries UTR/CDS
        List<Exon> exons = transcript.getExonsByCoordinate();
        assertFalse(exons.get(0).getUtrSegments().isEmpty(), "First exon should carry a UTR");
        assertFalse(exons.get(exons.size() - 1).getUtrSegments().isEmpty(), "Last exon should carry a UTR or CDS");
        assertTrue(transcript.getCdsLength() > 0, "CDS length should be greater than 0");
    }

    @Test
    @DisplayName("AT1G01020: reverse strand with two transcripts (alternative splicing)")
    void shouldAssembleAlternativeSplicing() {
        Gene gene = findGene("AT1G01020");
        assertNotNull(gene);
        assertEquals('-', gene.getStrand());
        assertEquals(2, gene.getTranscripts().size(), "should have two transcripts");

        Transcript first = gene.getTranscripts().get(0);
        Transcript second = gene.getTranscripts().get(1);
        assertEquals("AT1G01020.1", first.getDisplayName());
        assertEquals("AT1G01020.2", second.getDisplayName());

        // .1 has 9 exons; .2 has a longer 5' UTR (extending to 9130) adding one extra exon
        assertEquals(9, first.getExonCount(), ".1 transcript should have 9 exons");
        assertEquals(10, second.getExonCount(), ".2 transcript's longer 5' UTR adds an extra exon");
        assertTrue(second.getEnd() > first.getEnd(), "second transcript has a longer 3' end");
    }

    @Test
    @DisplayName("Exons on the minus strand are ordered along the transcription direction")
    void shouldSortExonsByTranscriptionDirection() {
        Gene gene = findGene("AT1G01020");
        Transcript transcript = gene.getTranscripts().get(0);
        List<Exon> sorted = transcript.getExonsByCoordinate();

        // By coordinate: the first exon has the smallest start
        assertTrue(sorted.get(0).getStart() < sorted.get(1).getStart());

        // By transcription order (descending on the minus strand): rank=1 is the highest coordinate
        List<Exon> byRank = transcript.getExons().stream()
                .sorted((a, b) -> Integer.compare(a.getRank(), b.getRank()))
                .toList();
        assertEquals(sorted.get(sorted.size() - 1).getStart(), byRank.get(0).getStart(),
                "On the minus strand rank=1 should be the highest-coordinate exon");
    }

    @Test
    @DisplayName("Computes intron intervals correctly")
    void shouldComputeIntrons() {
        Gene gene = findGene("AT1G01030");
        Transcript transcript = gene.getTranscripts().get(0);
        var introns = transcript.computeIntrons();

        assertEquals(3, introns.size(), "4 exons should produce 3 introns");
        for (var intron : introns) {
            assertTrue(intron.getEnd() >= intron.getStart(), "intron interval is invalid");
        }
    }

    @Test
    @DisplayName("Flags the representative transcript")
    void shouldMarkCanonicalTranscript() {
        Gene gene = findGene("AT1G01020");
        Transcript canonical = gene.getCanonicalTranscript();
        assertNotNull(canonical);
        assertTrue(canonical.isCanonical());
    }

    @Test
    @DisplayName("Parses gene attributes: aliases and description")
    void shouldParseGeneAttributes() {
        Gene gene = findGene("AT1G01040");
        assertNotNull(gene);
        assertEquals("DCL1,CAF,SUS1,SIN1", gene.getAlias());
        assertTrue(gene.getDescription().contains("dicer-like"));
    }

    @Test
    @DisplayName("Data service: searches genes by alias after loading")
    void shouldSearchByAlias() throws Exception {
        GenomeDataService service = new GenomeDataService();
        service.loadExample(LegacyAnnotationFixtures.byId("arabidopsis"));

        assertEquals(29, service.getGeneCount());
        assertFalse(service.search("DCL1").isEmpty(), "Alias DCL1 should retrieve AT1G01040");
        assertFalse(service.search("AT1G01020").isEmpty(), "Gene ID search should retrieve the gene");
        assertFalse(service.search("AT2G01008").isEmpty(),
                "Search covers every chromosome, including Chr2");
        assertTrue(service.search("NONEXISTENT_GENE").isEmpty(), "No match should return an empty list");

        // Fuzzy search by description keyword
        assertFalse(service.search("dicer").isEmpty(), "Description keyword search should retrieve the gene");
    }

    // ------------------------------------------------------------------
    // Tissue expression data
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Loads the six-tissue expression panel")
    void shouldLoadTissuePanel() throws Exception {
        ExpressionDataService service = new ExpressionDataService();
        service.loadFromResource(LegacyAnnotationFixtures.byId("arabidopsis").expressionResource(), "illustrative regression fixture");

        assertEquals(List.of("Root", "Stem", "Leaf", "Flower", "Silique", "Seed"),
                service.getTissues(), "Tissue panel must follow the file header order");
        assertEquals(22, service.getProfileCount(), "One profile per sample gene in this numeric fixture");
    }

    @Test
    @DisplayName("Reads per-tissue TPM values for a gene")
    void shouldReadTpmValues() throws Exception {
        ExpressionDataService service = new ExpressionDataService();
        service.loadFromResource(LegacyAnnotationFixtures.byId("arabidopsis").expressionResource(), "illustrative regression fixture");

        ExpressionProfile profile = service.getProfile("AT1G01010");
        assertNotNull(profile);
        assertEquals(14.27, profile.getTpm("Root"), 0.001);
        assertEquals(6.74, profile.getTpm("Leaf"), 0.001);
        assertEquals(0.0, profile.getTpm("Nonexistent"), 0.001,
                "Unknown tissues should read as zero");
    }

    @Test
    @DisplayName("Identifies the highest-expressing tissue")
    void shouldFindTopTissue() throws Exception {
        ExpressionDataService service = new ExpressionDataService();
        service.loadFromResource(LegacyAnnotationFixtures.byId("arabidopsis").expressionResource(), "illustrative regression fixture");

        ExpressionProfile profile = service.getProfile("AT1G01040");
        TissueExpression top = profile.topTissue();
        assertNotNull(top);
        assertEquals("Leaf", top.tissue(), "AT1G01040 peaks in Leaf in this panel");
        assertEquals(24.22, top.tpm(), 0.001);
    }

    @Test
    @DisplayName("Expression values are consistent with the loaded panel size")
    void shouldExposeFullPanelPerGene() throws Exception {
        ExpressionDataService service = new ExpressionDataService();
        service.loadFromResource(LegacyAnnotationFixtures.byId("arabidopsis").expressionResource(), "illustrative regression fixture");

        for (String geneId : List.of("AT1G01010", "AT1G01020", "AT1G01030",
                "AT1G01040", "AT1G01050")) {
            ExpressionProfile profile = service.getProfile(geneId);
            assertNotNull(profile, "Missing profile for " + geneId);
            assertEquals(service.getTissues().size(), profile.getValues().size(),
                    "Every gene should carry a value for each tissue");
            assertTrue(profile.sumTpm() > 0, "Total expression should be positive");
        }
    }

    @Test
    @DisplayName("Unknown genes have no expression profile")
    void shouldReturnNullForUnknownGene() throws Exception {
        ExpressionDataService service = new ExpressionDataService();
        service.loadFromResource(LegacyAnnotationFixtures.byId("arabidopsis").expressionResource(), "illustrative regression fixture");

        assertNull(service.getProfile("AT9G99999"));
        assertFalse(service.hasProfile("AT9G99999"));
        assertTrue(service.hasProfile("AT1G01010"));
    }

    @Test
    @DisplayName("Global maximum TPM spans the whole dataset")
    void shouldComputeGlobalMaximum() throws Exception {
        ExpressionDataService service = new ExpressionDataService();
        service.loadFromResource(LegacyAnnotationFixtures.byId("arabidopsis").expressionResource(), "illustrative regression fixture");

        // AT3G01050 in Root (24.63) is the highest value in the bundled panel
        assertEquals(24.63, service.globalMaxTpm(), 0.001);
    }

    private static Gene findGene(String id) {
        Optional<Gene> gene = result.genes().stream()
                .filter(g -> id.equals(g.getId()))
                .findFirst();
        return gene.orElse(null);
    }
}
