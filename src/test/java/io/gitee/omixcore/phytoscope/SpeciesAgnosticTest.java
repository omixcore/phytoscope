package io.gitee.omixcore.phytoscope;

import io.gitee.omixcore.phytoscope.model.Gene;
import io.gitee.omixcore.phytoscope.model.GenomeMetadata;
import io.gitee.omixcore.phytoscope.model.SequenceRegion;
import io.gitee.omixcore.phytoscope.model.Transcript;
import io.gitee.omixcore.phytoscope.parser.AnnotationFormat;
import io.gitee.omixcore.phytoscope.parser.Gff3Parser;
import io.gitee.omixcore.phytoscope.service.ExampleDataset;
import io.gitee.omixcore.phytoscope.service.ExpressionDataService;
import io.gitee.omixcore.phytoscope.service.GenomeDataService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the species-agnostic layer: metadata harvesting, dialect detection,
 * GTF parsing and cross-species loading.
 *
 * <p>These tests are the evidence that PhytoScope is a general framework rather
 * than an Arabidopsis-only viewer: every bundled example must load through the
 * same code path and report its own species, assembly and sequence regions.</p>
 */
class SpeciesAgnosticTest {

    // ------------------------------------------------------------------
    // Catalogue integrity
    // ------------------------------------------------------------------

    @Test
    @DisplayName("The bundled catalogue covers four distinct species")
    void shouldExposeFourSpecies() {
        assertEquals(4, ExampleDataset.CATALOGUE.size());
        long distinct = ExampleDataset.CATALOGUE.stream()
                .map(ExampleDataset::species)
                .distinct()
                .count();
        assertEquals(4, distinct, "Every catalogue entry must be a different species");
    }

    @Test
    @DisplayName("The catalogue exercises both annotation dialects")
    void shouldCoverBothFormats() {
        boolean gff3 = ExampleDataset.CATALOGUE.stream()
                .anyMatch(d -> d.format() == AnnotationFormat.GFF3);
        boolean gtf = ExampleDataset.CATALOGUE.stream()
                .anyMatch(d -> d.format() == AnnotationFormat.GTF);
        assertTrue(gff3, "At least one GFF3 example is expected");
        assertTrue(gtf, "At least one GTF example is expected");
    }

    @Test
    @DisplayName("Dataset lookup by id works and rejects unknown ids")
    void shouldLookUpById() {
        assertNotNull(ExampleDataset.byId("mint"));
        assertEquals("Mentha spicata", ExampleDataset.byId("mint").species());
        assertNull(ExampleDataset.byId("nonexistent"));
    }

    // ------------------------------------------------------------------
    // Cross-species loading
    // ------------------------------------------------------------------

    /**
     * Every bundled example must load and yield a non-empty, internally
     * consistent gene set. This single test is the core generality guarantee.
     */
    @ParameterizedTest(name = "loads {0}")
    @EnumSource(ExampleDatasetId.class)
    @DisplayName("Every bundled species loads through the same path")
    void shouldLoadEverySpecies(ExampleDatasetId id) throws Exception {
        ExampleDataset dataset = ExampleDataset.byId(id.value);
        assertNotNull(dataset, "catalogue entry must exist for " + id.value);

        GenomeDataService service = new GenomeDataService();
        service.loadExample(dataset);

        assertFalse(service.isEmpty(), dataset.species() + " should yield genes");
        assertTrue(service.getGeneCount() > 0);
        assertEquals(dataset.species(), service.getSpecies(),
                "species must come from the data, not from code");
        assertTrue(service.getMetadata().hasSequenceRegions(),
                "sequence regions must be available for the axis");
    }

    /** Enum wrapper so the parameterised test has stable, readable names. */
    enum ExampleDatasetId {
        ARABIDOPSIS("arabidopsis"),
        RICE("rice"),
        SOYBEAN("soybean"),
        MINT("mint");

        private final String value;

        ExampleDatasetId(String value) {
            this.value = value;
        }
    }

    @Test
    @DisplayName("Rice example: metadata and gene structure are recovered")
    void shouldLoadRiceWithMetadata() throws Exception {
        GenomeDataService service = new GenomeDataService();
        service.loadExample(ExampleDataset.byId("rice"));

        assertEquals("Oryza sativa Japonica Group", service.getSpecies());
        assertEquals("IRGSP-1.0 (Nipponbare)", service.getAssembly());
        assertEquals(List.of("Chr1", "Chr2"), service.getChromosomes());
        assertEquals("Chr1", service.getActiveSequence(),
                "the first declared sequence is shown first");

        GenomeMetadata metadata = service.getMetadata();
        SequenceRegion region = metadata.findRegion("Chr1");
        assertNotNull(region, "Chr1 must be declared in the header");
        assertEquals(1, region.start());
        assertEquals(43_270_923, region.end(),
                "the bundled rice excerpt is reported at the real Chr1 length, "
                        + "so the axis reads in true megabase units");

        Gene gene = service.findGene("Os01g0100100");
        assertNotNull(gene, "the first rice gene must be present");
        assertEquals('+', gene.getStrand());
        assertEquals(3449, gene.getStart());
        assertEquals(10297, gene.getEnd());
    }

