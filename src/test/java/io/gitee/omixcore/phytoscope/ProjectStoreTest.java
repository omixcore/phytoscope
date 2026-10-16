package io.gitee.omixcore.phytoscope;

import io.gitee.omixcore.phytoscope.project.ProjectStore;
import io.gitee.omixcore.phytoscope.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import static org.junit.jupiter.api.Assertions.*;

class ProjectStoreTest {
    @TempDir Path temp;
    @Test void changedSourceCannotBeSavedAsIfItWereTheDisplayedAnnotation() throws Exception {
        Path source = temp.resolve("source.gff3");
        Files.writeString(source, "c\ts\tgene\t1\t90\t.\t+\t.\tID=A\n");
        var genome = new GenomeDataService(); genome.loadFromFile(source);
        Files.writeString(source, "c\ts\tgene\t1\t99\t.\t+\t.\tID=A\n");
        Path output = temp.resolve("changed.phytoscope");
        var state = new ProjectStore.ViewState("c", "A", 1, 100, List.of(), "GENOMIC_START", false, "LINEAR", "");
        assertThrows(IOException.class, () -> new ProjectStore().save(output, genome, new ExpressionDataService(), state));
        assertFalse(Files.exists(output));
    }
    @Test void projectIsPortableAndPreservesSelectionSettingsAndMissingness() throws Exception {
        Path annotation = temp.resolve("original.gff3");
        Files.writeString(annotation, "##gff-version 3\n##sequence-region scaffold_X 1 1000\nscaffold_X\ts\tgene\t21\t99\t.\t-\t.\tID=A\n");
        Path matrix = temp.resolve("matrix.tsv"); Files.writeString(matrix, "# unit=counts\nGene\tRoot\tLeaf\nA\t0\tNA\n");
        var genome = new GenomeDataService(); genome.loadFromFile(annotation);
        var expression = new ExpressionDataService(); expression.loadFromFile(matrix); expression.reconcile(genome);
        var state = new ProjectStore.ViewState("scaffold_X", "A", 1, 200, List.of("A"), "TRANSCRIPTION_START", true, "LOG1P", "A");
        Path project = temp.resolve("saved.phytoscope"); var store = new ProjectStore(); store.save(project, genome, expression, state);
        Files.delete(annotation); Files.delete(matrix);
        try (var restored = store.load(project)) {
            assertEquals(state, restored.view()); assertEquals('-', restored.genome().findGene("A").getStrand());
            assertEquals("counts", restored.expression().getValueUnit());
            assertEquals(0, restored.expression().getProfile("A").getTpm("Root"));
            assertTrue(Double.isNaN(restored.expression().getProfile("A").getTpm("Leaf")));
            assertEquals(1, restored.manifest().importReport().matchedIds());
            Path again = temp.resolve("again.phytoscope"); store.save(again, restored.genome(), restored.expression(), restored.view());
            try (var second = store.load(again)) { assertEquals(1, second.genome().getGeneCount()); }
        }
        Path tampered = temp.resolve("tampered.phytoscope");
        try (var input = new ZipFile(project.toFile()); var zip = new ZipOutputStream(Files.newOutputStream(tampered))) {
            for (var e : Collections.list(input.entries())) {
                zip.putNextEntry(new ZipEntry(e.getName()));
                if (e.getName().startsWith("annotation/")) zip.write("altered annotation".getBytes());
                else try (var in = input.getInputStream(e)) { in.transferTo(zip); }
                zip.closeEntry();
            }
        }
        IOException ex = assertThrows(IOException.class, () -> store.load(tampered));
        assertTrue(ex.getMessage().contains("Checksum mismatch"));
    }
}
