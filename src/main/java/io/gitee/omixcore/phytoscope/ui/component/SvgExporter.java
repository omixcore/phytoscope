package io.gitee.omixcore.phytoscope.ui.component;

import io.gitee.omixcore.phytoscope.model.Exon;
import io.gitee.omixcore.phytoscope.model.Gene;
import io.gitee.omixcore.phytoscope.model.GenomicFeature;
import io.gitee.omixcore.phytoscope.model.Transcript;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Locale;

/**
 * Exports the current genome browser view as a vector SVG document.
 *
 * <p>JavaFX has no built-in SVG writer, so this class re-emits the same
 * geometry that {@link GeneTrackRenderer} draws on the canvas, using the
 * renderer's own coordinate mapping and colour rules. Keeping both paths fed
 * from a single source of layout constants guarantees that the exported SVG
 * matches what the user sees on screen.</p>
 *
 * <p>Text is rendered with generic font families so the file opens correctly
 * on machines without the original desktop fonts.</p>
 */
public class SvgExporter {

    private final GeneTrackRenderer renderer;

    public SvgExporter(GeneTrackRenderer renderer) {
        this.renderer = renderer;
    }

    /**
     * Writes the current view to the supplied writer.
     *
     * @param out    destination writer (the caller closes it)
     * @param width  canvas width in pixels
     * @param height canvas height in pixels
     */
    public void export(Writer out, double width, double height) throws IOException {
        StringBuilder sb = new StringBuilder(64 * 1024);

        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append(String.format(Locale.ROOT,
                "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"%.0f\" height=\"%.0f\" "
                        + "viewBox=\"0 0 %.0f %.0f\">\n", width, height, width, height));
        sb.append("  <defs>\n");
        sb.append("    <style>\n");
        sb.append("      .lbl { font-family: 'Segoe UI', Arial, sans-serif; }\n");
        sb.append("      .mono { font-family: Consolas, 'Courier New', monospace; }\n");
        sb.append("    </style>\n");
        sb.append("  </defs>\n");

        // Background
        sb.append(String.format(Locale.ROOT,
                "  <rect x=\"0\" y=\"0\" width=\"%.0f\" height=\"%.0f\" fill=\"%s\"/>\n",
                width, height, hex(GeneTrackRenderer.BACKGROUND)));

        appendTrackBackgrounds(sb);
        appendCoordinateAxis(sb, width);

        double y = GeneTrackRenderer.AXIS_HEIGHT;
        for (Gene gene : renderer.getGenes()) {
            double rowH = renderer.rowHeightFor(gene);
            appendGeneRow(sb, gene, y, rowH);
            y += rowH;
        }

        appendVerticalGridLines(sb, height);

        sb.append("</svg>\n");
        out.write(sb.toString());
    }

    // ------------------------------------------------------------------
    // Sections
    // ------------------------------------------------------------------

    private void appendTrackBackgrounds(StringBuilder sb) {
        double y = GeneTrackRenderer.AXIS_HEIGHT;
        int i = 0;
        for (Gene gene : renderer.getGenes()) {
            double rowH = renderer.rowHeightFor(gene);
            if (i % 2 == 1) {
                sb.append(rect(0, y, renderer.getCanvas().getWidth(), rowH,
                        GeneTrackRenderer.TRACK_ALT, 1.0));
            }
            if (gene == renderer.getSelectedGene()) {
                sb.append(rect(0, y, renderer.getCanvas().getWidth(), rowH,
                        GeneTrackRenderer.SELECTED_GLOW.deriveColor(0, 1, 1, 0.08), 1.0));
            }
            y += rowH;
            i++;
        }
    }

    private void appendCoordinateAxis(StringBuilder sb, double width) {
        sb.append(rect(0, 0, width, GeneTrackRenderer.AXIS_HEIGHT,
                GeneTrackRenderer.TRACK_ALT, 1.0));
        sb.append(line(0, GeneTrackRenderer.AXIS_HEIGHT - 0.5, width,
                GeneTrackRenderer.AXIS_HEIGHT - 0.5, GeneTrackRenderer.GRID_COLOR, 1));

        int span = renderer.getViewEnd() - renderer.getViewStart() + 1;
        int step = GeneTrackRenderer.niceStep(span);
        int first = ((renderer.getViewStart() + step - 1) / step) * step;

        for (int pos = first; pos <= renderer.getViewEnd(); pos += step) {
            double x = renderer.toPixel(pos);
            if (x < renderer.plotLeft() - 1 || x > renderer.plotRight() + 1) {
                continue;
            }
            sb.append(line(x, GeneTrackRenderer.AXIS_HEIGHT - 8, x,
                    GeneTrackRenderer.AXIS_HEIGHT - 1, GeneTrackRenderer.AXIS_COLOR, 1));
            sb.append(text(GeneTrackRenderer.formatPosition(pos), x,
                    GeneTrackRenderer.AXIS_HEIGHT - 11,
                    GeneTrackRenderer.AXIS_COLOR, 11, "mono", "middle"));
        }

        sb.append(text(renderer.axisCaption(),
                renderer.plotLeft(), 13, GeneTrackRenderer.MUTED_LABEL, 11, "lbl", "start"));

        String zoom = "Zoom: " + zoomWord(span);
        sb.append(text(zoom, renderer.plotRight(), 13,
                GeneTrackRenderer.MUTED_LABEL, 11, "lbl", "end"));
    }

