package io.gitee.omixcore.phytoscope.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gitee.omixcore.phytoscope.comparison.ComparisonFigure;
import io.gitee.omixcore.phytoscope.model.*;
import io.gitee.omixcore.phytoscope.service.*;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.security.*;
import java.time.Instant;
import java.util.*;

/** Measured single-process benchmark and machine-readable parser inventory; no native display required. */
public final class BenchmarkRunner {
    private BenchmarkRunner() { }
    public static void run(String[] args) throws Exception {
        Map<String, String> options = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--benchmark")) continue;
            if (!Set.of("--annotation", "--example", "--out").contains(args[i]) || i + 1 >= args.length) throw new IllegalArgumentException("Invalid benchmark option: " + args[i]);
            String key = args[i]; options.put(key, args[++i]);
        }
        if (options.containsKey("--annotation") == options.containsKey("--example")) throw new IllegalArgumentException("Choose one --annotation or --example");
        Path out = Path.of(options.getOrDefault("--out", "benchmark-results")); Files.createDirectories(out);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("timestamp_utc", Instant.now().toString()); report.put("java", System.getProperty("java.runtime.version"));
        report.put("os", System.getProperty("os.name") + " " + System.getProperty("os.arch"));
        report.put("available_processors", Runtime.getRuntime().availableProcessors());
        report.put("max_heap_bytes", Runtime.getRuntime().maxMemory()); report.put("method", "One cold-process import, followed by warm queries; repeat in fresh JVMs for distributions. Heap pool peaks are not RSS.");
        for (var pool : ManagementFactory.getMemoryPoolMXBeans()) pool.resetPeakUsage();
        var genome = new GenomeDataService(); long start = System.nanoTime();
        if (options.containsKey("--annotation")) genome.loadFromFile(Path.of(options.get("--annotation")));
        else {
            var example = ExampleDataset.byId(options.get("--example"));
            if (example == null) throw new IllegalArgumentException("Unknown example"); genome.loadExample(example);
        }
        report.put("import_ms", millis(start));
        report.put("genes", genome.getGeneCount()); report.put("features", genome.getRawFeatureCount()); report.put("sequences", genome.getChromosomes().size());
        List<Map<String, String>> sources = new ArrayList<>();
        for (var source : genome.getSourceInputs()) {
            var sha = MessageDigest.getInstance("SHA-256");
            try (var stream = new DigestInputStream(source.open(), sha)) { stream.transferTo(OutputStream.nullOutputStream()); }
            sources.add(Map.of("source", source.location(), "sha256", HexFormat.of().formatHex(sha.digest())));
        }
        report.put("sources", sources);
        start = System.nanoTime();
        for (String sequence : genome.getChromosomes()) { genome.setActiveSequence(sequence); genome.getGenesOnActiveSequence(); }
        report.put("select_all_sequences_ms", millis(start));
        List<Gene> genes = genome.getGenes();
        if (genes.isEmpty()) throw new IllegalArgumentException("Annotation has no genes");
        int repetitions = 100;
        List<Double> searchMs = new ArrayList<>();
        for (int i = 0; i < repetitions; i++) {
            String query = genes.get(i % genes.size()).getId(); start = System.nanoTime(); genome.search(query); searchMs.add(millis(start));
        }
        searchMs.sort(Double::compareTo); report.put("search_repetitions", repetitions); report.put("search_median_ms", searchMs.get(50)); report.put("search_p95_ms", searchMs.get(94));
        List<Gene> queryGenes = genes.subList(0, Math.min(4, genes.size()));
        List<Double> hierarchyMs = new ArrayList<>(); long visited = 0;
        for (int iteration = 0; iteration < 100; iteration++) {
            start = System.nanoTime();
            for (Gene target : queryGenes) {
                Gene gene = genome.findGene(target.getId()); visited++;
                for (Transcript tx : gene.getTranscripts()) {
                    visited += 1 + tx.getUnassignedSegments().size();
                    for (Exon exon : tx.getExons()) visited += 1 + exon.getCdsSegments().size() + exon.getUtrSegments().size();
                }
            }
            hierarchyMs.add(millis(start));
        }
        hierarchyMs.sort(Double::compareTo);
        report.put("hierarchy_query_ids", queryGenes.stream().map(Gene::getId).toList());
        report.put("hierarchy_query_median_ms", hierarchyMs.get(50));
        report.put("hierarchy_query_p95_ms", hierarchyMs.get(94)); report.put("hierarchy_records_visited_over_100_queries", visited);
        start = System.nanoTime();
        var figure = new ComparisonFigure().build(genes.subList(0, Math.min(10, genes.size())), new ExpressionDataService(), ComparisonFigure.Settings.defaults());
        report.put("comparison_10_loci_layout_ms", millis(start)); start = System.nanoTime();
        try (var writer = Files.newBufferedWriter(out.resolve("benchmark-comparison.svg"))) { figure.writeSvg(writer); }
        report.put("comparison_svg_write_ms", millis(start));
        report.put("heap_pool_peaks_bytes", ManagementFactory.getMemoryPoolMXBeans().stream().filter(p -> p.getType() == java.lang.management.MemoryType.HEAP)
                .collect(java.util.stream.Collectors.toMap(p -> p.getName(), p -> p.getPeakUsage().getUsed())));
        try (var writer = Files.newBufferedWriter(out.resolve("parsed-structure.tsv"))) {
            writer.write("GeneID\tTranscriptID\tSequence\tStrand\tFeature\tStart\tEnd\n");
            for (Gene gene : genes) {
                write(writer, gene, "", "gene", gene);
                for (Transcript tx : gene.getTranscripts()) {
                    write(writer, gene, tx.getId(), "transcript", tx);
                    for (Exon exon : tx.getExons()) {
                        write(writer, gene, tx.getId(), "exon", exon);
                        for (var cds : exon.getCdsSegments()) write(writer, gene, tx.getId(), "CDS", cds);
                        for (var utr : exon.getUtrSegments()) write(writer, gene, tx.getId(), "UTR", utr);
                    }
                    for (var segment : tx.getUnassignedSegments()) write(writer, gene, tx.getId(),
                            segment.getType() == FeatureType.CDS ? "CDS" : "UTR", segment);
                }
            }
        }
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(out.resolve("performance.json").toFile(), report);
        System.out.println(new ObjectMapper().writeValueAsString(report));
    }
    private static double millis(long start) { return (System.nanoTime() - start) / 1_000_000.0; }
    private static void write(Writer out, Gene g, String tx, String type, GenomicFeature f) throws IOException {
        out.write(String.join("\t", g.getId(), tx, f.getChromosome(), "" + f.getStrand(), type, "" + f.getStart(), "" + f.getEnd()) + "\n");
    }
}
