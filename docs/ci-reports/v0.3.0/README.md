# PhytoScope v0.3.0 · 三平台 CI 报告汇总（远端实测已回填）

> 批次：B03 建骨架（工程师寇豆码 Kou）· **本版按远端实测回填** · 日期：2026-11-14（ISO-8601 UTC 口径同设计 §7.10）
> 权威规格：`docs/design-fcs-final-delivery.md` §3.1、§5 B03 #7、§0.2 裁定 4、§0.3；增量 PRD R2 / §1.1。
> 目录契约：`docs/ci-reports/v0.3.0/{ubuntu,windows,macos}/` + `public-data-validation/`。

> ## 状态：`collection_state = remote_run_collected`（远端已实跑，结论已回填）
>
> **本文件记录的是远端实测结果，本地通过记录不能替代远端结果。**
> 依据：GitHub Actions run **`43605656726`**（`event=push`、`head_branch=dev`、
> `run_started_at=2026-11-14T14:35:25Z`、`updated_at=2026-11-14T14:41:50Z`），
> commit **`49a5fac`**；run 页面
> https://github.com/omixcore/phytoscope/actions/runs/43605656726
>
> **结论一句话：不是全绿。** windows-latest success / macos-latest success /
> **ubuntu-latest failure**（step 10 `Java and JavaFX regressions`，
> 14 个 JavaFX UI 测试在无头环境下 `Platform.startup` latch 超时）；
> `public-data-validation` failure 在 step 5 `Audit public Arabidopsis splicing and proteomics resources`，
> 属**外部网络数据源**，单列、不计入代码失败。
>
> **ubuntu 的修复已提交但尚未经远端重跑确认**；重跑确认前**不得写「三平台通过」**。
> 采集时间：**2026-11-14T14:41:50Z**（GitHub REST API，取 run `updated_at` 为快照时间）。
> 未取得项如实保留：三平台 `surefire-reports/` 明细、Linux 三个 JSON 的远端副本**仍待回收**
> —— 这两项需下载 artifact，未经下载不作任何内容声明。

---

## 1. 汇总字段（远端实测值已回填）

