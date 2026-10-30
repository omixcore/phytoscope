package io.gitee.omixcore.phytoscope.service;

import io.gitee.omixcore.phytoscope.parser.AnnotationFormat;
import java.util.List;
import java.util.Map;

/** Version-pinned plant reference annotation examples. */
public record ExampleDataset(String id, String label, String commonName, String clade,
                             String species, String assembly,
                             Map<String, Integer> chromosomeLengths,
                             String annotationResource, AnnotationFormat format,
                             String expressionResource, String attribution) {
    private static final String BASE = "/data/species/";

    public static final List<ExampleDataset> CATALOGUE = List.of(
            new ExampleDataset("arabidopsis", "Arabidopsis thaliana · TAIR10 · Chr1–Chr5",
                    "Thale cress Col-0", "Rosid · Brassicaceae", "Arabidopsis thaliana", "TAIR10", Map.of(),
                    BASE + "arabidopsis/Chr1.gff3.gz", AnnotationFormat.GFF3, "/data/case-studies/arabidopsis-atlas/expression.tsv",
                    "TAIR/Araport11 original June 2016 release; bundled Arabidopsis tissue transcriptome case: Expression Atlas E-MTAB-7978"),
            new ExampleDataset("rice", "Oryza sativa Japonica Group · 12 chromosomes",
                    "Rice Nipponbare", "Monocot · Poaceae", "Oryza sativa Japonica Group", "IRGSP-1.0", Map.of(),
                    BASE + "rice/rice_chromosomes.gff3", AnnotationFormat.GFF3, null,
                    "Ensembl Plants release 62: unmodified real gene families"),
            new ExampleDataset("maize", "Zea mays · 10 chromosomes",
                    "Maize B73", "Monocot · Poaceae", "Zea mays", "Zm-B73-REFERENCE-NAM-5.0", Map.of(),
                    BASE + "maize/maize_chromosomes.gff3", AnnotationFormat.GFF3, null,
                    "Ensembl Plants release 62: unmodified real gene families"),
            new ExampleDataset("soybean", "Glycine max · 20 chromosomes",
                    "Soybean Williams 82", "Rosid · Fabaceae", "Glycine max", "Glycine_max_v2.1", Map.of(),
                    BASE + "soybean/soybean_chromosomes.gff3", AnnotationFormat.GFF3, null,
                    "Ensembl Plants release 62: unmodified real gene families"),
            new ExampleDataset("mint", "Mentha longifolia · 12 chromosomes · sample subset",
                    "Horse mint CMEN585", "Asterid · Lamiaceae", "Mentha longifolia", "Mlong_CMEN585_v3.01b", Map.of(),
                    BASE + "mint/mentha_longifolia_chromosomes.gff3", AnnotationFormat.GFF3, null,
                    "WSU Mint Genome Resources, Mentha longifolia CMEN585 v3.01b (also INSDC GCA_001642375.2): "
                            + "two real Maker gene families per chromosome-scale sequence"),
            new ExampleDataset("mint-genome", "Mentha longifolia · 12 chromosomes · complete annotation · 94,883 genes",
                    "Horsemint Long3", "Asterid · Lamiaceae", "Mentha longifolia", "Long3_HAP_v2 (haplotype A)", Map.of(),
                    BASE + "mint/mentha_longifolia_horsemint.gff3.gz", AnnotationFormat.GFF3,
                    BASE + "mint/mentha_longifolia_expression.tsv",
                    "Lieberman, Henry & Comai 2025, chromosome-scale diploid horsemint genome "
                            + "(figshare 10.6084/m9.figshare.28457522): all 94,883 Braker3 gene models on "
                            + "haplotype A, 102,705 transcripts, coordinates unmodified"),
            new ExampleDataset("mint-plastid", "Mentha spicata · MG256495 chloroplast · 152 kb",
                    "Spearmint", "Asterid · Lamiaceae", "Mentha spicata", "MG256495 chloroplast", Map.of(),
                    BASE + "mint/mentha_spicata_chloroplast.gff3", AnnotationFormat.GFF3, null,
                    "INSDC MG256495 (GenBank/ENA), Mentha spicata chloroplast complete genome, 152,132 bp: "
                            + "feature coordinates re-encoded as GFF3 without alteration")
    );

    public static ExampleDataset byId(String id) {
        String canonicalId = "soybean-v2".equals(id) ? "soybean" : id;
        return CATALOGUE.stream().filter(d -> d.id.equals(canonicalId)).findFirst().orElse(null);
    }

    public static ExampleDataset defaultDataset() {
        return CATALOGUE.getFirst();
    }

    /** All classpath parts of a bundled annotation. */
    public List<String> annotationResources() {
        if (annotationResource == null || annotationResource.isBlank()) {
            return List.of();
        }
        if ("arabidopsis".equals(id) && annotationResource.equals(BASE + "arabidopsis/Chr1.gff3.gz")) {
            return List.of(
                    BASE + "arabidopsis/Chr1.gff3.gz",
                    BASE + "arabidopsis/Chr2.gff3.gz",
                    BASE + "arabidopsis/Chr3.gff3.gz",
                    BASE + "arabidopsis/Chr4.gff3.gz",
                    BASE + "arabidopsis/Chr5.gff3.gz");
        }
        return List.of(annotationResource);
    }

    public String fullLabel() {
        return commonName + " — " + label;
    }

    public Map<String, Integer> publishedSequenceLengths() {
        return chromosomeLengths == null ? Map.of() : chromosomeLengths;
    }

    public static String formatLength(int basePairs) {
        if (basePairs >= 1_000_000) return String.format(java.util.Locale.ROOT, "%.1f Mb", basePairs / 1_000_000.0);
        if (basePairs >= 1_000) return String.format(java.util.Locale.ROOT, "%.0f kb", basePairs / 1_000.0);
        return basePairs + " bp";
    }
}
