package io.gitee.omixcore.phytoscope.ui.component;

import io.gitee.omixcore.phytoscope.model.Exon;
import io.gitee.omixcore.phytoscope.model.Gene;
import io.gitee.omixcore.phytoscope.model.GenomicFeature;
import io.gitee.omixcore.phytoscope.model.Transcript;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Renders gene structure tracks onto a JavaFX {@link Canvas}. Per-gene
 * expression values are shown in the detail pane, not on the tracks.
 *
 * <p>Coordinate mapping: the genomic window [viewStart, viewEnd] is linearly
 * mapped onto the plot area [LABEL_AREA_WIDTH, width - RIGHT_PADDING]. This
 * component is stateless with respect to navigation; the view window is owned
 * by {@code GenomeBrowserView}.</p>
 */
public class GeneTrackRenderer {

    // ---- Palette (light theme, following common genome-browser conventions) ----
    static final Color BACKGROUND = Color.web("#FFFFFF");
    static final Color TRACK_ALT = Color.web("#F7F9FC");
    static final Color AXIS_COLOR = Color.web("#5A6B7B");
    static final Color GENE_BAR = Color.web("#7A8FA6");
    static final Color FEATURE_STROKE = Color.web("#2C3E50");
    static final Color CDS_FILL = Color.web("#2E5C8A");
    static final Color UTR_FILL = Color.web("#9BB7D4");
    static final Color INTRON_LINE = Color.web("#7A8FA6");
    static final Color LABEL_COLOR = Color.web("#2C3E50");
    static final Color MUTED_LABEL = Color.web("#8899AA");
    static final Color SELECTED_GLOW = Color.web("#E8833A");
    static final Color GRID_COLOR = Color.web("#E3E9F0");

    public static final double LABEL_AREA_WIDTH = 150;
    public static final double RIGHT_PADDING = 20;
    public static final double AXIS_HEIGHT = 34;
    public static final double ROW_HEIGHT = 62;
    public static final double CANONICAL_ROW_EXTRA = 26;

    private final Canvas canvas;
    private final GraphicsContext gc;

    private int viewStart = 1;
    private int viewEnd = 20000;

    private List<Gene> genes = new ArrayList<>();

    /**
     * Name of the sequence currently shown on the axis. Set from the dataset
     * metadata so nothing about the organism is assumed; never hard-coded.
     */
    private String sequenceLabel = "";

    private Gene selectedGene;
    private Consumer<Gene> onGeneSelected;
    private Gene hoveredGene;

    /** Extent used by the most recent {@link #render} call, in pixels. */
    private double lastRenderWidth = 900;
    private double lastRenderHeight = 600;

    public GeneTrackRenderer() {
        this.canvas = new Canvas(900, 600);
        this.gc = canvas.getGraphicsContext2D();
    }

    public Canvas getCanvas() {
        return canvas;
    }

    public void setGenes(List<Gene> genes) {
        this.genes = genes == null ? List.of() : genes;
        fitToAll();
    }

    public List<Gene> getGenes() {
        return genes;
    }

    /** Bound detailed tracks to avoid allocating a whole-genome-sized GPU canvas. */
    public List<Gene> getRenderedGenes() {
        List<Gene> visible = genes.stream().filter(g -> g.getEnd() >= viewStart && g.getStart() <= viewEnd).toList();
        if (visible.size() <= 100) return visible;
        int selected = visible.indexOf(selectedGene);
        int start = selected < 0 ? 0 : Math.min(Math.max(0, selected - 50), visible.size() - 100);
        return visible.subList(start, start + 100);
    }
    public String renderNotice() {
        long visible = genes.stream().filter(g -> g.getEnd() >= viewStart && g.getStart() <= viewEnd).count();
        return visible > 100 ? "Showing 100 of " + visible + " overlapping loci. Zoom in or select a gene for details." : "";
    }

    public void setOnGeneSelected(Consumer<Gene> callback) {
        this.onGeneSelected = callback;
    }

    public void setSelectedGene(Gene gene) {
        this.selectedGene = gene;
    }

    public Gene getSelectedGene() {
        return selectedGene;
    }

