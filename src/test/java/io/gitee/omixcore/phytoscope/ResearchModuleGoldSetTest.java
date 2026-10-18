package io.gitee.omixcore.phytoscope;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gitee.omixcore.phytoscope.model.Gene;
import io.gitee.omixcore.phytoscope.model.Transcript;
import io.gitee.omixcore.phytoscope.parser.AnnotationFormat;
import io.gitee.omixcore.phytoscope.research.DomainLinkService;
import io.gitee.omixcore.phytoscope.research.IntervalAnalysisService;
import io.gitee.omixcore.phytoscope.research.ProteinHomologyService;
import io.gitee.omixcore.phytoscope.research.SampleGroupingService;
import io.gitee.omixcore.phytoscope.research.SequenceQualityService;
import io.gitee.omixcore.phytoscope.service.ExampleDataset;
import io.gitee.omixcore.phytoscope.service.ExpressionDataService;
import io.gitee.omixcore.phytoscope.service.GeneSequenceService;
import io.gitee.omixcore.phytoscope.service.GenomeDataService;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Source-based calculation checks for the five research modules.
 *
 * This is an independent test-side oracle over pinned Arabidopsis inputs, not
 * a claim of cross-species orthology accuracy or a new biological discovery.
 */
class ResearchModuleGoldSetTest {
    private static final int UPSTREAM = 2_000;
    private static final int DOWNSTREAM = 200;
    private static final List<String> RBCS_IDS = List.of(
            "AT1G67090", "AT5G38410", "AT5G38420", "AT5G38430");
    private static final Pattern CHROMOSOME_WORD =
            Pattern.compile("(?i)\\bchromosome\\s+([0-9]+|[a-z]+)\\b");
    private static final Pattern CHROMOSOME_COLON =
            Pattern.compile("(?i)chromosome:[^:]+:([0-9]+|[a-z]+):");
    private static final ObjectMapper JSON = new ObjectMapper();

    private record FastaRecord(String id, String description, String sequence) {}
    private record Window(int start, int end) {}
    private record ReferenceAlignment(int matches, int length, int querySpan,
                                      int targetSpan, int score) {}

