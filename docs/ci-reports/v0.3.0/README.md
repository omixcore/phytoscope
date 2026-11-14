# PhytoScope v0.3.0 · 三平台 CI 报告汇总（骨架，B 类）

> 批次：B03 · 执行人：工程师寇豆码（Kou）· 日期：2026-11-14（ISO-8601 UTC 口径同设计 §7.10）
> 权威规格：`docs/design-fcs-final-delivery.md` §3.1、§5 B03 #7、§0.2 裁定 4、§0.3；增量 PRD R2 / §1.1。
> 目录契约：`docs/ci-reports/v0.3.0/{ubuntu,windows,macos}/` + `public-data-validation/`。

> ## 状态：`collection_state = materials_ready_pending_remote`（物料齐备，待远端回收）
>
> **所有结果状态初值一律 `not_collected`。结果真实回收前不得写 `pass`、不得写 success、不得填
> 任何 surefire / JSON 内容。** 三平台 `validation` 由 failure 转 success 属 **R2 远端实跑的目标态**，
> **不计入本机 A 类闭环**（本机 A 类闭环由 `docs/fcs-ci-goldset-fix-report.md` 本地实跑留痕证明）。
> 回收执行主体固定「**主理人（GitHub Actions REST API artifact download）**」，本机不生成、不伪造报告。
> **待远端回收 / 未取得**：run id、三平台 surefire-reports、三个 Linux JSON 均为空。

---

## 1. 汇总字段契约（初值如实，不得预填结论）

| 字段 | 初值 / 固定值 |
|---|---|
| `run_id` | `"pending_remote"` |
| `commit_sha` | `"pending_remote"`（R1 之后的新提交；回收后须与 `docs/fcs-final-push-checklist.md` 记录一致） |
| `workflow` | `"Validate and package PhytoScope"`（固定） |
| `trigger` | `"not_triggered"` |
| `triggered_at_utc` | `null` |
| `collected_at_utc` | `null` |
| `collection_method` | `"GitHub Actions REST API artifact download（主理人执行）"`（固定） |
| `collection_state` | **`"materials_ready_pending_remote"`** |
| `baseline_commit_sha` | **`"c79034e"`**（固定，PRD §1.1 对照基线） |
| `baseline_status` | `"validation 三平台全部 failure；public-data-validation 外部网络类 failure（单独归类）"`（固定） |
| `baseline_source` | `"docs/prd-fcs-final-delivery.md §1.1；docs/ci-reports/v0.3.0/c79034e-baseline.md"`（固定） |
| `platforms.{ubuntu,windows,macos}.status` | **`"not_collected"`** |
| `platforms.{os}.baseline_status` | `"failure"`（固定，`c79034e` 实跑结论） |
| `platforms.{os}.status_transition` | `"pending"`（初值；回收后填 **`failure → success`** 或 `failure → failure`） |
| `platforms.{os}.tests` | `{run: null, failures: null, errors: null, skipped: null}` |
| `platforms.{os}.skipped_before` | `12`（全局基线，固定） |
| `platforms.{os}.skipped_after` | `null`（回收后填，须 < 12 且 `ResearchModuleGoldSetTest` 非 skipped） |
| `platforms.{os}.goldset_executed` | `null` |
| `platforms.{os}.goldset_json_present` | `false` |
| `platforms.{os}.python_consumers` | `{bedtools: "pending", blastp: "pending"}` |
| `platforms.{os}.artifacts` | `[]` |
| `platforms.{os}.failure_reason` | `""`（失败时如实记录**代码类**原因；外网类原因一律归入 `public_data_validation`） |
| `public_data_validation.status` | **`"not_collected"`**（单列；典型 `"fail (external network)"`） |
| `public_data_validation.counts_in_platform_matrix` | **`false`**（固定） |
| `public_data_validation.failure_category` | **`"external_network"`**（固定，Q13，非代码逻辑） |
| `public_data_validation.failure_reason` | `"外网 PMC/PastDB/ENA 依赖不可达，非代码缺陷；不据此判定 validation job 失败"` |
| `evidence_chain_footnote` | `""`（**初值空字符串**，设计 §3.1 / §11.2；**证据链脚注，非失败项**）：R2 回收时由主理人如实记录「B01 曾暴露 `ComparisonWorkflowUiTest` 既有 FX 缺陷 → 主理人第三轮裁定必修 → B01b（`ca8743e`）测试侧两阶段 FX 重构修复（断言 verbatim / `src/main/java` 零改动 / 单条 `verify` 不变）→ 当前 HEAD 全量 `216/0/0/7` 绿」。**不并入 `c79034e` 基线、不并入外部网络类、不并入 failure → success 矩阵、不判代码失败**；本机 A 类留痕见 `docs/fcs-ci-goldset-fix-report.md` §2.8 与 `docs/fcs-phase2-verification-report.md` §5 |
| `overall_honest_statement` | `"A 类 7 项本机已闭环、B 类 6 项物料齐备待作者执行、C 类 1 项给出核查结论"`（固定） |
| `baseline_reference` | `"docs/prd-fcs-final-delivery.md §1 / §1.1（HEAD c79034e）；本 run 为其后的修复 HEAD"`（固定） |

