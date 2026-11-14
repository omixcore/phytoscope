# ubuntu-latest / validation · job-summary（远端实测已回填）

> 目录契约见 `docs/ci-reports/v0.3.0/README.md` §1.1 / §2。
> **本文件记录的是远端实测结果，本地通过记录不能替代远端结果。**

## 结论

| 项 | 值 |
|---|---|
| job 名 | `validation (ubuntu-latest)` |
| job id | **`132479612471`** |
| 结论 | **`failure`** |
| `status_transition` | **`failure → failure`**（`c79034e` 基线 failure，本 run 仍 failure） |
| started_at (UTC) | `2026-11-14T14:35:28Z` |
| completed_at (UTC) | `2026-11-14T14:41:49Z` |
| 证据来源 | GitHub Actions run **`43605656726`**（`event=push`、`head_branch=dev`、`run_started_at=2026-11-14T14:35:25Z`、`updated_at=2026-11-14T14:41:50Z`），commit **`49a5fac`**；run 页面 https://github.com/omixcore/phytoscope/actions/runs/43605656726 |

## 失败步骤与原因（代码类，非外网）

- **失败步骤**：**step 10 `Java and JavaFX regressions`**
- **原因**：**14 个 JavaFX UI 测试在无头环境下 `Platform.startup` latch 超时**
- **归类**：代码 / 测试环境类（`failure_category` 不属 `external_network`）
- **状态**：修复**已提交**，且已在 run `43609281554` 上**经远端重跑确认通过**（见文末「重跑确认」）

## 关键测试计数（远端实测，`Skipped: 0`）

| 测试类 | run / failures / errors / skipped |
|---|---|
| `ResearchModuleGoldSetTest` | **1 / 0 / 0 / 0** |
| `DomainCaseValidationTest` | **1 / 0 / 0 / 0** |
| `Gff2ParserSupportTest` | **5 / 0 / 0 / 0** |
| `ComparisonWorkflowUiTest` | **1 / 0 / 0 / 0** |

> 金标准链路**确已在远端实跑**，`Skipped: 0` 说明金标准门控不再是 `Skipped: 1` ——
> `c79034e` 基线的「金标准未执行」问题已在远端被证伪修复。
> 但**金标准通过 ≠ 该 job 通过**：本 job 仍因上面的 FX 无头超时而 failure。

## 字段

| 字段 | 值 |
|---|---|
| `status` | **`"failure"`** |
| `baseline_status` | `"failure"`（`c79034e`，固定） |
| `tests` | `{run: null, failures: null, errors: null, skipped: null}`（**总计数待从 `surefire-reports/` 回收后填**） |
| `skipped_before` | `12`（全局基线） |
| `skipped_after` | 金标准相关 4 类实测 `Skipped: 0`；全局 Skipped 总数**待回收后填** |
| `goldset_executed` | **`true`** |
| `goldset_json_present` | `false`（JSON 未回收，**待回收**） |
| `python_consumers` | `{bedtools: "pending", blastp: "pending"}` |
| `artifacts` | `[]`（**待回收**） |
| `failure_reason` | **`"Java and JavaFX regressions 步骤失败：14 个 JavaFX UI 测试无头环境 Platform.startup latch 超时"`** |

## 待回收产物清单（Linux 专属）

| 文件 | 状态 |
|---|---|
| `surefire-reports/`（逐类报告明细） | **待回收**（目录占位，本机不生成、不伪造） |
| `research-module-gold-set.json` | **待回收** |
| `bedtools-promoter-comparison.json` | **待回收** |
| `blastp-homology-comparison.json` | **待回收** |

## 基线失败项（`c79034e`，抄录 `c79034e-baseline.md`）

- `check_isoform_case.py`（`validate.yml:48`）`FileNotFoundError: src/main/resources/data/species/arabidopsis/TAIR10_genomic.fna.gz` → **代码类**，B01 已修。
- 连带：金标准门控未传参 → `Skipped: 1`、gold-set JSON 不产出、bedtools/BLASTP 失去输入 → **代码类**，B01 已修。

---

## 重跑确认（run `43609281554` / commit `0638dbe`）

> 上表记录的是 run `43605656726`（`49a5fac`，failure）。修复后重跑的结果如下，
> **当前发布口径以本段为准**；逐 job 全量记录见 `docs/ci-reports/v0.3.0/README.md` §7。

| 项 | 值 |
|---|---|
| job 名 | `validation (ubuntu-latest)` |
| job id | **`132490554008`** |
| 结论 | **`success`** |
| `status_transition` | **`failure → success`**（相对 `49a5fac` / `c79034e` 均为 failure） |
| started_at (UTC) | `2026-11-14T15:16:31Z` |
| completed_at (UTC) | `2026-11-14T15:32:41Z` |
| 证据来源 | run **`43609281554`**（`run_attempt=1`、`head_branch=dev`、`head_sha=0638dbe67c7bb406cd6fbcbedc6d5e389ec4d8f4`）；https://github.com/omixcore/phytoscope/actions/runs/43609281554 |

日志实测：

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

> 全局 `Skipped` 已由基线 `12` 降至 **`7`**；`errors = 0`、`failures = 0`。
> `surefire-reports/` 与三个 Linux JSON 的**内容**仍未下载回收，仍标「待回收」——
> 本表只填「已确证的计数」，不用本机报告充数。
