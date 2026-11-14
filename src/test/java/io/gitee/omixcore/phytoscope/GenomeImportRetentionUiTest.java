package io.gitee.omixcore.phytoscope;

import io.gitee.omixcore.phytoscope.intake.AnnotationMeta;
import io.gitee.omixcore.phytoscope.intake.Assembly;
import io.gitee.omixcore.phytoscope.intake.IntakeFileKind;
import io.gitee.omixcore.phytoscope.intake.IntakeFileResult;
import io.gitee.omixcore.phytoscope.intake.IntakeHealthCheck;
import io.gitee.omixcore.phytoscope.intake.IntakeManifestMeta;
import io.gitee.omixcore.phytoscope.intake.IntakeReport;
import io.gitee.omixcore.phytoscope.project.ProjectStore;
import io.gitee.omixcore.phytoscope.service.ExpressionDataService;
import io.gitee.omixcore.phytoscope.service.GeneSequenceService;
import io.gitee.omixcore.phytoscope.service.GenomeDataService;
import io.gitee.omixcore.phytoscope.ui.MainView;
import javafx.application.Platform;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Headless (Monocle) UI tests for the genome-import retention / restore fixes:
 * D3 (an annotation-only import must not retain the previous project's FASTA)
 * and D5 (reopening a v2 project auto-restores the recorded FASTA when it is
 * still present and byte-identical, and skips it with a warning otherwise).
 */
@EnabledIfSystemProperty(named = "phytoscope.ui.tests", matches = "true")
class GenomeImportRetentionUiTest {

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

    private static ProjectStore.ViewState viewState(String sequence) {
        return new ProjectStore.ViewState(sequence, null, 1, 500, List.of(),
                "GENOMIC_START", false, "LINEAR", "");
    }

    private static ProjectStore.ResearchState researchState() {
        return new ProjectStore.ResearchState(2000, 200, "BED", Map.of(), "", 30.0, "", "", 1, 1, "");
    }

    // ---- D3: an annotation-only import clears the previous project's FASTA --
    @Test
    void annotationOnlyImportClearsPreviousFasta() throws Exception {
        startPlatform();
        MainView view = fx(() -> new MainView(new GenomeDataService(), new ExpressionDataService()));

        // First import a genome WITH a FASTA.
        GeneSequenceService fasta = IntakeHealthCheck.parseFasta(fixture("chr_level.fa"));
        GenomeDataService genomeWithFasta = new GenomeDataService();
        genomeWithFasta.loadFromFile(fixture("chr_level.gff3"));
        fx(() -> { view.applyImportedGenome(fasta, genomeWithFasta, null, List.of(), null); return null; });
        assertTrue(view.getSequenceService().isLoaded(),
                "precondition: the FASTA must be loaded after a genome import");
        assertTrue(view.getSequenceService().getSequenceNames().contains("Chr1"));

        // Now import annotation only (no FASTA): the previous FASTA must be gone.
        GenomeDataService annotationOnly = new GenomeDataService();
        annotationOnly.loadFromFile(fixture("chr_level.gff3"));
        fx(() -> { view.applyImportedGenome(null, annotationOnly, null, null, null); return null; });
        assertFalse(view.getSequenceService().isLoaded(),
                "an annotation-only import must NOT retain the previous project's FASTA (D3)");
    }

    // ---- D5: reopening a v2 project re-binds the recorded FASTA ------------
    @Test
    void reopenRestoresFastaWhenHashMatches(@TempDir Path temp) throws Exception {
        startPlatform();
        var genome = new GenomeDataService();
        genome.loadFromFile(fixture("chr_level.gff3"));
        var expression = new ExpressionDataService();
        expression.loadFromFile(fixture("chr_level.tsv"));
        expression.reconcile(genome);

        Path fastaPath = fixture("chr_level.fa").toAbsolutePath();
        IntakeFileResult fastaResult = new IntakeFileResult(fastaPath.toString(), IntakeFileKind.FASTA,
                IntakeHealthCheck.sha256(fastaPath), Files.size(fastaPath), true, null);
        Assembly assembly = new Assembly("test-assembly", fastaPath.toString(), genome.getChromosomes());
        AnnotationMeta annotationMeta = new AnnotationMeta("GFF3", "v1", "chr_level.gff3");
        IntakeManifestMeta meta = new IntakeManifestMeta(IntakeReport.SPEC_VERSION, assembly,
                annotationMeta, List.of(fastaResult), List.of());

        Path project = temp.resolve("with-fasta.phytoscope");
        new ProjectStore().save(project, genome, expression, viewState("Chr1"), researchState(), meta);

        MainView view = fx(() -> new MainView(new GenomeDataService(), new ExpressionDataService()));
        fx(() -> { view.openProject(project); return null; });
        assertTrue(view.getSequenceService().isLoaded(),
                "reopening a v2 project must re-bind its recorded FASTA (D5)");
        assertTrue(view.getSequenceService().getSequenceNames().contains("Chr1"),
                "the restored FASTA must expose its sequence ids");
        assertTrue(view.getSequenceService().getSequenceNames().contains("Chr3"));
    }

    // ---- D5: a missing / tampered FASTA is skipped with a warning ----------
    @Test
    void reopenSkipsFastaWhenMissingOrTampered(@TempDir Path temp) throws Exception {
        startPlatform();
        var genome = new GenomeDataService();
        genome.loadFromFile(fixture("chr_level.gff3"));
        var expression = new ExpressionDataService();
        expression.loadFromFile(fixture("chr_level.tsv"));
        expression.reconcile(genome);

        // (a) FASTA entry points at a file that no longer exists.
        Path missingPath = temp.resolve("gone.fa").toAbsolutePath();
        IntakeFileResult missingResult = new IntakeFileResult(missingPath.toString(), IntakeFileKind.FASTA,
                "", 0L, true, null);
        Assembly assembly = new Assembly("test-assembly", missingPath.toString(), genome.getChromosomes());
        IntakeManifestMeta missingMeta = new IntakeManifestMeta(IntakeReport.SPEC_VERSION, assembly,
                new AnnotationMeta("GFF3", "v1", "chr_level.gff3"), List.of(missingResult), List.of());
        Path missingProject = temp.resolve("missing-fasta.phytoscope");
        new ProjectStore().save(missingProject, genome, expression, viewState("Chr1"), researchState(), missingMeta);

        MainView view = fx(() -> new MainView(new GenomeDataService(), new ExpressionDataService()));
        fx(() -> { view.openProject(missingProject); return null; });
        assertFalse(view.getSequenceService().isLoaded(),
                "a missing FASTA must be skipped, not silently reused (D5)");

        // (b) FASTA entry records a hash that does not match the current file.
        Path fastaPath = fixture("chr_level.fa").toAbsolutePath();
        IntakeFileResult tamperedResult = new IntakeFileResult(fastaPath.toString(), IntakeFileKind.FASTA,
                "deadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeef", Files.size(fastaPath),
                true, null);
        IntakeManifestMeta tamperedMeta = new IntakeManifestMeta(IntakeReport.SPEC_VERSION, assembly,
                new AnnotationMeta("GFF3", "v1", "chr_level.gff3"), List.of(tamperedResult), List.of());
        Path tamperedProject = temp.resolve("tampered-fasta.phytoscope");
        new ProjectStore().save(tamperedProject, genome, expression, viewState("Chr1"), researchState(), tamperedMeta);

        MainView view2 = fx(() -> new MainView(new GenomeDataService(), new ExpressionDataService()));
        fx(() -> { view2.openProject(tamperedProject); return null; });
        assertFalse(view2.getSequenceService().isLoaded(),
                "a FASTA whose hash changed since save must be skipped (D5)");
    }
}
