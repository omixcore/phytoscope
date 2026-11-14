# windows-latest / validation · job-summary（远端实测已回填）

> 目录契约见 `docs/ci-reports/v0.3.0/README.md` §1.1 / §2。
> **本文件记录的是远端实测结果，本地通过记录不能替代远端结果。**

## 结论

| 项 | 值 |
|---|---|
| job 名 | `validation (windows-latest)` |
| job id | **`132479612528`** |
| 结论 | **`success`** |
| `status_transition` | **`failure → success`**（`c79034e` 基线为 failure） |
| started_at (UTC) | `2026-11-14T14:35:28Z` |
| completed_at (UTC) | `2026-11-14T14:37:28Z` |
| 证据来源 | GitHub Actions run **`43605656726`**（`event=push`、`head_branch=dev`、`run_started_at=2026-11-14T14:35:25Z`、`updated_at=2026-11-14T14:41:50Z`），commit **`49a5fac`**；run 页面 https://github.com/omixcore/phytoscope/actions/runs/43605656726 |

## 关键测试计数（远端实测，`Skipped: 0`）

| 测试类 | run / failures / errors / skipped |
|---|---|
| `ResearchModuleGoldSetTest` | **1 / 0 / 0 / 0** |
| `DomainCaseValidationTest` | **1 / 0 / 0 / 0** |
| `Gff2ParserSupportTest` | **5 / 0 / 0 / 0** |
| `ComparisonWorkflowUiTest` | **1 / 0 / 0 / 0** |

> 该 job 无失败步骤，金标准门控在远端为 `Skipped: 0`。

## 字段

| 字段 | 值 |
|---|---|
| `status` | **`"success"`** |
| `baseline_status` | `"failure"`（`c79034e`，固定） |
| `tests` | `{run: null, failures: null, errors: null, skipped: null}`（**总计数待从 `surefire-reports/` 回收后填**） |
| `skipped_before` | `12`（全局基线） |
| `skipped_after` | 金标准相关 4 类实测 `Skipped: 0`；全局 Skipped 总数**待回收后填** |
| `goldset_executed` | **`true`** |
| `goldset_json_present` | `false`（Windows 不产出 Linux 专属 JSON；`surefire-reports/` **待回收**） |
| `python_consumers` | 不适用（Linux 专属） |
| `artifacts` | `[]`（**待回收**） |
| `failure_reason` | `""`（本 run 无失败步骤） |

## 待回收产物清单

| 文件 | 状态 |
|---|---|
| `surefire-reports/`（逐类报告明细） | **待回收**（目录占位，本机不生成、不伪造） |

> Linux 专属的 3 个 JSON 不适用于本平台。

## 基线失败项（`c79034e`，抄录 `c79034e-baseline.md`）

- `Real case, no display required`（`validate.yml:83-88`）`error: Unknown exact gene ID: AT1G67090`，退出码 3 → **代码类**，B01 已修（改走 `--gff3 examples/arabidopsis-atlas/annotation.gff3`）。
- 连带：金标准门控未传参 → `Skipped: 1` → **代码类**，B01 已修。本 run 已在远端确认 `Skipped: 0`。
