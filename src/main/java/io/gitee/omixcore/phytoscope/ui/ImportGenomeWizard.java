package io.gitee.omixcore.phytoscope.ui;

import io.gitee.omixcore.phytoscope.intake.AliasMapping;
import io.gitee.omixcore.phytoscope.intake.Assembly;
import io.gitee.omixcore.phytoscope.intake.AnnotationMeta;
import io.gitee.omixcore.phytoscope.intake.IntakeFileKind;
import io.gitee.omixcore.phytoscope.intake.IntakeHealthCheck;
import io.gitee.omixcore.phytoscope.intake.IntakeIssue;
import io.gitee.omixcore.phytoscope.intake.IntakeManifestMeta;
import io.gitee.omixcore.phytoscope.intake.IntakeReport;
import io.gitee.omixcore.phytoscope.intake.IntakeSeverity;
import io.gitee.omixcore.phytoscope.intake.IntakeFileResult;
import io.gitee.omixcore.phytoscope.parser.AnnotationFormat;
import io.gitee.omixcore.phytoscope.service.ExpressionDataService;
import io.gitee.omixcore.phytoscope.service.GeneSequenceService;
import io.gitee.omixcore.phytoscope.service.GenomeDataService;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Independent "Import your own genome" wizard.
 *
 * <p>Kept deliberately separate from the Example datasets flow and from the
 * legacy single-file import menu actions. It selects a FASTA + GFF3 (optionally an
 * expression matrix), runs {@link IntakeHealthCheck} before anything is loaded
 * into the main application, presents the report and an alias-confirmation panel,
 * and only then hands the parsed services (plus the confirmed alias map) back to
 * the host through {@link IntakeResultHandler}.</p>
 */
public class ImportGenomeWizard {

    /**
     * Callback invoked when the user accepts a passing health-check. The host
     * injects the parsed services and the confirmed aliases into the running
     * application; it is decoupled from {@code MainView} specifics.
     */
    public interface IntakeResultHandler {
        void accept(GeneSequenceService fasta, GenomeDataService genome, ExpressionDataService expression,
                   List<AliasMapping> aliasMap, IntakeManifestMeta meta);
    }

    private final IntakeResultHandler handler;
    private final Window owner;
    private final Stage stage = new Stage();

    private final TextField fastaField = new TextField();
    private final TextField gff3Field = new TextField();
    private final TextField exprField = new TextField();
    private final Label summaryLabel = new Label("Select a genome FASTA and a GFF3 annotation, then run the health-check.");
    private final TableView<IntakeIssue> issueTable = new TableView<>();
    private final TableView<AliasRow> aliasTable = new TableView<>();
    private final ObservableList<AliasRow> aliasRows = FXCollections.observableArrayList();
    private final Button loadButton = new Button("Load project");
    private final Button runButton = new Button("Run health-check");

    private Path fastaPath;
    private Path gff3Path;
    private Path exprPath;
    private IntakeReport lastReport;

    private final VBox root;

    public ImportGenomeWizard(IntakeResultHandler handler, Window owner) {
        this.handler = handler;
        this.owner = owner;
        this.root = buildScene();
    }

    /** Shows the wizard window (non-blocking). */
    public void show() {
        stage.initOwner(owner);
        stage.setTitle("Import your own genome");
        if (stage.getScene() == null) {
            stage.setScene(new Scene(root, 880, 660));
        }
        stage.show();
    }

    // ------------------------------------------------------------------
    // Programmatic hooks (used by tests and the menu action)
    // ------------------------------------------------------------------

    /** Drives file selection without opening a dialog; used by tests. */
    public void setSelectedFiles(Path fasta, Path gff3, Path expression) {
        this.fastaPath = fasta;
        this.gff3Path = gff3;
        this.exprPath = expression;
        fastaField.setText(fasta == null ? "" : fasta.toString());
        gff3Field.setText(gff3 == null ? "" : gff3.toString());
        exprField.setText(expression == null ? "" : expression.toString());
    }

