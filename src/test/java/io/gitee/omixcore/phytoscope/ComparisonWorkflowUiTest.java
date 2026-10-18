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
            assertTrue(expression.getImportReport().matchedIds() >= 30000);
            assertEquals(4, view.getComparisonPane().geneIds().size());
            assertEquals(17494, expression.getProfile("AT1G67090").getTpm("Rosette_leaf_8"));
            assertEquals(java.util.List.of("Root", "Rosette_leaf_1", "Rosette_leaf_8",
                    "Cauline_leaf", "Dry_seed", "Pollen"), expression.getTissues());
            assertTrue(scene.getRoot().lookupAll(".tab-pane").stream()
                    .filter(javafx.scene.control.TabPane.class::isInstance)
                    .map(javafx.scene.control.TabPane.class::cast)
                    .anyMatch(tabs -> tabs.getTabs().stream().anyMatch(tab -> "Research analysis".equals(tab.getText()))));

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
            var selectedLocus = renderer.getRenderedGenes().getFirst();
            var geneList = scene.getRoot().lookupAll(".list-view").stream()
                    .filter(javafx.scene.control.ListView.class::isInstance)
                    .map(javafx.scene.control.ListView.class::cast)
                    .filter(list -> list.getItems().contains(selectedLocus))
                    .findFirst().orElseThrow(() -> new AssertionError("Genome gene list missing"));
            geneList.getSelectionModel().select(selectedLocus);
            view.applyCss(); view.layout();
            assertEquals("AT1G67090", view.getResearchPane().selectedGeneId(),
                    "the research workspace should follow the selected genome-browser locus");
            var researchTabs = scene.getRoot().lookupAll(".tab-pane").stream()
                    .filter(javafx.scene.control.TabPane.class::isInstance)
                    .map(javafx.scene.control.TabPane.class::cast)
                    .filter(tabs -> tabs.getTabs().stream().anyMatch(tab -> "Research analysis".equals(tab.getText())))
                    .findFirst().orElseThrow();
            researchTabs.getSelectionModel().select(researchTabs.getTabs().stream()
                    .filter(tab -> "Research analysis".equals(tab.getText())).findFirst().orElseThrow());
            var analysisTabs = (javafx.scene.control.TabPane) view.getResearchPane().getCenter();
            analysisTabs.getSelectionModel().select(analysisTabs.getTabs().stream()
                    .filter(tab -> "Intervals".equals(tab.getText())).findFirst().orElseThrow());
            view.applyCss(); view.layout();
            var promoterButton = scene.getRoot().lookupAll(".button").stream()
                    .filter(javafx.scene.control.Button.class::isInstance).map(javafx.scene.control.Button.class::cast)
                    .filter(button -> "Extract promoter".equals(button.getText())).findFirst().orElseThrow();
            promoterButton.fire();
            analysisTabs.getSelectionModel().select(analysisTabs.getTabs().stream()
                    .filter(tab -> "Sequence QC".equals(tab.getText())).findFirst().orElseThrow());
            view.applyCss(); view.layout();
            var promoterOutput = (javafx.scene.control.TextArea) scene.lookup("#research-qc-sequence");
            assertNotNull(promoterOutput);
            assertTrue(promoterOutput.getText().startsWith(">promoter:"), "promoter action should populate the QC sequence input");
            assertEquals(2200, promoterOutput.getText().split("\\R")[1].length(),
                    "the default 2,000 upstream + 200 downstream window should be extracted");

            // Tissue-expression bars must be rendered in the right-hand gene details,
            // not squeezed into the genomic coordinate track.
            var labels = scene.getRoot().lookupAll(".label").stream()
                    .filter(javafx.scene.control.Label.class::isInstance)
                    .map(javafx.scene.control.Label.class::cast)
                    .map(javafx.scene.control.Label::getText).filter(java.util.Objects::nonNull).toList();
            assertTrue(labels.stream().anyMatch(label -> label.startsWith("Expression (TPM)")),
                    "selected measured gene must show the expression bar chart in the details pane");
            for (String tissue : expression.getTissues()) {
                assertTrue(labels.contains(tissue.replace('_', ' ')),
                        "right-hand chart should label sample group " + tissue);
            }
            // Reproduce the user's screenshot target: an ordinary, non-RBCS locus
            // must resolve to real source TPMs and six visible bars in the right pane.
            var ordinaryProfile = expression.getProfile("AT1G01860");
            assertNotNull(ordinaryProfile, "ordinary gene AT1G01860 must be covered by the bundled atlas");
            assertEquals(8.0, ordinaryProfile.getTpm("Root"), 0.0);
            assertEquals(17.0, ordinaryProfile.getTpm("Rosette_leaf_1"), 0.0);
            assertEquals(15.0, ordinaryProfile.getTpm("Rosette_leaf_8"), 0.0);
            assertEquals(16.0, ordinaryProfile.getTpm("Cauline_leaf"), 0.0);
            assertEquals(3.0, ordinaryProfile.getTpm("Dry_seed"), 0.0);
            assertEquals(2.0, ordinaryProfile.getTpm("Pollen"), 0.0);

            search.setText("AT1G01860");
            view.applyCss(); view.layout(); view.onViewportResized();
            var ordinaryLocus = renderer.getRenderedGenes().stream()
                    .filter(gene -> "AT1G01860".equals(gene.getId())).findFirst()
                    .orElseThrow(() -> new AssertionError("Ordinary gene AT1G01860 must be rendered"));

            var ordinaryGeneList = scene.getRoot().lookupAll(".list-view").stream()
                    .filter(javafx.scene.control.ListView.class::isInstance)
                    .map(javafx.scene.control.ListView.class::cast)
                    .filter(list -> list.getItems().contains(ordinaryLocus))
                    .findFirst().orElseThrow(() -> new AssertionError("Ordinary-gene result missing from browser list"));
            ordinaryGeneList.getSelectionModel().select(ordinaryLocus);
            view.applyCss(); view.layout();
            var expressionChart = scene.lookup("#tissue-expression-chart");
            assertNotNull(expressionChart, "ordinary gene should show a chart in the right-hand details pane");
            var bars = expressionChart.lookupAll(".tissue-expression-bar");
            assertEquals(6, bars.size(), "all six measured tissue conditions must render as bars");
            assertTrue(bars.stream().allMatch(node -> node instanceof javafx.scene.layout.Region
                    && ((javafx.scene.layout.Region) node).getPrefHeight() > 0),
                    "all six expression bars must have a positive height");
            var chartLabels = expressionChart.lookupAll(".label").stream()
                    .filter(javafx.scene.control.Label.class::isInstance)
                    .map(javafx.scene.control.Label.class::cast)
                    .map(javafx.scene.control.Label::getText).filter(java.util.Objects::nonNull).toList();
            for (String expected : java.util.List.of("8.00", "17.0", "15.0", "16.0", "3.00", "2.00")) {
                assertTrue(chartLabels.contains(expected), "bar chart must show source TPM value " + expected);
            }
            var chartBounds = expressionChart.localToScene(expressionChart.getBoundsInLocal());
            var genomeBounds = renderer.getCanvas().localToScene(renderer.getCanvas().getBoundsInLocal());
            assertTrue(chartBounds.getMinX() > genomeBounds.getMaxX(),
                    "tissue-expression chart must sit to the right of the genome browser");

            assertEquals(renderer.getCanvas().getWidth() - 18, renderer.plotRight(), 2.0,
                    "genome tracks should retain their width now that expression is in the details pane");
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
            var savedResearch = new ProjectStore.ResearchState(1800, 150, "BED",
                    java.util.Map.of("Root", "belowground", "Leaf", "aboveground"), "rice", 42.0,
                    "P1", "kinase", 8, 94, "research-workbench round trip");
            view.getResearchPane().restore(savedResearch);
            view.saveProject(project);
            view.loadAnnotation(Path.of("src/test/resources/chromosome-validation/rice.gff3"));
            assertTrue(expression.isEmpty()); assertTrue(view.getComparisonPane().geneIds().isEmpty());
            view.openProject(project); assertEquals(selected, view.getComparisonPane().geneIds());
            assertEquals("Chr1", view.getRenderer().getSequenceLabel());
            assertFalse(view.getRenderer().getGenes().isEmpty(), "restored renderer must be rebuilt for saved sequence");
            assertTrue(view.getRenderer().getGenes().stream().allMatch(g -> "Chr1".equals(g.getChromosome())));
            assertEquals(3000, view.getRenderer().getViewStart()); assertEquals(9000, view.getRenderer().getViewEnd());
            assertTrue(expression.getImportReport().matchedIds() >= 30000);
            assertEquals(settings, view.getComparisonPane().settings());
            assertEquals(savedResearch, view.getResearchPane().state(), "research settings, sample groups and last result should survive project restoration");
            view.closeProjectResources(); return null;
        });
        Platform.runLater(task); task.get(240, TimeUnit.SECONDS);
    }
}
