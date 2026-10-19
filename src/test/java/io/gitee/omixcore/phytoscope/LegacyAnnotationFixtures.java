package io.gitee.omixcore.phytoscope;

import io.gitee.omixcore.phytoscope.service.ExampleDataset;
import io.gitee.omixcore.phytoscope.parser.AnnotationFormat;
import java.util.List;
import java.util.Map;

/** Non-authoritative historical fixtures for parser regression only; never shipped or used as validation evidence. */
final class LegacyAnnotationFixtures {
    private static final String BASE = "/legacy-regression/";
    public static final List<ExampleDataset> CATALOGUE = List.of(
            new ExampleDataset(
                    "arabidopsis",
                    "Arabidopsis thaliana",
                    "Thale cress",
                    "Rosid \u00b7 Brassicaceae",
                    "Arabidopsis thaliana",
                    "TAIR10 / Araport11",
                    Map.of("Chr1", 30_427_671,
                            "Chr2", 19_698_289,
                            "Chr3", 23_459_830,
                            "Chr4", 18_585_056,
                            "Chr5", 26_975_502),
                    BASE + "arabidopsis/arabidopsis_thaliana_demo.gff3",
                    AnnotationFormat.GFF3,
                    BASE + "arabidopsis/arabidopsis_thaliana_expression.tsv",
                    "TAIR10 / Araport11 public annotation (arabidopsis.org, "
                            + "RefSeq NC_003071-NC_003076 for Chr2-Chr5); "
                            + "expression from PEO / AtGenExpress"),

            new ExampleDataset(
                    "rice",
                    "Oryza sativa",
                    "Rice (japonica)",
                    "Monocot \u00b7 Poaceae",
                    "Oryza sativa Japonica Group",
                    "IRGSP-1.0 (Nipponbare)",
                    Map.ofEntries(Map.entry("Chr1", 43_270_923),
                            Map.entry("Chr2", 35_937_250),
                            Map.entry("Chr3", 36_413_819),
                            Map.entry("Chr4", 35_502_694),
                            Map.entry("Chr5", 29_958_434),
                            Map.entry("Chr6", 31_248_787),
                            Map.entry("Chr7", 29_697_621),
                            Map.entry("Chr8", 28_443_022),
                            Map.entry("Chr9", 23_012_720),
                            Map.entry("Chr10", 23_207_287),
                            Map.entry("Chr11", 29_021_106),
                            Map.entry("Chr12", 27_531_856)),
                    BASE + "rice/oryza_sativa_chr1.gff3",
                    AnnotationFormat.GFF3,
                    BASE + "rice/oryza_sativa_expression.tsv",
                    "IRGSP-1.0 / RAP-DB public annotation "
                            + "(NCBI AP014957 for Chr1, AP014958 for Chr2); "
                            + "expression from RiceXPro"),

            new ExampleDataset(
                    "soybean",
                    "Glycine max",
                    "Soybean (3-chromosome GTF excerpt)",
                    "Rosid \u00b7 Fabaceae",
                    "Glycine max",
                    "Glycine max Wm82.a2.v1",
                    Map.of("Glyma01", 55_915_577,
                            "Glyma02", 48_577_594,
                            "Glyma03", 47_783_766),
                    BASE + "soybean/glycine_max_chr1.gtf",
                    AnnotationFormat.GTF,
                    BASE + "soybean/glycine_max_expression.tsv",
                    "Phytozome Glycine max Wm82.a2.v1 public annotation "
                            + "(illustrative subset), GTF dialect; expression from SoyBase"),

            new ExampleDataset(
                    "mint",
                    "Mentha spicata",
                    "Spearmint",
                    "Asterid \u00b7 Lamiaceae",
                    "Mentha spicata",
                    "Mentha spicata plastid MG256495 + mitochondrion ON631245-47",
                    Map.of("Pt", 152_132,
                            "Mt1", 158_164,
                            "Mt2", 87_431,
                            "Mt3", 40_953),
                    BASE + "mint/mentha_spicata_plastid.gff3",
                    AnnotationFormat.GFF3,
                    BASE + "mint/mentha_spicata_expression.tsv",
                    "Complete plastid genome GenBank MG256495 "
                            + "(Wang et al. 2017, Mitochondrial DNA Part B) plus the "
                            + "mitochondrial contigs ON631245-ON631247; the paired "
                            + "expression table is explicitly illustrative and is not "
                            + "linked to a verified public M. spicata RNA-seq dataset")

    );
    static ExampleDataset byId(String id) {
        return CATALOGUE.stream().filter(d -> d.id().equals(id)).findFirst().orElse(null);
    }
}
