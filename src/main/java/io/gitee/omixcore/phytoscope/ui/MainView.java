package io.gitee.omixcore.phytoscope.ui;

import io.gitee.omixcore.phytoscope.model.ExpressionProfile;
import io.gitee.omixcore.phytoscope.model.Gene;
import io.gitee.omixcore.phytoscope.model.SequenceRegion;
import io.gitee.omixcore.phytoscope.parser.AnnotationFormat;
import io.gitee.omixcore.phytoscope.service.ExampleDataset;
import io.gitee.omixcore.phytoscope.service.ExpressionDataService;
import io.gitee.omixcore.phytoscope.service.GenomeDataService;
import io.gitee.omixcore.phytoscope.ui.component.GeneDetailPane;
import io.gitee.omixcore.phytoscope.ui.component.GeneTrackRenderer;
import io.gitee.omixcore.phytoscope.ui.component.ImageExporter;
import io.gitee.omixcore.phytoscope.ui.component.SvgExporter;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/**
 * Main window: a three-pane layout with dataset and chromosome selectors.
 *
 * <pre>
 *  +---------------------------------------------------------------+
 *  |  Menu: File / Example datasets / View / Chromosome / Help     |
 *  |  Toolbar: dataset / sequence / search / zoom / export         |
 *  +------------+--------------------------------+-----------------+
 *  | Gene list  |  Genome structure tracks       |  Gene details   |
 *  | (selected  |                                |  + expression   |
 *  |  sequence) |                                |                 |
 *  +------------+--------------------------------+-----------------+
 *  |  Status bar                                                   |
 *  +---------------------------------------------------------------+
 * </pre>
 *
 * <p>The interface is organism-neutral: whichever dataset is loaded, the
 * species, assembly, sequence names and sample groups shown in the interface
 * are read from that dataset's own metadata.</p>
 */
@Component
public class MainView extends BorderPane {

    private final GenomeDataService dataService;
    private final ExpressionDataService expressionService;
    private final GeneTrackRenderer renderer = new GeneTrackRenderer();
    private final GeneDetailPane detailPane = new GeneDetailPane();

    private final ListView<Gene> geneList = new ListView<>();
    private final ObservableList<Gene> geneItems = FXCollections.observableArrayList();
    private final TextField searchField = new TextField();
    private final Label statusLabel = new Label();
    private final ComboBox<ExampleDataset> datasetSelector = new ComboBox<>();
    private final ComboBox<String> chromosomeSelector = new ComboBox<>();

    private final Pane canvasHolder = new Pane();
    private final ScrollPane browserScroll = new ScrollPane();

    private double dragAnchorX;
    private int dragAnchorViewStart;

    /** True while a load is in progress, used to mute change listeners. */
    private boolean loading;

    public MainView(GenomeDataService dataService, ExpressionDataService expressionService) {
        this.dataService = dataService;
        this.expressionService = expressionService;
        setStyle("-fx-background-color: #EEF2F7;");

        renderer.setOnGeneSelected(this::selectGene);
        detailPane.setExpressionData(expressionService::getProfile, expressionService.globalMaxTpm());

        setTop(new VBox(buildMenuBar(), buildToolBar()));
        setLeft(buildGeneListPane());
        setCenter(buildBrowserPane());
        setRight(buildDetailPane());
        setBottom(buildStatusBar());

        setupCanvasInteractions();
        Platform.runLater(this::refreshStatus);
    }

    // ------------------------------------------------------------------
    // Data loading
    // ------------------------------------------------------------------

    /** Loads the default bundled dataset so the window is never empty. */
    public void loadInitialData() {
        loadExample(ExampleDataset.defaultDataset());
    }

    /**
     * Loads one of the bundled example datasets and rebuilds every view.
     * This is how the cross-species panel is explored without any download.
     */
    private void loadExample(ExampleDataset dataset) {
        if (dataset == null) {
            return;
        }
        try {
            loading = true;
            dataService.loadExample(dataset);
            expressionService.loadExample(dataset);
            datasetSelector.getSelectionModel().select(dataset);
            afterDatasetChanged();
        } catch (Exception ex) {
            showLoadError("load example dataset", ex);
        } finally {
            loading = false;
        }
    }

