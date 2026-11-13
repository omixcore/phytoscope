package io.gitee.omixcore.phytoscope;

import io.gitee.omixcore.phytoscope.intake.AliasMapping;
import io.gitee.omixcore.phytoscope.intake.IntakeReport;
import io.gitee.omixcore.phytoscope.service.ExpressionDataService;
import io.gitee.omixcore.phytoscope.service.GeneSequenceService;
import io.gitee.omixcore.phytoscope.service.GenomeDataService;
import io.gitee.omixcore.phytoscope.ui.ImportGenomeWizard;
import javafx.application.Platform;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Headless (Monocle) UI tests for the independent "Import your own genome"
 * wizard. They drive the wizard through its programmatic hooks and verify that
 * the health-check gates the "Load project" action and that confirmed services
 * and aliases are handed back to the host handler.
 */
@EnabledIfSystemProperty(named = "phytoscope.ui.tests", matches = "true")
class ImportGenomeWizardUiTest {

    private record Captured(GeneSequenceService fasta, GenomeDataService genome,
                            ExpressionDataService expression, List<AliasMapping> aliasMap,
                            io.gitee.omixcore.phytoscope.intake.IntakeManifestMeta meta) { }

    private static Path fixture(String name) {
        return Path.of("src/test/resources/intake", name);
    }

    private static <T> T fx(Callable<T> call) throws Exception {
        FutureTask<T> task = new FutureTask<>(call);
        Platform.runLater(task);
        return task.get(30, TimeUnit.SECONDS);
    }

    private static void startPlatform() throws Exception {
        CountDownLatch ready = new CountDownLatch(1);
        try {
            Platform.startup(ready::countDown);
        } catch (IllegalStateException started) {
            Platform.runLater(ready::countDown);
        }
        assertTrue(ready.await(20, TimeUnit.SECONDS));
        fx(() -> {
            Platform.setImplicitExit(false);
            return null;
        });
    }

    private ImportGenomeWizard newWizard(AtomicReference<Captured> captured) throws Exception {
        return fx(() -> new ImportGenomeWizard((fasta, genome, expression, aliasMap, meta) ->
                captured.set(new Captured(fasta, genome, expression, aliasMap, meta)), null));
    }

    // ---- happy path: passing report enables Load and delivers services ------
    @Test
    void happyPathEnablesLoadAndDeliversServices() throws Exception {
        startPlatform();
        AtomicReference<Captured> captured = new AtomicReference<>();
        ImportGenomeWizard wizard = newWizard(captured);

        fx(() -> {
            wizard.setSelectedFiles(fixture("chr_level.fa"), fixture("chr_level.gff3"),
                    fixture("chr_level.tsv"));
            return null;
        });
        IntakeReport report = fx(() -> wizard.runHealthCheck());
        fx(() -> null); // let the UI-update runLater apply (enables Load button)
        assertTrue(report.pass(), report.summary());
        assertTrue(fx(() -> wizard.isLoadEnabled()), "Load must be enabled for a passing report");

        boolean loaded = fx(() -> wizard.triggerLoad());
        assertTrue(loaded, "triggerLoad must succeed when Load is enabled");
        Captured result = captured.get();
        assertNotNull(result, "handler must receive the parsed services");
        assertNotNull(result.fasta());
        assertNotNull(result.genome());
        assertNotNull(result.expression());
    }

    // ---- missing seqid: ERROR blocks the Load action -----------------------
    @Test
    void missingSeqidBlocksLoad() throws Exception {
        startPlatform();
        AtomicReference<Captured> captured = new AtomicReference<>();
        ImportGenomeWizard wizard = newWizard(captured);

        fx(() -> {
            wizard.setSelectedFiles(fixture("missing_seqid.fa"), fixture("missing_seqid.gff3"), null);
            return null;
        });
        IntakeReport report = fx(() -> wizard.runHealthCheck());
        fx(() -> null);
        assertFalse(report.pass());
        assertFalse(fx(() -> wizard.isLoadEnabled()), "Load must be disabled when there is an ERROR");
        assertFalse(fx(() -> wizard.triggerLoad()), "triggerLoad must be refused on ERROR");
        assertNull(captured.get(), "handler must NOT be invoked when blocked");
    }

    // ---- alias confirmation is delivered to the handler --------------------
    @Test
    void confirmedAliasesAreDeliveredToHandler() throws Exception {
        startPlatform();
        AtomicReference<Captured> captured = new AtomicReference<>();
        ImportGenomeWizard wizard = newWizard(captured);

        fx(() -> {
            wizard.setSelectedFiles(fixture("alias_mismatch.fa"), fixture("alias_mismatch.gff3"),
                    fixture("alias_mismatch.tsv"));
            return null;
        });
        IntakeReport report = fx(() -> wizard.runHealthCheck());
        fx(() -> null); // populate alias candidates
        assertTrue(report.pass());
        fx(() -> {
            wizard.confirmAllAliases();
            return null;
        });
        boolean loaded = fx(() -> wizard.triggerLoad());
        assertTrue(loaded);
        Captured result = captured.get();
        assertNotNull(result);
        assertTrue(result.aliasMap().stream()
                        .anyMatch(a -> "1".equals(a.from()) && "Chr1".equals(a.to())),
                "confirmed alias '1' -> 'Chr1' must be delivered: " + result.aliasMap());
    }

    // ---- report export writes JSON and Markdown ----------------------------
    @Test
    void exportWritesJsonAndMarkdown(@TempDir Path temp) throws Exception {
        startPlatform();
        AtomicReference<Captured> captured = new AtomicReference<>();
        ImportGenomeWizard wizard = newWizard(captured);
        fx(() -> {
            wizard.setSelectedFiles(fixture("chr_level.fa"), fixture("chr_level.gff3"),
                    fixture("chr_level.tsv"));
            return null;
        });
        fx(() -> wizard.runHealthCheck());
        fx(() -> {
            wizard.exportReport(temp);
            return null;
        });
        assertTrue(Files.exists(temp.resolve("intake-health-report.json")));
        assertTrue(Files.exists(temp.resolve("intake-health-report.md")));
        String json = Files.readString(temp.resolve("intake-health-report.json"));
        assertTrue(json.contains("\"specVersion\""));
    }
}
