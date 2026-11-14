# B01 · CI 金标准证据链修复 — 本地实跑留痕报告

> 批次：B01（`.github/`、`tools/`、`src/test/`）· 执行人：工程师寇豆码（Kou）· 日期：2026-11-14（ISO-8601 UTC 口径同设计 §7.10）
> 环境：Windows 11 x64 · JDK 21.0.6 · Python 3.13.14 · Git Bash · `mvnw -Pui-tests`（JavaFX Monocle headless）
> 权威规格：`docs/design-fcs-final-delivery.md` §5 B01、§0.1、§7；增量 PRD `docs/prd-fcs-final-delivery.md` R1/R7
> **状态口径**：本报告只记录本机真实输出。三平台远端 CI 报告回收属 B 类（主理人执行，裁定 4），**本报告不宣称三平台 CI 已通过**。

---

## 1. 改动摘要（只改 B01 区域）

| 文件 | 改动 |
|---|---|
| `.github/workflows/validate.yml` | Java 步加 `-Dphytoscope.referenceDataDir=target/reference-data`；Java 步后新增 `Assert gold-set report was actually produced`（全平台）；`check_isoform_case.py` 步传 `--reference target/reference-data/data/species/arabidopsis`；`Real case` 把 `--example arabidopsis` 换成 `--gff3 examples/arabidopsis-atlas/annotation.gff3`。`:41`、`:58`、`:87`、`:88` 未改动。 |
| `tools/check_isoform_case.py` | 新增 `argparse --reference`（默认 `target/reference-data/data/species/arabidopsis`，环境变量 `PHYTOSCOPE_REFERENCE_DATA_DIR` 可覆盖默认值）；`resolve()` 前加目录缺失 `SystemExit` guard；下游 `--reference` 传参改用同一变量。 |
| `tools/compare_promoters_bedtools.py` | `parse_args()` 之后、`shutil.which("bedtools")` 之前加 report 存在性 guard；`json.loads` 包 `try/except (FileNotFoundError, json.JSONDecodeError)` 统一 guard 文案。 |
| `tools/compare_homology_blastp.py` | 同上（`shutil.which("blastp")` 之前）。 |
| `src/test/resources/gff2/*.gff2`（4 个） | 最小 fixture：CDS phase、转录本边界、多 parent / 无 exon 的 CDS、1 个负例。 |
| `src/test/java/io/gitee/omixcore/phytoscope/Gff2ParserSupportTest.java` | 走**既有 importer** `Gff3Parser` 的兼容级测试；类注释明确「非独立解析器、非独立验证宣称」；零 `src/main/java/**` 改动、零新解析器。 |

---

## 2. 实跑命令与真实结果（逐条）

### 2.1 取回外迁数据

```sh
git clone --depth 1 https://gitee.com/omixcore/phytoscope-reference-data.git target/reference-data
```

结果：`CLONE_EXIT=0`；`data/species/arabidopsis/` 下取回 `TAIR10_genomic.fna.gz`、`Chr1..Chr5.gff3.gz`，`data/case-studies/arabidopsis-atlas/expression.tsv` 存在。

### 2.2 R1 主验收：gold-set 实跑且非 skip

```sh
sh mvnw -B -Pui-tests -Dphytoscope.referenceDataDir=target/reference-data test
```

结果：**`MAVEN_EXIT=1`**（关键输出逐条摘录于本节；当时写入的 `target/b01-*.log` 属 `target/` 运行时日志，B02 按执行顺序 `rm -rf target` 后已清除，未入 git、不复存在）。

```
[INFO] Running io.gitee.omixcore.phytoscope.ResearchModuleGoldSetTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 5.454 s -- in io.gitee.omixcore.phytoscope.ResearchModuleGoldSetTest
[ERROR] Tests run: 216, Failures: 0, Errors: 1, Skipped: 7
```

