package io.gitee.omixcore.phytoscope;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gitee.omixcore.phytoscope.model.Gene;
import io.gitee.omixcore.phytoscope.project.ProjectStore;
import io.gitee.omixcore.phytoscope.research.DomainLinkService;
import io.gitee.omixcore.phytoscope.service.ExpressionDataService;
import io.gitee.omixcore.phytoscope.service.GeneSequenceService;
import io.gitee.omixcore.phytoscope.service.GenomeDataService;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real reference coordinates are independently derived by the Python case pipeline. */
class DomainCaseValidationTest {
    private static final Path CASE = Path.of("examples/arabidopsis-isoform-validation");
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test void frozenIsoformsAndDomainBlocksMatchTheApplicationAndRestore() throws Exception {
        var genome = new GenomeDataService();
        genome.loadFromFile(CASE.resolve("annotation.gff3"));
        var sequence = new GeneSequenceService();
        sequence.loadBundledForDataset("arabidopsis");
        var mapping = new DomainLinkService();
        JsonNode candidates = JSON.readTree(CASE.resolve("candidates.json").toFile());
        assertEquals(16, candidates.size());
        Set<String> geneIds = new HashSet<>();
        List<Map<String, Object>> checked = new ArrayList<>();
        for (JsonNode c : candidates) {
            String geneId = c.get("gene").asText();
            geneIds.add(geneId);
            Gene gene = genome.findGene(geneId);
            assertNotNull(gene, geneId);
            var products = sequence.extract(gene).transcripts();
            for (String kind : List.of("canonical", "short")) {
                String id = c.get(kind + "_transcript").asText();
                var actual = products.stream().filter(t -> id.equals(t.transcriptId())).findFirst().orElseThrow();
                assertEquals(c.get(kind + "_sequence").asText(), actual.proteinSequence(), id);
            }
            String transcriptId = c.get("canonical_transcript").asText();
            var domain = new DomainLinkService.Domain(transcriptId, c.get("domain").asText(),
                    c.get("domain_start_aa").asInt(), c.get("domain_end_aa").asInt(), "UniProt snapshot");
            var links = mapping.link(domain, gene);
            assertEquals(1, links.size(), "Exact transcript ID must not leak to other isoforms");
            var expected = new ArrayList<DomainLinkService.GenomicBlock>();
            for (JsonNode b : c.get("domain_genomic_blocks")) {
                expected.add(new DomainLinkService.GenomicBlock(c.get("chromosome").asText(),
                        b.get(0).asInt(), b.get(1).asInt(), c.get("strand").asText().charAt(0)));
            }
            assertEquals(expected, links.getFirst().blocks(), c.get("candidate_id").asText());
            assertTrue(mapping.link(new DomainLinkService.Domain(geneId, "unresolved gene-level ID", 1, 2, "test"), gene).isEmpty());
            checked.add(Map.of("candidate", c.get("candidate_id").asText(), "transcript", transcriptId,
                    "blocks", links.getFirst().blocks(), "agreement", true));
        }
        assertEquals(13, geneIds.size());

        Gene cipk15 = genome.findGene("AT5G01810");
        var naf = mapping.link(new DomainLinkService.Domain("AT5G01810.1", "NAF", 299, 323, "UniProt P92937"), cipk15);
        assertEquals(List.of(new DomainLinkService.GenomicBlock("Chr5", 311354, 311428, '+')), naf.getFirst().blocks());
        var peptide = mapping.link(new DomainLinkService.Domain("AT5G01810.3", "Junction peptide", 276, 287, "Reyes Table S2 row 24784"), cipk15);
        assertEquals(List.of(new DomainLinkService.GenomicBlock("Chr5", 311285, 311308, '+'),
                new DomainLinkService.GenomicBlock("Chr5", 311444, 311455, '+')), peptide.getFirst().blocks());

        Path out = Path.of("target/research-validation");
        Files.createDirectories(out);
        JSON.writerWithDefaultPrettyPrinter().writeValue(out.resolve("domain-coordinate-check.json").toFile(), checked);
        String input = "AT5G01810.1\tNAF\t299\t323\nAT5G01810.3\tJunction peptide (single-study evidence)\t276\t287\n";
        String result = "CIPK15 candidate: independent validation threshold NOT met.\n" + naf + "\n" + peptide;
        var state = new ProjectStore.ResearchState(2000, 200, "BED", Map.of(), "", 30,
                "", "", 1, 1, result, "", "", "", input);
        var view = new ProjectStore.ViewState("Chr5", cipk15.getId(), cipk15.getStart(), cipk15.getEnd(),
                List.of(cipk15.getId()), "TRANSCRIPTION_START", false, "LINEAR", "AT5G01810");
        var store = new ProjectStore();
        Path project = out.resolve("cipk15-candidate.phytoscope");
        store.save(project, genome, new ExpressionDataService(), view, state);
        try (var restored = store.load(project)) {
            assertEquals(state, restored.manifest().research());
            assertEquals(view, restored.manifest().view());
        }
        Files.writeString(out.resolve("cipk15-analysis.tsv"),
                io.gitee.omixcore.phytoscope.ui.ResearchAnalysisPane.formatTsv(result));
    }
}
