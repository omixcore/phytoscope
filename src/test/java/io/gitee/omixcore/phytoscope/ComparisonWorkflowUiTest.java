package io.gitee.omixcore.phytoscope;

import io.gitee.omixcore.phytoscope.service.*;
import io.gitee.omixcore.phytoscope.project.ProjectStore;
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
            assertEquals(java.util.List.of("Root", "Rosette_leaf_1", "Rosette_leaf_8",
                    "Cauline_leaf", "Dry_seed", "Pollen"), expression.getTissues());

            // Exercise the real genome-browser tab, not only the comparison view.
            var browserTabs = scene.getRoot().lookupAll(".tab-pane").stream()
                    .filter(javafx.scene.control.TabPane.class::isInstance)
                    .map(javafx.scene.control.TabPane.class::cast)
                    .filter(tabs -> tabs.getTabs().stream().anyMatch(tab -> "Genome browser".equals(tab.getText())))
                    .findFirst().orElseThrow(() -> new AssertionError("Genome browser tab is missing"));
            browserTabs.getSelectionModel().select(browserTabs.getTabs().stream()
                    .filter(tab -> "Genome browser".equals(tab.getText())).findFirst().orElseThrow());
            var search = (javafx.scene.control.TextField) scene.lookup("#gene-search");
            assertNotNull(search, "gene search control must be present in the running UI");
            search.setText("AT1G67090");
            view.applyCss(); view.layout(); view.onViewportResized();

            var renderer = view.getRenderer();
            assertEquals(1, renderer.getRenderedGenes().size(), "the browser should render the selected measured locus");
            assertEquals("AT1G67090", renderer.getRenderedGenes().getFirst().getId());
            double expressionLeft = renderer.plotRight() + 10;
            double lastCellRight = expressionLeft + 5 * 64 + 59;
            assertTrue(expressionLeft > renderer.plotLeft(), "expression heatmap must be placed to the right of gene tracks");
            assertTrue(lastCellRight < renderer.getCanvas().getWidth(), "all six tissue columns must fit inside the canvas");
            var trackImage = renderer.getCanvas().snapshot(null, null);
            var pixels = trackImage.getPixelReader();
            for (int tissue = 0; tissue < expression.getTissues().size(); tissue++) {
                var cell = pixels.getColor((int) (expressionLeft + tissue * 64 + 20),
                        (int) (io.gitee.omixcore.phytoscope.ui.component.GeneTrackRenderer.AXIS_HEIGHT + 15));
                assertTrue(cell.getBlue() > cell.getRed(),
                        "tissue expression cell " + expression.getTissues().get(tissue) + " should be visibly rendered");
            }
            assertTrue(renderer.getRenderedGenes().size() <= 100);
            assertTrue(renderer.contentHeight() < 15000);
            Path out = Path.of("target/validation"); Files.createDirectories(out);
            view.getComparisonPane().export(out.resolve("atlas-comparison.svg"), false);
            view.getComparisonPane().export(out.resolve("atlas-comparison.png"), true);
            ImageIO.write(SwingFXUtils.fromFXImage(scene.snapshot(null), null), "png", out.resolve("atlas-workspace.png").toFile());
            var chromosome = (javafx.scene.control.ComboBox<String>) scene.lookup("#chromosome-selector");
            assertNotNull(chromosome);
            chromosome.getSelectionModel().select("Chr1");
            assertEquals("Chr1", view.getRenderer().getSequenceLabel());

            var selected = view.getComparisonPane().geneIds();
            var settings = new io.gitee.omixcore.phytoscope.comparison.ComparisonFigure.Settings(
                    io.gitee.omixcore.phytoscope.comparison.ComparisonFigure.Alignment.CDS_START, true,
                    io.gitee.omixcore.phytoscope.comparison.ComparisonFigure.Scale.LINEAR, true);
            view.getComparisonPane().restore(selected, settings);
            view.getRenderer().showWindow(3000, 9000);
            Path project = out.resolve("atlas.phytoscope");
            new ProjectStore().save(project, genome, expression, new ProjectStore.ViewState(
                    "Chr1", selected.getFirst(), 3000, 9000, selected, settings.alignment().name(),
                    settings.compressed(), settings.scale().name(), "AT5G38410"));
            view.loadAnnotation(Path.of("src/test/resources/chromosome-validation/rice.gff3"));
            assertTrue(expression.isEmpty()); assertTrue(view.getComparisonPane().geneIds().isEmpty());
            view.openProject(project); assertEquals(selected, view.getComparisonPane().geneIds());
            assertEquals("Chr1", view.getRenderer().getSequenceLabel());
            assertFalse(view.getRenderer().getGenes().isEmpty(), "restored renderer must be rebuilt for saved sequence");
            assertTrue(view.getRenderer().getGenes().stream().allMatch(g -> "Chr1".equals(g.getChromosome())));
            assertEquals(3000, view.getRenderer().getViewStart()); assertEquals(9000, view.getRenderer().getViewEnd());
            assertEquals(4, expression.getImportReport().matchedIds());
            assertEquals(settings, view.getComparisonPane().settings());
            view.closeProjectResources(); return null;
        });
        Platform.runLater(task); task.get(240, TimeUnit.SECONDS);
    }
}
