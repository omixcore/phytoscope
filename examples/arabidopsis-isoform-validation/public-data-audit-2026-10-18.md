# Public-data validation audit (18 October 2026)

## Decision

The public-data-only audit did **not** establish a new biological discovery or
independent protein support for CIPK15 `AT5G01810.3`. The public Arabidopsis
alternative-splicing catalog contains other CIPK15 events, but not the
candidate's coding deletion junction. One useful cross-resource result is that
the 66-bp exon deleted in the existing PIF6 candidate `AT3G62090.4` exactly
matches a PastDB exon-skipping event. This supports the existence of that exon
as an alternative-splicing event at catalog level; it does not validate the
complete transcript model, identify its original supporting studies, establish
translation, or make the isoform a new discovery. The sequence identity audit
still rejects `AT3G62090.4` as the published PIF6-beta protein.

No wet-lab work was performed or is planned. Non-detection in an exported
protein table or absence from an event catalog is not evidence that an isoform
or protein is absent.

## RNA evidence

The PastDB/VAST Arabidopsis TAIR10 download was checked against the 16 frozen
domain-deletion records spanning 13 genes in `candidates.tsv`. Twelve of the 13
candidate genes map to one or more events in the catalog. Gene-level event
presence was not counted as candidate-isoform evidence.

| Candidate | Frozen deleted interval | Processed catalog result | Interpretation |
| --- | --- | --- | --- |
| DI0003, CIPK15 `AT5G01810.3` | Chr5:311309–311443; junction 311308→311444 | Four CIPK15 events were cataloged, but none represents this interval/junction. The listed events are at Chr5:309739–309916 and Chr5:309805–310085. | No target-junction support in this catalog. This does not establish absence in RNA-seq or other data. |
| DI0006, PIF6 `AT3G62090.4` | Chr3:22989726–22989791 (66 bp) | PastDB event `AthEX0027630` is an exon-skipping event at exactly Chr3:22989726–22989791. Its event-metrics summary is mean 75.52, min 45.90, max 100.00. | Catalog-level support that this exon is alternatively spliced. The follow-up maps main-PSI condition groups to source labels and SRA runs, but the aggregated PSI does not prove the complete candidate exon chain. The candidate sequence remains distinct from the published PIF6-beta sequence. |
| DI0014, `AT1G48590.3` | Chr1:17963788–17963883 (96 bp) | The same interval appears as exon event `AthEX0002158`, but its catalog PSI summary is 100.00 in all reported samples. | The event record does not support skipping of the candidate exon in the summarized data. |

The PastDB resource was built from public Arabidopsis RNA-seq studies. Its
aggregate event and PSI tables are useful for screening, but the initial audit
did not join each event to original SRA accessions or source-study groups. The
catalog result therefore did not satisfy the frozen criterion for replicated
candidate-specific evidence. In the ABIOTIC PSI table, the PIF6 event had 39
nonempty condition-level values; 31 were below 100 (range 14.29–100). These
are aggregated condition summaries, not replicate-level counts or a new
statistical test.

### Condition-group and source-study mapping

The follow-up script `tools/summarize_pastdb_candidate_support.py` now joins
exact frozen deletion-block coordinates to PastDB event IDs, maps each ID to a
TAIR locus through PastDB's event-to-gene table, and then joins main PSI
condition groups to the download's source-study labels and SRA run accessions.
This fixed an initial false zero caused by treating the event table's display
gene symbols (for example, `PIF6`) as TAIR locus IDs. The complete tabular
output is generated in the public-data CI artifact under
`pastdb-candidate-study-support/`.

| Candidate(s) | Exact coordinate-matched event | Candidate deleted blocks / matched block is entire deletion | PSI condition groups (range) | GEO source groups | Mapped SRA runs | Interpretation |
| --- | --- | --- | ---: | ---: | ---: | --- |
| DI0006 PIF6 | `AthEX0027630`, Chr3:22989726–22989791, exon skipping | 1 / yes | 73 (20–100) | 33 | 215 | Stronger catalog traceability for the known event. PSI remains condition-aggregated and does not validate the full transcript chain or distinguish independent biological replication within each GEO study. |
| DI0014 AT1G48590 | `AthEX0002158`, Chr1:17963788–17963883, exon skipping | 1 / yes | 41 (100–100) | 24 | 123 | The summarized groups show full exon inclusion; this does not support skipping in those groups. |
| DI0010 AT5G56610 | `AthALTD0010952-3/3`, Chr5:22917255–22917377, alternative 5′ splice site | 1 / yes | 75 (50–100) | 31 | 224 | Coordinate concordance for an alternative splice-site event, not a demonstrated domain-deletion transcript. |
| DI0005 AT3G57800 | `AthALTD0014130-1/2`, Chr3:21409526–21409570, alternative 5′ splice site | 2 / no | 32 (0–100) | 17 | 100 | Only one block of the multi-block candidate deletion matches. |
| DI0001/DI0002 WAK3 | `AthEX0034008`, Chr1:7435281–7435439, exon skipping | 3 / no | 36 (100–100) | 21 | 104 | Only one block matches; the condition summaries show full inclusion. |
| DI0015/DI0016 WNK6 | `AthEX0034064`, Chr3:6455464–6455621, exon skipping | 3 / no | 90 (100–100) | 35 | 260 | Only one block matches; the condition summaries show full inclusion. |