- **金标准链路已打通**：`ResearchModuleGoldSetTest` `tests=1, skipped=0, failures=0, errors=0`；`DomainCaseValidationTest`、`MintTranscriptomeUiTest`、`ReferenceDatasetIntegrityTest`、`ComparisonWorkflowUiTest` 这 4 个同源门控类也一并**不再 skip**。
- **skip 数下降**：基线 `Tests run: 211, Skipped: 12` → 本次 `Tests run: 216, Skipped: 7`（216 = 211 + 新增 5 个 GFF2 测试）。剩余 7 个 skip 是另外 3 个外部归档门控类（`AdditionalNonModelReferenceTest` 3 / `NonModelVignaReferenceTest` 3 / `FullAnnotationValidationTest` 1），与本次修复无关。
- **唯一 error（非本次改动引入，需上报裁定）**：`ComparisonWorkflowUiTest.realAtlasComparisonProjectRoundTripAndVectorRasterExports:88` → `AssertionFailedError: promoter action should populate the QC sequence input ==> expected: <true> but was: <false>`。该测试原先因 `Assumptions.assumeTrue(-Dphytoscope.referenceDataDir)` 被 skip，传属性后首次实跑即暴露既有缺陷：`MainView.loadGenomeFasta()` 经 `LoadingOperation.run()` 在**后台线程**加载 36 MB `TAIR10_genomic.fna.gz`，随后用 `Platform.runLater` 在 FX 线程提交 `sequenceService.replaceWith(next)`；而该测试整体以 `Platform.runLater(task)` 运行在 FX 线程上并 `task.get(240s)` 阻塞，导致提交回调在 task 结束前永远无法执行，`ResearchAnalysisPane.promoter()` 因 `sequences.isLoaded()==false` 直接返回。单独重跑 `-Dtest=ComparisonWorkflowUiTest` 稳定复现（`UI_SINGLE_EXIT=1`），与显示器/bedtools 无关。**本报告未修改该测试**（超出 B01 授权文件清单，且设计 §5 B01 明确 R1 零算法/零断言体改动），详见 §4「遗留问题」。

**隔离验证（剔除上述无关 UI 测试，证明金标准链路本身为绿）**：

```sh
sh mvnw -B -Pui-tests -Dtest=ResearchModuleGoldSetTest,DomainCaseValidationTest,Gff2ParserSupportTest -Dphytoscope.referenceDataDir=target/reference-data test
```

