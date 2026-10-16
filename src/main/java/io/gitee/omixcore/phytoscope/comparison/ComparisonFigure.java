package io.gitee.omixcore.phytoscope.comparison;

import io.gitee.omixcore.phytoscope.model.*;
import io.gitee.omixcore.phytoscope.service.ExpressionDataService;
import javafx.scene.canvas.Canvas;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import java.io.*;
import java.util.*;

/** Shared drawing primitives for JavaFX preview, PNG and editable SVG. Never infers homology. */
public final class ComparisonFigure {
    public enum Alignment { GENOMIC_START, TRANSCRIPTION_START, CDS_START }
    public enum Scale { LINEAR, LOG1P }
    public record Settings(Alignment alignment, boolean compressed, Scale scale, boolean expression) {
        public static Settings defaults() { return new Settings(Alignment.TRANSCRIPTION_START, false, Scale.LOG1P, true); }
    }
    public record Mark(String kind, double x, double y, double width, double height, String fill, String text) { }
    public record Figure(double width, double height, List<Mark> marks, String table) {
        public void writeSvg(Writer out) throws IOException {
            out.write("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"" + width + "\" height=\"" + height + "\" viewBox=\"0 0 " + width + " " + height + "\">\n");
            out.write("<title>PhytoScope gene structure and expression comparison</title>\n");
            for (Mark m : marks) {
                String xy = " x=\"" + m.x + "\" y=\"" + m.y + "\"";
                switch (m.kind) {
                    case "text" -> out.write("<text" + xy + " fill=\"" + m.fill + "\" font-family=\"Arial,sans-serif\" font-size=\"" + m.height + "\">" + escape(m.text) + "</text>\n");
                    case "rect" -> out.write("<rect" + xy + " width=\"" + m.width + "\" height=\"" + m.height + "\" fill=\"" + m.fill + "\"/>\n");
                    case "line" -> out.write("<line x1=\"" + m.x + "\" y1=\"" + m.y + "\" x2=\"" + (m.x + m.width) + "\" y2=\"" + (m.y + m.height) + "\" stroke=\"" + m.fill + "\" stroke-width=\"1\"/>\n");
                    default -> throw new IllegalStateException(m.kind);
                }
            }
            out.write("</svg>\n");
        }
        public Canvas canvas() {
            if (width > 16000 || height > 16000 || width * height > 32_000_000)
                throw new IllegalArgumentException("Figure exceeds raster preview limits; reduce loci/samples or use the display-free SVG comparison command");
            Canvas canvas = new Canvas(width, height);
            var g = canvas.getGraphicsContext2D();
            for (Mark m : marks) {
                g.setFill(Color.web(m.fill)); g.setStroke(Color.web(m.fill));
                switch (m.kind) {
                    case "text" -> { g.setFont(Font.font("Arial", m.height)); g.fillText(m.text, m.x, m.y); }
                    case "rect" -> g.fillRect(m.x, m.y, m.width, m.height);
                    case "line" -> g.strokeLine(m.x, m.y, m.x + m.width, m.y + m.height);
                    default -> throw new IllegalStateException(m.kind);
                }
            }
            return canvas;
        }
    }
    private record Row(Gene gene, Transcript transcript, Axis axis, boolean first) { }
    /** Piecewise coordinate mapping, shared by all isoforms of a gene. Only union-exon gaps are compressed. */
    public static final class Axis {
        private final Gene gene;
        private final boolean reverse;
        private final List<int[]> gaps;
        private final double anchor;
        public Axis(Gene gene, Settings settings) {
            this.gene = gene;
            reverse = settings.alignment != Alignment.GENOMIC_START && gene.getStrand() == '-';
            List<int[]> intervals = gene.getTranscripts().stream().flatMap(t -> java.util.stream.Stream.concat(t.getExons().stream().map(e -> (GenomicFeature)e), t.getUnassignedSegments().stream()))
                    .map(e -> new int[]{e.getStart(), e.getEnd() + 1}).sorted(Comparator.comparingInt(a -> a[0])).toList();
            gaps = new ArrayList<>();
            if (settings.compressed && !intervals.isEmpty()) {
                int end = intervals.getFirst()[1];
                for (int[] region : intervals) {
                    if (region[0] > end + 80) gaps.add(new int[]{end, region[0]});
                    end = Math.max(end, region[1]);
                }
            }
            int origin = reverse ? gene.getEnd() + 1 : gene.getStart();
            if (settings.alignment == Alignment.CDS_START) {
                var cds = gene.getTranscripts().stream().flatMap(t -> t.getAllCdsSegments().stream()).toList();
                if (cds.isEmpty()) throw new IllegalArgumentException("No CDS for " + gene.getId() + "; choose another alignment");
                origin = reverse ? cds.stream().mapToInt(GenomicFeature::getEnd).max().orElseThrow() + 1
                        : cds.stream().mapToInt(GenomicFeature::getStart).min().orElseThrow();
            }
            anchor = map(origin);
        }
        private double map(int coordinate) {
            double result = coordinate - (double) gene.getStart();
            for (int[] gap : gaps) {
                double traversed = Math.max(0, Math.min(coordinate, gap[1]) - gap[0]);
                result -= traversed * (1 - 80.0 / (gap[1] - gap[0]));
            }
            return reverse ? -result : result;
        }
        public double position(int coordinate) { return map(coordinate) - anchor; }
        public boolean reverse() { return reverse; }
        public List<int[]> gaps() { return gaps.stream().map(int[]::clone).toList(); }
    }

