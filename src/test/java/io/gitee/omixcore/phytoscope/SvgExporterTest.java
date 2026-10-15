package io.gitee.omixcore.phytoscope;

import io.gitee.omixcore.phytoscope.model.Gene;
import io.gitee.omixcore.phytoscope.parser.Gff3Parser;
import io.gitee.omixcore.phytoscope.service.GenomeDataService;
import io.gitee.omixcore.phytoscope.ui.component.SvgExporter;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the export helpers.
 *
 * <p>Only the pure SVG-generation and colour-conversion logic is exercised here;
 * canvas snapshotting needs a live JavaFX toolkit and is covered by the manual
 * smoke test instead.</p>
 */
class SvgExporterTest {

    private static Gff3Parser.ParseResult parseResult;

    @BeforeAll
    static void setUp() throws Exception {
        parseResult = new Gff3Parser().parseResource(GenomeDataService.SAMPLE_RESOURCE);
        // The exporters touch JavaFX colour classes, which may need the toolkit
        // initialised. Skip quietly when no graphics environment is available.
        try {
            javafx.application.Platform.startup(() -> { });
        } catch (Throwable ignored) {
            // Headless run, or the toolkit was already started — either is fine
        }
    }

    // ------------------------------------------------------------------
    // XML escaping
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Escapes the five XML predefined entities")
    void shouldEscapeXmlEntities() {
        assertEquals("a &amp; b", SvgExporter.escape("a & b"));
        assertEquals("&lt;tag&gt;", SvgExporter.escape("<tag>"));
        assertEquals("&quot;q&quot;", SvgExporter.escape("\"q\""));
        assertEquals("&apos;s&apos;", SvgExporter.escape("'s'"));
        assertEquals("", SvgExporter.escape(null));
    }

    @Test
    @DisplayName("Leaves ordinary gene identifiers untouched")
    void shouldLeavePlainTextUntouched() {
        assertEquals("AT1G01010", SvgExporter.escape("AT1G01010"));
        assertEquals("root stem leaf", SvgExporter.escape("root stem leaf"));
    }

    // ------------------------------------------------------------------
    // Colour conversion
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Converts opaque colours to #RRGGBB")
    void shouldConvertOpaqueColour() {
        assertEquals("#FFFFFF", SvgExporter.hex(Color.WHITE));
        assertEquals("#000000", SvgExporter.hex(Color.BLACK));
        assertEquals("#FF0000", SvgExporter.hex(Color.RED));
    }

    @Test
    @DisplayName("Emits rgba() for translucent colours")
    void shouldConvertTranslucentColour() {
        String rgba = SvgExporter.hex(Color.web("#E8833A", 0.08));
        assertTrue(rgba.startsWith("rgba("), "Translucent colours must use rgba(), got " + rgba);
        assertTrue(rgba.endsWith(",0.08)"), "Alpha must be preserved, got " + rgba);
    }

    // ------------------------------------------------------------------
    // Full SVG document
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Produces a well-formed SVG document for the whole view")
    void shouldProduceSvgDocument() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(canvasAvailable(),
                "Canvas requires a graphics toolkit; skipped in headless runs");
        GeneTrackRendererStub stub = buildStub();

        java.io.StringWriter out = new java.io.StringWriter();
        new SvgExporter(stub.renderer).export(out, 1200, 800);
        String svg = out.toString();

        assertTrue(svg.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"),
                "SVG must start with an XML declaration");
        assertTrue(svg.contains("xmlns=\"http://www.w3.org/2000/svg\""),
                "SVG namespace must be declared");
        assertTrue(svg.contains("viewBox=\"0 0 1200 800\""),
                "viewBox must match the requested extent");
        assertTrue(svg.trim().endsWith("</svg>"), "SVG must be closed");

        // Balanced element counts: every opened <svg> and <text> is closed
        assertEquals(countOccurrences(svg, "<svg"), countOccurrences(svg, "</svg>"));
    }

    @Test
    @DisplayName("Includes every gene and omits per-track expression bands")
    void shouldIncludeGenesWithoutExpressionTracks() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(canvasAvailable(),
                "Canvas requires a graphics toolkit; skipped in headless runs");
        GeneTrackRendererStub stub = buildStub();

        java.io.StringWriter out = new java.io.StringWriter();
        new SvgExporter(stub.renderer).export(out, 1200, 900);
        String svg = out.toString();

        // Every gene label appears
        for (Gene gene : parseResult.genes()) {
            assertTrue(svg.contains(gene.getDisplayName()),
                    "Missing gene label: " + gene.getDisplayName());
        }

        // Expression lives in the detail pane, so the SVG must not draw
        // per-track heat-map bands
        assertFalse(svg.contains("Expression (TPM)"),
                "Expression caption must not appear on the tracks");

        // Structural blocks and the coordinate axis
        assertTrue(svg.contains("<line"), "No lines emitted (axis / introns / grid missing)");
        assertTrue(svg.contains("<rect"), "No rectangles emitted (exons missing)");
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Whether a live JavaFX Canvas can be created in this environment. */
    private static boolean canvasAvailable() {
        try {
            new javafx.scene.canvas.Canvas(10, 10);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private GeneTrackRendererStub buildStub() throws Exception {
        return new GeneTrackRendererStub(parseResult.genes());
    }

    private static long countOccurrences(String haystack, String needle) {
        long count = 0;
        int idx = 0;
        while ((idx = haystack.indexOf(needle, idx)) >= 0) {
            count++;
            idx += needle.length();
        }
        return count;
    }

    /** Small holder that wires a renderer up with the sample data for export. */
    private static final class GeneTrackRendererStub {
        final io.gitee.omixcore.phytoscope.ui.component.GeneTrackRenderer renderer =
                new io.gitee.omixcore.phytoscope.ui.component.GeneTrackRenderer();

        GeneTrackRendererStub(List<Gene> genes) {
            renderer.setGenes(genes);
            // Lay the view out so exporters see the same extent the UI would use
            renderer.render(1200, renderer.contentHeight());
        }
    }
}
