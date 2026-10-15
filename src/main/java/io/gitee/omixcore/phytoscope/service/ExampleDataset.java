package io.gitee.omixcore.phytoscope.service;

import io.gitee.omixcore.phytoscope.parser.AnnotationFormat;
import java.util.List;
import java.util.Map;

/**
 * Version-pinned reference annotation excerpts from Ensembl Plants release 62.
 * Nuclear chromosome sets and assemblies are cross-checked against assembly records.
 * Each excerpt includes two real gene families per nuclear chromosome, with original
 * sequence IDs and full lengths. No measured expression values are bundled.
 * See docs/chromosome-validation.md and the committed provenance manifest.
 */
public record ExampleDataset(String id, String label, String commonName, String clade,
                             String species, String assembly,
                             Map<String, Integer> chromosomeLengths,
                             String annotationResource, AnnotationFormat format,
                             String expressionResource, String attribution) {
    private static final String BASE = "/data/species/";

    public static final List<ExampleDataset> CATALOGUE = List.of(
            new ExampleDataset("arabidopsis", "Arabidopsis thaliana · 5 chromosomes",
                    "Thale cress Col-0", "Rosid · Brassicaceae", "Arabidopsis thaliana", "TAIR10", Map.of(),
                    BASE + "arabidopsis/arabidopsis_chromosomes.gff3", AnnotationFormat.GFF3, null,
                    "Ensembl Plants release 62: unmodified real gene families; https://ftp.ensemblgenomes.ebi.ac.uk/pub/plants/release-62/gff3/arabidopsis_thaliana/Arabidopsis_thaliana.TAIR10.62.gff3.gz"),
            new ExampleDataset("rice", "Oryza sativa Japonica Group · 12 chromosomes",
                    "Rice Nipponbare", "Monocot · Poaceae", "Oryza sativa Japonica Group", "IRGSP-1.0", Map.of(),
                    BASE + "rice/rice_chromosomes.gff3", AnnotationFormat.GFF3, null,
                    "Ensembl Plants release 62: unmodified real gene families; https://ftp.ensemblgenomes.ebi.ac.uk/pub/plants/release-62/gff3/oryza_sativa/Oryza_sativa.IRGSP-1.0.62.gff3.gz"),
            new ExampleDataset("maize", "Zea mays · 10 chromosomes",
                    "Maize B73", "Monocot · Poaceae", "Zea mays", "Zm-B73-REFERENCE-NAM-5.0", Map.of(),
                    BASE + "maize/maize_chromosomes.gff3", AnnotationFormat.GFF3, null,
                    "Ensembl Plants release 62: unmodified real gene families; https://ftp.ensemblgenomes.ebi.ac.uk/pub/plants/release-62/gff3/zea_mays/Zea_mays.Zm-B73-REFERENCE-NAM-5.0.62.gff3.gz"),
            new ExampleDataset("soybean", "Glycine max · 20 chromosomes",
                    "Soybean Williams 82", "Rosid · Fabaceae", "Glycine max", "Glycine_max_v2.1", Map.of(),
                    BASE + "soybean/soybean_chromosomes.gff3", AnnotationFormat.GFF3, null,
                    "Ensembl Plants release 62: unmodified real gene families; https://ftp.ensemblgenomes.ebi.ac.uk/pub/plants/release-62/gff3/glycine_max/Glycine_max.Glycine_max_v2.1.62.gff3.gz")
    );

    public static ExampleDataset byId(String id) {
        // Retain the previously published ID for the same verified soybean assembly.
        String canonicalId = "soybean-v2".equals(id) ? "soybean" : id;
        return CATALOGUE.stream().filter(d -> d.id.equals(canonicalId)).findFirst().orElse(null);
    }

    public static ExampleDataset defaultDataset() {
        return CATALOGUE.getFirst();
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
