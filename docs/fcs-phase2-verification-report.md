# PhytoScope FCS · 阶段2 验证汇总（T06 落点，骨架，B 类）

> 批次：B03 · 执行人：工程师寇豆码（Kou）· 日期：2026-11-14（ISO-8601 UTC 口径同设计 §7.10）
> 权威规格：`docs/design-fcs-final-delivery.md` §5 B03 #7、§3.1、§0.2 裁定 4、§0.3；增量 PRD R2。
> 数据落点：`docs/ci-reports/v0.3.0/`（汇总 `README.md` + `c79034e-baseline.md` + 三平台 job-summary）。

> ## 状态：`not_collected` / `collection_state = materials_ready_pending_remote`
>
> **三平台 CI 结果待远端回收**，回收前本文件只有骨架与对照基线，**不写 pass、不写 success**。
> 三平台 `validation` 由 failure 转 success 属 **R2 远端实跑的目标态，不计入本机 A 类闭环**。
> 本机 A 类闭环由 `docs/fcs-ci-goldset-fix-report.md` 本地实跑留痕证明。

---

## 1. 对照基线（`c79034e`）

| 项 | 值 |
|---|---|
| `baseline_commit_sha` | `c79034e`（固定） |
| `baseline_status` | **三平台 `validation` 全部 failure**；`public-data-validation` 外部网络类 failure（单独归类） |
| Java 层 | `Tests run: 211, Failures: 0, Errors: 0, Skipped: 12` |
| 金标准 | `ResearchModuleGoldSetTest = Tests run: 1, Skipped: 1` |
| 逐平台失败留档 | `docs/ci-reports/v0.3.0/c79034e-baseline.md` |

> 禁止沿用旧文档「CI 三平台通过（bedtools 8/8、BLASTP 20/20）」—— 那是数据外迁前历史留痕。

---

## 2. 逐平台变化（骨架，结果待远端回收）

| 平台 | `baseline_status` | `status_transition` | `skipped_before → skipped_after` | 金标准是否实跑 | 两处 Python 是否消费成功 | 状态 |
|---|---|---|---|---|---|---|
| ubuntu | `failure` | `pending`（目标 `failure → success`） | `12 → null` | `null` | `{bedtools: pending, blastp: pending}` | **`not_collected`** |
| windows | `failure` | `pending`（目标 `failure → success`） | `12 → null` | `null` | `{bedtools: pending, blastp: pending}` | **`not_collected`** |
| macos | `failure` | `pending`（目标 `failure → success`） | `12 → null` | `null` | `{bedtools: pending, blastp: pending}` | **`not_collected`** |

- `public-data-validation`（**单列**）：`failure_category = external_network`、`counts_in_platform_matrix = false`、
  `status = not_collected`。**不并入三平台矩阵、不据此判定代码失败**。
- 回收后须逐平台确认：`ResearchModuleGoldSetTest` 非 skipped、`skipped_after < 12`、
  gold-set JSON 被 `compare_promoters_bedtools.py` / `compare_homology_blastp.py` **两处成功消费**。

---

## 3. 本机 A 类证据（指针，不与远端目标态混淆）

| 项 | 证据 |
|---|---|
| 三类代码类 CI 失败已修 | `docs/fcs-ci-goldset-fix-report.md` §2.2 / §2.5 / §2.6 |
| 金标准非 skipped（Skipped 由 12 下降） | 同上 §2.2（本机 `Tests run: 216, Skipped: 7`） |
| gold-set JSON 真实生成 | 同上 §2.3（`GOLDSET_OK`，52,392 B） |
| 两处 Python 消费 / report guard | 同上 §2.4（缺 report 先报 guard）；bedtools 8/8 比对实跑留给 CI Linux |
| 全量测试 `BUILD SUCCESS` | 同上 §2.8（B01b：`Tests run: 216, Failures: 0, Errors: 0, Skipped: 7`） |

> **不计入本机 A 类闭环**：三平台 `validation` 转 success 是 **R2 远端实跑的目标态**，由主理人在
> 推送后触发 GitHub Actions 并回收报告，本文件在回收前只做骨架。

---

## 4. 对外口径（强制）

- 固定表述：「**A 类 7 项本机已闭环、B 类 6 项物料齐备待作者执行、C 类 1 项给出核查结论**」。
- 本项（R2）属 **B 类 6 项**之一：**物料齐备待作者执行**，禁止写成「三平台 CI 已通过」。
- **待远端回收**字段不得预填；结果真实回收前不得写 pass。
- 禁止把「推送凭证可用」写成「已推送」、把现有 `v0.2.0` Release 写成「已发布 v0.3.0」。

---

## 5. 证据链脚注（非失败项，设计 §11.2）

> 本节记录一处「发现 → 裁定 → 修复」全链路，**属本机 A 类已闭环事实的证据链脚注，
> 非 R2 远端报告的失败项**。它**不并入** `c79034e` 逐平台失败基线（不同 commit）、
> **不并入** `public-data-validation` 外部网络类、**不进**三平台 failure → success 矩阵、
> **不登记为失败、不判代码失败**；三平台 success 仍属 R2 远端目标态，不得虚报。

1. **发现（B01 / `b26dbac`）**：Java 步传 `-Dphytoscope.referenceDataDir=target/reference-data`
   后 5 个归档门控类全部实跑，`ComparisonWorkflowUiTest` 暴露既有 FX 线程缺陷（单个长
   `FutureTask` 独占 FX 线程，令 `LoadingOperation` 的 `Platform.runLater` commit 回调排不上，
   `sequenceService.isLoaded()==false`）。属数据外迁后长期被门控遮蔽的既有 UI 缺陷，
   **非 R1 引入、非外部网络**。
2. **裁定（主理人第三轮）**：否决「重新门控 / 拆独立系统属性使其 skip」「记为已知失败」
   「把生产加载路径改为同步」；要求必修且**保持真实异步加载覆盖**，不得用 `@Disabled` /
   `Assumptions` 绕过。
3. **落地（B01b / `ca8743e`，当前 HEAD）**：测试侧两阶段短 FX 任务（setup 短任务 + 断言短任务），
   测试线程有界等待 `getPendingLoad()` / `getSequenceService().isLoaded()`；fixture 改用
   `MainView.loadExpression(...)` 刷新 detail pane（`Expression (TPM)` caption）。
   **断言 verbatim、`src/main/java/**` 零改动、`.github/workflows/validate.yml` 单条 `verify` 不变**；
   顺带 B01b-2 把 `check_isoform_case.py` 的 `candidates.json` 改为 JSON 语义比较。
4. **本机实跑结果**：`sh mvnw -B -Pui-tests -Dphytoscope.referenceDataDir=target/reference-data test`
   → `Tests run: 216, Failures: 0, Errors: 0, Skipped: 7`、`BUILD SUCCESS`；
   `ResearchModuleGoldSetTest` 与 `ComparisonWorkflowUiTest` 均 `1 / 0 / 0 / 0`；
   gold-set JSON 真实生成。逐条留痕见 `docs/fcs-ci-goldset-fix-report.md` §2.2 / §2.8 / §4。

> 回收汇总文档 `docs/ci-reports/v0.3.0/README.md` 的 `evidence_chain_footnote` 字段（初值 `""`）
> 由主理人在 R2 远端回收时按本脚注如实填写；本机不预填远端结果。
