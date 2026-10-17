package io.gitee.omixcore.phytoscope.ui.component;

import io.gitee.omixcore.phytoscope.model.Exon;
import io.gitee.omixcore.phytoscope.model.ExpressionProfile;
import io.gitee.omixcore.phytoscope.model.Gene;
import io.gitee.omixcore.phytoscope.model.TissueExpression;
import io.gitee.omixcore.phytoscope.model.Transcript;
import io.gitee.omixcore.phytoscope.service.GeneSequenceService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.List;

/**
 * Right-hand detail panel: structural statistics, transcript breakdown and
 * tissue expression summary for the selected gene.
 */
public class GeneDetailPane extends ScrollPane {

    private final VBox content = new VBox(10);

    /** Expression profile supplier, injected by the main view. */
    private java.util.function.Function<String, ExpressionProfile> profileLookup = id -> null;

    private double globalMaxTpm = 1.0;

    /** Measurement unit taken from the loaded dataset, e.g. TPM or FPKM. */
    private String valueUnit = "unspecified";
    private List<String> samples = List.of();
    private String expressionLevel = "gene";
    private GeneSequenceService sequenceService;
    public void setSamples(List<String> samples) { this.samples = List.copyOf(samples); }
    public void setExpressionLevel(String level) { expressionLevel = level; }
    public void setSequenceService(GeneSequenceService service) { this.sequenceService = service; }

    public GeneDetailPane() {
        content.setPadding(new Insets(14));
        content.setStyle("-fx-background-color: #FFFFFF;");
        setContent(content);
        setFitToWidth(true);
        setStyle("-fx-background: #FFFFFF; -fx-border-color: transparent;");
        showEmptyState();
    }

    public void setExpressionData(java.util.function.Function<String, ExpressionProfile> lookup,
                                  double globalMaxTpm) {
        this.profileLookup = lookup == null ? id -> null : lookup;
        this.globalMaxTpm = globalMaxTpm <= 0 ? 1.0 : globalMaxTpm;
    }

    /** Sets the measurement unit shown next to expression values. */
    public void setValueUnit(String valueUnit) {
        this.valueUnit = valueUnit == null || valueUnit.isBlank() ? "unspecified" : valueUnit;
    }

    /** Clears the pane back to its empty state, used when no gene is selected. */
    public void clear() {
        showEmptyState();
    }

    private void showEmptyState() {
        content.getChildren().clear();
        Label title = new Label("Gene Details");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        title.setStyle("-fx-text-fill: #2C3E50;");

        Label hint = new Label("Click a gene in the list or on a track to see its\n"
                + "structural statistics, transcripts and tissue expression.");
        hint.setStyle("-fx-text-fill: #8899AA; -fx-font-size: 12px;");
        hint.setWrapText(true);

        content.getChildren().addAll(title, new Separator(), hint);
    }

    public void showGene(Gene gene) {
        content.getChildren().clear();
        if (gene == null) {
            showEmptyState();
            return;
        }

        Label title = new Label(gene.getDisplayName());
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        title.setStyle("-fx-text-fill: #E8833A;");

        content.getChildren().add(title);
        if (gene.getAlias() != null) {
            Label alias = new Label("Aliases: " + gene.getAlias());
            alias.setStyle("-fx-text-fill: #8899AA; -fx-font-size: 11px;");
            alias.setWrapText(true);
            content.getChildren().add(alias);
        }
        content.getChildren().add(new Separator());

        GridPane info = new GridPane();
        info.setHgap(12);
        info.setVgap(7);
        addRow(info, 0, "Gene ID", nullSafe(gene.getId()));
        addRow(info, 1, "Chromosome", nullSafe(gene.getChromosome()));
        addRow(info, 2, "Location", String.format("%,d \u2013 %,d", gene.getStart(), gene.getEnd()));
        addRow(info, 3, "Length", String.format("%,d bp", gene.length()));
        addRow(info, 4, "Strand", gene.isForwardStrand() ? "forward (+)" : "reverse (\u2212)");
        addRow(info, 5, "Transcripts", String.valueOf(gene.getTranscripts().size()));
        addRow(info, 6, "Species", nullSafe(gene.getSpecies()));
        if (gene.getLocusTag() != null) {
            addRow(info, 7, "Locus tag", gene.getLocusTag());
        }
        content.getChildren().add(info);

        if (gene.getDescription() != null && !gene.getDescription().isBlank()) {
            Label descTitle = sectionTitle("Description");
            Text descText = new Text(gene.getDescription());
            descText.setStyle("-fx-fill: #2C3E50; -fx-font-size: 12px;");
            TextFlow flow = new TextFlow(descText);
            flow.setPrefWidth(260);
            content.getChildren().addAll(new Separator(), descTitle, flow);
        }

        // Expression summary belongs in the right-hand details pane, not beside every locus.
        ExpressionProfile profile = expressionLevel.equals("gene") ? profileLookup.apply(gene.getId()) : null;
        content.getChildren().addAll(new Separator(), sectionTitle("Tissue expression"));
        if (profile != null && !profile.isEmpty()) {
            content.getChildren().add(buildExpressionCard(profile));
        } else {
            Label missing = new Label("No measurements for this gene in the loaded expression matrix. NA means unavailable, not zero expression.");
            missing.setWrapText(true);
            missing.setStyle("-fx-text-fill: #778596; -fx-font-size: 11px;");
            content.getChildren().add(missing);
        }

        content.getChildren().addAll(new Separator(), sectionTitle("Transcript Structure"));
        for (Transcript transcript : gene.getTranscripts()) {
            content.getChildren().add(buildTranscriptCard(transcript));
            if (expressionLevel.equals("transcript")) {
                ExpressionProfile tp = profileLookup.apply(transcript.getId());
                if (tp != null) content.getChildren().add(buildExpressionCard(tp));
            }
        }
    }

