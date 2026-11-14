# B01 / B01b · CI 金标准证据链修复 — 本地实跑留痕报告

> 批次：B01（`.github/`、`tools/`、`src/test/`）+ B01b（阻塞修复 / 跨平台换行消除）· 执行人：工程师寇豆码（Kou）· 日期：2026-11-14（ISO-8601 UTC 口径同设计 §7.10）
> 环境：Windows 11 x64 · JDK 21.0.6 · Python 3.13.14 · Git Bash · `mvnw -Pui-tests`（JavaFX Monocle headless）
> 权威规格：`docs/design-fcs-final-delivery.md` §5 B01、§0.1、§7；增量 PRD `docs/prd-fcs-final-delivery.md` R1/R7；第三轮下发 B01b 裁定
> **状态口径**：§2–§5 只记录本机真实输出；§6.1–§6.5 同理。**§6.6 例外**——它记录的是
> 远端 run `43609281554` 的实测结果，作为「ubuntu 修复是否生效」的独立证据。
> 本地与远端两条证据须分别标注来源，不得相互替代；「三平台矩阵通过」只能绑定
> `docs/ci-reports/v0.3.0/README.md` §7 的 run/job id 引用。
> **版本说明**：§2.2、§4.1、§5 已按 B01b 结果更新；`ComparisonWorkflowUiTest` 由「未解决遗留」改为「已修复（B01b）」；§2.8 记录新旧全量测试结果对比与 B01b-2 语义比较改动。

---

## 1. 改动摘要（只改 B01 / B01b 区域）

| 文件 | 批次 | 改动 |
|---|---|---|
| `.github/workflows/validate.yml` | B01 | Java 步加 `-Dphytoscope.referenceDataDir=target/reference-data`；Java 步后新增 `Assert gold-set report was actually produced`（全平台）；`check_isoform_case.py` 步传 `--reference target/reference-data/data/species/arabidopsis`；`Real case` 把 `--example arabidopsis` 换成 `--gff3 examples/arabidopsis-atlas/annotation.gff3`。`:41`、`:58`、`:87`、`:88` 未改动。 |
| `tools/check_isoform_case.py` | B01 | 新增 `argparse --reference`（默认 `target/reference-data/data/species/arabidopsis`，环境变量 `PHYTOSCOPE_REFERENCE_DATA_DIR` 可覆盖默认值）；`resolve()` 前加目录缺失 `SystemExit` guard；下游 `--reference` 传参改用同一变量。 |
| `tools/check_isoform_case.py` | B01b-2 | 新增 `canonical_text(path)`；冻结 `candidates.json` **由字节比较改为 JSON 语义比较**（`json.loads`，忽略 CRLF/LF 与键顺序）；`candidates.tsv` 逐字节比较与 `input_sha256` 输入校验保持不变；`annotation.gff3` / `discovery-manifest.json` 改为 LF 归一化后的严格文本比较。 |
| `tools/screen_domain_isoforms.py` | B01b-2 | 3 个被比较 / 派生哈希依赖的文本输出改为显式 `newline='\n'`（deterministic LF）；**未改动任何业务算法、候选筛选逻辑或 TSV 写入逻辑**。 |
| `tools/compare_promoters_bedtools.py` | B01 | `parse_args()` 之后、`shutil.which("bedtools")` 之前加 report 存在性 guard；`json.loads` 包 `try/except (FileNotFoundError, json.JSONDecodeError)` 统一 guard 文案。 |
| `tools/compare_homology_blastp.py` | B01 | 同上（`shutil.which("blastp")` 之前）。 |
| `src/test/resources/gff2/*.gff2`（4 个） | B01 | 最小 fixture：CDS phase、转录本边界、多 parent / 无 exon 的 CDS、1 个负例。 |
| `src/test/java/io/gitee/omixcore/phytoscope/Gff2ParserSupportTest.java` | B01 | 走**既有 importer** `Gff3Parser` 的兼容级测试；类注释明确「非独立解析器、非独立验证宣称」；零 `src/main/java/**` 改动、零新解析器。 |
| `src/test/java/io/gitee/omixcore/phytoscope/ComparisonWorkflowUiTest.java` | B01b | 单个长 FX `FutureTask` 拆为「setup / 断言」两个**短 FX 任务**；测试线程显式等待 `getPendingLoad()` 与 `getSequenceService().isLoaded()`；fixture 改用 `MainView.loadExpression(...)` 刷新 detail pane。**断言一条未删、未弱化、未改期望值**；未使用 `@Disabled` / `Assumptions` 绕过；未改生产异步加载路径为同步；`src/main/java/**` 零改动（复用已有 `getPendingLoad()` / `getSequenceService()` 只读 hook）。 |
| `docs/fcs-ci-goldset-fix-report.md` | B01/B01b | 本留痕报告。 |

