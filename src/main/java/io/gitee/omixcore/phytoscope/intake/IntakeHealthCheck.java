package io.gitee.omixcore.phytoscope.intake;

import io.gitee.omixcore.phytoscope.model.Exon;
import io.gitee.omixcore.phytoscope.model.Gene;
import io.gitee.omixcore.phytoscope.model.GenomicFeature;
import io.gitee.omixcore.phytoscope.model.Transcript;
import io.gitee.omixcore.phytoscope.parser.ParseDiagnostics;
import io.gitee.omixcore.phytoscope.service.ExpressionDataService;
import io.gitee.omixcore.phytoscope.service.GeneSequenceService;
import io.gitee.omixcore.phytoscope.service.GenomeDataService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Coordinator for the import-time health-check.
 *
 * <p>Runs the three checks defined by the incremental design &mdash; single-file
 * integrity (T01), cross-file reference consistency (T02) and sequence-name
 * coverage / alias detection (T03) &mdash; and assembles an {@link IntakeReport}.
 * It reuses the existing {@code GenomeDataService} / {@code GeneSequenceService}
 * / {@code ExpressionDataService} parsers; it never re-implements parsing or
 * coordinate conversion. Gzip handling is inherited from those parsers.</p>
 *
 * <p>Severity contract (&sect;1.4): {@link IntakeSeverity#ERROR} blocks the
 * import, {@link IntakeSeverity#WARNING} needs review, {@link IntakeSeverity#INFO}
 * is informational. The model is independent of {@code
 * AnnotationHealthService.Severity}.</p>
 */
public final class IntakeHealthCheck {

    /** Stable machine-readable issue codes. */
    public static final String CODE_FASTA_PARSE_ERROR = "FASTA_PARSE_ERROR";
    public static final String CODE_GFF3_PARSE_ERROR = "GFF3_PARSE_ERROR";
    public static final String CODE_TSV_PARSE_ERROR = "TSV_PARSE_ERROR";
    public static final String CODE_SEQID_NOT_IN_FASTA = "SEQID_NOT_IN_FASTA";
    public static final String CODE_FASTA_SEQ_NO_ANNOTATION = "FASTA_SEQ_NO_ANNOTATION";
    public static final String CODE_EXPRESSION_ID_UNMATCHED = "EXPRESSION_ID_UNMATCHED";
    public static final String CODE_EXPRESSION_STANDALONE = "EXPRESSION_STANDALONE";
    public static final String CODE_SEQUENCE_ALIAS_SUGGESTED = "SEQUENCE_ALIAS_SUGGESTED";
    public static final String CODE_ALIAS_AMBIGUOUS = "ALIAS_AMBIGUOUS";
    public static final String CODE_COORDINATE_OUT_OF_BOUNDS = "COORDINATE_OUT_OF_BOUNDS";
    public static final String CODE_SKIPPED_ROWS = "SKIPPED_ROWS";
    public static final String CODE_ORPHAN_FEATURE = "ORPHAN_FEATURE";
    public static final String CODE_NO_INPUT = "NO_INPUT_FILES";

    private static final int MAX_DETAIL_ISSUES = 25;

    private IntakeHealthCheck() {
    }

    /**
     * Runs the full check over the supplied files.
     *
     * <p>A genome FASTA and a GFF3 annotation are the minimum meaningful inputs;
     * the expression matrix is optional. Any of them may be {@code null}.</p>
     *
     * @param fasta      genome FASTA path, nullable
     * @param gff3       annotation path (GFF3/GTF), nullable
     * @param expression expression matrix path, nullable
     * @return the assembled {@link IntakeReport}
     */
    public static IntakeReport check(Path fasta, Path gff3, Path expression) {
        return check(fasta, gff3, expression, Map.of());
    }

    /**
     * Runs the full check, additionally applying user-confirmed sequence-name
     * aliases to the parsed FASTA <em>before</em> the cross-file reference checks
     * run. This lets the wizard re-evaluate missing-seqid errors against the now
     * aliased FASTA after the user supplies a manual mapping (manual-alias
     * recompute, fix D4). Each target id is validated against the FASTA sequence
     * set; an unknown target is kept as an {@code ERROR} rather than silently
     * accepted.
     *
     * @param fasta            genome FASTA path, nullable
     * @param gff3             annotation path (GFF3/GTF), nullable
     * @param expression       expression matrix path, nullable
     * @param confirmedAliases annotation-side name to FASTA id mappings, nullable/empty
     * @return the assembled {@link IntakeReport}
     */
    public static IntakeReport check(Path fasta, Path gff3, Path expression,
                                    Map<String, String> confirmedAliases) {
        List<IntakeFileResult> files = new ArrayList<>();
        List<IntakeIssue> issues = new ArrayList<>();

        GeneSequenceService sequence = null;
        GenomeDataService genome = null;
        ExpressionDataService expr = null;

        if (fasta != null) {
            try {
                sequence = parseFasta(fasta);
                files.add(result(fasta, IntakeFileKind.FASTA, true, null));
            } catch (IOException ex) {
                files.add(result(fasta, IntakeFileKind.FASTA, false, ex.getMessage()));
                issues.add(new IntakeIssue(IntakeSeverity.ERROR, CODE_FASTA_PARSE_ERROR,
                        "Genome FASTA could not be parsed: " + ex.getMessage(),
                        display(fasta), null,
                        "Fix the FASTA file (valid nucleotides, no truncation/encoding errors) and re-import."));
            }
        }

        if (gff3 != null) {
            try {
                genome = parseAnnotation(gff3);
                files.add(result(gff3, IntakeFileKind.GFF3, true, null));
            } catch (IOException ex) {
                files.add(result(gff3, IntakeFileKind.GFF3, false, ex.getMessage()));
                issues.add(new IntakeIssue(IntakeSeverity.ERROR, CODE_GFF3_PARSE_ERROR,
                        "Annotation could not be parsed: " + ex.getMessage(),
                        display(gff3), null,
                        "Fix the annotation file (valid nine-column rows, no truncation) and re-import."));
            }
        }

        if (expression != null) {
            try {
                expr = parseExpression(expression, genome);
                files.add(result(expression, IntakeFileKind.EXPRESSION, true, null));
            } catch (IOException ex) {
                files.add(result(expression, IntakeFileKind.EXPRESSION, false, ex.getMessage()));
                issues.add(new IntakeIssue(IntakeSeverity.ERROR, CODE_TSV_PARSE_ERROR,
                        "Expression matrix could not be parsed: " + ex.getMessage(),
                        display(expression), null,
                        "Fix the matrix (header with # unit=/# level=, matching column counts) and re-import."));
            }
        }

        if (sequence == null && genome == null) {
            issues.add(new IntakeIssue(IntakeSeverity.ERROR, CODE_NO_INPUT,
                    "At least a genome FASTA and an annotation must be supplied to run a meaningful check.",
                    null, null, "Choose a FASTA and a GFF3 file in the wizard."));
        }

        if (sequence != null && genome != null) {
            // Apply user-confirmed aliases BEFORE the reference checks so that a
            // manually supplied mapping can clear a SEQID_NOT_IN_FASTA error.
            if (confirmedAliases != null && !confirmedAliases.isEmpty()) {
                List<String> fastaNames = sequence.getSequenceNames();
                Map<String, String> valid = new LinkedHashMap<>();
                for (Map.Entry<String, String> entry : confirmedAliases.entrySet()) {
                    String from = entry.getKey();
                    String to = entry.getValue();
                    if (from == null || to == null) {
                        continue;
                    }
                    if (sequence.getResolvedLength(to) < 0) {
                        issues.add(new IntakeIssue(IntakeSeverity.ERROR, CODE_SEQID_NOT_IN_FASTA,
                                "Alias target '" + to + "' (mapped from '" + from
                                        + "') does not exist in the genome FASTA.",
                                display(fasta), null,
                                "Choose a FASTA sequence id that is present in the genome FASTA."));
                        continue;
                    }
                    valid.put(from, to);
                }
                sequence.addAliases(valid);
            }
            issues.addAll(checkSeqidSubset(sequence, genome, display(gff3)));
            issues.addAll(checkSequenceCoverage(sequence, genome, display(fasta)));
            for (AliasMapping candidate : detectAliasCandidates(sequence, genome)) {
                if ("auto-normalize".equals(candidate.confirmedBy())) {
                    issues.add(new IntakeIssue(IntakeSeverity.INFO, CODE_SEQUENCE_ALIAS_SUGGESTED,
                            "Sequence name '" + candidate.from() + "' will be mapped to FASTA id '"
                                    + candidate.to() + "' via normalisation.",
                            display(gff3), null,
                            "Confirm the alias in the wizard so the project records it."));
                } else {
                    issues.add(new IntakeIssue(IntakeSeverity.WARNING, CODE_ALIAS_AMBIGUOUS,
                            "Sequence name '" + candidate.from() + "' matches multiple FASTA ids ("
                                    + candidate.to() + "); an unambiguous single target must be confirmed "
                                    + "before the import can resolve it.",
                            display(gff3), null,
                            "Manually confirm a single target FASTA id for '" + candidate.from()
                                    + "' in the wizard."));
                }
            }
            issues.addAll(checkParseDiagnostics(genome, display(gff3)));
            issues.addAll(checkCoordinateBounds(sequence, genome, display(gff3)));
        }

        if (expr != null) {
            if (genome == null || genome.isEmpty()) {
                issues.add(new IntakeIssue(IntakeSeverity.INFO, CODE_EXPRESSION_STANDALONE,
                        "Expression matrix provided without an annotation; feature IDs are not matched "
                                + "against genes and it is treated as a standalone matrix.",
                        display(expression), null,
                        "Load an annotation to match expression IDs, or keep the matrix standalone."));
            } else {
                issues.addAll(checkExpressionMatch(genome, expr, display(expression)));
            }
        }

        String summary = buildSummary(files, issues);
        boolean pass = issues.stream().noneMatch(i -> i.severity() == IntakeSeverity.ERROR);
        return new IntakeReport(files, issues, summary, pass);
    }

    // ------------------------------------------------------------------
    // Cross-file checks
    // ------------------------------------------------------------------

    /**
     * Verifies that every annotation seqid is present in the FASTA sequence set
     * (exact case-insensitive match or normalisation match). A missing seqid is an
     * {@link IntakeSeverity#ERROR}; the first gene carrying it supplies the
     * source line number (AC-3). Child-feature-only seqids that never appear on a
     * gene are reported with a null line (precision boundary, &sect;1.5).
     */
    public static List<IntakeIssue> checkSeqidSubset(GeneSequenceService fasta, GenomeDataService genome, String gffFile) {
        List<IntakeIssue> issues = new ArrayList<>();
        Set<String> reported = new HashSet<>();
        for (Gene gene : genome.getGenes()) {
            String chr = gene.getChromosome();
            if (!isResolved(fasta, chr)) {
                issues.add(new IntakeIssue(IntakeSeverity.ERROR, CODE_SEQID_NOT_IN_FASTA,
                        "Annotation references sequence '" + chr + "' (gene " + gene.getId()
                                + ") which is absent from the provided FASTA.",
                        gffFile, String.valueOf(gene.getLineNumber()),
                        "Add the sequence to the FASTA, or declare an alias mapping from '" + chr
                                + "' to a FASTA sequence id."));
                reported.add(chr);
            }
        }
        for (String chr : genome.getChromosomes()) {
            if (!isResolved(fasta, chr) && !reported.contains(chr)) {
                issues.add(new IntakeIssue(IntakeSeverity.ERROR, CODE_SEQID_NOT_IN_FASTA,
                        "Annotation references sequence '" + chr + "' which is absent from the provided FASTA.",
                        gffFile, null,
                        "Add the sequence to the FASTA, or declare an alias mapping from '" + chr
                                + "' to a FASTA sequence id."));
            }
        }
        return issues;
    }

    /**
     * Flags FASTA sequences that carry no annotation genes (coverage gap). This is
     * informational: such sequences are retained without error (&sect;1.4).
     */
    public static List<IntakeIssue> checkSequenceCoverage(GeneSequenceService fasta, GenomeDataService genome, String fastaFile) {
        List<IntakeIssue> issues = new ArrayList<>();
        if (genome.isEmpty()) {
            return issues;
        }
        Set<String> annoNames = new HashSet<>(genome.getChromosomes());
        Set<String> annoNorm = new HashSet<>();
        for (String name : annoNames) {
            annoNorm.add(GeneSequenceService.normalizeSequenceName(name));
        }
        for (String name : fasta.getSequenceNames()) {
            if (!annoNames.contains(name)
                    && !annoNorm.contains(GeneSequenceService.normalizeSequenceName(name))) {
                issues.add(new IntakeIssue(IntakeSeverity.INFO, CODE_FASTA_SEQ_NO_ANNOTATION,
                        "FASTA sequence '" + name + "' has no genes in the annotation (retained without error).",
                        fastaFile, null,
                        "Optional: ensure the annotation covers all intended sequences."));
            }
        }
        return issues;
    }

    /**
     * Checks that every annotation feature's 1-based closed interval lies within
     * the bounds of the FASTA contig it resolves to (exact, alias or
     * normalisation match). A feature whose {@code end} exceeds the contig length,
     * or whose {@code start < 1} / {@code end < start}, is reported as an
     * {@link IntakeSeverity#ERROR} with its id and source line number (coordinate
     * out-of-bounds, fix D6b). Sequences that do not resolve against the FASTA
     * are skipped here; they are already surfaced by {@link #checkSeqidSubset}.
     */
    public static List<IntakeIssue> checkCoordinateBounds(GeneSequenceService fasta, GenomeDataService genome, String gffFile) {
        List<IntakeIssue> issues = new ArrayList<>();
        for (Gene gene : genome.getGenes()) {
            String chr = gene.getChromosome();
            int length = fasta.getResolvedLength(chr);
            if (length < 0) {
                continue; // unresolved sequence is reported by checkSeqidSubset
            }
            // The gene span.
            checkInterval(issues, chr, length, gene.getStart(), gene.getEnd(), gene.getId(),
                    gene.getLineNumber(), gffFile);
            // Child features: exons, CDS (incl. unassigned CDS), and UTR segments.
            for (Transcript transcript : gene.getTranscripts()) {
                for (Exon exon : transcript.getExons()) {
                    checkInterval(issues, chr, length, exon.getStart(), exon.getEnd(), exon.getId(),
                            exon.getLineNumber(), gffFile);
                }
                for (GenomicFeature cds : transcript.getAllCdsSegments()) {
                    checkInterval(issues, chr, length, cds.getStart(), cds.getEnd(), cds.getId(),
                            cds.getLineNumber(), gffFile);
                }
                for (Exon exon : transcript.getExons()) {
                    for (GenomicFeature utr : exon.getUtrSegments()) {
                        checkInterval(issues, chr, length, utr.getStart(), utr.getEnd(), utr.getId(),
                                utr.getLineNumber(), gffFile);
                    }
                }
                for (GenomicFeature utr : transcript.getUnassignedSegments()) {
                    checkInterval(issues, chr, length, utr.getStart(), utr.getEnd(), utr.getId(),
                            utr.getLineNumber(), gffFile);
                }
            }
        }
        return issues;
    }

    private static void checkInterval(List<IntakeIssue> issues, String chr, int length, int start, int end,
                                     String id, int line, String gffFile) {
        boolean invalid = start < 1 || end < start;
        boolean exceeds = length > 0 && end > length;
        if (!invalid && !exceeds) {
            return;
        }
        String detail = invalid
                ? "has an invalid interval [" + start + "," + end
                    + "] (start must be >= 1 and end must be >= start)"
                : "ends at " + end + " which exceeds the FASTA contig length " + length;
        issues.add(new IntakeIssue(IntakeSeverity.ERROR, CODE_COORDINATE_OUT_OF_BOUNDS,
                "Feature '" + (id == null ? "?" : id) + "' on sequence '" + chr + "' " + detail + ".",
                gffFile, line > 0 ? String.valueOf(line) : null,
                "Fix the coordinates so the feature lies within the FASTA sequence."));
    }

    /**
     * Checks expression feature-id matching against the annotation. Unmatched IDs
     * (when an annotation exists) are a {@link IntakeSeverity#WARNING}; a matrix
     * with no annotation is informational (handled by the caller).
     */
    public static List<IntakeIssue> checkExpressionMatch(GenomeDataService genome, ExpressionDataService expr, String exprFile) {
        List<IntakeIssue> issues = new ArrayList<>();
        if (genome.isEmpty()) {
            return issues;
        }
        ExpressionDataService.ImportReport report = expr.getImportReport();
        if (report != null && report.unmatchedIds() != null && !report.unmatchedIds().isEmpty()) {
            List<String> ids = report.unmatchedIds();
            List<String> preview = ids.size() > 10 ? ids.subList(0, 10) : ids;
            issues.add(new IntakeIssue(IntakeSeverity.WARNING, CODE_EXPRESSION_ID_UNMATCHED,
                    ids.size() + " expression feature_id(s) did not match any annotation ID: "
                            + String.join(", ", preview) + (ids.size() > preview.size() ? ", …" : ""),
                    exprFile, null,
                    "Verify the expression IDs match the annotation (case/version), or accept them as standalone."));
        }
        return issues;
    }

    /**
     * Surfaces parser diagnostics (skipped rows, orphan features) as
     * {@link IntakeSeverity#WARNING} summary and capped {@link IntakeSeverity#INFO}
     * detail. These are non-fatal data-quality signals, not import blockers.
     */
    public static List<IntakeIssue> checkParseDiagnostics(GenomeDataService genome, String gffFile) {
        List<IntakeIssue> issues = new ArrayList<>();
        ParseDiagnostics diagnostics = genome.getDiagnostics() == null
                ? ParseDiagnostics.empty() : genome.getDiagnostics();
        if (diagnostics.skippedRowTotal() > 0) {
            String message = diagnostics.skippedRowTotal()
                    + " malformed annotation row(s) were skipped during parsing. A fully clean file has none.";
            issues.add(new IntakeIssue(IntakeSeverity.WARNING, CODE_SKIPPED_ROWS, message, gffFile, null,
                    "Review the skipped rows; structural problems may hide real features."));
        }
        int limit = Math.min(diagnostics.orphans().size(), MAX_DETAIL_ISSUES);
        for (int i = 0; i < limit; i++) {
            ParseDiagnostics.OrphanFeature orphan = diagnostics.orphans().get(i);
            issues.add(new IntakeIssue(IntakeSeverity.WARNING, CODE_ORPHAN_FEATURE,
                    "Feature '" + orphan.featureId() + "' references missing "
                            + (orphan.target() == ParseDiagnostics.OrphanTarget.GENE ? "gene" : "transcript")
                            + " parent '" + orphan.missingParentId() + "'.",
                    gffFile, String.valueOf(orphan.lineNumber()),
                    "Fix the Parent attribute, or remove the orphan feature."));
        }
        if (diagnostics.orphans().size() > MAX_DETAIL_ISSUES) {
            issues.add(new IntakeIssue(IntakeSeverity.WARNING, CODE_ORPHAN_FEATURE,
                    (diagnostics.orphans().size() - MAX_DETAIL_ISSUES)
                            + " further orphan feature(s) were truncated from this report.",
                    gffFile, null, "Open the annotation in a genome browser to inspect the remainder."));
        }
        return issues;
    }

    /**
     * Detects sequence-name alias candidates between the annotation and the FASTA.
     *
     * <p>A chromosome absent from the FASTA under an exact match but resolved by
     * {@link GeneSequenceService#normalizeSequenceName(String)} (e.g. {@code 1} vs
     * {@code Chr1}) yields an {@code auto-normalize} candidate. Such pairs are
     * flagged as informational alias suggestions by the caller. Genuinely missing
     * seqids (no normalisation match) are not turned into candidates &mdash; they
     * are {@link IntakeSeverity#ERROR} findings from {@link #checkSeqidSubset} and
     * may instead be resolved by a user-declared manual mapping in the wizard.</p>
     */
    public static List<AliasMapping> detectAliasCandidates(GeneSequenceService fasta, GenomeDataService genome) {
        List<AliasMapping> candidates = new ArrayList<>();
        Set<String> fastaNames = new HashSet<>(fasta.getSequenceNames());
        // Normalised key -> ALL FASTA ids that normalise to it. The first-wins
        // putIfAbsent behaviour is intentionally avoided: every colliding id must
        // be visible so ambiguous cases can be surfaced instead of silently
        // resolving to the wrong FASTA record (alias-resolution fix D1).
        Map<String, List<String>> normToFastaIds = new HashMap<>();
        for (String name : fastaNames) {
            normToFastaIds.computeIfAbsent(
                    GeneSequenceService.normalizeSequenceName(name), k -> new ArrayList<>()).add(name);
        }
        for (String chr : genome.getChromosomes()) {
            if (fastaNames.contains(chr) || fastaNames.stream().anyMatch(n -> n.equalsIgnoreCase(chr))) {
                continue; // exact match: no alias needed
            }
            String norm = GeneSequenceService.normalizeSequenceName(chr);
            List<String> matches = normToFastaIds.getOrDefault(norm, List.of());
            if (matches.size() == 1) {
                // Exactly one FASTA id matches on normalisation: a safe,
                // auto-confirmable alias candidate.
                candidates.add(new AliasMapping(chr, matches.get(0), "auto-normalize"));
            } else if (matches.size() > 1) {
                // Ambiguous: more than one FASTA id normalises to the chromosome.
                // Do NOT auto-map. Surface a WARNING and an unconfirmed "user"
                // candidate so the user must explicitly choose the correct target.
                candidates.add(new AliasMapping(chr, String.join(", ", matches), "user"));
            }
        }
        return candidates;
    }

    // ------------------------------------------------------------------
    // Parsing helpers (shared by the wizard load path)
    // ------------------------------------------------------------------

    /** Parses a genome FASTA into a fresh service; gzip is handled internally. */
    public static GeneSequenceService parseFasta(Path fasta) throws IOException {
        GeneSequenceService service = new GeneSequenceService();
        service.loadFromFile(fasta);
        return service;
    }

    /** Parses an annotation (GFF3/GTF) into a fresh service; gzip is handled internally. */
    public static GenomeDataService parseAnnotation(Path gff3) throws IOException {
        GenomeDataService service = new GenomeDataService();
        service.loadFromFile(gff3);
        return service;
    }

    /** Parses an expression matrix and reconciles it against an annotation (if any). */
    public static ExpressionDataService parseExpression(Path expression, GenomeDataService genome) throws IOException {
        ExpressionDataService service = new ExpressionDataService();
        service.loadFromFile(expression);
        if (genome != null) {
            service.reconcile(genome);
        }
        return service;
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    /**
     * True when the annotation chromosome resolves against the FASTA, either by an
     * exact (case-insensitive) FASTA id or by a confirmed/parse-time alias. The
     * alias map is consulted so that a user-confirmed manual mapping (D4) clears a
     * previously missing-seqid error rather than being ignored.
     */
    private static boolean isResolved(GeneSequenceService fasta, String chr) {
        return fasta.resolveSequenceName(chr) != null;
    }

    private static IntakeFileResult result(Path path, IntakeFileKind kind, boolean ok, String error) {
        return new IntakeFileResult(display(path), kind, sha256(path), sizeOrZero(path), ok, error);
    }

    private static String buildSummary(List<IntakeFileResult> files, List<IntakeIssue> issues) {
        long errors = issues.stream().filter(i -> i.severity() == IntakeSeverity.ERROR).count();
        long warnings = issues.stream().filter(i -> i.severity() == IntakeSeverity.WARNING).count();
        long infos = issues.stream().filter(i -> i.severity() == IntakeSeverity.INFO).count();
        String verdict = errors > 0
                ? "Import is blocked until the errors above are resolved."
                : "No blocking errors; the import may proceed (review any warnings/info).";
        return "Checked " + files.size() + " file(s): " + errors + " error(s), " + warnings
                + " warning(s), " + infos + " info(s). " + verdict;
    }

    private static String display(Path path) {
        return path == null ? null : path.toString();
    }

    private static long sizeOrZero(Path path) {
        try {
            return Files.size(path);
        } catch (IOException ex) {
            return 0L;
        }
    }

    private static String sha256Hex(Path path) {
        return sha256(path);
    }

    /**
     * Computes the lowercase 64-char hex SHA-256 of a file's raw bytes.
     *
     * <p>Reused by the import wizard and {@code MainView} to verify, before a load,
     * that the selected files are byte-for-byte identical to those that produced
     * the health-check report (stale-report fix D2), and to re-verify a project's
     * recorded FASTA before auto-rebinding it on reopen (D5). Returns an empty
     * string when the file cannot be read.</p>
     *
     * @param path file to hash, nullable
     * @return lowercase SHA-256 hex, or {@code ""} on failure
     */
    public static String sha256(Path path) {
        if (path == null) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (var in = Files.newInputStream(path)) {
                byte[] buffer = new byte[65536];
                int n;
                while ((n = in.read(buffer)) >= 0) {
                    digest.update(buffer, 0, n);
                }
            }
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (Exception ex) {
            return "";
        }
    }
}
