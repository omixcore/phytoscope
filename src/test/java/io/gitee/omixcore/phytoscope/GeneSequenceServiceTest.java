package io.gitee.omixcore.phytoscope;

import io.gitee.omixcore.phytoscope.service.GeneSequenceService;
import io.gitee.omixcore.phytoscope.service.GenomeDataService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeneSequenceServiceTest {

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

    @Test
    void rejectsDuplicateFastaIdsAndProteinSequences() {
        GeneSequenceService service = new GeneSequenceService();
        assertThrows(java.io.IOException.class,
                () -> service.loadFromContent(">chr1\\nATGC\\n>chr1\\nATGC\\n".replace("\\n", "\n"), "duplicate"));
        assertThrows(java.io.IOException.class,
                () -> service.loadFromContent(">protein\\nMKWVTF\\n".replace("\\n", "\n"), "protein"));
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
