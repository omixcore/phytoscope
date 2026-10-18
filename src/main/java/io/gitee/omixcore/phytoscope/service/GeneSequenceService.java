package io.gitee.omixcore.phytoscope.service;

import io.gitee.omixcore.phytoscope.model.Exon;
import io.gitee.omixcore.phytoscope.model.FeatureType;
import io.gitee.omixcore.phytoscope.model.Gene;
import io.gitee.omixcore.phytoscope.model.GenomicFeature;
import io.gitee.omixcore.phytoscope.model.Transcript;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 * Loads a genome FASTA and extracts strand-aware gene/transcript sequences
 * from the currently loaded annotation model. Coordinates are 1-based inclusive
 * in the annotation and converted only at the sequence slicing boundary.
 *
 * <p>The genome FASTA must match the annotation assembly. UTR sequence is
 * taken from explicit annotation features when available; when an annotation
 * has exons and CDS but no UTR features, terminal exonic bases outside the CDS
 * boundaries are inferred and clearly labelled as inferred.</p>
 */
public final class GeneSequenceService {

    public static final String ARABIDOPSIS_TAIR10_RESOURCE =
            "/data/species/arabidopsis/TAIR10_genomic.fna.gz";

    private static final Pattern CHROMOSOME_WORD =
            Pattern.compile("(?i)\\bchromosome\\s+([0-9]+|[a-z]+)\\b");
    private static final Pattern CHROMOSOME_COLON =
            Pattern.compile("(?i)chromosome:[^:]+:([0-9]+|[a-z]+):");
    private static final String CODONS =
            "TTT TTC TTA TTG TCT TCC TCA TCG TAT TAC TAA TAG TGT TGC TGA TGG " +
            "CTT CTC CTA CTG CCT CCC CCA CCG CAT CAC CAA CAG CGT CGC CGA CGG " +
            "ATT ATC ATA ATG ACT ACC ACA ACG AAT AAC AAA AAG AGT AGC AGA AGG " +
            "GTT GTC GTA GTG GCT GCC GCA GCG GAT GAC GAA GAG GGT GGC GGA GGG";
    private static final String AMINO_ACIDS =
            "FFLLSSSSYY**CC*WLLLLPPPPHHQQRRRRIIIMTTTTNNKKSSRRVVVVAAAADDEEGGGG";
    private static final Map<String, Character> CODON_TABLE = buildCodonTable();

    private Map<String, String> sequences = Map.of();
    private Map<String, String> aliases = Map.of();
    private String sourceLabel;

    public record TranscriptSequences(
            String transcriptId,
            String transcriptName,
            String proteinId,
            boolean canonical,
            int exonCount,
            String transcriptSequence,
            String cdsSequence,
            String fivePrimeUtrSequence,
            String threePrimeUtrSequence,
            String proteinSequence,
            String fivePrimeUtrSource,
            String threePrimeUtrSource,
            String proteinNote) {
    }

    public record GeneSequences(
            String geneId,
            String chromosome,
            int start,
            int end,
            char strand,
            String sourceLabel,
            String genomicSpanSequence,
            List<TranscriptSequences> transcripts) {
        public GeneSequences {
            transcripts = List.copyOf(transcripts);
        }
    }

    public boolean isLoaded() {
        return !sequences.isEmpty();
    }

    public int getSequenceCount() {
        return sequences.size();
    }

    public String getSourceLabel() {
        return sourceLabel;
    }

    public void clear() {
        sequences = Map.of();
        aliases = Map.of();
        sourceLabel = null;
    }

    public void replaceWith(GeneSequenceService other) {
        Objects.requireNonNull(other, "other");
        sequences = other.sequences;
        aliases = other.aliases;
        sourceLabel = other.sourceLabel;
    }

