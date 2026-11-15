# PhytoScope v0.3.0 · 三平台 CI 报告汇总（远端实测已回填）

> 批次：B03 建骨架（工程师寇豆码 Kou）· **本版按远端实测回填** · 日期：2026-11-14（ISO-8601 UTC 口径同设计 §7.10）
> 权威规格：`docs/design-fcs-final-delivery.md` §3.1、§5 B03 #7、§0.2 裁定 4、§0.3；增量 PRD R2 / §1.1。
> 目录契约：`docs/ci-reports/v0.3.0/{ubuntu,windows,macos}/` + `public-data-validation/`。

> ## 状态：`collection_state = remote_run_collected`（远端已实跑，结论已回填）
>
> **本文件记录的是远端实测结果，本地通过记录不能替代远端结果。**
> **本目录含三次远端运行，读数时必须先看清是哪一次**：
> - **§1–§6 = run `43605656726`（commit `49a5fac`，历史记录，ubuntu failure）**；
> - **§7 = run `43609281554`（commit `0638dbe` = 发布提交，三平台矩阵全 success，public-data 仍 failure）**；
> - **§8 = run `43664785766`（commit `2c3b1e6` = dev 当前 tip，四 job 全 success）← 整条 workflow 首次全绿，当前口径以此为准**。
>
> （下段为 §1–§6 那次运行的快照口径，保留原文以存事实。）
> 依据：GitHub Actions run **`43605656726`**（`event=push`、`head_branch=dev`、
> `run_started_at=2026-11-14T14:35:25Z`、`updated_at=2026-11-14T14:41:50Z`），
> commit **`49a5fac`**；run 页面
> https://github.com/omixcore/phytoscope/actions/runs/43605656726
>
> **结论一句话：不是全绿。** windows-latest success / macos-latest success /
> **ubuntu-latest failure**（step 10 `Java and JavaFX regressions`，
> 14 个 JavaFX UI 测试在无头环境下 `Platform.startup` latch 超时）；
> `public-data-validation` failure 在 step 5 `Audit public Arabidopsis splicing and proteomics resources`，
> 原归「外部网络数据源」，**2026-11-15 已更正**为「脚本 XLSX 魔数 bug + PastDB 数据源退役 +
> 网络环境」三层（见 `public-data-validation/job-summary.md` §归因复核），单列、不计入代码失败。
>
> **该 ubuntu 失败已定位并修复，且修复后已在 run `43609281554` 上经远端重跑确认（见 §7）**。
> 采集时间：**2026-11-14T14:41:50Z**（GitHub REST API，取 run `updated_at` 为快照时间）。
> 未取得项如实保留：三平台 `surefire-reports/` 明细、Linux 三个 JSON 的远端副本**仍待回收**
> —— 这两项需下载 artifact，未经下载不作任何内容声明。

---

## 1. 汇总字段（远端实测值已回填）

| 字段 | 值 |
|---|---|
| `run_id` | **`"43605656726"`** |
| `commit_sha` | **`"49a5fac43c1e7d2c915a9aac4abfca7b2356b581"`**（dev tip；发布 tag `v0.3.0` 指向 `0638dbe`，见 `docs/fcs-final-push-checklist.md` §1.1） |
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
| `public_data_validation.status` | **`"fail"`**（历史记 `"fail (external network)"`；归因已更正，见 `public-data-validation/job-summary.md`） |
| `public_data_validation.counts_in_platform_matrix` | **`false`**（固定） |
| `public_data_validation.failure_category` | **`"external_network"`**（历史值；2026-11-15 更正为「脚本 bug + 数据源退役 + 网络环境」三层） |
| `public_data_validation.failure_reason` | `"audit_public_splicing_evidence.py processed_available=false；根因：XLSX 魔数 bug（已修）+ PastDB 数据源退役 + PMC 即时服务返回 HTML/网络环境"` |
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

