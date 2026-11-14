# PhytoScope 投稿增量 PRD（FCS Code & Data Letter）

- **语言**：中文（与需求一致）
- **技术栈**：Java 21 + JavaFX + Maven（既有仓库，不新增技术栈）
- **项目名**：`phytoscope`（参考数据仓库 `phytoscope-reference-data`）
- **原始需求复述**：将 PhytoScope 投稿至 Frontiers of Computer Science（FCS），优先投稿类型 Code & Data Letter，备选 Software Research Article。核心定位为「通用、跨物种、可复现的植物基因组分析软件」。基本原则：**不为了凑功能而增加算法，不为了凑图而强行做实验**——重点补齐技术证据、实验验证与论文材料，而非新增功能。

---

## 1. 产品目标与定位

**一段话定位**：PhytoScope 是一个本地运行的 JavaFX 桌面端植物基因结构浏览器，面向「无需专属物种门户、无需上传数据、结果可溯源」的植物研究者，让用户以统一的导入 → 浏览 → 查询 → 比较 → 导出管线，跨物种（从模式作物到门户缺乏的非模式植物）完成基因结构查看、表达矩阵挂载、序列提取、蛋白质相似性与结构域映射等分析，并以 SHA-256 溯源与版本化 Manifest 保证结果可复现。

**投稿类型选择依据（为何 Code & Data Letter 优先）**：

1. **成果类型契合**：PhytoScope 的贡献是「可复用的软件 + 可追溯的数据 + 独立交叉验证证据」，而非「新算法」或「新生物学发现」。FCS 的 Code & Data Letter 正是面向软件/数据工具的短文类型，匹配度最高。
2. **篇幅与审稿成本**：Code & Data Letter 篇幅短、聚焦「软件是什么 + 正确性与可复现性证据 + 数据可用性」，与本项目"不新增算法、只补证据"的边界完全一致；无需虚构算法创新即可成立。
3. **升级路径**：Software Research Article 要求更深的方法学/算法贡献与更完整的对比+用户研究；当前 JBrowse2/IGV/TBtools 对比与真实用户研究尚未执行，故作为备选——若第四批对比与用户研究补齐，可再评估升级投稿类型。

**三条核心主张（卖点，均有仓库证据支撑）**：

| # | 主张 | 一句话 | 支撑文档 |
|---|---|---|---|
| 1 | **通用性（species-agnostic portability）** | 同一管线零物种特定代码，模式作物到门户缺乏的非模式植物皆可复现 | `non-model-portability-evidence.md`、`chromosome-validation.md` |
| 2 | **正确性（独立交叉验证）** | 结构/序列/翻译/模块输出与 gffutils、BLASTP、bedtools、Salmon 等独立工具逐位一致 | `validation-and-benchmarks.md`、`research-module-validation.md` |
| 3 | **可复现性（traceable provenance）** | SHA-256 校验、版本化 Manifest、双平台镜像数据仓库、可移植项目文件 | `portability-and-reproducibility.md`、`phytoscope-reference-data/MANIFEST.json` |

> 注：不把「竞争力/性能优越」列为核心卖点——当前对比实验未完成，历史性能数字为 runner-specific，避免未测先断。

---

## 2. 需求池（逐项转可执行条目）

**状态图例**：
- **✅ 已有沉淀** = 仓库已有实现/文档/测试，只需盘点确认 + 补少量缺口
- **🔶 有缺口** = 部分已有、需补齐关键环节
- **🆕 需新增** = 尚未开展，需新增实测/产出

**优先级**：P0 = 投稿前必须；P1 = 重要（影响论文质量但不阻塞投稿）；P2 = 可选。

### 第一阶段·软件规范与发布

