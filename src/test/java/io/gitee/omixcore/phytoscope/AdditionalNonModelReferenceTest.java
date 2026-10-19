package io.gitee.omixcore.phytoscope;

import io.gitee.omixcore.phytoscope.model.Gene;
import io.gitee.omixcore.phytoscope.service.GeneSequenceService;
import io.gitee.omixcore.phytoscope.service.GenomeDataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.*;

/** Full-source GFF3 checks for two unrelated medicinal/ornamental non-model plants. */
@EnabledIfSystemProperty(named = "phytoscope.additionalReferenceDir", matches = ".+")
class AdditionalNonModelReferenceTest {
    private static final Path DATA = Path.of(System.getProperty("phytoscope.additionalReferenceDir"));
    private static final Set<String> GENE_TYPES = Set.of("gene", "protein_coding_gene", "pseudogene", "ncrna_gene",
            "lncrna_gene", "mirna_gene", "snrna_gene", "snorna_gene", "rrna_gene", "transposable_element_gene");

    private record SourceFeature(String sequence, String type, int start, int end, char strand,
                                 Map<String, String> attributes) { }

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
        SourceFeature[] sourceGene = new SourceFeature[1];
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
                        Map<String, String> attributes = parseAttributes(fields[8]);
                        for (String attribute : fields[8].split(";")) {
                            if (attribute.startsWith("ID=")) {
                                String sourceId = attribute.substring(3);
                                sourceGeneIds.add(sourceId);
                                if (sourceId.equals("gene-" + targetId)) {
                                    sourceGene[0] = new SourceFeature(fields[0], fields[2], Integer.parseInt(fields[3]),
                                            Integer.parseInt(fields[4]), fields[6].charAt(0), attributes);
                                }
                            }
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
        assertIndependentSequenceProducts(source, targetSequence, gene, sourceGene[0]);
    }

    /** Reconstruct sequence products from raw source GFF3 rows and a separately fetched RefSeq scaffold. */
    private static void assertIndependentSequenceProducts(Path annotation, String sequenceId, Gene gene,
                                                          SourceFeature sourceGene) throws Exception {
        assertNotNull(sourceGene, "source gene feature not found");
        assertEquals(sequenceId, sourceGene.sequence());

        String genomicReference;
        try (var reader = java.nio.file.Files.newBufferedReader(DATA.resolve(sequenceId + ".fna"), StandardCharsets.US_ASCII)) {
            StringBuilder sequence = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) if (!line.startsWith(">")) sequence.append(line.trim());
            genomicReference = sequence.toString().toUpperCase(java.util.Locale.ROOT);
        }
        String expectedSpan = orient(genomicReference.substring(sourceGene.start() - 1, sourceGene.end()), sourceGene.strand());

        GeneSequenceService sequenceService = new GeneSequenceService();
        sequenceService.loadFromFile(DATA.resolve(sequenceId + ".fna"));
        var products = sequenceService.extract(gene);
        assertEquals(expectedSpan, products.genomicSpanSequence(), "genomic span must be source-coordinate exact");

