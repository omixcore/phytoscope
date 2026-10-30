package io.gitee.omixcore.phytoscope;

import io.gitee.omixcore.phytoscope.model.Gene;
import io.gitee.omixcore.phytoscope.model.Transcript;
import io.gitee.omixcore.phytoscope.service.ExpressionDataService;
import io.gitee.omixcore.phytoscope.service.GeneSequenceService;
import io.gitee.omixcore.phytoscope.service.GenomeDataService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Small golden regression set: one synthetic locus panel expressed twice (GFF3
 * and GTF), a matching synthetic FASTA and a matching expression matrix.
 * Pins cross-format equivalence, strand handling, transcript isoforms,
 * CDS/UTR extraction and identifier reconciliation so later performance or
 * refactoring work cannot silently change results. The loci are illustrative,
 * not real biology.
 */
@DisplayName("Golden regression: cross-format, strand, isoform, extraction and reconciliation")
class GoldenRegressionTest {

    private static final String CHROMOSOME = "Chr1";

    /** 60 bases: ACGT repeated, so every interval has a predictable sequence. */
    private static final String CHR_SEQUENCE = "ACGT".repeat(15);

    private static final String GFF3 = """
            ##gff-version 3
            ##sequence-region Chr1 1 60
            Chr1\tsrc\tgene\t1\t40\t.\t+\t.\tID=gene:G1;Name=PLUS
            Chr1\tsrc\tmRNA\t1\t40\t.\t+\t.\tID=transcript:T1;Parent=gene:G1
            Chr1\tsrc\texon\t1\t20\t.\t+\t.\tParent=transcript:T1
            Chr1\tsrc\texon\t31\t40\t.\t+\t.\tParent=transcript:T1
            Chr1\tsrc\tCDS\t5\t20\t.\t+\t0\tID=CDS:T1-1;Parent=transcript:T1
            Chr1\tsrc\tCDS\t31\t36\t.\t+\t0\tID=CDS:T1-2;Parent=transcript:T1
            Chr1\tsrc\tfive_prime_UTR\t1\t4\t.\t+\t.\tParent=transcript:T1
            Chr1\tsrc\tthree_prime_UTR\t37\t40\t.\t+\t.\tParent=transcript:T1
            Chr1\tsrc\tmRNA\t1\t40\t.\t+\t.\tID=transcript:T2;Parent=gene:G1
            Chr1\tsrc\texon\t1\t40\t.\t+\t.\tParent=transcript:T2
            Chr1\tsrc\tCDS\t5\t36\t.\t+\t0\tID=CDS:T2-1;Parent=transcript:T2
            Chr1\tsrc\tgene\t45\t60\t.\t-\t.\tID=gene:G2;Name=MINUS
            Chr1\tsrc\tmRNA\t45\t60\t.\t-\t.\tID=transcript:T3;Parent=gene:G2
            Chr1\tsrc\texon\t45\t60\t.\t-\t.\tParent=transcript:T3
            Chr1\tsrc\tCDS\t48\t57\t.\t-\t0\tID=CDS:T3-1;Parent=transcript:T3
            """;

    private static final String GTF = """
            Chr1\tsrc\tgene\t1\t40\t.\t+\t.\tgene_id "G1"; gene_name "PLUS";
            Chr1\tsrc\ttranscript\t1\t40\t.\t+\t.\tgene_id "G1"; transcript_id "T1";
            Chr1\tsrc\texon\t1\t20\t.\t+\t.\tgene_id "G1"; transcript_id "T1";
            Chr1\tsrc\texon\t31\t40\t.\t+\t.\tgene_id "G1"; transcript_id "T1";
            Chr1\tsrc\tCDS\t5\t20\t.\t+\t0\tgene_id "G1"; transcript_id "T1";
            Chr1\tsrc\tCDS\t31\t36\t.\t+\t0\tgene_id "G1"; transcript_id "T1";
            Chr1\tsrc\ttranscript\t1\t40\t.\t+\t.\tgene_id "G1"; transcript_id "T2";
            Chr1\tsrc\texon\t1\t40\t.\t+\t.\tgene_id "G1"; transcript_id "T2";
            Chr1\tsrc\tCDS\t5\t36\t.\t+\t0\tgene_id "G1"; transcript_id "T2";
            Chr1\tsrc\tgene\t45\t60\t.\t-\t.\tgene_id "G2"; gene_name "MINUS";
            Chr1\tsrc\ttranscript\t45\t60\t.\t-\t.\tgene_id "G2"; transcript_id "T3";
            Chr1\tsrc\texon\t45\t60\t.\t-\t.\tgene_id "G2"; transcript_id "T3";
            Chr1\tsrc\tCDS\t48\t57\t.\t-\t0\tgene_id "G2"; transcript_id "T3";
            """;

