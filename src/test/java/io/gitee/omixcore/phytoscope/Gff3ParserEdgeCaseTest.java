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
        assertEquals(FeatureType.TRANSCRIPT, FeatureType.fromSoTerm("miRNA_primary_transcript"));
        assertEquals(FeatureType.TRANSCRIPT, FeatureType.fromSoTerm("pseudogenic_transcript"));
        assertEquals(FeatureType.EXON, FeatureType.fromSoTerm("pseudogenic_exon"));
    }

    @Test
    void honorsGff2HeaderAsGtfDialect() throws Exception {
        String gff2 = "##gff-version 2\n"
                + "chrA\tsrc\tgene\t1\t100\t.\t+\t.\tgene_id \"g1\"; gene_name \"Alpha\";\n";
        var result = new Gff3Parser().parse(new ByteArrayInputStream(
                gff2.getBytes(StandardCharsets.UTF_8)), null, "gff2.gff");
        assertEquals(AnnotationFormat.GTF, result.format());
        assertEquals(1, result.genes().size());
        assertEquals("g1", result.genes().getFirst().getId());
    }

    @Test
    void duplicateTranscriptIdKeepsChildrenOnRetainedTranscript() throws Exception {
        String gff = "##gff-version 3\n"
                + "chrA\tsrc\tgene\t1\t500\t.\t+\t.\tID=g1;Name=G1\n"
                + "chrA\tsrc\tmRNA\t1\t500\t.\t+\t.\tID=t1;Parent=g1\n"
                + "chrA\tsrc\tmRNA\t1\t500\t.\t+\t.\tID=t1;Parent=g1\n"
                + "chrA\tsrc\texon\t100\t200\t.\t+\t.\tID=e1;Parent=t1\n";
        var result = new Gff3Parser().parse(new ByteArrayInputStream(
                gff.getBytes(StandardCharsets.UTF_8)), null, "dup.gff3");
        assertEquals(1, result.genes().getFirst().getTranscripts().size());
        assertEquals(1, result.genes().getFirst().getTranscripts().getFirst().getExonCount());
    }

    @Test
    void decodesUtf8PercentEscapesInGff3Attributes() throws Exception {
        String gff = "##gff-version 3\n"
                + "chrA\tsrc\tgene\t1\t100\t.\t+\t.\tID=g1;Name=%E7%8E%89%E7%B1%B3\n";
        var result = new Gff3Parser().parse(new ByteArrayInputStream(
                gff.getBytes(StandardCharsets.UTF_8)), null, "utf8.gff3");
        assertEquals("玉米", result.genes().getFirst().getName());
    }

    @Test
    void decodesLongUnicodeGtfValuesWithoutUndersizedBuffer() throws Exception {
        String expected = "水稻参考基因组的长功能描述包含多个Unicode字符和%符号";
        String gtf = "chrA\tsrc\tgene\t1\t100\t.\t+\t.\tgene_id \"g1\"; gene_name \""
                + expected.replace("%", "%25") + "%20版本信息\";\n";
        var result = new Gff3Parser().parse(new ByteArrayInputStream(
                gtf.getBytes(StandardCharsets.UTF_8)), AnnotationFormat.GTF, "unicode.gtf");
        assertEquals(expected + " 版本信息", result.genes().getFirst().getName());
    }

    @Test
    void keepsAccessionSequenceIdsAndExactMinusStrandModelsWhenOptionalMetadataIsAbsent() throws Exception {
        // Independent hand-calculated mini gold set. The accession-style seqid has
        // no sequence-region/chromosome directive, gene Name, or source metadata.
        String gff = "##gff-version 3\n"
                + "CM099999.1\t.\tgene\t101\t260\t.\t-\t.\tID=gene-X\n"
                + "CM099999.1\t.\tmRNA\t101\t260\t.\t-\t.\tID=tx-X.1;Parent=gene-X\n"
                + "CM099999.1\t.\texon\t101\t130\t.\t-\t.\tParent=tx-X.1\n"
                + "CM099999.1\t.\tfive_prime_UTR\t121\t130\t.\t-\t.\tParent=tx-X.1\n"
                + "CM099999.1\t.\tCDS\t110\t120\t.\t-\t0\tParent=tx-X.1\n"
                + "CM099999.1\t.\texon\t201\t260\t.\t-\t.\tParent=tx-X.1\n"
                + "CM099999.1\t.\tCDS\t201\t230\t.\t-\t2\tParent=tx-X.1\n"
                + "CM099999.1\t.\tthree_prime_UTR\t231\t260\t.\t-\t.\tParent=tx-X.1\n";
        var result = new Gff3Parser().parse(new ByteArrayInputStream(
                gff.getBytes(StandardCharsets.UTF_8)), null, "accession.gff3");

        assertEquals(1, result.genes().size());
        var gene = result.genes().getFirst();
        assertEquals("gene-X", gene.getId());
        assertEquals("CM099999.1", gene.getChromosome());
        assertEquals(101, gene.getStart());
        assertEquals(260, gene.getEnd());
        assertEquals('-', gene.getStrand());
        var transcript = gene.getTranscripts().getFirst();
        assertEquals("tx-X.1", transcript.getId());
        assertEquals(2, transcript.getExonCount());
        assertEquals(41, transcript.getCdsLength());
        assertEquals(40, transcript.getExons().stream()
                .flatMap(exon -> exon.getUtrSegments().stream())
                .mapToInt(io.gitee.omixcore.phytoscope.model.GenomicFeature::length).sum());
    }
}
