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
                    BASE + "arabidopsis/Chr1.gff3", AnnotationFormat.GFF3, null,
                    "TAIR/Araport11 original June 2016 release: complete nuclear chromosome annotations generated from the pinned source archive"),
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
                    "Ensembl Plants release 62: unmodified real gene families")
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
        if ("arabidopsis".equals(id) && annotationResource.equals(BASE + "arabidopsis/Chr1.gff3")) {
            return List.of(
                    BASE + "arabidopsis/Chr1.gff3",
                    BASE + "arabidopsis/Chr2.gff3",
                    BASE + "arabidopsis/Chr3.gff3",
                    BASE + "arabidopsis/Chr4.gff3",
                    BASE + "arabidopsis/Chr5.gff3");
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
