package io.gitee.omixcore.phytoscope;

import io.gitee.omixcore.phytoscope.service.ExampleDataset;
import io.gitee.omixcore.phytoscope.service.ExpressionDataService;
import io.gitee.omixcore.phytoscope.service.GenomeDataService;
import io.gitee.omixcore.phytoscope.ui.MainView;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** Run with mvn -Pui-tests test; real JavaFX controls using Monocle, no display required. */
@EnabledIfSystemProperty(named = "phytoscope.ui.tests", matches = "true")
class AdaptiveChromosomeUiTest {
    @Test
    @SuppressWarnings("unchecked")
    void datasetDropdownMenuAndGeneSelectionStayInSync() throws Exception {
        var ready = new CountDownLatch(1);
        try {
            Platform.startup(ready::countDown);
        } catch (IllegalStateException alreadyStarted) {
            Platform.runLater(ready::countDown);
        }
        assertTrue(ready.await(20, TimeUnit.SECONDS));
        var task = new FutureTask<Void>(() -> {
            var service = new GenomeDataService();
            var expression = new ExpressionDataService();
            var view = new MainView(service, expression);
            var scene = new Scene(view, 1400, 860);
            view.loadInitialData();
            view.applyCss();
            view.layout();
            var datasetSelector = (ComboBox<ExampleDataset>) scene.lookup("#dataset-selector");
            var selector = (ComboBox<String>) scene.lookup("#chromosome-selector");
            var count = (Label) scene.lookup("#sequence-count");
            var menuBar = (MenuBar) ((VBox) view.getTop()).getChildren().getFirst();
            var menu = menuBar.getMenus().stream().filter(m -> m.getText().equals("Chromosome")).findFirst().orElseThrow();
            for (String id : List.of("soybean-v2", "rice", "maize", "arabidopsis", "mint", "soybean", "soybean-v2")) {
                datasetSelector.getSelectionModel().select(ExampleDataset.byId(id));
                int expected = switch (id) {
                    case "soybean-v2" -> 20;
                    case "rice" -> 12;
                    case "maize" -> 10;
                    case "arabidopsis" -> 5;
                    case "mint" -> 4;
                    default -> 3;
                };
                assertEquals(expected, selector.getItems().size(), id);
                assertEquals(expected + " sequences", count.getText());
                assertEquals(service.getChromosomes(), List.copyOf(selector.getItems()));
                for (String seq : List.copyOf(selector.getItems())) {
                    selector.getSelectionModel().select(seq);
                    assertEquals(seq, service.getActiveSequence());
                    assertFalse(service.getGenesOnActiveSequence().isEmpty());
                }
                // The dynamically rebuilt menu must also update the dropdown.
                menu.getOnShowing().handle(null);
                assertEquals(expected, menu.getItems().size());
                menu.getItems().getFirst().fire();
                assertEquals(selector.getItems().getFirst(), selector.getValue());
                assertEquals(selector.getValue(), service.getActiveSequence());
                if (ExampleDataset.byId(id).expressionResource() == null) {
                    assertTrue(expression.isEmpty(), "Previous species expression must be cleared");
                }
                view.applyCss();
                view.layout();
                assertTrue(count.getWidth() >= count.prefWidth(-1) - 1,
                        "The sequence count must remain readable at the default window size");
                Path output = Path.of("target/validation/ui-" + id + ".png");
                Files.createDirectories(output.getParent());
                ImageIO.write(SwingFXUtils.fromFXImage(scene.snapshot(null), null), "png", output.toFile());
            }
            return null;
        });
        Platform.runLater(task);
        task.get(60, TimeUnit.SECONDS);
    }
}
