# Evidence package for the PhytoScope research workflow

## Proposed scientific contribution

PhytoScope links a selected plant locus to strand-aware promoter/custom-interval
extraction, explicit sample grouping, sequence screening, cross-species protein
similarity, and splice-aware domain-to-genome mapping in one recoverable desktop
project. Its contribution is workflow integration and provenance, not a new
orthology algorithm, domain predictor, or causal regulatory inference method.
The central evaluation question is whether the linked workflow reduces handoffs
and errors in a real plant analysis while preserving enough provenance to
reproduce it.

## Evidence status

| Evidence | Current evidence | Remaining work before a strong methods-paper claim |
| --- | --- | --- |
| Operational workflow | Five tabs in the desktop UI; browser locus synchronization; promoter extraction feeds sequence QC; sample groups, FASTA/domain inputs and results are saved in portable projects; analysis output exports as TSV. The browser accepts IGV-style `sequence:start-end` navigation and preserves the visible range in project view state. Automated regressions cover promoter extraction, TSV formatting, project save/replace/restore and coordinate navigation. The latest three-platform CI passed on 2026-10-18; full annotation-coordinate checks and standalone packaging passed on Ubuntu. | Add automated interaction coverage for each research tab and a native file-save interaction test. |
| Annotation correctness and scale | Independent source-to-model coordinate checks cover Arabidopsis, rice, maize and soybean. Eight checks contain 4,641,758 exact gene/transcript/exon/CDS/UTR tuples with no missing or extra tuples. Full-annotation import times are recorded in `benchmarks/results/2026-10-16/`. | Existing timings are shared-container smoke measurements. Repeat controlled runs on documented hardware and memory limits. |
| Existing-tool comparison | On four Arabidopsis loci, PhytoScope and gffutils 0.13 produced identical 78 source tuples in three fresh-process alternating-order trials. | The baseline is one species and a narrow extraction task. Extend to matched loci in the four plants and compare each research function only to a tool that provides the same output. Do not compare incomparable memory/index designs as a general speed ranking. |
| Research-module correctness | Unit cases cover strand-aware promoter boundaries, DNA/CDS flags, measured zero versus missing expression, local-alignment identity and coverage, a pairwise-cell limit, and domain coordinates across multi-exon minus-strand CDS. | Build a versioned gold set from curated plant homologs and domain records. Report precision/recall, coordinate agreement, runtimes, and cases that fail or are rejected. |
| Independent biological example | A reproducible Arabidopsis RBCS-family case is packaged from Expression Atlas E-MTAB-7978. It links four loci to TAIR10/Araport11 gene models and six tissue measurements; the source cells, hashes and project-restoration regression are retained. Independent mutant evidence supplies functional context for RBCS1A/RBCS3B; it does not validate the tissue-expression ranking or establish a new finding. | This is a known biological pattern, not a new discovery. The six selected atlas conditions each have one assay, and the current case does not include a matched multi-tool task study or independent raw-data reanalysis. Use it as an integration and reproducibility case only. A claim that PhytoScope enabled a finding unavailable through ordinary workflows still requires a separate, independently checked case. |

## Fair comparison protocol

For each of the five functions, define the input and output before choosing a
baseline. Use identical species, assembly, annotation release, sequences,
coordinate convention and thresholds. Freeze tool versions and parameters;
record setup and runtime separately. Include curated positive and negative
homolog pairs, exact domain-to-CDS mappings, promoter boundary cases, and
missing-versus-zero sample values. Report accuracy, runtime, memory where
measurable, and failure/rejection boundaries per task. A structure parser
comparison cannot stand in for homology, domain mapping or end-user task
performance.

## Scientific claim boundary

The software currently supports reproducible locus-centered inspection and
hypothesis generation. It does not establish orthology from reciprocal best
hits, predict domains, test differential expression, infer promoter function,
or demonstrate that researchers make a discovery unavailable through other
workflows. A methods-paper claim should be limited to the operational workflow
until curated module-level benchmarks and an independent, reproducible plant
case have been completed.