    @Test
    @DisplayName("Rice example: chromosome 2 is populated and independently addressable")
    void shouldExposeRiceChromosomeTwo() throws Exception {
        GenomeDataService service = new GenomeDataService();
        service.loadExample(ExampleDataset.byId("rice"));

        assertTrue(service.setActiveSequence("Chr2"), "the selector must be able to reach Chr2");
        assertEquals("Chr2", service.getActiveSequence());

        List<Gene> onChr2 = service.getGenesOnActiveSequence();
        assertEquals(5, onChr2.size(), "the Chr2 excerpt carries five genes");
        assertEquals(List.of("Os02g0100050", "Os02g0100100", "Os02g0100150",
                        "Os02g0100200", "Os02g0100250"),
                onChr2.stream().map(Gene::getDisplayName).toList());

        // Real AP014958 coordinates survive the load, and the sequence is drawn
        // at its published size rather than the excerpt's own extent.
        Gene first = service.findGene("Os02g0100050");
        assertNotNull(first);
        assertEquals(627, first.getStart());
        assertEquals(800, first.getEnd());

        int[] span = service.getActiveSequenceSpan();
        assertNotNull(span);
        assertEquals(1, span[0]);
        assertEquals(35_937_250, span[1]);

        // A twelve-exon gene model on Chr2 proves the multi-exon path is not
        // specific to the first chromosome.
        Gene multiExon = service.findGene("Os02g0100100");
        assertNotNull(multiExon);
        assertEquals('-', multiExon.getStrand());
        assertEquals(12, multiExon.getTranscripts().get(0).getExonCount(),
                "Os02g0100100 is a twelve-exon gene on the minus strand");
    }

    @Test
    @DisplayName("Rice example: alternative splicing is preserved")
    void shouldRecoverRiceAlternativeSplicing() throws Exception {
        GenomeDataService service = new GenomeDataService();
        service.loadExample(ExampleDataset.byId("rice"));

        Gene gene = service.findGene("Os01g0100600");
        assertNotNull(gene);
        assertEquals(2, gene.getTranscripts().size(),
                "Os01g0100600 carries two transcripts");
    }

    // ------------------------------------------------------------------
    // GTF support
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Soybean GTF loads even though it has no ## directives")
    void shouldLoadGtfDataset() throws Exception {
        GenomeDataService service = new GenomeDataService();
        service.loadExample(ExampleDataset.byId("soybean"));

        assertEquals("Glycine max", service.getSpecies());
        assertEquals(AnnotationFormat.GTF, service.getFormat());
        assertTrue(service.getGeneCount() >= 12, "all twelve soybean genes should load");
        assertEquals(List.of("Glyma01", "Glyma02", "Glyma03"), service.getChromosomes());

        // Without a ##sequence-region directive the span is inferred from the data,
        // so the last gene must still be inside the declared region.
        SequenceRegion region = service.getMetadata().findRegion("Glyma01");
        assertNotNull(region, "the observed sequence must be registered as a region");
        assertTrue(region.end() >= 35200,
                "inferred region must cover the last observed coordinate");
    }

    @Test
    @DisplayName("Soybean GTF exposes later chromosomes through the same selector")
    void shouldSwitchGtfChromosomes() throws Exception {
        GenomeDataService service = new GenomeDataService();
        service.loadExample(ExampleDataset.byId("soybean"));

        // Glyma02 is a sparse, multi-isoform chromosome: four genes, one of them
        // carrying four alternative transcripts.
        assertTrue(service.setActiveSequence("Glyma02"));
        List<Gene> onChr2 = service.getGenesOnActiveSequence();
        assertEquals(4, onChr2.size());
        assertEquals(List.of("GmISU01.02G000100", "GmISU01.02G000200",
                        "GmISU01.02G000300", "GmISU01.02G000400"),
                onChr2.stream().map(Gene::getDisplayName).toList());

        // GTF has no sequence-region directive, so the published length supplied
        // by the catalogue is what widens the axis to the real chromosome size.
        int[] span = service.getActiveSequenceSpan();
        assertNotNull(span);
        assertEquals(1, span[0], "the declared span runs from base 1");
        assertEquals(48_577_594, span[1],
                "Glyma02 must be drawn at its published length");

        Gene spliced = service.findGene("GmISU01.02G000100");
        assertNotNull(spliced);
        assertEquals(5, spliced.getTranscripts().size(),
                "the first Glyma02 gene carries five alternative transcripts");

        // Glyma03 sits far from position 1, so its genes keep large real
        // coordinates — a good check that the GTF path preserves them.
        assertTrue(service.setActiveSequence("Glyma03"));
        assertEquals(4, service.getGenesOnActiveSequence().size());
        Gene deep = service.findGene("GmISU01.03G000300");
        assertNotNull(deep);
        assertEquals('+', deep.getStrand());
        assertEquals(158_741, deep.getStart());
        assertEquals(163_922, deep.getEnd());

        assertFalse(service.setActiveSequence("Glyma99"), "unknown sequences are refused");
        assertEquals("Glyma03", service.getActiveSequence());
    }