    /**
     * Imports an annotation file chosen by the user. Any organism and either
     * supported dialect is accepted; species and sequence spans are taken from
     * the file itself rather than assumed.
     */
    private void importAnnotation() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Import annotation");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Annotation (*.gff3, *.gff, *.gtf, *.gff2)",
                        AnnotationFormat.allExtensions()),
                new FileChooser.ExtensionFilter("All files", "*.*"));
        File file = chooser.showOpenDialog(windowOrNull());
        if (file == null) {
            return;
        }
        try {
            loading = true;
            dataService.loadFromFile(file.toPath());
            // Expression data from the previous dataset no longer applies.
            expressionService.clear();
            datasetSelector.getSelectionModel().clearSelection();
            afterDatasetChanged();
        } catch (Exception ex) {
            showLoadError("import annotation", ex);
        } finally {
            loading = false;
        }
    }

    /**
     * Imports an expression matrix chosen by the user and re-aligns the
     * expression summary against the genes currently loaded.
     */
    private void importExpression() {
        if (dataService.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION,
                    "Load an annotation before importing expression data.").showAndWait();
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Import expression matrix");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Expression matrix (*.tsv, *.csv, *.txt)",
                        "*.tsv", "*.csv", "*.txt"),
                new FileChooser.ExtensionFilter("All files", "*.*"));
        File file = chooser.showOpenDialog(windowOrNull());
        if (file == null) {
            return;
        }
        try {
            expressionService.loadFromFile(file.toPath());
            afterDatasetChanged();
        } catch (Exception ex) {
            showLoadError("import expression matrix", ex);
        }
    }

    /**
     * Pushes a freshly loaded dataset into every view: the chromosome selector,
     * the renderer, the gene list, the detail pane, the axis caption and the
     * status bar.
     */
    private void afterDatasetChanged() {
        detailPane.setExpressionData(expressionService::getProfile,
                expressionService.globalMaxTpm());
        detailPane.setValueUnit(expressionService.getValueUnit());

        refreshChromosomeSelector();
        showActiveChromosome();
    }

    /**
     * Rebuilds the chromosome selector from the loaded metadata. The panel is
     * populated from the data itself, so any annotation with any number of
     * sequences works; a dataset with a single sequence still shows it, which
     * keeps the control's meaning obvious.
     */
    private void refreshChromosomeSelector() {
        List<String> sequences = dataService.getChromosomes();
        boolean wasLoading = loading;
        loading = true;
        try {
            chromosomeSelector.getItems().setAll(sequences);
            chromosomeSelector.getSelectionModel().select(dataService.getActiveSequence());
        } finally {
            loading = wasLoading;
        }
    }

    /**
     * Renders the currently selected chromosome: its genes, its axis caption and
     * the coordinate window it should be drawn in.
     *
     * <p>When the sequence's published length is known the whole chromosome is
     * shown, so genes sit at their true coordinates — the Chr4 excerpt covers
     * only the first 16 kb of an 18.6 Mb chromosome, and that contrast is the
     * point of a genome-browser view. Two View actions cover the other needs:
     * "Fit all genes" tightens the window to the excerpt the file actually
     * describes, and "Show full chromosome" re-opens the whole sequence after
     * zooming.</p>
     */
    private void showActiveChromosome() {
        String sequence = dataService.getActiveSequence();
        renderer.setSequenceLabel(sequence == null ? "" : sequence);

        List<Gene> genes = dataService.getGenesOnActiveSequence();
        geneItems.setAll(genes);
        renderer.setGenes(genes);
        searchField.clear();

        int[] span = dataService.getActiveSequenceSpan();
        if (span != null && span[1] > span[0]) {
            renderer.showWindow(span[0], span[1]);
        } else {
            renderer.fitToAll();
        }
        redraw();

        if (!genes.isEmpty()) {
            geneList.getSelectionModel().selectFirst();
            selectGene(genes.get(0));
        } else {
            detailPane.clear();
        }
    }

    /**
     * Widens the view to the whole declared sequence, so the genes sit at their
     * true chromosome coordinates instead of being spread over the excerpt.
     */
    private void showFullChromosome() {
        SequenceRegion region = dataService.getMetadata().findRegion(dataService.getActiveSequence());
        if (region == null || !region.isUsable()) {
            return;
        }
        renderer.showWindow(region.start(), region.end());
        redraw();
    }

    /**
     * Switches the browser to another chromosome, keeping genes from other
     * sequences out of the panel and the track view.
     */
    private void selectChromosome(String sequence) {
        if (sequence == null || !dataService.setActiveSequence(sequence)) {
            return;
        }
        showActiveChromosome();
    }

    /**
     * Looks for a gene anywhere in the dataset and, when it lives on a different
     * chromosome, switches there first so the hit is actually visible.
     *
     * @return true when the view moved to another chromosome
     */
    private boolean revealGeneAcrossChromosomes(Gene gene) {
        if (gene == null || gene.getChromosome() == null
                || gene.getChromosome().equals(dataService.getActiveSequence())) {
            return false;
        }
        if (!dataService.setActiveSequence(gene.getChromosome())) {
            return false;
        }
        loading = true;
        try {
            chromosomeSelector.getSelectionModel().select(gene.getChromosome());
        } finally {
            loading = false;
        }
        return true;
    }

    private void showLoadError(String action, Exception ex) {
        statusLabel.setText("Failed to " + action + ": " + ex.getMessage());
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Load failed");
        alert.setHeaderText("Could not " + action + ".");
        alert.setContentText(String.valueOf(ex.getMessage()));
        alert.showAndWait();
    }

    private javafx.stage.Window windowOrNull() {
        return getScene() == null ? null : getScene().getWindow();
    }

    // ------------------------------------------------------------------
    // Menu bar
    // ------------------------------------------------------------------

    private MenuBar buildMenuBar() {
        MenuItem importAnnotationItem = new MenuItem("Import annotation (GFF3 / GTF)\u2026");
        importAnnotationItem.setOnAction(e -> importAnnotation());

        MenuItem importExpressionItem = new MenuItem("Import expression matrix\u2026");
        importExpressionItem.setOnAction(e -> importExpression());

        MenuItem exportPngItem = new MenuItem("Export view as PNG\u2026");
        exportPngItem.setOnAction(e -> exportPng());

        MenuItem exportSvgItem = new MenuItem("Export view as SVG\u2026");
        exportSvgItem.setOnAction(e -> exportSvg());

        MenuItem quit = new MenuItem("Quit");
        quit.setOnAction(e -> Platform.exit());

        Menu file = new Menu("File");
        file.getItems().addAll(importAnnotationItem, importExpressionItem,
                new SeparatorMenuItem(), exportPngItem, exportSvgItem,
                new SeparatorMenuItem(), quit);

        // One entry per bundled species, so the panel is one click away.
        Menu examples = new Menu("Example datasets");
        for (ExampleDataset dataset : ExampleDataset.CATALOGUE) {
            MenuItem item = new MenuItem(dataset.fullLabel());
            item.setOnAction(e -> loadExample(dataset));
            examples.getItems().add(item);
        }

        MenuItem fitAllItem = new MenuItem("Fit all genes");
        fitAllItem.setOnAction(e -> {
            renderer.fitToAll();
            redraw();
        });
        MenuItem fullChromosomeItem = new MenuItem("Show full chromosome");
        fullChromosomeItem.setOnAction(e -> showFullChromosome());
        MenuItem zoomInItem = new MenuItem("Zoom in");
        zoomInItem.setOnAction(e -> {
            renderer.zoom(0.7);
            redraw();
        });
        MenuItem zoomOutItem = new MenuItem("Zoom out");
        zoomOutItem.setOnAction(e -> {
            renderer.zoom(1.4);
            redraw();
        });
        Menu view = new Menu("View");
        view.getItems().addAll(fitAllItem, fullChromosomeItem,
                new SeparatorMenuItem(), zoomInItem, zoomOutItem);

        // One entry per sequence in the loaded annotation.
        Menu chromosomeMenu = new Menu("Chromosome");
        chromosomeMenu.setOnShowing(e -> {
            chromosomeMenu.getItems().clear();
            for (String sequence : dataService.getChromosomes()) {
                CheckMenuItem item = new CheckMenuItem(
                        sequence + "   (" + sequenceSizeLabel(sequence) + ")");
                item.setSelected(sequence.equals(dataService.getActiveSequence()));
                item.setOnAction(e2 -> selectChromosome(sequence));
                chromosomeMenu.getItems().add(item);
            }
            if (chromosomeMenu.getItems().isEmpty()) {
                MenuItem none = new MenuItem("No sequence loaded");
                none.setDisable(true);
                chromosomeMenu.getItems().add(none);
            }
        });

        MenuItem about = new MenuItem("About PhytoScope");
        about.setOnAction(e -> showAbout());
        Menu help = new Menu("Help");
        help.getItems().add(about);

        MenuBar bar = new MenuBar(file, examples, view, chromosomeMenu, help);
        bar.setStyle("-fx-background-color: #FFFFFF;");
        return bar;
    }

    private void showAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About PhytoScope");
        alert.setHeaderText("PhytoScope \u2014 Plant Genome Structure Browser");
        alert.setContentText("A lightweight, species-agnostic desktop framework for\n"
                + "visualising gene structure from plant genome annotations.\n\n"
                + "Reads GFF3 and GTF/GFF2 from any organism, shows one\n"
                + "chromosome at a time and reports sample-level expression\n"
                + "in the gene detail pane.\n\n"
                + "Loaded dataset: " + dataService.getMetadata().summary());
        alert.showAndWait();
    }

    // ------------------------------------------------------------------
    // Toolbar
    // ------------------------------------------------------------------

    private HBox buildToolBar() {
        HBox bar = new HBox(10);
        bar.setPadding(new Insets(10, 14, 10, 14));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color: #FFFFFF;"
                + " -fx-border-color: transparent transparent #E3E9F0 transparent;");

        Label brand = new Label("PhytoScope");
        brand.setFont(Font.font("Segoe UI", FontWeight.BOLD, 17));
        brand.setStyle("-fx-text-fill: #2E5C8A;");

        Label subtitle = new Label("Plant Genome Structure Browser");
        subtitle.setStyle("-fx-text-fill: #8899AA; -fx-font-size: 11.5px;");

        VBox brandBox = new VBox(0, brand, subtitle);

        // Dataset selector: switches between the bundled species examples.
        datasetSelector.getItems().setAll(ExampleDataset.CATALOGUE);
        datasetSelector.setPrefWidth(260);
        datasetSelector.setTooltip(new Tooltip(
                "Switch between the bundled example datasets (four species)"));
        datasetSelector.setConverter(new StringConverter<>() {
            @Override
            public String toString(ExampleDataset dataset) {
                return dataset == null ? "" : dataset.fullLabel();
            }

            @Override
            public ExampleDataset fromString(String string) {
                return null;
            }
        });
        datasetSelector.getSelectionModel().selectedItemProperty().addListener((obs, old, dataset) -> {
            if (!loading && dataset != null) {
                loadExample(dataset);
            }
        });

        // Chromosome selector: the browser shows one sequence at a time, and the
        // list is rebuilt from whatever sequences the loaded data declares.
        chromosomeSelector.setPrefWidth(175);
        chromosomeSelector.setTooltip(new Tooltip(
                "Show one chromosome / sequence at a time\n"
                        + "(the list is built from the loaded annotation)"));
        chromosomeSelector.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, sequence) -> {
                    if (!loading && sequence != null) {
                        selectChromosome(sequence);
                    }
                });

        Label chromosomeLabel = new Label("Sequence");
        chromosomeLabel.setStyle("-fx-text-fill: #6B7B8B; -fx-font-size: 11.5px;");

        Region spacer0 = new Region();
        HBox.setHgrow(spacer0, Priority.NEVER);

        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);

        searchField.setPromptText("Search gene ID / name / alias");
        searchField.setPrefWidth(220);
        searchField.textProperty().addListener((obs, old, val) -> applySearch(val));

        Button zoomIn = toolButton("Zoom +", "Zoom in about the centre (or scroll up)");
        zoomIn.setOnAction(e -> {
            renderer.zoom(0.7);
            redraw();
        });

        Button zoomOut = toolButton("Zoom \u2212", "Zoom out about the centre (or scroll down)");
        zoomOut.setOnAction(e -> {
            renderer.zoom(1.4);
            redraw();
        });

        Button fitAll = toolButton("Fit all", "Show all genes");
        fitAll.setOnAction(e -> {
            renderer.fitToAll();
            redraw();
        });

        Button focusSelected = toolButton("Focus selected", "Zoom the view onto the selected gene");
        focusSelected.setOnAction(e -> {
            Gene gene = renderer.getSelectedGene();
            if (gene != null) {
                renderer.focusOn(gene, 0.25);
                redraw();
            }
        });

        // Export menu: the browser view can be saved as a raster or vector image
        MenuItem exportPngItem = new MenuItem("Export view as PNG...");
        exportPngItem.setOnAction(e -> exportPng());
        MenuItem exportSvgItem = new MenuItem("Export view as SVG...");
        exportSvgItem.setOnAction(e -> exportSvg());
        MenuButton exportMenu = new MenuButton("Export", null, exportPngItem, exportSvgItem);
        exportMenu.setStyle("-fx-background-color: #E8EFF7; -fx-text-fill: #2E5C8A;"
                + " -fx-background-radius: 5; -fx-font-size: 12px; -fx-cursor: hand;");
        exportMenu.setTooltip(new Tooltip(
                "Save the current browser view (gene structure tracks)"));

        bar.getChildren().addAll(brandBox, spacer0, datasetSelector, chromosomeLabel,
                chromosomeSelector, spacer1, searchField,
                zoomOut, zoomIn, fitAll, focusSelected, exportMenu);
        return bar;
    }

    // ------------------------------------------------------------------
    // Export
    // ------------------------------------------------------------------

    /** Saves the current view as a PNG image. */
    private void exportPng() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export view as PNG");
        chooser.setInitialFileName(defaultExportName() + ".png");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PNG image (*.png)", "*.png"));
        File file = chooser.showSaveDialog(getScene() == null ? null : getScene().getWindow());
        if (file == null) {
            return;
        }
        try {
            ImageExporter.exportPng(renderer.getCanvas(), file, ImageExporter.DEFAULT_SCALE);
            statusLabel.setText("Exported PNG: " + file.getAbsolutePath());
        } catch (Exception ex) {
            showExportError("PNG", ex);
        }
    }

    /** Saves the current view as a vector SVG document. */
    private void exportSvg() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export view as SVG");
        chooser.setInitialFileName(defaultExportName() + ".svg");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("SVG vector image (*.svg)", "*.svg"));
        File file = chooser.showSaveDialog(getScene() == null ? null : getScene().getWindow());
        if (file == null) {
            return;
        }
        File target = file.getName().toLowerCase().endsWith(".svg")
                ? file : new File(file.getParentFile(), file.getName() + ".svg");
        try (Writer writer = Files.newBufferedWriter(target.toPath(), StandardCharsets.UTF_8)) {
            new SvgExporter(renderer).export(writer,
                    renderer.getLastRenderWidth(), renderer.getLastRenderHeight());
            statusLabel.setText("Exported SVG: " + target.getAbsolutePath());
        } catch (Exception ex) {
            showExportError("SVG", ex);
        }
    }

    /** A sensible default file name reflecting the current window and selection. */
    private String defaultExportName() {
        Gene selected = renderer.getSelectedGene();
        String locus = selected != null ? selected.getDisplayName()
                : dataService.getGenesOnActiveSequence().isEmpty() ? "phytoscope"
                        : dataService.getGenesOnActiveSequence().get(0).getDisplayName();
        String sequence = dataService.getActiveSequence();
        String prefix = sequence == null || sequence.isBlank() ? "phytoscope" : sequence;
        return "phytoscope_" + prefix + "_" + locus + "_"
                + renderer.getViewStart() + "-" + renderer.getViewEnd();
    }

    /** Length of one loaded sequence, as a short human-readable string. */
    private String sequenceSizeLabel(String sequence) {
        SequenceRegion region = dataService.getMetadata().findRegion(sequence);
        if (region == null || !region.isUsable()) {
            return "span unknown";
        }
        return ExampleDataset.formatLength(region.length());
    }

    private void showExportError(String format, Exception ex) {
        statusLabel.setText("Failed to export " + format + ": " + ex.getMessage());
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Export failed");
        alert.setHeaderText("Could not export the view as " + format + ".");
        alert.setContentText(String.valueOf(ex.getMessage()));
        alert.showAndWait();
    }

    private Button toolButton(String text, String tooltip) {
        Button button = new Button(text);
        button.setStyle("-fx-background-color: #F0F4F9; -fx-text-fill: #2C3E50;"
                + " -fx-background-radius: 5; -fx-font-size: 12px; -fx-cursor: hand;");
        button.setTooltip(new Tooltip(tooltip));
        return button;
    }

    private void applySearch(String keyword) {
        List<Gene> hits = dataService.search(keyword);
        // A hit on another chromosome is only meaningful if we go there.
        if (!hits.isEmpty()) {
            revealGeneAcrossChromosomes(hits.get(0));
        }
        geneItems.setAll(hits);
        renderer.setGenes(hits);
        renderer.fitToAll();
        redraw();
        if (hits.isEmpty()) {
            detailPane.clear();
        }
    }

    // ------------------------------------------------------------------
    // Gene list
    // ------------------------------------------------------------------

    private VBox buildGeneListPane() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(12));
        box.setPrefWidth(240);
        box.setMinWidth(200);
        box.setStyle("-fx-background-color: #FFFFFF;"
                + " -fx-border-color: transparent #E3E9F0 transparent transparent;");

        Label title = new Label("Genes");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        title.setStyle("-fx-text-fill: #2C3E50;");

        geneList.setItems(geneItems);
        geneList.setCellFactory(lv -> new GeneListCell());
        geneList.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(geneList, Priority.ALWAYS);

        geneList.getSelectionModel().selectedItemProperty().addListener((obs, old, gene) -> {
            if (gene != null) {
                renderer.setSelectedGene(gene);
                detailPane.showGene(gene);
                redraw();
            }
        });

        box.getChildren().addAll(title, geneList);
        return box;
    }

    /** Gene list cell: name on the first line, locus and strand on the second. */
    private static class GeneListCell extends ListCell<Gene> {
        @Override
        protected void updateItem(Gene gene, boolean empty) {
            super.updateItem(gene, empty);
            if (empty || gene == null) {
                setText(null);
                setGraphic(null);
                return;
            }
            VBox box = new VBox(1);
            Label name = new Label(gene.getDisplayName());
            name.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
            name.setStyle("-fx-text-fill: #2C3E50;");

            String sub = String.format("%s:%,d\u2013%,d  %s", gene.getChromosome(),
                    gene.getStart(), gene.getEnd(), gene.isForwardStrand() ? "+" : "\u2212");
            Label meta = new Label(sub);
            meta.setStyle("-fx-text-fill: #8899AA; -fx-font-size: 10.5px;");

            box.getChildren().addAll(name, meta);
            setGraphic(box);
            setStyle("-fx-padding: 6 8 6 8;");
        }
    }

    // ------------------------------------------------------------------
    // Browser pane
    // ------------------------------------------------------------------

    private VBox buildBrowserPane() {
        VBox box = new VBox();
        box.setPadding(new Insets(12));

        Label hint = new Label("Scroll to zoom  \u00b7  drag to pan  \u00b7  "
                + "click a gene for details  \u00b7  expression values are shown in the detail pane");
        hint.setStyle("-fx-text-fill: #8899AA; -fx-font-size: 11px;");
        VBox.setMargin(hint, new Insets(0, 0, 8, 2));

        canvasHolder.getChildren().add(renderer.getCanvas());
        canvasHolder.setStyle("-fx-background-color: #FFFFFF;"
                + " -fx-border-color: #E3E9F0; -fx-border-radius: 6; -fx-background-radius: 6;");

        browserScroll.setContent(canvasHolder);
        browserScroll.setFitToWidth(true);
        browserScroll.setPannable(false);
        browserScroll.setStyle("-fx-background: #FFFFFF; -fx-background-color: #FFFFFF;"
                + " -fx-border-color: #E3E9F0; -fx-border-radius: 6;");
        VBox.setVgrow(browserScroll, Priority.ALWAYS);

        box.getChildren().addAll(hint, browserScroll);
        return box;
    }

    private void setupCanvasInteractions() {
        var canvas = renderer.getCanvas();

        canvas.addEventHandler(ScrollEvent.SCROLL, event -> {
            double factor = event.getDeltaY() > 0 ? 0.85 : 1.18;
            renderer.zoom(factor);
            redraw();
            event.consume();
        });

        canvas.setOnMousePressed(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                dragAnchorX = event.getX();
                dragAnchorViewStart = renderer.getViewStart();
            }
        });

        canvas.setOnMouseDragged(event -> {
            if (event.isPrimaryButtonDown()) {
                double dx = event.getX() - dragAnchorX;
                double plotWidth = renderer.plotRight() - renderer.plotLeft();
                if (plotWidth <= 0) {
                    return;
                }
                int baseSpan = renderer.getViewEnd() - dragAnchorViewStart + 1;
                int deltaBp = (int) Math.round(-dx / plotWidth * baseSpan);
                int newStart = Math.max(1, dragAnchorViewStart + deltaBp);
                renderer.focusRange(newStart, newStart + baseSpan - 1);
                redraw();
            }
        });

        canvas.setOnMouseClicked(event -> {
            Gene hit = renderer.hitTest(event.getX(), event.getY());
            if (hit != null) {
                geneList.getSelectionModel().select(hit);
                geneList.scrollTo(hit);
            }
        });

        canvas.setOnMouseMoved(event -> {
            Gene hit = renderer.hitTest(event.getX(), event.getY());
            renderer.setHoveredGene(hit);
            canvas.setCursor(hit != null ? javafx.scene.Cursor.HAND : javafx.scene.Cursor.DEFAULT);
            if (hit != null) {
                ExpressionProfile profile = expressionService.getProfile(hit.getId());
                String extra = profile == null ? "" : "\n" + expressionService.describe(hit.getId());
                Tooltip.install(canvas, new Tooltip(String.format(
                        "%s\n%s:%,d\u2013%,d\n%d bp \u00b7 %s%s",
                        hit.getDisplayName(), hit.getChromosome(), hit.getStart(), hit.getEnd(),
                        hit.length(), hit.isForwardStrand() ? "forward +" : "reverse \u2212", extra)));
            }
        });
    }

    // ------------------------------------------------------------------
    // Detail pane
    // ------------------------------------------------------------------

    private VBox buildDetailPane() {
        VBox box = new VBox();
        box.setPrefWidth(330);
        box.setMinWidth(270);
        box.setStyle("-fx-background-color: #FFFFFF;"
                + " -fx-border-color: transparent transparent transparent #E3E9F0;");
        VBox.setVgrow(detailPane, Priority.ALWAYS);
        box.getChildren().add(detailPane);
        return box;
    }

    // ------------------------------------------------------------------
    // Status bar
    // ------------------------------------------------------------------

    private HBox buildStatusBar() {
        HBox bar = new HBox(16);
        bar.setPadding(new Insets(6, 14, 6, 14));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color: #FFFFFF;"
                + " -fx-border-color: #E3E9F0 transparent transparent transparent;");
        statusLabel.setStyle("-fx-text-fill: #6B7B8B; -fx-font-size: 11.5px;");
        bar.getChildren().add(statusLabel);
        return bar;
    }

    private void refreshStatus() {
        var metadata = dataService.getMetadata();
        String expression = expressionService.isEmpty()
                ? "no expression data"
                : expressionService.getTissues().size() + " "
                        + (expressionService.getTissues().size() == 1 ? "sample group" : "sample groups");
        String sequence = dataService.getActiveSequence();
        String sequenceCount = metadata.getSequenceCount()
                + (metadata.getSequenceCount() == 1 ? " sequence" : " sequences");
        statusLabel.setText(String.format(
                "%s  |  %s  |  %s  |  %s  |  %d genes  |  %d features  |  %s  | "
                        + "  %s window %,d\u2013%,d (%s bp)",
                metadata.getSourceLabel(),
                metadata.getSpecies(),
                dataService.getFormat().getLabel() + " / " + metadata.getAssembly(),
                sequenceCount,
                dataService.getGeneCount(),
                dataService.getRawFeatureCount(),
                expression,
                sequence == null ? "no chromosome selected" : sequence,
                renderer.getViewStart(), renderer.getViewEnd(),
                formatSpan(renderer.getSpan())));
    }

    private static String formatSpan(int span) {
        return span < 10_000 ? String.valueOf(span) : String.format("%,d", span);
    }

    /** Redraws the canvas and keeps the scroll extent in sync. */
    private void redraw() {
        double width = Math.max(600, browserScroll.getViewportBounds().getWidth() - 4);
        double height = Math.max(400, renderer.contentHeight());
        renderer.render(width, height);
        canvasHolder.setPrefSize(width, height);
        refreshStatus();
    }

    public void onViewportResized() {
        redraw();
    }

    /** Selects a gene and syncs highlight, detail panel, list and canvas. */
    private void selectGene(Gene gene) {
        if (gene == null) {
            return;
        }
        if (geneList.getSelectionModel().getSelectedItem() != gene) {
            geneList.getSelectionModel().select(gene);
        }
        renderer.setSelectedGene(gene);
        detailPane.showGene(gene);
        redraw();
    }

    public GeneTrackRenderer getRenderer() {
        return renderer;
    }
}
