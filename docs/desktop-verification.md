# Desktop package verification — v0.2.0 (Windows x64)

## Package identity

| Item | Value |
| --- | --- |
| Archive | `PhytoScope-Windows-AMD64.zip` (110,405,267 bytes), published as a GitHub Release attachment |
| SHA-256 | `763b1c42350872047f36eb6c944ea3ebfd10ffaaca7a1fb8a59645a2392b3cce` |
| Built from | dev branch, application version 0.2.0, JDK 21.0.6 `jpackage` app-image |
| Bundled data | Complete horsemint genome (94,883 genes, gz) + 4-leaf TPM panel + CMEN585 subset + spearmint chloroplast + TAIR10 sequence; slim jlink runtime (17 modules) |
| Launcher options | `-Xmx3g`, main jar `app/phytoscope-0.2.0.jar` |
| Packaging note | The archive exceeds the 100 MB git file limit, so it is published as a GitHub Release attachment instead of a repository blob |

## Automated checks (2026-10-30, builder machine)

- ZIP CRC integrity: **PASS** (`unzip -t`, 210 entries, no errors)
- Layout: `PhytoScope/` contains `PhytoScope.exe`, `app/`, `runtime/` — **PASS**
- Bundled jar contains all five `data/species/mint` resources (annotation as gzip, streamed transparently) — **PASS**
- Packaged-runtime smoke test (`--benchmark --example rice`): the bundled
  `PhytoScope.exe` runtime launched headlessly and parsed the rice example — **PASS**
- SHA-256 self-check (`sha256sum -c SHA256SUMS.txt` inside `desktop_program/`): **OK**
- Full test suite at packaging time: 149 tests, 0 failures (plus Monocle UI suite)

## Manual checklist (run on a clean Windows machine before each release)

Monocle-based tests cannot certify native dialogs, DPI scaling, graphics drivers
or security prompts. Record the following on the actual target machine:

1. Windows SmartScreen prompt on first launch — record what an end user sees.
2. Double-click `PhytoScope.exe`: window appears interactive within a few seconds.
3. Dataset selector: switch Arabidopsis → Mint (complete) → Mint (plastid); genes and chromosomes update.
4. Select a gene: detail pane shows structure, sequence and the 4-column TPM chart.
5. File → Import annotation… with a local GFF3 and GTF; File → Import expression matrix… with a local TSV; check the import report dialog.
6. File → Import genome FASTA… and confirm the sequence view populates.
7. Add 2–3 genes to the comparison tab; export PNG and SVG.
8. File → Save project…, close, reopen the `.phytoscope` project; verify view, selection and expression restore.
9. Batch export round-trip on one chromosome.
10. Record OS edition/build, screen scaling and GPU driver of the test machine.
