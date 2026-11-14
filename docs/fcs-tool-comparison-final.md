# PhytoScope FCS · 三工具定性对照定稿（R6，B 类）

> 批次：B03 · 执行人：工程师寇豆码（Kou）· 日期：2026-11-14（ISO-8601 UTC 口径同设计 §7.10）
> 权威规格：`docs/design-fcs-final-delivery.md` §5 B03 #4、§0.2 裁定 5、§7.9；增量 PRD R6。
> 上游底稿：`docs/fcs-phase3-comparison-plan.md` §4（共同任务矩阵、功能差异）、
> `docs/validation-and-benchmarks.md`（五任务用户任务协议）、`docs/prd-fcs-submission.md` §3.1。

---

## 1. 定稿声明（固定标注，不得删改）

**JBrowse 2 / IGV / TBtools 三工具行固定标注「定性对照（基于公开文档，非本机实测）」。**

- **JBrowse 2 未安装**（`fcs-phase3-comparison-plan.md` §1 环境侦察：`which jbrowse` 无结果）。
- **IGV 2.19.3 / TBtools-II 为桌面 GUI，本环境未脚本化**，无可复现的无头快照或计时。
- 三工具**未在本机实测**、**无计时数字**、**不冒充实测**。
- **不作「更快 / 更好 / 更易用」主张**，不作省时、发现率、可用性优劣比较。
- **gffutils 0.13 / Biopython 1.88 只引用既有实机报告**（`fcs-phase3-comparison-plan.md` §3.1/§3.2、
  冻结于 `docs/paper-figures/data/*.json`），**本批不重跑计时、不重造数字**；冻结数据
  `measurement_class = historical_measured_not_new`、`not_new_measurement = true`。

> R6 状态：**B 类（只交物料/协议，需作者执行）** —— 本文件是定稿标注 + 共同任务矩阵 + 功能差异单列，
> 外部实机补充（A8）**未执行**，见 `docs/fcs-tool-comparison-external.md`（空白协议）。

---

## 2. 共同任务矩阵（只比共同支持的任务，不比优劣）

> 完全承接 `docs/fcs-phase3-comparison-plan.md` §4.1 的既有定性矩阵，**不新增任何实测结论**。

| 任务（来自 `docs/validation-and-benchmarks.md` 五任务协议） | PhytoScope | IGV 2.19.3 | JBrowse 2 | TBtools-II |
|---|---|---|---|---|
| 按基因 ID 定位（不同序列上的多个基因座） | ✅ 本地搜索 + 多序列切换 | ✅ 按基因名/坐标跳转（需索引或 `.genome`） | ✅ 文本搜索/坐标跳转（需 text-index） | ⚠️ 以工具为单位，非连续浏览器导航 |
| 查看异构体（exon/CDS/UTR 坐标） | ✅ 结构轨道 + 详情面板 | ✅ 结构轨道（缩放查看） | ✅ 结构轨道 | ✅ Gene Structure View（GFF3 绘图，非连续浏览器） |
| 关联表达（样本值 + 样本顺序） | ✅ 导入表达矩阵并精确 ID 对齐 | ⚠️ 以 bigWig/覆盖度轨道呈现，无「样本矩阵」原生概念 | ⚠️ 定量轨道（bigWig 等），无矩阵原生概念 | ✅ 热图（样本矩阵） |
| 生成对比图（结构/表达，可编辑、单位/共享刻度正确） | ✅ 内置 ComparisonFigure + PNG/SVG/TSV 导出 | ⚠️ 截图/会话，无「多基因座对齐对比图」原生视图 | ⚠️ 多视图并排，无原生对齐对比图 | ⚠️ 各可视化独立导出 |
| 恢复分析（保存/重开项目状态） | ✅ 项目保存/重开 | ✅ 会话 `.xml` 保存/加载 | ✅ 会话 URL/JSON 共享 | ⚠️ 无统一项目会话概念 |
| 无门户外物种（非模式）导入 | ✅ 通用 GFF3/GTF + FASTA + 矩阵 | ✅ 通用格式（需本地 FASTA/GFF + 索引） | ✅ 通用格式（需配置/插件） | ✅ 通用格式（各功能各自导入） |
| 头less/可复现导出 | ✅ `--gff3/--example/--loci/--out/--format` | ⚠️ 批处理 `-b script` 可截图，仍需图形环境 | ⚠️ CLI 生成配置，渲染在浏览器 | ⚠️ GUI 为主 |