| 字段 | 值 |
|---|---|
| `run_id` | **`"43605656726"`** |
| `commit_sha` | **`"49a5fac43c1e7d2c915a9aac4abfca7b2356b581"`**（dev tip；发布 tag `v0.3.0` 指向 `3e8b83c`，见 `docs/fcs-final-push-checklist.md` §1.1） |
| `workflow` | `"Validate and package PhytoScope"`（固定） |
| `trigger` | **`"push"`**（**不是** `workflow_dispatch`） |
| `triggered_at_utc` | **`"2026-11-14T14:35:25Z"`**（= `run_started_at` / `created_at`） |
| `run_updated_at_utc` | **`"2026-11-14T14:41:50Z"`** |
| `head_branch` | **`"dev"`** |
| `run_html_url` | https://github.com/omixcore/phytoscope/actions/runs/43605656726 |
| `collected_at_utc` | **`"2026-11-14T14:41:50Z"`**（以 run `updated_at` 为采集快照时间；run 已结束，其后状态不再变化） |
| `collection_method` | `"GitHub Actions REST API（run + jobs 查询）"`（固定口径） |
| `collection_state` | **`"remote_run_collected"`** |
| `baseline_commit_sha` | **`"c79034e"`**（固定，PRD §1.1 对照基线） |
| `baseline_status` | `"validation 三平台全部 failure；public-data-validation 外部网络类 failure（单独归类）"`（固定） |
| `baseline_source` | `"docs/prd-fcs-final-delivery.md §1.1；docs/ci-reports/v0.3.0/c79034e-baseline.md"`（固定） |
| `platforms.ubuntu.status` | **`"failure"`** |
| `platforms.windows.status` | **`"success"`** |
| `platforms.macos.status` | **`"success"`** |
| `platforms.{os}.baseline_status` | `"failure"`（固定，`c79034e` 实跑结论） |
| `platforms.ubuntu.status_transition` | **`failure → failure`**（基线 failure，本 run 仍 failure） |
| `platforms.windows.status_transition` | **`failure → success`** |
| `platforms.macos.status_transition` | **`failure → success`** |
| `platforms.{os}.tests` | `{run: null, failures: null, errors: null, skipped: null}`（**总计数待从 `surefire-reports/` 回收后填**；已确证的金标准计数见 §1.1） |
| `platforms.{os}.skipped_before` | `12`（全局基线，固定） |
| `platforms.{os}.skipped_after` | **`0`（仅指金标准相关 4 个测试类实测 `Skipped: 0`）**；**全局 Skipped 总数待回收后填**，不得把此 `0` 读作全局 |
| `platforms.{os}.goldset_executed` | **`true`**（远端实跑，见 §1.1） |
| `platforms.{os}.goldset_json_present` | `false`（JSON 未随 artifact 回收，**待回收**） |
| `platforms.{os}.python_consumers` | `{bedtools: "pending", blastp: "pending"}`（消费结果随 JSON 回收后填） |
| `platforms.{os}.artifacts` | `[]`（artifact 未下载，**待回收**） |
| `platforms.ubuntu.failure_reason` | **`"Java and JavaFX regressions 步骤失败：14 个 JavaFX UI 测试在无头环境下 Platform.startup latch 超时（代码类，非外网）"`** |
| `platforms.{windows,macos}.failure_reason` | `""`（该平台本 run 无失败） |
| `public_data_validation.status` | **`"fail (external network)"`** |
| `public_data_validation.counts_in_platform_matrix` | **`false`**（固定） |
| `public_data_validation.failure_category` | **`"external_network"`**（固定，Q13，非代码逻辑） |
| `public_data_validation.failure_reason` | `"外网 PMC/PastDB/ENA 依赖不可达，非代码缺陷；不据此判定 validation job 失败"` |
| `evidence_chain_footnote` | `""`（string；**证据链脚注，非失败项**，见设计 §3.1 / §11.2）：B01 曾暴露 `ComparisonWorkflowUiTest` 既有 FX 缺陷 → 主理人第三轮裁定必修 → B01b（`ca8743e`）测试侧两阶段 FX 重构修复（断言 verbatim / `src/main/java` 零改动 / 单条 `verify` 不变）。**不并入 `c79034e` 基线、不并入外部网络类、不进 failure→success 矩阵、不判代码失败**。**注意**：本 run ubuntu 的 FX 失败属**新增的无头环境并发/超时问题**，与该脚注的既有缺陷修复不是同一事项，勿混写 |
| `overall_honest_statement` | `"A 类 7 项本机已闭环、B 类 6 项物料齐备待作者执行、C 类 1 项给出核查结论"`（固定） |
| `baseline_reference` | `"docs/prd-fcs-final-delivery.md §1 / §1.1（HEAD c79034e）；本 run 为其后的修复 HEAD"`（固定） |

---

## 1.1 远端实测结果（按 job，run `43605656726` / commit `49a5fac`）

| job | job_id | 结论 | started_at (UTC) | completed_at (UTC) | 失败步骤 |
|---|---|---|---|---|---|
| `validation (ubuntu-latest)` | **`132479612471`** | **failure** | `2026-11-14T14:35:28Z` | `2026-11-14T14:41:49Z` | **step 10 `Java and JavaFX regressions`** |
| `validation (windows-latest)` | **`132479612528`** | **success** | `2026-11-14T14:35:28Z` | `2026-11-14T14:37:28Z` | — |
| `validation (macos-latest)` | **`132479612388`** | **success** | `2026-11-14T14:35:29Z` | `2026-11-14T14:37:51Z` | — |
| `public-data-validation` | **`132479612205`** | **failure** | `2026-11-14T14:35:29Z` | `2026-11-14T14:36:40Z` | **step 5 `Audit public Arabidopsis splicing and proteomics resources`** |

失败原因（按 job）：
- `validation (ubuntu-latest)` / step 10 `Java and JavaFX regressions`：**14 个 JavaFX UI 测试在无头环境
  `Platform.startup` latch 超时**（代码 / 测试环境类，**非外网**）。
- `public-data-validation` / step 5 `Audit public Arabidopsis splicing and proteomics resources`：
  **外网 PMC / PastDB / ENA 不可达**（`failure_category = external_network`），
  **非代码缺陷**，`counts_in_platform_matrix = false`。