    private static final String FASTA = ">" + CHROMOSOME + "\n" + CHR_SEQUENCE + "\n";

    private static GenomeDataService gff3;
    private static GenomeDataService gtf;
    private static GeneSequenceService sequences;

    @BeforeAll
    static void parseFixtures() throws Exception {
        gff3 = new GenomeDataService();
        gff3.loadFromContent(GFF3, "golden gff3");
        gtf = new GenomeDataService();
        gtf.loadFromContent(GTF, "golden gtf");
        sequences = new GeneSequenceService();
        sequences.loadFromContent(FASTA, "golden fasta");
    }

    /** GFF3 keeps gene:/transcript: prefixes in IDs; GTF uses bare IDs. Compare on the bare part. */
    private static String bare(String id) {
        return id.replaceFirst("^(gene|transcript):", "");
    }

    private static Map<String, Gene> byId(GenomeDataService service) {
        return service.getGenes().stream()
                .collect(Collectors.toMap(g -> bare(g.getId()), g -> g));
    }

    private static String slice(int start, int end) {
        return CHR_SEQUENCE.substring(start - 1, end);
    }

    private static String revcomp(String dna) {
        var out = new StringBuilder(dna.length());
        for (int i = dna.length() - 1; i >= 0; i--) {
            out.append(switch (dna.charAt(i)) {
                case 'A' -> 'T';
                case 'T' -> 'A';
                case 'C' -> 'G';
                case 'G' -> 'C';
                default -> throw new IllegalArgumentException(dna);
            });
        }
        return out.toString();
    }

    @Test
    @DisplayName("GFF3 and GTF dialects assemble identical gene structures")
    void crossFormatEquivalence() {
        assertEquals(2, gff3.getGenes().size());
        assertEquals(2, gtf.getGenes().size());
        Map<String, Gene> fromGff3 = byId(gff3);
        Map<String, Gene> fromGtf = byId(gtf);
        assertEquals(fromGff3.keySet(), fromGtf.keySet(), "same gene IDs in both dialects");
        for (String id : fromGff3.keySet()) {
            Gene a = fromGff3.get(id), b = fromGtf.get(id);
            assertEquals(a.getChromosome(), b.getChromosome(), id);
            assertEquals(a.getStart(), b.getStart(), id);
            assertEquals(a.getEnd(), b.getEnd(), id);
            assertEquals(a.getStrand(), b.getStrand(), id);
            var txA = a.getTranscripts().stream().map(t -> bare(t.getId())).sorted().toList();
            var txB = b.getTranscripts().stream().map(t -> bare(t.getId())).sorted().toList();
            assertEquals(txA, txB, id + " transcripts");
            for (String txId : txA) {
                Transcript ta = transcript(a, txId), tb = transcript(b, txId);
                assertEquals(coords(ta.getExonsByCoordinate()), coords(tb.getExonsByCoordinate()),
                        id + "/" + txId + " exons");
                assertEquals(coords(ta.getAllCdsSegments()), coords(tb.getAllCdsSegments()),
                        id + "/" + txId + " CDS");
            }
        }
    }

    private static Transcript transcript(Gene gene, String bareTxId) {
        return gene.getTranscripts().stream()
                .filter(t -> bare(t.getId()).equals(bareTxId)).findFirst().orElseThrow();
    }

    private static List<String> coords(List<? extends io.gitee.omixcore.phytoscope.model.GenomicFeature> features) {
        return features.stream().map(f -> f.getStart() + "-" + f.getEnd())
                .collect(Collectors.toList());
    }

    @Test
    @DisplayName("Plus-strand isoforms keep annotated CDS and UTR boundaries")
    void plusStrandIsoforms() throws Exception {
        Gene g1 = byId(gff3).get("G1");
        assertEquals('+', g1.getStrand());
        assertEquals(2, g1.getTranscripts().size());

        var extracted = sequences.extract(g1);
        assertEquals(slice(1, 40), extracted.genomicSpanSequence(),
                "gene span sequence is stored in chromosome orientation");

        var t1 = extracted.transcripts().stream()
                .filter(t -> bare(t.transcriptId()).equals("T1")).findFirst().orElseThrow();
        assertEquals(slice(1, 20) + slice(31, 40), t1.transcriptSequence(), "spliced transcript");
        assertEquals(slice(5, 20) + slice(31, 36), t1.cdsSequence(), "two-segment CDS");
        assertEquals(slice(1, 4), t1.fivePrimeUtrSequence(), "annotated 5' UTR");
        assertEquals(slice(37, 40), t1.threePrimeUtrSequence(), "annotated 3' UTR");

        var t2 = extracted.transcripts().stream()
                .filter(t -> bare(t.transcriptId()).equals("T2")).findFirst().orElseThrow();
        assertEquals(slice(5, 36), t2.cdsSequence(), "single-exon CDS");
        assertEquals(slice(1, 4), t2.fivePrimeUtrSequence(), "5' UTR inferred from exon/CDS boundary");
        assertEquals(slice(37, 40), t2.threePrimeUtrSequence(), "3' UTR inferred from exon/CDS boundary");
    }

