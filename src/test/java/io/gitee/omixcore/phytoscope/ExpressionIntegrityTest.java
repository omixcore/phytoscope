package io.gitee.omixcore.phytoscope;

import io.gitee.omixcore.phytoscope.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ExpressionIntegrityTest {
    @TempDir Path temp;
    private ExpressionDataService load(String text) throws Exception {
        Path p = temp.resolve("expression.tsv"); Files.writeString(p, text);
        var service = new ExpressionDataService(); service.loadFromFile(p); return service;
    }
    @Test void invalidAndMissingAreReportedWithoutConvertingThemToZero() throws Exception {
        var s = load("# unit=TPM\nGene\tRoot\tLeaf\tSeed\tPollen\tOther\na\t0\tNA\tbad\tNaN\t-1\n");
        assertEquals(1, s.getImportReport().missingCells()); assertEquals(3, s.getImportReport().invalidCells());
        assertEquals(0, s.getProfile("a").getTpm("Root"));
        assertTrue(Double.isNaN(s.getProfile("a").getTpm("Leaf")));
        assertTrue(Double.isNaN(s.getProfile("a").getTpm("Seed")));
    }
    @Test void headersAndDuplicatesCannotMisalignOrOverwriteData() throws Exception {
        var s = load("Gene\tRoot\na\t2\n"); Path p = temp.resolve("bad.tsv");
        for (String bad : List.of("Gene\t\tLeaf\na\t1\t2\n", "Gene\tRoot\tRoot\na\t1\t2\n", "Gene\tRoot\na\t1\na\t3\n", "Gene\tRoot\na\t1\t2\n")) {
            Files.writeString(p, bad); assertThrows(IOException.class, () -> s.loadFromFile(p));
            assertEquals(2, s.getProfile("a").getTpm("Root"));
        }
    }
    @Test void quotedCsvAndSignedDeclaredUnitsWork() throws Exception {
        var s = load("# unit=log2 ratio\nGene,\"Root, stressed\",Leaf\n\"gene\"\"A\",-2,0\n");
        assertEquals("log2 ratio", s.getValueUnit()); assertEquals(-2, s.getProfile("gene\"A").getTpm("Root, stressed"));
    }
    @Test void mappingUsesAnnotationIdsWithoutGuessingVersionsOrAggregatingTranscripts() throws Exception {
        GenomeDataService genome = new GenomeDataService();
        genome.loadFromContent("##gff-version 3\nc\ts\tgene\t1\t50\t.\t+\t.\tID=gene:A\nc\ts\tmRNA\t1\t50\t.\t+\t.\tID=transcript:A.1;Parent=gene:A\n");
        var gene = load("Gene\tRoot\nA\t4\nA.9\t7\n"); var report = gene.reconcile(genome);
        assertEquals(1, report.matchedIds()); assertEquals(List.of("A.9"), report.unmatchedIds());
        assertEquals(4, gene.getProfile("gene:A").getTpm("Root"));
        var conflict = load("Gene\tRoot\nA\t4\ngene:A\t8\n");
        assertEquals(0, conflict.reconcile(genome).matchedIds());
        assertNull(conflict.getProfileForAnnotation("gene:A"));
        assertEquals(2, conflict.getImportReport().unmatchedIds().size());
        var tx = load("# level=transcript\nID\tRoot\nA.1\t6\n"); tx.reconcile(genome);
        assertNull(tx.getProfile("gene:A")); assertEquals(6, tx.getProfile("transcript:A.1").getTpm("Root"));
    }
}
