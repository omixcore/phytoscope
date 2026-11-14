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

    // ---- D2: a report is stale once the file changes; load is refused -------
    @Test
    void staleReportRefusesLoadAfterFileChange(@TempDir Path temp) throws Exception {
        startPlatform();
        AtomicReference<Captured> captured = new AtomicReference<>();
        ImportGenomeWizard wizard = newWizard(captured);

        Path working = temp.resolve("working.fa");
        Files.copy(fixture("chr_level.fa"), working);
        fx(() -> {
            wizard.setSelectedFiles(working, fixture("chr_level.gff3"), null);
            return null;
        });
        IntakeReport report = fx(() -> wizard.runHealthCheck());
        fx(() -> null); // let the UI-update runLater enable Load
        assertTrue(report.pass(), report.summary());
        assertTrue(fx(() -> wizard.isLoadEnabled()), "Load enabled after a passing check");

        // Tamper with the SAME file path so its SHA-256 no longer matches the
        // recorded report hash.
        String tampered = Files.readString(fixture("chr_level.fa"))
                .replaceFirst("(?m)^>", ">Chr9\n>");
        Files.writeString(working, tampered);
        assertFalse(fx(() -> wizard.triggerLoad()),
                "triggerLoad must refuse when the file changed since the health-check (D2)");
        assertNull(captured.get(), "handler must NOT fire for a tampered/stale selection");

        // Re-pointing at a different file without re-running the check also
        // invalidates the cached report and disables Load.
        fx(() -> {
            wizard.setSelectedFiles(fixture("chr_level.fa"), fixture("chr_level.gff3"), null);
            return null;
        });
        assertFalse(fx(() -> wizard.isLoadEnabled()),
                "Load must be disabled while the report is stale (D2)");
    }

    // ---- D4: a confirmed manual alias re-evaluates and unblocks Load --------
    @Test
    void manualAliasRecomputeUnblocksLoad() throws Exception {
        startPlatform();
        AtomicReference<Captured> captured = new AtomicReference<>();
        ImportGenomeWizard wizard = newWizard(captured);

        fx(() -> {
            wizard.setSelectedFiles(fixture("missing_seqid.fa"), fixture("missing_seqid.gff3"), null);
            return null;
        });
        IntakeReport report = fx(() -> wizard.runHealthCheck());
        fx(() -> null);
        assertFalse(report.pass(), "precondition: the missing-seqid import is blocked");
        assertFalse(fx(() -> wizard.isLoadEnabled()), "Load disabled while there is an ERROR");

        // The missing seqid is 'Chr2'; map it to the FASTA id 'Chr1'.
        fx(() -> {
            wizard.addManualAliasRow("Chr2", "Chr1");
            wizard.recomputeAliases();
            return null;
        });
        IntakeReport recomputed = fx(() -> wizard.getReport());
        assertTrue(recomputed.pass(),
                "a valid manual alias must clear the SEQID error (D4): " + recomputed.summary());
        assertTrue(fx(() -> wizard.isLoadEnabled()),
                "Load must be re-enabled after the manual alias recompute (D4)");

        boolean loaded = fx(() -> wizard.triggerLoad());
        assertTrue(loaded, "triggerLoad must succeed once the alias unblocks the import");
        Captured result = captured.get();
        assertNotNull(result, "handler must receive the parsed services");
        assertTrue(result.aliasMap().stream()
                        .anyMatch(a -> "Chr2".equals(a.from()) && "Chr1".equals(a.to())),
                "the manual alias must be delivered to the handler: " + result.aliasMap());
    }

    // ---- D6c: the TSV report is written with the documented columns --------
    @Test
    void exportWritesTsvReport(@TempDir Path temp) throws Exception {
        startPlatform();
        AtomicReference<Captured> captured = new AtomicReference<>();
        ImportGenomeWizard wizard = newWizard(captured);
        fx(() -> {
            wizard.setSelectedFiles(fixture("missing_seqid.fa"), fixture("missing_seqid.gff3"), null);
            return null;
        });
        fx(() -> wizard.runHealthCheck());
        fx(() -> {
            wizard.exportReportTsv(temp);
            return null;
        });
        Path tsv = temp.resolve("intake-health-report.tsv");
        assertTrue(Files.exists(tsv), "the TSV report must be written");
        List<String> lines = Files.readAllLines(tsv);
        assertFalse(lines.isEmpty(), "the TSV report must have a header line");
        assertEquals("severity\tcode\tfile\tlineOrCoord\tmessage", lines.get(0),
                "the TSV header must match the documented columns exactly");
        String body = String.join("\n", lines.subList(1, lines.size()));
        assertTrue(body.contains("SEQID_NOT_IN_FASTA"),
                "the TSV body must carry the issue code(s): " + body);
    }
}