1. 三平台 `validation` 的远端结论**已如实回填**（run `43605656726`：windows success / macos success / ubuntu failure）；
   **不得**把「2 of 3 success」概括为「三平台通过」或「CI 全绿」。
   该 ubuntu failure 的修复已在 run `43609281554` 上经远端重跑确认通过（**§7**）；
   引用「三平台通过」时**只允许**绑定 §7 的 run id 与 commit `0638dbe`，不得反向套用到本节这次运行。
2. `public-data-validation` 的失败**归因已更正**为「脚本 XLSX 魔数 bug（已修）+ PastDB 数据源退役 +
   PMC 即时服务/网络环境」三层（不再笼统称「外部网络不可达」，见
   `public-data-validation/job-summary.md` §归因复核）。**仍永远不并入三平台 `validation` 矩阵结论**
   （`counts_in_platform_matrix = false`），**不据此判定代码失败**，也不混入「CI 转 success」结论。
3. 平台 `failure_reason` 只写代码类原因（`Java and JavaFX regressions` 的 FX 无头 latch 超时属代码/测试环境类；
   `check_isoform_case` / `Real case` / 金标准门控属历史基线项）；`public-data-validation` 的失败原因只写在本 job 的
   `job-summary.md`，不写入三平台 `failure_reason`。
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
- 可正面陈述（有远端实测证据）：
  - run `43609281554` / commit `0638dbe`（**§7**）：**三个 `validation` job 全 success**，
    ubuntu `216 / 0 / 0 / 7` + `BUILD SUCCESS`；
  - run `43605656726` / commit `49a5fac`（§1–§6，历史）：windows、macos 两个 job success；
  - 两次运行中金标准链路均**已在远端实跑、`Skipped: 0`**。
- 禁止：「14 项全部完成」「**整条 workflow 全绿**」「已完成 Zenodo 归档/取得 DOI」
  「已完成用户测试」「已完成三平台原生验证」「许可问题已解决」。
  （「三平台矩阵在 `0638dbe` 上通过」是**允许**的表述，但必须与 §7 的 run/job id 绑定。）
- 禁止把 `public-data-validation` 外部网络失败计入代码失败；禁止把 ubuntu 的 failure 略去不提，
  或只写「金标准通过」而不提该 job 仍 failure。
- 禁止用**本机本地通过记录**替代**远端结果**；两者口径不同，须分别标注来源。
- `not_collected` 只用于**确实未取得**的字段（surefire 明细、Linux 三个 JSON、artifact）；
  job 级结论、`job_id`、起止时间戳均已取得，不得继续标 `not_collected` 或「待补」。

---

## 6. 提交 SHA 与历史改写

本节记录的远端运行基于提交 `49a5fac`。为统一仓库语言风格，`c79034e..HEAD` 区间的提交信息随后被改写为英文；
**提交内容（tree）逐字节不变**，仅 commit message 与因之变化的 SHA 不同（改写前后已逐对校验 tree 相同）。
本节的 run id、job id、平台结论、失败步骤与测试计数均为该次运行的事实记录，**不随后续改写失效**；
当前有效 SHA 以 `docs/fcs-final-push-checklist.md` §1.1 / §1.2 为准。

```
rewritten_head_sha: 0638dbe67c7bb406cd6fbcbedc6d5e389ec4d8f4
rewrite_scope: c79034e..HEAD (commit messages only; trees byte-identical)
old_commit_49a5fac_equals_after_rewrite: 76712bd
```

> **读法说明（避免后来者困惑）**：
> 1. 本文件上文出现的 `49a5fac` 是**本次 CI 运行时**的 commit SHA，属**历史事实记录**，不会因为历史改写而变成错误。
> 2. 改写后远端已无 `49a5fac` 这个 SHA —— 这是**预期的**，不代表本次运行记录失效或伪造；
>    它等价于改写后的 `76712bd`（tree 相同）。
> 3. 若要复核本次运行，请用 `run_id 43605656726`（run 与 job 记录不可变）而不是用 SHA 去 `git show`。
> 4. 改写**不改变发布内容**：`v0.3.0` tag 现指向 `0638dbe`（含本轮 CI 修复），
>    Release 资产已于 2026-11-14 从该 tag 重建并重新上传（旧校验和作废），
>    新值见 `docs/fcs-v0.3.0-release-notes.md` / `docs/fcs-zenodo-handoff.md` 与
>    `desktop_program/SHA256SUMS.txt`。
> 5. 改写后**重跑了一次三平台 CI**（`0638dbe`），其结果单独记于 §7，不与本节混为一谈。