    /**
     * Vertical column chart for the selected gene. Each sample is scaled against
     * this gene's maximum so low-abundance profiles remain visible.
     */
    private VBox buildExpressionCard(ExpressionProfile profile) {
        VBox card = new VBox(8);
        card.setId("tissue-expression-chart");
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color: #F7F9FC; -fx-background-radius: 6;"
                + " -fx-border-color: #E3E9F0; -fx-border-radius: 6;");

        Label caption = new Label("Expression (" + valueUnit + ")");
        caption.setStyle("-fx-text-fill: #526579; -fx-font-size: 11px; -fx-font-weight: bold;");
        card.getChildren().add(caption);

        List<String> sampleNames = samples.isEmpty()
                ? profile.getValues().stream().map(TissueExpression::tissue).toList() : samples;
        double max = sampleNames.stream().mapToDouble(profile::getTpm)
                .filter(Double::isFinite).map(Math::abs).max().orElse(0.0);
        if (max <= 0) max = 1.0;

        HBox chart = new HBox(4);
        chart.setAlignment(Pos.BOTTOM_CENTER);
        chart.setMinHeight(150);
        chart.setPrefHeight(150);
        chart.setMaxHeight(150);

        for (String sample : sampleNames) {
            double measurement = profile.getTpm(sample);
            VBox column = new VBox(3);
            column.setAlignment(Pos.BOTTOM_CENTER);
            column.setMinWidth(0);
            column.setPrefWidth(38);
            column.setMaxWidth(Double.MAX_VALUE);

            Label value = new Label(Double.isFinite(measurement) ? formatMeasurement(measurement) : "NA");
            value.setFont(Font.font("Consolas", 9));
            value.setStyle("-fx-text-fill: #34495E;");
            value.setTooltip(new javafx.scene.control.Tooltip(sample + ": "
                    + (Double.isFinite(measurement) ? measurement + " " + valueUnit : "not measured")));

            StackPane barArea = new StackPane();
            barArea.setMinHeight(90);
            barArea.setPrefHeight(90);
            barArea.setMaxHeight(90);
            barArea.setMinWidth(25);
            barArea.setAlignment(Pos.BOTTOM_CENTER);
            if (Double.isFinite(measurement)) {
                double fraction = Math.min(1.0, Math.abs(measurement) / max);
                Region bar = new Region();
                bar.getStyleClass().add("tissue-expression-bar");
                bar.setMinWidth(16);
                bar.setPrefWidth(22);
                bar.setMaxWidth(22);
                bar.setMinHeight(measurement == 0 ? 2 : 3);
                bar.setPrefHeight(Math.max(2, fraction * 82));
                bar.setMaxHeight(Math.max(2, fraction * 82));
                bar.setStyle("-fx-background-color: " + (measurement < 0 ? "#B86248" : toHex(barColour(fraction))) + ";"
                        + "-fx-background-radius: 3 3 0 0;");
                StackPane.setAlignment(bar, Pos.BOTTOM_CENTER);
                barArea.getChildren().add(bar);
            } else {
                Label na = new Label("—");
                na.setStyle("-fx-text-fill: #A0AAB5;");
                StackPane.setAlignment(na, Pos.CENTER);
                barArea.getChildren().add(na);
            }

            Label name = new Label(displaySampleName(sample));
            name.setWrapText(true);
            name.setAlignment(Pos.TOP_CENTER);
            name.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
            name.setMinHeight(30);
            name.setPrefWidth(40);
            name.setMaxWidth(42);
            name.setStyle("-fx-text-fill: #526579; -fx-font-size: 9px;");
            name.setTooltip(new javafx.scene.control.Tooltip(sample));
            column.getChildren().addAll(value, barArea, name);
            chart.getChildren().add(column);
            HBox.setHgrow(column, Priority.ALWAYS);
        }
        card.getChildren().add(chart);

