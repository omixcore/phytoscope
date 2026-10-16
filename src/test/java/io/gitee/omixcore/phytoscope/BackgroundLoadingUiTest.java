package io.gitee.omixcore.phytoscope;

import io.gitee.omixcore.phytoscope.service.*;
import io.gitee.omixcore.phytoscope.ui.MainView;
import io.gitee.omixcore.phytoscope.ui.component.LoadingOperation;
import javafx.application.Platform;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named="phytoscope.ui.tests", matches="true")
class BackgroundLoadingUiTest {
    private static <T> T fx(Callable<T> call) throws Exception {
        FutureTask<T> task = new FutureTask<>(call); Platform.runLater(task); return task.get(30, TimeUnit.SECONDS);
    }
    @Test void loadingDoesNotBlockFxAndCancellationNeverCommitsStagedData() throws Exception {
        CountDownLatch ready = new CountDownLatch(1);
        try { Platform.startup(ready::countDown); } catch (IllegalStateException started) { Platform.runLater(ready::countDown); }
        assertTrue(ready.await(20, TimeUnit.SECONDS)); fx(() -> { Platform.setImplicitExit(false); return null; });
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1); var workerDone = new CountDownLatch(1);
        AtomicBoolean committed = new AtomicBoolean(); AtomicReference<Throwable> failure = new AtomicReference<>();
        var pending = fx(() -> LoadingOperation.run(null,"Validation loading",() -> {
            entered.countDown(); try { release.await(); return "staged"; } finally { workerDone.countDown(); }
        }, value -> committed.set(true),failure::set));
        assertTrue(entered.await(10,TimeUnit.SECONDS));
        assertEquals("responsive",fx(() -> "responsive"),"The FX event queue must run while data are loading");
        assertTrue(pending.cancel(true)); assertTrue(workerDone.await(10,TimeUnit.SECONDS));
        fx(() -> null); assertFalse(committed.get()); assertTrue(failure.get() instanceof CancellationException);
        var genome = new GenomeDataService(); var expression = new ExpressionDataService();
        MainView view = fx(() -> new MainView(genome,expression));
        var loading = fx(() -> view.startExampleLoad(ExampleDataset.byId("rice"),false));
        loading.get(60,TimeUnit.SECONDS);
        fx(() -> { assertEquals(12,genome.getChromosomes().size()); assertEquals(24,genome.getGeneCount()); return null; });
    }
}