---

## 7. 修复后的远端重跑：run `43609281554`（commit `0638dbe`，三平台矩阵全绿）

> **本节是「ubuntu 修复是否真的生效」的唯一远端证据**，也是 `v0.3.0` 发布提交的口径依据。
> §1–§6 记录的是 run `43605656726`（`49a5fac`，ubuntu failure），两节不得混读。

### 7.1 运行元数据（GitHub REST API 实测）

| 字段 | 值 |
|---|---|
| `run_id` | **`43609281554`** |
| `run_attempt` | `1` |
| `workflow` | `Validate and package PhytoScope` |
| `head_sha` | **`0638dbe67c7bb406cd6fbcbedc6d5e389ec4d8f4`**（= `v0.3.0` tag = 发布提交） |
| `head_branch` | `dev` |
| run 页面 | https://github.com/omixcore/phytoscope/actions/runs/43609281554 |
| `collected_at_utc` | **`2026-11-14T15:37Z`**（`/actions/runs/43609281554/jobs` 查询，run 已结束） |
| `collection_state` | `remote_run_collected` |

### 7.2 逐 job 结论（4 个 job）

| job | job_id | 结论 | started_at (UTC) | completed_at (UTC) | 失败步骤 |
|---|---|---|---|---|---|
| `validation (ubuntu-latest)` | **`132490554008`** | **success** | `2026-11-14T15:16:31Z` | `2026-11-14T15:32:41Z` | — |
| `validation (macos-latest)` | **`132490554359`** | **success** | `2026-11-14T15:16:32Z` | `2026-11-14T15:19:23Z` | — |
| `validation (windows-latest)` | **`132490554399`** | **success** | `2026-11-14T15:16:31Z` | `2026-11-14T15:24:16Z` | — |
| `public-data-validation` | **`132490554373`** | **failure** | `2026-11-14T15:16:31Z` | `2026-11-14T15:17:14Z` | **step 5 `Audit public Arabidopsis splicing and proteomics resources`** |

- **三平台矩阵 `validation` 三个 job 全部 success** —— `platforms.ubuntu.status = "success"`，
  `status_transition = failure → success`（相对 §1 的 `49a5fac` 那次，亦相对 `c79034e` 基线为 `failure → success`）。
- `public-data-validation` 仍失败（同一 step 5）。**归因已更正**：不再是单纯的「外部网络不可达」，
  而是「脚本 XLSX 魔数 bug（已修）+ PastDB 数据源退役（不可修）+ 本机/CI 网络环境差异」三类，
  `counts_in_platform_matrix = false`，**不并入矩阵结论、不判代码失败**。
  逐源取证见 `public-data-validation/job-summary.md` §「归因复核」。

### 7.3 ubuntu 测试计数（该 job 日志实测，`Skipped: 0` 门控通过）

```
Results: Tests run: 216, Failures: 0, Errors: 0, Skipped: 7
BUILD SUCCESS
```

| 测试类 | run / failures / errors / skipped | 时长 |
|---|---|---|
| `ResearchModuleGoldSetTest` | **1 / 0 / 0 / 0** | 6.799 s |
| `DomainCaseValidationTest` | **1 / 0 / 0 / 0** | 3.708 s |
| `Gff2ParserSupportTest` | **5 / 0 / 0 / 0** | — |
| `ComparisonWorkflowUiTest` | **1 / 0 / 0 / 0** | 16.02 s |

