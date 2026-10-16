package io.gitee.omixcore.phytoscope;

import io.gitee.omixcore.phytoscope.cli.BatchRenderer;
import javafx.application.Application;

import java.util.List;

/**
 * Command-line entry point.
 *
 * <p>Does not extend {@link Application}, avoiding the "JavaFX runtime
 * components are missing" error in some launch modes.</p>
 *
 * <p>Two modes are available from the same jar:</p>
 * <pre>
 *   # interactive desktop application
 *   java -jar target/phytoscope-x.y.z.jar
 *   mvn javafx:run
 *
 *   # headless batch rendering, for reproducible figures
 *   java -jar target/phytoscope-x.y.z.jar --example maize --loci Zm00001eb000010 --out figures
 * </pre>
 *
 * <p>Batch mode is selected automatically as soon as a rendering option is
 * present, so the GUI never starts on a display-less machine.</p>
 */
public final class PhytoScopeLauncher {

    private PhytoScopeLauncher() {
    }

    public static void main(String[] args) {
        if (java.util.Arrays.asList(args).contains("--compare")) {
            try { io.gitee.omixcore.phytoscope.cli.ComparisonRunner.run(args); }
            catch (Exception ex) { System.err.println("error: " + ex.getMessage()); System.exit(3); }
            return;
        }
        if (java.util.Arrays.asList(args).contains("--benchmark")) {
            try { io.gitee.omixcore.phytoscope.cli.BenchmarkRunner.run(args); }
            catch (Exception ex) { System.err.println("error: " + ex.getMessage()); System.exit(3); }
            return;
        }
        if (isBatchRequest(args)) {
            BatchRenderer.main(args);
            return;
        }
        Application.launch(PhytoScopeApplication.class, args);
    }

    /**
     * True when the arguments ask for batch rendering rather than the GUI.
     * {@code --help} alone is treated as a batch request so that usage can be
     * printed on a headless machine.
     */
    static boolean isBatchRequest(String[] args) {
        List<String> batchFlags = List.of(
                "--gff3", "--annotation", "--expression", "--example",
                "--loci", "--out", "--outdir", "--format", "--width", "--height",
                "--padding", "--no-expression", "-h", "--help");
        for (String arg : args) {
            if (batchFlags.contains(arg)) {
                return true;
            }
        }
        return false;
    }
}
