# ubuntu-latest / validation · job-summary（骨架，B 类）

> 批次：B03 · 目录契约见 `docs/ci-reports/v0.3.0/README.md` §2、设计 §3.1。
> **状态：`not_collected`** · `collection_state = materials_ready_pending_remote` · 结果**待远端回收**。
> 回收执行主体：主理人（GitHub Actions REST API artifact download）。本机不生成、不伪造任何报告内容。

| 字段 | 初值 |
|---|---|
| `status` | **`not_collected`** |
| `baseline_status` | `"failure"`（`c79034e`，固定） |
| `status_transition` | `"pending"`（回收后填 `failure → success` / `failure → failure`） |
| `tests` | `{run: null, failures: null, errors: null, skipped: null}` |
| `skipped_before` | `12`（全局基线） |
| `skipped_after` | `null`（须 < 12 且 `ResearchModuleGoldSetTest` 非 skipped） |
| `goldset_executed` | `null` |
| `goldset_json_present` | `false` |
| `python_consumers` | `{bedtools: "pending", blastp: "pending"}` |
| `artifacts` | `[]` |
| `failure_reason` | `""`（只写代码类原因；外网类归入 `public-data-validation`） |

## 待回收产物清单（Linux 专属，当前全部为空）

| 文件 | 状态 |
|---|---|
| `surefire-reports/`（Skipped 须体现 `ResearchModuleGoldSetTest` 非 skipped） | **待远端回收**（目录占位 `.gitkeep`） |
| `research-module-gold-set.json` | **待远端回收** |
| `bedtools-promoter-comparison.json` | **待远端回收** |
| `blastp-homology-comparison.json` | **待远端回收** |

## 基线失败项（该平台，抄录 `c79034e-baseline.md`）

- `check_isoform_case.py`（`validate.yml:48`）`FileNotFoundError: src/main/resources/data/species/arabidopsis/TAIR10_genomic.fna.gz` → **代码类**，B01 已修。
- 连带：金标准门控未传参 → `Skipped: 1`、gold-set JSON 不产出、bedtools/BLASTP 失去输入 → **代码类**，B01 已修。

> 三平台 `validation` 转 success 属 **R2 远端实跑的目标态，不计入本机 A 类闭环**；
> 结果真实回收前**不得写 pass**。