    @Test
    void realArabidopsisInputsMatchIndependentResearchModuleCalculations() throws Exception {
        Path output = Path.of("target/research-validation");
        Files.createDirectories(output);

        GeneSequenceService sequences = new GeneSequenceService();
        sequences.loadBundledForDataset("arabidopsis");
        assertTrue(sequences.isLoaded(), "Pinned TAIR10 genome must be available in the test runtime");
        Map<String, FastaRecord> fasta = readBundledFasta();
        String chr1Sequence = resolveSequence(fasta, "Chr1");
        assertNotNull(chr1Sequence, "The independent FASTA reader must resolve TAIR10 Chr1");

        // 1. Promoter windows: independently calculate coordinates and directly
        // slice/reverse-complement the pinned genomic FASTA for both strands.
        GenomeDataService chr1 = new GenomeDataService();
        chr1.loadFromResource("/data/species/arabidopsis/Chr1.gff3", AnnotationFormat.GFF3);
        List<Gene> plusGenes = selectSpreadGenes(chr1, '+', chr1Sequence.length(), 4);
        List<Gene> minusGenes = selectSpreadGenes(chr1, '-', chr1Sequence.length(), 4);
        assertEquals(4, plusGenes.size());
        assertEquals(4, minusGenes.size());

        IntervalAnalysisService intervalService = new IntervalAnalysisService();
        List<Map<String, Object>> promoterRows = new ArrayList<>();
        for (Gene gene : concat(plusGenes, minusGenes)) {
            Window expectedWindow = referencePromoter(gene, chr1Sequence.length());
            String expectedSequence = referenceSlice(chr1Sequence, expectedWindow.start(),
                    expectedWindow.end(), gene.getStrand());
            var interval = intervalService.promoterInterval(gene, UPSTREAM, DOWNSTREAM, chr1Sequence.length());
            assertEquals(expectedWindow.start(), interval.start(), gene.getId() + " start");
            assertEquals(expectedWindow.end(), interval.end(), gene.getId() + " end");
            assertEquals(expectedSequence, sequences.extractPromoter(gene, UPSTREAM, DOWNSTREAM),
                    gene.getId() + " promoter from sequence service");
            assertEquals(expectedSequence, intervalService.extract(interval, sequences).sequence(),
                    gene.getId() + " promoter from interval service");
            assertEquals(UPSTREAM + DOWNSTREAM, expectedSequence.length(),
                    gene.getId() + " internal promoter window length");
            promoterRows.add(Map.of(
                    "gene_id", gene.getId(),
                    "chromosome", gene.getChromosome(),
                    "strand", String.valueOf(gene.getStrand()),
                    "start_1based_closed", expectedWindow.start(),
                    "end_1based_closed", expectedWindow.end(),
                    "length_bp", expectedSequence.length(),
                    "sequence_sha256", sha256(expectedSequence),
                    "sequence", expectedSequence));
        }

        // 2. Sequence QC and 3. protein similarity: use real RBCS gene models
        // and CDS/protein sequences extracted from the pinned genome.
        GenomeDataService rbcs = new GenomeDataService();
        rbcs.loadFromFile(Path.of("examples/arabidopsis-atlas/annotation.gff3"));
        SequenceQualityService quality = new SequenceQualityService();
        List<Map<String, Object>> qcRows = new ArrayList<>();
        List<ProteinHomologyService.Protein> proteins = new ArrayList<>();
        Map<String, String> proteinSequences = new LinkedHashMap<>();
        for (String geneId : RBCS_IDS) {
            Gene gene = rbcs.findGene(geneId);
            assertNotNull(gene, "Pinned Araport11 RBCS locus " + geneId);
            Transcript canonical = gene.getCanonicalTranscript();
            assertNotNull(canonical, "Canonical or representative transcript " + geneId);
            var extracted = sequences.extract(gene);
            var transcript = extracted.transcripts().stream()
                    .filter(t -> canonical.getId().equals(t.transcriptId()))
                    .findFirst().orElseThrow(() -> new AssertionError("Missing extracted transcript " + canonical.getId()));
            String cds = transcript.cdsSequence();
            var observedQc = quality.assessCds(transcript.transcriptId(), cds);
            Map<String, Object> expectedQc = referenceCdsMetrics(cds);
            assertEquals(((Number) expectedQc.get("length")).intValue(), observedQc.length(), geneId + " CDS length");
            assertEquals(((Number) expectedQc.get("ambiguous_bases")).intValue(), observedQc.ambiguousBases(),
                    geneId + " ambiguous bases");
            assertEquals((Boolean) expectedQc.get("length_multiple_of_three"), observedQc.lengthMultipleOfThree(),
                    geneId + " reading-frame flag");
            assertEquals(((Number) expectedQc.get("gc_percent")).doubleValue(), observedQc.gcPercent(), 1e-9,
                    geneId + " GC percentage");
            assertEquals(expectedQc.get("flags"), observedQc.flags(), geneId + " QC flags");
            qcRows.add(Map.of(
                    "gene_id", geneId,
                    "transcript_id", transcript.transcriptId(),
                    "cds_length", observedQc.length(),
                    "gc_percent", observedQc.gcPercent(),
                    "ambiguous_bases", observedQc.ambiguousBases(),
                    "length_multiple_of_three", observedQc.lengthMultipleOfThree(),
                    "flags", observedQc.flags(),
                    "cds_sha256", sha256(cds)));

            String protein = transcript.proteinSequence();
            assertFalse(protein.isEmpty(), geneId + " must have an extracted protein sequence");
            proteinSequences.put(geneId, protein);
            proteins.add(new ProteinHomologyService.Protein("Arabidopsis thaliana", geneId, protein));
        }

        // Two real proteins from unrelated annotated families provide negative controls.
        List<ProteinHomologyService.Protein> negativeControls = new ArrayList<>();
        Map<String, String> negativeFamilies = new LinkedHashMap<>();
        for (Map.Entry<String, String> spec : List.of(
                Map.entry("AT1G01010", "NAC001"),
                Map.entry("AT1G09200", "Histone H3.1"))) {
            Gene negativeGene = chr1.findGene(spec.getKey());
            assertNotNull(negativeGene, "Pinned negative-control gene " + spec.getKey());
            Transcript negativeCanonical = negativeGene.getCanonicalTranscript();
            assertNotNull(negativeCanonical, "Representative transcript " + spec.getKey());
            var negativeExtracted = sequences.extract(negativeGene);
            var negativeTranscript = negativeExtracted.transcripts().stream()
                    .filter(t -> negativeCanonical.getId().equals(t.transcriptId()))
                    .findFirst().orElseThrow(() -> new AssertionError("Missing negative-control transcript " + negativeCanonical.getId()));
            assertFalse(negativeTranscript.proteinSequence().isEmpty(), "Negative-control protein must be non-empty");
            negativeControls.add(new ProteinHomologyService.Protein("Arabidopsis thaliana",
                    spec.getKey(), negativeTranscript.proteinSequence()));
            negativeFamilies.put(spec.getKey(), spec.getValue());
        }

        ProteinHomologyService homology = new ProteinHomologyService();
        List<ProteinHomologyService.Hit> observedHits = homology.compare(proteins, proteins, 0);
        List<ProteinHomologyService.Protein> allReferenceProteins = new ArrayList<>(proteins);
        allReferenceProteins.addAll(negativeControls);
        List<ProteinHomologyService.Hit> filteredHits = homology.compare(
                proteins, allReferenceProteins, 30, 20, 0.5, 0.5);
        assertEquals(RBCS_IDS.size() * RBCS_IDS.size(), filteredHits.size(),
                "Coverage-aware filtering should retain all known within-family pairs");
        assertTrue(filteredHits.stream().noneMatch(h -> negativeFamilies.containsKey(h.targetId())),
                "Coverage-aware filtering should suppress unrelated-family negative controls");
        Map<String, ProteinHomologyService.Hit> hitsByPair = new HashMap<>();
        for (var hit : observedHits) hitsByPair.put(pairKey(hit.queryId(), hit.targetId()), hit);
        int expectedPairCount = 0;
        int[][] scores = new int[proteins.size()][proteins.size()];
        ReferenceAlignment[][] alignments = new ReferenceAlignment[proteins.size()][proteins.size()];
        for (int i = 0; i < proteins.size(); i++) {
            for (int j = 0; j < proteins.size(); j++) {
                ReferenceAlignment alignment = referenceSmithWaterman(
                        proteinSequences.get(proteins.get(i).id()),
                        proteinSequences.get(proteins.get(j).id()));
                alignments[i][j] = alignment;
                scores[i][j] = alignment.score();
            }
        }
        int[] bestTargetForQuery = new int[proteins.size()];
        int[] bestQueryForTarget = new int[proteins.size()];
        for (int i = 0; i < proteins.size(); i++) {
            int best = 0;
            for (int j = 1; j < proteins.size(); j++) {
                if (scores[i][j] > scores[i][best]) best = j;
            }
            bestTargetForQuery[i] = best;
        }
        for (int j = 0; j < proteins.size(); j++) {
            int best = 0;
            for (int i = 1; i < proteins.size(); i++) {
                if (scores[i][j] > scores[best][j]) best = i;
            }
            bestQueryForTarget[j] = best;
        }
        List<Map<String, Object>> alignmentRows = new ArrayList<>();
        for (int i = 0; i < proteins.size(); i++) {
            String queryId = proteins.get(i).id();
            for (int j = 0; j < proteins.size(); j++) {
                String targetId = proteins.get(j).id();
                ReferenceAlignment expected = alignments[i][j];
                if (expected.length() == 0) continue;
                expectedPairCount++;
                var observed = hitsByPair.get(pairKey(queryId, targetId));
                assertNotNull(observed, "Missing similarity pair " + queryId + " -> " + targetId);
                double expectedIdentity = 100.0 * expected.matches() / expected.length();
                double expectedQueryCoverage = (double) expected.querySpan() / cleanProtein(proteinSequences.get(queryId)).length();
                double expectedTargetCoverage = (double) expected.targetSpan() / cleanProtein(proteinSequences.get(targetId)).length();
                assertEquals(expectedIdentity, observed.identity(), 1e-9, queryId + " -> " + targetId + " identity");
                assertEquals(expected.length(), observed.alignedLength(), queryId + " -> " + targetId + " alignment length");
                assertEquals(expectedQueryCoverage, observed.queryCoverage(), 1e-9, queryId + " -> " + targetId + " query coverage");
                assertEquals(expectedTargetCoverage, observed.targetCoverage(), 1e-9, queryId + " -> " + targetId + " target coverage");
                boolean expectedRbh = bestTargetForQuery[i] == j && bestQueryForTarget[j] == i;
                assertEquals(expectedRbh, observed.reciprocalBestHit(), queryId + " -> " + targetId + " RBH flag");
                alignmentRows.add(Map.of(
                        "query_gene", queryId,
                        "target_gene", targetId,
                        "identity_percent", observed.identity(),
                        "aligned_length", observed.alignedLength(),
                        "query_coverage", observed.queryCoverage(),
                        "target_coverage", observed.targetCoverage(),
                        "reciprocal_best_hit_flag", observed.reciprocalBestHit()));
            }
        }
        assertEquals(expectedPairCount, observedHits.size(), "All positive local alignments must be reported");
        assertEquals(RBCS_IDS.size() * RBCS_IDS.size(), observedHits.size(),
                "The known RBCS family should have a local alignment for every within-family pair");
        // This is a family-similarity check, not an orthology ground truth.
        for (String id : RBCS_IDS) {
            var self = hitsByPair.get(pairKey(id, id));
            assertNotNull(self);
            assertEquals(100.0, self.identity(), 1e-9);
            assertEquals(1.0, self.queryCoverage(), 1e-9);
            assertEquals(1.0, self.targetCoverage(), 1e-9);
        }

        // 4. Expression grouping: source values are pinned single-assay Atlas
        // measurements. The grouping is descriptive arithmetic, not replication.
        ExpressionDataService expression = new ExpressionDataService();
        expression.loadFromResource("/data/case-studies/arabidopsis-atlas/expression.tsv",
                "Expression Atlas E-MTAB-7978");
        expression.reconcile(chr1);
        SampleGroupingService groups = makeAtlasGroups();
        var ordinaryMean = groups.summarize(expression, "AT1G01860", SampleGroupingService.Statistic.MEAN);
        var ordinaryMedian = groups.summarize(expression, "AT1G01860", SampleGroupingService.Statistic.MEDIAN);
        var leafMean = summary(ordinaryMean, "leaf");
        var leafMedian = summary(ordinaryMedian, "leaf");
        var ordinaryLow = summary(ordinaryMean, "low");
        assertEquals(3, leafMean.n());
        assertEquals(16.0, leafMean.value(), 1e-9);
        assertEquals(16.0, leafMedian.value(), 1e-9);
        assertEquals(2, ordinaryLow.n());
        assertEquals(2.5, ordinaryLow.value(), 1e-9);

        expression.reconcile(rbcs);
        var zeroSummary = summary(groups.summarize(expression, "AT5G38430",
                SampleGroupingService.Statistic.MEAN), "low");
        assertEquals(2, zeroSummary.n());
        assertEquals(0.0, zeroSummary.value(), 1e-9,
                "Measured zero values must be retained in a group summary");

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("title", "Real-data research-module calculation checks");
        report.put("date", "2026-10-18");
        report.put("reference", "Arabidopsis TAIR10 genome, Araport11 annotation, Expression Atlas E-MTAB-7978");
        report.put("promoter_extraction", Map.of(
                "independent_oracle", "direct 1-based closed FASTA slice and test-side reverse complement",
                "upstream_bp", UPSTREAM,
                "downstream_bp", DOWNSTREAM,
                "loci_checked", promoterRows.size(),
                "plus_strand_loci", plusGenes.size(),
                "minus_strand_loci", minusGenes.size(),
                "records", promoterRows));
        report.put("sequence_quality", Map.of(
                "independent_oracle", "test-side base counts, frame check, start/stop checks and flag reconstruction",
                "cds_records_checked", qcRows.size(),
                "records", qcRows));
        List<Map<String, Object>> referenceProteinRows = new ArrayList<>();
        for (var protein : proteins) {
            referenceProteinRows.add(Map.of("gene_id", protein.id(), "family", "RBCS", "sequence", protein.sequence()));
        }
        for (var protein : negativeControls) {
            referenceProteinRows.add(Map.of("gene_id", protein.id(), "family", negativeFamilies.get(protein.id()),
                    "sequence", protein.sequence()));
        }
        List<Map<String, Object>> filteredHitRows = filteredHits.stream()
                .map(hit -> Map.<String, Object>of(
                        "query_gene", hit.queryId(),
                        "target_gene", hit.targetId(),
                        "identity_percent", hit.identity(),
                        "aligned_length", hit.alignedLength(),
                        "query_coverage", hit.queryCoverage(),
                        "target_coverage", hit.targetCoverage()))
                .toList();
        Map<String, Object> filteredSearch = new LinkedHashMap<>();
        filteredSearch.put("minimum_identity_percent", 30);
        filteredSearch.put("minimum_aligned_length_aa", 20);
        filteredSearch.put("minimum_query_coverage_fraction", 0.5);
        filteredSearch.put("minimum_target_coverage_fraction", 0.5);
        filteredSearch.put("positive_family_pairs_expected", RBCS_IDS.size() * RBCS_IDS.size());
        filteredSearch.put("positive_family_pairs_reported", filteredHits.size());
        filteredSearch.put("negative_control_pairs_expected", proteins.size() * negativeControls.size());
        filteredSearch.put("negative_control_hits_reported", filteredHits.stream()
                .filter(hit -> negativeFamilies.containsKey(hit.targetId())).count());
        filteredSearch.put("hits", filteredHitRows);
        Map<String, Object> similarityReport = new LinkedHashMap<>();
        similarityReport.put("independent_oracle", "test-side Smith-Waterman dynamic-programming reference");
        similarityReport.put("rbcS_protein_records", proteins.size());
        similarityReport.put("pairwise_hits_checked", observedHits.size());
        similarityReport.put("records", alignmentRows);
        similarityReport.put("reference_proteins", referenceProteinRows);
        similarityReport.put("filtered_search", filteredSearch);
        similarityReport.put("interpretation", "RBCS family similarity classification only; not orthology inference.");
        report.put("protein_similarity", similarityReport);
        Map<String, Object> expressionSummary = new LinkedHashMap<>();
        expressionSummary.put("source", "Expression Atlas E-MTAB-7978; single-assay condition values");
        expressionSummary.put("ordinary_gene", "AT1G01860");
        expressionSummary.put("leaf_mean", leafMean.value());
        expressionSummary.put("leaf_median", leafMedian.value());
        expressionSummary.put("leaf_observations", leafMean.n());
        expressionSummary.put("ordinary_low_mean", ordinaryLow.value());
        expressionSummary.put("ordinary_low_observations", ordinaryLow.n());
        expressionSummary.put("zero_gene", "AT5G38430");
        expressionSummary.put("zero_group_mean", zeroSummary.value());
        expressionSummary.put("zero_group_observations", zeroSummary.n());
        expressionSummary.put("interpretation", "Descriptive aggregation across labelled conditions, not replicate-based differential expression.");
        report.put("expression_grouping", expressionSummary);
        report.put("domain_mapping", Map.of(
                "real_records", 16,
                "genes", 13,
                "validation", "Separately checked against independent Python coordinates by check_isoform_case.py and DomainCaseValidationTest",
                "interpretation", "Coordinate mapping validation does not establish isoform function or a new biological discovery."));
        report.put("claim_boundary", "These calculations validate selected implementation outputs on pinned real inputs. They do not establish general cross-species accuracy, orthology, usability or a new biological discovery.");
        JSON.writerWithDefaultPrettyPrinter().writeValue(output.resolve("research-module-gold-set.json").toFile(), report);
    }

