# public-data-validation（单列）· job-summary（远端实测已回填）

> 目录契约见 `docs/ci-reports/v0.3.0/README.md` §1.1 / §2、设计 §3.1 / 裁定 4 / Q13。
> **本 job 单列，不并入三平台 `validation` 矩阵。**
> **本文件记录的是远端实测结果，本地通过记录不能替代远端结果。**
>
> ## ✅ 修复已生效（2026-11-15 · run `43664785766` / commit `2c3b1e6`）
>
> 本 job 在 commit `2c3b1e6`（dev 当前 tip）上**已转绿**：job id `132661642837`，
> `conclusion = success`，step 5 `Audit public Arabidopsis splicing and proteomics resources`
> **success**（`started_at 2026-11-15T01:48:17Z` / `completed_at 2026-11-15T01:49:32Z`）。
> 修复点：`tools/audit_public_splicing_evidence.py` 的 XLSX 魔数判断由
> `b"PK\\x03\\x04"`（双反斜杠字面量）改为 `b"PK\x03\x04"`（真实字节序列），
> 并给 `fetch_bytes` 加 `allow_self_signed` 仅放行 PastDB 自签证书、把已退役的 PastDB
> 归因明确记为 `source retired`。完整结论见 `README.md` §8。
> **以下 §结论 / §失败原因 记录的是修复前的历史失败（run `43605656726` / `49a5fac` 与
> `43609281554` / `0638dbe`），保留作取证与因果链依据，不得再当作当前状态。**
>
> **归因更正（2026-11-15）**：本 job 的失败曾被笼统归为「外部网络不可达」。经下载
> run `43609281554` 的 `report.json` artifact 逐源复核，实际是**三类不同原因**，不可再统称
> 「外网不可达」：① 脚本 ZIP 魔数判断 bug（`b"PK\\x03\\x04"` 双反斜杠，误判一切合法 XLSX）；
> ② PastDB（pastdb.crg.eu）**数据源已退役**（根路径即 404 占位页，姊妹库 vastdb 仍活）；
> ③ PMC 即时文件服务返回 HTML「Preparing to download」而非 XLSX。详见 §「归因复核」。

## 结论

| 项 | 值 |
|---|---|
| job 名 | `public-data-validation` |
| job id | **`132479612205`** |
| 结论 | **`failure`** |
| `failure_category` | **`external_network`**（历史归类；现更精确为「脚本 bug + 数据源退役 + 网络环境」三类，见 §归因复核） |
| `counts_in_platform_matrix` | **`false`**（固定） |
| started_at (UTC) | `2026-11-14T14:35:29Z` |
| completed_at (UTC) | `2026-11-14T14:36:40Z` |
| 失败步骤 | **step 5 `Audit public Arabidopsis splicing and proteomics resources`** |
| 证据来源 | GitHub Actions run **`43605656726`**（`event=push`、`head_branch=dev`、`run_started_at=2026-11-14T14:35:25Z`、`updated_at=2026-11-14T14:41:50Z`），commit **`49a5fac`**；run 页面 https://github.com/omixcore/phytoscope/actions/runs/43605656726 |

## 失败原因（历史归类，已更正）

- 失败步骤为 **step 5 `Audit public Arabidopsis splicing and proteomics resources`**。
- 脚本退出码 2，触发条件是「No public processed splicing table could be retrieved」——
  即 `processed_available = False`，而非任一具体网络错误。
- **原归类「外网 PMC / PastDB / ENA 依赖不可达」不准确**，已被 §「归因复核」更精确的三层归因取代。

## 字段

| 字段 | 值 |
|---|---|
| `status` | **`"fail"`**（历史记 `"fail (external network)"`，见归因更正） |
| `failure_category` | **`external_network`**（历史值；现更精确为三层归因，见下） |
| `counts_in_platform_matrix` | **`false`**（固定） |
| `failure_reason` | `"audit_public_splicing_evidence.py 无法取得任何公开已处理剪接表（processed_available=false）；根因三类：脚本 XLSX 魔数 bug、PastDB 数据源退役、PMC 即时服务返回 HTML"` |
| `collection_state` | **`remote_run_collected`** |
| `collection_method` | `"GitHub Actions REST API（run + jobs + artifact 查询）"` |
| `artifacts` | 已回收：`report.json` / `candidate-public-splicing-summary.tsv` / `PRJNA1418670-run-metadata.tsv` / `README.md` |
| `status_transition` | **`failure → failure`**（`c79034e` 基线亦失败；**不参与** validation 的 failure → success 叙事） |