| ID | 需求条目 | 状态 | 验收口径（怎么算"完成"） | 数据来源 | 优先级 | 依赖 |
|---|---|---|---|---|---|---|
| R1.1 | GFF3/GTF/GFF2 支持范围与解析规则 | ✅ 已有（GFF2 需确认） | `input-format-specification.md` 已定义 GFF3/GTF 规则；补一句 GFF2 支持范围与「GTF/GFF2 走既有 importer」的明确边界；不新增解析器 | 既有规范 + 既有 importer 代码 | P0 | 无 |
| R1.2 | FASTA 规范与染色体 ID 匹配 | ✅ 已有 | 规范已定义（首个空白前非空标识、case/原名保留、重复 ID 拒绝）；验收=抽查文档与 `GeneSequenceService.normalizeSequenceName`/别名逻辑一致 | `input-format-specification.md`、`GeneSequenceService` | P0 | R1.1 |
| R1.3 | 表达矩阵 TSV/CSV 标准与元数据 | ✅ 已有 | 规范已定义（UTF-8 TSV 模板、CSV/分号兼容、`#unit`/`#level` 元数据、零 vs 缺失区分）；验收=文档与 `ExpressionDataService` 行为一致 | `input-format-specification.md`、`ExpressionDataService` | P0 | R1.1 |
| R1.4 | Gene/Transcript/Exon/CDS/UTR 数据模型统一 | ✅ 已有 | 模型已统一（共享父边、多父、不连续 feature、无 exon 的 CDS/UTR 单独保留不虚构）；验收=抽查 `GenomeDataService` 与结构清单一致 | 既有数据模型 + `validate_structure_inventory.py` | P0 | R1.1 |
| R1.5 | 坐标体系/正负链/缺失值/重复 ID 处理规范 | ✅ 已有 | 规范已定义（GFF3 1-based closed、BED 0-based half-open 显式转换、正负链、缺失 vs 零、重复 ID 拒绝）；验收=文档成文 + 相关测试通过 | `input-format-specification.md`、D1-D6 测试 | P0 | R1.4 |
| R1.6 | 数据格式版本与兼容性管理机制 | ✅ 已有 | Manifest `schemaVersion` 1-4 兼容读、`applicationVersion` 记录、旧 schema 缺失字段记为 unknown；验收=project 测试覆盖 schemaVersion 1-4 | `project/ProjectStore`、project 测试 | P0 | R1.4 |
| R2.1 | 完善 phytoscope-reference-data 独立仓库 | ✅ 已有 | 独立仓库已建、9 文件已归档、双远端已推送（同一提交 `acac2cf`）；验收=README/MANIFEST 状态已回填且 `sha256sum -c` 9/9 OK | `phytoscope-reference-data/` | P0 | 无 |
| R2.2 | 补齐远端/来源/版本/SHA-256 | ✅ 已有（许可待复核） | MANIFEST.json 9 条含来源/版本/SHA-256；**缺口**：4 条 `license_status: needs_confirmation` 需逐条复核后在 LICENSES.md 定稿 | `MANIFEST.json`、`LICENSES.md` | P0 | R2.1 |
| R2.3 | 统一开发分支/正式版本/论文功能描述 | 🔶 有缺口 | 分支已统一（仅 `dev`）；**缺口**：主仓库 README 仍写「remote URLs pending user provision」而 reference-data README 已确认推送，需统一两仓库表述 + 冻结论文所描述的功能集与版本号 | README 两侧、git branch/tag | P0 | R2.1 |
| R2.4 | 重建 Windows 安装包并核对 | 🔶 有缺口 | 重新 `package_desktop.py` 生成瘦身版 ZIP（已测 44.01 MiB）→ 发布为 GitHub Release 附件 → 更新 SHA256SUMS.txt；**缺口**：当前发布页仍是 110 MB 历史 v0.2.0 包 | `desktop-distribution.md`、`desktop-verification.md` | P0 | R2.3 |
| R2.5 | 验证 Linux/Windows/macOS 安装启动 | 🔶 有缺口 | CI 三平台构建通过，但 Monocle 不覆盖原生对话框/DPI；**缺口**：在三平台真机各录一次「启动 → 导入 → 浏览 → 导出 → 存/读项目」手动清单 | `desktop-verification.md` 手动清单 | P0 | R2.4 |
| R2.6 | 正式版本标签和 DOI（Zenodo） | 🆕 需新增 | 冻结投稿版本 → 打 tag（建议 `v1.0.0` 或保留 `v0.2.0`+patch 版本，待拍板）→ 经 Zenodo 归档软件+数据两仓库，取得 DOI 并回填 README/CITATION | GitHub/gitee Releases、Zenodo | P0 | R2.3, R2.4 |
| R3.1 | GFF3/GTF/GFF2 格式测试集 | 🔶 有缺口 | GFF3/GTF 已有 GoldenRegressionTest/四物种 fixture；**缺口**：为 GFF2 补一组专用最小 fixture（含 CDS phase、转录本边界）确认 importer 覆盖 | `src/test/resources`、四物种 fixture | P0 | R1.1 |
| R3.2 | 异常注释/缺失字段/重复ID/错误坐标测试 | ✅ 已有 | `Gff3ParserEdgeCaseTest` + D6 边界/越界 + intake health-check 已覆盖；验收=盘点测试清单确认真实覆盖 | 既有测试 + intake fixture | P0 | R1.5 |
| R3.3 | 染色体命名和正负链测试 | ✅ 已有 | `AdaptiveChromosomeTest`/`SpeciesAgnosticTest`/负链 fixture 已覆盖；验收=盘点确认 | `chromosome-validation.md` | P0 | R1.5 |
| R3.4 | 表达矩阵缺失值/零值/ID 不匹配测试 | ✅ 已有 | `ExpressionDelimiterTest`/D 系列/`AdditionalNonModelReferenceTest` 已覆盖零 vs 缺失、重复、ID 对齐；验收=盘点确认 | 既有测试 | P0 | R1.3 |
| R3.5 | 项目保存/恢复/损坏文件/异常退出测试 | ✅ 已有 | project 测试覆盖 save→replace→restore、tamper 拒绝、checksum 失配 abort、异常退出保留旧数据；验收=盘点确认 | `project/ProjectStore` 测试 | P0 | R1.6 |

