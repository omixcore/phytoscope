package io.gitee.omixcore.phytoscope;

import io.gitee.omixcore.phytoscope.service.GeneSequenceService;
import io.gitee.omixcore.phytoscope.service.GenomeDataService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeneSequenceServiceTest {

    /**
     * A 7-record, KB-scale FASTA fixture that mirrors the TAIR10 record names
     * (1-5, Mt, Pt). The complete TAIR10 genome was archived in the independent
     * reference-data repository, so this test no longer reads a bundled MB-scale
     * resource. Headers include "chromosome N" descriptions to exercise the same
     * alias resolution (Chr1 -&gt; 1) as the archived data.
     */
    private static final String SMALL_FASTA = smallFasta();
    private static final String SMALL_FASTA_LABEL = "small 7-record fixture";

    private static String smallFasta() {
        String bases = "ACGT".repeat(15); // 60 bp
        StringBuilder fasta = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            fasta.append('>').append(i)
                    .append(" chromosome ").append(i)
                    .append(" dna:chromosome chromosome:TAIR10:").append(i)
                    .append(":1:1000:1\n")
                    .append(bases).append('\n');
        }
        fasta.append(">Mt mitochondrion\n").append(bases).append('\n');
        fasta.append(">Pt chloroplast\n").append(bases).append('\n');
        return fasta.toString();
    }

    @Test
    void extractsSplicedTranscriptCdsUtrsAndProteinInTranscriptionOrientation() throws Exception {
        String annotation = """
                ##gff-version 3
                ##sequence-region chr1 1 15
                chr1\\ttest\\tgene\\t1\\t15\\t.\\t+\\t.\\tID=g1;Name=PlusGene
                chr1\\ttest\\tmRNA\\t1\\t15\\t.\\t+\\t.\\tID=t1;Parent=g1
                chr1\\ttest\\texon\\t1\\t6\\t.\\t+\\t.\\tID=e1;Parent=t1
                chr1\\ttest\\texon\\t10\\t15\\t.\\t+\\t.\\tID=e2;Parent=t1
                chr1\\ttest\\tfive_prime_UTR\\t1\\t2\\t.\\t+\\t.\\tParent=t1
                chr1\\ttest\\tCDS\\t3\\t6\\t.\\t+\\t0\\tParent=t1
                chr1\\ttest\\tCDS\\t10\\t13\\t.\\t+\\t2\\tParent=t1
                chr1\\ttest\\tthree_prime_UTR\\t14\\t15\\t.\\t+\\t.\\tParent=t1
                """.replace("\\t", "\t");
        GenomeDataService genome = new GenomeDataService();
        genome.loadFromContent(annotation, "test annotation");
        GeneSequenceService sequences = new GeneSequenceService();
        sequences.loadFromContent(">chr1\\nAACCATGGGGTTTAA\\n".replace("\\n", "\n"), "synthetic genome");
        var result = sequences.extract(genome.findGene("g1"));
        assertEquals("AACCATGGGGTTTAA", result.genomicSpanSequence());
        var transcript = result.transcripts().getFirst();
        assertEquals("AACCATGTTTAA", transcript.transcriptSequence());
        assertEquals("CCATGTTT", transcript.cdsSequence());
        assertEquals("AA", transcript.fivePrimeUtrSequence());
        assertEquals("AA", transcript.threePrimeUtrSequence());
        assertEquals("PC", transcript.proteinSequence());
        assertEquals("annotated", transcript.fivePrimeUtrSource());
    }

    @Test
    void reverseComplementsIupacAndTranslatesStandardCodons() {
        assertEquals("NBDHVKMWSRYACGT", GeneSequenceService.reverseComplement("ACGTRYSWKMBDHVN"));
        assertEquals("MKF", GeneSequenceService.translateCds("ATGAAATTTTAA"));
        assertEquals("MX", GeneSequenceService.translateCds("ATGNNN"));
        assertEquals("", GeneSequenceService.translateCds(""));
    }

    /**
     * Replaces the archived TAIR10 resource test with an equivalent small
     * fixture: 7 records, alias resolution, strand-aware reverse complement,
     * and a user-supplied source label. The four assertions of the original
     * (loaded state, record count, +/- strand reciprocity, source label) are
     * preserved with the same structure.
     */
    @Test
    void smallFixtureResolvesChrAliasesAndReturnsRealGenomicBases() throws Exception {
        GeneSequenceService sequences = new GeneSequenceService();
        sequences.loadFromContent(SMALL_FASTA, SMALL_FASTA_LABEL);
        assertTrue(sequences.isLoaded());
        assertEquals(7, sequences.getSequenceCount());
        var forward = sequences.extract(new io.gitee.omixcore.phytoscope.model.Gene(
                "test-plus", "test-plus", "Chr1", 1, 60, '+'));
        var reverse = sequences.extract(new io.gitee.omixcore.phytoscope.model.Gene(
                "test-minus", "test-minus", "1", 1, 60, '-'));
        assertEquals(60, forward.genomicSpanSequence().length());
        assertEquals(GeneSequenceService.reverseComplement(forward.genomicSpanSequence()),
                reverse.genomicSpanSequence());
        assertEquals(SMALL_FASTA_LABEL, forward.sourceLabel());
    }

    /** AC-3: without a user-supplied FASTA the service fails loudly and never fabricates a sequence. */
    @Test
    void unloadedFastaFailsLoudlyInsteadOfReturningPlaceholderSequence() {
        GeneSequenceService sequences = new GeneSequenceService();
        assertFalse(sequences.isLoaded());
        assertEquals(0, sequences.getSequenceCount());
        assertNull(sequences.getSourceLabel());
        var gene = new io.gitee.omixcore.phytoscope.model.Gene(
                "test", "test", "1", 1, 10, '+');
        java.io.IOException extractError = assertThrows(java.io.IOException.class,
                () -> sequences.extract(gene));
        assertTrue(extractError.getMessage().contains(GeneSequenceService.NO_FASTA_MESSAGE));
        java.io.IOException intervalError = assertThrows(java.io.IOException.class,
                () -> sequences.extractInterval("1", 1, 10, '+'));
        assertTrue(intervalError.getMessage().contains(GeneSequenceService.NO_FASTA_MESSAGE));
        assertTrue(GeneSequenceService.NO_FASTA_MESSAGE.contains("Import genome FASTA"));
        assertTrue(GeneSequenceService.NO_FASTA_MESSAGE.contains("*.gz"));
    }

    @Test
    void rejectsDuplicateFastaIdsAndProteinSequences() {
        GeneSequenceService service = new GeneSequenceService();
        assertThrows(java.io.IOException.class,
                () -> service.loadFromContent(">chr1\\nATGC\\n>chr1\\nATGC\\n".replace("\\n", "\n"), "duplicate"));
        assertThrows(java.io.IOException.class,
                () -> service.loadFromContent(">protein\\nMKWVTF\\n".replace("\\n", "\n"), "protein"));
    }

    @Test
    void fastaHeadersUseAnyWhitespaceAndAmbiguousAliasesAreRejected() throws Exception {
        GeneSequenceService service = new GeneSequenceService();
        service.loadFromContent(">Chr1\tdescription\nAAAA\n>1 another\nCCCC\n", "reference");
        assertEquals("AAAA", service.extractInterval("Chr1", 1, 4, '+'));
        assertEquals("CCCC", service.extractInterval("1", 1, 4, '+'));
        var error = assertThrows(java.io.IOException.class,
                () -> service.extractInterval("chr1", 1, 4, '+'));
        assertTrue(error.getMessage().contains("Ambiguous FASTA"));
    }

    @Test
    void reportsAssemblyChromosomeMismatchInsteadOfReturningWrongSequence() throws Exception {
        String annotation = """
                ##gff-version 3
                chr1\\ttest\\tgene\\t1\\t3\\t.\\t+\\t.\\tID=g1
                """.replace("\\t", "\t");
        GenomeDataService genome = new GenomeDataService();
        genome.loadFromContent(annotation, "test annotation");
        GeneSequenceService sequences = new GeneSequenceService();
        sequences.loadFromContent(">chr2\\nATG\\n".replace("\\n", "\n"), "mismatched assembly");
        assertTrue(assertThrows(java.io.IOException.class,
                () -> sequences.extract(genome.findGene("g1"))).getMessage().contains("no sequence matching"));
    }
}