---

## 2. 实跑命令与真实结果（逐条）

### 2.1 取回外迁数据

```sh
git clone --depth 1 https://gitee.com/omixcore/phytoscope-reference-data.git target/reference-data
```

结果：`CLONE_EXIT=0`；`data/species/arabidopsis/` 下取回 `TAIR10_genomic.fna.gz`、`Chr1..Chr5.gff3.gz`，`data/case-studies/arabidopsis-atlas/expression.tsv` 存在。B02 按执行顺序 `rm -rf target` 后，B01b 重新 clone 一次（`target/reference-data`），供全量测试与 isoform case 使用。

### 2.2 R1 主验收：gold-set 实跑且非 skip

```sh
sh mvnw -B -Pui-tests -Dphytoscope.referenceDataDir=target/reference-data test
```

**B01 当时结果（历史，保留作对比）**：`MAVEN_EXIT=1`（关键输出逐条摘录于本节；当时写入的 `target/b01-*.log` 属 `target/` 运行时日志，B02 按执行顺序 `rm -rf target` 后已清除，未入 git、不复存在）。

```
[INFO] Running io.gitee.omixcore.phytoscope.ResearchModuleGoldSetTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 5.454 s -- in io.gitee.omixcore.phytoscope.ResearchModuleGoldSetTest
[ERROR] Tests run: 216, Failures: 0, Errors: 1, Skipped: 7
```

- **金标准链路已打通**：`ResearchModuleGoldSetTest` `tests=1, skipped=0, failures=0, errors=0`；`DomainCaseValidationTest`、`MintTranscriptomeUiTest`、`ReferenceDatasetIntegrityTest`、`ComparisonWorkflowUiTest` 这 4 个同源门控类也一并**不再 skip**。
- **skip 数下降**：基线 `Tests run: 211, Skipped: 12` → 本次 `Tests run: 216, Skipped: 7`（216 = 211 + 新增 5 个 GFF2 测试）。剩余 7 个 skip 是另外 3 个外部归档门控类（`AdditionalNonModelReferenceTest` 3 / `NonModelVignaReferenceTest` 3 / `FullAnnotationValidationTest` 1），与本次修复无关。
- **B01 唯一 error（已由 B01b 修复，见 §2.8）**：`ComparisonWorkflowUiTest.realAtlasComparisonProjectRoundTripAndVectorRasterExports:88` → `AssertionFailedError: promoter action should populate the QC sequence input ==> expected: <true> but was: <false>`。主理人第三轮裁定否决「重新门控 / 拆独立系统属性使其 skip」「记为已知失败」「把生产加载路径改为同步」三种方案，要求必修并保持真实异步加载覆盖。

**B01b 修复后重跑结果（当前有效）**：

```
[INFO] Running io.gitee.omixcore.phytoscope.ResearchModuleGoldSetTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 6.161 s -- in io.gitee.omixcore.phytoscope.ResearchModuleGoldSetTest
[INFO] Running io.gitee.omixcore.phytoscope.ComparisonWorkflowUiTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[WARNING] Tests run: 216, Failures: 0, Errors: 0, Skipped: 7
[INFO] BUILD SUCCESS
[INFO] Total time:  50.658 s
```

- `FULL_TEST_EXIT=0`；`ResearchModuleGoldSetTest skipped=0`（surefire XML `tests/skipped/failures/errors = 1 0 0 0`）；`ComparisonWorkflowUiTest` 通过（surefire XML `1 0 0 0`）；全量 `Skipped=7`，**不高于基线 12**。
- 日志：`target/b01b-full-test2.log`（运行时日志，未入 git；关键输出已逐条摘录于本报告，报告不依赖其长期存在）。

