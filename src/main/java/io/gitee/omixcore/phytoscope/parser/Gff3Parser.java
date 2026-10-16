package io.gitee.omixcore.phytoscope.parser;

import io.gitee.omixcore.phytoscope.model.Exon;
import io.gitee.omixcore.phytoscope.model.FeatureType;
import io.gitee.omixcore.phytoscope.model.Gene;
import io.gitee.omixcore.phytoscope.model.GenomeMetadata;
import io.gitee.omixcore.phytoscope.model.GenomicFeature;
import io.gitee.omixcore.phytoscope.model.SequenceRegion;
import io.gitee.omixcore.phytoscope.model.Transcript;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.Enumeration;
import java.util.zip.GZIPInputStream;

/**
 * Format-agnostic parser for gene-structure annotations.
 *
 * <p>Handles GFF3 and GTF/GFF2 with one core: the two dialects differ only in
 * how column 9 encodes attributes ({@code key=value;} vs {@code key "value";})
 * and in the header, so the dialect is detected once and the same assembly
 * pipeline is reused. This is what makes the framework species- and
 * portal-independent — a file from TAIR, Phytozome, Ensembl Plants or NCBI is
 * all read by the same code path.</p>
 *
 * <p>Assembly follows a two-pass strategy: rows are bucketed by type first and
 * the gene hierarchy is resolved afterwards, so feature order in the file never
 * matters. Nothing about the organism is assumed; species, assembly and
 * sequence regions are harvested from the header when present and otherwise
 * inferred from the data itself.</p>
 */
public class Gff3Parser {

    /**
     * Parse result: the assembled genes plus everything needed to describe the
     * dataset without knowing which species it came from.
     *
     * @param genes       assembled gene models, sorted by sequence then position
     * @param featureCount number of annotation rows that took part in assembly
     * @param format      dialect that was detected or supplied
     * @param metadata    dataset metadata (species, assembly, sequence regions)
     */
    public record ParseResult(List<Gene> genes,
                              int featureCount,
                              AnnotationFormat format,
                              GenomeMetadata metadata) {

        /** Convenience accessor kept for readability at call sites. */
        public List<String> chromosomes() {
            return metadata.getSequenceNames();
        }

        public String species() {
            return metadata.isSpeciesUnknown() ? null : metadata.getSpecies();
        }
    }

    /** Format used when nothing better can be determined. */
    private static final AnnotationFormat DEFAULT_FORMAT = AnnotationFormat.GFF3;

    // ------------------------------------------------------------------
    // Entry points
    // ------------------------------------------------------------------

    /**
     * Parses an annotation from a classpath resource.
     *
     * @param resourcePath classpath path, e.g. "/data/arabidopsis_chr1_sample.gff3"
     */
    public ParseResult parseResource(String resourcePath) throws IOException {
        return parseResource(resourcePath, null);
    }

