package io.gitee.omixcore.phytoscope.cli;

import io.gitee.omixcore.phytoscope.comparison.ComparisonFigure;
import io.gitee.omixcore.phytoscope.project.ProjectStore;
import io.gitee.omixcore.phytoscope.service.*;
import java.nio.file.*;
import java.util.*;

/** Display-free SVG comparison and portable project creation from one repeatable command. */
public final class ComparisonRunner {
    private ComparisonRunner() { }
    public static void run(String[] args) throws Exception {
        Map<String, String> values = new LinkedHashMap<>();
        Set<String> switches = new HashSet<>();
        for (int i = 0; i < args.length; i++) {
            if (Set.of("--compare", "--compress-introns", "--no-expression").contains(args[i])) switches.add(args[i]);
            else {
                if (!Set.of("--gff3", "--annotation", "--expression", "--example", "--loci", "--out", "--alignment", "--scale", "--project").contains(args[i]) || i + 1 >= args.length)
                    throw new IllegalArgumentException("Unknown or incomplete comparison option: " + args[i]);
                String key = args[i]; if (values.put(key, args[++i]) != null) throw new IllegalArgumentException("Repeated option: " + key);
            }
        }
        GenomeDataService genome = new GenomeDataService(); ExpressionDataService expression = new ExpressionDataService();
        String annotation = values.getOrDefault("--gff3", values.get("--annotation"));
        if ((annotation == null) == (values.get("--example") == null)) throw new IllegalArgumentException("Choose exactly one annotation or example");
        if (annotation != null) genome.loadFromFile(Path.of(annotation));
        else {
            ExampleDataset example = ExampleDataset.byId(values.get("--example"));
            if (example == null) throw new IllegalArgumentException("Unknown example");
            genome.loadExample(example);
        }
        if (values.containsKey("--expression")) {
            expression.loadFromFile(Path.of(values.get("--expression"))); expression.reconcile(genome);
            System.out.println(expression.getImportReport().summary());
        }
        if (!values.containsKey("--loci")) throw new IllegalArgumentException("--compare requires explicit --loci");
        var ids = Arrays.stream(values.get("--loci").split(",")).map(String::trim).distinct().toList();
        var genes = ids.stream().map(id -> genome.getGenes().stream()
                .filter(g -> java.util.Objects.equals(g.getId(), id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown exact gene ID: " + id)))
                .toList();
        var settings = new ComparisonFigure.Settings(
                ComparisonFigure.Alignment.valueOf(values.getOrDefault("--alignment", "TRANSCRIPTION_START").toUpperCase(Locale.ROOT)),
                switches.contains("--compress-introns"),
                ComparisonFigure.Scale.valueOf(values.getOrDefault("--scale", "LOG1P").toUpperCase(Locale.ROOT)), !switches.contains("--no-expression"));
        var figure = new ComparisonFigure().build(genes, expression, settings);
        Path output = Path.of(values.getOrDefault("--out", "phytoscope-comparison")); Files.createDirectories(output);
        try (var writer = Files.newBufferedWriter(output.resolve("comparison.svg"))) { figure.writeSvg(writer); }
        Files.writeString(output.resolve("comparison.tsv"), figure.table());
        Files.writeString(output.resolve("expression-import.txt"), expression.getImportReport().summary());
        if (values.containsKey("--project")) {
            genome.setActiveSequence(genes.getFirst().getChromosome());
            new ProjectStore().save(Path.of(values.get("--project")), genome, expression,
                    new ProjectStore.ViewState(genome.getActiveSequence(), genes.getFirst().getId(), genes.getFirst().getStart(), genes.getFirst().getEnd(),
                            genes.stream().map(g -> g.getId()).toList(), settings.alignment().name(), settings.compressed(), settings.scale().name(), ""));
        }
        System.out.println("Wrote comparison.svg and comparison.tsv to " + output.toAbsolutePath());
    }
}