**隔离验证（剔除 UI 测试，证明金标准链路本身为绿；B01 当时执行）**：

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

真实输出（B01b 最终全量绿跑后复验）：

```
GOLDSET_OK
tests/skipped/failures/errors= 1 0 0 0
GOLDSET_SKIPPED_ZERO_OK
```

- 文件：`target/research-validation/research-module-gold-set.json`，**52,392 B**。
- SHA-256（B01b 最终全量绿跑那一次）：`db05be65e460903e9089509b102ac840419bc50709f09bd8a9d690560741ee05`。
- B01 隔离绿跑 / 全量跑的 SHA-256 分别为 `133e74860b25981e24e62e9cf3c1c86a67849b8dd9126451cddbe4366464a498` / `337c8d3d38c42d5db556e7e6ceabd0ece3bc389afc39e3a756006bb9a3d376f7`。**跨次不同是因为该 JSON 顶层含 `date` 运行时间戳**；它是 `target/` 运行时产物，按设计 §7.6 **不作为仓库内可提交数据源**，也不做跨次稳定哈希主张。

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

### 2.5 外迁路径修复 + B01b-2 跨平台换行消除（Linux CI 步的本机等价验证）

```sh
python tools/check_isoform_case.py --reference target/reference-data/data/species/arabidopsis
```

**B01 当时结果**：`exit=1`，但在失败前**已成功解析外迁目录并执行完整 screen**（打印 `complete_cds_models=48002`、`candidate_rows=16`、`candidate_genes=13`，且 `input_sha256` 中 `TAIR10_genomic.fna.gz` / `Chr1..Chr5.gff3` 全部与冻结清单一致），证明原 `FileNotFoundError: src/main/resources/.../TAIR10_genomic.fna.gz` 已消除。失败点 `ValueError: Screen result changed: candidates.json`：原因是**本机 Windows 换行差异**而非数据/路径问题——`examples/arabidopsis-isoform-validation/candidates.json` 与重跑产物**JSON 语义完全相等**（16 条记录逐条一致），仅 `write_text()` 在 Windows 写出 `\r\n`、冻结文件为 `\n`；`candidates.tsv` 则逐字节相等。B01 如实记录本机 exit=1，未伪造 PASS。

**B01b-2 修复（本机 Windows 与 CI Linux 都应 exit 0）**：

- `tools/check_isoform_case.py`：**由字节比较改为语义等价比较** —— 对冻结 `candidates.json` 用 `json.loads(canonical_text(...))` 后比较，忽略 CRLF/LF 与键顺序差异；`canonical_text()` 把 `\r\n` 归一为 `\n`。**`candidates.tsv` 的逐字节比较与 `input_sha256` 输入校验保持不变**；`annotation.gff3` / `discovery-manifest.json` 仍做严格文本比较，但同样 LF 归一化，避免不同 OS 写出差异造成伪失败。
- `tools/screen_domain_isoforms.py`：`candidates.json` / `annotation.gff3` / `discovery-manifest.json` 三个**被下游比较或派生哈希依赖**的文本输出改用 `open(..., newline='\n')` 显式 LF 写出（`discovery-manifest.json` 的 `candidate_sha256` 依赖 `candidates.json` 字节）。**仅为消除 platforms 换行不确定性，未改变任何算法、候选筛选逻辑或 TSV 写入逻辑**。
- 原因：Windows `Path.write_text()` 可能写 CRLF，既让字节比较伪失败，也会改变派生 `candidate_sha256` / `input_sha256` 关联产物；JSON 语义比较对内容等价性不变，同时把生成端统一为 LF，使 Windows 与 Linux 的冻结产物字节一致。

**B01b 真实输出**：

```
ISOFORM_EXIT=0
PASS: fixed inputs, candidates, all peptide rows and failed outcomes reproduced
```

日志：`target/b01b-isoform.log`（运行时日志，未入 git；关键输出已摘录）。

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

### 2.8 B01b 阻塞修复：新旧结果对比与修法留痕

**新旧全量测试对比（真实输出）**：