    private static SampleGroupingService makeAtlasGroups() {
        SampleGroupingService groups = new SampleGroupingService();
        groups.assign("Root", "root");
        groups.assign("Rosette_leaf_1", "leaf");
        groups.assign("Rosette_leaf_8", "leaf");
        groups.assign("Cauline_leaf", "leaf");
        groups.assign("Dry_seed", "low");
        groups.assign("Pollen", "low");
        return groups;
    }

    private static SampleGroupingService.GroupSummary summary(
            List<SampleGroupingService.GroupSummary> summaries, String name) {
        return summaries.stream().filter(s -> s.group().equals(name)).findFirst()
                .orElseThrow(() -> new AssertionError("Missing group " + name));
    }

    private static List<Gene> selectSpreadGenes(GenomeDataService genome, char strand,
                                                int sequenceLength, int count) {
        List<Gene> eligible = genome.getGenes().stream()
                .filter(g -> "Chr1".equalsIgnoreCase(g.getChromosome()))
                .filter(g -> g.getStrand() == strand)
                .filter(g -> g.getStart() > UPSTREAM + DOWNSTREAM
                        && g.getEnd() < sequenceLength - UPSTREAM - DOWNSTREAM)
                .sorted(Comparator.comparingInt(Gene::getStart))
                .toList();
        assertTrue(eligible.size() >= count, "Need " + count + " real Chr1 loci on strand " + strand);
        List<Gene> selected = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int index = count == 1 ? eligible.size() / 2
                    : (int) Math.round(i * (eligible.size() - 1) / (double) (count - 1));
            selected.add(eligible.get(index));
        }
        return List.copyOf(selected);
    }

    private static List<Gene> concat(List<Gene> first, List<Gene> second) {
        List<Gene> all = new ArrayList<>(first);
        all.addAll(second);
        return all;
    }

    private static Window referencePromoter(Gene gene, int sequenceLength) {
        long tss = gene.getStrand() == '-' ? gene.getEnd() : gene.getStart();
        long rawStart = gene.getStrand() == '-' ? tss - DOWNSTREAM + 1 : tss - UPSTREAM;
        long rawEnd = gene.getStrand() == '-' ? tss + UPSTREAM : tss + DOWNSTREAM - 1;
        int start = (int) Math.max(1, Math.min(sequenceLength, rawStart));
        int end = (int) Math.max(1, Math.min(sequenceLength, rawEnd));
        if (end < start) throw new IllegalArgumentException("Empty reference promoter interval for " + gene.getId());
        return new Window(start, end);
    }

    private static Map<String, Object> referenceCdsMetrics(String sequence) {
        String upper = sequence.toUpperCase(Locale.ROOT);
        int gc = 0;
        int ambiguous = 0;
        for (char c : upper.toCharArray()) {
            if (c == 'G' || c == 'C') gc++;
            if ("ACGTU".indexOf(c) < 0) ambiguous++;
        }
        boolean inFrame = upper.length() % 3 == 0;
        List<String> flags = new ArrayList<>();
        if (upper.isEmpty()) flags.add("empty sequence");
        if (ambiguous > 0) flags.add(ambiguous + " ambiguous/non-ACGT bases");
        if (!inFrame) flags.add("CDS length is not divisible by three");
        String normalized = upper.replace('U', 'T');
        if (normalized.length() >= 3 && !normalized.startsWith("ATG"))
            flags.add("CDS does not start with ATG (may be partial or use a non-standard code)");
        if (normalized.length() >= 3 && !Set.of("TAA", "TAG", "TGA")
                .contains(normalized.substring(normalized.length() - 3)))
            flags.add("CDS has no terminal stop codon (may be partial)");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("length", upper.length());
        result.put("gc_percent", upper.isEmpty() ? Double.NaN : 100.0 * gc / upper.length());
        result.put("ambiguous_bases", ambiguous);
        result.put("length_multiple_of_three", inFrame);
        result.put("flags", flags);
        return result;
    }

    private static ReferenceAlignment referenceSmithWaterman(String query, String target) {
        String q = cleanProtein(query);
        String t = cleanProtein(target);
        int n = q.length(), m = t.length();
        if (n == 0 || m == 0) return new ReferenceAlignment(0, 0, 0, 0, 0);
        int[][] matrix = new int[n + 1][m + 1];
        int best = 0, bestI = 0, bestJ = 0;
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                int diagonal = matrix[i - 1][j - 1] + (q.charAt(i - 1) == t.charAt(j - 1) ? 2 : -1);
                int up = matrix[i - 1][j] - 2;
                int left = matrix[i][j - 1] - 2;
                matrix[i][j] = Math.max(0, Math.max(diagonal, Math.max(up, left)));
                if (matrix[i][j] > best) {
                    best = matrix[i][j];
                    bestI = i;
                    bestJ = j;
                }
            }
        }
        int i = bestI, j = bestJ, matches = 0, length = 0, querySpan = 0, targetSpan = 0;
        while (i > 0 && j > 0 && matrix[i][j] > 0) {
            int value = matrix[i][j];
            int diagonal = matrix[i - 1][j - 1] + (q.charAt(i - 1) == t.charAt(j - 1) ? 2 : -1);
            if (value == diagonal) {
                if (q.charAt(i - 1) == t.charAt(j - 1)) matches++;
                i--; j--; querySpan++; targetSpan++;
            } else if (value == matrix[i - 1][j] - 2) {
                i--; querySpan++;
            } else if (value == matrix[i][j - 1] - 2) {
                j--; targetSpan++;
            } else {
                throw new AssertionError("Reference alignment traceback failed");
            }
            length++;
        }
        return new ReferenceAlignment(matches, length, querySpan, targetSpan, best);
    }

    private static String cleanProtein(String sequence) {
        return sequence == null ? "" : sequence.toUpperCase(Locale.ROOT).replaceAll("[^A-Z*]", "");
    }

    private static String pairKey(String queryId, String targetId) {
        return queryId + "\u0000" + targetId;
    }

    private static String referenceSlice(String chromosome, int start, int end, char strand) {
        String forward = chromosome.substring(start - 1, end).toUpperCase(Locale.ROOT);
        if (strand == '+') return forward;
        StringBuilder reverse = new StringBuilder(forward.length());
        for (int i = forward.length() - 1; i >= 0; i--) reverse.append(referenceComplement(forward.charAt(i)));
        return reverse.toString();
    }

    private static char referenceComplement(char base) {
        return switch (Character.toUpperCase(base)) {
            case 'A' -> 'T';
            case 'T', 'U' -> 'A';
            case 'C' -> 'G';
            case 'G' -> 'C';
            case 'R' -> 'Y';
            case 'Y' -> 'R';
            case 'S' -> 'S';
            case 'W' -> 'W';
            case 'K' -> 'M';
            case 'M' -> 'K';
            case 'B' -> 'V';
            case 'V' -> 'B';
            case 'D' -> 'H';
            case 'H' -> 'D';
            case 'N' -> 'N';
            default -> 'N';
        };
    }

    private static Map<String, FastaRecord> readBundledFasta() throws IOException {
        InputStream resource = ResearchModuleGoldSetTest.class
                .getResourceAsStream(GeneSequenceService.ARABIDOPSIS_TAIR10_RESOURCE);
        if (resource == null) throw new IOException("Missing pinned TAIR10 genomic FASTA resource");
        Map<String, FastaRecord> records = new LinkedHashMap<>();
        try (InputStream raw = resource;
             GZIPInputStream gzip = new GZIPInputStream(raw);
             BufferedReader reader = new BufferedReader(new InputStreamReader(gzip, StandardCharsets.UTF_8))) {
            String header = null;
            StringBuilder sequence = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(">")) {
                    if (header != null) putFastaRecord(records, header, sequence);
                    header = line.substring(1).trim();
                    sequence.setLength(0);
                } else if (!line.isBlank()) {
                    if (header == null) throw new IOException("FASTA sequence appears before first header");
                    sequence.append(line.replaceAll("\\s+", "").toUpperCase(Locale.ROOT));
                }
            }
            if (header != null) putFastaRecord(records, header, sequence);
        }
        if (records.isEmpty()) throw new IOException("No records in pinned TAIR10 FASTA");
        return records;
    }

    private static void putFastaRecord(Map<String, FastaRecord> records, String header, StringBuilder sequence)
            throws IOException {
        int space = header.indexOf(' ');
        String id = space < 0 ? header : header.substring(0, space);
        String description = space < 0 ? "" : header.substring(space + 1);
        if (sequence.isEmpty()) throw new IOException("Empty FASTA record " + id);
        if (records.putIfAbsent(id, new FastaRecord(id, description, sequence.toString())) != null)
            throw new IOException("Duplicate FASTA record ID " + id);
    }

    private static String resolveSequence(Map<String, FastaRecord> records, String chromosome) {
        String target = normalizeSequenceName(chromosome);
        for (FastaRecord record : records.values()) {
            if (normalizeSequenceName(record.id()).equals(target)) return record.sequence();
            Matcher word = CHROMOSOME_WORD.matcher(record.description());
            if (word.find() && normalizeSequenceName(word.group(1)).equals(target)) return record.sequence();
            Matcher colon = CHROMOSOME_COLON.matcher(record.description());
            if (colon.find() && normalizeSequenceName(colon.group(1)).equals(target)) return record.sequence();
        }
        return null;
    }

    private static String normalizeSequenceName(String name) {
        String normalized = name == null ? "" : name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        return normalized.startsWith("chr") && normalized.length() > 3
                ? normalized.substring(3) : normalized;
    }

    private static String sha256(String value) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        return java.util.HexFormat.of().formatHex(digest);
    }
}