    private static String zoomWord(int span) {
        if (span <= 200) {
            return "base level";
        } else if (span <= 5_000) {
            return "exon level";
        } else if (span <= 200_000) {
            return "gene level";
        }
        return "region level";
    }

    private void appendVerticalGridLines(StringBuilder sb, double height) {
        int span = renderer.getViewEnd() - renderer.getViewStart() + 1;
        int step = GeneTrackRenderer.niceStep(span);
        int first = ((renderer.getViewStart() + step - 1) / step) * step;
        for (int pos = first; pos <= renderer.getViewEnd(); pos += step) {
            double x = renderer.toPixel(pos);
            if (x < renderer.plotLeft() || x > renderer.plotRight()) {
                continue;
            }
            sb.append(line(x, GeneTrackRenderer.AXIS_HEIGHT, x, height,
                    GeneTrackRenderer.GRID_COLOR, 1));
        }
    }

    private void appendGeneRow(StringBuilder sb, Gene gene, double top, double rowHeight) {
        double centerY = top + rowHeight / 2;
        boolean expanded = gene == renderer.getSelectedGene();

        // Gene label and strand
        Color labelColour = expanded ? GeneTrackRenderer.SELECTED_GLOW : GeneTrackRenderer.LABEL_COLOR;
        sb.append(text(gene.getDisplayName(), 10, centerY + 4, labelColour, 12, "lbl", "start", true));
        sb.append(text(gene.isForwardStrand() ? "\u2192 +" : "\u2190 \u2212",
                10, centerY + 18, GeneTrackRenderer.MUTED_LABEL, 10, "mono", "start"));

        // Gene body bar
        double geneX1 = renderer.toPixel(gene.getStart());
        double geneX2 = renderer.toPixel(gene.getEnd());
        if (geneX2 - geneX1 < 2) {
            geneX2 = geneX1 + 2;
        }
        double barY = expanded ? top + 14 : centerY - 6;
        sb.append(rect(geneX1, barY, geneX2 - geneX1, 12,
                expanded ? GeneTrackRenderer.SELECTED_GLOW : GeneTrackRenderer.GENE_BAR, 1.0));

        if (expanded) {
            List<Transcript> transcripts = gene.getTranscripts();
            double tY = barY + 12 + 10;
            for (int i = 0; i < transcripts.size(); i++) {
                appendTranscript(sb, transcripts.get(i), tY + i * 22);
            }
        } else {
            Transcript canonical = gene.getCanonicalTranscript();
            if (canonical != null) {
                appendTranscriptMini(sb, canonical, centerY - 6, 12);
            }
        }

        if (expanded) {
            sb.append(String.format(Locale.ROOT,
                    "  <rect x=\"0.5\" y=\"%.1f\" width=\"%.1f\" height=\"%.1f\" "
                            + "fill=\"none\" stroke=\"%s\" stroke-width=\"1.5\"/>\n",
                    top + 0.5, renderer.getCanvas().getWidth() - 1, rowHeight - 1,
                    hex(GeneTrackRenderer.SELECTED_GLOW)));
        }
    }

    private void appendTranscriptMini(StringBuilder sb, Transcript transcript, double y, double h) {
        List<Exon> exons = transcript.getExonsByCoordinate();
        if (exons.isEmpty()) {
            return;
        }
        double lineY = y + h / 2;
        for (GenomicFeature intron : transcript.computeIntrons()) {
            sb.append(line(renderer.toPixel(intron.getStart()), lineY,
                    renderer.toPixel(intron.getEnd()), lineY,
                    GeneTrackRenderer.INTRON_LINE, 1.2));
        }
        for (Exon exon : exons) {
            appendExonBlock(sb, exon, y, h, false);
        }
    }

    private void appendTranscript(StringBuilder sb, Transcript transcript, double y) {
        List<Exon> exons = transcript.getExonsByCoordinate();
        if (exons.isEmpty()) {
            return;
        }
        double h = 12;
        double lineY = y + h / 2;

        for (GenomicFeature intron : transcript.computeIntrons()) {
            double x1 = renderer.toPixel(intron.getStart());
            double x2 = renderer.toPixel(intron.getEnd());
            sb.append(line(x1, lineY, x2, lineY, GeneTrackRenderer.INTRON_LINE, 1.4));
            if (x2 - x1 > 14) {
                double midX = (x1 + x2) / 2;
                double dir = transcript.isForwardStrand() ? 1 : -1;
                sb.append(line(midX - 3 * dir, lineY - 3, midX, lineY,
                        GeneTrackRenderer.INTRON_LINE, 1.4));
                sb.append(line(midX - 3 * dir, lineY + 3, midX, lineY,
                        GeneTrackRenderer.INTRON_LINE, 1.4));
            }
        }

        for (Exon exon : exons) {
            appendExonBlock(sb, exon, y, h, true);
        }

        String label = transcript.getDisplayName();
        if (transcript.isCanonical()) {
            label += "  \u2605";
        }
        label += String.format(Locale.ROOT, "  exons:%d  CDS:%dbp",
                transcript.getExonCount(), transcript.getCdsLength());
        sb.append(text(label, renderer.plotLeft() + 2, y - 2,
                GeneTrackRenderer.MUTED_LABEL, 10, "mono", "start"));
    }