    @Test
    @DisplayName("GTF hierarchy is linked by gene_id / transcript_id")
    void shouldAssembleGtfHierarchy() throws Exception {
        GenomeDataService service = new GenomeDataService();
        service.loadExample(ExampleDataset.byId("soybean"));

        Gene gene = service.findGene("Glyma.01G000100");
        assertNotNull(gene, "gene_id must become the gene identifier");
        assertEquals(1, gene.getTranscripts().size());

        Transcript transcript = gene.getTranscripts().get(0);
        assertEquals("Glyma.01G000100.1", transcript.getId());
        assertEquals(4, transcript.getExonCount(), "four exons in this gene model");
        assertTrue(transcript.getCdsLength() > 0, "CDS must be recovered from GTF");
    }

    @Test
    @DisplayName("GTF minus-strand genes keep their transcription order")
    void shouldHandleGtfMinusStrand() throws Exception {
        GenomeDataService service = new GenomeDataService();
        service.loadExample(ExampleDataset.byId("soybean"));

        Gene gene = service.findGene("Glyma.01G000300");
        assertNotNull(gene);
        assertEquals('-', gene.getStrand());
        assertEquals(2, gene.getTranscripts().size(), "two splice isoforms expected");
    }

    // ------------------------------------------------------------------
    // Chromosome selection
    // ------------------------------------------------------------------

    @Test
    @DisplayName("A multi-sequence annotation exposes every chromosome and starts on the first")
    void shouldExposeEveryChromosome() throws Exception {
        String content = "##gff-version 3\n"
                + "##species=Testus multipartitus\n"
                + "##sequence-region Chr1 1 500000\n"
                + "##sequence-region Chr2 1 300000\n"
                + geneLine("Chr1", "g1", 1_000, 2_000)
                + geneLine("Chr2", "g2", 50_000, 52_000)
                + geneLine("Chr2", "g3", 100_000, 103_000);

        GenomeDataService service = new GenomeDataService();
        service.loadFromContent(content);

        assertEquals(List.of("Chr1", "Chr2"), service.getChromosomes());
        assertEquals(List.of("Chr1", "Chr2"), service.getPopulatedChromosomes());
        assertEquals("Chr1", service.getActiveSequence(), "the first sequence is shown first");
        assertEquals(1, service.getGenesOnActiveSequence().size());
    }

    @Test
    @DisplayName("Switching chromosome swaps the visible gene set and the drawn span")
    void shouldSwitchChromosome() throws Exception {
        String content = "##gff-version 3\n"
                + "##sequence-region Chr1 1 500000\n"
                + "##sequence-region Chr2 1 300000\n"
                + geneLine("Chr1", "g1", 1_000, 2_000)
                + geneLine("Chr2", "g2", 50_000, 52_000)
                + geneLine("Chr2", "g3", 100_000, 103_000);

        GenomeDataService service = new GenomeDataService();
        service.loadFromContent(content);

        assertTrue(service.setActiveSequence("Chr2"), "moving to a declared sequence must work");
        assertEquals("Chr2", service.getActiveSequence());
        assertEquals(2, service.getGenesOnActiveSequence().size());
        assertEquals(List.of("g2", "g3"), service.getGenesOnActiveSequence().stream()
                .map(Gene::getDisplayName).toList());

        int[] span = service.getActiveSequenceSpan();
        assertNotNull(span);
        assertEquals(1, span[0], "the whole chromosome is drawn, not just the genes");
        assertEquals(300_000, span[1]);

        // Re-selecting the sequence already shown is a no-op, and unknown
        // sequences are refused rather than blanking the view.
        assertFalse(service.setActiveSequence("Chr2"));
        assertFalse(service.setActiveSequence("Chr9"));
        assertEquals("Chr2", service.getActiveSequence());
    }