### 第二阶段·跨物种验证

| ID | 需求条目 | 状态 | 验收口径 | 数据来源 | 优先级 | 依赖 |
|---|---|---|---|---|---|---|
| R4 | 保留拟南芥/水稻/玉米/大豆四物种验证 | ✅ 已有 | 四物种 Ensembl 62 完整导入 + assembly accession 审计 + 染色体数/length 独立清单已通过；验收=重跑 `audit_reference_datasets.py` 得 0 失败 | `chromosome-validation.md` | P0 | R3 |
| R5 | 新增至少两个完整非模式案例 | 🔶 有缺口 | 已有水稻豆 **full gold**（结构/序列/翻译/表达 re-quantify）+ 穿心莲 + 木槿两个 import/search/序列级案例；**缺口**：确认「两个完整」口径——是否需把穿心莲或木槿之一提升为 full gold，或新增第三个 | `non-model-portability-evidence.md` | P0 | R4 |
| R6 | 验证不同物种 GFF3/GTF 导入 | ✅ 已有 | 水稻豆 GFF3+GTF 双格式计数已断言；四物种 GFF3 + mint GTF→GFF3 已验证；验收=盘点记录各物种格式覆盖 | 非模式 manifest | P1 | R5 |
| R7 | 验证基因结构/转录本/CDS 坐标 | ✅ 已有 | 25 条 multi-locus 序列 gold（5 loci × 5 物种）逐条比对已冻结；验收=重跑 `build_nonmodel_sequence_gold.py` + Java 测试一致 | `multi-locus-sequence-gold.tsv` | P0 | R5 |
| R8 | 验证 FASTA 序列提取和蛋白质翻译 | ✅ 已有 | 水稻豆负链三外显子 locus 的 span/transcript/CDS/蛋白与 RefSeq 记录一致（含 GFF3/GTF stop_codon 差异处理）；验收=盘点该证据 | `non-model-portability-evidence.md` | P0 | R7 |
| R9 | 记录每个物种组装/注释版本/校验值 | ✅ 已有 | 各物种 manifest.json 已记录组装 accession/注释 release/来源/SHA-256/访问日期；验收=统一导出为论文补充表（S1） | 各 `manifest.json` | P0 | R5 |