    private void appendExonBlock(StringBuilder sb, Exon exon, double y, double h, boolean detailed) {
        double x1 = renderer.toPixel(exon.getStart());
        double x2 = renderer.toPixel(exon.getEnd());
        if (x2 < x1) {
            return;
        }
        double w = Math.max(1.5, x2 - x1);

        if (!exon.isCoding()) {
            sb.append(rect(x1, y, w, h, GeneTrackRenderer.UTR_FILL, 1.0));
            sb.append(strokeRect(x1, y, w, h, GeneTrackRenderer.FEATURE_STROKE, 0.8));
            return;
        }

        sb.append(rect(x1, y, w, h, GeneTrackRenderer.UTR_FILL, 1.0));

        if (detailed && exon.getCdsSegments().size() == 1 && exon.getUtrSegments().isEmpty()) {
            sb.append(rect(x1, y - 1.5, w, h + 3, GeneTrackRenderer.CDS_FILL, 1.0));
        } else {
            for (GenomicFeature cds : exon.getCdsSegments()) {
                double cx1 = renderer.toPixel(cds.getStart());
                double cx2 = renderer.toPixel(cds.getEnd());
                double cw = Math.max(1.5, cx2 - cx1);
                sb.append(rect(cx1, y + 1, cw, h - 2, GeneTrackRenderer.CDS_FILL, 1.0));
            }
        }
        sb.append(strokeRect(x1, y, w, h, GeneTrackRenderer.FEATURE_STROKE, 0.8));
    }

    // ------------------------------------------------------------------
    // SVG primitives
    // ------------------------------------------------------------------

    private static String rect(double x, double y, double w, double h, Color fill, double opacity) {
        return String.format(Locale.ROOT,
                "  <rect x=\"%.2f\" y=\"%.2f\" width=\"%.2f\" height=\"%.2f\" fill=\"%s\"%s/>\n",
                x, y, w, h, hex(fill), opacity < 1.0
                        ? String.format(Locale.ROOT, " fill-opacity=\"%.2f\"", opacity) : "");
    }

    private static String strokeRect(double x, double y, double w, double h, Color stroke, double strokeWidth) {
        return String.format(Locale.ROOT,
                "  <rect x=\"%.2f\" y=\"%.2f\" width=\"%.2f\" height=\"%.2f\" "
                        + "fill=\"none\" stroke=\"%s\" stroke-width=\"%.2f\"/>\n",
                x, y, w, h, hex(stroke), strokeWidth);
    }

    private static String line(double x1, double y1, double x2, double y2, Color stroke, double strokeWidth) {
        return String.format(Locale.ROOT,
                "  <line x1=\"%.2f\" y1=\"%.2f\" x2=\"%.2f\" y2=\"%.2f\" "
                        + "stroke=\"%s\" stroke-width=\"%.2f\" stroke-linecap=\"round\"/>\n",
                x1, y1, x2, y2, hex(stroke), strokeWidth);
    }

    private static String text(String content, double x, double y, Color fill, double size,
                               String cssClass, String anchor) {
        return text(content, x, y, fill, size, cssClass, anchor, false);
    }

    private static String text(String content, double x, double y, Color fill, double size,
                               String cssClass, String anchor, boolean bold) {
        return String.format(Locale.ROOT,
                "  <text x=\"%.2f\" y=\"%.2f\" fill=\"%s\" font-size=\"%.1f\" "
                        + "class=\"%s\" text-anchor=\"%s\"%s>%s</text>\n",
                x, y, hex(fill), size, cssClass, anchor,
                bold ? " font-weight=\"bold\"" : "", escape(content));
    }

    // ------------------------------------------------------------------
    // Colour helpers
    // ------------------------------------------------------------------

    /** Converts a JavaFX colour to a #RRGGBB string, or rgba() when transparent. */
    public static String hex(Color c) {
        if (c.getOpacity() < 1.0) {
            return withAlpha(c, c.getOpacity());
        }
        return String.format("#%02X%02X%02X",
                (int) Math.round(c.getRed() * 255),
                (int) Math.round(c.getGreen() * 255),
                (int) Math.round(c.getBlue() * 255));
    }

    /** An rgba() string with the given alpha applied. */
    public static String withAlpha(Color c, double alpha) {
        return String.format(Locale.ROOT, "rgba(%d,%d,%d,%.2f)",
                (int) Math.round(c.getRed() * 255),
                (int) Math.round(c.getGreen() * 255),
                (int) Math.round(c.getBlue() * 255), alpha);
    }

    /** Escapes the five XML predefined entities. */
    public static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