图例：✅ 原生支持；⚠️ 需额外配置/外部工具/非原生。

> **三工具行结论**：均为「定性对照（基于公开文档，非本机实测）」。矩阵中的 ✅/⚠️ 描述的是
> **公开文档所述的原生能力有无**，不是实机计时、不是性能或易用性评分、不排名。

---

## 3. 功能差异（单独列出，不混入共同任务、不排名）

承接 `fcs-phase3-comparison-plan.md` §4.2，原样保留，不新增优劣判断：

- **部署形态**：PhytoScope = 单机 JavaFX 桌面 + 头less CLI；IGV = Java 桌面/Web；JBrowse2 = 浏览器（需静态托管）；TBtools = Java 桌面工具箱。
- **数据范围**：PhytoScope 聚焦「基因结构浏览 + 表达关联 + 对比图 + 序列导出 + 同源/结构域/启动子研究模块」；IGV/JBrowse2 是通用基因组浏览器（BAM/VCF/bigWig/互作等，超出 PhytoScope 范围）；TBtools 是数十种工具的集合（范围不同）。
- **交互范式**：IGV/JBrowse2 是连续坐标缩放导航；PhytoScope 是「基因座为中心的离散选择 + 并排对比」；TBtools 是离散工具入口。
- **序列产品导出**：PhytoScope 直接导出基因组区间/转录本/CDS/蛋白/推断 UTR；IGV/JBrowse2 导出参考 FASTA 子串，转录本/CDS/蛋白拼接需外部脚本；TBtools 有 Fasta/GFF 工具可近似完成（需多步）。
- **表达数据模型**：PhytoScope 原生「样本×基因矩阵 + 单位/样本顺序/缺失≠0」契约；IGV/JBrowse2 用定量轨道（bigWig），不原生持有样本矩阵语义；TBtools 热图持有矩阵。

**说明**：以上为定性描述，基于公开文档与工具常识，**非本机实测**；不据此作「易用性/省时/发现率」任何主张。

---

## 4. 既有实机基线（只引用，不重造）

| 基线 | 既有报告 | 本批处理 |
|---|---|---|
| gffutils 0.13 结构导入 | `docs/fcs-phase3-comparison-plan.md` §3.1；冻结 `docs/paper-figures/data/gffutils-summary.json` | 只引用，`not_new_measurement=true`，**未重跑计时** |
| Biopython 1.88 SeqIO 序列重建 | `docs/fcs-phase3-comparison-plan.md` §3.2；冻结 `docs/paper-figures/data/biopython-summary.json` | 只引用，**未重跑计时** |
| bedtools 2.31.1 / BLASTP | CI Linux runner 既有 8/8、20/20 对比；`docs/research-module-validation.md` | 只引用，本机无 bedtools，不重复 |

> 冻结 JSON 的 SHA-256 与来源声明见 `docs/paper-figures/data/SHA256SUMS.txt`、
> `docs/paper-figures/data/README.md`。**Figure S1 工具对比图的编号权威见
> `docs/fcs-figure-numbering-map.md`**（FigureS1，补充图）。

---

## 5. 未执行 / 待作者动作（B 类如实标注）

- **外部实机补充对照（A8）未执行**：需带显示环境的机器安装 JBrowse2 / IGV / TBtools 并做同任务实机，
  协议见 `docs/fcs-tool-comparison-external.md`（空白、显式「未执行」）。
- **用户研究未执行**：见 `docs/fcs-user-study-results.md` 与 manuscript §4.2/§4.4「future work」。
- 本文件**不把定性对照写成实机计时**、**不写「已完成三工具同任务对照实测」**。