    public void setHoveredGene(Gene gene) {
        this.hoveredGene = gene;
    }

    public Gene getHoveredGene() {
        return hoveredGene;
    }

    // ---- Sequence label ------------------------------------------------------

    /** Sets the sequence name drawn next to the axis, e.g. "Chr1" or "chr01". */
    public void setSequenceLabel(String sequenceLabel) {
        this.sequenceLabel = sequenceLabel == null ? "" : sequenceLabel;
    }

    public String getSequenceLabel() {
        return sequenceLabel;
    }

    /** Width in pixels used by the most recent render pass. */
    public double getLastRenderWidth() {
        return lastRenderWidth;
    }

    /** Height in pixels used by the most recent render pass. */
    public double getLastRenderHeight() {
        return lastRenderHeight;
    }

    // ---- View window --------------------------------------------------------

    public int getViewStart() {
        return viewStart;
    }

    public int getViewEnd() {
        return viewEnd;
    }

    public int getSpan() {
        return Math.max(1, viewEnd - viewStart + 1);
    }

    public void fitToAll() {
        if (genes.isEmpty()) {
            viewStart = 1;
            viewEnd = Math.max(1000, (int) canvas.getWidth());
            return;
        }
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (Gene gene : genes) {
            min = Math.min(min, gene.getStart());
            max = Math.max(max, gene.getEnd());
        }
        int padding = Math.max(200, (max - min) / 20);
        viewStart = Math.max(1, min - padding);
        viewEnd = max + padding;
    }

    /**
     * Frames the given coordinate window, e.g. a whole chromosome.
     *
     * <p>Used when the sequence is shown at its true size: the axis then reads
     * in real megabase units and the genes sit where they belong, instead of
     * being stretched across the window by {@link #fitToAll()}.</p>
     *
     * @param start 1-based inclusive window start
     * @param end   1-based inclusive window end
     */
    public void showWindow(int start, int end) {
        if (end <= start) {
            return;
        }
        viewStart = Math.max(1, start);
        viewEnd = Math.max(viewStart + 1, end);
    }

    /** Zooms about the canvas centre. factor &lt; 1 zooms in. */
    public void zoom(double factor) {
        int center = viewStart + getSpan() / 2;
        int newSpan = clampSpan((int) Math.round(getSpan() * factor));
        viewStart = Math.max(1, center - newSpan / 2);
        viewEnd = viewStart + newSpan - 1;
    }

    private int clampSpan(int span) {
        return Math.max(40, Math.min(500_000_000, span));
    }

    /** Pans the window by a fraction of the current span (positive = rightwards). */
    public void pan(double fractionOfSpan) {
        int delta = (int) Math.round(getSpan() * fractionOfSpan);
        int newStart = viewStart + delta;
        int newEnd = viewEnd + delta;
        if (newStart < 1) {
            newEnd += 1 - newStart;
            newStart = 1;
        }
        viewStart = newStart;
        viewEnd = Math.max(viewEnd, newEnd);
    }

    /** Focuses the window on a gene with the given proportional padding. */
    public void focusOn(Gene gene, double paddingRatio) {
        if (gene == null) {
            return;
        }
        int padding = (int) (gene.length() * paddingRatio);
        viewStart = Math.max(1, gene.getStart() - padding);
        viewEnd = gene.getEnd() + padding;
    }

    /** Sets the window directly (used by drag-panning). */
    public void focusRange(int start, int end) {
        if (end <= start) {
            return;
        }
        viewStart = Math.max(1, start);
        viewEnd = Math.max(viewStart + 1, end);
    }

    // ---- Coordinate conversion ---------------------------------------------

    public double plotLeft() {
        return LABEL_AREA_WIDTH;
    }

    public double plotRight() {
        return canvas.getWidth() - RIGHT_PADDING;
    }

    double toPixel(int genomicPos) {
        double ratio = (double) (genomicPos - viewStart) / getSpan();
        return plotLeft() + ratio * (plotRight() - plotLeft());
    }

    public int pixelToGenomic(double pixelX) {
        double ratio = (pixelX - plotLeft()) / (plotRight() - plotLeft());
        return (int) Math.round(viewStart + ratio * getSpan());
    }

