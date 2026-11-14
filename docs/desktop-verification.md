# Desktop package verification — v0.2.0 (Windows x64) + B05 0.3.0 增量补注

> **B05 (2026-11-14) 增量补注 — 上方 §"Package identity"–"Manual checklist" 是 v0.2.0
> 历史记录，数字与措辞原样保留、不外推为当前实测。**
>
> - **Windows x64 已测（0.3.0，本机构建 A 类）**：`sh mvnw -B -DskipTests package`
>   `BUILD SUCCESS` → `tools/package_desktop.py --jar target/phytoscope-0.3.0.jar
>   --out target/desktop-v0.3.0` `DESKTOP_EXIT=0`，packaged runtime smoke
>   `PASS: packaged runtime launched and parsed the rice example`；CLI smoke
>   `java -Xmx3g -jar target/phytoscope-0.3.0.jar --benchmark --example rice`
>   `SMOKE_EXIT=0`（stderr 空）。权威包 `../dist/PhytoScope-Windows-0.3.0.zip`
>   （46,185,966 B / SHA-256 `dcc2051f...`），`sha256sum -c desktop_program/SHA256SUMS.txt`
>   **3/3 OK**。
> - **macOS · Linux 待真机**：本机未做、无结果。两份空白清单见
>   `docs/fcs-desktop-native-verification-checklist.md`（需作者在有显示环境的真机执行）。
> - **GitHub Actions 三平台构建 artifact ≠ 原生人工验证**：JavaFX 测试使用 Monocle
>   headless，不能认证原生对话框、DPI 缩放、图形驱动、签名/公证或 SmartScreen 提示；
>   三平台 artifact 只能证明「可构建」，不能替代真机启动/导入/导出人工检查。
> - **Release 附件公开动作由主理人用远端 API 尝试，未成功前不得称已发布**；
>   当前 GitHub Release 只有 `v0.2.0`，`release_state = pending_release`。
> - 详见 `docs/fcs-v0.3.0-release-notes.md` 与 `docs/fcs-performance-benchmark.md`。
>
> 历史 `desktop_program/SHA256SUMS.txt` 曾把 `763b1c42...` 挂在 `AMD64.zip` 名下；
> 逐字节重测表明该哈希实属 `PhytoScope-Windows-0.2.0.zip`，而真实 `AMD64.zip` 为
> `927b0002...`。B05 只更正 checksum 文件的关联，历史 ZIP 字节一字未动。

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
