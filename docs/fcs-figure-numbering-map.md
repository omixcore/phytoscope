# PhytoScope FCS · 论文图号对照表（唯一权威）

> **状态**：`final`。本文件是论文 Figure 1–5 + Figure S1 的**唯一权威编号定义**（设计 §3.4 / §7.1）。
> 其余文档（`fcs-paper-outline.md`、`fcs-paper-manuscript-draft.md`、`prd-fcs-submission.md`、
> `fcs-submission-gap-audit.md`、`fcs-phase3-comparison-plan.md`、`paper-figures/README.md`、
> `make_paper_figures.py`）**只引用本表，不另立编号**。
> 图文件命名以 `docs/paper-figures/README.md`「Numbering note」为图像文件的命名权威。
> 批次：B03 · 执行人：工程师寇豆码（Kou）· 日期：2026-11-14（ISO-8601 UTC 口径同设计 §7.10）。
> 权威规格：`docs/design-fcs-final-delivery.md` §3.4、§5 B03、§7.1；增量 PRD R9。

**最终编号固定**：`Figure1` 架构 / `Figure2` 数据模型与输入格式 / `Figure3` 七模块金标准 /
`Figure4` 跨物种验证 / `Figure5` 可复现性 / `FigureS1` 工具对比（补充图）。

---

## 1. 字段定义

| 字段 | 含义 |
|---|---|
| `figure_id` | 唯一编号，取值 `Figure1`–`Figure5` / `FigureS1` |
| `title` | 图题（与图文件 caption 一致） |
| `content` | 该图实际展示的内容 |
| `data_sources` | 数据源（必须是仓库内可提交文件 / 已冻结产物，不得指向 `target/` 运行时产物） |
| `generator` | 生成脚本 + 函数 |
| `outputs` | 产出文件（`docs/paper-figures/` 下 PNG + SVG） |
| `references` | 各文档引用位置 |
| `old_mismatch` | 修订前的错位表述 |
| `status` | 固定 `final` |

---

## 2. 对照表（6 行，唯一权威）

| `figure_id` | `title` | `content` | `data_sources` | `generator` | `outputs` | `references` | `old_mismatch` | `status` |
|---|---|---|---|---|---|---|---|---|
| `Figure1` | 架构与本地管线（schematic） | 一条本地管线：Import → Health-check → Model → Browse/Query → Research modules → Export；六示例； health-check 布局 | `src/main/java/**` 类名、`docs/input-format-specification.md` | `docs/paper-figures/make_paper_figures.py :: make_figure1()` | `Figure1.png`、`Figure1.svg` | outline §3.1/§4、manuscript §3.1、`prd-fcs-submission.md` §4.1 图1 | 无 | `final` |
| `Figure2` | **数据模型与输入格式** | (a) GFF3/GTF/GFF2 → 统一模型；(b) 坐标/值约定（1-based closed GFF3 vs 0-based half-open BED、正负链、measured-zero vs missing）；(c) 真实水稻豆 `LOC124833352` 负链位点的 GFF3-vs-GTF `stop_codon` 边界 | vigna `manifest.json`、`docs/input-format-specification.md` | `make_figure2()` | `Figure2.png`、`Figure2.svg` | outline §3.2(b)/§4、manuscript §3.2、`fcs-submission-gap-audit.md` #1 | outline §3.2「Figures/tables」旧把 Figure 2 标为 cross-species gold；`prd-fcs-submission.md` §4.1 旧图2 标题「跨物种可移植性」 | `final` |
| `Figure3` | 七模块金标准一致性 | (a) 模块 vs 独立基线一致性（promoter 8/8 bedtools、homology 20/20 BLASTP、domain 16/16、sequence QC 4/4、expression grouping）；(b) RBCS + CYP71A 同源分类矩阵 | `docs/research-module-validation.md`、`docs/chromosome-validation.md` | `make_figure3()` | `Figure3.png`、`Figure3.svg` | outline §3.2/§3.3/§4、manuscript §3.3 | 无 | `final` |
| `Figure4` | **跨物种验证与正确性** | (a) 4 模式（Ensembl r62）+ 3 非模式（RefSeq）导入基因数；(b) CDS 长度 vs 蛋白 aa；(c) byte-exact 一致性网格 + 25 条源推导编码转录本 | `docs/chromosome-validation.md`、`docs/non-model-portability-evidence.md`、`src/test/resources/non-model/additional-nonmodel-manifest.json`、`multi-locus-sequence-gold.tsv` | `make_figure4()` | `Figure4.png`、`Figure4.svg` | outline §3.2/§3.3/§4、manuscript §3.2、`fcs-submission-gap-audit.md` #19/#20 | outline §3.2/§3.3 旧把 Figure 4 标为 comparison / fair comparison；`prd-fcs-submission.md` §4.1 旧图4 标题「软件对比与功能差异」 | `final` |
| `Figure5` | 可复现性（provenance + 自包含项目） | (a) manifest v2 / schemaVersion 4 的逐文件 SHA-256、别名、溯源；(b) save → reopen → verify 往返，checksum 失配中止；(c) 独立双镜像数据仓库 + `sha256sum -c` 9/9 OK | `docs/portability-and-reproducibility.md`、`docs/design-slim-bundled-data.md` §2.1（9 文件 / 68,518,847 B，Panel c 实数据） | `make_figure5()` | `Figure5.png`、`Figure5.svg` | outline §3.5/§4、manuscript §3.5 | `make_paper_figures.py` docstring `:20-22` 与注释 `:607` 误把 Figure S1 的两个 JSON 数据源标为 Figure 5（B02 已修正） | `final` |
| `FigureS1` | 工具对比（**补充图**） | (a) 定量：gffutils 0.13 vs PhytoScope 结构元组一致（8/8、3 轮交替；冷导入/建索引分别报告、不作速度优劣主张）；Biopython 1.88 SeqIO vs PhytoScope 序列/链向逐字节一致（3 物种 × 6 产物）；(b) 定性能力矩阵（PhytoScope vs JBrowse2/IGV/TBtools），无优劣主张 | **`docs/paper-figures/data/gffutils-summary.json`、`docs/paper-figures/data/biopython-summary.json`**（B02 冻结，非新测，`measurement_class=historical_measured_not_new`）+ `docs/fcs-phase3-comparison-plan.md` §4.1 定性矩阵 | `make_figureS1()` | `FigureS1.png`、`FigureS1.svg` | outline §4（Figure S1 行）、manuscript §3.4、`fcs-tool-comparison-final.md`、`fcs-phase3-comparison-plan.md` §4 | `make_paper_figures.py` docstring `:20-22` 错标 Figure 5；`:607` 注释旧标 Figure 5；outline §3.3 旧把工具对比归入 Figure 4 | `final` |