    @Test
    @DisplayName("Searching finds a locus on another chromosome and moves the view there")
    void shouldSearchAcrossChromosomes() throws Exception {
        String content = "##gff-version 3\n"
                + "##sequence-region Chr1 1 500000\n"
                + "##sequence-region Chr2 1 300000\n"
                + geneLine("Chr1", "g1", 1_000, 2_000)
                + geneLine("Chr2", "g2", 50_000, 52_000);

        GenomeDataService service = new GenomeDataService();
        service.loadFromContent(content);

        List<Gene> hits = service.search("g2");
        assertEquals(1, hits.size());
        assertEquals("Chr2", hits.get(0).getChromosome());
        assertTrue(service.setActiveSequence(hits.get(0).getChromosome()));
        assertEquals(List.of("g2"), service.getGenesOnActiveSequence().stream()
                .map(Gene::getDisplayName).toList());
    }

    @Test
    @DisplayName("Blank search returns the genes of the sequence on screen")
    void shouldReturnActiveSequenceGenesForBlankSearch() throws Exception {
        String content = "##gff-version 3\n"
                + "##sequence-region Chr1 1 500000\n"
                + "##sequence-region Chr2 1 300000\n"
                + geneLine("Chr1", "g1", 1_000, 2_000)
                + geneLine("Chr2", "g2", 50_000, 52_000);

        GenomeDataService service = new GenomeDataService();
        service.loadFromContent(content);
        service.setActiveSequence("Chr2");

        assertEquals(1, service.search("").size());
        assertEquals("Chr2", service.search("").get(0).getChromosome());
    }

    @Test
    @DisplayName("The bundled examples report their real chromosome size")
    void shouldApplyPublishedChromosomeLengths() throws Exception {
        GenomeDataService service = new GenomeDataService();
        service.loadExample(ExampleDataset.byId("rice"));

        int[] span = service.getActiveSequenceSpan();
        assertNotNull(span);
        assertEquals(1, span[0]);
        assertEquals(43_270_923, span[1],
                "rice chromosome 1 must be drawn at its published length");

        // The excerpt's own genes keep their real coordinates inside that span.
        Gene gene = service.findGene("Os01g0100100");
        assertNotNull(gene);
        assertEquals(3_449, gene.getStart());
    }

    @Test
    @DisplayName("A published length re-anchors the region to base 1")
    void shouldApplyPublishedLengthsAnchoredAtOne() {
        GenomeMetadata metadata = GenomeMetadata.fromAnnotation(
                "Species", "assembly", "", "test",
                List.of(new SequenceRegion("Chr1", 1, 20_000)));

        GenomeMetadata unchanged = metadata.withSequenceLengths(Map.of("Chr1", 5_000));
        assertEquals(20_000, unchanged.findRegion("Chr1").end(),
                "a shorter published length must not shrink the observed span");

        GenomeMetadata grown = metadata.withSequenceLengths(Map.of("Chr1", 3_000_000));
        assertEquals(3_000_000, grown.findRegion("Chr1").end());
        assertEquals(1, grown.findRegion("Chr1").start());

        // Unknown sequence ids are left alone.
        assertEquals(20_000, metadata.withSequenceLengths(Map.of("ChrX", 9_999_999))
                .findRegion("Chr1").end());
    }

    @Test
    @DisplayName("A header-less dialect is re-anchored so a real chromosome axis is possible")
    void shouldAnchorInferredRegionsAtOne() {
        // GTF carries no ##sequence-region, so the region is inferred from the
        // data and would otherwise start at the first gene. Once the published
        // chromosome length is supplied the axis must read from base 1, or a
        // viewer could never draw the true chromosome.
        GenomeMetadata inferred = GenomeMetadata.fromAnnotation(
                "Glycine max", "Wm82.a2.v1", "", "test",
                List.of(new SequenceRegion("Glyma01", 4200, 35_200)));

        assertEquals(4200, inferred.findRegion("Glyma01").start());

        GenomeMetadata real = inferred.withSequenceLengths(Map.of("Glyma01", 55_915_577));
        assertEquals(1, real.findRegion("Glyma01").start(),
                "a published chromosome length describes the whole sequence");
        assertEquals(55_915_577, real.findRegion("Glyma01").end());
    }

