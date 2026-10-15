package io.gitee.omixcore.phytoscope.service;

import io.gitee.omixcore.phytoscope.parser.AnnotationFormat;

import java.util.List;
import java.util.Map;

/**
 * Registry of the example datasets bundled with PhytoScope.
 *
 * <p>The framework itself is organism-agnostic; this catalogue exists purely so
 * that the application ships with something to look at, and so that the
 * cross-species generality can be demonstrated without any download. Every
 * entry is a small, traceable excerpt of a public genome annotation.</p>
 *
 * <p>The four entries deliberately span the angiosperm phylogeny — a rosid
 * reference model, a monocot cereal, another rosid legume and an asterid
 * medicinal herb — and deliberately use two different annotation dialects, so
 * that a single walk through the catalogue exercises the whole read path.</p>
 *
 * <p>Each entry also carries the real sequence span of every sequence its
 * excerpt was taken from. Those spans are not part of the excerpted annotation
 * file itself (which only covers the first few tens of kilobases), but they let
 * the viewer present each sequence at its true size — the coordinate axis reads
 * in real megabase units and the genes appear where they belong on the
 * chromosome instead of being stretched across the whole window.</p>
 *
 * <p>Every entry bundles more than one sequence, because a sequence selector
 * with a single entry is not a selector. The panel covers nuclear chromosomes
 * (Arabidopsis, rice, soybean), a plastome and three mitochondrial contigs
 * (mint), and it spans both annotation dialects.</p>
 */
public record ExampleDataset(String id,
                             String label,
                             String commonName,
                             String clade,
                             String species,
                             String assembly,
                             Map<String, Integer> chromosomeLengths,
                             String annotationResource,
                             AnnotationFormat format,
                             String expressionResource,
                             String attribution) {

    /** Classpath directory holding the bundled example datasets. */
    private static final String BASE = "/data/species/";

    /**
     * The bundled catalogue, shown in the dataset selector.
     * Order runs from the reference model outward across the phylogeny.
     *
     * <p>Sequence lengths are the published sizes of the real sequences the
     * excerpts come from: the five TAIR10 chromosomes (Chr1 30,427,671 bp;
     * Chr2 19,698,289 bp, RefSeq NC_003071; Chr3 23,459,830 bp, NC_003074;
     * Chr4 18,585,056 bp, NC_003075; Chr5 26,975,502 bp, NC_003076),
     * IRGSP-1.0 Chr1 (43,270,923 bp, NC_008394) and Chr2 (35,937,250 bp,
     * NC_008395), Glycine max Wm82.a2.v1 chromosomes 1-3 (55,915,577 bp,
     * 48,577,594 bp and 47,783,766 bp) and the Mentha spicata organelle
     * panel: the plastome MG256495 (152,132 bp) plus the three mitochondrial
     * contigs ON631245 (158,164 bp), ON631246 (87,431 bp) and ON631247
     * (40,953 bp).</p>
     *
     * <p>The map is keyed by the sequence ids used inside each annotation file,
     * so an organism whose sequences are not called "Chr1..N" — a legume
     * chromosome, a plastome, a mitochondrial contig — is described just as
     * naturally. Nothing is hard coded per species: whatever keys a file's own
     * header declares become the entries offered by the sequence selector, and
     * every bundled dataset deliberately carries more than one so that the
     * selector is meaningful out of the box.</p>
     */
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
                    Map.of("Chr1", 43_270_923,
                            "Chr2", 35_937_250),
                    BASE + "rice/oryza_sativa_chr1.gff3",
                    AnnotationFormat.GFF3,
                    BASE + "rice/oryza_sativa_expression.tsv",
                    "IRGSP-1.0 / RAP-DB public annotation "
                            + "(NCBI AP014957 for Chr1, AP014958 for Chr2); "
                            + "expression from RiceXPro"),

            new ExampleDataset(
                    "soybean",
                    "Glycine max",
                    "Soybean",
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
                            + "mitochondrial contigs ON631245-ON631247; "
                            + "expression from public M. spicata RNA-seq")
    );

    /** Looks up a catalogue entry by its short id, or {@code null}. */
    public static ExampleDataset byId(String id) {
        return CATALOGUE.stream()
                .filter(d -> d.id.equals(id))
                .findFirst()
                .orElse(null);
    }

    /**
     * The dataset loaded at startup: the reference model, because it has the
     * richest multi-transcript examples and is the most familiar starting point.
     */
    public static ExampleDataset defaultDataset() {
        return CATALOGUE.get(0);
    }

    /**
     * One-line clade description used in the dataset selector, so the
     * phylogenetic spread of the panel is visible at a glance.
     */
    public String fullLabel() {
        return species + "  \u2014  " + commonName + "  (" + clade + ")";
    }

    /**
     * Published lengths of the sequences the excerpts were taken from, keyed by
     * the sequence ids used inside the annotation files, ready to hand to
     * {@link io.gitee.omixcore.phytoscope.model.GenomeMetadata#withSequenceLengths}.
     */
    public Map<String, Integer> publishedSequenceLengths() {
        return chromosomeLengths == null ? Map.of() : chromosomeLengths;
    }

    /** Formats a base-pair count as an approximate megabase/lobase value. */
    public static String formatLength(int basePairs) {
        if (basePairs >= 1_000_000) {
            return String.format(java.util.Locale.ROOT, "%.1f Mb", basePairs / 1_000_000.0);
        }
        if (basePairs >= 1_000) {
            return String.format(java.util.Locale.ROOT, "%.0f kb", basePairs / 1_000.0);
        }
        return basePairs + " bp";
    }
}
