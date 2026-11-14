# public-data-validation（单列·外部网络类）· job-summary（骨架，B 类）

> 批次：B03 · 目录契约见 `docs/ci-reports/v0.3.0/README.md` §2、设计 §3.1 / 裁定 4 / Q13。
> **本 job 单列，不并入三平台 `validation` 矩阵**。

| 字段 | 值 |
|---|---|
| `status` | **`not_collected`**（单列；典型 `"fail (external network)"`） |
| `counts_in_platform_matrix` | **`false`**（固定） |
| `failure_category` | **`external_network`**（固定，**非代码逻辑**） |
| `failure_reason` | `"外网 PMC/PastDB/ENA 依赖不可达，非代码缺陷；不据此判定 validation job 失败"` |
| `collection_state` | **`materials_ready_pending_remote`** |
| `collection_method` | `"GitHub Actions REST API artifact download（主理人执行）"` |
| `artifacts` | `[]`（目标：`audit_public_splicing_evidence` 产物 / 外网失败原因记录，**待远端回收**） |
| `status_transition` | `"pending"`（`c79034e` 基线为外部网络类失败；**不参与** failure → success 的 validation 叙事） |

## 归类强制口径

- 该失败是 **外部网络类（PMC / PastDB / ENA 不可达）**，**非代码逻辑**。
- **不并入三平台 `validation` 矩阵结论**（`counts_in_platform_matrix = false`）。
- **不据此判定 validation job 失败**、**不混入「CI 转 success」结论**（PRD §8 第 5 条 / Q13）。
- 三平台 `failure_reason` 字段**不得**写入外网类原因；外网类原因一律只写在本文件。
- 三平台 `validation` 转 success 属 **R2 远端实跑的目标态，不计入本机 A 类闭环**。

> 结果真实回收前**不得写 pass / success**。
