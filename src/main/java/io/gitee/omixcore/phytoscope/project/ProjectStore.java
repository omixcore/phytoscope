package io.gitee.omixcore.phytoscope.project;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gitee.omixcore.phytoscope.BuildInfo;
import io.gitee.omixcore.phytoscope.intake.AliasMapping;
import io.gitee.omixcore.phytoscope.intake.AnnotationMeta;
import io.gitee.omixcore.phytoscope.intake.Assembly;
import io.gitee.omixcore.phytoscope.intake.IntakeFileResult;
import io.gitee.omixcore.phytoscope.intake.IntakeManifestMeta;
import io.gitee.omixcore.phytoscope.intake.IntakeReport;
import io.gitee.omixcore.phytoscope.parser.AnnotationFormat;
import io.gitee.omixcore.phytoscope.service.ExpressionDataService;
import io.gitee.omixcore.phytoscope.service.GenomeDataService;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.zip.*;

/** Portable, versioned projects. All source bytes are embedded and SHA-256 checked before parsing. */
public final class ProjectStore {
    private static final ObjectMapper JSON = configureMapper(new ObjectMapper());
    private static final long MAX_BYTES = 2_000_000_000L;

    /** Backward/forward compatible: ignore unknown manifest fields so older projects still load. */
    private static ObjectMapper configureMapper(ObjectMapper mapper) {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }
    public record ViewState(String sequence, String selectedGene, int start, int end,
                            List<String> comparisonGenes, String alignment, boolean compressed,
                            String expressionScale, String search) { }
    public record Source(String entry, String origin, String sha256) { }
    public record ResearchState(int upstream, int downstream, String intervalFormat,
                                Map<String, String> sampleGroups, String homologySpecies,
                                double homologyIdentity, String domainProtein, String domainName,
                                int domainStartAa, int domainEndAa, String lastResult,
                                String customInterval, String qcSequence, String proteinFasta, String domainInput) {
        public ResearchState(int upstream, int downstream, String intervalFormat,
                             Map<String, String> sampleGroups, String homologySpecies,
                             double homologyIdentity, String domainProtein, String domainName,
                             int domainStartAa, int domainEndAa, String lastResult) {
            this(upstream, downstream, intervalFormat, sampleGroups, homologySpecies, homologyIdentity,
                    domainProtein, domainName, domainStartAa, domainEndAa, lastResult, "", "", "", "");
        }
        public ResearchState { sampleGroups = sampleGroups == null ? Map.of() : Map.copyOf(sampleGroups); }
        public static ResearchState defaults() { return new ResearchState(2000, 200, "BED", Map.of(), "", 30.0, "", "", 1, 1, "", "", "", "", ""); }
    }
    /**
     * Project manifest. The first eight fields are the historical schema; the
     * trailing five are the manifest v2 additions: {@code specVersion}, assembly
     * identity, annotation metadata, per-file integrity results and the confirmed
     * sequence-name alias map. Schema version is bumped to 4 when v2 data is
     * written; {@link #load(Path)} accepts 1..4 and tolerates null v2 fields so
     * older projects keep opening.
     */
    public record Manifest(int schemaVersion, String applicationVersion, String annotationFormat,
                           List<Source> annotations, Source expression, ViewState view,
                           ExpressionDataService.ImportReport importReport, ResearchState research,
                           String specVersion, Assembly assembly, AnnotationMeta annotationMeta,
                           List<IntakeFileResult> files, List<AliasMapping> aliasMap) {

        /** JSON creator tolerant of older manifests that omit the v2 fields. */
        @JsonCreator
        public static Manifest fromJson(
                @JsonProperty("schemaVersion") int schemaVersion,
                @JsonProperty("applicationVersion") String applicationVersion,
                @JsonProperty("annotationFormat") String annotationFormat,
                @JsonProperty("annotations") List<Source> annotations,
                @JsonProperty("expression") Source expression,
                @JsonProperty("view") ViewState view,
                @JsonProperty("importReport") ExpressionDataService.ImportReport importReport,
                @JsonProperty("research") ResearchState research,
                @JsonProperty(value = "specVersion", required = false) String specVersion,
                @JsonProperty(value = "assembly", required = false) Assembly assembly,
                @JsonProperty(value = "annotationMeta", required = false) AnnotationMeta annotationMeta,
                @JsonProperty(value = "files", required = false) List<IntakeFileResult> files,
                @JsonProperty(value = "aliasMap", required = false) List<AliasMapping> aliasMap) {
            return new Manifest(schemaVersion, applicationVersion, annotationFormat, annotations, expression,
                    view, importReport, research, specVersion, assembly, annotationMeta, files, aliasMap);
        }
    }
    public record Restored(GenomeDataService genome, ExpressionDataService expression,
                           ViewState view, Manifest manifest, Path workspace) implements AutoCloseable {
        @Override public void close() throws IOException { deleteTree(workspace); }
    }