    /**
     * Loads the bundled TAIR10 genome for the Arabidopsis example when present.
     * Other examples intentionally require the user to provide the matching
     * genome FASTA rather than silently borrowing a different assembly.
     */
    public void loadBundledForDataset(String datasetId) throws IOException {
        clear();
        if (!"arabidopsis".equals(datasetId)) {
            return;
        }
        try (InputStream check = GeneSequenceService.class.getResourceAsStream(ARABIDOPSIS_TAIR10_RESOURCE)) {
            if (check == null) {
                return;
            }
        }
        loadFromResource(ARABIDOPSIS_TAIR10_RESOURCE);
    }

    public void loadFromResource(String resourcePath) throws IOException {
        try (InputStream input = GeneSequenceService.class.getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IOException("Genome FASTA resource not found: " + resourcePath);
            }
            load(input, resourcePath);
        }
    }

    public void loadFromFile(Path path) throws IOException {
        Objects.requireNonNull(path, "path");
        try (InputStream input = Files.newInputStream(path)) {
            load(input, path.toAbsolutePath().toString());
        }
    }

    /** Useful for tests and embedded FASTA snippets; applies the same validation as file loading. */
    public void loadFromContent(String fasta, String label) throws IOException {
        load(new java.io.ByteArrayInputStream(fasta.getBytes(StandardCharsets.UTF_8)), label);
    }

    private void load(InputStream raw, String label) throws IOException {
        BufferedInputStream buffered = new BufferedInputStream(raw);
        buffered.mark(2);
        int first = buffered.read();
        int second = buffered.read();
        buffered.reset();
        InputStream decoded = first == 0x1f && second == 0x8b ? new GZIPInputStream(buffered) : buffered;

        Map<String, String> nextSequences = new LinkedHashMap<>();
        Map<String, String> nextAliases = new LinkedHashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(decoded, StandardCharsets.UTF_8))) {
            String id = null;
            String description = "";
            StringBuilder sequence = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(">")) {
                    addRecord(id, description, sequence, nextSequences, nextAliases, label);
                    String header = line.substring(1).trim();
                    if (header.isEmpty()) throw new IOException("FASTA contains an empty header");
                    int space = header.indexOf(' ');
                    id = space < 0 ? header : header.substring(0, space);
                    description = space < 0 ? "" : header.substring(space + 1);
                    sequence.setLength(0);
                } else if (!line.isBlank()) {
                    if (id == null) throw new IOException("FASTA sequence data occurs before the first > header");
                    String cleaned = line.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
                    if (!cleaned.matches("[ACGTURYSWKMBDHVN.*-]+")) {
                        throw new IOException("Non-nucleotide characters found in genome FASTA record " + id
                                + "; select a genomic DNA FASTA, not a protein FASTA");
                    }
                    sequence.append(cleaned.replace('U', 'T'));
                }
            }
            addRecord(id, description, sequence, nextSequences, nextAliases, label);
        }
        if (nextSequences.isEmpty()) throw new IOException("No FASTA records were found in " + label);
        sequences = Map.copyOf(nextSequences);
        aliases = Map.copyOf(nextAliases);
        sourceLabel = label;
    }

    private static void addRecord(String id, String description, StringBuilder sequence,
                                  Map<String, String> target, Map<String, String> targetAliases,
                                  String label) throws IOException {
        if (id == null) return;
        if (sequence.isEmpty()) throw new IOException("Empty FASTA record " + id + " in " + label);
        String seq = sequence.toString();
        if (target.putIfAbsent(id, seq) != null) {
            throw new IOException("Duplicate FASTA record ID " + id + " in " + label);
        }
        addAlias(targetAliases, id, id);
        Matcher word = CHROMOSOME_WORD.matcher(description);
        if (word.find()) addChromosomeAliases(targetAliases, word.group(1), id);
        Matcher colon = CHROMOSOME_COLON.matcher(description);
        if (colon.find()) addChromosomeAliases(targetAliases, colon.group(1), id);
    }

    private static void addChromosomeAliases(Map<String, String> aliases, String chromosome, String recordId) {
        addAlias(aliases, chromosome, recordId);
        addAlias(aliases, "chr" + chromosome, recordId);
    }

    private static void addAlias(Map<String, String> aliases, String alias, String recordId) {
        String key = normalizeSequenceName(alias);
        if (!key.isEmpty()) aliases.putIfAbsent(key, recordId);
    }

    private static String normalizeSequenceName(String value) {
        if (value == null) return "";
        String normalized = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        if (normalized.startsWith("chr") && normalized.length() > 3) {
            normalized = normalized.substring(3);
        }
        return normalized;
    }

    public GeneSequences extract(Gene gene) throws IOException {
        Objects.requireNonNull(gene, "gene");
        if (!isLoaded()) throw new IOException("No genome FASTA loaded. Import a FASTA matching the annotation assembly.");
        String chromosomeId = resolveSequenceId(gene.getChromosome());
        String chromosomeSequence = sequences.get(chromosomeId);
        String geneSequence = extractInterval(chromosomeSequence, gene.getStart(), gene.getEnd(),
                gene.getChromosome(), gene.getStrand());
        List<TranscriptSequences> transcripts = new ArrayList<>();
        for (Transcript transcript : gene.getTranscripts()) {
            transcripts.add(extractTranscript(transcript, chromosomeSequence));
        }
        return new GeneSequences(gene.getId(), gene.getChromosome(), gene.getStart(), gene.getEnd(),
                gene.getStrand(), sourceLabel, geneSequence, transcripts);
    }

    /** Extract a 1-based closed genomic interval in the requested transcription orientation. */
    public String extractInterval(String chromosome, int start, int end, char strand) throws IOException {
        if (start < 1 || end < start) throw new IOException("Interval must be 1-based, closed, and non-empty");
        if (!isLoaded()) throw new IOException("No genome FASTA loaded.");
        String id = resolveSequenceId(chromosome);
        return extractInterval(sequences.get(id), start, end, chromosome, strand);
    }

    /** Promoter window upstream of a gene TSS, clipped to the sequence boundary. */
    public String extractPromoter(Gene gene, int upstream, int downstream) throws IOException {
        Objects.requireNonNull(gene, "gene");
        if (gene.getStrand() != '+' && gene.getStrand() != '-')
            throw new IOException("Promoter direction requires a known gene strand");
        if (upstream < 0 || downstream < 0 || (long) upstream + downstream == 0)
            throw new IOException("Promoter lengths must be non-negative and not both zero");
        int tss = gene.getStrand() == '-' ? gene.getEnd() : gene.getStart();
        String sequenceId = resolveSequenceId(gene.getChromosome());
        int sequenceLength = sequences.get(sequenceId).length();
        // Match the research interval convention: downstream includes the TSS,
        // so an interior window has exactly upstream + downstream bases.
        long rawStart = gene.getStrand() == '-' ? (long)tss - downstream + 1 : (long)tss - upstream;
        long rawEnd = gene.getStrand() == '-' ? (long)tss + upstream : (long)tss + downstream - 1;
        int start = (int)Math.max(1, Math.min(sequenceLength, rawStart));
        int end = (int)Math.max(1, Math.min(sequenceLength, rawEnd));
        return extractInterval(gene.getChromosome(), start, end, gene.getStrand());
    }

    private TranscriptSequences extractTranscript(Transcript transcript, String chromosomeSequence) throws IOException {
        List<Exon> exons = new ArrayList<>(transcript.getExons());
        exons.sort(transcript.getStrand() == '-'
                ? Comparator.comparingInt(GenomicFeature::getStart).reversed()
                : Comparator.comparingInt(GenomicFeature::getStart));
        StringBuilder transcriptSequence = new StringBuilder();
        for (Exon exon : exons) {
            transcriptSequence.append(extractInterval(chromosomeSequence, exon.getStart(), exon.getEnd(),
                    transcript.getChromosome(), transcript.getStrand()));
        }

        List<GenomicFeature> cdsSegments = distinctSegments(transcript.getAllCdsSegments());
        cdsSegments.sort(segmentOrder(transcript.getStrand()));
        String cds = joinSegments(chromosomeSequence, cdsSegments, transcript.getChromosome(), transcript.getStrand());

        List<GenomicFeature> utr5 = annotatedUtrSegments(transcript, FeatureType.FIVE_UTR);
        List<GenomicFeature> utr3 = annotatedUtrSegments(transcript, FeatureType.THREE_UTR);
        String utr5Source = "annotated";
        String utr3Source = "annotated";
        if (utr5.isEmpty() && !cdsSegments.isEmpty()) {
            utr5 = inferTerminalUtr(exons, cdsSegments, transcript.getStrand(), true);
            utr5Source = utr5.isEmpty() ? "not annotated / not inferable" : "inferred from exon/CDS boundaries";
        }
        if (utr3.isEmpty() && !cdsSegments.isEmpty()) {
            utr3 = inferTerminalUtr(exons, cdsSegments, transcript.getStrand(), false);
            utr3Source = utr3.isEmpty() ? "not annotated / not inferable" : "inferred from exon/CDS boundaries";
        }
        utr5.sort(segmentOrder(transcript.getStrand()));
        utr3.sort(segmentOrder(transcript.getStrand()));
        String fivePrime = joinSegments(chromosomeSequence, utr5, transcript.getChromosome(), transcript.getStrand());
        String threePrime = joinSegments(chromosomeSequence, utr3, transcript.getChromosome(), transcript.getStrand());

        String protein = translateCds(cds);
        String proteinNote;
        if (cds.isEmpty()) {
            proteinNote = "No annotated CDS; protein sequence is unavailable for this transcript.";
        } else if (cds.length() % 3 != 0) {
            proteinNote = "CDS length is not divisible by three; translation may represent a partial CDS or an annotation inconsistency.";
        } else {
            proteinNote = "Translated from the annotated CDS using the standard nuclear genetic code; terminal stop codon excluded.";
        }
        return new TranscriptSequences(transcript.getId(), transcript.getDisplayName(), transcript.getProteinId(),
                transcript.isCanonical(), exons.size(), transcriptSequence.toString(), cds, fivePrime, threePrime,
                protein, utr5Source, utr3Source, proteinNote);
    }

    private static List<GenomicFeature> annotatedUtrSegments(Transcript transcript, FeatureType type) {
        List<GenomicFeature> segments = new ArrayList<>();
        for (Exon exon : transcript.getExons()) {
            for (GenomicFeature segment : exon.getUtrSegments()) {
                if (segment.getType() == type) segments.add(segment);
            }
        }
        for (GenomicFeature segment : transcript.getUnassignedSegments()) {
            if (segment.getType() == type) segments.add(segment);
        }
        return distinctSegments(segments);
    }

    private static List<GenomicFeature> inferTerminalUtr(List<Exon> exons, List<GenomicFeature> cds,
                                                          char strand, boolean fivePrime) {
        int minCds = cds.stream().mapToInt(GenomicFeature::getStart).min().orElseThrow();
        int maxCds = cds.stream().mapToInt(GenomicFeature::getEnd).max().orElseThrow();
        List<GenomicFeature> inferred = new ArrayList<>();
        for (Exon exon : exons) {
            int start;
            int end;
            boolean highSide = (strand == '-') == fivePrime;
            if (highSide) {
                start = Math.max(exon.getStart(), maxCds + 1);
                end = exon.getEnd();
            } else {
                start = exon.getStart();
                end = Math.min(exon.getEnd(), minCds - 1);
            }
            if (end >= start) {
                inferred.add(new GenomicFeature(null, exon.getChromosome(),
                        fivePrime ? FeatureType.FIVE_UTR : FeatureType.THREE_UTR,
                        start, end, strand));
            }
        }
        return inferred;
    }

    private String resolveSequenceId(String chromosome) throws IOException {
        String exact = sequences.keySet().stream().filter(id -> id.equalsIgnoreCase(chromosome)).findFirst().orElse(null);
        if (exact != null) return exact;
        String resolved = aliases.get(normalizeSequenceName(chromosome));
        if (resolved == null) {
            throw new IOException("FASTA source " + sourceLabel + " has no sequence matching annotation chromosome "
                    + chromosome + ". Confirm that the genome assembly and chromosome naming match the annotation.");
        }
        return resolved;
    }

    private static List<GenomicFeature> distinctSegments(List<GenomicFeature> input) {
        Map<String, GenomicFeature> unique = new LinkedHashMap<>();
        for (GenomicFeature segment : input) {
            String key = segment.getChromosome() + ":" + segment.getStart() + ":" + segment.getEnd()
                    + ":" + segment.getType();
            unique.putIfAbsent(key, segment);
        }
        return new ArrayList<>(unique.values());
    }

    private static Comparator<GenomicFeature> segmentOrder(char strand) {
        Comparator<GenomicFeature> comparator = Comparator.comparingInt(GenomicFeature::getStart);
        return strand == '-' ? comparator.reversed() : comparator;
    }

    private String joinSegments(String chromosomeSequence, List<GenomicFeature> segments,
                                String chromosome, char strand) throws IOException {
        StringBuilder joined = new StringBuilder();
        for (GenomicFeature segment : segments) {
            joined.append(extractInterval(chromosomeSequence, segment.getStart(), segment.getEnd(), chromosome, strand));
        }
        return joined.toString();
    }

    private static String extractInterval(String chromosomeSequence, int start, int end,
                                          String chromosome, char strand) throws IOException {
        if (start < 1 || end < start || end > chromosomeSequence.length()) {
            throw new IOException("Annotation interval " + chromosome + ":" + start + "-" + end
                    + " falls outside the loaded genome sequence length " + chromosomeSequence.length()
                    + ". The FASTA may belong to a different assembly.");
        }
        String sequence = chromosomeSequence.substring(start - 1, end);
        return strand == '-' ? reverseComplement(sequence) : sequence;
    }

    public static String reverseComplement(String sequence) {
        StringBuilder result = new StringBuilder(sequence.length());
        for (int i = sequence.length() - 1; i >= 0; i--) {
            result.append(complement(sequence.charAt(i)));
        }
        return result.toString();
    }

    private static char complement(char nucleotide) {
        return switch (Character.toUpperCase(nucleotide)) {
            case 'A' -> 'T';
            case 'T', 'U' -> 'A';
            case 'C' -> 'G';
            case 'G' -> 'C';
            case 'R' -> 'Y';
            case 'Y' -> 'R';
            case 'S' -> 'S';
            case 'W' -> 'W';
            case 'K' -> 'M';
            case 'M' -> 'K';
            case 'B' -> 'V';
            case 'V' -> 'B';
            case 'D' -> 'H';
            case 'H' -> 'D';
            case 'N', '.', '-' -> Character.toUpperCase(nucleotide);
            default -> 'N';
        };
    }

    public static String translateCds(String cds) {
        if (cds == null || cds.isEmpty()) return "";
        StringBuilder protein = new StringBuilder(cds.length() / 3);
        for (int i = 0; i + 2 < cds.length(); i += 3) {
            String codon = cds.substring(i, i + 3).toUpperCase(Locale.ROOT);
            protein.append(CODON_TABLE.getOrDefault(codon, 'X'));
        }
        if (!protein.isEmpty() && protein.charAt(protein.length() - 1) == '*') {
            protein.setLength(protein.length() - 1);
        }
        return protein.toString();
    }

    private static Map<String, Character> buildCodonTable() {
        String[] codons = CODONS.split(" ");
        Map<String, Character> table = new LinkedHashMap<>();
        for (int i = 0; i < codons.length; i++) table.put(codons[i], AMINO_ACIDS.charAt(i));
        return Map.copyOf(table);
    }
}
