package io.gitee.omixcore.phytoscope.ui;

import io.gitee.omixcore.phytoscope.model.Gene;
import io.gitee.omixcore.phytoscope.model.SequenceRegion;
import io.gitee.omixcore.phytoscope.project.ProjectStore;
import io.gitee.omixcore.phytoscope.research.*;
import io.gitee.omixcore.phytoscope.service.*;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Compact workbench exposing the five analysis steps alongside the genome browser. */
public final class ResearchAnalysisPane extends BorderPane {
    private final GenomeDataService genome;
    private final ExpressionDataService expression;
    private final GeneSequenceService sequences;
    private final TextField geneId = new TextField();
    private final TextField upstream = new TextField("2000"), downstream = new TextField("200");
    private final TextField custom = new TextField();
    private final TextField group = new TextField("group");
    private final TextArea qcSequence = new TextArea(), proteinFasta = new TextArea(), domains = new TextArea();
    private final TextArea result = new TextArea();
    private final ComboBox<String> sample = new ComboBox<>();
    private final SampleGroupingService grouping = new SampleGroupingService();
    private final TextField species = new TextField("species A"), identity = new TextField("30");

    public ResearchAnalysisPane(GenomeDataService genome, ExpressionDataService expression, GeneSequenceService sequences) {
        this.genome = genome; this.expression = expression; this.sequences = sequences;
        setPadding(new javafx.geometry.Insets(12));
        geneId.setId("research-gene-id"); custom.setId("research-custom-interval");
        custom.setPromptText("sequence  start  end  label  score  strand");
        sample.setId("research-sample"); group.setId("research-group");
        qcSequence.setId("research-qc-sequence"); proteinFasta.setId("research-protein-fasta");
        domains.setId("research-domain-input"); result.setId("research-result");
        TabPane tabs = new TabPane();
        tabs.getTabs().addAll(tab("Intervals", intervalPane()), tab("Sample groups", groupsPane()),
                tab("Sequence QC", qcPane()), tab("Homology", homologyPane()), tab("Domains", domainPane()));
        result.setEditable(false); result.setWrapText(true); result.setPrefRowCount(8);
        Button export = new Button("Export results…"); export.setOnAction(e -> export());
        setCenter(tabs); setBottom(new VBox(6, new Label("Analysis output"), result, export));
        refreshSamples();
    }
    private static Tab tab(String title, javafx.scene.Node content) { Tab t = new Tab(title, content); t.setClosable(false); return t; }
    private VBox box(javafx.scene.Node... nodes) { VBox b = new VBox(8, nodes); b.setPadding(new javafx.geometry.Insets(12)); return b; }
    private HBox row(javafx.scene.Node... nodes) { return new HBox(8, nodes); }
    private javafx.scene.Node intervalPane() {
        Button promote = new Button("Extract promoter"); promote.setOnAction(e -> promoter());
        Button customButton = new Button("Extract custom interval"); customButton.setOnAction(e -> customInterval());
        geneId.setPromptText("Gene identifier");
        return box(new Label("Use the loaded genome FASTA. Coordinates are 1-based, closed."), row(geneId, new Label("Upstream"), upstream, new Label("Downstream"), downstream, promote), custom, customButton);
    }
    private javafx.scene.Node groupsPane() {
        sample.setMaxWidth(Double.MAX_VALUE); sample.setPromptText("Expression sample");
        Button assign = new Button("Assign group"); assign.setOnAction(e -> {
            if (sample.getValue() == null || group.getText().isBlank()) return;
            grouping.assign(sample.getValue(), group.getText()); write("Sample groups", grouping.assignments().toString());
        });
        Button summarize = new Button("Summarize selected gene"); summarize.setOnAction(e -> {
            Gene g = resolveGene(); if (g != null) write("Grouped expression (mean; missing values excluded)", grouping.summarize(expression, g.getId(), SampleGroupingService.Statistic.MEAN).toString());
        });
        return box(new Label("Assign expression samples to biological groups; assignments are saved with the project."), row(sample, group, assign), summarize);
    }
    private javafx.scene.Node qcPane() {
        qcSequence.setPromptText("Paste DNA sequence in FASTA or raw format"); qcSequence.setPrefRowCount(5);
        Button check = new Button("Check DNA / promoter"); check.setOnAction(e -> assess(false));
        Button checkCds = new Button("Check CDS"); checkCds.setOnAction(e -> assess(true));
        return box(new Label("Paste DNA or CDS, or extract a promoter first. QC reports length, GC, ambiguous bases and coding flags."), qcSequence, row(check, checkCds));
    }
    private void assess(boolean coding) {
        try {
            String s = qcSequence.getText().replaceAll("(?m)^>.*$", "").replaceAll("\\s", "");
            var r = new SequenceQualityService().assess(geneId.getText().isBlank()?"sequence":geneId.getText(), s, coding);
            write(coding ? "CDS QC (flags are checks, not diagnoses)" : "Sequence QC (flags are checks, not diagnoses)", "length="+r.length()+"; GC="+r.gcPercent()+"%; ambiguous="+r.ambiguousBases()+"; length_multiple_of_3="+r.lengthMultipleOfThree()+"; flags="+r.flags());
        } catch(Exception ex) { write("Sequence QC failed", ex.getMessage()); }
    }
    private javafx.scene.Node homologyPane() {
        proteinFasta.setPromptText("Paste FASTA proteins from both species; header format: >species|protein_id"); proteinFasta.setPrefRowCount(8);
        Button run = new Button("Compare proteins"); run.setOnAction(e -> homology());
        return box(new Label("Pairwise protein similarity; provide both species in one FASTA. Results are candidate similarities, not orthology proof."), row(new Label("Query species"), species, new Label("Minimum identity %"), identity), proteinFasta, run);
    }
    private javafx.scene.Node domainPane() {
        domains.setPromptText("One domain per line: protein_id<TAB>domain name<TAB>start_aa<TAB>end_aa"); domains.setPrefRowCount(5);
        Button map = new Button("Map domains to gene models"); map.setOnAction(e -> mapDomains());
        return box(new Label("Map amino-acid coordinates through annotated CDS exons onto genomic blocks."), domains, map);
    }
    private void refreshSamples() { sample.setItems(FXCollections.observableArrayList(expression.getTissues())); }
    public void refresh() { refreshSamples(); }
    public void selectGene(Gene gene) { if (gene != null) geneId.setText(gene.getId()); }
    public String selectedGeneId() { return geneId.getText(); }
    private Gene resolveGene() {
        Gene g = genome.findGene(geneId.getText().trim());
        if (g == null) write("Input needed", "Enter a valid gene identifier from the loaded annotation.");
        return g;
    }
    private void promoter() {
        try {
            Gene g=resolveGene(); if(g==null)return;
            int u=Integer.parseInt(upstream.getText().trim()), d=Integer.parseInt(downstream.getText().trim());
            int length=Optional.ofNullable(genome.getMetadata().findRegion(g.getChromosome())).map(SequenceRegion::length).orElse(Integer.MAX_VALUE);
            var interval=new IntervalAnalysisService().promoterInterval(g,u,d,length);
            var seq=new IntervalAnalysisService().extract(interval,sequences);
            qcSequence.setText(">"+interval.label()+"\n"+seq.sequence());
            write("Promoter sequence", interval.sequence()+":"+interval.start()+"-"+interval.end()+" ("+interval.strand()+")\n"+seq.sequence());
        } catch(Exception ex) { write("Promoter extraction failed", ex.getMessage()); }
    }
    private void customInterval() {
        try {
            String[] c=custom.getText().trim().split("\\s+");
            if(c.length<3)throw new IllegalArgumentException("Enter sequence, start, end and optional name/score/strand");
            int start=Integer.parseInt(c[1]),end=Integer.parseInt(c[2]); char strand=c.length>5?c[5].charAt(0):'.';
            String seq=sequences.extractInterval(c[0],start,end,strand);
            qcSequence.setText(">"+(c.length>3?c[3]:c[0]+":"+start+"-"+end)+"\n"+seq);
            write("Custom interval sequence", c[0]+":"+start+"-"+end+"\n"+seq);
        } catch(Exception ex) { write("Interval extraction failed", ex.getMessage()); }
    }
    private void homology() {
        try {
            List<ProteinHomologyService.Protein> ps=new ArrayList<>(); String id=null; StringBuilder seq=new StringBuilder();
            for(String line:proteinFasta.getText().split("\\R")) { if(line.startsWith(">")) { if(id!=null)addProtein(ps,id,seq); id=line.substring(1).trim(); seq.setLength(0); } else seq.append(line.trim()); }
            if(id!=null)addProtein(ps,id,seq);
            double min=Double.parseDouble(identity.getText().trim());
            var hits=new ProteinHomologyService().compare(ps.stream().filter(p->p.species().equalsIgnoreCase(species.getText().trim())).toList(), ps.stream().filter(p->!p.species().equalsIgnoreCase(species.getText().trim())).toList(),min);
            write("Protein similarity hits", hits.toString());
        } catch(Exception ex) { write("Homology comparison failed", ex.getMessage()); }
    }
    private static void addProtein(List<ProteinHomologyService.Protein> ps,String header,StringBuilder sequence) {
        String[] p=header.split("\\|",2); if(p.length!=2)throw new IllegalArgumentException("FASTA headers must be >species|protein_id");
        ps.add(new ProteinHomologyService.Protein(p[0],p[1],sequence.toString()));
    }
    private void mapDomains() {
        try { Gene gene=resolveGene(); if(gene==null)return; List<String> out=new ArrayList<>();
            for(String line:domains.getText().split("\\R")) { if(line.isBlank()||line.startsWith("#"))continue; String[] c=line.split("\\t");
                var d=new DomainLinkService.Domain(c[0],c[1],Integer.parseInt(c[2]),Integer.parseInt(c[3]),"user supplied");
                out.add(new DomainLinkService().link(d,gene).toString()); }
            write("Domain-to-genome links", String.join("\n",out));
        } catch(Exception ex) { write("Domain mapping failed", ex.getMessage()); }
    }
    private void write(String title,String text) { result.setText(title+"\n"+Objects.toString(text,"")); }
    private void export() {
        FileChooser chooser=new FileChooser(); chooser.setTitle("Export analysis results"); chooser.setInitialFileName("phytoscope-analysis.tsv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Tab-separated results", "*.tsv"));
        var file=chooser.showSaveDialog(getScene()==null?null:getScene().getWindow()); if(file==null)return;
        try {
            Files.writeString(file.toPath(), formatTsv(result.getText()));
        } catch(IOException ex) { write("Export failed",ex.getMessage()); }
    }
    public static String formatTsv(String text) {
        String[] lines=Objects.toString(text,"").split("\\R",2);
        String title=lines.length==0?"":lines[0]; String body=lines.length<2?"":lines[1];
        String safeBody=body.replace("\\", "\\\\").replace("\t", "\\t").replace("\r", "\\r").replace("\n", "\\n");
        return "Analysis\tResult\n"+title.replace("\t"," ")+"\t"+safeBody+"\n";
    }
    public ProjectStore.ResearchState state() { return new ProjectStore.ResearchState(parse(upstream.getText(),2000),parse(downstream.getText(),200),"BED",grouping.assignments(),species.getText(),parseDouble(identity.getText(),30),"","",1,1,result.getText(),custom.getText(),qcSequence.getText(),proteinFasta.getText(),domains.getText()); }
    public void restore(ProjectStore.ResearchState state) { if(state==null)return; upstream.setText(Integer.toString(state.upstream())); downstream.setText(Integer.toString(state.downstream())); grouping.clear(); grouping.assignAll(state.sampleGroups()); species.setText(Objects.toString(state.homologySpecies(),"")); identity.setText(Double.toString(state.homologyIdentity())); result.setText(Objects.toString(state.lastResult(),"")); custom.setText(Objects.toString(state.customInterval(),"")); qcSequence.setText(Objects.toString(state.qcSequence(),"")); proteinFasta.setText(Objects.toString(state.proteinFasta(),"")); domains.setText(Objects.toString(state.domainInput(),"")); refreshSamples(); }
    private static int parse(String s,int d){try{return Integer.parseInt(s);}catch(Exception e){return d;}}
    private static double parseDouble(String s,double d){try{return Double.parseDouble(s);}catch(Exception e){return d;}}
}
