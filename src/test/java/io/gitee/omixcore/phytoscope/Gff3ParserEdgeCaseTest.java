package io.gitee.omixcore.phytoscope;

import io.gitee.omixcore.phytoscope.model.FeatureType;
import io.gitee.omixcore.phytoscope.parser.AnnotationFormat;
import io.gitee.omixcore.phytoscope.parser.Gff3Parser;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class Gff3ParserEdgeCaseTest {

    @Test
    void preservesBlankColumnsAndHeaderDeclaredGff3Format() throws Exception {
        var result = new Gff3Parser().parse(new ByteArrayInputStream("""
                ##gff-version 3
                ##sequence-region chrA 1 1000
                chrA\tsrc\tgene\t100\t600\t.\t+\t.\tID=g1;Name=Alpha
                chrA\tsrc\tmRNA\t100\t600\t.\t+\t.\tID=t1;Parent=g1
                chrA\tsrc\texon\t100\t200\t.\t+\t.\tID=e1;Parent=t1
                """.getBytes(StandardCharsets.UTF_8)), null, "edge.gff3");
        assertEquals(AnnotationFormat.GFF3, result.format());
        assertEquals(1, result.genes().size());
        assertEquals(1, result.genes().getFirst().getTranscripts().size());
        assertEquals(1, result.genes().getFirst().getTranscripts().getFirst().getExonCount());
    }

    @Test
    void resolvesCommonPlantFeatureAliasesWithoutSpeciesSpecificNames() {
        assertEquals(FeatureType.GENE, FeatureType.fromSoTerm("miRNA_gene"));
        assertEquals(FeatureType.TRANSCRIPT, FeatureType.fromSoTerm("primary_transcript"));
        assertEquals(FeatureType.FIVE_UTR, FeatureType.fromSoTerm("five_prime_UTR_region"));
        assertEquals(FeatureType.THREE_UTR, FeatureType.fromSoTerm("three_prime_UTR_region"));
    }

    @Test
    void decodesUtf8PercentEscapesInGff3Attributes() throws Exception {
        String gff = "##gff-version 3\n"
                + "chrA\tsrc\tgene\t1\t100\t.\t+\t.\tID=g1;Name=%E7%8E%89%E7%B1%B3\n";
        var result = new Gff3Parser().parse(new ByteArrayInputStream(
                gff.getBytes(StandardCharsets.UTF_8)), null, "utf8.gff3");
        assertEquals("玉米", result.genes().getFirst().getName());
    }
}