### 第三阶段·核心分析模块金标准

| ID | 需求条目 | 状态 | 验收口径 | 数据来源 | 优先级 | 依赖 |
|---|---|---|---|---|---|---|
| R10 | Promoter（启动子提取及链方向） | ✅ 已有 | 8 个拟南芥启动子区间与 `bedtools getfasta -s` 逐位一致（4 正链+4 负链，0 mismatch）；验收=盘点该 CI 证据 | `research-module-validation.md` | P0 | R8 |
| R11 | Sequence QC（GC/长度/阅读框/起止密码子） | ✅ 已有 | 4 条真实 RBCS CDS 的 GC/长度/frame/起止密码子与参考计算一致、无 QC flag；验收=盘点 | `research-module-validation.md` | P1 | R8 |
| R12 | Homology（不同保守程度蛋白家族） | 🔶 有缺口 | 已有 RBCS 家族（高保守 paralog-rich）与 BLASTP 20/20 一致；**缺口**：清单要求「不同保守程度家族」——需补 1 个中/低保守家族金标准以支撑"不同保守程度"表述，否则在论文中降级为"单一高保守家族 smoke test" | `research-module-validation.md`、`compare_homology_blastp.py` | P1 | R8 |
| R13 | Domain（结构域位置与多外显子 CDS 映射） | ✅ 已有 | 16 条结构域/缺失记录（含多外显子 junction）与独立 Python 坐标一致；验收=盘点 | `research-module-validation.md`、`DomainCaseValidationTest` | P1 | R7 |
| R14 | Expression（样本分组/均值/中位数/缺失值） | ✅ 已有 | 拟南芥 leaf 分组 mean=median=16.0、低组 mean=2.5、测量零保留为 0 已断言；验收=盘点 | `research-module-validation.md` | P1 | R3.4 |
| R15 | Comparison（多基因结构比较和坐标映射） | ✅ 已有 | v0.2.0 多 locus 比较工作区（共享尺度、链向对齐、intron 压缩、SVG/PNG/TSV 导出）已有 + 测试；验收=盘点功能与测试 | CHANGELOG 0.2.0 | P1 | R7 |
| R16 | Project（分析输入/参数/结果完整恢复） | ✅ 已有 | 项目保存/恢复含视图/选择/参数/表达快照，checksum 保护；验收=盘点 project 测试 | `portability-and-reproducibility.md` | P0 | R3.5 |

### 第四阶段·对比实验与用户验证

| ID | 需求条目 | 状态 | 验收口径 | 数据来源 | 优先级 | 依赖 |
|---|---|---|---|---|---|---|
| R17 | 与 JBrowse2（基因结构浏览） | 🆕 需新增 | 同源文件/位点/输出要求，记录「加载时间、浏览操作、注释显示正确性」三指标；只比共同任务（基因结构显示），JBrowse2 需自建/上传 vs 本地打开列为功能差异 | 拟南芥 TAIR10 子集 + 水稻豆 locus | P1 | R2.5, 对比方案 |
| R18 | 与 IGV（基因组区域/表达轨道） | 🆕 需新增 | 同源区域/表达轨道，记录「数据兼容性、显示能力」；IGV 通用 NGS viewer 无结构比较/金标准导出列为差异 | 拟南芥 + 水稻豆 | P1 | R2.5 |
| R19 | 与 TBtools（植物基因组分析） | 🆕 需新增 | 同任务（如序列提取/GFF 处理）记录「功能覆盖、操作步骤、结果一致性」；TBtools 脚本集合 vs 交互式列为差异 | 拟南芥/水稻豆 | P1 | R2.5 |
| R20 | 与 gffutils（GFF 注释解析） | ✅ 已有 | 同 GFF3 同 locus，层级正确性/内存/耗时三指标已实现（四物种 + 水稻豆 public case，冷启动分开报告）；验收=盘点 + 汇入对比图表 | `compare_gffutils.py`、`compare_public_case.py` | P0 | R7 |
| R21 | 与 Biopython（序列提取处理） | 🆕 需新增 | 同 FASTA+坐标，比较「序列一致性、链方向、耗时」；promoter 已有 bedtools 基线，Biopython 需新增同 locus 脚本 | TAIR10 FASTA + 水稻豆 locus | P1 | R8 |
| R22 | 统一数据/任务/硬件性能测试 | 🆕 需新增 | 固定数据+任务+单台声明硬件，≥3 次交替冷启动，报告导入时间/峰值内存/搜索 p50·p95/比较布局耗时；不 extrapolate 历史数字 | `performance.json`（历史仅参考）+ 新实测 | P1 | R2.5 |
| R23 | 真实科研人员使用测试 | 🆕 需新增 | 招募 ≥3 名研究者，固定任务+随机工具顺序，记录成功/错误/外部编辑/时间，产出可用性定性结论；无参与者则明确「未做用户研究」 | 五任务协议（`validation-and-benchmarks.md`） | P2 | R17-R22 |

