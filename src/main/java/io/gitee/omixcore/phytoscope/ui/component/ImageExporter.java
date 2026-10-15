package io.gitee.omixcore.phytoscope.ui.component;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

/**
 * Exports the genome browser canvas as a PNG image.
 *
 * <p>Uses a JavaFX snapshot of the canvas, which captures exactly what is on
 * screen at the current zoom and pan. The image is scaled by a configurable
 * factor so users can produce higher-resolution output for figures.</p>
 */
public final class ImageExporter {

    /** Default output scale; 2x gives a crisp image suitable for reports. */
    public static final double DEFAULT_SCALE = 2.0;

    private ImageExporter() {
    }

    /**
     * Writes the canvas contents to a PNG file.
     *
     * @param canvas destination canvas
     * @param file   target file (overwritten if it exists)
     * @param scale  output scale factor, 1.0 = pixel-exact
     */
    public static void exportPng(Canvas canvas, File file, double scale) throws IOException {
        double s = scale <= 0 ? 1.0 : scale;

        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.WHITE);
        params.setTransform(javafx.scene.transform.Transform.scale(s, s));

        WritableImage image = canvas.snapshot(params, null);

        File target = ensurePngExtension(file);
        if (!ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", target)) {
            throw new IOException("No PNG writer available in this JRE");
        }
    }

    /** Adds a .png extension when the user did not type one. */
    static File ensurePngExtension(File file) {
        String name = file.getName();
        if (name.toLowerCase().endsWith(".png")) {
            return file;
        }
        return new File(file.getParentFile(), name + ".png");
    }
}
