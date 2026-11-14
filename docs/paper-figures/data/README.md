# Figure S1 冻结 JSON（R8）— 来源、分支与复现

本目录保存 `make_paper_figures.py` 的 `make_figureS1()` 所需的两个机器可读输入，使**干净 clone（无 `target/`）**也能一次跑出 6 组 12 张图。

## 1. 实际采用的分支：**分支 A（按字节拷贝运行时产物 + 追加 `_provenance`）**

设计 §3.2 规定二选一；本次执行时本机 `target/phase3-comparison/{gffutils,biopython}/summary.json` **确实存在**，故按优先级采用分支 A：

- `gffutils-summary.json` ← `target/phase3-comparison/gffutils/summary.json`
- `biopython-summary.json` ← `target/phase3-comparison/biopython/summary.json`

**所有业务字段（含 3 轮 `trials`、`source_gold_tuple_count=8`、各 `median_*_ms`、`cases[3].products[6].status="MATCH"`）逐字段原样保留，未改动任何一个实测数值、未重跑任何计时。** 仅追加：

1. 顶层 `_provenance` 块（字段见下）；
2. 设计 §3.2 两表的两个描述性契约字段：`note`（两文件）与顶层 `all_gold_products_match=true`（Biopython，由 3 个 `cases[].all_gold_products_match` 均为 `true` 直接聚合，`docs/fcs-phase3-comparison-plan.md` §3.2 亦明确「3 物种全部 `all_gold_products_match=true`」）。运行时原文用 `interpretation` 表达同义说明，此处保留未删。

`gffutils-summary.json` 的 `median_phytoscope_full_import_ms=2670.7585` / `median_gffutils_full_index_build_ms=57801.95119999917` 是运行时 JSON 的**精确实测值**；`docs/fcs-phase3-comparison-plan.md` §3.1 表格将其四舍五入记为 2,670.76 / 57,801.95，二者一致，本目录不改写为四舍五入值。

> 若未来在干净 clone（无 `target/phase3-comparison/`）上重建，按设计 §3.2 **分支 B**：从 `docs/fcs-phase3-comparison-plan.md` §3.1/§3.2 已记录的实测数值构造最小 JSON，并在 `_provenance` 中把 `source_kind` 改为 `transcribed_from_committed_report`、为每个数值标注 `transcribed_from`；同样须保持 `not_new_measurement=true` 与 `measurement_class="historical_measured_not_new"`，**禁止新测、禁止虚构任何计时**。本次未使用分支 B。

## 2. `非新测` 声明（强制）

两个 JSON 的 `_provenance` 均含：

- `not_new_measurement: true`
- `measurement_class: "historical_measured_not_new"`
- `source_document: "docs/fcs-phase3-comparison-plan.md"`
- `measured_on_head: "247ff11"`（既有报告记录的仓库 HEAD）
- `statement: "非新测；仅冻结既有实机基线，未重跑计时"`

即：这是**既有实机基线的冻结副本**，不是本次新测结果；不得引用为「当前版本实测」。三工具（JBrowse2 / IGV / TBtools）的定性对照与「非本机实测」措辞由 `docs/fcs-tool-comparison-final.md`（B03）定稿，本目录只标「非新测」。

## 3. 生成脚本名与生成方式

| 冻结 JSON | 原始运行时产物 | 产生该产物的脚本（`_provenance.generator_scripts`） |
|---|---|---|
| `gffutils-summary.json` | `target/phase3-comparison/gffutils/summary.json` | `tools/compare_public_case.py` |
| `biopython-summary.json` | `target/phase3-comparison/biopython/summary.json` | `tools/compare_biopython_seqio.py` |

冻结动作本身（本次）是一次性「读取运行时 JSON → 追加 `_provenance`/契约字段 → `json.dumps(ensure_ascii=False, indent=2)` 写出 → `sha256sum` 落 `SHA256SUMS.txt`」，**不是**新的测量脚本，也不修改上述两个 `tools/*.py`。

`_provenance` 公共字段：`artifact_kind="frozen_phase3_summary"`、`source_kind="runtime_copy"`、`source_sections`（`["§3.1"]` / `["§3.2"]`）、`original_runtime_path`、`measured_at="2026-11-14"`、`measured_on_head="247ff11"`、`measurement_class`、`not_new_measurement`、`frozen_at`（本次冻结 UTC 时刻）、`generator_scripts`、`sha256_record="docs/paper-figures/data/SHA256SUMS.txt"`、`statement`。

## 4. 校验和

`SHA256SUMS.txt` 两行分别对应两个 JSON，**JSON 内不自指哈希**（只记 `sha256_record` 路径）：

```sh
cd docs/paper-figures/data && sha256sum -c SHA256SUMS.txt && cd ../../..
```

两个 JSON 与 `SHA256SUMS.txt` 均以 **LF** 换行、UTF-8 写出（仓库 `.gitattributes` 为 `* text=auto eol=lf`）。Windows 下 `Path.write_text()` 默认会写成 CRLF，会使 `sha256sum -c` 把文件名误读成 `*.json\r`；因此冻结时统一用 LF 写字节并据此计算校验和，保证跨平台 `sha256sum -c` 可用。**未改动任何业务数值**，仅统一换行。

## 5. 复现命令（干净 clone 语义）

```sh
# 从仓库根目录执行；删除所有运行时产物后，仅用本目录的冻结 JSON 重生成 6 组 12 文件
rm -rf target
python docs/paper-figures/make_paper_figures.py
python -c "import pathlib; d=pathlib.Path('docs/paper-figures'); fs=[f'{n}.{e}' for n in ['Figure1','Figure2','Figure3','Figure4','Figure5','FigureS1'] for e in ['png','svg']]; miss=[f for f in fs if not (d/f).is_file()]; print('missing=',miss); assert not miss, miss"
cd docs/paper-figures/data && sha256sum -c SHA256SUMS.txt && cd ../../..
grep -n "target.*phase3-comparison" docs/paper-figures/make_paper_figures.py || echo "NO_RUNTIME_PATH_OK"
```

`make_paper_figures.py` 的 `GFFUTILS_SUMMARY` / `BIOPYTHON_SUMMARY` 现已指向本目录（B02），脚本中不再出现 `target/phase3-comparison` 运行时路径。绘图逻辑与数值一律未改；图文件只由脚本重生成，不手工编辑。

## 6. 边界

- 这两个 JSON 是**仓库内可提交数据源**（与 `target/` 运行时产物不同）；按设计 §7.6，gold-set / bedtools / BLASTP 的 JSON 仍只能作为 CI artifact 由主理人回收，不得当作仓库内数据源。
- 本目录不新增任何第三方依赖、不改 `pom.xml`、不重跑 gffutils / Biopython 计时。