### 金标准链路已在远端确认实跑（`Skipped: 0`）

| 测试类 | 远端结果（run / failures / errors / skipped） |
|---|---|
| `ResearchModuleGoldSetTest` | **1 / 0 / 0 / 0** |
| `DomainCaseValidationTest` | **1 / 0 / 0 / 0** |
| `Gff2ParserSupportTest` | **5 / 0 / 0 / 0** |
| `ComparisonWorkflowUiTest` | **1 / 0 / 0 / 0** |

> **口径强调**：这四行是**远端**（ubuntu runner）实测，**`Skipped: 0`** 说明金标准门控在 CI 上
> 不再是 `Skipped: 1`，即 `c79034e` 基线的「金标准未执行」问题**已在远端被证伪修复**。
> 本机 `216 / 0 / 0 / 7` 是**本地**记录，**不能替代**上表；两者不可互换引用。
>
> 同时必须承认：金标准通过**不等于**整个 ubuntu job 通过 —— 该 job 仍因
> `Java and JavaFX regressions` 步骤的 14 个 FX UI 测试无头超时而 failure。

---

## 2. 逐平台状态（job 结论已回填，明细待回收）

| 平台 | job_id | 结论 | 失败步骤 | `job-summary.md` | `surefire-reports/` 明细 | Linux 专属 3 个 JSON |
|---|---|---|---|---|---|---|
| ubuntu | `132479612471` | **failure** | **step 10 `Java and JavaFX regressions`** | [ubuntu/job-summary.md](ubuntu/job-summary.md) | **待回收**（目录占位） | `research-module-gold-set.json` / `bedtools-promoter-comparison.json` / `blastp-homology-comparison.json`：**待回收** |
| windows | `132479612528` | **success** | — | [windows/job-summary.md](windows/job-summary.md) | **待回收**（目录占位） | 不适用 |
| macos | `132479612388` | **success** | — | [macos/job-summary.md](macos/job-summary.md) | **待回收**（目录占位） | 不适用 |
| public-data-validation（单列） | `132479612205` | **fail (external network)** | **step 5 `Audit public Arabidopsis splicing and proteomics resources`** | [public-data-validation/job-summary.md](public-data-validation/job-summary.md) | 不适用 | 不适用 |

> **硬约束（仍然有效）**：本机（Windows）**不得**生成或伪造 `surefire-reports/` 与三个 JSON 的内容。
> 已回填的只有 **job 级结论**与**明确的测试类计数**（§1.1），均来自远端 run 的真实输出；
> 未实际下载的 artifact 一律保持「**待回收**」，不得用本地报告充数。

---

## 3. 对照基线

- **`c79034e-baseline.md`**（本目录）：逐平台 failure 清单留档，抄录自 PRD §1.1，作为
  **failure → success** 变化的唯一对照基线。
- 基线 Java 层：`Tests run: 211, Failures: 0, Errors: 0, Skipped: 12`，其中
  `ResearchModuleGoldSetTest = Tests run: 1, Skipped: 1`。
- **禁止**把旧文档「CI 三平台通过（bedtools 8/8、BLASTP 20/20）」沿用为当前状态 —— 那是数据外迁前
  历史留痕（设计 §7.3 / PRD §8 第 5 条）。

---

## 4. 状态归属与归类（强制）

1. 三平台 `validation` 的远端结论**已如实回填**（windows success / macos success / ubuntu failure）；
   **不得**把「2 of 3 success」概括为「三平台通过」或「CI 全绿」。
   ubuntu 的修复已提交但**尚未经远端重跑确认**，确认前保持 failure 口径。
2. `public-data-validation` 的失败是**外部网络类**（PMC/PastDB/ENA 不可达，`failure_category = external_network`），
   **永远不并入三平台 `validation` 矩阵结论**（`counts_in_platform_matrix = false`），
   **不据此判定代码失败**，也不混入「CI 转 success」结论。
3. 平台 `failure_reason` 只写代码类原因（`Java and JavaFX regressions` 的 FX 无头 latch 超时属代码/测试环境类；
   `check_isoform_case` / `Real case` / 金标准门控属历史基线项）；外网类原因一律只归入 `public_data_validation`。