| 项 | B01（修复前） | B01b（修复后） |
|---|---|---|
| 命令 | `sh mvnw -B -Pui-tests -Dphytoscope.referenceDataDir=target/reference-data test` | 同左 |
| 退出码 | `MAVEN_EXIT=1` | `FULL_TEST_EXIT=0` |
| Tests run | 216 | 216 |
| Failures | 0 | 0 |
| Errors | **1**（`ComparisonWorkflowUiTest`） | **0** |
| Skipped | 7（≤ 基线 12） | 7（≤ 基线 12） |
| `ResearchModuleGoldSetTest` | `1 / 0 / 0 / 0`（skipped=0） | `1 / 0 / 0 / 0`（skipped=0） |
| `ComparisonWorkflowUiTest` | 失败 | `1 / 0 / 0 / 0`（通过） |
| BUILD | FAILURE | **SUCCESS** |

**根因（只读核实，未改生产代码）**：`src/main/java/io/gitee/omixcore/phytoscope/ui/MainView.java:332-348` 的 `loadGenomeFasta(Path)` 经 `LoadingOperation.run(...)` 在后台线程解析 36 MB FASTA，其 commit 回调 `sequenceService.replaceWith(next)` 由 `src/main/java/io/gitee/omixcore/phytoscope/ui/component/LoadingOperation.java` 经 `Platform.runLater` 排入 FX 事件队列。原测试把 27–264 行整段放进一个 `FutureTask` 并在 FX 线程 `task.get(240s)` 阻塞，FX 事件队列被独占 → commit 回调永远排不上 → `sequenceService.isLoaded()==false` → promoter 断言失败。项目内已有正确范式：`BackgroundLoadingUiTest.fx(Callable<T>)` 短 FX 任务、`MintTranscriptomeUiTest` 的「后台 commit 在 FX 线程执行，测试不能在 FX 线程里阻塞等待」。

**修法（测试侧重构，保持真实异步加载覆盖）**：

1. 新增 `Fixture(MainView view, Scene scene, GenomeDataService genome, ExpressionDataService expression)` record 与短 FX helper `fx(Callable<T>)`（`Platform.runLater(task); task.get(240, TimeUnit.SECONDS)`）。
2. **Phase 1（短 FX 任务）**：只构造 `MainView` / `Scene` 并调用 `loadArchivedAtlasCase(...)`，触发真实 `MainView.loadGenomeFasta(...)` 后立即返回，**让 FX 线程空闲**，使 `LoadingOperation` 的 `Platform.runLater` commit 回调可执行。
3. **测试线程显式有界等待**：通过 `fx(() -> fixture.view().getPendingLoad())` 取回 `CompletableFuture<?>`，用 `fastaLoad.get(240, TimeUnit.SECONDS)` 等待真实后台 commit（同时建立 happens-before），再用 `awaitSequenceServiceLoaded(view, 60)` 轮询 `view.getSequenceService().isLoaded()`，超时抛明确 `AssertionError`。`requireReferenceData()` 条件 skip 仍在 FX `FutureTask` 外部，符合 `:21-23` 注释要求。
4. **Phase 2（短 FX 任务）**：执行原 27–264 行断言体，**断言一条未删、未弱化、未改期望值**。

**第二处隐性失败及修复**：两阶段重构后首次重跑仍失败于 `selected measured gene must show the expression bar chart in the details pane`（`Expression (unspecified)` ≠ 期望 `Expression (TPM)`）。根因是 fixture 直接调用 `expression.loadFromFile(...)` + `expression.reconcile(genome)`，绕过 `MainView.loadExpression(...)` / `refreshExpressionViews()`，`GeneDetailPane.valueUnit` 未刷新。改为调用 `view.loadExpression(...)`（public non-dialog UI 入口），detail pane 的 `valueUnit` / `samples` / `level` 被刷新，caption 恢复为 `Expression (TPM)`；所有断言保持不变，随后全量测试 `BUILD SUCCESS`。

**未采用被否决方案**：未把测试重新门控 / 拆独立系统属性使其 skip；未记为已知失败；未改生产加载路径为同步；未使用 `@Disabled` 或 `Assumptions` 绕过；**未在 `src/main/java/**` 新增 getter 或改动任何加载语义**（`MainView` 已有 `getPendingLoad()` / `getSequenceService()` 只读 hook）。

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