### 第五阶段·论文及投稿材料

| ID | 需求条目 | 状态 | 验收口径 | 数据来源 | 优先级 | 依赖 |
|---|---|---|---|---|---|---|
| R24 | 核心技术贡献和创新点 | 🆕 需新增 | 提炼「通用/正确/可复现」三点+软件工程贡献（本地 BYOD、溯源、可移植项目），不含新算法/新发现宣称 | 全部既有证据 | P0 | R1-R23 |
| R25 | 软件架构图和数据处理流程图 | 🔶 有缺口 | 已有 class-diagram/sequence-diagram（mermaid）；**缺口**：导出为论文可用的矢量图 + 补一张「导入→校验→建模→查询→导出」数据流总图 | `docs/*.mermaid` | P1 | R24 |
| R26 | 主要功能展示图 | 🔶 有缺口 | 已有 Figure1.jpg（旧版）；**缺口**：更新为瘦身后工作流 + 六示例 + 导入 health-check 的截图 | 应用实际截图 | P1 | R2.3 |
| R27 | 跨物种验证结果图表 | 🔶 有缺口 | 数据已有（四物种+三非模式）；**缺口**：绘制跨物种结构一致性/序列 gold 对比图（主图2） | 各 gold 集 + 测试结果 | P0 | R4-R9 |
| R28 | 软件对比和性能基准图表 | 🔶 有缺口 | gffutils/Biopython 数据可出图；JBrowse2/IGV/TBtools 需先完成 R17-R19；**缺口**：对比矩阵图 + 性能基准图 | R17-R22 实测 | P1 | R17-R22 |
| R29 | Materials and Methods / Results / Discussion | 🆕 需新增 | 按 FCS 模板撰写三段；Results 只写实测、Discussion 只做有证据的边界声明 | 全部证据 | P0 | R24-R28 |
| R30 | 整理代码/数据/软件版本/复现说明 | 🆕 需新增 | 产出 CITATION/复现说明/数据可用性声明（两仓库 DOI、版本 tag、SHA-256、环境） | 两仓库 manifest + tag | P0 | R2.6 |
| R31 | 核查参考文献真实性 | 🆕 需新增 | 逐条核对文献 DOI/卷期/页码/作者，去除无法核实条目；PIF6 等案例文献需确认真实可引用 | 文献库 | P0 | R29 |

---

## 3. 对比实验设计

**原则**：只比较「共同支持的任务」，功能差异单独列表、不比优劣；统一数据源、位点、输出要求与硬件；≥3 次交替冷启动；把 setup 与 task 时间分开记录。

### 3.1 五工具对比矩阵草案