    /**
     * Parses an annotation from a classpath resource with an explicit format.
     *
     * @param resourcePath classpath path
     * @param format       dialect to assume, or {@code null} to auto-detect
     */
    public ParseResult parseResource(String resourcePath, AnnotationFormat format) throws IOException {
        try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("Classpath resource not found: " + resourcePath);
            }
            String label = resourcePath.substring(resourcePath.lastIndexOf('/') + 1);
            AnnotationFormat resolved = format != null ? format : AnnotationFormat.fromFileName(label);
            return parse(in, resolved, label);
        }
    }

    /**
     * Parses multiple classpath resources as one logical annotation stream.
     *
     * <p>This is used for large bundled references that are intentionally split
     * into chromosome-sized UTF-8 files so no individual Git object exceeds the
     * GitHub 100 MB file limit. Each part is parsed in order and the GFF3 header
     * directives are merged by the normal parser.</p>
     */
    public ParseResult parseResources(List<String> resourcePaths, AnnotationFormat format) throws IOException {
        if (resourcePaths == null || resourcePaths.isEmpty()) {
            return parse(new java.io.ByteArrayInputStream(new byte[0]), format, "empty annotation");
        }
        List<InputStream> streams = new ArrayList<>();
        try {
            for (String resourcePath : resourcePaths) {
                InputStream in = getClass().getResourceAsStream(resourcePath);
                if (in == null) {
                    throw new IOException("Classpath resource not found: " + resourcePath);
                }
                streams.add(in);
            }
            Enumeration<InputStream> enumeration = Collections.enumeration(streams);
            return parse(new java.io.SequenceInputStream(enumeration), format,
                    String.join(", ", resourcePaths));
        } catch (IOException | RuntimeException ex) {
            for (InputStream stream : streams) {
                try { stream.close(); } catch (IOException ignored) { }
            }
            throw ex;
        }
    }

    /** Convenience overload for multiple resources with auto-detected format. */
    public ParseResult parseResources(List<String> resourcePaths) throws IOException {
        return parseResources(resourcePaths, null);
    }

    /**
     * Parses an annotation from a local file, auto-detecting the format.
     */
    public ParseResult parseFile(Path file) throws IOException {
        return parseFile(file, null);
    }

    /**
     * Parses an annotation from a local file.
     *
     * @param file   annotation file
     * @param format dialect to assume, or {@code null} to auto-detect from content
     */
    public ParseResult parseFile(Path file, AnnotationFormat format) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            String label = file.getFileName().toString();
            AnnotationFormat resolved = format != null ? format : AnnotationFormat.fromFileName(label);
            return parse(in, resolved, label);
        }
    }

    /**
     * Parses an annotation stream, auto-detecting the dialect from its content.
     *
     * @param in annotation stream, UTF-8
     */
    public ParseResult parse(InputStream in) throws IOException {
        return parse(in, null, "annotation");
    }

    /**
     * Parses an annotation stream.
     *
     * @param in     annotation stream, UTF-8
     * @param format dialect to assume, or {@code null} to auto-detect
     * @param label  human-readable source name recorded in the metadata
     */
    public ParseResult parse(InputStream in, AnnotationFormat format, String label) throws IOException {
        // Pass 1: collect rows into per-type buckets, preserving file order.
        List<RawFeature> rawGenes = new ArrayList<>();
        List<RawFeature> rawTranscripts = new ArrayList<>();
        List<RawFeature> rawExons = new ArrayList<>();
        List<RawFeature> rawCds = new ArrayList<>();
        List<RawFeature> rawUtr5 = new ArrayList<>();
        List<RawFeature> rawUtr3 = new ArrayList<>();

        HeaderInfo header = new HeaderInfo();
        AnnotationFormat[] detected = {format};
        List<String> chromosomes = new ArrayList<>();
        Map<String, int[]> chromosomeBounds = new LinkedHashMap<>();
        int featureCount = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            boolean inFasta = false;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                // Strip a UTF-8 BOM, otherwise the first directive is not recognised.
                if (firstLine) {
                    firstLine = false;
                    if (!line.isEmpty() && line.charAt(0) == '\uFEFF') {
                        line = line.substring(1);
                    }
                }
                // An annotation file may carry a FASTA section at the end.
                if (line.startsWith("##FASTA") || line.startsWith(">")) {
                    inFasta = true;
                    continue;
                }
                if (inFasta) {
                    continue;
                }
                if (line.isBlank() || line.startsWith("#")) {
                    collectHeader(line, header);
                    continue;
                }

                String[] cols = line.split("\t", -1);
                if (cols.length < 9) {
                    continue; // Malformed row, skip
                }
                if (detected[0] == null) {
                    detected[0] = header.declaredFormat != null
                            ? header.declaredFormat : detectFormat(cols[8]);
                }
                RawFeature raw = RawFeature.fromColumns(cols, dialectOf(detected[0]));
                if (raw == null) {
                    continue;
                }
                featureCount++;

                if (!chromosomes.contains(raw.chromosome)) {
                    chromosomes.add(raw.chromosome);
                }
                trackBounds(chromosomeBounds, raw);

                FeatureType type = FeatureType.fromSoTerm(raw.type);
                switch (type) {
                    case GENE -> rawGenes.add(raw);
                    case MRNA, TRANSCRIPT -> rawTranscripts.add(raw);
                    case EXON -> rawExons.add(raw);
                    case CDS -> rawCds.add(raw);
                    case FIVE_UTR -> rawUtr5.add(raw);
                    case THREE_UTR -> rawUtr3.add(raw);
                    default -> { /* Other types do not take part in structure assembly */ }
                }
            }
        }

        AnnotationFormat usedFormat = detected[0] != null ? detected[0] : DEFAULT_FORMAT;

        // Pass 2: assemble the hierarchy rooted at each gene.
        Map<String, Gene> geneById = new LinkedHashMap<>();
        for (RawFeature raw : rawGenes) {
            Gene gene = new Gene(raw.id, raw.name, raw.chromosome, raw.start, raw.end, raw.strand);
            gene.setAttributes(raw.attributes);
            gene.setSpecies(header.species);
            gene.setLocusTag(raw.get("locus_tag", "gene_id", "gene"));
            gene.setDescription(raw.get("Note", "description", "product"));
            gene.setAlias(raw.get("Alias", "gene_name"));
            geneById.put(raw.id, gene);
        }

        Map<String, Transcript> transcriptById = new HashMap<>();
        for (RawFeature raw : rawTranscripts) {
            Gene gene = findGene(raw, geneById);
            if (gene == null) {
                continue;
            }
            Transcript transcript = new Transcript(raw.id, raw.chromosome, raw.start, raw.end, raw.strand);
            transcript.setAttributes(raw.attributes);
            transcript.setName(raw.name);
            transcript.setProteinId(raw.get("protein_id"));
            transcript.setSpecies(header.species);
            Transcript retained = transcriptById.putIfAbsent(raw.id, transcript);
            if (retained == null) {
                gene.addTranscript(transcript);
            } else {
                // Duplicate transcript IDs must keep pointing at the retained
                // hierarchy node; otherwise child exons/CDS/UTRs attach to an
                // orphan object and silently disappear from the gene model.
                transcript = retained;
            }
        }

        for (RawFeature raw : rawExons) {
            Transcript parent = resolveTranscript(raw, transcriptById);
            if (parent == null) {
                continue;
            }
            Exon exon = new Exon(raw.id, raw.chromosome, raw.start, raw.end, raw.strand);
            exon.setAttributes(raw.attributes);
            exon.setName(raw.name);
            parent.addExon(exon);
        }

        for (RawFeature raw : rawCds) {
            Transcript parent = resolveTranscript(raw, transcriptById);
            if (parent == null) {
                continue;
            }
            GenomicFeature cds = new GenomicFeature(raw.id, raw.chromosome, FeatureType.CDS,
                    raw.start, raw.end, raw.strand);
            cds.setAttributes(raw.attributes);
            attachToExon(parent, cds);
        }
        for (RawFeature raw : rawUtr5) {
            attachUtr(raw, transcriptById, FeatureType.FIVE_UTR);
        }
        for (RawFeature raw : rawUtr3) {
            attachUtr(raw, transcriptById, FeatureType.THREE_UTR);
        }

        // Wrap up: order exons and flag the representative transcript.
        List<Gene> genes = new ArrayList<>(geneById.values());
        for (Gene gene : genes) {
            for (Transcript transcript : gene.getTranscripts()) {
                Exon.sortByTranscriptionOrder(transcript.getExons(), transcript.getStrand());
            }
            Transcript canonical = gene.getCanonicalTranscript();
            if (canonical != null) {
                canonical.setCanonical(true);
            }
        }
        genes.sort((a, b) -> {
            int bySeq = compareSequence(a.getChromosome(), b.getChromosome(), chromosomes);
            return bySeq != 0 ? bySeq : Integer.compare(a.getStart(), b.getStart());
        });

        GenomeMetadata metadata = buildMetadata(header, label, chromosomes, chromosomeBounds);
        return new ParseResult(genes, featureCount, usedFormat, metadata);
    }

    // ------------------------------------------------------------------
    // Header and format handling
    // ------------------------------------------------------------------

    /** Mutable holder for the values collected from the comment header. */
    private static final class HeaderInfo {
        String species;
        String assembly;
        String description;
        AnnotationFormat declaredFormat;
        final List<SequenceRegion> regions = new ArrayList<>();
    }

    private static void collectHeader(String line, HeaderInfo header) {
        if (!line.startsWith("#")) {
            return;
        }
        String lower = line.toLowerCase();
        if (lower.startsWith("##sequence-region")) {
            // ##sequence-region <id> <start> <end>
            String[] parts = line.trim().split("\\s+");
            if (parts.length >= 4) {
                try {
                    header.regions.add(new SequenceRegion(parts[1],
                            Integer.parseInt(parts[2]), Integer.parseInt(parts[3])));
                } catch (NumberFormatException ignored) {
                    // Malformed directive, ignore
                }
            }
            return;
        }
        // Real-world headers are inconsistent: some files use "##species=X",
        // others "##species\tX" or "##species X". Accept all of them.
        if (header.species == null) {
            header.species = headerValue(line, lower, "species");
        }
        if (header.assembly == null) {
            header.assembly = headerValue(line, lower, "assembly");
        }
        if (header.description == null) {
            header.description = headerValue(line, lower, "description");
        }
        if (header.declaredFormat == null && lower.startsWith("##gff-version")) {
            String version = lower.replace("##gff-version", "").trim();
            if (version.startsWith("3")) {
                header.declaredFormat = AnnotationFormat.GFF3;
            } else if (version.startsWith("2")) {
                header.declaredFormat = AnnotationFormat.GTF;
            }
        }
    }

    /**
     * Reads a header value regardless of the separator used after the key:
     * {@code key=value}, {@code key value} or {@code key<TAB>value}.
     *
     * <p>The key must open the directive, so that a mention of "species"
     * inside a free-text description is not mistaken for the directive itself.</p>
     */
    private static String headerValue(String line, String lower, String key) {
        String stripped = lower.replaceFirst("^#+", "");
        if (!stripped.startsWith(key)) {
            return null;
        }
        int afterKey = stripped.length() > key.length() ? stripped.length() : 0;
        if (afterKey == 0) {
            return null;
        }
        // Reject a prefix match such as "speciesx" masquerading as "species".
        char boundary = stripped.charAt(key.length());
        if (Character.isLetterOrDigit(boundary)) {
            return null;
        }
        // Slice the original line at the same offset to preserve its casing.
        int hashCount = line.length() - line.replaceFirst("^#+", "").length();
        String rest = line.substring(hashCount + key.length());
        rest = rest.replaceFirst("^[\\s=:]+", "");
        if (rest.isEmpty()) {
            return null;
        }
        if (rest.startsWith("\"")) {
            int close = rest.indexOf('"', 1);
            if (close > 0) {
                return rest.substring(1, close).trim();
            }
        }
        return rest.trim();
    }

    /**
     * Detects the attribute dialect from a column-9 sample.
     * {@code gene_id "X";} marks GTF, {@code ID=X;} marks GFF3.
     */
    static AnnotationFormat detectFormat(String attributeColumn) {
        if (attributeColumn == null) {
            return DEFAULT_FORMAT;
        }
        String trimmed = attributeColumn.trim();
        // GTF quotes its values; GFF3 never does.
        if (trimmed.contains("\"")) {
            return AnnotationFormat.GTF;
        }
        if (trimmed.contains("=")) {
            return AnnotationFormat.GFF3;
        }
        return DEFAULT_FORMAT;
    }

    private static AttributeDialect dialectOf(AnnotationFormat format) {
        return format == AnnotationFormat.GTF ? AttributeDialect.GTF : AttributeDialect.GFF3;
    }

    private static GenomeMetadata buildMetadata(HeaderInfo header, String label,
                                                List<String> chromosomes,
                                                Map<String, int[]> bounds) {
        // Preserve declaration order, merging duplicate declarations and observed
        // bounds. Incomplete headers must never clip genes or hide a sequence.
        Map<String, SequenceRegion> regions = new LinkedHashMap<>();
        for (SequenceRegion region : header.regions) {
            if (region.id() != null && !region.id().isBlank()
                    && region.start() >= 1 && region.end() >= region.start()) {
                regions.merge(region.id(), region, Gff3Parser::mergeRegions);
            }
        }
        for (String chromosome : chromosomes) {
            int[] b = bounds.get(chromosome);
            SequenceRegion observed = b == null ? new SequenceRegion(chromosome, 1, 1)
                    : new SequenceRegion(chromosome, b[0], b[1]);
            regions.merge(chromosome, observed, Gff3Parser::mergeRegions);
        }
        return GenomeMetadata.fromAnnotation(header.species, header.assembly,
                header.description, label, new ArrayList<>(regions.values()));
    }

    private static SequenceRegion mergeRegions(SequenceRegion a, SequenceRegion b) {
        return new SequenceRegion(a.id(), Math.min(a.start(), b.start()),
                Math.max(a.end(), b.end()));
    }

    private static void trackBounds(Map<String, int[]> bounds, RawFeature raw) {
        int[] existing = bounds.get(raw.chromosome);
        if (existing == null) {
            bounds.put(raw.chromosome, new int[]{raw.start, raw.end});
        } else {
            existing[0] = Math.min(existing[0], raw.start);
            existing[1] = Math.max(existing[1], raw.end);
        }
    }

    /**
     * Orders chromosomes the way they appear in the file, which matches the
     * declaration order readers expect; unknown names sort last alphabetically.
     */
    private static int compareSequence(String a, String b, List<String> order) {
        int ia = order.indexOf(a);
        int ib = order.indexOf(b);
        if (ia < 0 && ib < 0) {
            return a.compareTo(b);
        }
        if (ia < 0) {
            return 1;
        }
        if (ib < 0) {
            return -1;
        }
        return Integer.compare(ia, ib);
    }

    // ------------------------------------------------------------------
    // Hierarchy assembly helpers
    // ------------------------------------------------------------------

    private void attachUtr(RawFeature raw, Map<String, Transcript> transcriptById, FeatureType type) {
        Transcript parent = resolveTranscript(raw, transcriptById);
        if (parent == null) {
            return;
        }
        GenomicFeature utr = new GenomicFeature(raw.id, raw.chromosome, type,
                raw.start, raw.end, raw.strand);
        utr.setAttributes(raw.attributes);
        attachToExon(parent, utr);
    }

    /**
     * Attaches a CDS or UTR segment to the exon it overlaps.
     * If the transcript has no exons registered yet, a carrier exon is created
     * so nothing is lost.
     */
    private void attachToExon(Transcript transcript, GenomicFeature segment) {
        for (Exon exon : transcript.getExons()) {
            if (segment.getStart() <= exon.getEnd() && segment.getEnd() >= exon.getStart()) {
                if (segment.getType() == FeatureType.CDS) {
                    exon.addCds(segment);
                } else {
                    exon.addUtr(segment);
                }
                return;
            }
        }
        Exon synthetic = new Exon(segment.getId(), segment.getChromosome(),
                segment.getStart(), segment.getEnd(), segment.getStrand());
        if (segment.getType() == FeatureType.CDS) {
            synthetic.addCds(segment);
        } else {
            synthetic.addUtr(segment);
        }
        transcript.addExon(synthetic);
    }

    /** Locates the transcript via the Parent attribute. */
    private Transcript resolveTranscript(RawFeature raw, Map<String, Transcript> transcriptById) {
        for (String parentId : raw.getParents()) {
            Transcript t = transcriptById.get(parentId);
            if (t != null) {
                return t;
            }
        }
        return null;
    }

    /** Locates the gene via the Parent attribute, with graceful fallbacks. */
    private Gene findGene(RawFeature raw, Map<String, Gene> geneById) {
        for (String parentId : raw.getParents()) {
            Gene gene = geneById.get(parentId);
            if (gene != null) {
                return gene;
            }
        }
        // Some mRNA rows have no Parent; fall back to an ID prefix relationship.
        if (raw.id != null) {
            for (Map.Entry<String, Gene> e : geneById.entrySet()) {
                if (raw.id.startsWith(e.getKey()) || e.getKey().startsWith(raw.id)) {
                    return e.getValue();
                }
            }
        }
        // Final fallback: assign to the gene with the largest coordinate overlap.
        Gene best = null;
        int bestOverlap = 0;
        for (Gene gene : geneById.values()) {
            if (!gene.getChromosome().equals(raw.chromosome)) {
                continue;
            }
            int overlapStart = Math.max(gene.getStart(), raw.start);
            int overlapEnd = Math.min(gene.getEnd(), raw.end);
            int overlap = overlapEnd - overlapStart + 1;
            if (overlap > bestOverlap) {
                bestOverlap = overlap;
                best = gene;
            }
        }
        return best;
    }

    // ------------------------------------------------------------------
    // Attribute dialects
    // ------------------------------------------------------------------

    /** How column 9 encodes its key/value pairs. */
    private enum AttributeDialect {
        GFF3, GTF
    }

    /**
     * One raw annotation row, with attribute parsing that adapts to the dialect.
     */
    private static final class RawFeature {
        String chromosome;
        String source;
        String type;
        int start;
        int end;
        char strand;
        String attributes;
        String id;
        String name;
        private Map<String, String> attrMap;

        static RawFeature fromColumns(String[] cols, AttributeDialect dialect) {
            try {
                RawFeature f = new RawFeature();
                f.chromosome = cols[0];
                f.source = cols[1];
                f.type = cols[2];
                f.start = Integer.parseInt(cols[3].trim());
                f.end = Integer.parseInt(cols[4].trim());
                f.strand = cols[6].isEmpty() ? '.' : cols[6].charAt(0);
                f.attributes = cols[8];
                f.attrMap = dialect == AttributeDialect.GTF
                        ? parseGtfAttributes(cols[8]) : parseGff3Attributes(cols[8]);
                f.id = f.resolveId(dialect);
                f.name = f.resolveName(dialect);
                return f;
            } catch (NumberFormatException ex) {
                return null;
            }
        }

        /**
         * In GFF3 the ID is explicit. GTF has no ID, so the transcript or gene
         * identifier plays that role depending on the feature type.
         */
        private String resolveId(AttributeDialect dialect) {
            String explicit = attrMap.get("ID");
            if (explicit != null) {
                return explicit;
            }
            String featureType = type == null ? "" : type.toLowerCase();
            if (featureType.contains("gene")) {
                return firstNonBlank("gene_id", "ID", "gene");
            }
            if (featureType.contains("rna") || featureType.contains("transcript")) {
                return firstNonBlank("transcript_id", "ID", "gene_id");
            }
            // Exon/CDS rows: prefer the transcript so segments group correctly,
            // but fall back to the gene so nothing is dropped.
            String transcript = attrMap.get("transcript_id");
            if (transcript != null && !transcript.isBlank()) {
                return transcript + ":" + start + "-" + end;
            }
            return firstNonBlank("gene_id", "ID");
        }

        private String resolveName(AttributeDialect dialect) {
            String name = attrMap.get("Name");
            if (name != null && !name.isBlank()) {
                return name;
            }
            if (dialect == AttributeDialect.GTF) {
                return firstNonBlank("gene_name", "transcript_name", "gene_id");
            }
            return null;
        }

        private String firstNonBlank(String... keys) {
            for (String key : keys) {
                String v = attrMap.get(key);
                if (v != null && !v.isBlank()) {
                    return v;
                }
            }
            return null;
        }

        /** Parses the GFF3 {@code key=value;key=value} form; values may be escaped. */
        private static Map<String, String> parseGff3Attributes(String attrString) {
            Map<String, String> map = new HashMap<>();
            for (String pair : attrString.split(";")) {
                if (pair.isBlank()) {
                    continue;
                }
                int eq = pair.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String key = pair.substring(0, eq).trim();
                String value = decode(pair.substring(eq + 1).trim());
                map.put(key, value);
            }
            return map;
        }

        /** Parses the GTF {@code key "value"; } form, also tolerating {@code key value;}. */
        private static Map<String, String> parseGtfAttributes(String attrString) {
            Map<String, String> map = new HashMap<>();
            for (String pair : attrString.split(";")) {
                String trimmed = pair.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                int space = trimmed.indexOf(' ');
                String key;
                String value;
                if (space < 0) {
                    key = trimmed;
                    value = "";
                } else {
                    key = trimmed.substring(0, space).trim();
                    value = trimmed.substring(space + 1).trim();
                }
                if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }
                if (!key.isEmpty()) {
                    map.put(key, decode(value));
                }
            }
            return map;
        }

        /** Handles the percent-escaping defined by the GFF3 spec. */
        private static String decode(String value) {
            if (value.indexOf('%') < 0) {
                return value;
            }
            byte[] bytes = new byte[value.length()];
            int n = 0;
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                if (c == '%' && i + 2 < value.length()) {
                    try {
                        bytes[n++] = (byte) Integer.parseInt(value.substring(i + 1, i + 3), 16);
                        i += 2;
                        continue;
                    } catch (NumberFormatException ignored) {
                        // Preserve malformed escapes literally below.
                    }
                }
                // Keep ordinary UTF-16 code units by encoding them as UTF-8.
                byte[] encoded = String.valueOf(c).getBytes(StandardCharsets.UTF_8);
                for (byte b : encoded) bytes[n++] = b;
            }
            return new String(bytes, 0, n, StandardCharsets.UTF_8);
        }

        String get(String... keys) {
            for (String key : keys) {
                String v = attrMap.get(key);
                if (v != null && !v.isBlank()) {
                    return v;
                }
            }
            return null;
        }

        List<String> getParents() {
            String parent = attrMap.get("Parent");
            if (parent == null || parent.isBlank()) {
                // GTF has no Parent attribute: the transcript_id links exon/CDS rows
                // back to the mRNA, and gene_id links the mRNA back to the gene.
                String featureType = type == null ? "" : type.toLowerCase();
                if (featureType.contains("gene")) {
                    return List.of();
                }
                if (featureType.contains("rna") || featureType.contains("transcript")) {
                    String geneId = attrMap.get("gene_id");
                    return geneId == null || geneId.isBlank() ? List.of() : List.of(geneId);
                }
                String transcriptId = attrMap.get("transcript_id");
                if (transcriptId == null || transcriptId.isBlank()) {
                    return List.of();
                }
                return List.of(transcriptId);
            }
            List<String> ids = new ArrayList<>();
            for (String p : parent.split(",")) {
                if (!p.isBlank()) {
                    ids.add(p.trim());
                }
            }
            return ids;
        }
    }
}
