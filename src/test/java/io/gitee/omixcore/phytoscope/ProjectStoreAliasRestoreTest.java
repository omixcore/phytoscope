package io.gitee.omixcore.phytoscope;

import io.gitee.omixcore.phytoscope.intake.AliasMapping;
import io.gitee.omixcore.phytoscope.intake.Assembly;
import io.gitee.omixcore.phytoscope.intake.IntakeManifestMeta;
import io.gitee.omixcore.phytoscope.intake.IntakeReport;
import io.gitee.omixcore.phytoscope.project.ProjectStore;
import io.gitee.omixcore.phytoscope.service.ExpressionDataService;
import io.gitee.omixcore.phytoscope.service.GeneSequenceService;
import io.gitee.omixcore.phytoscope.service.GenomeDataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Acceptance tests for the manifest v2 alias persistence (AC-2) and the
 * backward/forward compatible project schema (AC-5).
 */
class ProjectStoreAliasRestoreTest {

    private static Path fixture(String name) {
        return Path.of("src/test/resources/intake", name);
    }

    private ProjectStore.ViewState viewState(String sequence) {
        return new ProjectStore.ViewState(sequence, null, 1, 500, List.of(),
                "GENOMIC_START", false, "LINEAR", "");
    }

    /** AC-2: an alias map written by the v2 save survives a load. */
    @Test
    void aliasMapSurvivesProjectRoundTrip(@TempDir Path temp) throws Exception {
        var genome = new GenomeDataService();
        genome.loadFromFile(fixture("chr_level.gff3"));
        var expression = new ExpressionDataService();
        expression.loadFromFile(fixture("chr_level.tsv"));
        expression.reconcile(genome);

        List<AliasMapping> aliases = List.of(
                new AliasMapping("1", "Chr1", "auto-normalize"),
                new AliasMapping("2", "Chr2", "user"));
        Assembly assembly = new Assembly("test-assembly", "chr_level.fa", genome.getChromosomes());
        IntakeManifestMeta meta = new IntakeManifestMeta(IntakeReport.SPEC_VERSION, assembly,
                new io.gitee.omixcore.phytoscope.intake.AnnotationMeta("GFF3", "v1", "chr_level.gff3"),
                List.of(), aliases);

        Path project = temp.resolve("with-aliases.phytoscope");
        var store = new ProjectStore();
        store.save(project, genome, expression, viewState("Chr1"), new ProjectStore.ResearchState(
                2000, 200, "BED", Map.of(), "", 30.0, "", "", 1, 1, ""), meta);

        try (var restored = store.load(project)) {
            List<AliasMapping> restoredAliases = restored.manifest().aliasMap();
            assertNotNull(restoredAliases, "alias map must be present after load");
            assertEquals(2, restoredAliases.size(), "both aliases must survive");
            assertTrue(restoredAliases.stream().anyMatch(a -> "1".equals(a.from()) && "Chr1".equals(a.to())),
                    "annotation '1' -> FASTA 'Chr1' must be restored");
            assertTrue(restoredAliases.stream().anyMatch(a -> "2".equals(a.from()) && "Chr2".equals(a.to())),
                    "annotation '2' -> FASTA 'Chr2' must be restored");
            assertEquals(IntakeReport.SPEC_VERSION, restored.manifest().specVersion());
            assertEquals("test-assembly", restored.manifest().assembly().id());
        }
    }

    /** AC-2 (unit): a declared alias lets a differently-named id resolve. */
    @Test
    void aliasResolutionWorksAfterAddAliases() throws Exception {
        GeneSequenceService fasta = new GeneSequenceService();
        fasta.loadFromFile(fixture("chr_level.fa"));
        // "CHROM1" normalises to "om1", which does NOT match FASTA id "Chr1" ("1"),
        // so it must fail before the alias is declared.
        assertThrows(Exception.class, () -> fasta.extractInterval("CHROM1", 1, 100, '+'));
        fasta.addAliases(Map.of("CHROM1", "Chr1"));
        String resolved = fasta.extractInterval("CHROM1", 1, 100, '+');
        assertNotNull(resolved);
        assertEquals(100, resolved.length(), "alias 'CHROM1' must resolve to Chr1 and yield 100 bases");
    }

    /** AC-5: a project saved with the legacy (schemaVersion 3) path still loads. */
    @Test
    void legacyV3ProjectStillLoads(@TempDir Path temp) throws Exception {
        var genome = new GenomeDataService();
        genome.loadFromFile(fixture("chr_level.gff3"));
        var expression = new ExpressionDataService();
        expression.loadFromFile(fixture("chr_level.tsv"));
        expression.reconcile(genome);

        Path project = temp.resolve("legacy.phytoscope");
        new ProjectStore().save(project, genome, expression, viewState("Chr1"));

        try (var restored = new ProjectStore().load(project)) {
            assertEquals(3, restored.manifest().schemaVersion());
            assertEquals("Chr1", restored.genome().getActiveSequence());
            // Legacy projects carry no alias map; the loader must tolerate null.
            assertTrue(restored.manifest().aliasMap() == null
                    || restored.manifest().aliasMap().isEmpty());
        }
    }

    /** AC-5: an older schemaVersion (2) manifest still loads (tolerant schema). */
    @Test
    void olderSchemaVersionLoads(@TempDir Path temp) throws Exception {
        var genome = new GenomeDataService();
        genome.loadFromFile(fixture("chr_level.gff3"));
        var expression = new ExpressionDataService();
        expression.loadFromFile(fixture("chr_level.tsv"));
        expression.reconcile(genome);

        Path v3 = temp.resolve("v3.phytoscope");
        new ProjectStore().save(v3, genome, expression, viewState("Chr1"));

        // Downgrade the embedded manifest to schemaVersion 2 (older client) and
        // repack; load() must still accept it (guard allows 1..4, unknown fields
        // are ignored).
        Path v2 = temp.resolve("v2.phytoscope");
        downgradeSchema(v3, v2, 3, 2);

        try (var restored = new ProjectStore().load(v2)) {
            assertEquals(2, restored.manifest().schemaVersion());
            assertEquals("Chr1", restored.genome().getActiveSequence());
        }
    }

    private static void downgradeSchema(Path source, Path target, int from, int to) throws Exception {
        try (var zip = new ZipFile(source.toFile());
             var out = new ZipOutputStream(java.nio.file.Files.newOutputStream(target))) {
            for (var entry : Collections.list(zip.entries())) {
                out.putNextEntry(new ZipEntry(entry.getName()));
                if (entry.getName().equals("project.json")) {
                    byte[] bytes = zip.getInputStream(entry).readAllBytes();
                    String json = new String(bytes, java.nio.charset.StandardCharsets.UTF_8)
                            .replace("\"schemaVersion\" : " + from, "\"schemaVersion\" : " + to);
                    out.write(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                } else {
                    zip.getInputStream(entry).transferTo(out);
                }
                out.closeEntry();
            }
        }
    }
}