    /** Runs the health-check and refreshes the report + alias panels. */
    public IntakeReport runHealthCheck() {
        lastReport = IntakeHealthCheck.check(fastaPath, gff3Path, exprPath);
        Platform.runLater(() -> {
            issueTable.setItems(FXCollections.observableArrayList(lastReport.issues()));
            summaryLabel.setText(lastReport.summary());
            loadButton.setDisable(!(lastReport != null && lastReport.pass()));
            populateAliasCandidates();
        });
        return lastReport;
    }

    /** Confirms every proposed alias mapping; used by tests. */
    public void confirmAllAliases() {
        for (AliasRow row : aliasRows) {
            row.confirmed().set(true);
        }
    }

    /** True when the "Load project" action is currently enabled. */
    public boolean isLoadEnabled() {
        return !loadButton.isDisabled();
    }

    /** Returns the most recent report, or null if none has been computed. */
    public IntakeReport getReport() {
        return lastReport;
    }

    /** Writes the JSON and Markdown reports into the given directory. */
    public void exportReport(Path directory) throws IOException {
        if (lastReport == null) {
            lastReport = IntakeHealthCheck.check(fastaPath, gff3Path, exprPath);
        }
        if (!Files.isDirectory(directory)) {
            Files.createDirectories(directory);
        }
        Files.writeString(directory.resolve("intake-health-report.json"), lastReport.toJson());
        Files.writeString(directory.resolve("intake-health-report.md"), lastReport.toMarkdown());
    }

    /** Simulates clicking "Load project"; returns false when blocked by errors. */
    public boolean triggerLoad() {
        if (loadButton.isDisabled()) {
            return false;
        }
        doLoad();
        return true;
    }

    // ------------------------------------------------------------------
    // Internal actions
    // ------------------------------------------------------------------

    private void chooseFasta() {
        Path selected = choose("Genome FASTA", "FASTA", "*.fa", "*.fasta", "*.fna", "*.fa.gz", "*.fasta.gz", "*.fna.gz");
        if (selected != null) {
            fastaPath = selected;
            fastaField.setText(selected.toString());
        }
    }

    private void chooseGff3() {
        Path selected = choose("Annotation (GFF3 / GTF)", "Annotation",
                AnnotationFormat.allExtensions());
        if (selected != null) {
            gff3Path = selected;
            gff3Field.setText(selected.toString());
        }
    }

    private void chooseExpression() {
        Path selected = choose("Expression matrix", "Expression matrix", "*.tsv", "*.csv", "*.txt");
        if (selected != null) {
            exprPath = selected;
            exprField.setText(selected.toString());
        }
    }