    public void save(Path target, GenomeDataService genome, ExpressionDataService expression,
                     ViewState view) throws IOException {
        save(target, genome, expression, view, ResearchState.defaults());
    }
    public void save(Path target, GenomeDataService genome, ExpressionDataService expression,
                     ViewState view, ResearchState research) throws IOException {
        if (genome.getSourceInputs().isEmpty()) throw new IOException("Import a file or bundled reference before saving");
        Path absolute = target.toAbsolutePath();
        Path temp = Files.createTempFile(absolute.getParent(), ".phytoscope-", ".tmp");
        try {
            try (var zip = new ZipOutputStream(Files.newOutputStream(temp))) {
                List<Source> sources = new ArrayList<>();
                int i = 0;
                for (var source : genome.getSourceInputs()) {
                    String entry = "annotation/part-" + (++i) + ".txt";
                    try (var in = source.open()) {
                        Source written = write(zip, entry, source.location(), in);
                        if (!written.sha256().equals(source.sha256())) throw new IOException("Annotation changed since import: " + source.location() + "; reload it before saving");
                        sources.add(written);
                    }
                }
                Source matrix = null;
                if (!expression.isEmpty()) {
                    matrix = write(zip, "expression.tsv", expression.getSource(),
                            new ByteArrayInputStream(expression.serializeMatrix().getBytes(StandardCharsets.UTF_8)));
                }
                Manifest manifest = new Manifest(3, BuildInfo.version(), genome.getFormat().name(), List.copyOf(sources), matrix, view, expression.getImportReport(), research,
                        null, null, null, null, null);
                zip.putNextEntry(new ZipEntry("project.json"));
                zip.write(JSON.writerWithDefaultPrettyPrinter().writeValueAsBytes(manifest));
                zip.closeEntry();
            }
            checkCancelled();
            try { Files.move(temp, absolute, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(temp, absolute, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temp); }
    }

    /**
     * Saves a project in manifest v2 (schemaVersion 4) carrying the import
     * health-check metadata: specification version, assembly identity, annotation
     * metadata, per-file integrity results and the confirmed sequence-name alias
     * map. SHA-256 is computed and verified by {@link #load(Path)} exactly as for
     * the v3 path, so the manifest remains tamper-evident.
     */
    public void save(Path target, GenomeDataService genome, ExpressionDataService expression,
                     ViewState view, ResearchState research, IntakeManifestMeta meta) throws IOException {
        if (genome.getSourceInputs().isEmpty()) throw new IOException("Import a file or bundled reference before saving");
        Path absolute = target.toAbsolutePath();
        Path temp = Files.createTempFile(absolute.getParent(), ".phytoscope-", ".tmp");
        try {
            try (var zip = new ZipOutputStream(Files.newOutputStream(temp))) {
                List<Source> sources = new ArrayList<>();
                int i = 0;
                for (var source : genome.getSourceInputs()) {
                    String entry = "annotation/part-" + (++i) + ".txt";
                    try (var in = source.open()) {
                        Source written = write(zip, entry, source.location(), in);
                        if (!written.sha256().equals(source.sha256())) throw new IOException("Annotation changed since import: " + source.location() + "; reload it before saving");
                        sources.add(written);
                    }
                }
                Source matrix = null;
                if (!expression.isEmpty()) {
                    matrix = write(zip, "expression.tsv", expression.getSource(),
                            new ByteArrayInputStream(expression.serializeMatrix().getBytes(StandardCharsets.UTF_8)));
                }
                Manifest manifest = new Manifest(4, BuildInfo.version(), genome.getFormat().name(), List.copyOf(sources), matrix, view,
                        expression.getImportReport(), research,
                        IntakeReport.SPEC_VERSION,
                        meta == null ? null : meta.assembly(),
                        meta == null ? null : meta.annotationMeta(),
                        meta == null ? List.of() : meta.files(),
                        meta == null ? List.of() : meta.aliasMap());
                zip.putNextEntry(new ZipEntry("project.json"));
                zip.write(JSON.writerWithDefaultPrettyPrinter().writeValueAsBytes(manifest));
                zip.closeEntry();
            }
            checkCancelled();
            try { Files.move(temp, absolute, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(temp, absolute, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temp); }
    }

    public Restored load(Path source) throws IOException {
        Path workspace = Files.createTempDirectory("phytoscope-project-");
        try (var zip = new ZipFile(source.toFile())) {
            var manifestEntry = zip.getEntry("project.json");
            if (manifestEntry == null || manifestEntry.getSize() > 4_000_000) throw new IOException("Invalid project manifest");
            Manifest m;
            try (var in = zip.getInputStream(manifestEntry)) { m = JSON.readValue(in.readNBytes(4_000_001), Manifest.class); }
            if ((m.schemaVersion() < 1 || m.schemaVersion() > 4) || m.annotations() == null || m.annotations().isEmpty() || m.view() == null)
                throw new IOException("Unsupported or incomplete project schema");
            if (m.view().start() < 1 || m.view().end() < m.view().start()) throw new IOException("Invalid saved coordinate window");
            io.gitee.omixcore.phytoscope.comparison.ComparisonFigure.Alignment.valueOf(m.view().alignment());
            io.gitee.omixcore.phytoscope.comparison.ComparisonFigure.Scale.valueOf(m.view().expressionScale());
            if (m.view().comparisonGenes() == null || m.view().comparisonGenes().size() > 100) throw new IOException("Invalid saved comparison selection");
            List<Path> parts = new ArrayList<>();
            long[] budget = {MAX_BYTES};
            for (Source item : m.annotations()) parts.add(extract(zip, item, workspace, budget));
            Path matrix = m.expression() == null ? null : extract(zip, m.expression(), workspace, budget);
            GenomeDataService genome = new GenomeDataService();
            genome.loadParts(parts, AnnotationFormat.valueOf(m.annotationFormat()));
            if (!genome.getChromosomes().contains(m.view().sequence())) throw new IOException("Saved sequence is absent from annotation");
            genome.setActiveSequence(m.view().sequence());
            if (m.view().selectedGene() != null && genome.findGene(m.view().selectedGene()) == null) throw new IOException("Saved gene is absent");
            for (String id : m.view().comparisonGenes()) if (genome.findGene(id) == null) throw new IOException("Saved comparison gene is absent: " + id);
            ExpressionDataService expression = new ExpressionDataService();
            if (matrix != null) { expression.loadFromFile(matrix); expression.reconcile(genome); expression.restoreImportAudit(m.importReport(), m.expression().origin()); }
            if (m.research() == null) m = new Manifest(m.schemaVersion(), m.applicationVersion(), m.annotationFormat(), m.annotations(), m.expression(), m.view(), m.importReport(), ResearchState.defaults(),
                    m.specVersion(), m.assembly(), m.annotationMeta(), m.files(), m.aliasMap());
            return new Restored(genome, expression, m.view(), m, workspace);
        } catch (Exception ex) {
            deleteTree(workspace);
            if (ex instanceof IOException io) throw io;
            throw new IOException("Invalid project: " + ex.getMessage(), ex);
        }
    }

    private static Source write(ZipOutputStream zip, String entry, String origin, InputStream input) throws IOException {
        MessageDigest digest = sha();
        zip.putNextEntry(new ZipEntry(entry));
        byte[] buffer = new byte[65536]; int n;
        while ((n = input.read(buffer)) >= 0) { checkCancelled(); digest.update(buffer, 0, n); zip.write(buffer, 0, n); }
        zip.closeEntry();
        return new Source(entry, origin, HexFormat.of().formatHex(digest.digest()));
    }
    private static Path extract(ZipFile zip, Source source, Path workspace, long[] budget) throws IOException {
        if (source.entry() == null || source.sha256() == null) throw new IOException("Invalid source record");
        Path target = workspace.resolve(source.entry()).normalize();
        if (!target.startsWith(workspace) || Files.exists(target)) throw new IOException("Invalid or duplicate project path");
        var entry = zip.getEntry(source.entry());
        if (entry == null) throw new IOException("Missing source " + source.entry());
        Files.createDirectories(target.getParent());
        MessageDigest digest = sha();
        try (var in = zip.getInputStream(entry); var out = Files.newOutputStream(target)) {
            byte[] buffer = new byte[65536]; int n;
            while ((n = in.read(buffer)) >= 0) {
                checkCancelled();
                budget[0] -= n;
                if (budget[0] < 0) throw new IOException("Project exceeds 2 GB uncompressed limit");
                digest.update(buffer, 0, n); out.write(buffer, 0, n);
            }
        }
        if (!source.sha256().equals(HexFormat.of().formatHex(digest.digest()))) throw new IOException("Checksum mismatch: " + source.entry());
        return target;
    }
    private static void checkCancelled() throws java.io.InterruptedIOException {
        if (Thread.currentThread().isInterrupted()) throw new java.io.InterruptedIOException("Project operation cancelled");
    }
    private static MessageDigest sha() { try { return MessageDigest.getInstance("SHA-256"); } catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); } }
    private static void deleteTree(Path path) throws IOException {
        if (Files.exists(path)) try (var paths = Files.walk(path)) {
            for (Path p : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(p);
        }
    }
}