| 工具 | 类型 | 共同任务（公平比较） | 比较指标 | 功能差异（单独列出，不比优劣） | 状态 |
|---|---|---|---|---|---|
| JBrowse2 | Web 基因组浏览器 | 基因结构浏览 | 加载时间、浏览操作、注释显示正确性 | 需自建/上传服务 vs PhytoScope 本地打开；无内建蛋白质翻译/多基因比较导出 | 🆕 待做 |
| IGV | 桌面基因组浏览器 | 基因组区域浏览 + 表达轨道 | 数据兼容性、显示能力 | 通用 NGS viewer，无基因结构比较/金标准导出；表达轨道单位与分组较弱 | 🆕 待做 |
| TBtools | 植物基因组分析套件 | 植物基因组常见分析（序列提取/GFF 处理） | 功能覆盖、操作步骤、结果一致性 | 脚本/菜单驱动 vs 交互式浏览；结果可溯源性与项目化较弱 | 🆕 待做 |
| gffutils | Python 库 | GFF 注释解析（层级正确性） | 层级正确性、内存、耗时（index 构建 vs in-memory 分开报告） | SQLite 索引 vs PhytoScope 内存模型；无 UI | ✅ 已做 |
| Biopython | Python 库 | 序列提取处理 | 序列一致性、链方向、耗时 | 无注释层级/UI/表达整合 | 🆕 待做 |

**结论导向**：论文只主张「在共同任务上结果一致 + 各自设计差异」，不主张「更快/更好/更易用」，除非 R22 统一硬件实测支持且措辞谨慎。

### 3.2 五组测试数据用途

| 组 | 物种/参考 | 在对比与论文中的用途 |
|---|---|---|
| 1 | 拟南芥 TAIR10 / Araport11 | **全模块金标准 + 统一对比基准**：序列提取/翻译、Promoter、SeqQC、Homology、Domain、Expression 的金标准载体；gffutils/Biopython 基线同源数据；表达矩阵 E-MTAB-7978 |
| 2 | 水稻 IRGSP-1.0 / Ensembl 62 | **单子叶模式物种**：跨物种 GTF/GFF3 导入、染色体命名差异、RBCS 同源蛋白家族跨物种成员 |
| 3 | 玉米 B73 NAM 5.0 / Ensembl 62 | **复杂大基因组**：685 序列、多序列浏览与性能/内存压力场景 |
| 4 | 水稻豆 GCF_029865625.1 | **非模式 full gold**：负链三外显子结构+翻译+表达 re-quantify 的端到端案例；gffutils public case 对比载体 |
| 5 | 另一非模式（穿心莲 or 木槿，待拍板） | **第二非模式广度**：import/search/序列提取级可移植性证据（若提升为 full gold 则更完整） |

> 大豆（Glycine_max v2.1）保留在「四物种复核」中作为第 6 组复核数据，不进入 5 组展示清单。

---

## 4. 论文图表清单

### 4.1 五张主图

| # | 图 | 内容 | 数据来源 | 产出方 |
|---|---|---|---|---|
| 图1 | 软件总览与工作流 | 导入→校验→建模→浏览/查询→比较→导出 管线 + 六示例截图 | 应用截图（更新 Figure1.jpg） | 架构师/工程师截图，PM 定稿 |
| 图2 | 跨物种可移植性 | 四模式+三非模式的结构/序列金标准一致性（坐标、序列 hash 逐位一致） | R4-R9 gold 集 | QA 出数据，工程师/PM 绘图 |
| 图3 | 核心模块金标准验证 | Promoter/SeqQC/Homology/Domain/Expression 与独立工具（bedtools/BLASTP）一致结果 | `research-module-validation.md` CI 结果 | QA 出数据 |
| 图4 | 软件对比与功能差异 | 五工具共同任务一致性 + 功能差异对照（gffutils/Biopython 定量 + JBrowse2/IGV/TBtools 定性） | R17-R22 | QA 实测 + PM 绘图 |
| 图5 | 可复现性机制 | 项目保存/恢复、SHA-256 溯源、双平台数据镜像、Manifest 版本示意 | R2/R16 证据 | 架构师/工程师 |

### 4.2 五张补充图

