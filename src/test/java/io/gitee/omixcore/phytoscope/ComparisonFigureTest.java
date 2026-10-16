package io.gitee.omixcore.phytoscope;

import io.gitee.omixcore.phytoscope.comparison.ComparisonFigure;
import io.gitee.omixcore.phytoscope.model.*;
import io.gitee.omixcore.phytoscope.service.*;
import org.junit.jupiter.api.Test;
import java.io.StringWriter;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ComparisonFigureTest {
    private Gene gene(char strand) {
        Gene g = new Gene("A&1", "example", "scaffold_7", 101, 2000, strand);
        Transcript t = new Transcript("A.1", "scaffold_7", 101, 2000, strand);
        Exon a = new Exon("e1", "scaffold_7", 101, 200, strand), b = new Exon("e2", "scaffold_7", 1801, 2000, strand);
        a.addCds(new GenomicFeature("c1", "scaffold_7", FeatureType.CDS, 151, 200, strand));
        b.addCds(new GenomicFeature("c2", "scaffold_7", FeatureType.CDS, 1801, 1900, strand));
        t.addExon(a); t.addExon(b); g.addTranscript(t); return g;
    }
    @Test void coordinateTransformKeepsInclusiveLengthsAndMinusDirection() {
        var defaults = ComparisonFigure.Settings.defaults();
        var plus = new ComparisonFigure.Axis(gene('+'), defaults); var minus = new ComparisonFigure.Axis(gene('-'), defaults);
        assertEquals(0, plus.position(101)); assertEquals(1900, plus.position(2001));
        assertEquals(0, minus.position(2001)); assertEquals(1900, minus.position(101));
        var cds = new ComparisonFigure.Axis(gene('-'), new ComparisonFigure.Settings(ComparisonFigure.Alignment.CDS_START, false, ComparisonFigure.Scale.LINEAR, true));
        assertEquals(0, cds.position(1901)); assertEquals(-100, cds.position(2001));
        var compressed = new ComparisonFigure.Axis(gene('+'), new ComparisonFigure.Settings(ComparisonFigure.Alignment.TRANSCRIPTION_START, true, ComparisonFigure.Scale.LINEAR, true));
        assertEquals(380, compressed.position(2001));
    }
    @Test void svgAndTablePreserveCoordinatesAndEscapeIdentifiers() throws Exception {
        var figure = new ComparisonFigure().build(List.of(gene('+'), gene('-')), new ExpressionDataService(), ComparisonFigure.Settings.defaults());
        StringWriter svg = new StringWriter(); figure.writeSvg(svg);
        assertTrue(svg.toString().contains("A.1")); assertTrue(figure.table().contains("\"151\"\t\"200\""));
        javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new java.io.ByteArrayInputStream(svg.toString().getBytes()));
        assertTrue(figure.marks().stream().filter(m -> m.kind().equals("rect")).allMatch(m -> m.width() >= 0));
    }
}
