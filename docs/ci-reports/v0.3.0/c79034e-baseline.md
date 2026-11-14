# 对照基线留档 · `c79034e` 三平台 CI 失败清单

> 批次：B03 · 执行人：工程师寇豆码（Kou）· 日期：2026-11-14（ISO-8601 UTC 口径同设计 §7.10）
> 来源：`docs/prd-fcs-final-delivery.md` §1.1（主理人从 GitHub Actions 最新 `c79034e` 运行拉取，实测）。
> 权威规格：`docs/design-fcs-final-delivery.md` §3.1、§5 B03 #7、§0.1。
> 本文件是**对照基线留档**，供修复后 HEAD 的逐平台 **failure → success** 变化对照；
> **不是当前状态**，不得沿用为「已通过」。

---

## 1. 基线总览

| 项 | 值 |
|---|---|
| `baseline_commit_sha` | `c79034e`（2026-11-14 15:14） |
| `workflow` | `Validate and package PhytoScope` |
| `matrix.os` | `ubuntu-latest` / `windows-latest` / `macos-latest` |
| `baseline_status` | **三个平台 `validation` job 全部 `failure`，不是 success** |
| Java 层 | `Tests run: 211, Failures: 0, Errors: 0, Skipped: 12` |
| 其中金标准 | `ResearchModuleGoldSetTest = Tests run: 1, Skipped: 1`（被 `Assumptions` 门控跳过） |
| 连带后果 | `target/research-validation/research-module-gold-set.json` 不产出 → bedtools / BLASTP 两处对比失去输入 |

> 旧文档（`research-module-validation.md` 等）「CI 三平台通过（bedtools 8/8、BLASTP 20/20）」
> 一律为**数据外迁前历史留痕**，禁止沿用为当前状态（设计 §7.3 / PRD §8 第 5 条）。

---

## 2. 逐平台失败清单（逐条抄录 PRD §1.1）

| 平台 / job | 失败步骤 | 报错 | 归类 | R1 修复要求（已于 B01/B01b 落地） |
|---|---|---|---|---|
| `ubuntu-latest` / validation | `Reproduce frozen isoform screen and independent validation`（`tools/check_isoform_case.py`，`validate.yml:48`） | `FileNotFoundError: src/main/resources/data/species/arabidopsis/TAIR10_genomic.fna.gz` | **代码类**（数据外迁后脚本仍硬编码主仓内置路径） | 脚本新增 `--reference`，CI 传 `--reference target/reference-data/data/species/arabidopsis`（B01）；B01b-2 另把 `candidates.json` 改 JSON 语义比较 |
| `windows-latest` / validation | `Real case, no display required`（`validate.yml:83-88`，`--loci AT1G67090,...`） | `error: Unknown exact gene ID: AT1G67090`，退出码 3 | **代码类**（atlas 案例入口已移除，CI 仍用仅存在于完整注释的基因 ID） | `Real case` 改 `--gff3 examples/arabidopsis-atlas/annotation.gff3`，保留 `--expression` 与 4 个 `--loci`（B01） |
| `macos-latest` / validation | 同上 `Real case, no display required` | 同上 `Unknown exact gene ID`，退出码 3 | **代码类** | 同 windows（B01） |
| 三平台 / validation（连带） | `Java and JavaFX regressions` → bedtools/BLASTP 对比 | `ResearchModuleGoldSetTest = Skipped: 1`，`research-module-gold-set.json` 不产出 | **代码类**（`-Dphytoscope.referenceDataDir` 未传） | `validate.yml:51` 显式传 `-Dphytoscope.referenceDataDir=target/reference-data`，新增 gold-set 断言步；两个 `compare_*.py` 加 report guard（B01） |
| `public-data-validation`（独立 job） | `Audit public Arabidopsis splicing and proteomics resources` | 外部 PMC / PastDB / ENA 网络数据源不可达 | **外部网络类（非代码逻辑）** | 单独归类，不计入 validation job 成败判定（Q13） |

---

## 3. 本机 A 类已闭环的修复证据（指针，不冒充远端）

| 修复 | 本地证据 |
|---|---|
| 金标准实跑、非 skip、Skipped 由 12 下降 | `docs/fcs-ci-goldset-fix-report.md` §2.2 / §2.3（本机 `Tests run: 216, Skipped: 0 for gold-set`） |
| 全量测试 `BUILD SUCCESS` | `docs/fcs-ci-goldset-fix-report.md` §2.8（B01b：`Tests run: 216, Failures: 0, Errors: 0, Skipped: 7`） |
| gold-set JSON 真实生成 | 同上 §2.3（`GOLDSET_OK`，52,392 B） |
| 两处 Python 消费 / guard | 同上 §2.4（report 缺失先报 guard）；bedtools 8/8 启动子比对实跑**留给 CI Linux**（本机无 bedtools，如实标注） |
| `check_isoform_case.py` / Real case 本机等价验证 | 同上 §2.5（`ISOFORM_EXIT=0`）、§2.6（`REAL_CASE_EXIT=0`，无 `Unknown exact gene ID`） |

> **状态归属硬约束**：以上均为**本机 A 类**证据。三平台 `validation` 由 failure 转 success 属
> **R2 远端实跑的目标态**，**不计入本机 A 类闭环**；`public-data-validation` 外网失败永远单独归类。

---

## 4. 结果回收后的填写口径（当前全部 `not_collected`）

- 各平台 `job-summary.md` 的 `status_transition` 由 `"pending"` 填为 **`failure → success`**（或如实
  `failure → failure`），并填 `skipped_after`（须 < 12）、`goldset_executed`、`python_consumers`、
  `artifacts` 实际路径。
- 汇总 `README.md` 的 `run_id` / `commit_sha` / `trigger` / `collected_at_utc` 由主理人经远端 API 回收后填。
- **结果真实回收前，任何字段不得写 `pass` / success**。
