# public-data-validation（单列 · 外部网络类）· job-summary（远端实测已回填）

> 目录契约见 `docs/ci-reports/v0.3.0/README.md` §1.1 / §2、设计 §3.1 / 裁定 4 / Q13。
> **本 job 单列，不并入三平台 `validation` 矩阵。**
> **本文件记录的是远端实测结果，本地通过记录不能替代远端结果。**

## 结论

| 项 | 值 |
|---|---|
| job 名 | `public-data-validation` |
| job id | **`132479612205`** |
| 结论 | **`fail (external network)`** |
| `failure_category` | **`external_network`**（固定，**非代码逻辑**） |
| `counts_in_platform_matrix` | **`false`**（固定） |
| started_at (UTC) | `2026-11-14T14:35:29Z` |
| completed_at (UTC) | `2026-11-14T14:36:40Z` |
| 失败步骤 | **step 5 `Audit public Arabidopsis splicing and proteomics resources`** |
| 证据来源 | GitHub Actions run **`43605656726`**（`event=push`、`head_branch=dev`、`run_started_at=2026-11-14T14:35:25Z`、`updated_at=2026-11-14T14:41:50Z`），commit **`49a5fac`**；run 页面 https://github.com/omixcore/phytoscope/actions/runs/43605656726 |

## 失败原因

- 失败步骤为 **step 5 `Audit public Arabidopsis splicing and proteomics resources`**。
- **外网 PMC / PastDB / ENA 依赖不可达**，属**外部网络数据源**问题。
- **非代码缺陷**，**不据此判定 validation job 失败**，**不混入「CI 转 success」结论**。

## 字段

| 字段 | 值 |
|---|---|
| `status` | **`"fail (external network)"`** |
| `failure_category` | **`"external_network"`**（固定） |
| `counts_in_platform_matrix` | **`false`**（固定） |
| `failure_reason` | `"外网 PMC/PastDB/ENA 依赖不可达，非代码缺陷；不据此判定 validation job 失败"` |
| `collection_state` | **`remote_run_collected`** |
| `collection_method` | `"GitHub Actions REST API（run + jobs 查询）"` |
| `artifacts` | `[]`（目标：`audit_public_splicing_evidence` 产物 / 外网失败原因记录，**待回收**） |
| `status_transition` | **`failure → failure`**（`c79034e` 基线亦为外部网络类失败；**不参与** validation 的 failure → success 叙事） |

## 归类强制口径

- 该失败是 **外部网络类（PMC / PastDB / ENA 不可达）**，**非代码逻辑**。
- **不并入三平台 `validation` 矩阵结论**（`counts_in_platform_matrix = false`）。
- **不据此判定 validation job 失败**、**不混入「CI 转 success」结论**（PRD §8 第 5 条 / Q13）。
- 三平台 `failure_reason` 字段**不得**写入外网类原因；外网类原因一律只写在本文件。
- 不得把本 job 的失败与 ubuntu 的 FX 无头超时失败混为一谈：**两者性质不同**，
  ubuntu 的 failure 属代码/测试环境类，本 job 的 failure 属外部网络类。