    @Test
    @DisplayName("Arabidopsis ships all five chromosomes at their published sizes")
    void shouldExposeFiveArabidopsisChromosomes() throws Exception {
        GenomeDataService service = new GenomeDataService();
        service.loadExample(ExampleDataset.byId("arabidopsis"));

        assertEquals(List.of("Chr1", "Chr2", "Chr3", "Chr4", "Chr5"), service.getChromosomes(),
                "the reference model must not be limited to a couple of chromosomes");
        assertEquals(List.of("Chr1", "Chr2", "Chr3", "Chr4", "Chr5"),
                service.getPopulatedChromosomes(), "every chromosome carries at least one gene");

        Map<String, Integer> expected = Map.of(
                "Chr1", 30_427_671,
                "Chr2", 19_698_289,
                "Chr3", 23_459_830,
                "Chr4", 18_585_056,
                "Chr5", 26_975_502);
        for (Map.Entry<String, Integer> entry : expected.entrySet()) {
            // Chr1 is already on screen, so switching to it is legitimately a
            // no-op; every later chromosome must actually move the view.
            service.setActiveSequence(entry.getKey());
            assertEquals(entry.getKey(), service.getActiveSequence(),
                    "the view must be on " + entry.getKey());
            int[] span = service.getActiveSequenceSpan();
            assertNotNull(span, "no span for " + entry.getKey());
            assertEquals(1, span[0]);
            assertEquals(entry.getValue().intValue(), span[1],
                    entry.getKey() + " must be drawn at its published length");
        }
        assertFalse(service.setActiveSequence("Chr9"), "unknown sequences are refused");
    }

    @Test
    @DisplayName("Genes keep their real coordinates on every chromosome, not just Chr1")
    void shouldKeepRealCoordinatesOnLateChromosomes() throws Exception {
        GenomeDataService service = new GenomeDataService();
        service.loadExample(ExampleDataset.byId("arabidopsis"));

        // AT3G01010 sits at the top of chromosome 3 in the real annotation.
        Gene chr3 = service.findGene("AT3G01010");
        assertNotNull(chr3);
        assertEquals("Chr3", chr3.getChromosome());
        assertEquals(4342, chr3.getStart());
        assertEquals(4818, chr3.getEnd());

        // AT4G00020, the BRCA2 homolog, is further down chromosome 4.
        Gene chr4 = service.findGene("AT4G00020");
        assertNotNull(chr4);
        assertEquals("Chr4", chr4.getChromosome());
        assertEquals(2895, chr4.getStart());
        assertEquals(10_504, chr4.getEnd());

        // AT5G01010 is the first gene at the top of chromosome 5, minus strand.
        Gene chr5 = service.findGene("AT5G01010");
        assertNotNull(chr5);
        assertEquals("Chr5", chr5.getChromosome());
        assertEquals('-', chr5.getStrand());
        assertEquals(995, chr5.getStart());
        assertEquals(5156, chr5.getEnd());
    }

    @Test
    @DisplayName("Sequence naming is taken from the file, so non-Chr1 schemes work")
    void shouldFollowWhicheverSequenceNamesTheFileDeclares() throws Exception {
        // A legume-style scaffold and a plastome, neither of which is called
        // "Chr1": the selector must offer exactly what the annotation declares.
        String content = "##gff-version 3\n"
                + "##species=Customus plantus\n"
                + "##sequence-region Glyma01 1 55915577\n"
                + "##sequence-region Scaffold_7 1 250000\n"
                + geneLine("Glyma01", "g1", 1_000, 2_000)
                + geneLine("Scaffold_7", "g2", 4_000, 5_500);

        GenomeDataService service = new GenomeDataService();
        service.loadFromContent(content);

        assertEquals(List.of("Glyma01", "Scaffold_7"), service.getChromosomes());
        assertEquals("Glyma01", service.getActiveSequence());

        assertTrue(service.setActiveSequence("Scaffold_7"));
        assertEquals(List.of("g2"), service.getGenesOnActiveSequence().stream()
                .map(Gene::getDisplayName).toList());
    }

    @Test
    @DisplayName("A sequence whose excerpt sits mid-chromosome reports its own data span")
    void shouldReportSpanFromTheDataForLateChromosomes() throws Exception {
        // Chr4 is the awkward case in the bundled Arabidopsis file: the excerpt
        // begins well past position 1, so a viewer that blindly opened the whole
        // chromosome would render every gene as a single pixel.
        GenomeDataService service = new GenomeDataService();
        service.loadExample(ExampleDataset.byId("arabidopsis"));
        service.setActiveSequence("Chr4");

        List<Gene> genes = service.getGenesOnActiveSequence();
        assertFalse(genes.isEmpty());

        int first = genes.get(0).getStart();
        int last = genes.get(genes.size() - 1).getEnd();
        int[] span = service.getActiveSequenceSpan();

        assertNotNull(span);
        assertEquals(1, span[0], "the declared span still runs from base 1");
        assertEquals(18_585_056, span[1]);

        // The genes are far smaller than the chromosome, which is exactly why
        // the view opens on the data range instead of the full sequence.
        assertTrue(span[1] - span[0] > 100 * (last - first),
                "the chromosome is orders of magnitude wider than the excerpt");
    }