## 归因复核（2026-11-15，基于 run `43609281554` 的 report.json 逐源取证）

下载 artifact `public-data-validation`（run `43609281554`，5,417 B）后，`report.json` 的
`sources` 显示四个源**分别**失败于不同原因：

| 源 | 失败现象 | 真实原因 | 是否脚本可修 |
|---|---|---|---|
| `pastdb_table_s1` / `_s3`（PMC） | PMC `bin/...xlsx` 返回 HTML「Preparing to download」页 | PMC 即时文件服务需 JS/重定向，静态抓取拿不到 XLSX | 否（改走 Springer 静态镜像） |
| `pastdb_table_s1` / `_s3`（Springer） | 返回合法 `PK\x03\x04` ZIP，却被判「not an XLSX ZIP payload」 | **脚本 bug**：`b"PK\\x03\\x04"` 双反斜杠，魔数永不匹配 | **是（已修）** |
| `PastDB_downloads` | `SSL: CERTIFICATE_VERIFY_FAILED`（自签证书）→ 修复后又 `404` | 先自签证书（脚本未容忍），后暴露**数据源已退役**：`pastdb.crg.eu` 根路径即 404 占位页 | 证书可修；404 不可修 |
| `ENA_PRJNA1418670` | 本机代理 `502 Bad Gateway`；CI 直连正常 | 本机网络代理干扰，非代码、非数据源 | 否（环境） |
| `Zhang2019_Figshare` | `SSL: UNEXPECTED_EOF` / 本机 `403`（WAF） | figshare API 从当前网络被 WAF 拦截 | 否（环境） |

**脚本 bug 已修复并本地验证**：`fetch_supplement` 的魔数判断改为 `b"PK\x03\x04"` 后，
Springer 静态镜像的两个 XLSX 均可正常解析，`pastdb_table_s3` 现解析出 **10 个候选行**
（修复前为 0），脚本正常退出（exit 0，不再触发「No public processed splicing table」）。
PastDB 数据源退役不可通过修脚本解决，已在该源记录中如实标注「source retired」。

**结论**：本 job 的失败**不是单纯的「外部网络抖动」**，而是「脚本解析 bug（已修）+
一个数据源退役（不可修）+ 本机/CI 网络环境差异」的组合。文档此前一律写「外部网络不可达」
属**过度简化、归因不准**，现统一更正为上述三层表述。

## 归类强制口径

- 该 job **仍不并入三平台 `validation` 矩阵**（`counts_in_platform_matrix = false`）。
- **仍不据此判定 validation job 失败**、**不混入「CI 转 success」结论**（PRD §8 第 5 条 / Q13）。
- **但**不得再以「外部网络抖动」一笔带过：脚本 bug（已修）与数据源退役（不可修）是**可查证的具体原因**，
  须如实记录。三平台 `failure_reason` 字段**不得**写入本 job 的失败原因。
- 不得把本 job 的失败与 ubuntu 的 FX 无头超时失败混为一谈：**两者性质不同**。

---

## 重跑确认（run `43609281554` / commit `0638dbe`）

| 项 | 值 |
|---|---|
| job id | **`132490554373`** |
| 结论 | **`failure`** |
| started_at (UTC) | `2026-11-14T15:16:31Z` |
| completed_at (UTC) | `2026-11-14T15:17:14Z` |
| 失败步骤 | **step 5 `Audit public Arabidopsis splicing and proteomics resources`** |

> 该次运行的 `report.json` 已回收并逐源复核（见 §归因复核）。结论：**失败模式与
> run `43605656726` 相同**，根因是「脚本 XLSX 魔数 bug + PastDB 数据源退役 + 网络环境」，
> 而非单纯外部网络抖动。脚本 bug 已在本地修复并验证（`pastdb_table_s3` 解析出 10 行、exit 0）；
> PastDB 退役不可修。全量记录见 `docs/ci-reports/v0.3.0/README.md` §7。