        String sourceGeneId = sourceGene.attributes().get("ID");
        List<SourceFeature> transcripts = readTranscripts(annotation, sourceGeneId);
        assertFalse(transcripts.isEmpty(), "source GFF3 exposes transcript children");
        Set<String> transcriptIds = transcripts.stream().map(f -> f.attributes().get("ID"))
                .filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        List<SourceFeature> children = readChildren(annotation, transcriptIds);
        int checkedCodingProducts = 0;
        for (var transcript : products.transcripts()) {
            String transcriptId = transcript.transcriptId();
            SourceFeature sourceTranscript = transcripts.stream()
                    .filter(f -> transcriptId.equals(f.attributes().get("ID"))).findFirst().orElse(null);
            if (sourceTranscript == null) continue;
            List<SourceFeature> transcriptChildren = children.stream().filter(f -> parentIds(f).contains(transcriptId)).toList();
            List<SourceFeature> exons = ordered(transcriptChildren, "exon");
            List<SourceFeature> cds = ordered(transcriptChildren, "CDS");
            if (exons.isEmpty() || cds.isEmpty()) continue;
            assertEquals(joinFeatures(genomicReference, exons, sourceGene.strand()), transcript.transcriptSequence(),
                    "transcript sequence independently reconstructed from source exon rows: " + transcriptId);
            assertEquals(joinFeatures(genomicReference, cds, sourceGene.strand()), transcript.cdsSequence(),
                    "CDS independently reconstructed from source CDS rows: " + transcriptId);
            checkedCodingProducts++;
        }
        long expectedCodingProducts = transcriptIds.stream().filter(id -> children.stream()
                .anyMatch(f -> parentIds(f).contains(id) && f.type().equalsIgnoreCase("exon"))
                && children.stream().anyMatch(f -> parentIds(f).contains(id) && f.type().equalsIgnoreCase("CDS")))
                .count();
        assertTrue(expectedCodingProducts > 0, "source annotation exposes at least one coding transcript");
        assertEquals(expectedCodingProducts, checkedCodingProducts,
                "all source coding isoforms must have exact transcript and CDS exports");
    }

    private static List<SourceFeature> readTranscripts(Path annotation, String geneId) throws Exception {
        List<SourceFeature> result = new ArrayList<>();
        try (var raw = new GZIPInputStream(java.nio.file.Files.newInputStream(annotation));
             var reader = new BufferedReader(new InputStreamReader(raw, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) continue;
                SourceFeature feature = parseFeature(line);
                String type = feature.type().toLowerCase();
                if ((type.contains("rna") || type.contains("transcript")) && parentIds(feature).contains(geneId)) {
                    result.add(feature);
                }
            }
        }
        return result;
    }

    private static List<SourceFeature> readChildren(Path annotation, Set<String> transcriptIds) throws Exception {
        List<SourceFeature> result = new ArrayList<>();
        try (var raw = new GZIPInputStream(java.nio.file.Files.newInputStream(annotation));
             var reader = new BufferedReader(new InputStreamReader(raw, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) continue;
                SourceFeature feature = parseFeature(line);
                if (parentIds(feature).stream().anyMatch(transcriptIds::contains)) result.add(feature);
            }
        }
        return result;
    }

    private static SourceFeature parseFeature(String line) {
        String[] fields = line.split("\\t", 9);
        return new SourceFeature(fields[0], fields[2], Integer.parseInt(fields[3]), Integer.parseInt(fields[4]),
                fields[6].charAt(0), parseAttributes(fields[8]));
    }

    private static List<SourceFeature> ordered(List<SourceFeature> features, String type) {
        return features.stream().filter(f -> f.type().equalsIgnoreCase(type))
                .sorted(Comparator.comparingInt(SourceFeature::start)).toList();
    }

    private static String joinFeatures(String genome, List<SourceFeature> features, char strand) {
        StringBuilder joined = new StringBuilder();
        for (var feature : features) joined.append(genome, feature.start() - 1, feature.end());
        return strand == '-' ? reverseComplement(joined.toString()) : joined.toString();
    }

    private static String orient(String sequence, char strand) {
        return strand == '-' ? reverseComplement(sequence) : sequence;
    }

    private static String reverseComplement(String sequence) {
        StringBuilder result = new StringBuilder(sequence.length());
        for (int i = sequence.length() - 1; i >= 0; i--) {
            result.append(switch (sequence.charAt(i)) {
                case 'A' -> 'T'; case 'C' -> 'G'; case 'G' -> 'C'; case 'T' -> 'A';
                case 'R' -> 'Y'; case 'Y' -> 'R'; case 'S' -> 'S'; case 'W' -> 'W';
                case 'K' -> 'M'; case 'M' -> 'K'; case 'B' -> 'V'; case 'V' -> 'B';
                case 'D' -> 'H'; case 'H' -> 'D'; case 'N', '.', '-' -> sequence.charAt(i);
                default -> 'N';
            });
        }
        return result.toString();
    }

    private static Set<String> parentIds(SourceFeature feature) {
        String parent = feature.attributes().get("Parent");
        return parent == null ? Set.of() : java.util.Arrays.stream(parent.split(","))
                .collect(java.util.stream.Collectors.toSet());
    }

    private static Map<String, String> parseAttributes(String text) {
        Map<String, String> result = new java.util.LinkedHashMap<>();
        for (String field : text.split(";")) {
            int equals = field.indexOf('=');
            if (equals > 0) result.put(field.substring(0, equals), field.substring(equals + 1));
        }
        return result;
    }
}