> **数据源硬约束（设计 §7.6）**：Figure S1 的两个 summary JSON 已于 B02 冻结到
> `docs/paper-figures/data/`，并附 `_provenance` 与 `SHA256SUMS.txt`。任何文档都**不得**再把
> `target/phase3-comparison/{gffutils,biopython}/summary.json` 写成 Figure S1 的数据源——那是
> `target/` 运行时产物，干净 clone 下不存在。

---

## 3. 引用修改映射（B03 已执行）

| 文档 / 位置 | 修订前 | 修订后 | 说明 |
|---|---|---|---|
| `docs/fcs-paper-outline.md` §3.2「Figures/tables」 | Figure 2 (cross-species gold)、Figure 4 (comparison) | Figure 2 数据模型与输入格式 / Figure 3 七模块 / Figure 4 跨物种验证 / Figure S1 工具对比 | 只改图号引用，不重排正文 |
| `docs/fcs-paper-outline.md` §3.3「Figures/tables」 | Figure 2, 3, 4, 5 + Fair comparison 小节 | 按本表对齐；工具对比改引 Figure S1 | 同上 |
| `docs/fcs-paper-outline.md` §4 Figure S1 行 Data source | `target/phase3-comparison/*.json` | `docs/paper-figures/data/*.json` | 消除悬空运行时路径 |
| `docs/fcs-paper-manuscript-draft.md` §3.2 标题 | Cross-species portability … (Figure 2) | Standard data model and input formats (Figure 2); cross-species validation (Figure 4) | 跨物种内容改引 Figure 4 |
| `docs/fcs-paper-manuscript-draft.md` §3.4 标题 / 图引用 | Fair comparison with existing tools (Figure 4) | Tool comparison with existing tools (Figure S1) | 工具对比属补充图；注明三工具定性非实测 |
| `docs/fcs-paper-manuscript-draft.md` §3.1 / §3.3 / §3.5 | Figure 1 / 3 / 5 | 保持不变 | 已正确 |
| `docs/prd-fcs-submission.md` §4 | 图2「跨物种可移植性」/ 图4「软件对比与功能差异」 | **只加修订说明，不重排旧编号**（设计 §7.1 / Q5） | 旧编号体系保留，用一条修订说明指向本表 |
| `docs/fcs-submission-gap-audit.md` 图表述处 | #1 / #19 / #20 / #27 等散落表述 | 按本表对齐的引用说明 | 不改过时状态值，仅按结构追加 |
| `docs/paper-figures/README.md` | 「Residual mismatch to flag」 | B02 已标记「已解决」并指向本文件 | 本文件即为该悬空引用的落点 |

---

## 4. 一致性说明

- `docs/paper-figures/README.md`「Numbering note」只负责**图文件命名**（`Figure{1..5}.png|svg`、
  `FigureS1.png|svg`）；本文件负责**编号语义与跨文档引用**。二者不冲突。
- 6 组 12 个 PNG/SVG 由 `docs/paper-figures/make_paper_figures.py` 一次生成（B02 已在删除
  `target/` 后验证），`make_figureS1()` 存为 `FigureS1`（正确，脚本 `:929` 未改）。
- 补充图/表（Sup Fig 1–5、Sup Table 1–4）沿用 `docs/fcs-paper-outline.md` §5 编号，**不在本表范围内**；
  本表只定义 5 张主图 + 1 张补充图 Figure S1。
