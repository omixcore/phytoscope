package io.gitee.omixcore.phytoscope.ui.component;

import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.stage.Window;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Background staging with a modal, cancellable progress surface; commits only on the FX thread. */
public final class LoadingOperation {
    private LoadingOperation() { }

    /** Work that receives a progress publisher and may throw like a Callable. */
    @FunctionalInterface
    public interface ProgressWork<T> {
        T apply(Consumer<String> progress) throws Exception;
    }

    public static <T> CompletableFuture<T> run(Window owner, String title, Callable<T> work,
                                              Consumer<T> commit, Consumer<Throwable> failure) {
        return run(owner, title, progress -> work.call(), commit, failure);
    }

    /**
     * Background staging with an optional progress line. The work function
     * receives a consumer whose text is shown on the dialog, so long parses
     * can report how far they have got.
     */
    public static <T> CompletableFuture<T> run(Window owner, String title,
                                              ProgressWork<T> work,
                                              Consumer<T> commit, Consumer<Throwable> failure) {
        if (!Platform.isFxApplicationThread()) throw new IllegalStateException("Start loads on the FX thread");
        Dialog<Void> dialog = new Dialog<>(); if (owner != null) dialog.initOwner(owner);
        dialog.setTitle(title); dialog.setHeaderText(title);
        Label label = new Label("Preparing and validating data. The current analysis is retained until completion.");
        label.setWrapText(true); ProgressIndicator spinner = new ProgressIndicator(); spinner.setPrefSize(44,44);
        VBox body = new VBox(12, spinner, label); body.setPadding(new Insets(15)); body.setPrefWidth(420);
        dialog.getDialogPane().setContent(body); dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        java.util.concurrent.atomic.AtomicBoolean cancelled = new AtomicBoolean(), finished = new AtomicBoolean();
        // Progress text is produced on the worker thread but must touch the label on the FX thread.
        Consumer<String> publish = text -> Platform.runLater(() -> { if (!finished.get()) label.setText(text); });
        java.util.concurrent.atomic.AtomicReference<Thread> thread = new java.util.concurrent.atomic.AtomicReference<>();
        CompletableFuture<T> completion = new CompletableFuture<>() {
            @Override public boolean cancel(boolean interrupt) {
                if (isDone()) return false;
                cancelled.set(true);
                if (interrupt && thread.get() != null) thread.get().interrupt();
                Platform.runLater(dialog::close);
                return super.cancel(interrupt);
            }
        };
        Thread worker = new Thread(() -> {
            try {
                T value = work.apply(publish);
                Platform.runLater(() -> {
                    if (cancelled.get()) { discard(value); return; }
                    finished.set(true); dialog.close();
                    try { commit.accept(value); completion.complete(value); }
                    catch (Throwable ex) { discard(value); failure.accept(ex); completion.completeExceptionally(ex); }
                });
            } catch (Throwable ex) {
                Platform.runLater(() -> {
                    if (cancelled.get()) return;
                    finished.set(true); dialog.close(); failure.accept(ex); completion.completeExceptionally(ex);
                });
            }
        }, "phytoscope-data-loader");
        worker.setDaemon(true); thread.set(worker);
        dialog.setOnCloseRequest(e -> {
            if (finished.get()) return;
            cancelled.set(true); worker.interrupt(); finished.set(true);
            CancellationException ex = new CancellationException("Loading cancelled; the previous analysis was retained");
            failure.accept(ex); completion.completeExceptionally(ex);
        });
        dialog.show(); worker.start(); return completion;
    }
    private static void discard(Object value) {
        if (value instanceof AutoCloseable closeable) try { closeable.close(); } catch (Exception ignored) { }
    }
}