```
ISOLATED_EXIT=0
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 -- DomainCaseValidationTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0 -- Gff2ParserSupportTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 -- ResearchModuleGoldSetTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### 2.3 gold-set 报告存在性与 surefire 非 skip 证据

```sh
test -s target/research-validation/research-module-gold-set.json && echo "GOLDSET_OK"
python -c "import glob,xml.etree.ElementTree as E; p=glob.glob('target/surefire-reports/TEST-*ResearchModuleGoldSetTest.xml'); assert p; r=E.parse(p[0]).getroot(); print('tests/skipped/failures/errors=',r.get('tests'),r.get('skipped'),r.get('failures'),r.get('errors')); assert int(r.get('skipped') or 0)==0"
```

真实输出：

```
GOLDSET_OK
tests/skipped/failures/errors= 1 0 0 0
```

- 文件：`target/research-validation/research-module-gold-set.json`，**52,392 B**。
- SHA-256（隔离绿跑那一次）：`133e74860b25981e24e62e9cf3c1c86a67849b8dd9126451cddbe4366464a498`。
- 全量跑那一次的 SHA-256 为 `337c8d3d38c42d5db556e7e6ceabd0ece3bc389afc39e3a756006bb9a3d376f7`。**两者不同是因为该 JSON 顶层含 `date` 运行时间戳**；它是 `target/` 运行时产物，按设计 §7.6 **不作为仓库内可提交数据源**，也不做跨次稳定哈希主张。

### 2.4 两个 report 守卫（本机无 bedtools 也应先报 guard）

```sh
python tools/compare_promoters_bedtools.py --report target/nonexistent.json --genome target/reference-data/data/species/arabidopsis/TAIR10_genomic.fna.gz --out target/guard-bedtools.json; echo "exit=$?"
python tools/compare_homology_blastp.py --report target/nonexistent.json --out target/guard-blastp.json; echo "exit=$?"
```

真实输出（stderr）：

```
[guard] gold-set report 缺失: target\nonexistent.json；请先运行 `sh mvnw -B -Pui-tests -Dphytoscope.referenceDataDir=target/reference-data test` 产出该文件，禁止静默跳过
exit=1
[guard] gold-set report 缺失: target\nonexistent.json；请先运行 `sh mvnw -B -Pui-tests -Dphytoscope.referenceDataDir=target/reference-data test` 产出该文件，禁止静默跳过
exit=1
```

- 两条均 `exit=1` 且**先报 guard**，未出现 `bedtools is required` / 未出现裸 `FileNotFoundError` → 守卫顺序正确（本机 `bedtools` 不在 PATH、`blastp` 2.17.0+ 在 PATH，两种环境都被 guard 覆盖）。
- **本机无 bedtools**：`bedtools getfasta -s` 的 8/8 启动子比对实跑**留给 CI Linux runner**（`sudo apt-get install bedtools`，设计 §6 / §7.9）。本报告不声称本机完成该比对。

### 2.5 外迁路径修复（Linux CI 步的本机等价验证）

```sh
python tools/check_isoform_case.py --reference target/reference-data/data/species/arabidopsis
```

真实输出：`exit=1`，但在失败前**已成功解析外迁目录并执行完整 screen**（打印 `complete_cds_models=48002`、`candidate_rows=16`、`candidate_genes=13`，且 `input_sha256` 中 `TAIR10_genomic.fna.gz` / `Chr1..Chr5.gff3` 全部与冻结清单一致），证明原 `FileNotFoundError: src/main/resources/.../TAIR10_genomic.fna.gz` 已消除。

失败点：`ValueError: Screen result changed: candidates.json`，原因是**本机 Windows 换行差异**而非数据/路径问题——`examples/arabidopsis-isoform-validation/candidates.json` 与重跑产物**JSON 语义完全相等**（16 条记录逐条一致），仅 `write_text()` 在 Windows 写出 `\r\n`、冻结文件为 `\n`（首个字节差异在第 1 字节）；`candidates.tsv` 则逐字节相等。该 CI 步是 `if: runner.os == 'Linux'`，Linux 下 `write_text()` 产生 `\n`，按字节比较可通过。设计 §5 B01 亦注明「本机可跑至输入校验」。**如实记录本机 exit=1，不伪造成 PASS**。

### 2.6 Real case 位点修复（本地等价 smoke）

```sh
sh mvnw -B -DskipTests package
java -Xmx3g -jar target/phytoscope-0.3.0.jar --compare --gff3 examples/arabidopsis-atlas/annotation.gff3 --expression examples/arabidopsis-atlas/expression.tsv --loci AT1G67090,AT5G38410,AT5G38420,AT5G38430 --out target/case-study --project target/case-study.phytoscope
```

真实输出：`PACKAGE_EXIT=0`（`target/phytoscope-0.3.0.jar` 22,358,916 B）、`REAL_CASE_EXIT=0`；日志首行 `Read 32833 rows; 32833 profiles; 4 matched of 32833 input IDs against 4 annotation IDs; 0 missing cells; 0 invalid cells.` —— **无 `error: Unknown exact gene ID: AT1G67090`**。`examples/arabidopsis-atlas/annotation.gff3` 中 4 个位点全部存在（已逐条核验）。

### 2.7 R7 专用测试

```sh
sh mvnw -B -Pui-tests -Dtest=Gff2ParserSupportTest test
```

```
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

4 个 fixture（`cds-phase.gff2` / `transcript-boundaries.gff2` / `multi-parent-no-exon-cds.gff2` / `negative-orphan.gff2`）覆盖：CDS phase 0/1/2 保留、转录本边界（含负链双转录本）、`Parent=t-a,t-b` 多 parent 的 CDS 双挂、无重叠 exon 的 CDS 保留为 unassigned segment 且不臆造 exon、负例（未解析 parent 记为 orphan + 非数字坐标行被跳过且计入 diagnostics）。测试走 `Gff3Parser.parseResource(..., null)` 既有 importer；`##gff-version 2` 头与 `.gff2` 文件名均映射到既有 GTF dialect。**兼容级、非独立解析器、非独立验证宣称**。