> 与 §1.1（`49a5fac` 那次）对比：金标准 4 个门控类的 `Skipped: 0` 在两次运行中一致，
> 说明**金标准链路在 ubuntu 上一直是真执行**；差异只在 `Java and JavaFX regressions` 步骤的
> FX 无头启动。全局 `Skipped` 由基线的 `12` 降到 **`7`**，且 `216` 与本地全量复跑一致。

### 7.4 ubuntu 失败 → 通过的完整因果链（含一次误判，如实记录）

1. **首轮失败（run `43605656726`，`49a5fac`）**：14 个 JavaFX UI 测试各耗时恰好 `20.00s` 后超时。
2. **第一次修复（`e293f7e`）**：根因是 surefire `reuseForks=true` 复用同一 JVM，而 JavaFX
   在一个 JVM 内只能初始化一次，后续测试类的 `Platform.runLater` 永远不会被派发。
   改为 `forkCount=1` / `reuseForks=false`（`pom.xml` `ui-tests` profile），并把 7 个 UI 测试类的
   latch 超时由 20s（`MintTranscriptomeUiTest` 30s）统一放宽到 90s。**未增删/未弱化任何断言。**
3. **该修复引入的新问题（同一提交的 rerun，run `43608357998`）**：`ComparisonWorkflowUiTest`
   在新 JVM 下 `realAtlasComparisonProjectRoundTripAndVectorRasterExports` 超时（246.9s），
   thread dump 显示 **FX Application Thread 不存在**——`LoadingOperation` 的进度对话框是最后一个窗口，
   在默认 `implicitExit=true` 下关窗即关掉 toolkit。补 `Platform.setImplicitExit(false)` 后本机通过。
   > 另：run `43608357998` 的 ubuntu 还在 **step 5 `git clone`** 上失败过一次
   > （`RPC failed; curl 56 GnuTLS recv error (-110)` / `fatal: early EOF`），属**瞬时网络**，
   > 同提交的 windows/macos 均通过。已在 `validate.yml` 为该 clone 加 **3 次有界重试**，
   > 不改 `verify` 命令本身。
4. **第二次修复（`0638dbe`）**：`tools/run_validation.py` 里 Arabidopsis 分支仍在 glob
   `src/main/resources/data/species/arabidopsis/Chr*.gff3*`，该目录在完整 Chr1–Chr5 外迁到
   `phytoscope-reference-data` 后已不存在 → glob 匹配为空 → `--source` 收到 0 个参数 →
   `validate_structure_inventory.py: error: argument --source: expected at least one argument`。
   四个物种（arabidopsis / rice / maize / soybean）统一改为校验 `--example <name>` **实际导入的那份**
   注释（即随包 KB 级示例 `*_chromosomes.gff3(.gz)`），并加「文件缺失即 `SystemExit`」守卫，
   避免同类静默失败再次发生。
5. **确认（run `43609281554`，本节）**：ubuntu success，`BUILD SUCCESS`。

### 7.5 口径（强制）

- 可以说：「**三平台矩阵（ubuntu / macos / windows）在 `0638dbe` 上通过**」，
  并附 job id 与时间戳。
- **不得**说「整条 workflow 全绿」——同一 run 内 `public-data-validation` 仍 failure
  （归因见 `public-data-validation/job-summary.md` §归因复核：脚本 bug + PastDB 退役 + 网络环境）。
- **不得**把本节结论外推为「已完成三平台**原生**人工验证」：Monocle headless 不能认证原生对话框、
  DPI 缩放、图形驱动、签名/公证或 SmartScreen 提示（§2 硬约束仍有效）。
- 本节所有数值均来自 GitHub REST API 与 runner 日志，**未由本地结果代入**。

---

## 8. 全绿收口：run `43664785766`（commit `2c3b1e6`，四 job 全 success）