    @Test
    @DisplayName("Mint ships a plastome plus three mitochondrial contigs")
    void shouldExposeMintOrganellePanel() throws Exception {
        GenomeDataService service = new GenomeDataService();
        service.loadExample(ExampleDataset.byId("mint"));

        // Organelle sequences are not called "Chr1..N", and the mitochondrial
        // genome is assembled into several contigs: both facts are simply read
        // off the annotation, with no per-species code.
        assertEquals(List.of("Pt", "Mt1", "Mt2", "Mt3"), service.getChromosomes());
        assertEquals(List.of("Pt", "Mt1", "Mt2", "Mt3"), service.getPopulatedChromosomes(),
                "every organelle sequence carries at least one gene");
        assertEquals("Pt", service.getActiveSequence(), "the plastome is shown first");

        Map<String, Integer> expected = Map.of(
                "Pt", 152_132,
                "Mt1", 158_164,
                "Mt2", 87_431,
                "Mt3", 40_953);
        for (Map.Entry<String, Integer> entry : expected.entrySet()) {
            service.setActiveSequence(entry.getKey());
            assertEquals(entry.getKey(), service.getActiveSequence());
            int[] span = service.getActiveSequenceSpan();
            assertNotNull(span, "no span for " + entry.getKey());
            assertEquals(1, span[0]);
            assertEquals(entry.getValue().intValue(), span[1],
                    entry.getKey() + " must be drawn at its published length");
        }
    }

    @Test
    @DisplayName("Mitochondrial genes keep their classic shorthand and structure")
    void shouldReadMitochondrialGeneModels() throws Exception {
        GenomeDataService service = new GenomeDataService();
        service.loadExample(ExampleDataset.byId("mint"));

        // Mt1 carries the four-exon nad7, which is the most structurally
        // interesting example in the organelle panel.
        service.setActiveSequence("Mt1");
        assertEquals(5, service.getGenesOnActiveSequence().size());

        Gene nad7 = service.findGene("nad7");
        assertNotNull(nad7, "mitochondrial genes keep their published locus names");
        assertEquals("Mt1", nad7.getChromosome());
        assertEquals('+', nad7.getStrand());
        assertEquals(53_618, nad7.getStart());
        assertEquals(58_340, nad7.getEnd());

        Transcript transcript = nad7.getTranscripts().get(0);
        assertEquals(4, transcript.getExonCount(), "nad7 is a four-exon gene in cis");
        assertEquals(3, transcript.getIntronCount());

        // Mt3 holds a minus-strand gene, so strand handling is exercised on the
        // organelle contigs too.
        service.setActiveSequence("Mt3");
        Gene cox3 = service.findGene("cox3");
        assertNotNull(cox3);
        assertEquals('-', cox3.getStrand());
        assertEquals(22_793, cox3.getStart());
        assertEquals(23_590, cox3.getEnd());
    }

    @Test
    @DisplayName("Trans-spliced organelle loci are excluded rather than drawn wrongly")
    void shouldExcludeTransSplicedLoci() throws Exception {
        // A linear track cannot faithfully render an exon whose blocks are
        // scattered around a repeat-rich mitochondrial genome, so the bundled
        // panel simply leaves those records out — the annotation stays honest
        // about what it contains.
        GenomeDataService service = new GenomeDataService();
        service.loadExample(ExampleDataset.byId("mint"));

        for (Gene gene : service.getGenes()) {
            int span = gene.getEnd() - gene.getStart();
            assertTrue(span < 80_000,
                    gene.getId() + " spans " + span
                            + " bp, which suggests a trans-spliced locus leaked in");
        }
    }

    // ------------------------------------------------------------------
    // The sequence selector must be usable for every species
    // ------------------------------------------------------------------

    /**
     * A sequence selector with one entry is not a selector. This is the
     * regression guard for the oversight where only the reference model got a
     * full complement of sequences while the other three planets exposed a
     * single chromosome each.
     */
    @ParameterizedTest(name = "selector is usable for {0}")
    @EnumSource(ExampleDatasetId.class)
    @DisplayName("Every bundled species offers more than one selectable sequence")
    void shouldOfferMultipleSequencesForEverySpecies(ExampleDatasetId id) throws Exception {
        ExampleDataset dataset = ExampleDataset.byId(id.value);

        GenomeDataService service = new GenomeDataService();
        service.loadExample(dataset);

        List<String> sequences = service.getChromosomes();
        assertTrue(sequences.size() >= 2,
                dataset.species() + " exposes only " + sequences
                        + "; the sequence selector would have nothing to switch to");

        // Every offered sequence must carry genes and carry a usable span, so
        // switching to it never yields a blank view.
        for (String sequence : sequences) {
            service.setActiveSequence(sequence);
            assertEquals(sequence, service.getActiveSequence());
            assertFalse(service.getGenesOnActiveSequence().isEmpty(),
                    sequence + " of " + dataset.species() + " has no genes");

            int[] span = service.getActiveSequenceSpan();
            assertNotNull(span, "no span for " + sequence);
            assertEquals(1, span[0]);
            assertTrue(span[1] > 1, "degenerate span for " + sequence);
        }
    }