---

## 3. CI 断言步的可执行性核对

`validate.yml` 新增步（全平台、Java 步之后）：

```yaml
- name: Assert gold-set report was actually produced
  shell: bash
  run: |
    test -s target/research-validation/research-module-gold-set.json
    python - <<'PY'
    import glob, xml.etree.ElementTree as ET
    p = glob.glob("target/surefire-reports/TEST-*ResearchModuleGoldSetTest.xml")
    assert p, "surefire report for ResearchModuleGoldSetTest missing"
    r = ET.parse(p[0]).getroot()
    print("tests/skipped/failures/errors =", r.get("tests"), r.get("skipped"), r.get("failures"), r.get("errors"))
    assert int(r.get("skipped") or 0) == 0, "gold-set test was SKIPPED, not executed"
    assert int(r.get("failures") or 0) == 0 and int(r.get("errors") or 0) == 0
    PY
```

本地同等断言（`§2.3`）通过：`tests/skipped/failures/errors = 1 0 0 0`。「禁止静默跳过」由 `test -s` + `skipped==0` 双重保证。

---

## 4. 遗留问题与边界声明（如实、未解决）

1. **`ComparisonWorkflowUiTest` 首次实跑失败（需主理人/架构师裁定）**：如 §2.2 所述，传 `-Dphytoscope.referenceDataDir` 后 5 个原 skip 的门控类全部实跑，其中该 JavaFX UI 工作流测试因「FX 线程被 `task.get()` 阻塞 → 异步 FASTA 提交回调无法执行」而确定性失败（本机单跑稳定复现）。这会影响远端 `verify` 全绿，属设计 §5 B01 处方之外的既有测试缺陷。B01 授权文件清单不含该测试，且硬约束要求「不新增/不改既有断言体、零算法改动」，故**本次未修改**；建议由后续批次裁定：或让该测试改用同步 FASTA 加载，或把该门控从 `phytoscope.referenceDataDir` 拆分，或接受其为已知失败并单独记录。**本报告不宣称三平台 CI 已通过、不宣称全部测试绿。**
2. **本机无 bedtools**：8/8 启动子独立比对实跑留给 CI Linux（`validate.yml` Linux 专属步）；本机只验证 guard 报错。
3. **`check_isoform_case.py` 本机 exit=1**：仅因 Windows CRLF 与冻结 LF 的字节差异（语义一致），Linux CI 预期通过；未伪造 PASS。
4. **gold-set / bedtools / BLASTP 三个 JSON 均为 `target/` 运行时产物**：按设计 §7.6，只能由主理人在推送后经远端 API 回收落到 `docs/ci-reports/v0.3.0/`（B 类）；本机不生成、不伪造、不提交其内容。
5. **非新测声明**：本批次未重跑任何计时、未产生新的性能数值；所有数字均为本机命令真实输出或设计/PRD 既有基线。

---

## 5. 结论

- R1 的**核心目标已达成**：`-Dphytoscope.referenceDataDir=target/reference-data` 使 `ResearchModuleGoldSetTest` 由 skip 变为实跑（`skipped=0`），产出 `target/research-validation/research-module-gold-set.json`（52,392 B），CI 断言步的本地等价校验通过；下游两个 `compare_*.py` 在 report 缺失时显式 `SystemExit` 且早于外部工具检查。
- R7 的 4 个 fixture + 5 个兼容级测试全绿，明确「非独立解析器 / 非独立验证宣称」，零 `src/main/java/**` 改动。
- **尚存 1 项需裁定的既有 UI 测试失败**（§4.1），以及 2 项本机环境限制（无 bedtools / Windows 换行），均已如实标注，未伪造成 PASS、未写成 B 类已完成。