本地同等断言（§2.3）通过：`tests/skipped/failures/errors = 1 0 0 0`。「禁止静默跳过」由 `test -s` + `skipped==0` 双重保证。

---

## 4. 遗留问题与边界声明

1. **`ComparisonWorkflowUiTest` —— 已修复（B01b），不再是遗留问题**：如 §2.2 / §2.8 所述，B01 阶段该 JavaFX UI 工作流测试因「长 `FutureTask` 独占 FX 线程 → 异步 FASTA commit 回调无法执行」确定性失败。B01b 按主理人裁定改为两个短 FX 任务 + 测试线程有界等待，保持真实异步加载覆盖，全量测试 `BUILD SUCCESS`、`Failures=0 / Errors=0`、`ComparisonWorkflowUiTest` 通过、`ResearchModuleGoldSetTest skipped=0`、全量 `Skipped=7 ≤ 12`。生产加载路径、`src/main/java/**`、断言体均未改动。**本报告仍不宣称三平台 CI 已通过**（远端报告回收属 B 类）。
2. **本机无 bedtools**：8/8 启动子独立比对实跑留给 CI Linux（`validate.yml` Linux 专属步）；本机只验证 guard 报错。
3. **`check_isoform_case.py` 本机结果 —— 已修复（B01b-2）**：B01 时因 Windows CRLF 与冻结 LF 的字节差异 exit=1（语义一致，未伪造 PASS）；B01b-2 把 `candidates.json` **由字节比较改为 JSON 语义比较**，并把生成器 3 个文本输出固定为 LF，本机 Windows `ISOFORM_EXIT=0`，输出 `PASS: fixed inputs, candidates, all peptide rows and failed outcomes reproduced`；CI Linux 预期同样通过。`candidates.tsv` 逐字节比较与 `input_sha256` 输入校验保持不变。
4. **gold-set / bedtools / BLASTP 三个 JSON 均为 `target/` 运行时产物**：按设计 §7.6，只能由主理人在推送后经远端 API 回收落到 `docs/ci-reports/v0.3.0/`（B 类）；本机不生成、不伪造、不提交其内容。
5. **非新测声明**：本批次未重跑任何计时、未产生新的性能数值；所有数字均为本机命令真实输出或设计/PRD 既有基线。
6. **运行时日志**：`target/b01-*.log`（B02 `rm -rf target` 后已清除）、`target/b01b-full-test.log` / `b01b-full-test2.log` / `b01b-isoform.log` 均为 `target/` 运行时日志，未入 git、不作为长期证据；关键输出已逐条摘录于本报告。

---

## 5. 结论

- R1 的**核心目标已达成且全量测试为绿**：`-Dphytoscope.referenceDataDir=target/reference-data` 使 `ResearchModuleGoldSetTest` 由 skip 变为实跑（`skipped=0`），产出 `target/research-validation/research-module-gold-set.json`（52,392 B），CI 断言步的本地等价校验通过；下游两个 `compare_*.py` 在 report 缺失时显式 `SystemExit` 且早于外部工具检查。
- R7 的 4 个 fixture + 5 个兼容级测试全绿，明确「非独立解析器 / 非独立验证宣称」，零 `src/main/java/**` 改动。
- **B01b 阻塞修复已完成**：`sh mvnw -B -Pui-tests -Dphytoscope.referenceDataDir=target/reference-data test` → `Tests run: 216, Failures: 0, Errors: 0, Skipped: 7`，`BUILD SUCCESS`；`ResearchModuleGoldSetTest skipped=0`、`ComparisonWorkflowUiTest` 通过、全量 Skipped 不高于基线 12。未使用 `@Disabled` / `Assumptions` 绕过，未改生产加载语义。
- **B01b-2 跨平台换行不确定性已消除**：`check_isoform_case.py` 本机 Windows `exit=0`；`candidates.tsv` 字节比较与 `input_sha256` 校验保持；`screen_domain_isoforms.py` 仅固定 LF 输出、未改算法。
- **仍如实标注的边界**：本机无 bedtools（8/8 比对留给 CI Linux）；Zenodo DOI 仍属 B 类未取得。
  三平台远端 CI 与 Release 已在 **§6.6** 记录实测结果（run `43609281554` 三平台矩阵 success、
  Release 已发布并已重传资产），但**本报告不代替** `docs/ci-reports/v0.3.0/README.md` §7 的逐 job 记录。