| # | 图 | 内容 | 数据来源 | 产出方 |
|---|---|---|---|---|
| 补图1 | 解析与坐标体系 | GFF3/GTF/GFF2 解析、1-based vs 0-based、正负链、stop_codon 差异 | `input-format-specification.md` + 水稻豆案例 | 架构师 |
| 补图2 | 非模式详细案例 | 水稻豆 LOC124833352 负链三外显子结构+翻译对齐 | `non-model-portability-evidence.md` | QA/工程师 |
| 补图3 | 表达矩阵 | 缺失 vs 零区分、样本分组、ID 对齐报告 | R3.4/R14 | QA |
| 补图4 | 软件架构 | 类图/序列图（已有 mermaid 导出矢量图） | `docs/*.mermaid` | 架构师 |
| 补图5 | 性能基准 | 统一硬件下导入时间/峰值内存/搜索延迟/比较布局 | R22 新实测 | QA |

### 4.3 四张补充表

| # | 表 | 内容 | 数据来源 | 产出方 |
|---|---|---|---|---|
| 补表1 | 参考数据清单 | 9 文件 + 组装/注释版本 + SHA-256 + 来源 + 许可 | `phytoscope-reference-data/MANIFEST.json` | 工程师/PM |
| 补表2 | 测试覆盖矩阵 | R1-R31 需求条目 ↔ 测试/工具/证据映射 | 需求池本表 + 测试清单 | PM + QA |
| 补表3 | 五工具功能差异对照 | 共同任务 vs 独有功能 vs 设计差异 | R17-R22 | PM |
| 补表4 | 复现与软件信息 | 软件版本/tag/DOI、数据仓库、运行环境、复现命令 | R2.6/R30 | 工程师/PM |

---

## 5. 待确认问题（需用户/主理人拍板）

1. **论文语言**：中文还是英文？（FCS 为英文期刊，投稿正文须英文；本 PRD 仅文档语言，投稿语言需确认英文写作负责人）
2. **DOI 方案**：Zenodo 归档「软件仓库 + 数据仓库」各取一个 DOI 是否可行？（FCS Code & Data Letter 通常要求软件有可引用 DOI；需确认 Zenodo 可访问性）
3. **非模式第二物种选哪个**：穿心莲（Andrographis paniculata）还是木槿（Hibiscus syriacus）作为「另一非模式」进入 5 组展示？是否需把其一提升为 full gold（当前两者均为 import/search/序列级，非 full）？
4. **正式版本号**：投稿冻结版本用 `v0.2.0`（已 tag）还是升级 `v1.0.0`？影响 R2.3/R2.6/R30 的 tag 与 DOI。
5. **GFF2 边界**：是否需要在论文中显式宣称 GFF2 支持？（当前 importer 兼容 GTF/GFF2，但缺专用测试集；若不宣称，R3.1 降为 P2）
6. **对比实验范围**：JBrowse2/IGV/TBtools 三项「需新增」的工作量较大且需人工操作 GUI——是否接受「以定性功能差异 + 官方文档特征」替代「实机交互计时」，以控制投稿前成本？
7. **用户研究**：R23 真实用户测试是否纳入本次投稿范围，还是明确标注「未做、列为 future work」？（建议 P2，避免拖延投稿）
8. **许可复核**：4 条 `needs_confirmation` 许可（TAIR/Araport11、E-MTAB-7978、mint 表达矩阵、mint GFF3 CC BY 4.0）由谁复核、何时定稿？（影响数据可用性声明合规性）

---

## 附：执行顺序建议（与清单五批对齐）

1. **第一批（软件基础）**：R1.* 盘点 → R2.* 发布/许可/tag/DOI → R3.* 测试盘点补 GFF2
2. **第二批（实验验证）**：R4-R9 跨物种 → R10-R16 模块金标准（补 R12 中/低保守家族）
3. **第三批（软件比较）**：R20 已就绪 → R21 新增 Biopython → R17/R18/R19 定性/定量
4. **第四批（性能与用户研究）**：R22 统一硬件实测 → R23（可选）
5. **第五批（论文撰写）**：R24-R31 按 P0 顺序产出

> **红线**：全程不新增算法、不虚构未测数字、不把 runner-specific 历史性能当当前实测；所有结论只来自盘点确认或新实测的证据。