The counts above are source-mapped catalog coverage, **not** counts of
independently replicated validations: PastDB PSI entries are condition-group
aggregates. Source labels and SRA accessions identify where those groups came
from, but this analysis did not reprocess reads, model replicate variation,
harmonize conditions across studies, or perform statistical tests. For the six
events, all matched deletion blocks are not equivalent to complete candidate
transcript structures. CIPK15 remains without an exact target-event match.

## Protein evidence

Two public PXD014032 processed candidate tables from the Zhang et al. 2019
study group were downloaded and searched across all columns:

| File | Rows | Search strings | Result | SHA-256 |
| --- | ---: | --- | --- | --- |
| `20190404_4Tissue_Candidates.tsv` | 51,326 | `AT5G01810`, `CIPK15`, `ESVEEETEEEMR` | 0 matching rows | `c6c576e741f8e41478185ecf86a4cb2a11515a087d425c547382d33f36747ed3` |
| `20190525_ABA_treated_Candidates.tsv` | 128,917 | `AT5G01810`, `CIPK15`, `ESVEEETEEEMR` | 0 matching rows | `8086f5ba3611dd8e7d7e11cfb2f26824ba3be2c08b1d864163affaa9b0f7e09a` |

These are processed quantification candidate tables, not raw spectra. Their
non-detection is not proof of protein absence. In addition, the CI audit searched
three publicly downloadable Zhang et al. 2019 supplementary spectral-library
text files (950,117,352 bytes total) for the exact junction peptide
`ESVEEETEEEMR`:

| Supplementary file | Bytes | Exact peptide text | SHA-256 |
| --- | ---: | --- | --- |
| `Supplementary_File_3a_Fusion_peakview_01.txt` | 328,683,517 | not found | `9b3a53078a2fa28be89110b2d0c580d07e2e618fcf33b298af08a9528c39707e` |
| `Supplementary_File_3b_Fusion_peakview_02.txt` | 333,080,420 | not found | `35e41b9794e9484bd39c49947c876c7a9d2f7b62e860695f21018decd4c1226a` |
| `Supplementary_File_6_5600_Peakview.txt` | 288,353,415 | not found | `08364396f3d331a0697e0d32c078c1924a7c26b1355a2555e0c9ca52be28ae92` |

The supplementary-file text search is a screen of those exported library files,
not a raw-spectrum reanalysis; not finding the peptide string does not establish
protein absence. PXD012708, PXD012710 and PXD014032 are one publication/source-
study group, and these three files are not independent studies. The DDA search
outputs and raw spectra were not searched here. The single eligible CIPK15
junction peptide previously attributed to the Mergner source remains one-source
evidence.

## Long-read Iso-Seq feasibility

NCBI BioProject `PRJNA1418670` metadata describes one PacBio Sequel II SRA
experiment/run with eight barcoded root samples (two replicates per condition
and time point). The submitted unaligned `roots_subreads.bam` is
886,585,032,640 bytes (about 887 GB). Its candidate-specific molecules have
not been downloaded, demultiplexed, or aligned in this audit. The accession's
existence is not transcript support.

## Reproduction and source records

- Candidate set and frozen coordinates: [`candidates.tsv`](candidates.tsv).
- PastDB download page: <https://pastdb.crg.eu/wiki/Downloads>.
- PastDB gene/event map: <https://vastdb.crg.eu/downloads/araTha10/EVENTID_to_GENEID-araTha10.tab.gz>.
- PastDB event coordinates: <https://vastdb.crg.eu/downloads/araTha10/EVENT_INFO-araTha10.tab.gz>.
- PastDB event PSI summaries: <https://vastdb.crg.eu/downloads/araTha10/EVENT_METRICS-araTha10.tab.gz>.
- PastDB main condition PSI: <https://vastdb.crg.eu/downloads/araTha10/PSI_main-araTha10.tab.gz>.
- PastDB sample/source information: <https://vastdb.crg.eu/downloads/araTha10/SAMPLE_INFO-araTha10.tab.gz>.
- PXD014032 processed tables: <https://ftp.pride.ebi.ac.uk/pride/data/archive/2019/09/PXD014032/>.
- Iso-Seq BioProject: <https://www.ncbi.nlm.nih.gov/bioproject/PRJNA1418670>.

The row-count and sequence-text searches were performed on the complete
downloaded PXD014032 TSV files. PastDB event records were joined by EventID to
the event-to-gene map and compared with the candidate deletion coordinates.
Exact event-coordinate agreement is a screen, not proof of full-length
transcript structure or biological function.

PastDB input SHA-256 values: event-to-gene map
`144b82791b4a2aee6b4bb397b779644eb0c7013ee0982e5e4d5256b1d5730c30`, event
coordinates `46d567040c82095157133336961fde0a06521a2702cf9654e3fba1de88c67652`,
and event metrics
`c2ae1d0f4df5a28af5d0b0995079e9985b8b23fdb50717fed1c10ea9cc31d39d`.

## Conclusion

The strongest result is a public catalog-level structural concordance for a
known PIF6 exon-skipping event, plus a negative target-junction screen for
CIPK15 in the assessed PastDB catalog. Neither result establishes a novel
biological finding. The independent discovery claim remains open and must be
reported as unestablished unless a candidate passes source-deduplicated,
candidate-specific public-data criteria.