---

## 6. ubuntu 无头 JavaFX 失败（2026-11-14 远端实测）

### 6.1 远端事实（GitHub Actions run `43605656726`，commit `49a5fac`）

| 平台 | 结论 |
|---|---|
| `validation (windows-latest)` | success |
| `validation (macos-latest)` | success |
| `validation (ubuntu-latest)` | **failure**，失败步为第 10 步 `Java and JavaFX regressions` |
| `public-data-validation` | failure（依赖外网 PMC / PastDB / ENA，非代码缺陷，不在本节范围） |

ubuntu 该步内**四个门控类全部通过且 `Skipped: 0`**：`ResearchModuleGoldSetTest 1/0/0/0`、`DomainCaseValidationTest 1/0/0/0`、`Gff2ParserSupportTest 5/0/0/0`、`ComparisonWorkflowUiTest 1/0/0/0`（12.41 s）。换言之 R1 的「禁止静默跳过」与 §2.8 的两阶段异步工作流修复在远端依旧成立，失败面只在无头 JavaFX 的**平台初始化**。

失败集中在 6 个 JavaFX UI 测试类、共 14 个方法，`Failures` 均为同一断言：

```
org.opentest4j.AssertionFailedError: expected: <true> but was: <false>
    at ...UiTest.startPlatform(...:55)
    → assertTrue(ready.await(20, TimeUnit.SECONDS))
```

| 类 | 方法数 | 方法名 | 报错位置 |
|---|---|---|---|
| `GenomeImportRetentionUiTest` | 3 | `annotationOnlyImportClearsPreviousFasta`、`reopenRestoresFastaWhenHashMatches`、`reopenSkipsFastaWhenMissingOrTampered` | `GenomeImportRetentionUiTest.java:55` |
| `ImportGenomeWizardUiTest` | 7 | `happyPathEnablesLoadAndDeliversServices`、`missingSeqidBlocksLoad`、`confirmedAliasesAreDeliveredToHandler`、`exportWritesJsonAndMarkdown`、`staleReportRefusesLoadAfterFileChange`、`manualAliasRecomputeUnblocksLoad`、`exportWritesTsvReport` | `ImportGenomeWizardUiTest.java:53` |
| `BackgroundLoadingUiTest` | 1 | `loadingDoesNotBlockFxAndCancellationNeverCommitsStagedData` | `BackgroundLoadingUiTest.java:21` |
| `AdaptiveChromosomeUiTest` | 1 | `datasetDropdownMenuAndGeneSelectionStayInSync` | `AdaptiveChromosomeUiTest.java:39` |
| `MintTranscriptomeUiTest` | 1 | `mintGenomeLeafExpressionPanelIsFullyBrowsable` | 30.00 s latch 超时 |
| `UnassignedSegmentsUiTest` | 1 | `preservesSourceSegmentsWithoutInventingExonsOrSpliceJunctions` | `UnassignedSegmentsUiTest.java:15` |

每个失败方法耗时精确等于 latch 上限（20.00 s；`MintTranscriptomeUiTest` 为 30.00 s），说明 `Platform.startup(ready::countDown)` 的回调在规定时间内**一次都没被执行**，而不是某条业务断言取值不符。

### 6.2 根因判断

`-Pui-tests` 在 surefire 里注入 `phytoscope.ui.tests=true`、`glass.platform=Monocle`、`monocle.platform=Headless`、`prism.order=sw`（`pom.xml` 的 `ui-tests` profile）。surefire 默认 `reuseForks=true`，**全部测试类共用同一个 JVM**，而 JavaFX 的 toolkit 每个 JVM 只能初始化一次：

1. 第一个调用 `Platform.startup` 的类拿到活的 FX Application Thread（`ComparisonWorkflowUiTest` 12.41 s 通过即为反证）；
2. 后续每个类只能落到 `catch (IllegalStateException) { Platform.runLater(ready::countDown); }` 分支，此时无头 Monocle 的 FX 线程已不再及时派发该任务，latch 走到超时；
3. ubuntu runner 更慢、图形栈不同，暴露得最彻底；macOS / Windows 因为时序不同而侥幸通过（`validation (windows-latest)`、`validation (macos-latest)` 均为 success）。