    private Path choose(String title, String label, String... extensions) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter(label, extensions),
                new FileChooser.ExtensionFilter("All files", "*.*"));
        File file = chooser.showOpenDialog(stage);
        return file == null ? null : file.toPath();
    }

    private void populateAliasCandidates() {
        aliasRows.clear();
        if (fastaPath == null || gff3Path == null || lastReport == null || !lastReport.pass()) {
            aliasTable.setItems(aliasRows);
            return;
        }
        try {
            GeneSequenceService fasta = IntakeHealthCheck.parseFasta(fastaPath);
            GenomeDataService genome = IntakeHealthCheck.parseAnnotation(gff3Path);
            for (AliasMapping candidate : IntakeHealthCheck.detectAliasCandidates(fasta, genome)) {
                aliasRows.add(new AliasRow(candidate.from(), candidate.to(), true, "auto-normalize"));
            }
        } catch (IOException ignored) {
            // Alias detection is best-effort; parse errors are already reported.
        }
        aliasTable.setItems(aliasRows);
    }

    private void addManualAlias() {
        // Small inline collector reusing the manual-entry fields from the scene.
        if (manualFrom != null && manualTo != null && !manualFrom.getText().isBlank() && !manualTo.getText().isBlank()) {
            aliasRows.add(new AliasRow(manualFrom.getText().trim(), manualTo.getText().trim(), true, "user"));
            aliasTable.setItems(aliasRows);
            manualFrom.clear();
            manualTo.clear();
        }
    }

    private void doLoad() {
        try {
            GeneSequenceService fasta = fastaPath == null ? null : IntakeHealthCheck.parseFasta(fastaPath);
            GenomeDataService genome = gff3Path == null ? null : IntakeHealthCheck.parseAnnotation(gff3Path);
            ExpressionDataService expr = exprPath == null ? null : IntakeHealthCheck.parseExpression(exprPath, genome);
            List<AliasMapping> confirmed = confirmedAliases();
            IntakeManifestMeta meta = buildMeta(fasta, genome, confirmed);
            if (handler != null) {
                handler.accept(fasta, genome, expr, confirmed, meta);
            }
            stage.close();
        } catch (IOException ex) {
            new Alert(Alert.AlertType.ERROR, "Failed to load files: " + ex.getMessage()).showAndWait();
        }
    }

    private List<AliasMapping> confirmedAliases() {
        List<AliasMapping> result = new ArrayList<>();
        for (AliasRow row : aliasRows) {
            String from = row.from().get();
            String to = row.to().get();
            if (row.confirmed().get() && from != null && !from.isBlank()
                    && to != null && !to.isBlank()) {
                result.add(new AliasMapping(from, to, row.confirmedBy()));
            }
        }
        return result;
    }

    private IntakeManifestMeta buildMeta(GeneSequenceService fasta, GenomeDataService genome, List<AliasMapping> aliasMap) {
        Assembly assembly = fasta == null ? null
                : new Assembly(fasta.getSourceLabel() == null ? "imported-assembly" : fasta.getSourceLabel(),
                fastaPath == null ? "" : fastaPath.toString(), fasta.getSequenceNames());
        AnnotationMeta annotationMeta = genome == null ? null
                : new AnnotationMeta(genome.getFormat().name(), "gff3", genome.getCurrentSource());
        List<IntakeFileResult> files = lastReport == null ? List.of() : lastReport.files();
        return new IntakeManifestMeta(IntakeReport.SPEC_VERSION, assembly, annotationMeta, files, aliasMap);
    }

    // ------------------------------------------------------------------
    // Scene construction
    // ------------------------------------------------------------------

    private TextField manualFrom;
    private TextField manualTo;

    private VBox buildScene() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(12));

        HBox fastaRow = fileRow("Genome FASTA (.fa/.fasta/.fna, .gz)", fastaField, this::chooseFasta);
        HBox gff3Row = fileRow("Annotation (GFF3 / GTF)", gff3Field, this::chooseGff3);
        HBox exprRow = fileRow("Expression matrix (.tsv, optional)", exprField, this::chooseExpression);

        runButton.setOnAction(e -> runHealthCheck());

        issueTable.setPrefHeight(180);
        issueTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<IntakeIssue, String> sevCol = new TableColumn<>("Severity");
        sevCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().severity().name()));
        TableColumn<IntakeIssue, String> codeCol = new TableColumn<>("Code");
        codeCol.setCellValueFactory(new PropertyValueFactory<>("code"));
        TableColumn<IntakeIssue, String> fileCol = new TableColumn<>("File");
        fileCol.setCellValueFactory(new PropertyValueFactory<>("file"));
        TableColumn<IntakeIssue, String> locCol = new TableColumn<>("Line/Coord");
        locCol.setCellValueFactory(new PropertyValueFactory<>("lineOrCoord"));
        TableColumn<IntakeIssue, String> msgCol = new TableColumn<>("Message");
        msgCol.setCellValueFactory(new PropertyValueFactory<>("message"));
        issueTable.getColumns().addAll(sevCol, codeCol, fileCol, locCol, msgCol);

        aliasTable.setPrefHeight(120);
        aliasTable.setEditable(true);
        TableColumn<AliasRow, String> fromCol = new TableColumn<>("Annotation seqid");
        fromCol.setCellValueFactory(new PropertyValueFactory<>("from"));
        TableColumn<AliasRow, String> toCol = new TableColumn<>("FASTA id");
        toCol.setCellValueFactory(new PropertyValueFactory<>("to"));
        toCol.setCellFactory(TextFieldTableCell.forTableColumn());
        toCol.setOnEditCommit(e -> e.getRowValue().to().set(e.getNewValue() == null ? "" : e.getNewValue().trim()));
        TableColumn<AliasRow, Boolean> confirmCol = new TableColumn<>("Confirm");
        confirmCol.setCellValueFactory(c -> c.getValue().confirmed());
        confirmCol.setCellFactory(CheckBoxTableCell.forTableColumn(confirmCol));
        aliasTable.getColumns().addAll(fromCol, toCol, confirmCol);

        manualFrom = new TextField();
        manualFrom.setPromptText("Annotation seqid (e.g. 1)");
        manualTo = new TextField();
        manualTo.setPromptText("FASTA id (e.g. Chr1)");
        Button addAliasButton = new Button("Add manual alias");
        addAliasButton.setOnAction(e -> addManualAlias());
        HBox manualRow = new HBox(6, new Label("Manual alias:"), manualFrom, manualTo, addAliasButton);

        loadButton.setDisable(true);
        loadButton.setOnAction(e -> doLoad());
        Button exportJsonButton = new Button("Export JSON");
        exportJsonButton.setOnAction(e -> exportToDirectory("json"));
        Button exportMdButton = new Button("Export Markdown");
        exportMdButton.setOnAction(e -> exportToDirectory("md"));
        Button closeButton = new Button("Close");
        closeButton.setOnAction(e -> stage.close());
        HBox actionRow = new HBox(8, runButton, exportJsonButton, exportMdButton, loadButton, closeButton);

        root.getChildren().addAll(
                new Label("Import your own genome — bring FASTA + GFF3 (+ optional expression matrix)"),
                fastaRow, gff3Row, exprRow, runButton, summaryLabel,
                new Label("Health-check issues"), issueTable,
                new Label("Sequence-name aliases (confirm to record in the project)"), manualRow, aliasTable,
                new Separator(), actionRow);
        return root;
    }

    private HBox fileRow(String label, TextField field, Runnable onChoose) {
        field.setEditable(false);
        field.setPrefWidth(420);
        Button button = new Button("Choose…");
        button.setOnAction(e -> onChoose.run());
        HBox row = new HBox(6, new Label(label), field, button);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        HBox.setHgrow(field, Priority.ALWAYS);
        return row;
    }

    private void exportToDirectory(String kind) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose directory for the health-check report");
        File dirFile = chooser.showDialog(stage);
        if (dirFile == null) {
            return;
        }
        Path dir = dirFile.toPath();
        try {
            exportReport(dir);
            new Alert(Alert.AlertType.INFORMATION, "Report exported to " + dir).showAndWait();
        } catch (IOException ex) {
            new Alert(Alert.AlertType.ERROR, "Export failed: " + ex.getMessage()).showAndWait();
        }
    }

    /** One alias row in the confirmation table. */
    public static final class AliasRow {
        private final StringProperty from = new SimpleStringProperty();
        private final StringProperty to = new SimpleStringProperty();
        private final BooleanProperty confirmed = new SimpleBooleanProperty();
        private final String confirmedBy;

        public AliasRow(String from, String to, boolean confirmed, String confirmedBy) {
            this.from.set(from);
            this.to.set(to);
            this.confirmed.set(confirmed);
            this.confirmedBy = confirmedBy == null ? "user" : confirmedBy;
        }

        public StringProperty from() {
            return from;
        }

        public StringProperty to() {
            return to;
        }

        public BooleanProperty confirmed() {
            return confirmed;
        }

        public String confirmedBy() {
            return confirmedBy;
        }
    }
}
