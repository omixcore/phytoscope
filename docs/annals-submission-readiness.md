# Annals manuscript and submission package status

## Guideline snapshot checked 2026-10-19

The current *Annals of the New York Academy of Sciences* guidance lists Original Articles as 2,500–7,000 words (excluding abstract, online Methods, references, and figure legends), an unreferenced abstract up to 200 words, 4–8 figures and/or tables, and up to 175 references. The submission package calls for title, author and affiliation details, keywords, graphical abstract, Introduction, Materials and Methods, Results, Discussion, Conclusion, author statements, data availability, references, and figure legends. Figures, editable tables, and supplementary information are uploaded separately. See the [current author guidelines](https://nyaspubs.onlinelibrary.wiley.com/hub/journal/17496632/author-guidelines.html).

## Drafted package

The repository now contains an English Original Article draft, Word manuscript, five reproducible vector figures, Supplementary Information, Word supplement, cover letter, figure-generation script, and an author submission checklist under `manuscript/annals/`. The manuscript body (Introduction, Results, Discussion, and Conclusion) is approximately 2,646 words; it includes 20 references, one abstract, and five figures. The text separates the evidence by test scope and preserves all stated limitations.

## Evidence and claim boundary

| Claim area | Evidence recorded | Supported wording | Limit |
|---|---|---|---|
| Portability | Four reference plant systems and three additional public plant assemblies; same tested import/search workflow | Portable across the specified releases and recognized formats | Not universal compatibility; relies on compatible user files |
| Annotation fidelity | 4,641,758 exact recognized structural tuples across eight inventories, with zero missing or extra | Exact agreement for tested features and fields | Does not independently validate biological annotation truth or unsupported feature types |
| Selected sequences | Five selected coding loci per each of three additional plants; extra rice-bean CDS/protein record check | Selected exported sequences reproduce independent source reconstruction | Not genome-wide sequence validation |
| Expression import | Three PRJNA765494 IC426787 samples re-quantified with Salmon 2.8.0 against GCF_029865625.1 RefSeq; 34,914/34,914 matrix IDs matched | Real public data can be imported and identifiers reconciled to exact release | IC426787 reads vs VRB3 reference; not genotype-matched expression biology or differential expression |
| Alternative workflow | `gffutils==0.13` comparison for exact annotation hierarchy tasks | Scripted parser/hierarchy outputs agree on frozen cases | No broad speed ranking; no matched browser/manual end-to-end comparison |
| Research modules | Small independent promoter, similarity, expression arithmetic, and domain-coordinate checks | Exact results for specified test panels | Smoke tests, not general accuracy, orthology, or usability estimates |
| Researcher value | JavaFX visible-action and project round-trip regressions | Tested UI actions and recoverable project state work as specified | No participant study; no time-saving, ease-of-use, or user-value claim |

## Author actions before submission

- Add final author names/order, affiliations, corresponding-author information, and ORCID IDs.
- Verify contributions, funding, acknowledgments, competing interests, and any requested ethics statements.
- Have all authors review and approve the scientific claims, figures, references, and declarations.
- Replace the repository branch URL with an immutable commit and preferably an archival DOI; verify repository and source-data licenses.
- Confirm journal fit and recheck the live submission requirements immediately before upload.
- Upload the manuscript, each figure, editable tables if used, and Supplementary Information as separate files in the journal portal.

The manuscript package is drafted for author review. It is not yet ready for submission until author metadata, declarations, archive DOI, and final scientific review are completed.
