package io.gitee.omixcore.phytoscope;

import io.gitee.omixcore.phytoscope.service.GeneSequenceService;
import io.gitee.omixcore.phytoscope.service.GenomeDataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** End-to-end import and sequence validation using a public non-model plant assembly. */
@EnabledIfSystemProperty(named = "phytoscope.vignaReferenceDir", matches = ".+")
class NonModelVignaReferenceTest {
    private static final Path DATA = Path.of(System.getProperty("phytoscope.vignaReferenceDir",
            "src/test/resources/non-model/vigna-umbellata"));
    private static final String CHROMOSOME = "NC_142174.1";

    @Test void fullRefSeqGffImportsAndMatchesIndependentMinusStrandSequenceGold() throws Exception {
        var genome = new GenomeDataService();
        genome.loadFromFile(DATA.resolve("GCF_029865625.1_genomic.gff.gz"));
        assertEquals(41_713, genome.getGeneCount());
        assertEquals(232, genome.getMetadata().getSequenceCount());
        assertTrue(genome.getChromosomes().contains(CHROMOSOME));

        var gene = genome.findGene("gene-LOC124833352");
        assertNotNull(gene);
        assertEquals(CHROMOSOME, gene.getChromosome());
        assertEquals(16_647, gene.getStart());
        assertEquals(23_821, gene.getEnd());
        assertEquals('-', gene.getStrand());
        assertEquals(1, gene.getTranscripts().size());
        assertEquals(3, gene.getTranscripts().getFirst().getExonCount());
        assertEquals(1_983, gene.getTranscripts().getFirst().getCdsLength());

        var sequences = new GeneSequenceService();
        sequences.loadFromFile(DATA.resolve("NC_142174.1.fna"));
        var extracted = sequences.extract(gene);
        assertEquals(7_175, extracted.genomicSpanSequence().length());
        assertEquals(1, extracted.transcripts().size());
        var transcript = extracted.transcripts().getFirst();
        assertEquals("rna-XM_088100767.1", transcript.transcriptId());
        assertEquals(3, transcript.exonCount());
        Map<String, String> gold = readGold();
        assertEquals(gold.get("genomic_span"), extracted.genomicSpanSequence());
        assertEquals(gold.get("transcript"), transcript.transcriptSequence());
        assertEquals(gold.get("CDS"), transcript.cdsSequence());
        assertEquals(gold.get("protein"), transcript.proteinSequence());
        assertEquals(gold.get("five_prime_UTR_inferred"), transcript.fivePrimeUtrSequence());
        assertEquals(gold.get("three_prime_UTR_inferred"), transcript.threePrimeUtrSequence());
        assertEquals("inferred from exon/CDS boundaries", transcript.fivePrimeUtrSource());
        assertEquals("inferred from exon/CDS boundaries", transcript.threePrimeUtrSource());
        assertMultiLocusSequenceGold(genome, sequences);
    }

    @Test void fullRefSeqGtfImportsAndFindsTheSameLocus() throws Exception {
        var genome = new GenomeDataService();
        genome.loadFromFile(DATA.resolve("GCF_029865625.1_genomic.gtf.gz"));
        assertEquals(41_713, genome.getGeneCount());
        assertEquals(99, genome.getMetadata().getSequenceCount());
        var gene = genome.findGene("LOC124833352");
        assertNotNull(gene);
        assertEquals(CHROMOSOME, gene.getChromosome());
        assertEquals(16_647, gene.getStart());
        assertEquals(23_821, gene.getEnd());
        assertEquals('-', gene.getStrand());
        assertEquals(1, gene.getTranscripts().size());
        var model = gene.getTranscripts().getFirst();
        assertEquals(3, model.getExonCount());
        assertEquals(1_980, model.getCdsLength());
        assertEquals(3, model.getStopCodonSegments().stream().mapToInt(s -> s.length()).sum());
        assertEquals(List.of(List.of(16_697, 17_344), List.of(17_656, 18_209), List.of(23_014, 23_791)),
                model.getAllCdsSegments().stream().map(s -> List.of(s.getStart(), s.getEnd())).sorted(
                        java.util.Comparator.comparingInt(x -> x.get(0))).toList());
        var sequences = new GeneSequenceService();
        sequences.loadFromFile(DATA.resolve("NC_142174.1.fna"));
        var extracted = sequences.extract(gene);
        var transcript = extracted.transcripts().getFirst();
        Map<String, String> gold = readGold();
        assertEquals(gold.get("CDS_GTF_without_stop_codon"), transcript.cdsSequence());
        assertEquals(gold.get("protein"), transcript.proteinSequence());
        assertEquals(gold.get("five_prime_UTR_inferred"), transcript.fivePrimeUtrSequence());
        assertEquals(gold.get("three_prime_UTR_inferred"), transcript.threePrimeUtrSequence());
        assertEquals(47, transcript.threePrimeUtrSequence().length(), "stop_codon must not be mislabeled as inferred UTR");
    }

    private static Map<String, String> readGold() throws Exception {
        Map<String, String> result = new LinkedHashMap<>();
        try (var raw = NonModelVignaReferenceTest.class.getResourceAsStream(
                "/non-model/vigna-umbellata/gold-LOC124833352.fasta")) {
            assertNotNull(raw);
            try (var reader = new BufferedReader(new InputStreamReader(raw, StandardCharsets.UTF_8))) {
                String key = null;
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith(">")) {
                        key = line.substring(1);
                        result.put(key, "");
                    } else if (key != null) {
                        result.put(key, result.get(key) + line.trim());
                    }
                }
            }
        }
        return result;
    }

    private static void assertMultiLocusSequenceGold(GenomeDataService genome,
                                                      GeneSequenceService sequences) throws Exception {
        int checked = 0;
        try (var raw = NonModelVignaReferenceTest.class.getResourceAsStream(
                "/non-model/multi-locus-sequence-gold.tsv")) {
            assertNotNull(raw);
            try (var reader = new BufferedReader(new InputStreamReader(raw, StandardCharsets.UTF_8))) {
                reader.readLine();
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] row = line.split("\\t");
                    if (!row[1].equals("GCF_029865625.1_genomic.gff.gz") || !row[2].equals(CHROMOSOME)) continue;
                    var gene = genome.findGene(row[3]);
                    assertNotNull(gene, "frozen source-gold gene must be searchable: " + row[3]);
                    assertEquals(Integer.parseInt(row[5]), gene.getStart());
                    assertEquals(Integer.parseInt(row[6]), gene.getEnd());
                    assertEquals(row[7].charAt(0), gene.getStrand());
                    var products = sequences.extract(gene);
                    assertEquals(row[11], sha256(products.genomicSpanSequence()), row[3] + " span hash");
                    var transcript = products.transcripts().stream()
                            .filter(tx -> tx.transcriptId().equals(row[4])).findFirst().orElse(null);
                    assertNotNull(transcript, "frozen source-gold transcript must be exported: " + row[4]);
                    assertEquals(Integer.parseInt(row[8]), transcript.exonCount());
                    assertEquals(Integer.parseInt(row[10]), transcript.cdsSequence().length());
                    assertEquals(row[12], sha256(transcript.transcriptSequence()), row[4] + " transcript hash");
                    assertEquals(row[13], sha256(transcript.cdsSequence()), row[4] + " CDS hash");
                    checked++;
                }
            }
        }
        assertTrue(checked >= 5, "at least five coding transcripts are covered by frozen sequence gold");
    }

    private static String sha256(String text) throws Exception {
        return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                .digest(text.getBytes(StandardCharsets.US_ASCII)));
    }
}
