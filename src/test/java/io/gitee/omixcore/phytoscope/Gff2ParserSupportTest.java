package io.gitee.omixcore.phytoscope;

import io.gitee.omixcore.phytoscope.model.Gene;
import io.gitee.omixcore.phytoscope.model.GenomicFeature;
import io.gitee.omixcore.phytoscope.model.Transcript;
import io.gitee.omixcore.phytoscope.parser.AnnotationFormat;
import io.gitee.omixcore.phytoscope.parser.Gff3Parser;
import io.gitee.omixcore.phytoscope.parser.ParseDiagnostics;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GFF2 support-boundary tests (R7).
 *
 * <p><b>Support level: compatibility level only.</b> These tests deliberately
 * drive the <em>existing</em> importer {@link Gff3Parser} (the single
 * format-agnostic parser already used for GFF3/GTF), which treats a
 * {@code ##gff-version 2} / {@code *.gff2} file as its GTF dialect. There is
 * <b>no standalone GFF2 parser</b> in this codebase and this suite adds none.</p>
 *
 * <p>Accordingly these are <b>not an independent validation claim</b>: they do
 * not certify full GFF2 specification conformance, do not introduce a new
 * algorithm, and do not compare against an independently implemented GFF2
 * reader. They only pin the observable compatibility behaviour of the existing
 * importer on small, hand-checked fixtures: CDS {@code phase} preservation,
 * transcript boundaries, multi-{@code Parent} CDS attachment, CDS segments
 * without an overlapping exon, and diagnostics for unusable input.</p>
 *
 * <p>Fixtures live in {@code src/test/resources/gff2/}. No production code
 * under {@code src/main/java/**} is modified by this suite.</p>
 */
class Gff2ParserSupportTest {

    /** Reads a committed GFF2 fixture through the existing importer. */
    private static Gff3Parser.ParseResult parseFixture(String name) throws Exception {
        return new Gff3Parser().parseResource("/gff2/" + name, null);
    }

    /** Locates a transcript by id, failing with a readable message if absent. */
    private static Transcript transcript(Gene gene, String id) {
        return gene.getTranscripts().stream()
                .filter(t -> id.equals(t.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("missing transcript: " + id));
    }

    @Test
    void gff2HeaderIsResolvedByTheExistingImporterAsItsGtfDialect() throws Exception {
        // The header, not the file name, declares GFF2; format is left as null
        // so the importer's own detection is exercised.
        String gff2 = "##gff-version 2\n"
                + "chrA\tsrc\tgene\t1\t100\t.\t+\t.\tgene_id \"g1\"; gene_name \"Alpha\";\n";
        var result = new Gff3Parser().parse(
                new ByteArrayInputStream(gff2.getBytes(StandardCharsets.UTF_8)),
                null, "header.gff2");

        assertEquals(AnnotationFormat.GTF, result.format(),
                "existing importer maps a GFF2 header onto its GTF dialect");
        assertEquals(1, result.genes().size());
        assertEquals("g1", result.genes().getFirst().getId());
        assertEquals(AnnotationFormat.GTF, AnnotationFormat.fromFileName("sample.gff2"));
    }

    @Test
    void preservesCdsPhaseAndTranscriptBoundaries() throws Exception {
        var result = parseFixture("cds-phase.gff2");

        assertEquals(AnnotationFormat.GTF, result.format());
        assertEquals(1, result.genes().size());
        Gene gene = result.genes().getFirst();
        assertEquals("g-phase", gene.getId());
        assertEquals("chrA", gene.getChromosome());
        assertEquals(1, gene.getStart());
        assertEquals(500, gene.getEnd());
        assertEquals('+', gene.getStrand());
        assertEquals(1, gene.getTranscripts().size());

        Transcript tx = gene.getTranscripts().getFirst();
        assertEquals("t-phase", tx.getId());
        assertEquals(1, tx.getStart());
        assertEquals(500, tx.getEnd());
        assertEquals(3, tx.getExonCount());

        // Column-8 phase values must survive assembly on each CDS segment.
        List<GenomicFeature> cdsSegments = tx.getAllCdsSegments();
        assertEquals(3, cdsSegments.size());
        assertEquals(List.of("0", "1", "2"),
                cdsSegments.stream().map(GenomicFeature::getPhase).toList());
        assertEquals(162, tx.getCdsLength());
    }

    @Test
    void keepsDistinctTranscriptBoundariesOnMinusStrand() throws Exception {
        var result = parseFixture("transcript-boundaries.gff2");

        assertEquals(AnnotationFormat.GTF, result.format());
        assertEquals(1, result.genes().size());
        Gene gene = result.genes().getFirst();
        assertEquals("g-bound", gene.getId());
        assertEquals(100, gene.getStart());
        assertEquals(1800, gene.getEnd());
        assertEquals('-', gene.getStrand());
        assertEquals(2, gene.getTranscripts().size());

        Transcript shortTx = transcript(gene, "t-short");
        assertEquals(100, shortTx.getStart());
        assertEquals(900, shortTx.getEnd());
        assertEquals(2, shortTx.getExonCount());
        assertEquals(List.of("0"),
                shortTx.getAllCdsSegments().stream().map(GenomicFeature::getPhase).toList());

        Transcript longTx = transcript(gene, "t-long");
        assertEquals(1000, longTx.getStart());
        assertEquals(1800, longTx.getEnd());
        assertEquals(2, longTx.getExonCount());
        assertEquals(List.of("2"),
                longTx.getAllCdsSegments().stream().map(GenomicFeature::getPhase).toList());
    }

    @Test
    void multiParentCdsIsAttachedToBothTranscriptsAndExonlessCdsIsRetained() throws Exception {
        var result = parseFixture("multi-parent-no-exon-cds.gff2");

        assertEquals(AnnotationFormat.GTF, result.format());
        assertEquals(1, result.genes().size());
        Gene gene = result.genes().getFirst();
        assertEquals(2, gene.getTranscripts().size());

        Transcript ta = transcript(gene, "t-a");
        Transcript tb = transcript(gene, "t-b");
        assertEquals(1, ta.getStart());
        assertEquals(600, ta.getEnd());
        assertEquals(200, tb.getStart());
        assertEquals(900, tb.getEnd());

        // One CDS row names two parents ("Parent=t-a,t-b"): the existing
        // importer attaches a copy to each resolved transcript.
        assertTrue(ta.getAllCdsSegments().stream().anyMatch(s -> s.getStart() == 120));
        assertTrue(tb.getAllCdsSegments().stream().anyMatch(s -> s.getStart() == 120));

        // A CDS with no overlapping exon is retained as an unassigned segment;
        // no exon or splice junction is invented for it.
        assertEquals(1, ta.getExonCount());
        assertEquals(1, tb.getExonCount());
        assertEquals(2, ta.getAllCdsSegments().size());
        assertEquals(2, tb.getAllCdsSegments().size());
        assertEquals(1, ta.getUnassignedSegments().size());
        assertEquals(2, tb.getUnassignedSegments().size());
        assertTrue(ta.getUnassignedSegments().stream().anyMatch(s -> s.getStart() == 400));
        assertTrue(tb.getUnassignedSegments().stream().anyMatch(s -> s.getStart() == 400));
    }

    @Test
    void negativeCaseUnresolvedParentAndMalformedRowAreReportedNotSilentlyAccepted() throws Exception {
        var result = parseFixture("negative-orphan.gff2");

        assertEquals(AnnotationFormat.GTF, result.format());
        assertEquals(1, result.genes().size());
        Gene gene = result.genes().getFirst();
        assertEquals(1, gene.getTranscripts().size());
        Transcript ok = transcript(gene, "t-ok");
        assertEquals(1, ok.getExonCount());

        ParseDiagnostics diagnostics = result.diagnostics();
        // (1) A CDS whose transcript_id does not resolve must be recorded as an
        //     orphan, never attached to the surviving transcript.
        assertTrue(diagnostics.orphans().stream().anyMatch(
                        o -> "t-missing".equals(o.missingParentId())
                                && o.target() == ParseDiagnostics.OrphanTarget.TRANSCRIPT),
                "unresolved GFF2 parent must be reported as an orphan");
        assertTrue(ok.getAllCdsSegments().isEmpty(),
                "orphan CDS must not be silently accepted into the gene model");

        // (2) A row with a non-numeric start is skipped and counted, not parsed
        //     into a fabricated feature.
        assertEquals(1, diagnostics.skippedRowTotal());
        assertEquals(1, diagnostics.skippedRows().size());
        assertEquals(ParseDiagnostics.SkipReason.UNUSABLE_COORDINATES_OR_ATTRIBUTES,
                diagnostics.skippedRows().getFirst().reason());
    }
}