    public Figure build(List<Gene> genes, ExpressionDataService expression, Settings settings) {
        if (genes == null || genes.isEmpty()) throw new IllegalArgumentException("Select at least one gene");
        if (genes.size() > 100) throw new IllegalArgumentException("Compare at most 100 genes per figure");
        List<Row> rows = new ArrayList<>();
        double min = 0, max = 1;
        for (Gene gene : genes) {
            Axis axis = new Axis(gene, settings);
            min = Math.min(min, Math.min(axis.position(gene.getStart()), axis.position(gene.getEnd() + 1)));
            max = Math.max(max, Math.max(axis.position(gene.getStart()), axis.position(gene.getEnd() + 1)));
            boolean first = true;
            if (gene.getTranscripts().isEmpty()) rows.add(new Row(gene, null, axis, true));
            for (Transcript tx : gene.getTranscripts()) { rows.add(new Row(gene, tx, axis, first)); first = false; }
        }
        boolean withExpression = settings.expression && !expression.isEmpty();
        List<String> samples = withExpression ? expression.getTissues() : List.of();
        if (samples.size() > 200 || rows.size() > 2000) throw new IllegalArgumentException("Select at most 200 samples / 2000 transcript rows");
        double exprX = 950, cell = 88, width = Math.max(1100, exprX + cell * samples.size() + 30);
        double header = 160, height = header + rows.size() * 45 + 65;
        List<Mark> marks = new ArrayList<>();
        rect(marks, 0, 0, width, height, "#FFFFFF");
        text(marks, 24, 29, "PhytoScope | Gene structure and expression", 19, "#244762");
        text(marks, 24, 52, "Alignment: " + settings.alignment + " | " + (settings.compressed ? "Compressed introns (nonlinear); gaps capped at 80 display units" : "Shared linear bp scale"), 12, "#536878");
        text(marks, 24, 73, "Exon: pale blue | CDS: blue | UTR: orange | gray: missing expression | coordinates: 1-based inclusive", 12, "#536878");
        String unit = expression.getValueUnit();
        if (withExpression) text(marks, 24, 94, "Expression: " + expression.getLevel() + " level; " + unit + "; shared " + settings.scale + " color scale; values shown in input units", 12, "#536878");
        double denominator = Math.max(1, max - min), scale = 620.0 / denominator;
        double zero = 290 - min * scale;
        line(marks, zero, 135, 0, height - 180, "#C7D4DF");
        text(marks, 290, 128, String.format(Locale.ROOT, "%.0f", min), 10, "#536878");
        text(marks, 850, 128, String.format(Locale.ROOT, "%.0f %s", max, settings.compressed ? "display units" : "bp"), 10, "#536878");
        for (int c = 0; c < samples.size(); c++) {
            String sample = samples.get(c);
            int split = sample.indexOf('_');
            String first = split > 0 ? sample.substring(0, split) : sample;
            String second = split > 0 ? sample.substring(split + 1) : "[" + (c + 1) + "]";
            if (first.length() > 12) { second = first.substring(12); first = first.substring(0, 12); }
            text(marks, exprX + c * cell, 125, first, 11, "#244762");
            text(marks, exprX + c * cell, 143, second.length() > 12 ? second.substring(0, 11) + "…" : second, 10, "#536878");
        }
        double maximum = Math.max(1e-12, expression.globalMaxTpm());
        boolean signed = expression.getProfileIds().stream().map(expression::getRawProfile)
                .flatMap(p -> p.getValues().stream()).anyMatch(v -> v.tpm() < 0);
        if (withExpression) text(marks, exprX, 105, String.format(Locale.ROOT, signed ? "Color endpoints: -%.4g → 0 → %.4g %s" : "Color endpoints: 0 → %.4g %s", signed ? new Object[]{maximum, maximum, unit} : new Object[]{maximum, unit}), 11, "#536878");
        StringBuilder table = new StringBuilder("GeneID\tTranscriptID\tSequence\tStrand\tFeature\tStart\tEnd\tSample\tValue\tUnit\tLevel\n");
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i); Gene gene = row.gene; Transcript tx = row.transcript;
            double y = header + 45 * i;
            if (row.first) addTable(table, gene, null, "gene", gene.getStart(), gene.getEnd(), "", "", unit, expression.getLevel());
            if (tx != null) addTable(table, gene, tx, "transcript", tx.getStart(), tx.getEnd(), "", "", unit, expression.getLevel());
            if (i % 2 == 0) rect(marks, 18, y - 17, width - 36, 43, "#F6F9FC");
            text(marks, 24, y - 1, tx == null ? gene.getId() : tx.getId(), 12, "#244762");
            text(marks, 24, y + 15, gene.getChromosome() + ":" + gene.getStart() + "–" + gene.getEnd() + " (" + gene.getStrand() + ")", 10, "#536878");
            line(marks, zero, y - 17, 0, 43, "#C7D4DF");
            double a = zero + row.axis.position(gene.getStart()) * scale;
            double b = zero + row.axis.position(gene.getEnd() + 1) * scale;
            line(marks, Math.min(a, b), y, Math.abs(b - a), 0, "#8299AA");
            text(marks, Math.max(a, b) - 7, y - 5, row.axis.reverse || gene.getStrand() != '-' ? ">" : "<", 11, "#244762");
            if (tx != null) for (Exon exon : tx.getExonsByCoordinate()) {
                segment(marks, row.axis, zero, scale, y, exon, 14, "#B4D7EA");
                addTable(table, gene, tx, "exon", exon.getStart(), exon.getEnd(), "", "", unit, expression.getLevel());
                for (var cds : exon.getCdsSegments()) {
                    segment(marks, row.axis, zero, scale, y, cds, 20, "#387FA4");
                    addTable(table, gene, tx, "CDS", cds.getStart(), cds.getEnd(), "", "", unit, expression.getLevel());
                }
                for (var utr : exon.getUtrSegments()) {
                    segment(marks, row.axis, zero, scale, y, utr, 12, "#E4A867");
                    addTable(table, gene, tx, utr.getType().name(), utr.getStart(), utr.getEnd(), "", "", unit, expression.getLevel());
                }
            }
            if (tx != null) for (var segment : tx.getUnassignedSegments()) {
                boolean coding = segment.getType() == FeatureType.CDS;
                segment(marks, row.axis, zero, scale, y, segment, coding ? 20 : 12, coding ? "#387FA4" : "#E4A867");
                text(marks, zero + row.axis.position(segment.getStart()) * scale, y - 12, "*", 12, "#8C573A");
                addTable(table, gene, tx, coding ? "CDS" : "UTR", segment.getStart(), segment.getEnd(), "", "", unit, expression.getLevel());
            }
            for (int[] gap : row.axis.gaps()) {
                double x = zero + (row.axis.position(gap[0]) + row.axis.position(gap[1])) / 2 * scale;
                text(marks, x - 4, y + 4, "//", 11, "#244762");
            }
            boolean drawExpression = withExpression && (expression.getLevel().equals("transcript") || row.first);
            ExpressionProfile profile = expression.getProfileForAnnotation(expression.getLevel().equals("transcript") && tx != null ? tx.getId() : gene.getId());
            if (drawExpression) for (int c = 0; c < samples.size(); c++) {
                String sample = samples.get(c);
                double value = profile == null ? Double.NaN : profile.getTpm(sample);
                double fraction = Double.isFinite(value) ? (settings.scale == Scale.LOG1P ? Math.log1p(Math.abs(value)) / Math.log1p(maximum) : Math.abs(value) / maximum) : 0;
                String color = !Double.isFinite(value) ? "#E1E5EA" : color(Math.max(0, Math.min(1, fraction)), value < 0);
                rect(marks, exprX + c * cell, y - 13, cell - 4, 27, color);
                String formatted = Double.isFinite(value) ? String.format(Locale.ROOT, "%.3g", value) : "NA";
                text(marks, exprX + c * cell + 4, y + 4, formatted, 11, fraction > .6 && Double.isFinite(value) ? "#FFFFFF" : "#243B4D");
                addTable(table, gene, expression.getLevel().equals("gene") ? null : tx, "expression", gene.getStart(), gene.getEnd(), sample, Double.isFinite(value) ? Double.toString(value) : "NA", unit, expression.getLevel());
            }
        }
        text(marks, 24, height - 45, "* = source CDS/UTR without an overlapping annotated exon; not counted as an exon.", 11, "#536878");
        text(marks, 24, height - 25, "Selected loci are user-defined; alignment does not establish homology or causation. Full sample labels and source coordinates are in the TSV.", 12, "#536878");
        return new Figure(width, height, List.copyOf(marks), table.toString());
    }
    private static void segment(List<Mark> marks, Axis axis, double zero, double scale, double y, GenomicFeature feature, int h, String color) {
        double a = zero + axis.position(feature.getStart()) * scale, b = zero + axis.position(feature.getEnd() + 1) * scale;
        rect(marks, Math.min(a, b), y - h / 2.0, Math.max(.5, Math.abs(b - a)), h, color);
    }
    private static void addTable(StringBuilder out, Gene g, Transcript t, String feature, int start, int end, String sample, String value, String unit, String level) {
        for (String s : List.of(g.getId(), t == null ? "" : t.getId(), g.getChromosome(), "" + g.getStrand(), feature, "" + start, "" + end, sample, value, unit, level)) {
            out.append('"').append(s.replace("\"", "\"\"")).append('"').append('\t');
        }
        out.setLength(out.length() - 1); out.append('\n');
    }
    private static String color(double fraction, boolean negative) {
        int[] end = negative ? new int[]{171, 70, 47} : new int[]{35, 105, 149};
        return String.format(Locale.ROOT, "#%02x%02x%02x", (int)(239 + fraction * (end[0] - 239)), (int)(247 + fraction * (end[1] - 247)), (int)(251 + fraction * (end[2] - 251)));
    }
    private static void text(List<Mark> m, double x, double y, String text, int size, String color) { m.add(new Mark("text", x, y, 0, size, color, text == null ? "" : text)); }
    private static void rect(List<Mark> m, double x, double y, double w, double h, String color) { m.add(new Mark("rect", x, y, w, h, color, "")); }
    private static void line(List<Mark> m, double x, double y, double w, double h, String color) { m.add(new Mark("line", x, y, w, h, color, "")); }
    private static String escape(String s) { return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;"); }
}
