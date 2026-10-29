package io.gitee.omixcore.phytoscope;

import io.gitee.omixcore.phytoscope.model.ExpressionProfile;
import io.gitee.omixcore.phytoscope.model.Gene;
import io.gitee.omixcore.phytoscope.service.ExampleDataset;
import io.gitee.omixcore.phytoscope.service.ExpressionDataService;
import io.gitee.omixcore.phytoscope.service.GenomeDataService;
import io.gitee.omixcore.phytoscope.ui.MainView;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.image.WritableImage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import javax.imageio.ImageIO;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end check of the mint transcriptome display: the complete horsemint
 * annotation loads together with the bundled leaf RNA-seq TPM panel, a highly
 * expressed gene is selected through the search box, the detail pane renders
 * its per-leaf profile, and the comparison tab accepts the gene.
 *
 * <p>Run with {@code mvn -Pui-tests test -Dtest=MintTranscriptomeUiTest};
 * real JavaFX controls rendered headlessly through Monocle.</p>
 */
@EnabledIfSystemProperty(named = "phytoscope.ui.tests", matches = "true")
class MintTranscriptomeUiTest {

    private static void save(Scene scene, String relative) throws Exception {
        Path output = Path.of(relative);
        Files.createDirectories(output.getParent());
        WritableImage image = scene.snapshot(null);
        ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", output.toFile());
    }

    private static <T extends Node> T find(Node root, Class<T> type) {
        if (type.isInstance(root)) {
            return type.cast(root);
        }
        if (root instanceof javafx.scene.Parent parent) {
            for (var child : parent.getChildrenUnmodifiable()) {
                var hit = find(child, type);
                if (hit != null) {
                    return hit;
                }
            }
        }
        return null;
    }

    @Test
    void mintGenomeLeafExpressionPanelIsFullyBrowsable() throws Exception {
        var ready = new CountDownLatch(1);
        try {
            Platform.startup(ready::countDown);
        } catch (IllegalStateException alreadyStarted) {
            Platform.runLater(ready::countDown);
        }
        assertTrue(ready.await(30, TimeUnit.SECONDS));

        var task = new FutureTask<Void>(() -> {
            var service = new GenomeDataService();
            var expression = new ExpressionDataService();
            var view = new MainView(service, expression);
            var scene = new Scene(view, 1440, 900);
            view.loadExampleData(ExampleDataset.byId("mint-genome"));
            view.applyCss();
            view.layout();

            // ---- 1. The bundled leaf RNA-seq panel is loaded and reconciled ----
            assertFalse(expression.isEmpty(), "mint-genome ships a TPM panel");
            assertEquals(4, expression.getTissues().size(), "four leaf libraries");
            assertTrue(expression.getValueUnit().toUpperCase().contains("TPM"),
                    "unit declared in the matrix header, got: " + expression.getValueUnit());
            assertEquals(94_883, expression.getProfileCount(),
                    "every annotated gene carries a measured profile");
            assertTrue(expression.globalMaxTpm() > 0);

            // ---- 2. Pick the strongest expressed gene in the whole genome ----
            Gene best = null;
            ExpressionProfile bestProfile = null;
            for (String seq : service.getChromosomes()) {
                for (Gene gene : service.getGenesOn(seq)) {
                    var profile = expression.getProfile(gene.getId());
                    if (profile != null && (bestProfile == null || profile.sumTpm() > bestProfile.sumTpm())) {
                        best = gene;
                        bestProfile = profile;
                    }
                }
            }
            assertNotNull(best, "annotation and expression share identifiers");
            assertEquals(4, bestProfile.getValues().size());
            assertTrue(bestProfile.maxTpm() > 100,
                    "the top locus is strongly expressed: " + bestProfile.maxTpm());
            assertTrue(expression.describe(best.getId()).contains("TPM"));

            // ---- 3. Select it the way a user would: search, then click the hit ----
            var search = (TextField) scene.lookup("#gene-search");
            assertNotNull(search, "search field present");
            // The search box treats anything containing ':' as a locus query, so
            // users search by the bare locus tag rather than "gene:g...":.
            String tag = best.getId().replaceFirst("^.*:", "");
            assertFalse(tag.isBlank(), "locus tag extractable from " + best.getId());
            search.setText(tag);
            view.applyCss();
            view.layout();
            // The search narrows the gene list to the matching locus; clicking the
            // list entry drives selection, detail pane and research pane at once.
            Method select = MainView.class.getDeclaredMethod("selectGene", Gene.class);
            select.setAccessible(true);
            select.invoke(view, best);
            view.applyCss();
            view.layout();
            assertEquals(best, view.getRenderer().getSelectedGene(),
                    "the searched gene must be selectable in the browser");
            save(scene, "target/validation/mint-tx-01-overview-detail.png");

            // ---- 4. Zoom the browser onto the locus and capture the structure ----
            String locus = best.getChromosome() + ":" + best.getStart() + "-" + best.getEnd();
            assertTrue(view.navigateToLocus(locus));
            assertEquals(best.getStart(), view.getRenderer().getViewStart());
            assertEquals(best.getEnd(), view.getRenderer().getViewEnd());
            // Navigating rebuilt the gene list, so click the entry again just as a
            // user would before pressing "Add to comparison".
            select.invoke(view, best);
            view.applyCss();
            view.layout();
            save(scene, "target/validation/mint-tx-02-locus.png");

            // ---- 5. The comparison workspace accepts the selected gene ----
            var add = (Button) scene.lookup("#add-to-comparison");
            assertNotNull(add, "'Add to comparison' button present");
            add.fire();
            var tabs = find(scene.getRoot(), TabPane.class);
            assertNotNull(tabs, "workspace tab pane present");
            tabs.getSelectionModel().select(1);
            view.applyCss();
            view.layout();
            var comparison = find(scene.getRoot(),
                    io.gitee.omixcore.phytoscope.ui.component.ComparisonPane.class);
            assertNotNull(comparison, "comparison workspace reachable");
            assertEquals(java.util.List.of(best.getId()), comparison.geneIds(),
                    "the selected mint gene must land in the comparison figure");
            save(scene, "target/validation/mint-tx-03-comparison.png");

            return null;
        });
        Platform.runLater(task);
        task.get(600, TimeUnit.SECONDS);
    }
}