### 6.3 修复内容（两处，均为测试基础设施层）

1. **`pom.xml` 的 `ui-tests` profile**：在 surefire `<configuration>` 内、（与既有 `<systemPropertyVariables>` 并列）新增
   `<forkCount>1</forkCount>` 与 `<reuseForks>false</reuseForks>`，让**每个测试类在独立 JVM 中运行**，恢复「首次初始化即可用」这一前提。未改动任何其它 profile、未改依赖版本、未改 surefire 版本；`.github/workflows/validate.yml` 第 51 行的 `sh mvnw -B -Pui-tests -Dphytoscope.referenceDataDir=target/reference-data verify` **一字未改**（刻意保持的「单条 verify 处方」）。
2. **6 个 UI 测试类的平台启动等待上限 20 s → 90 s**（`MintTranscriptomeUiTest` 由 30 s → 90 s；`ComparisonWorkflowUiTest:56` 同属「平台启动等待」，按同一语义一并放宽，共 7 处）。这只是**等待上限**，不放宽任何正确性判定：断言、期望值、`entered.await(...)`/`workerDone.await(...)`/`task.get(...)` 等其它 latch 一律未动，目的是在慢 runner 上给 JavaFX 初始化留足时间。
3. **`reuseForks=false` 顺带暴露并修复的第二个隐性缺陷（同一根因族）**：JVM 隔离后 `ComparisonWorkflowUiTest` 首次运行出现 246.9 s 的 `TimeoutException`（`fx:28` ← 阶段二 `:86`）。`jcmd Thread.print` 显示该 JVM 内**已无 FX Application Thread**（只剩 `Monocle Timer` / `Prism Font Disposer`，测试线程停在 `FutureTask.get`）。原因是 `LoadingOperation` 会 `dialog.show()` 并用 `dialog.close()` 结束（`LoadingOperation.java:60/67/78`）：在全新 JVM 中 JavaFX 的 `implicitExit` 仍为默认 `true`，最后一个窗口关闭即**关停 toolkit**，阶段二的 `Platform.runLater` 任务永远排不上。修法与项目内既有的 `BackgroundLoadingUiTest:21`、`GenomeImportRetentionUiTest:57`、`ImportGenomeWizardUiTest:55` 完全一致：在 `ComparisonWorkflowUiTest` 启动完成后先执行 `fx(() -> { Platform.setImplicitExit(false); return null; })`。**未改任何断言**，也未触碰 `src/main/java/**`。

### 6.4 本地验证（真实输出，Windows + JDK 21，`JAVA_HOME=C:/Program Files/Java/jdk-21`）

```
rm -rf target/reference-data && git clone --depth 1 https://gitee.com/omixcore/phytoscope-reference-data.git target/reference-data
sh mvnw -B -Pui-tests -Dphytoscope.referenceDataDir=target/reference-data test
```

| 项 | 结果 |
|---|---|
| 退出码 | `EXIT=0` |
| 汇总行 | `[WARNING] Tests run: 216, Failures: 0, Errors: 0, Skipped: 7` |
| BUILD | `BUILD SUCCESS`，`Total time: 01:09 min`（墙钟 72 s） |
| surefire 报告数 | 40 个 `TEST-*.xml` |
| `ResearchModuleGoldSetTest` | `tests=1 failures=0 errors=0 skipped=0`（5.245 s） |
| `DomainCaseValidationTest` | `tests=1 failures=0 errors=0 skipped=0`（3.05 s） |
| `Gff2ParserSupportTest` | `tests=5 failures=0 errors=0 skipped=0` |
| `ComparisonWorkflowUiTest` | `tests=1 failures=0 errors=0 skipped=0`（11.528 s） |
| 7 个 UI 测试类 | 全部通过：`AdaptiveChromosomeUiTest 1`、`BackgroundLoadingUiTest 1`、`ComparisonWorkflowUiTest 1`、`GenomeImportRetentionUiTest 3`、`ImportGenomeWizardUiTest 7`、`MintTranscriptomeUiTest 1`、`UnassignedSegmentsUiTest 1`（合计 15，覆盖远端 14 个失败方法） |
| `Skipped=7` 来源 | `AdditionalNonModelReferenceTest 3`、`NonModelVignaReferenceTest 3`、`FullAnnotationValidationTest 1`（均为既有外部数据条件跳过，与本次修复无关） |