    double rowHeightFor(Gene gene) {
        return gene == selectedGene ? ROW_HEIGHT + CANONICAL_ROW_EXTRA : ROW_HEIGHT;
    }

    /** Total content height, for sizing the scroll container. */
    public double contentHeight() {
        double h = AXIS_HEIGHT;
        for (Gene gene : getRenderedGenes()) {
            h += rowHeightFor(gene);
        }
        return h + 20;
    }

    /** Hit test: returns the gene under the given pixel coordinate, or null. */
    public Gene hitTest(double pixelX, double pixelY) {
        if (pixelY < AXIS_HEIGHT) {
            return null;
        }
        double y = AXIS_HEIGHT;
        for (Gene gene : getRenderedGenes()) {
            double rowH = rowHeightFor(gene);
            if (pixelY >= y && pixelY < y + rowH) {
                double gx1 = toPixel(gene.getStart());
                double gx2 = toPixel(gene.getEnd());
                return pixelX >= gx1 - 4 && pixelX <= gx2 + 4 ? gene : null;
            }
            y += rowH;
            if (y > pixelY) return null;
        }
        return null;
    }

    // ---- Rendering ----------------------------------------------------------

    public void render(double width, double height) {
        if (canvas.getWidth() != width) {
            canvas.setWidth(width);
        }
        if (canvas.getHeight() != height) {
            canvas.setHeight(height);
        }

        // Remember the last rendered extent so exporters can reproduce it exactly
        this.lastRenderWidth = width;
        this.lastRenderHeight = height;

        gc.setFill(BACKGROUND);
        gc.fillRect(0, 0, width, height);

        drawTrackBackgrounds();
        drawCoordinateAxis(width);

        double y = AXIS_HEIGHT;
        for (Gene gene : getRenderedGenes()) {
            double rowH = rowHeightFor(gene);
            drawGeneRow(gene, y, rowH);
            y += rowH;
        }

        drawVerticalGridLines(height);
        if (!renderNotice().isEmpty()) {
            gc.setFill(LABEL_COLOR); gc.setFont(Font.font("Arial", 12));
            gc.fillText(renderNotice(), 12, height - 5);
        }
    }

    private void drawTrackBackgrounds() {
        double y = AXIS_HEIGHT;
        int i = 0;
        for (Gene gene : getRenderedGenes()) {
            double rowH = rowHeightFor(gene);
            if (i % 2 == 1) {
                gc.setFill(TRACK_ALT);
                gc.fillRect(0, y, canvas.getWidth(), rowH);
            }
            if (gene == selectedGene) {
                gc.setFill(SELECTED_GLOW.deriveColor(0, 1, 1, 0.08));
                gc.fillRect(0, y, canvas.getWidth(), rowH);
            }
            y += rowH;
            i++;
        }
    }

    private void drawCoordinateAxis(double width) {
        gc.setFill(TRACK_ALT);
        gc.fillRect(0, 0, width, AXIS_HEIGHT);

        gc.setStroke(GRID_COLOR);
        gc.setLineWidth(1);
        gc.strokeLine(0, AXIS_HEIGHT - 0.5, width, AXIS_HEIGHT - 0.5);

        int step = niceStep(getSpan());
        int first = ((viewStart + step - 1) / step) * step;

        gc.setFont(Font.font("Consolas", 11));
        gc.setTextAlign(TextAlignment.CENTER);
        for (int pos = first; pos <= viewEnd; pos += step) {
            double x = toPixel(pos);
            if (x < plotLeft() - 1 || x > plotRight() + 1) {
                continue;
            }
            gc.setStroke(AXIS_COLOR);
            gc.setLineWidth(1);
            gc.strokeLine(x, AXIS_HEIGHT - 8, x, AXIS_HEIGHT - 1);

            gc.setFill(AXIS_COLOR);
            gc.fillText(formatPosition(pos), x, AXIS_HEIGHT - 11);
        }

        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFont(Font.font("Segoe UI", 11));
        gc.setFill(MUTED_LABEL);
        gc.fillText(axisCaption(), plotLeft(), 13);

        gc.setTextAlign(TextAlignment.RIGHT);
        gc.fillText(zoomLevelLabel(), plotRight(), 13);
        gc.setTextAlign(TextAlignment.LEFT);
    }

