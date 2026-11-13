# Desktop package verification — v0.2.0 (Windows x64)

> **Data-archiving notice (post-`8fd01ca`).** The complete reference data
> (TAIR10 genome FASTA, Araport11 Chr1-Chr5, the 94,883-gene horsemint annotation
> and its 4-leaf TPM matrix, and the 32,833-gene E-MTAB-7978 matrix) has been moved
> out of this repository's current tree into the independent
> `phytoscope-reference-data` repository - the local directory is ready, remote URLs
> are pending user provision. The application ships only six KB-scale example
> annotations, performs no download at runtime, and sequence extraction requires a
> user-imported genome FASTA. Every MB-scale figure, table and count below is a
> **historical v0.2.0 / `8fd01ca`-era measurement** recorded while the complete data
> was bundled; reproducing it now requires the external data and it must not be read
> as a current-tree benchmark. No numbers have been re-measured or invented. Git
> history pack (~209 MiB) and fresh-clone size are unchanged; only the current
> checkout and its build artifacts got smaller.

## Package identity

| Item | Value |
| --- | --- |
| Archive | `PhytoScope-Windows-AMD64.zip` (110,405,267 bytes), published as a GitHub Release attachment |
| SHA-256 | `763b1c42350872047f36eb6c944ea3ebfd10ffaaca7a1fb8a59645a2392b3cce` |
| Built from | dev branch, application version 0.2.0, JDK 21.0.6 `jpackage` app-image |
| Bundled data (historical v0.2.0 package) | Complete horsemint genome (94,883 genes, gz) + 4-leaf TPM panel + CMEN585 subset + spearmint chloroplast + TAIR10 sequence; slim jlink runtime (17 modules) - historical only; the current tree ships KB-scale examples. |
| Launcher options | `-Xmx3g`, main jar `app/phytoscope-0.2.0.jar` |
| Packaging note | The archive exceeds the 100 MB git file limit, so it is published as a GitHub Release attachment instead of a repository blob |

## Automated checks (2026-10-30, builder machine)

- ZIP CRC integrity: **PASS** (`unzip -t`, 210 entries, no errors)
- Layout: `PhytoScope/` contains `PhytoScope.exe`, `app/`, `runtime/` — **PASS**
- Bundled jar contains all five `data/species/mint` resources (annotation as gzip, streamed transparently) — **PASS** (historical v0.2.0 package; the current jar no longer contains MB-scale data)
- Packaged-runtime smoke test (`--benchmark --example rice`): the bundled
  `PhytoScope.exe` runtime launched headlessly and parsed the rice example — **PASS**
- SHA-256 self-check (`sha256sum -c SHA256SUMS.txt` inside `desktop_program/`): **OK**
- Full test suite at packaging time (historical v0.2.0): 149 tests, 0 failures (plus Monocle UI suite)

## Manual checklist (run on a clean Windows machine before each release)

Monocle-based tests cannot certify native dialogs, DPI scaling, graphics drivers
or security prompts. Record the following on the actual target machine:

1. Windows SmartScreen prompt on first launch — record what an end user sees.
2. Double-click `PhytoScope.exe`: window appears interactive within a few seconds.
3. Dataset selector: switch among the six KB-scale examples (Arabidopsis, Rice, Maize, Soybean, Mint, Mint plastid); the "Mint (complete)" entry was removed together with the archived data.
4. Select a gene: detail pane shows structure; sequence extraction requires File → Import genome FASTA…, and expression tracks require a user-imported matrix (no complete data is bundled).
5. File → Import annotation… with a local GFF3 and GTF; File → Import expression matrix… with a local TSV; check the import report dialog.
6. File → Import genome FASTA… and confirm the sequence view populates.
7. Add 2–3 genes to the comparison tab; export PNG and SVG.
8. File → Save project…, close, reopen the `.phytoscope` project; verify view, selection and expression restore.
9. Batch export round-trip on one chromosome.
10. Record OS edition/build, screen scaling and GPU driver of the test machine.