        TissueExpression top = profile.topTissue();
        if (top != null) {
            Label peak = new Label(String.format("Peak: %s (%.3g %s)",
                    top.tissue(), top.tpm(), valueUnit));
            peak.setStyle("-fx-text-fill: #E8833A; -fx-font-size: 11px;");
            card.getChildren().add(peak);
        }
        return card;
    }

    private static String formatMeasurement(double value) {
        double abs = Math.abs(value);
        if (abs >= 1000) return String.format(java.util.Locale.ROOT, "%.2g", value);
        if (abs >= 10) return String.format(java.util.Locale.ROOT, "%.1f", value);
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    private static String displaySampleName(String sample) {
        if (sample == null) return "—";
        return sample.replace('_', ' ');
    }

    private static Color barColour(double fraction) {
        // Same sequential ramp as the track heat-map, simplified to three stops
        if (fraction < 0.5) {
            return Color.web("#A8CEE0").interpolate(Color.web("#63A6B8"), fraction * 2);
        }
        return Color.web("#63A6B8").interpolate(Color.web("#D9583B"), (fraction - 0.5) * 2);
    }

    private static String toHex(Color color) {
        return String.format("#%02X%02X%02X",
                (int) Math.round(color.getRed() * 255),
                (int) Math.round(color.getGreen() * 255),
                (int) Math.round(color.getBlue() * 255));
    }

    private VBox buildTranscriptCard(Transcript transcript) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color: #F7F9FC; -fx-background-radius: 6;"
                + " -fx-border-color: #E3E9F0; -fx-border-radius: 6;");

        Label name = new Label(transcript.getDisplayName()
                + (transcript.isCanonical() ? "  \u2605 representative" : ""));
        name.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
        name.setStyle("-fx-text-fill: #2C3E50;");

        GridPane stats = new GridPane();
        stats.setHgap(10);
        stats.setVgap(4);
        addRow(stats, 0, "Exons", transcript.getExonCount() + "");
        addRow(stats, 1, "Introns", transcript.getIntronCount() + "");
        addRow(stats, 2, "Exonic length", transcript.getExonicLength() + " bp");
        addRow(stats, 3, "CDS length", transcript.getCdsLength() + " bp");
        if (transcript.getProteinId() != null) {
            addRow(stats, 4, "Protein ID", transcript.getProteinId());
        }

        card.getChildren().addAll(name, stats);
        if (!transcript.getUnassignedSegments().isEmpty()) {
            Label warning = new Label(transcript.getUnassignedSegments().size() + " CDS/UTR segments lack an overlapping annotated exon. They are retained, not counted as exons.");
            warning.setWrapText(true); warning.setStyle("-fx-text-fill: #986235;"); card.getChildren().add(warning);
        }

        List<Exon> exons = transcript.getExonsByCoordinate();
        if (!exons.isEmpty()) {
            Label exonTitle = new Label("Exon coordinates (genomic order)");
            exonTitle.setStyle("-fx-text-fill: #8899AA; -fx-font-size: 11px;");
            card.getChildren().add(exonTitle);

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < exons.size(); i++) {
                Exon exon = exons.get(i);
                sb.append(String.format("E%d: %,d\u2013%,d (%d bp)", i + 1,
                        exon.getStart(), exon.getEnd(), exon.length()));
                if (exon.isCoding()) {
                    sb.append(" [CDS]");
                }
                if (i < exons.size() - 1) {
                    sb.append("\n");
                }
            }
            Label exonList = new Label(sb.toString());
            exonList.setFont(Font.font("Consolas", 10.5));
            exonList.setStyle("-fx-text-fill: #4A5B6B;");
            card.getChildren().add(exonList);
        }

        VBox.setVgrow(card, Priority.NEVER);
        return card;
    }

    private Label sectionTitle(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        label.setStyle("-fx-text-fill: #2C3E50;");
        return label;
    }

    private void addRow(GridPane grid, int row, String key, String value) {
        Label k = new Label(key);
        k.setStyle("-fx-text-fill: #8899AA; -fx-font-size: 11.5px;");
        Label v = new Label(value);
        v.setStyle("-fx-text-fill: #2C3E50; -fx-font-size: 11.5px;");
        v.setWrapText(true);
        v.setMaxWidth(180);
        grid.add(k, 0, row);
        grid.add(v, 1, row);
    }

    private static String nullSafe(String value) {
        return value == null || value.isBlank() ? "\u2014" : value;
    }
}