    @Test
    @DisplayName("Minus-strand gene yields reverse-complemented sequence")
    void minusStrandReverseComplement() throws Exception {
        Gene g2 = byId(gff3).get("G2");
        assertEquals('-', g2.getStrand());
        var extracted = sequences.extract(g2);
        assertEquals(slice(45, 60), extracted.genomicSpanSequence(),
                "gene span sequence is stored in chromosome orientation");
        var t3 = extracted.transcripts().getFirst();
        assertEquals(revcomp(slice(48, 57)), t3.cdsSequence(), "CDS reverse-complemented");
        assertEquals(revcomp(slice(45, 60)), t3.transcriptSequence());
        // Independent check of the interval helper in the same orientation.
        assertEquals(revcomp(slice(45, 60)), sequences.extractInterval(CHROMOSOME, 45, 60, '-'));
    }

    @Test
    @DisplayName("Expression reconciliation matches bare and gene:-prefixed IDs")
    void expressionReconciliation(@TempDir Path temp) throws Exception {
        Path matrix = temp.resolve("golden.tsv");
        Files.writeString(matrix, """
                # unit: TPM
                # level: gene
                GeneID\tleaf_A\tleaf_B
                G1\t10.5\t1.5
                gene:G2\t3\t9
                G3\t0\t7
                """);
        var expression = new ExpressionDataService();
        expression.loadFromFile(matrix);
        var report = expression.reconcile(gff3);

        assertEquals(2, report.matchedIds(), "G1 and gene:G2 reconcile, G3 does not");
        assertEquals(List.of("G3"), report.unmatchedIds());
        assertTrue(report.summary().contains("2 matched of 3"), report.summary());
        String g1AnnotationId = byId(gff3).get("G1").getId();
        String g2AnnotationId = byId(gff3).get("G2").getId();
        assertEquals(10.5, expression.getProfileForAnnotation(g1AnnotationId).getTpm("leaf_A"));
        assertEquals(9.0, expression.getProfileForAnnotation(g2AnnotationId).getTpm("leaf_B"),
                "gene: prefix on the input side must still map");
        assertTrue(expression.describe(g1AnnotationId).contains("leaf_A"));
    }

    @Test
    @DisplayName("Near-match hints diagnose case and version-suffix mismatches")
    void diagnosticHints(@TempDir Path temp) throws Exception {
        Path matrix = temp.resolve("hints.tsv");
        Files.writeString(matrix, """
                GeneID\tleaf_A
                g1\t1
                G2.1\t2
                """);
        var expression = new ExpressionDataService();
        expression.loadFromFile(matrix);
        var report = expression.reconcile(gff3);
        String summary = report.summary();
        assertTrue(summary.contains("identifier case must match"), summary);
        assertTrue(summary.contains("version suffix"), summary);
    }

    @Test
    @DisplayName("Duplicate matrix identifiers fail with line numbers")
    void duplicateIdentifierRejected(@TempDir Path temp) throws Exception {
        Path matrix = temp.resolve("dup.tsv");
        Files.writeString(matrix, """
                GeneID\tleaf_A
                G1\t1
                G1\t2
                """);
        var expression = new ExpressionDataService();
        var thrown = assertThrows(IOException.class, () -> expression.loadFromFile(matrix));
        assertTrue(thrown.getMessage().contains("Duplicate identifier 'G1'"), thrown.getMessage());
        assertTrue(thrown.getMessage().contains("line"), thrown.getMessage());
    }

    @Test
    @DisplayName("Reconciling twice after replacing data keeps counts stable")
    void reconcileIsRepeatable(@TempDir Path temp) throws Exception {
        Path matrix = temp.resolve("repeat.tsv");
        Files.writeString(matrix, "GeneID\tleaf_A\nG1\t5\n");
        var expression = new ExpressionDataService();
        expression.loadFromFile(matrix);
        var first = expression.reconcile(gff3);
        var second = expression.reconcile(gff3);
        assertEquals(first.matchedIds(), second.matchedIds());
        assertEquals(first.unmatchedIds(), second.unmatchedIds());
        assertEquals(1, second.matchedIds());
    }
}
