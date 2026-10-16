package io.gitee.omixcore.phytoscope;
import io.gitee.omixcore.phytoscope.service.GenomeDataService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SharedFeatureTest {
    @Test void mirnaIsATranscriptOnlyWhenItsExplicitParentIsAGene() throws Exception {
        var genome = new GenomeDataService();
        genome.loadFromContent("""
                c	s	gene	1	100	.	+	.	ID=A
                c	s	miRNA_primary_transcript	1	100	.	+	.	ID=A.1;Parent=A
                c	s	miRNA	30	50	.	+	.	ID=mature;Parent=A.1
                c	s	gene	201	300	.	+	.	ID=B
                c	s	miRNA	201	300	.	+	.	ID=B.1;Parent=B
                c	s	exon	201	300	.	+	.	ID=e;Parent=B.1
                """);
        assertEquals(1, genome.findGene("A").getTranscripts().size());
        assertEquals("A.1", genome.findGene("A").getTranscripts().getFirst().getId());
        assertEquals(1, genome.findGene("B").getTranscripts().getFirst().getExonCount());
    }
    @Test void everyExplicitTranscriptParentRetainsSharedExonCdsAndUtr() throws Exception {
        var genome = new GenomeDataService();
        genome.loadFromContent("""
                ##gff-version 3
                c\ts\tgene\t1\t100\t.\t-\t.\tID=A
                c\ts\tmRNA\t1\t100\t.\t-\t.\tID=A.1;Parent=A
                c\ts\tmRNA\t1\t100\t.\t-\t.\tID=A.2;Parent=A
                c\ts\texon\t1\t100\t.\t-\t.\tID=e;Parent=A.1,A.2
                c\ts\tCDS\t1\t80\t.\t-\t0\tID=cds;Parent=A.1,A.2
                c\ts\tfive_prime_UTR\t81\t100\t.\t-\t.\tID=utr;Parent=A.1,A.2
                """);
        for (var tx : genome.findGene("A").getTranscripts()) {
            assertEquals(1, tx.getExonCount()); assertEquals(80, tx.getCdsLength());
            assertEquals(1, tx.getExons().getFirst().getUtrSegments().size());
        }
    }
}
