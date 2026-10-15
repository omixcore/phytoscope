package io.gitee.omixcore.phytoscope.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Species- and assembly-independent description of an annotation dataset.
 *
 * <p>PhytoScope is designed as a general framework: nothing about the organism
 * is hard-coded. Every dataset carries its own metadata, harvested from the
 * annotation header where available ({@code ##species}, {@code ##assembly},
 * {@code ##sequence-region}) and completed by the loader otherwise.</p>
 *
 * <p>Instances are immutable; use {@link #fromAnnotation} to derive one from a
 * parse result and {@link Builder} when metadata must be supplied explicitly.</p>
 */
public final class GenomeMetadata {

    /** Placeholder used when a value could not be determined from the data. */
    public static final String UNKNOWN = "unknown";

    private final String species;
    private final String assembly;
    private final String description;
    private final String sourceLabel;
    private final List<SequenceRegion> sequenceRegions;
    private final Map<String, SequenceRegion> regionIndex;

    private GenomeMetadata(String species, String assembly, String description,
                           String sourceLabel, List<SequenceRegion> sequenceRegions) {
        this.species = blankToUnknown(species);
        this.assembly = blankToUnknown(assembly);
        this.description = description == null ? "" : description.trim();
        this.sourceLabel = blankToUnknown(sourceLabel);
        this.sequenceRegions = List.copyOf(sequenceRegions);
        Map<String, SequenceRegion> index = new LinkedHashMap<>();
        for (SequenceRegion region : this.sequenceRegions) {
            index.putIfAbsent(region.id(), region);
        }
        this.regionIndex = Map.copyOf(index);
    }

    // ------------------------------------------------------------------
    // Factories
    // ------------------------------------------------------------------

    /**
     * Builds metadata from the values parsed out of an annotation file.
     *
     * @param species         value of the {@code ##species} directive, may be null
     * @param assembly        value of the {@code ##assembly} directive, may be null
     * @param description     free-text description, may be null
     * @param sourceLabel     human-readable origin of the data, e.g. a file name
     * @param sequenceRegions declared sequence regions, may be empty
     */
    public static GenomeMetadata fromAnnotation(String species, String assembly, String description,
                                                String sourceLabel, List<SequenceRegion> sequenceRegions) {
        return new GenomeMetadata(species, assembly, description, sourceLabel,
                sequenceRegions == null ? List.of() : sequenceRegions);
    }

    /**
     * Derives metadata when the header carried none: sequence regions are
     * inferred from the chromosomes actually observed in the file.
     *
     * @param observedChromosomes chromosome names seen while parsing
     * @param observedBounds      matching [min, max] coordinate per chromosome
     */
    public static GenomeMetadata inferred(String sourceLabel,
                                          List<String> observedChromosomes,
                                          Map<String, int[]> observedBounds) {
        List<SequenceRegion> regions = new java.util.ArrayList<>();
        for (String chromosome : observedChromosomes) {
            int[] bounds = observedBounds.get(chromosome);
            if (bounds != null) {
                regions.add(new SequenceRegion(chromosome, bounds[0], bounds[1]));
            } else {
                regions.add(new SequenceRegion(chromosome, 1, 1));
            }
        }
        return new GenomeMetadata(null, null, "", sourceLabel, regions);
    }

    // ------------------------------------------------------------------
    // Queries
    // ------------------------------------------------------------------

    public String getSpecies() {
        return species;
    }

    public String getAssembly() {
        return assembly;
    }

    public String getDescription() {
        return description;
    }

    public String getSourceLabel() {
        return sourceLabel;
    }

    public List<SequenceRegion> getSequenceRegions() {
        return sequenceRegions;
    }

    /** Sequence names in declaration order. */
    public List<String> getSequenceNames() {
        return sequenceRegions.stream().map(SequenceRegion::id).toList();
    }

    /** Looks up a declared region, or {@code null} when the id is unknown. */
    public SequenceRegion findRegion(String id) {
        return id == null ? null : regionIndex.get(id);
    }

    /** First declared region, used as the default view anchor; never null. */
    public SequenceRegion primaryRegion() {
        if (!sequenceRegions.isEmpty()) {
            return sequenceRegions.get(0);
        }
        return new SequenceRegion(UNKNOWN, 1, 1);
    }

    public int getSequenceCount() {
        return sequenceRegions.size();
    }

    public boolean hasSequenceRegions() {
        return !sequenceRegions.isEmpty();
    }

    /** True when the species name was never determined. */
    public boolean isSpeciesUnknown() {
        return UNKNOWN.equals(species);
    }

    /** Short label suitable for a status bar or window title. */
    public String summary() {
        String organism = isSpeciesUnknown() ? "Unspecified species" : species;
        StringBuilder sb = new StringBuilder(organism);
        if (!UNKNOWN.equals(assembly)) {
            sb.append(" (").append(assembly).append(')');
        }
        if (!sequenceRegions.isEmpty()) {
            sb.append(" \u00b7 ").append(sequenceRegions.size())
                    .append(sequenceRegions.size() == 1 ? " sequence" : " sequences");
        }
        return sb.toString();
    }

    /**
     * Returns a copy in which the named sequence regions report a different
     * span. Used when an annotation excerpt is known to have been taken from a
     * larger, published sequence: the genes stay where they are, but the axis
     * can be drawn at the sequence's true size.
     *
     * <p>A published length always describes a whole sequence, so the region is
     * re-anchored to base 1. That matters for dialects such as GTF, which carry
     * no {@code ##sequence-region} directive: there the observed region is
     * inferred from the data and would otherwise begin at the first gene,
     * making a position-1-anchored axis impossible.</p>
     *
     * <p>Regions not mentioned in {@code lengths} are left untouched, and
     * regions whose new length is not longer than the observed data are
     * ignored, so an excerpt can never be shrunk below its own coordinates.</p>
     *
     * @param lengths published length per sequence id, in base pairs
     * @return a new metadata instance, or {@code this} when nothing changes
     */
    public GenomeMetadata withSequenceLengths(Map<String, Integer> lengths) {
        if (lengths == null || lengths.isEmpty() || sequenceRegions.isEmpty()) {
            return this;
        }
        List<SequenceRegion> updated = new java.util.ArrayList<>(sequenceRegions.size());
        boolean changed = false;
        for (SequenceRegion region : sequenceRegions) {
            Integer declaredLength = lengths.get(region.id());
            if (declaredLength != null && declaredLength > region.length()) {
                updated.add(new SequenceRegion(region.id(), 1, declaredLength));
                changed = true;
            } else {
                updated.add(region);
            }
        }
        if (!changed) {
            return this;
        }
        return new GenomeMetadata(species, assembly, description, sourceLabel, updated);
    }

    private static String blankToUnknown(String value) {
        return value == null || value.isBlank() ? UNKNOWN : value.trim();
    }

    @Override
    public String toString() {
        return "GenomeMetadata[" + summary() + ", source=" + sourceLabel + "]";
    }

    // ------------------------------------------------------------------
    // Builder
    // ------------------------------------------------------------------

    /**
     * Mutable builder for cases where metadata is assembled step by step,
     * for example by an interactive import wizard.
     */
    public static final class Builder {
        private String species;
        private String assembly;
        private String description = "";
        private String sourceLabel;
        private final List<SequenceRegion> regions = new java.util.ArrayList<>();

        public Builder species(String species) {
            this.species = species;
            return this;
        }

        public Builder assembly(String assembly) {
            this.assembly = assembly;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder sourceLabel(String sourceLabel) {
            this.sourceLabel = sourceLabel;
            return this;
        }

        public Builder addRegion(String id, int start, int end) {
            regions.add(new SequenceRegion(id, start, end));
            return this;
        }

        public Builder addRegions(List<SequenceRegion> more) {
            if (more != null) {
                regions.addAll(more);
            }
            return this;
        }

        public GenomeMetadata build() {
            return new GenomeMetadata(species, assembly, description, sourceLabel, regions);
        }
    }
}