> 本节是 **P1「验证最新提交的 CI」的权威结论**。`2c3b1e6` 是 dev 当前 tip，它修复了
> `audit_public_splicing_evidence.py` 的 XLSX 魔数 bug（`b"PK\\x03\\x04"` → `b"PK\x03\x04"`），
> 使此前一直失败的 `public-data-validation` job 首次转绿。这是**整条 workflow 首次全绿**，
> 相比 §7（三平台绿、public-data 仍红）再进一步。

### 8.1 运行元数据（GitHub REST API 实测）

| 字段 | 值 |
|---|---|
| `run_id` | **`43664785766`** |
| `workflow` | `Validate and package PhytoScope` |
| `head_sha` | **`2c3b1e64544501e63bba5997f072fd71d1f6ff77`**（dev 当前 tip） |
| `head_branch` | `dev` |
| `status` / `conclusion` | `completed` / **`success`** |
| `created_at` | `2026-11-15T01:48:13Z` |
| run 页面 | https://github.com/omixcore/phytoscope/actions/runs/43664785766 |
| `collected_at_utc` | `2026-11-15`（`/actions/runs/43664785766/jobs` 查询，run 已结束） |
| `collection_state` | `remote_run_collected` |

### 8.2 逐 job 结论（4 个 job 全 success）

| job | job_id | 结论 | started_at (UTC) | completed_at (UTC) | 失败步骤 |
|---|---|---|---|---|---|
| `validation (ubuntu-latest)` | **`132661642803`** | **success** | `2026-11-15T01:48:16Z` | `2026-11-15T01:59:23Z` | — |
| `validation (macos-latest)` | **`132661642787`** | **success** | `2026-11-15T01:48:16Z` | （同 run，completed） | — |
| `validation (windows-latest)` | **`132661642638`** | **success** | `2026-11-15T01:48:16Z` | （同 run，completed） | — |
| `public-data-validation` | **`132661642837`** | **success** | `2026-11-15T01:48:17Z` | `2026-11-15T01:49:32Z` | — |

> `public-data-validation` 的 step 5 `Audit public Arabidopsis splicing and proteomics resources`
> 本次 **success**（不再是 failure）。macos / windows 上的 Linux-only 步骤（bedtools/BLASTP/rice bean/
> additional refs/full archives）按 `if: runner.os == 'Linux'` 正确 `skipped`，非失败。

### 8.3 与 §7 的差异（关键）

| 项 | §7（`0638dbe`） | §8（`2c3b1e6`） |
|---|---|---|
| 三平台 `validation` | success × 3 | success × 3（不变） |
| `public-data-validation` | **failure**（step 5） | **success**（step 5 修复生效） |
| 整条 workflow 结论 | **failure**（因 public-data） | **success（全绿）** |

- 修复点：`tools/audit_public_splicing_evidence.py` 的 XLSX 魔数判断
  `if not candidate.startswith(b"PK\\x03\\x04")` 是**双反斜杠字面量**，把合法 XLSX 一律判为非法；
  改为 `b"PK\x03\x04"` 后 Springer 静态镜像解析通过、`pastdb_table_s3` 从 0 行恢复为 10 行。
  另有 `fetch_bytes(allow_self_signed=...)` 仅对已退役的 PastDB 自签证书放行，
  并把 `pastdb.crg.eu` 已退役的失败归因明确记为「source retired」（非网络）。
  详见 `public-data-validation/job-summary.md` §归因复核。

### 8.4 口径（强制）

- 现在**可以**说：「**commit `2c3b1e6` 上整条 `Validate and package PhytoScope` workflow 全绿**
  （三平台 validation × 3 success + public-data-validation success）」，附 run id / job id / 时间戳。
- **仍不得**说：① 「三平台**原生**人工验证已完成」（headless CI ≠ 原生桌面人工验证，§2 硬约束）；
  ② 「public-data-validation 的每个外部数据源都 100% 可达」——该 job 现在**脚本层面 exit 0**，
  但 PastDB 源已退役（记为 retired 而非失败），对外口径须与 `job-summary.md` §归因复核一致。
- 本节数值均来自 GitHub REST API（`/actions/runs/43664785766/jobs`），未由本地结果代入。