    /**
     * Caption drawn above the axis: the active sequence name and the window
     * span. The sequence name comes from the dataset metadata, so any organism
     * or naming scheme displays correctly.
     */
    String axisCaption() {
        String label = sequenceLabel == null || sequenceLabel.isBlank() ? "sequence" : sequenceLabel;
        return label + "  |  span " + formatPosition(getSpan()) + " bp";
    }

    private String zoomLevelLabel() {
        int span = getSpan();
        if (span <= 200) {
            return "Zoom: base level";
        } else if (span <= 5_000) {
            return "Zoom: exon level";
        } else if (span <= 200_000) {
            return "Zoom: gene level";
        }
        return "Zoom: region level";
    }

    private void drawVerticalGridLines(double height) {
        int step = niceStep(getSpan());
        int first = ((viewStart + step - 1) / step) * step;
        gc.setStroke(GRID_COLOR);
        gc.setLineWidth(1);
        for (int pos = first; pos <= viewEnd; pos += step) {
            double x = toPixel(pos);
            if (x < plotLeft() || x > plotRight()) {
                continue;
            }
            gc.strokeLine(x, AXIS_HEIGHT, x, height);
        }
    }

    /** Draws one gene row: label, gene body and transcript detail. */
    private void drawGeneRow(Gene gene, double top, double rowHeight) {
        double centerY = top + rowHeight / 2;

        // Left-hand label
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        gc.setFill(gene == selectedGene ? SELECTED_GLOW : LABEL_COLOR);
        gc.fillText(gene.getDisplayName(), 10, centerY + 4);

        gc.setFont(Font.font("Consolas", 10));
        gc.setFill(MUTED_LABEL);
        gc.fillText(gene.isForwardStrand() ? "\u2192 +" : "\u2190 \u2212", 10, centerY + 18);

        double geneX1 = toPixel(gene.getStart());
        double geneX2 = toPixel(gene.getEnd());
        if (geneX2 - geneX1 < 2) {
            geneX2 = geneX1 + 2;
        }
        boolean expanded = gene == selectedGene;
        double barY = expanded ? top + 14 : centerY - 6;
        double barH = 12;

        gc.setFill(gene == selectedGene ? SELECTED_GLOW : GENE_BAR);
        gc.fillRect(geneX1, barY, geneX2 - geneX1, barH);

        if (expanded) {
            List<Transcript> transcripts = gene.getTranscripts();
            double tY = barY + barH + 10;
            for (int i = 0; i < transcripts.size(); i++) {
                drawTranscript(transcripts.get(i), tY + i * 22);
            }
        } else {
            Transcript canonical = gene.getCanonicalTranscript();
            if (canonical != null) {
                drawTranscriptMini(canonical, centerY - 6, 12);
            }
        }

        // Expression values live in the detail pane, not on the tracks.

        if (gene == selectedGene) {
            gc.setStroke(SELECTED_GLOW);
            gc.setLineWidth(1.5);
            gc.strokeRect(0.5, top + 0.5, canvas.getWidth() - 1, rowHeight - 1);
        }
    }

    private void drawTranscriptMini(Transcript transcript, double y, double h) {
        List<Exon> exons = transcript.getExonsByCoordinate();
        double lineY = y + h / 2;
        gc.setStroke(INTRON_LINE);
        gc.setLineWidth(1.2);
        for (GenomicFeature intron : transcript.computeIntrons()) {
            gc.strokeLine(toPixel(intron.getStart()), lineY, toPixel(intron.getEnd()), lineY);
        }
        for (Exon exon : exons) {
            drawExonBlock(exon, y, h, false);
        }
        drawUnassigned(transcript, y, h);
    }

