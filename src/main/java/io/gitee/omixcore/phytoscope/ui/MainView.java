package io.gitee.omixcore.phytoscope.ui;

import io.gitee.omixcore.phytoscope.comparison.ComparisonFigure;
import io.gitee.omixcore.phytoscope.project.ProjectStore;
import io.gitee.omixcore.phytoscope.ui.component.ComparisonPane;
import io.gitee.omixcore.phytoscope.ui.component.LoadingOperation;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import java.nio.file.Path;
import java.io.IOException;
import io.gitee.omixcore.phytoscope.model.ExpressionProfile;
import io.gitee.omixcore.phytoscope.model.Gene;
import io.gitee.omixcore.phytoscope.model.SequenceRegion;
import io.gitee.omixcore.phytoscope.parser.AnnotationFormat;
import io.gitee.omixcore.phytoscope.service.ExampleDataset;
import io.gitee.omixcore.phytoscope.service.ExpressionDataService;
import io.gitee.omixcore.phytoscope.service.GenomeDataService;
import io.gitee.omixcore.phytoscope.service.GeneSequenceService;
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
    private final GeneSequenceService sequenceService = new GeneSequenceService();
    private final GeneTrackRenderer renderer = new GeneTrackRenderer();
    private ExampleDataset activeExample;
    private java.util.concurrent.CompletableFuture<?> pendingLoad = java.util.concurrent.CompletableFuture.completedFuture(null);
    private ComparisonPane comparisonPane;
    private ProjectStore.Restored restoredProject;
    private final TabPane workspaceTabs = new TabPane();
    private final GeneDetailPane detailPane = new GeneDetailPane();

    private final ListView<Gene> geneList = new ListView<>();
    private final ObservableList<Gene> geneItems = FXCollections.observableArrayList();
    private final TextField searchField = new TextField();
    private final Label browserNotice = new Label();
    private final Label statusLabel = new Label();
    private final ComboBox<ExampleDataset> datasetSelector = new ComboBox<>();
    private final ComboBox<String> chromosomeSelector = new ComboBox<>();
    private final Label sequenceCountLabel = new Label();

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
        detailPane.setExpressionData(expressionService::getProfileForAnnotation, expressionService.globalMaxTpm());
        detailPane.setSequenceService(sequenceService);

        setTop(new VBox(buildMenuBar(), buildToolBar()));
        setLeft(buildGeneListPane());
        comparisonPane = new ComparisonPane(dataService, expressionService);
        Tab browser = new Tab("Genome browser", buildBrowserPane()); browser.setClosable(false);
        Tab comparison = new Tab("Gene comparison", comparisonPane); comparison.setClosable(false);
        workspaceTabs.getTabs().addAll(browser, comparison);
        setCenter(workspaceTabs);
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

    public void loadExampleData(ExampleDataset dataset) { loadExample(dataset); }
    public java.util.concurrent.CompletableFuture<?> getPendingLoad() { return pendingLoad; }
    public void loadInitialDataAsync() { startExampleLoad(ExampleDataset.defaultDataset(), false); }
    public java.util.concurrent.CompletableFuture<?> startExampleLoad(ExampleDataset dataset, boolean atlas) {
        pendingLoad = LoadingOperation.run(windowOrNull(), "Loading " + dataset.species(), () -> {
            GenomeDataService next = new GenomeDataService(); next.loadExample(dataset);
            ExpressionDataService measurements = new ExpressionDataService(); measurements.loadExample(dataset);
            measurements.reconcile(next);
            if (atlas) {
                measurements.loadFromResource("/data/case-studies/arabidopsis-atlas/expression.tsv", "Expression Atlas E-MTAB-7978 (single-assay conditions)");
                measurements.reconcile(next);
            }
            return new Loaded(next, measurements);
        }, next -> {
            dataService.replaceWith(next.genome()); expressionService.replaceWith(next.expression());
            activeExample = dataset; datasetSelector.setTooltip(new Tooltip(dataset.label() + "\n" + dataset.attribution())); loading = true;
            try { datasetSelector.getSelectionModel().select(dataset); } finally { loading = false; }
            comparisonPane.clear(); afterDatasetChanged();
            if (atlas) pinAtlasGenes();
        }, this::loadFailed);
        return pendingLoad;
    }
    private record Loaded(GenomeDataService genome, ExpressionDataService expression) { }
    private void loadFailed(Throwable ex) {
        loading = true;
        try { datasetSelector.getSelectionModel().select(activeExample); } finally { loading = false; }
        if (ex instanceof java.util.concurrent.CancellationException) statusLabel.setText(ex.getMessage());
        else showLoadError("load data", ex instanceof Exception error ? error : new IOException(ex));
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
            expressionService.reconcile(dataService);
            activeExample = dataset;
            comparisonPane.clear();
            datasetSelector.getSelectionModel().select(dataset);
            datasetSelector.setTooltip(new Tooltip(dataset.label() + "\nAssembly: "
                    + dataset.assembly() + "\n" + dataset.attribution()));
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
        pendingLoad = LoadingOperation.run(windowOrNull(), "Importing annotation", () -> {
            GenomeDataService next = new GenomeDataService(); next.loadFromFile(file.toPath()); return next;
        }, next -> {
            dataService.replaceWith(next); expressionService.clear(); comparisonPane.clear(); activeExample = null;
            loading = true; try { datasetSelector.getSelectionModel().clearSelection(); } finally { loading = false; }
            afterDatasetChanged();
        }, this::loadFailed);
    }

    /**
     * Imports an expression matrix chosen by the user and re-aligns the
     * expression summary against the genes currently loaded.
     */
    /**
     * Imports a genome DNA FASTA matching the active annotation assembly.
     * Sequence extraction remains disabled until this explicit reference is loaded.
     */
    private void importGenomeFasta() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Import matching genome DNA FASTA");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Genome FASTA (*.fa, *.fasta, *.fna, *.gz)",
                        "*.fa", "*.fasta", "*.fna", "*.fa.gz", "*.fasta.gz", "*.fna.gz"),
                new FileChooser.ExtensionFilter("All files", "*.*"));
        File file = chooser.showOpenDialog(windowOrNull());
        if (file == null) return;
        pendingLoad = LoadingOperation.run(windowOrNull(), "Loading genome FASTA", () -> {
            GeneSequenceService next = new GeneSequenceService();
            next.loadFromFile(file.toPath());
            return next;
        }, next -> {
            sequenceService.replaceWith(next);
            Gene gene = renderer.getSelectedGene();
            if (gene != null) detailPane.showGene(gene);
            statusLabel.setText("Genome FASTA loaded: " + sequenceService.getSequenceCount()
                    + " sequences · " + file.getName()
                    + ". Confirm assembly and chromosome names match the annotation.");
        }, this::loadFailed);
    }

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
        pendingLoad = LoadingOperation.run(windowOrNull(), "Importing expression", () -> {
            ExpressionDataService next = new ExpressionDataService(); next.loadFromFile(file.toPath()); next.reconcile(dataService); return next;
        }, next -> { expressionService.replaceWith(next); refreshExpressionViews(); showImportReport(); }, this::loadFailed);
    }

    /** Non-dialog entry points shared by GUI actions and end-to-end tests. */
    public void loadAnnotation(Path file) throws IOException {
        dataService.loadFromFile(file);
        activeExample = null; expressionService.clear(); comparisonPane.clear();
        boolean before = loading; loading = true;
        try { datasetSelector.getSelectionModel().clearSelection(); } finally { loading = before; }
        afterDatasetChanged();
    }
    public void loadExpression(Path file) throws IOException {
        expressionService.loadFromFile(file); expressionService.reconcile(dataService);
        refreshExpressionViews();
    }
    private void refreshExpressionViews() {
        // Keep the browser window and selection when only measurements change.
        detailPane.setExpressionData(expressionService::getProfileForAnnotation, expressionService.globalMaxTpm());
        detailPane.setValueUnit(expressionService.getValueUnit());
        detailPane.setSamples(expressionService.getTissues());
        detailPane.setExpressionLevel(expressionService.getLevel());
        Gene gene = renderer.getSelectedGene(); if (gene != null) detailPane.showGene(gene);
        comparisonPane.refresh(); refreshStatus();
    }
    private void showImportReport() {
        Alert report = new Alert(Alert.AlertType.INFORMATION);
        report.setTitle("Expression import report"); report.setHeaderText("Identifier matching and data quality");
        TextArea text = new TextArea(expressionService.getImportReport().summary());
        text.setEditable(false); text.setWrapText(true); text.setPrefSize(720, 400);
        report.getDialogPane().setContent(text); report.showAndWait();
    }
    public void loadAtlasCase() throws IOException {
        loadExample(ExampleDataset.byId("arabidopsis"));
        expressionService.loadFromResource("/data/case-studies/arabidopsis-atlas/expression.tsv", "Expression Atlas E-MTAB-7978 (single-assay conditions)");
        expressionService.reconcile(dataService); afterDatasetChanged();
        pinAtlasGenes();
    }
    private void pinAtlasGenes() {
        for (String id : List.of("AT1G67090", "AT5G38410", "AT5G38420", "AT5G38430")) {
            Gene gene = dataService.findGene(id);
            if (gene == null) throw new IllegalStateException("Case-study gene absent: " + id);
            comparisonPane.add(gene);
        }
        workspaceTabs.getSelectionModel().select(1);
    }
    public ComparisonPane getComparisonPane() { return comparisonPane; }
    public void saveProject(Path target) throws IOException {
        new ProjectStore().save(target, dataService, expressionService, captureViewState());
    }
    private ProjectStore.ViewState captureViewState() {
        Gene selected = renderer.getSelectedGene(); var settings = comparisonPane.settings();
        return new ProjectStore.ViewState(dataService.getActiveSequence(), selected == null ? null : selected.getId(),
                        renderer.getViewStart(), renderer.getViewEnd(), comparisonPane.geneIds(), settings.alignment().name(),
                        settings.compressed(), settings.scale().name(), searchField.getText());
    }
    public void openProject(Path source) throws IOException {
        ProjectStore.Restored next = new ProjectStore().load(source);
        applyRestoredProject(next);
    }
    private void applyRestoredProject(ProjectStore.Restored next) throws IOException {
        var state = next.view();
        var settings = new ComparisonFigure.Settings(ComparisonFigure.Alignment.valueOf(state.alignment()), state.compressed(),
                ComparisonFigure.Scale.valueOf(state.expressionScale()), true);
        dataService.replaceWith(next.genome()); expressionService.replaceWith(next.expression());
        var previous = restoredProject; restoredProject = next; activeExample = null;
        boolean before = loading; loading = true;
        try { datasetSelector.getSelectionModel().clearSelection(); } finally { loading = before; }

        // ProjectStore restores the saved active sequence on the staged genome.
        // Rebuild the browser from that sequence before restoring the search box;
        // otherwise a cross-chromosome search can leave the renderer on its first hit.
        dataService.setActiveSequence(state.sequence());
        boolean restoreLoading = loading;
        loading = true;
        try {
            afterDatasetChanged();
        } finally {
            loading = restoreLoading;
        }
        if (state.search() != null && !state.search().isBlank()) {
            // Search text is view state, not a reason to change the restored
            // sequence or rebuild the renderer. The saved sequence/window must
            // remain authoritative during project restoration.
            boolean searchLoading = loading;
            loading = true;
            try { searchField.setText(state.search()); }
            finally { loading = searchLoading; }
        }
        if (state.selectedGene() != null) selectGene(dataService.findGene(state.selectedGene()));
        else { geneList.getSelectionModel().clearSelection(); renderer.setSelectedGene(null); detailPane.clear(); }
        renderer.showWindow(state.start(), state.end());
        comparisonPane.restore(state.comparisonGenes(), settings); redraw();
        if (previous != null) try { previous.close(); } catch (IOException ex) { statusLabel.setTooltip(new Tooltip("Previous project cleanup: " + ex.getMessage())); }
    }
    public void closeProjectResources() {
        if (restoredProject != null) try { restoredProject.close(); } catch (IOException ex) { statusLabel.setText(ex.getMessage()); }
    }
    private void chooseProject(boolean save) {
        FileChooser chooser = new FileChooser(); chooser.setTitle(save ? "Save portable project" : "Open project");
        chooser.setInitialFileName("analysis.phytoscope");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PhytoScope project", "*.phytoscope"));
        File file = save ? chooser.showSaveDialog(windowOrNull()) : chooser.showOpenDialog(windowOrNull());
        if (file == null) return;
        if (save) {
            var state = captureViewState();
            pendingLoad = LoadingOperation.run(windowOrNull(), "Saving project", () -> {
                new ProjectStore().save(file.toPath(), dataService, expressionService, state); return file;
            }, done -> statusLabel.setText("Saved project: " + done), this::loadFailed);
        } else pendingLoad = LoadingOperation.run(windowOrNull(), "Restoring project", () -> new ProjectStore().load(file.toPath()), next -> {
            try { applyRestoredProject(next); statusLabel.setText("Restored project: " + file); }
            catch (IOException ex) { throw new java.io.UncheckedIOException(ex); }
        }, this::loadFailed);
    }

    /**
     * Pushes a freshly loaded dataset into every view: the chromosome selector,
     * the renderer, the gene list, the detail pane, the axis caption and the
     * status bar.
     */
    private void afterDatasetChanged() {
        detailPane.setExpressionData(expressionService::getProfileForAnnotation,
                expressionService.globalMaxTpm());
        detailPane.setValueUnit(expressionService.getValueUnit());

        detailPane.setSamples(expressionService.getTissues());
        detailPane.setExpressionLevel(expressionService.getLevel());
        refreshChromosomeSelector();
        showActiveChromosome();
        comparisonPane.refresh();
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
            chromosomeSelector.setDisable(sequences.isEmpty());
            sequenceCountLabel.setText(sequences.size() + (sequences.size() == 1 ? " sequence" : " sequences"));
            sequenceCountLabel.setTooltip(new Tooltip(
                    "Sequences present in this annotation, including scaffolds and organelles.\n"
                            + "An excerpt may contain fewer chromosomes than the whole genome."));
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
        // Menu selection and dropdown selection share one state.
        boolean wasLoading = loading;
        loading = true;
        try {
            chromosomeSelector.getSelectionModel().select(sequence);
        } finally {
            loading = wasLoading;
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
        MenuItem openProject = new MenuItem("Open project…"); openProject.setOnAction(e -> chooseProject(false));
        MenuItem saveProject = new MenuItem("Save project…"); saveProject.setOnAction(e -> chooseProject(true));
        MenuItem quality = new MenuItem("Expression import report…"); quality.setOnAction(e -> showImportReport());
        file.getItems().addAll(openProject, saveProject, new SeparatorMenuItem(), importAnnotationItem, importGenomeFastaItem, importExpressionItem, quality,
                new SeparatorMenuItem(), exportPngItem, exportSvgItem,
                new SeparatorMenuItem(), quit);

        // One entry per bundled species, so the panel is one click away.
        Menu examples = new Menu("Example datasets");
        for (ExampleDataset dataset : ExampleDataset.CATALOGUE) {
            MenuItem item = new MenuItem(dataset.fullLabel());
            item.setOnAction(e -> startExampleLoad(dataset, false));
            examples.getItems().add(item);
        }

        MenuItem atlas = new MenuItem("Real expression case: Arabidopsis RBCS atlas…");
        atlas.setOnAction(e -> startExampleLoad(ExampleDataset.byId("arabidopsis"), true));
        examples.getItems().addAll(new SeparatorMenuItem(), atlas);
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

    private VBox buildToolBar() {
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
        datasetSelector.setId("dataset-selector");
        datasetSelector.setPrefWidth(260);
        datasetSelector.setTooltip(new Tooltip(
                "Switch between the bundled example datasets"));
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
                startExampleLoad(dataset, false);
            }
        });

        // Chromosome selector: the browser shows one sequence at a time, and the
        // list is rebuilt from whatever sequences the loaded data declares.
        chromosomeSelector.setId("chromosome-selector");
        sequenceCountLabel.setId("sequence-count");
        sequenceCountLabel.setMinWidth(Region.USE_PREF_SIZE);
        sequenceCountLabel.setStyle("-fx-text-fill: #6B7B8B; -fx-font-size: 11px;");
        chromosomeSelector.setPrefWidth(175);
        chromosomeSelector.setVisibleRowCount(12);
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
        searchField.setId("gene-search");
        searchField.setPrefWidth(220);
        searchField.textProperty().addListener((obs, old, val) -> {
            if (!loading) applySearch(val);
        });

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

        brandBox.setMinWidth(190);
        datasetSelector.setMinWidth(240); chromosomeSelector.setMinWidth(150);
        bar.getChildren().addAll(brandBox, datasetSelector, chromosomeLabel, chromosomeSelector, sequenceCountLabel);
        HBox actions = new HBox(10, searchField, zoomOut, zoomIn, fitAll, focusSelected, exportMenu);
        actions.setAlignment(Pos.CENTER_LEFT); actions.setPadding(new Insets(0, 14, 10, 14));
        actions.setStyle("-fx-background-color: #FFFFFF;");
        VBox result = new VBox(bar, actions); return result;
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
        applySearch(keyword, true);
    }

    /** Applies a saved query without allowing it to change the restored chromosome. */
    private void applySearch(String keyword, boolean revealAcrossChromosomes) {
        List<Gene> hits = dataService.search(keyword);
        // Interactive searches may reveal the first hit on another chromosome;
        // project restoration deliberately keeps the saved active sequence.
        if (revealAcrossChromosomes && !hits.isEmpty()) {
            revealGeneAcrossChromosomes(hits.get(0));
        }
        List<Gene> visible = hits.stream()
                .filter(g -> java.util.Objects.equals(g.getChromosome(), dataService.getActiveSequence()))
                .toList();
        renderer.setSequenceLabel(dataService.getActiveSequence());
        geneItems.setAll(visible);
        renderer.setGenes(visible);
        renderer.fitToAll();
        redraw();
        if (visible.isEmpty()) {
            geneList.getSelectionModel().clearSelection(); renderer.setSelectedGene(null); detailPane.clear();
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
                if (revealGeneAcrossChromosomes(gene)) {
                    renderer.setSequenceLabel(gene.getChromosome());
                    renderer.setGenes(geneItems.stream().filter(g -> java.util.Objects.equals(g.getChromosome(), gene.getChromosome())).toList());
                    renderer.focusOn(gene, 0.25);
                }
                renderer.setSelectedGene(gene);
                detailPane.showGene(gene);
                redraw();
            }
        });

        Button addComparison = new Button("Add to comparison"); addComparison.setId("add-to-comparison");
        addComparison.setOnAction(e -> { comparisonPane.add(geneList.getSelectionModel().getSelectedItem()); workspaceTabs.getSelectionModel().select(1); });
        box.getChildren().addAll(title, geneList, addComparison);
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

        browserNotice.setWrapText(true); browserNotice.setStyle("-fx-text-fill: #986235; -fx-font-size: 11px;");
        box.getChildren().addAll(hint, browserNotice, browserScroll);
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
                ExpressionProfile profile = expressionService.getProfileForAnnotation(hit.getId());
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
        browserNotice.setText(renderer.renderNotice());
        statusLabel.setText(metadata.getSpecies() + " | " + metadata.getAssembly() + " | "
                + sequenceCount + " | " + dataService.getGeneCount() + " loci | " + expression
                + " | " + (sequence == null ? "No sequence" : sequence) + ": "
                + renderer.getViewStart() + "–" + renderer.getViewEnd());
        statusLabel.setTooltip(new Tooltip(metadata.getSourceLabel()));
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
