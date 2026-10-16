package io.gitee.omixcore.phytoscope.service;

import io.gitee.omixcore.phytoscope.model.Gene;
import io.gitee.omixcore.phytoscope.model.GenomeMetadata;
import io.gitee.omixcore.phytoscope.model.SequenceRegion;
import io.gitee.omixcore.phytoscope.parser.AnnotationFormat;
import io.gitee.omixcore.phytoscope.parser.Gff3Parser;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Genome annotation service: loads and caches gene models and answers queries.
 *
 * <p>Deliberately species-agnostic. The service holds no built-in organism
 * knowledge; everything it reports about the dataset comes from the
 * {@link GenomeMetadata} produced by the parser. The bundled sample is only a
 * convenience default, so the application is usable the moment it starts.</p>
 */
@Service
public class GenomeDataService {

    private final List<Gene> genes = new ArrayList<>();
    private final Map<String, Gene> geneIndex = new LinkedHashMap<>();
    private final Map<String, List<Gene>> genesBySequence = new LinkedHashMap<>();

    private GenomeMetadata metadata = GenomeMetadata.fromAnnotation(
            null, null, "", "No data loaded", List.of());
    private AnnotationFormat format = AnnotationFormat.GFF3;
    private int rawFeatureCount;

    /** Sequence currently selected in the chromosome selector; null when empty. */
    private String activeSequence;

    private final Gff3Parser parser = new Gff3Parser();

    // ------------------------------------------------------------------
    // Loading
    // ------------------------------------------------------------------

    /**
     * Loads the catalogue's default bundled example dataset.
     */
    public void loadSampleData() throws IOException {
        loadExample(ExampleDataset.defaultDataset());
    }

    /**
     * Loads one of the bundled example datasets.
     *
     * <p>Bundled excerpts cover only the first few tens of kilobases of the
     * sequence they came from. The catalogue also records the published length
     * of that sequence, which is applied here so the browser shows the genes at
     * their real position on the real chromosome.</p>
     *
     * @param dataset catalogue entry to load
     */
    public void loadExample(ExampleDataset dataset) throws IOException {
        Objects.requireNonNull(dataset, "dataset must not be null");
        Gff3Parser.ParseResult result = dataset.annotationResources().size() == 1
                ? parser.parseResource(dataset.annotationResource(), dataset.format())
                : parser.parseResources(dataset.annotationResources(), dataset.format());
        applyResult(result, dataset.publishedSequenceLengths());
    }

    /**
     * Loads a dataset from the classpath with an explicit format.
     *
     * @param resourcePath classpath path of the annotation
     * @param format       dialect to assume, or {@code null} to auto-detect
     */
    public void loadFromResource(String resourcePath, AnnotationFormat format) throws IOException {
        Gff3Parser.ParseResult result = parser.parseResource(resourcePath, format);
        applyResult(result);
    }

    /**
     * Loads an annotation from a local file, auto-detecting GFF3 vs GTF.
     */
    public void loadFromFile(Path file) throws IOException {
        loadFromFile(file, null);
    }

    /**
     * Loads an annotation from a local file.
     *
     * @param file   annotation file
     * @param format dialect to assume, or {@code null} to auto-detect
     */
    public void loadFromFile(Path file, AnnotationFormat format) throws IOException {
        Gff3Parser.ParseResult result = parser.parseFile(file, format);
        applyResult(result);
    }