    /**
     * The generality claim in its strongest form: feed the engine a genome it
     * has never seen, whose sequences are named nothing like the bundled ones
     * and which mixes three genome compartments, and every sequence must still
     * appear in the selector and be independently addressable.
     *
     * <p>This is the guard against the framework quietly ossifying into "a
     * handful of known chromosomes": nothing here is declared in code, so the
     * only way the five sequences can be found is by reading them from the
     * annotation.</p>
     */
    @Test
    @DisplayName("An arbitrary genome defines its own sequences and compartments")
    void shouldAdaptToAnUnfamiliarGenome() throws Exception {
        String content = "##gff-version 3\n"
                + "##species=Quercus robur\n"
                + "##assembly=Oak v1 (synthetic probe)\n"
                // nuclear scaffold, unplaced nuclear contig, plastid,
                // mitochondrion and a linkage group: five naming schemes,
                // three genome compartments, none known to the application.
                + "##sequence-region scaffold_0042 1 2450000\n"
                + "##sequence-region contig00099 1 87000\n"
                + "##sequence-region chloroplast 1 161000\n"
                + "##sequence-region mitochondrion 1 640000\n"
                + "##sequence-region LG7 1 91000000\n"
                + geneLine("scaffold_0042", "QRY_A", 12_000, 15_500)
                + geneLine("contig00099", "QRY_B", 4_400, 9_800)
                + geneLine("chloroplast", "QRY_rbcL", 1, 1_420)
                + geneLine("mitochondrion", "QRY_nad9", 30_500, 33_200)
                + geneLine("LG7", "QRY_Z", 71_000_000, 71_012_000);

        GenomeDataService service = new GenomeDataService();
        service.loadFromContent(content);

        assertEquals("Quercus robur", service.getSpecies(),
                "the species must be read from the annotation, not assumed");
        assertEquals(
                List.of("scaffold_0042", "contig00099", "chloroplast",
                        "mitochondrion", "LG7"),
                service.getChromosomes(),
                "the selector must offer exactly the declared sequences");
        assertEquals(List.of("scaffold_0042", "contig00099", "chloroplast",
                        "mitochondrion", "LG7"),
                service.getPopulatedChromosomes());

        // Every compartment is independently addressable and keeps its own span.
        Map<String, Integer> spans = Map.of(
                "scaffold_0042", 2_450_000,
                "contig00099", 87_000,
                "chloroplast", 161_000,
                "mitochondrion", 640_000,
                "LG7", 91_000_000);
        for (Map.Entry<String, Integer> entry : spans.entrySet()) {
            assertTrue(service.setActiveSequence(entry.getKey()),
                    "must be able to switch to " + entry.getKey());
            assertEquals(entry.getKey(), service.getActiveSequence());
            assertEquals(1, service.getGenesOnActiveSequence().size(),
                    entry.getKey() + " should carry exactly one probe gene");

            int[] span = service.getActiveSequenceSpan();
            assertNotNull(span, "no span for " + entry.getKey());
            assertEquals(1, span[0]);
            assertEquals(entry.getValue().intValue(), span[1],
                    entry.getKey() + " must be reported at its declared size");
        }

        // A gene deep inside a large linkage group keeps its real coordinate,
        // so nothing is being normalised or re-anchored per sequence type.
        service.setActiveSequence("LG7");
        Gene deep = service.findGene("QRY_Z");
        assertNotNull(deep);
        assertEquals("LG7", deep.getChromosome());
        assertEquals(71_000_000, deep.getStart());
        assertEquals(71_012_000, deep.getEnd());
    }

    /**
     * The same genome written as a header-less GTF. Here there is no
     * {@code ##sequence-region} to read, so the sequences can only be found by
     * observing the data — the dialect must not change whether the selector
     * works.
     */
    @Test
    @DisplayName("A header-less GTF still discovers every sequence by observation")
    void shouldObserveSequencesInHeaderlessGtf() throws Exception {
        String content =
                geneLineGtf("scaffold_0042", "QRY_A", 12_000, 15_500)
                        + geneLineGtf("contig00099", "QRY_B", 4_400, 9_800)
                        + geneLineGtf("chloroplast", "QRY_rbcL", 1, 1_420)
                        + geneLineGtf("mitochondrion", "QRY_nad9", 30_500, 33_200)
                        + geneLineGtf("LG7", "QRY_Z", 71_000_000, 71_012_000);

        GenomeDataService service = new GenomeDataService();
        service.loadFromContent(content);

        assertEquals(AnnotationFormat.GTF, service.getFormat(),
                "the dialect is detected from the attribute syntax alone");
        assertEquals(
                List.of("scaffold_0042", "contig00099", "chloroplast",
                        "mitochondrion", "LG7"),
                service.getChromosomes(),
                "sequences must be discovered in first-seen order without a header");

        // Without a declared region the span falls back to the observed data,
        // which is exactly the case a published length is meant to correct.
        service.setActiveSequence("contig00099");
        int[] span = service.getActiveSequenceSpan();
        assertNotNull(span);
        assertEquals(4_400, span[0]);
        assertEquals(9_800, span[1]);

        GenomeMetadata widened = service.getMetadata()
                .withSequenceLengths(Map.of("contig00099", 87_000));
        assertEquals(1, widened.findRegion("contig00099").start(),
                "supplying the real size re-anchors the sequence to base 1");
        assertEquals(87_000, widened.findRegion("contig00099").end());
    }

