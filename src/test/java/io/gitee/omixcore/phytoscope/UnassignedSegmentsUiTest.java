package io.gitee.omixcore.phytoscope;
import io.gitee.omixcore.phytoscope.service.GenomeDataService;
import io.gitee.omixcore.phytoscope.ui.component.*;
import javafx.application.Platform;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.util.concurrent.*;
import java.io.*;
import static org.junit.jupiter.api.Assertions.*;
@EnabledIfSystemProperty(named="phytoscope.ui.tests",matches="true")
class UnassignedSegmentsUiTest {
    @Test void preservesSourceSegmentsWithoutInventingExonsOrSpliceJunctions() throws Exception {
        CountDownLatch ready = new CountDownLatch(1);
        try { Platform.startup(ready::countDown); } catch (IllegalStateException started) { Platform.runLater(ready::countDown); }
        assertTrue(ready.await(20,TimeUnit.SECONDS));
        FutureTask<Void> task = new FutureTask<>(() -> {
            var genome = new GenomeDataService();
            genome.loadFromContent("""
                    c\ts\tgene\t1\t100\t.\t+\t.\tID=A
                    c\ts\tmRNA\t1\t100\t.\t+\t.\tID=A.1;Parent=A
                    c\ts\texon\t1\t40\t.\t+\t.\tID=e;Parent=A.1
                    c\ts\tCDS\t60\t100\t.\t+\t0\tID=c;Parent=A.1
                    c\ts\tthree_prime_UTR\t41\t59\t.\t+\t.\tID=u;Parent=A.1
                    """);
            var gene=genome.findGene("A"); var tx=gene.getTranscripts().getFirst();
            assertEquals(1,tx.getExonCount()); assertEquals(0,tx.getIntronCount()); assertEquals(40,tx.getExonicLength());
            assertEquals(41,tx.getCdsLength()); assertEquals(2,tx.getUnassignedSegments().size());
            var renderer = new GeneTrackRenderer(); renderer.setGenes(genome.getGenes()); renderer.setSelectedGene(gene); renderer.render(1000,400);
            StringWriter svg=new StringWriter(); new SvgExporter(renderer).export(svg,1000,400);
            assertTrue(svg.toString().contains("unassigned CDS/UTR")); assertTrue(svg.toString().contains("stroke-dasharray"));
            javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(svg.toString().getBytes()));
            return null;
        }); Platform.runLater(task); task.get(30,TimeUnit.SECONDS);
    }
}
