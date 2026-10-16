package io.gitee.omixcore.phytoscope;

import io.gitee.omixcore.phytoscope.service.*;
import io.gitee.omixcore.phytoscope.ui.MainView;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.embed.swing.SwingFXUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import javax.imageio.ImageIO;
import java.nio.file.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named="phytoscope.ui.tests", matches="true")
class ComparisonWorkflowUiTest {
    @Test void realAtlasComparisonProjectRoundTripAndVectorRasterExports() throws Exception {
        CountDownLatch ready = new CountDownLatch(1);
        try { Platform.startup(ready::countDown); } catch (IllegalStateException started) { Platform.runLater(ready::countDown); }
        assertTrue(ready.await(20, TimeUnit.SECONDS));
        FutureTask<Void> task = new FutureTask<>(() -> {
            var genome = new GenomeDataService(); var expression = new ExpressionDataService();
            var view = new MainView(genome, expression); var scene = new Scene(view, 1550, 900);
            view.loadAtlasCase(); view.applyCss(); view.layout();
            assertEquals(4, expression.getImportReport().matchedIds());
            assertEquals(4, view.getComparisonPane().geneIds().size());
            assertEquals(17494, expression.getProfile("AT1G67090").getTpm("Rosette_leaf_8"));
            assertTrue(view.getRenderer().getRenderedGenes().size() <= 100);
            assertTrue(view.getRenderer().contentHeight() < 15000);
            Path out = Path.of("target/validation"); Files.createDirectories(out);
            view.getComparisonPane().export(out.resolve("atlas-comparison.svg"), false);
            view.getComparisonPane().export(out.resolve("atlas-comparison.png"), true);
            ImageIO.write(SwingFXUtils.fromFXImage(scene.snapshot(null), null), "png", out.resolve("atlas-workspace.png").toFile());
            var selected = view.getComparisonPane().geneIds();
            var settings = new io.gitee.omixcore.phytoscope.comparison.ComparisonFigure.Settings(
                    io.gitee.omixcore.phytoscope.comparison.ComparisonFigure.Alignment.CDS_START, true,
                    io.gitee.omixcore.phytoscope.comparison.ComparisonFigure.Scale.LINEAR, true);
            view.getComparisonPane().restore(selected, settings);
            view.getRenderer().showWindow(3000, 9000);
            Path project = out.resolve("atlas.phytoscope"); view.saveProject(project);
            view.loadAnnotation(Path.of("src/test/resources/chromosome-validation/rice.gff3"));
            assertTrue(expression.isEmpty()); assertTrue(view.getComparisonPane().geneIds().isEmpty());
            view.openProject(project); assertEquals(selected, view.getComparisonPane().geneIds());
            assertEquals(3000, view.getRenderer().getViewStart()); assertEquals(9000, view.getRenderer().getViewEnd());
            assertEquals(4, expression.getImportReport().matchedIds());
            assertEquals(settings, view.getComparisonPane().settings());
            view.closeProjectResources(); return null;
        });
        Platform.runLater(task); task.get(240, TimeUnit.SECONDS);
    }
}