    private static String geneLineGtf(String sequence, String id, int start, int end) {
        return sequence + "\tsrc\tgene\t" + start + "\t" + end + "\t.\t+\t.\t"
                + "gene_id \"" + id + "\"; gene_name \"" + id + "\";\n";
    }

    private static String geneLine(String chromosome, String id, int start, int end) {
        return chromosome + "\tsrc\tgene\t" + start + "\t" + end + "\t.\t+\t.\t"
                + "ID=" + id + ";Name=" + id + "\n";
    }

    // ------------------------------------------------------------------
    // Dialect detection
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Format is detected from the attribute syntax")
    void shouldDetectDialectFromContent() throws Exception {
        String gff3 = "##gff-version 3\nChr1\tsrc\tgene\t1\t100\t.\t+\t.\tID=g1;Name=g1\n";
        String gtf = "Chr1\tsrc\tgene\t1\t100\t.\t+\t.\tgene_id \"g1\"; gene_name \"g1\";\n";

        assertEquals(AnnotationFormat.GFF3, parseFormatOf(gff3));
        assertEquals(AnnotationFormat.GTF, parseFormatOf(gtf));
    }

    @Test
    @DisplayName("A GFF3 file with a FASTA trailer still parses cleanly")
    void shouldIgnoreFastaTrailer() throws Exception {
        String content = "##gff-version 3\n"
                + "Chr1\tsrc\tgene\t10\t200\t.\t+\t.\tID=g1;Name=g1\n"
                + "Chr1\tsrc\tmRNA\t10\t200\t.\t+\t.\tID=g1.1;Parent=g1\n"
                + "Chr1\tsrc\texon\t10\t200\t.\t+\t.\tParent=g1.1\n"
                + "##FASTA\n"
                + ">Chr1\n"
                + "ACGTACGTACGT\n";
        Gff3Parser.ParseResult result = new Gff3Parser().parse(
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
        assertEquals(1, result.genes().size());
        assertEquals(3, result.featureCount(), "FASTA lines must not be counted");
    }

    private static AnnotationFormat parseFormatOf(String content) throws Exception {
        return new Gff3Parser().parse(
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))).format();
    }

    // ------------------------------------------------------------------
    // Metaphylogenetic spread of the expression panels
    // ------------------------------------------------------------------

    @ParameterizedTest(name = "expression panel for {0}")
    @EnumSource(ExampleDatasetId.class)
    @DisplayName("Every species ships a matching expression panel")
    void shouldLoadExpressionForEverySpecies(ExampleDatasetId id) throws Exception {
        ExampleDataset dataset = ExampleDataset.byId(id.value);
        ExpressionDataService expression = new ExpressionDataService();
        expression.loadExample(dataset);

        assertFalse(expression.isEmpty(),
                dataset.species() + " should have a tissue panel");
        assertFalse(expression.getTissues().isEmpty());

        GenomeDataService genomes = new GenomeDataService();
        genomes.loadExample(dataset);

        // At least one gene of the dataset must have a profile, so the heat-map
        // has something to draw and the panels are genuinely aligned.
        long matched = genomes.getGenes().stream()
                .filter(g -> expression.hasProfile(g.getId()))
                .count();
        assertTrue(matched > 0,
                "no expression row matched any gene of " + dataset.species());
    }

    @Test
    @DisplayName("Mint plastid panel is leaf-biased, as expected for photosynthetic genes")
    void shouldShowMintLeafBias() throws Exception {
        ExpressionDataService expression = new ExpressionDataService();
        expression.loadExample(ExampleDataset.byId("mint"));

        assertEquals("Leaf", expression.getProfile("rbcL").topTissue().tissue());
        assertEquals("Leaf", expression.getProfile("psbA").topTissue().tissue());
    }

    @Test
    @DisplayName("Unit is read from the expression file header")
    void shouldReadUnitFromHeader() throws Exception {
        ExpressionDataService expression = new ExpressionDataService();
        expression.loadExample(ExampleDataset.byId("rice"));
        assertEquals("TPM", expression.getValueUnit());
    }
}