4. `docs/fcs-phase2-verification-report.md` 与 `docs/fcs-submission-gap-audit.md` #10
   须与本文件一致：**ubuntu 明确写 failure（附步骤与原因），不得写 pass**；
   金标准计数可引用 §1.1 的远端实测值，但**不得**把本地 `216/0/0/7` 当远端结果。
5. **证据链脚注（非失败项，设计 §11.2）**：B01 → B01b 的 `ComparisonWorkflowUiTest` 既有 FX
   缺陷修复（B01b / `ca8743e`，测试侧两阶段 FX 重构、断言 verbatim、`src/main/java` 零改动）
   只写入 `evidence_chain_footnote`，**不并入 `c79034e` 基线、不并入 `public-data-validation` 外部网络类、
   不进 failure → success 矩阵、不登记为失败、不判代码失败**。
   主理人第三轮裁定已否决「重新门控 / 记已知失败 / 改生产加载为同步」三种变通。
   **本 run ubuntu 的 FX 失败是另一个事项**（无头环境下 14 个 FX UI 测试 `Platform.startup` latch 超时），
   不属本脚注，不得借脚注「已闭环」掩盖 ubuntu 的 failure。

---

## 5. 对外口径（禁止虚报）

- 固定表述：「**A 类 7 项本机已闭环、B 类 6 项物料齐备待作者执行、C 类 1 项给出核查结论**」。
- 可正面陈述（有远端实测证据）：run `43605656726` / commit `49a5fac` 上
  **windows、macos 两个 `validation` job 为 success**，且**金标准链路已在远端实跑、`Skipped: 0`**。
- 禁止：「14 项全部完成」「**CI 三平台通过 / CI 全绿**」「已完成 Zenodo 归档/取得 DOI」
  「已完成用户测试」「已完成三平台原生验证」「许可问题已解决」。
- 禁止把 `public-data-validation` 外部网络失败计入代码失败；禁止把 ubuntu 的 failure 略去不提，
  或只写「金标准通过」而不提该 job 仍 failure。
- 禁止用**本机本地通过记录**替代**远端结果**；两者口径不同，须分别标注来源。
- `not_collected` 只用于**确实未取得**的字段（surefire 明细、Linux 三个 JSON、artifact）；
  job 级结论、`job_id`、起止时间戳均已取得，不得继续标 `not_collected` 或「待补」。

---

## 6. 提交 SHA 与历史改写

本节记录的远端运行基于提交 `49a5fac`。此后为统一仓库语言风格，`c79034e..HEAD` 区间的提交信息被改写为英文；
**提交内容（tree）逐字节不变**，仅 commit message 与因之变化的 SHA 不同。本节的 run id、job id、平台结论、
失败步骤与测试计数均为该次运行的事实记录，**不随后续改写失效**；当前有效 SHA 以
`docs/fcs-final-push-checklist.md` 的「发布提交 vs 分支最新提交」一节为准。

```
rewritten_head_sha: （待主理人回填）
```

> **读法说明（避免后来者困惑）**：
> 1. 本文件上文出现的 `49a5fac` 是**本次 CI 运行时**的 commit SHA，属**历史事实记录**，不会因为历史改写而变成错误。
> 2. 历史改写后 remote 上可能已无 `49a5fac` 这个 SHA —— 这是**预期的**，不代表本次运行记录失效或伪造。
> 3. 若要复核本次运行，请用 `run_id 43605656726`（run 与 job 记录不可变）而不是用 SHA 去 `git show`。
> 4. 历史改写**不改变发布内容**：`v0.3.0` tag 仍指向 `3e8b83c` 的**内容**（改写若涉及该提交，
>    其 tree 逐字节不变），Release 资产 `PhytoScope-Windows-0.3.0.zip` 46,185,790 B
>    与 `SHA256SUMS.txt` 1,547 B 亦不受影响。
> 5. 改写完成后须同步更新 `docs/fcs-final-push-checklist.md` §1.1 的 SHA 列，
>    并在此处回填 `rewritten_head_sha`。
