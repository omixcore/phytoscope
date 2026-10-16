package io.gitee.omixcore.phytoscope.ui.component;

import io.gitee.omixcore.phytoscope.comparison.ComparisonFigure;
import io.gitee.omixcore.phytoscope.model.Gene;
import io.gitee.omixcore.phytoscope.service.ExpressionDataService;
import io.gitee.omixcore.phytoscope.service.GenomeDataService;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Explicitly pinned loci, independent of browser chromosome and search results. */
public final class ComparisonPane extends BorderPane {
    private final GenomeDataService genome;
    private final ExpressionDataService expression;
    private final List<String> ids = new ArrayList<>();
    private final ComboBox<ComparisonFigure.Alignment> alignment = new ComboBox<>(FXCollections.observableArrayList(ComparisonFigure.Alignment.values()));
    private final ComboBox<ComparisonFigure.Scale> scale = new ComboBox<>(FXCollections.observableArrayList(ComparisonFigure.Scale.values()));
    private final CheckBox compressed = new CheckBox("Compress introns");
    private final ListView<String> selected = new ListView<>();
    private final ScrollPane preview = new ScrollPane();
    private final Label message = new Label("Add genes from the browser to compare their transcripts and expression.");
    private ComparisonFigure.Figure figure;

    public ComparisonPane(GenomeDataService genome, ExpressionDataService expression) {
        this.genome = genome; this.expression = expression;
        setId("comparison-pane"); setPadding(new Insets(12));
        alignment.setValue(ComparisonFigure.Alignment.TRANSCRIPTION_START);
        alignment.setId("comparison-alignment");
        scale.setValue(ComparisonFigure.Scale.LOG1P); scale.setId("expression-scale");
        compressed.setId("compress-introns");
        Button refresh = new Button("Refresh"); refresh.setOnAction(e -> refresh());
        Button svg = new Button("Export SVG + TSV"); svg.setOnAction(e -> chooseExport(false));
        Button png = new Button("Export PNG + TSV"); png.setOnAction(e -> chooseExport(true));
        HBox controls = new HBox(10, new Label("Align"), alignment, compressed, new Label("Expression"), scale, refresh, svg, png);
        FlowPane flow = new FlowPane(10, 8); flow.getChildren().addAll(controls.getChildren());
        message.setWrapText(true); message.setId("comparison-message");
        setTop(new VBox(8, flow, message));
        selected.setId("comparison-genes"); selected.setPrefWidth(200);
        Button remove = new Button("Remove selected"); remove.setOnAction(e -> { ids.remove(selected.getSelectionModel().getSelectedItem()); refresh(); });
        Button clear = new Button("Clear"); clear.setOnAction(e -> clear());
        setLeft(new VBox(8, selected, remove, clear));
        setCenter(preview);
        alignment.setOnAction(e -> refresh()); scale.setOnAction(e -> refresh()); compressed.setOnAction(e -> refresh());
    }
    public void add(Gene gene) {
        if (gene == null || ids.contains(gene.getId())) return;
        if (ids.size() >= 100) { message.setText("Compare at most 100 genes per figure."); return; }
        ids.add(gene.getId()); refresh();
    }
    public void clear() { ids.clear(); refresh(); }
    public List<String> geneIds() { return List.copyOf(ids); }
    public ComparisonFigure.Settings settings() { return new ComparisonFigure.Settings(alignment.getValue(), compressed.isSelected(), scale.getValue(), true); }
    public void restore(List<String> genes, ComparisonFigure.Settings settings) {
        ids.clear(); ids.addAll(genes);
        alignment.setValue(settings.alignment()); compressed.setSelected(settings.compressed()); scale.setValue(settings.scale()); refresh();
    }
    public void refresh() {
        selected.getItems().setAll(ids); figure = null; preview.setContent(null);
        if (ids.isEmpty()) { message.setText("Add genes from the browser. No homology is inferred from selected loci."); return; }
        try {
            List<Gene> genes = ids.stream().map(genome::findGene).filter(Objects::nonNull).toList();
            figure = new ComparisonFigure().build(genes, expression, settings());
            preview.setContent(figure.canvas());
            message.setText(ids.size() + " loci | " + expression.getLevel() + " expression | gray = missing; 0 is a measured zero. Full sample names are in the exported TSV.");
        } catch (RuntimeException ex) { message.setText(ex.getMessage()); }
    }
    public void export(Path target, boolean png) throws IOException {
        if (figure == null) throw new IOException("No valid comparison to export");
        if (png) ImageExporter.exportPng(figure.canvas(), target.toFile(), 1);
        else try (Writer out = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) { figure.writeSvg(out); }
        String name = target.getFileName().toString().replaceFirst("\\.(svg|png)$", "");
        Files.writeString(target.resolveSibling(name + ".tsv"), figure.table(), StandardCharsets.UTF_8);
    }
    private void chooseExport(boolean png) {
        FileChooser chooser = new FileChooser();
        String extension = png ? "png" : "svg";
        chooser.setInitialFileName("phytoscope-comparison." + extension);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(extension.toUpperCase(Locale.ROOT), "*." + extension));
        File target = chooser.showSaveDialog(getScene() == null ? null : getScene().getWindow());
        if (target == null) return;
        try { export(target.toPath(), png); message.setText("Exported figure and data table: " + target); }
        catch (Exception ex) { message.setText("Export failed: " + ex.getMessage()); }
    }
}