    /**
     * Loads an annotation held in memory, auto-detecting the dialect. Handy for
     * tests and for pasted snippets; the label becomes the reported source.
     *
     * @param content annotation text
     * @param label   human-readable origin recorded in the metadata
     */
    public void loadFromContent(String content, String label) throws IOException {
        Gff3Parser.ParseResult result = parser.parse(
                new java.io.ByteArrayInputStream(
                        content.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                null, label);
        applyResult(result);
    }

    /** Loads annotation text held in memory, using a generic source label. */
    public void loadFromContent(String content) throws IOException {
        loadFromContent(content, "in-memory annotation");
    }

    private void applyResult(Gff3Parser.ParseResult result) {
        applyResult(result, Map.of());
    }

    private void applyResult(Gff3Parser.ParseResult result, Map<String, Integer> sequenceLengths) {
        genes.clear();
        geneIndex.clear();
        genesBySequence.clear();

        genes.addAll(result.genes());
        for (Gene gene : genes) {
            genesBySequence.computeIfAbsent(gene.getChromosome(), key -> new ArrayList<>()).add(gene);
            geneIndex.put(gene.getDisplayName(), gene);
            if (gene.getId() != null) {
                geneIndex.put(gene.getId(), gene);
            }
        }
        this.metadata = result.metadata().withSequenceLengths(sequenceLengths);
        this.format = result.format();
        this.rawFeatureCount = result.featureCount();
        // Prefer a populated sequence: headers may declare unannotated organelles
        // or scaffolds before the first nuclear gene. Keep them selectable.
        this.activeSequence = metadata.getSequenceNames().stream()
                .filter(name -> genesBySequence.containsKey(name)).findFirst()
                .orElse(metadata.getSequenceNames().isEmpty()
                        ? null : metadata.getSequenceNames().get(0));
    }

    // ------------------------------------------------------------------
    // Queries
    // ------------------------------------------------------------------

    public List<Gene> getGenes() {
        return List.copyOf(genes);
    }

    /**
     * Genes on the currently selected sequence, in coordinate order.
     *
     * <p>When several sequences are present the browser shows one at a time,
     * exactly as a genome browser does; this is the list the gene panel and the
     * track view are built from.</p>
     */
    public List<Gene> getGenesOnActiveSequence() {
        return getGenesOn(activeSequence);
    }

    /** Sequence names, in the order they were declared or first observed. */
    public List<String> getChromosomes() {
        return metadata.getSequenceNames();
    }

    /** The sequence currently shown, or {@code null} when no data is loaded. */
    public String getActiveSequence() {
        return activeSequence;
    }

    /**
     * Selects the sequence to display. Unknown names are ignored so that a
     * stale selection cannot blank the view.
     *
     * @return true when the selection actually changed
     */
    public boolean setActiveSequence(String sequence) {
        if (sequence == null || !getChromosomes().contains(sequence)
                || sequence.equals(activeSequence)) {
            return false;
        }
        this.activeSequence = sequence;
        return true;
    }

    /** Genes located on one sequence, in coordinate order. */
    public List<Gene> getGenesOn(String sequence) {
        if (sequence == null) {
            return List.of();
        }
        return List.copyOf(genesBySequence.getOrDefault(sequence, List.of()));
    }

    /**
     * Spans of the loaded sequences as they should be drawn: the declared span
     * for the selected sequence when available, otherwise the span covered by
     * its genes.
     *
     * @return [start, end] in 1-based inclusive coordinates, or null when empty
     */
    public int[] getActiveSequenceSpan() {
        if (activeSequence == null) {
            return null;
        }
        SequenceRegion region = metadata.findRegion(activeSequence);
        if (region != null && region.isUsable()) {
            return new int[]{region.start(), region.end()};
        }
        List<Gene> onSequence = getGenesOn(activeSequence);
        if (onSequence.isEmpty()) {
            return null;
        }
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (Gene gene : onSequence) {
            min = Math.min(min, gene.getStart());
            max = Math.max(max, gene.getEnd());
        }
        return new int[]{min, max};
    }

    /** Sequences that actually carry at least one gene, in metadata order. */
    public List<String> getPopulatedChromosomes() {
        return getChromosomes().stream()
                .filter(genesBySequence::containsKey)
                .toList();
    }

    public Gene findGene(String idOrName) {
        return geneIndex.get(idOrName);
    }

    /**
     * Fuzzy-searches genes by keyword across ID, name, alias and description.
     *
     * <p>The search spans the whole dataset rather than only the selected
     * sequence, so looking for a locus on another chromosome still finds it.</p>
     */
    public List<Gene> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return getGenesOnActiveSequence();
        }
        String kw = keyword.trim().toLowerCase();
        List<Gene> hits = new ArrayList<>();
        for (Gene gene : genes) {
            if (contains(gene.getId(), kw)
                    || contains(gene.getName(), kw)
                    || contains(gene.getAlias(), kw)
                    || contains(gene.getDescription(), kw)) {
                hits.add(gene);
            }
        }
        return hits;
    }

    private static boolean contains(String value, String lowercaseKeyword) {
        return value != null && value.toLowerCase().contains(lowercaseKeyword);
    }

    /** Dataset metadata: species, assembly and sequence regions. */
    public GenomeMetadata getMetadata() {
        return metadata;
    }

    /** Format the current dataset was read from. */
    public AnnotationFormat getFormat() {
        return format;
    }

    public String getSpecies() {
        return metadata.getSpecies();
    }

    public String getAssembly() {
        return metadata.getAssembly();
    }

    /** Human-readable origin of the loaded dataset. */
    public String getCurrentSource() {
        return metadata.getSourceLabel();
    }

    public int getGeneCount() {
        return genes.size();
    }

    public int getRawFeatureCount() {
        return rawFeatureCount;
    }

    public boolean isEmpty() {
        return genes.isEmpty();
    }
}