    private void drawTranscript(Transcript transcript, double y) {
        List<Exon> exons = transcript.getExonsByCoordinate();
        double h = 12;
        double lineY = y + h / 2;

        gc.setStroke(INTRON_LINE);
        gc.setLineWidth(1.4);
        for (GenomicFeature intron : transcript.computeIntrons()) {
            double x1 = toPixel(intron.getStart());
            double x2 = toPixel(intron.getEnd());
            gc.strokeLine(x1, lineY, x2, lineY);
            if (x2 - x1 > 14) {
                double midX = (x1 + x2) / 2;
                double dir = transcript.isForwardStrand() ? 1 : -1;
                gc.strokeLine(midX - 3 * dir, lineY - 3, midX, lineY);
                gc.strokeLine(midX - 3 * dir, lineY + 3, midX, lineY);
            }
        }

        for (Exon exon : exons) {
            drawExonBlock(exon, y, h, true);
        }
        drawUnassigned(transcript, y, h);

        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFont(Font.font("Consolas", 10));
        gc.setFill(MUTED_LABEL);
        String label = transcript.getDisplayName();
        if (transcript.isCanonical()) {
            label += "  \u2605";
        }
        label += String.format("  exons:%d  CDS:%dbp", transcript.getExonCount(), transcript.getCdsLength());
        if (!transcript.getUnassignedSegments().isEmpty()) label += "  * unassigned CDS/UTR";
        gc.fillText(label, plotLeft() + 2, y - 2);
    }

    private void drawUnassigned(Transcript transcript, double y, double height) {
        for (GenomicFeature segment : transcript.getUnassignedSegments()) {
            double x = toPixel(segment.getStart()), w = Math.max(1.5, toPixel(segment.getEnd()) - x);
            gc.setFill(segment.getType() == io.gitee.omixcore.phytoscope.model.FeatureType.CDS ? CDS_FILL : UTR_FILL);
            gc.fillRect(x, y, w, height); gc.setStroke(FEATURE_STROKE); gc.setLineDashes(2,2);
            gc.strokeRect(x, y, w, height); gc.setLineDashes();
        }
    }

    private void drawExonBlock(Exon exon, double y, double h, boolean detailed) {
        double x1 = toPixel(exon.getStart());
        double x2 = toPixel(exon.getEnd());
        if (x2 < x1) {
            return;
        }
        double w = Math.max(1.5, x2 - x1);

        if (!exon.isCoding()) {
            gc.setFill(UTR_FILL);
            gc.fillRect(x1, y, w, h);
            gc.setStroke(FEATURE_STROKE);
            gc.setLineWidth(0.8);
            gc.strokeRect(x1, y, w, h);
            return;
        }

        gc.setFill(UTR_FILL);
        gc.fillRect(x1, y, w, h);

        if (detailed && exon.getCdsSegments().size() == 1 && exon.getUtrSegments().isEmpty()) {
            gc.setFill(CDS_FILL);
            gc.fillRect(x1, y - 1.5, w, h + 3);
        } else {
            double cdsY = y + 1;
            double cdsH = h - 2;
            for (GenomicFeature cds : exon.getCdsSegments()) {
                double cx1 = toPixel(cds.getStart());
                double cx2 = toPixel(cds.getEnd());
                double cw = Math.max(1.5, cx2 - cx1);
                gc.setFill(CDS_FILL);
                gc.fillRect(cx1, cdsY, cw, cdsH);
            }
        }

        gc.setStroke(FEATURE_STROKE);
        gc.setLineWidth(0.8);
        gc.strokeRect(x1, y, w, h);
    }

    /** Chooses a readable tick step of the form 1/2/5 x 10^n. */
    static int niceStep(int span) {
        int target = Math.max(1, span / 8);
        int magnitude = 1;
        while (magnitude * 10 <= target) {
            magnitude *= 10;
        }
        int normalized = target / magnitude;
        int niceMultiplier;
        if (normalized <= 1) {
            niceMultiplier = 1;
        } else if (normalized <= 2) {
            niceMultiplier = 2;
        } else if (normalized <= 5) {
            niceMultiplier = 5;
        } else {
            niceMultiplier = 10;
        }
        return Math.max(1, niceMultiplier * magnitude);
    }

    static String formatPosition(int pos) {
        if (pos < 10_000) {
            return String.valueOf(pos);
        }
        if (pos < 1_000_000) {
            return String.format("%,d", pos);
        }
        return String.format(java.util.Locale.ROOT, "%.1f Mb", pos / 1_000_000.0);
    }
}