**耗时对比（同机、同命令、仅 `reuseForks` 不同，各跑一轮）**：

| 模式 | Total time | 墙钟 | JavaFX 启动告警次数（= 实际 JVM 数证据） |
|---|---|---|---|
| `reuseForks=true`（修复前口径，共用 JVM） | `31.940 s` | 33 s | 1 |
| `reuseForks=false`（修复后） | `01:09 min` | 72 s | 8 |

即每类独立 JVM 在本机增加约 **+37 s（≈2.2 倍）**，绝对增量不足 1 分钟，远低于 12 分钟告警线，无需精简建议；三平台各多出的这部分时间对 CI 预算可忽略。

**过程中如实记录的两次失败（未伪造 PASS）**：`reuseForks=false` 首轮全量为 `Tests run: 216, Failures: 0, Errors: 1, Skipped: 7`（`ComparisonWorkflowUiTest` 阶段二 246.9 s `TimeoutException`），按 §6.3 第 3 条修复后转为上述 `Errors: 0`；单类复现运行 `-Dtest=ComparisonWorkflowUiTest` 亦由 `EXIT=1 / Errors: 1` 变为 `EXIT=0`（11.19 s）。

### 6.5 边界声明

- 本节所述修复（`e293f7e`）未改 `src/main/java/**`、未改任何断言体与期望值、
  未改 `.github/workflows/validate.yml`、未改 `README.md` / `CHANGELOG.md` / `CITATION.cff`；
  未使用 `@Disabled`、`Assumptions` 或重新门控等回避手段。
  > 后续提交 `0638dbe`（见 §6.6）**确有**改 `.github/workflows/validate.yml`（只加 clone 重试）
  > 与 `tools/run_validation.py`，那属 §6.6 的独立问题，不影响本节「未改工作流」的陈述范围。
- §6.1–§6.4 的验证全部在**本机 Windows + JDK 21** 上执行；远端 CI 配置当时未改动。

### 6.6 远端确认（本节结论已由 run `43609281554` 证实）

- **`e293f7e` 的修复已在远端生效**：run `43609281554`（`run_attempt=1`，commit `0638dbe` = `v0.3.0`）
  的 `validation (ubuntu-latest)` **success**（job `132490554008`，`15:16:31Z → 15:32:41Z`），
  `Results: Tests run: 216, Failures: 0, Errors: 0, Skipped: 7` + `BUILD SUCCESS`；
  四个门控类 `Skipped: 0`（`ResearchModuleGoldSetTest 1/0/0/0` 6.799 s、
  `DomainCaseValidationTest 1/0/0/0` 3.708 s、`Gff2ParserSupportTest 5/0/0/0`、
  `ComparisonWorkflowUiTest 1/0/0/0` 16.02 s）。
- **中途还暴露并修掉了第二个问题（`0638dbe`）**：`tools/run_validation.py` 的 Arabidopsis 分支
  仍在 glob 已被外迁的 `src/main/resources/data/species/arabidopsis/Chr*.gff3*`，匹配为空导致
  `--source` 收到 0 个参数 → `validate_structure_inventory.py: error: argument --source:
  expected at least one argument`。四个物种统一改为校验 `--example <name>` 实际导入的随包示例注释，
  并加文件缺失守卫。此前一次尝试误指向 `phytoscope-reference-data` 的全量 Chr1–Chr5，
  引发 `expected_records: 811827 / parsed_records: 571` 不一致，已回退（如实记录）。
- **同期一次瞬时网络失败**：run `43608357998` 的 ubuntu 在 step 5 `git clone`
  报 `RPC failed; curl 56 GnuTLS recv error (-110)` / `fatal: early EOF`，同提交 windows/macos 通过；
  已在 `validate.yml` 为该 clone 加 3 次有界重试，**`verify` 命令本身未改**。
- **因此**：§6.4 的「本地通过」与远端结果现已**相互独立地同时成立**；
  但引用时仍须分别标注来源，不得用其一替代另一。
- 完整逐 job 记录见 `docs/ci-reports/v0.3.0/README.md` **§7**。