---

## 2. 逐平台骨架（实际内容待远端回收）

| 平台 | `job-summary.md` | `surefire-reports/` | Linux 专属 3 个 JSON |
|---|---|---|---|
| ubuntu | [ubuntu/job-summary.md](ubuntu/job-summary.md) | 目录占位，**`not_collected`** | `research-module-gold-set.json` / `bedtools-promoter-comparison.json` / `blastp-homology-comparison.json`：**待远端回收** |
| windows | [windows/job-summary.md](windows/job-summary.md) | 目录占位，**`not_collected`** | 不适用 |
| macos | [macos/job-summary.md](macos/job-summary.md) | 目录占位，**`not_collected`** | 不适用 |
| public-data-validation（单列） | [public-data-validation/job-summary.md](public-data-validation/job-summary.md) | 不适用 | 不适用 |

> **硬约束**：本机（Windows）**不得**生成或伪造 `surefire-reports/` 内容、三个 JSON 的内容。
> 骨架中只能出现目录 + 模板 + `not_collected`。真实报告由主理人在推送后经远端 API 回收填入。

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

1. 三平台 `validation` 由 failure 转 success 是 **R2 远端实跑的目标态证据，不计入本机 A 类闭环**。
2. `public-data-validation` 的失败是**外部网络类**（PMC/PastDB/ENA 不可达，`failure_category = external_network`），
   **永远不并入三平台 `validation` 矩阵结论**（`counts_in_platform_matrix = false`），
   **不据此判定代码失败**，也不混入「CI 转 success」结论。
3. 平台 `failure_reason` 只写代码类原因（`check_isoform_case` / `Real case` / 金标准门控）；
   外网类原因一律归入 `public_data_validation`。
4. `docs/fcs-phase2-verification-report.md` 与 `docs/fcs-submission-gap-audit.md` #10
   在结果真实回收前只写「**待远端回收**」，不得写 pass。
5. **证据链脚注（非失败项，设计 §11.2）**：B01 → B01b 的 `ComparisonWorkflowUiTest` 既有 FX
   缺陷修复（B01b / `ca8743e`，测试侧两阶段 FX 重构、断言 verbatim、`src/main/java` 零改动、
   当前 HEAD 全量 `216/0/0/7` 绿）只写入 `evidence_chain_footnote`，**不并入 `c79034e` 基线、
   不并入 `public-data-validation` 外部网络类、不进 failure → success 矩阵、不登记为失败、
   不判代码失败**。主理人第三轮裁定已否决「重新门控 / 记已知失败 / 改生产加载为同步」三种变通。

---

## 5. 对外口径（禁止虚报）

- 固定表述：「**A 类 7 项本机已闭环、B 类 6 项物料齐备待作者执行、C 类 1 项给出核查结论**」。
- 禁止：「14 项全部完成」「CI 三平台通过（指 `c79034e`）」「已完成 Zenodo 归档/取得 DOI」
  「已完成用户测试」「已完成三平台原生验证」「许可问题已解决」。
- 禁止把「推送凭证可用」写成「已推送」；把现有 `v0.2.0` Release 写成「已发布 v0.3.0」；
  把 `public-data-validation` 外部网络失败计入代码失败。
- **三平台转 success 属 R2 远端目标态，不计入本机 A 类闭环**；`future work` 与 B 类事项如实标注。
