# PhytoScope FCS 投稿 · 阶段6 对比实验执行报告

> 阶段6（第三批）目标：对 JBrowse2 / IGV / TBtools 做「定性功能对照 + 可行处实机」，
> 对 gffutils / Biopython 做「本地实机计时」。本报告只记录真实跑出的数字；
> 无法实机的工具如实标注为「定性对照，非本机实测」，绝不编造计时。
>
> 执行人：寇豆码（software-engineer-6） · 仓库 `dev` HEAD=`247ff11` · 本机 Windows 11 amd64。

---

## 1. 环境侦察结果（只读探测）

| 工具/环境 | 本机可用 | 版本 / 路径 | 说明 |
| --- | --- | --- | --- |
| Python | ✅ | 3.13.12（`C:\Users\asus\.workbuddy\binaries\python\versions\3.13.12`） | `python` 命令可用 |
| pip | ✅（经 `python -m pip`） | pip 26.1.2 | 顶层 `pip` shim 损坏（`envs/default` 下 `pip.exe` 缺 `pip` 模块）；用 `python -m pip` 正常 |
| gffutils | ✅（已安装） | 0.13 | 本次 `python -m pip install gffutils==0.13 biopython` 装好，版本精确匹配脚本校验 |
| Biopython | ✅（已安装） | 1.88 | 同上；连带 numpy 2.5.4 |
| Java | ✅ | 21.0.6+8-LTS | 满足 PhytoScope 运行要求 |
| PhytoScope JAR | ✅ | `target/phytoscope-0.2.0.jar`（内容=当前 0.3.0 源码，含 `--benchmark`/`--compare`/批量渲染三种 CLI 模式） | 本报告不重新打包、不改 pom |
| blastp | ✅ | 2.17.0+（`/f/NCBI/blast-2.17.0+/bin/blastp`） | 阶段6之外，阶段5同源对比已用 |
| bedtools | ❌ | 不在 PATH | 本机不可用；仓库 CI run #192（Ubuntu）已用 bedtools v2.31.1 做过 8 段启动子提取对比，本机不重复 |
| JBrowse2 | ❌ | 未安装（`which jbrowse` 无结果） | 不强行 npm 安装 → 定性对照 |
| IGV | ✅（GUI，未脚本化） | 2.19.3，`C:\Program Files\IGV_2.19.3\lib\igv.jar`；用户目录 `~/.igv`、`~/igv` 存在，日志确认曾在本机运行 | 桌面 GUI，无显示环境的脚本化快照不可靠 → 定性对照 |
| TBtools | ✅（GUI，未脚本化） | TBtools-II，`C:\Program Files\TBtools\TBtools-II.exe`（+ `TBtools_JRE1.6.jar`） | 桌面 GUI 工具箱 → 定性对照 |

**结论**：gffutils 与 Biopython 两条「实机计时」路线可落地；blastp 本机可用（阶段5已用）；
JBrowse2 未装、IGV/TBtools 为 GUI 应用，三者均走「定性功能对照」。

---

## 2. 本地已冻结/可复用的对比数据

| 数据 | 路径 | 用途 |
| --- | --- | --- |
| 水稻豆 Vigna GFF3/GTF | `target/non-model/vigna-umbellata/GCF_029865625.1_genomic.gff.gz`（SHA-256 已核）、`_genomic.gtf.gz` | gffutils 全量注释解析对比 |
| 水稻豆 10 号染色体 FASTA | `target/non-model/vigna-umbellata/NC_142174.1.fna`（25,756,035 bp） | Biopython 序列重建 |
| 水稻豆金标 | `src/test/resources/non-model/vigna-umbellata/gold-LOC124833352.fasta` | 序列一致性对照 |
| 穿心莲 / 木槿 GFF3 + scaffold | `target/additional-nonmodel/GCF_009805555.1_*`、`GCF_006381635.1_*`、`NW_026137634.1.fna`、`NW_024062564.1.fna` | Biopython 多物种扩展 |
| 穿心莲 / 木槿金标 | `src/test/resources/non-model/{andrographis-paniculata,hibiscus-syriacus}/gold-*.fasta` | 序列一致性对照 |
| 多基因座序列金标 | `src/test/resources/non-model/multi-locus-sequence-gold.tsv`（3 物种 × 5 基因座 = 25 条编码转录本哈希） | 交叉核验 |
| TAIR10 / Araport11 | `phytoscope-reference-data/data/species/arabidopsis/TAIR10_genomic.fna.gz` + `Chr1-5.gff3.gz` | 可选 Biopython 扩展（本次未用，避免与已冻结金标重复） |

---

## 3. 可实机项清单（真实跑出）

### 3.1 gffutils 0.13 —— 水稻豆 GCF_029865625.1 全量注释解析对比

**协议**：复用 `tools/compare_public_case.py`（3 轮交替执行顺序的独立进程实验），
PhytoScope（`--benchmark`，`-Xmx3g`）与 gffutils（`compare_gffutils.py`，磁盘 SQLite 索引）
对同一份 726,726 行全量 GFF3 导入，检索同一冻结基因座 `gene-LOC124833352`，
并各自与「由源 GFF3 独立推导的结构元组」三方比对。层级正确性 = 输出元组 == 源元组 == 对方元组。

**三轮正式对比结果（`status=PASS`，逐轮元组一致性均 `true`）**：

| 指标 | 实测值（3 轮） |
| --- | --- |
| 源 GFF3 独立推导结构元组数（金标） | **8**（1 gene + 1 mRNA + 3 exon + 3 CDS；该注释无显式 UTR） |
| 三方一致性（PhytoScope == gffutils == 源元组） | **3/3 轮通过** |
| PhytoScope 冷导入全量 GFF3（中位，`-Xmx3g`） | **2,670.76 ms**（三轮 2,904.33 / 2,670.76 / 2,648.19） |
| gffutils 冷建 SQLite 索引（中位） | **57,801.95 ms**（三轮 57,402.73 / 62,941.73 / 57,801.95） |
| PhytoScope 选中基因座搜索（100 次，中位/p95） | 13.46 ms / 16.16 ms |
| gffutils 选中基因座查询（100 次，中位/p95） | 0.24 ms / 0.28 ms |
| PhytoScope 全量导入规模 | 41,713 基因 / 726,726 特征 / 232 序列 |
| PhytoScope 峰值堆内存（实际池峰值，非 RSS） | Old Gen ≈ 1.58 GB，Eden ≈ 0.49 GB，Survivor ≈ 0.07 GB（配置上限 3 GB） |
| gffutils 峰值内存（RSS） | Windows 无 `resource` 模块，未测（如实标注，脚本记录 `not measured`） |

> 注：gffutils 构建的是磁盘 SQLite 索引，PhytoScope 是内存模型，二者存储设计不同，
> 冷启动（导入/建索引）分开报告，不互相比较为「速度优劣」。计时为 3 轮独立进程
> 交替执行顺序（先导单次 59.78 s 仅作流程自检，不计入结果）。

- 机器可读结果：`target/phase3-comparison/gffutils/summary.json`（+ 每轮 `trial-N/` 下的 `performance.json` 与 `parsed-structure.tsv`）。

### 3.2 Biopython 1.88 —— SeqIO 序列提取 / 反向互补 vs 冻结金标

**协议**：新脚本 `tools/compare_biopython_seqio.py`。仅用 Biopython `SeqIO`/`Seq` + 源 GFF3
特征行，对 3 个非模式植物目标基因座做链方向感知重建：基因组区间（genomic span）、
拼接转录本、CDS、翻译蛋白、推断 5'/3' UTR；与冻结金标 FASTA（`gold-*.fasta`，即 Java 测试
已断言 PhytoScope 导出与之逐字节一致的文件）逐字节比对。每物种 3 轮取中位计时。

**结果（3 物种全部 `all_gold_products_match=true`，报告 `status=PASS`）**：

| 物种 / 基因座 | 链 | 外显子 | CDS 长度 | 蛋白 aa | 全产物一致 | FASTA 载入(ms) | span(ms) | transcript(ms) | CDS(ms) | 翻译(ms) |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| vigna `LOC124833352` | − | 3 | 1,983¹ | 660 | ✅ | 112.4 | 0.087 | 0.037 | 0.018 | 0.219 |
| andrographis `LOC127243311` | + | 11 | 3,372 | 1,123 | ✅ | 12.5 | 0.005 | 0.022 | 0.007 | 0.320 |
| hibiscus `LOC120212642` | − | 2 | 624 | 207 | ✅ | 7.1 | 0.019 | 0.012 | 0.006 | 0.083 |

¹ 水稻豆 GFF3 CDS 含末端终止密码子（1,983 nt）；GTF 将终止密码子单列为 `stop_codon`（1,980 nt），
二者翻译为同一 RefSeq 蛋白 XP_087956893.1——Biopython 重建与冻结金标一致。

**结论**：Biopython SeqIO 在 3 个非模式物种（正链 1 例、负链 2 例）上与冻结金标
逐字节一致，链方向处理正确；计时为 Biopython 单进程内的真实墙钟中位（非 GUI、非速度优劣主张）。

原始输出：`target/phase3-comparison/biopython/summary.json` + 每物种 `<species>.json`。

### 3.3 blastp（阶段5已做，本机复核可用）

本机 `blastp 2.17.0+` 可用；仓库 CI run #192 已用 BLASTP 2.12.0+ 与 PhytoScope
在 20/20 蛋白相似性分类上达成一致。本报告不重复计时，仅记录「本机 blastp 可实机」这一环境事实。

---

## 4. 定性对照项清单（JBrowse2 / IGV / TBtools —— 定性，非本机实测）

以下矩阵基于三个工具的公开文档与已知功能，仅比较「与 PhytoScope 共同支持的任务」；
**未在本机脚本化实测，无计时数字，不冒充实测**。功能差异单独列出。

### 4.1 共同任务功能对照矩阵

| 任务（来自 `docs/validation-and-benchmarks.md` 的用户任务协议） | PhytoScope | IGV 2.19.3 | JBrowse2 | TBtools-II |
| --- | --- | --- | --- | --- |
| 按基因 ID 定位（不同序列上的多个基因座） | ✅ 本地搜索 + 多序列切换 | ✅ 按基因名/坐标跳转（需索引或 `.genome`） | ✅ 文本搜索/坐标跳转（需 text-index） | ⚠️ 以工具为单位，非连续浏览器导航 |
| 查看异构体（exon/CDS/UTR 坐标） | ✅ 结构轨道 + 详情面板 | ✅ 结构轨道（缩放查看） | ✅ 结构轨道 | ✅ Gene Structure View（GFF3 绘图，非连续浏览器） |
| 关联表达（样本值 + 样本顺序） | ✅ 导入表达矩阵并精确 ID 对齐 | ⚠️ 以 bigWig/覆盖度轨道呈现，无「样本矩阵」原生概念 | ⚠️ 定量轨道（bigWig 等），无矩阵原生概念 | ✅ 热图（样本矩阵） |
| 生成对比图（结构/表达，可编辑、单位/共享刻度正确） | ✅ 内置 ComparisonFigure + PNG/SVG/TSV 导出 | ⚠️ 截图/会话，无「多基因座对齐对比图」原生视图 | ⚠️ 多视图并排，无原生对齐对比图 | ⚠️ 各可视化独立导出 |
| 恢复分析（保存/重开项目状态） | ✅ 项目保存/重开 | ✅ 会话 `.xml` 保存/加载 | ✅ 会话 URL/JSON 共享 | ⚠️ 无统一项目会话概念 |
| 无门户外物种（非模式）导入 | ✅ 通用 GFF3/GTF + FASTA + 矩阵 | ✅ 通用格式（需本地 FASTA/GFF + 索引） | ✅ 通用格式（需配置/插件） | ✅ 通用格式（各功能各自导入） |
| 头less/可复现导出 | ✅ `--gff3/--example/--loci/--out/--format` | ⚠️ 批处理 `-b script` 可截图，仍需图形环境 | ⚠️ CLI 生成配置，渲染在浏览器 | ⚠️ GUI 为主 |

图例：✅ 原生支持；⚠️ 需额外配置/外部工具/非原生。

### 4.2 功能差异单独列出（不混入共同任务）

- **部署形态**：PhytoScope = 单机 JavaFX 桌面 + 头less CLI；IGV = Java 桌面/Web；JBrowse2 = 浏览器（需静态托管）；TBtools = Java 桌面工具箱。
- **数据范围**：PhytoScope 聚焦「基因结构浏览 + 表达关联 + 对比图 + 序列导出 + 同源/结构域/启动子研究模块」；IGV/JBrowse2 是通用基因组浏览器（覆盖比对 BAM、变异 VCF、定量 bigWig、互作/环状等，超出 PhytoScope 范围）；TBtools 是数十种工具的集合（BLAST/GO/富集/绘图等，范围不同）。
- **交互范式**：IGV/JBrowse2 是连续坐标缩放导航；PhytoScope 是「基因座为中心的离散选择 + 并排对比」；TBtools 是离散工具入口。
- **序列产品导出**：PhytoScope 直接导出基因组区间/转录本/CDS/蛋白/推断 UTR；IGV/JBrowse2 导出参考 FASTA 子串，但转录本/CDS/蛋白拼接需外部脚本；TBtools 有 Fasta/GFF 工具可近似完成（需多步）。
- **表达数据模型**：PhytoScope 原生「样本×基因矩阵 + 单位/样本顺序/缺失≠0」契约；IGV/JBrowse2 用定量轨道（bigWig），不原生持有样本矩阵语义；TBtools 热图持有矩阵。

**说明**：上述为定性描述，基于公开文档与工具常识，**非本机实测**；不据此作「易用性/省时/发现率」任何主张。

---

## 5. 卡点与后续

- **卡点 1**：`bedtools` 本机不在 PATH；不强行安装。仓库 CI 已有 bedtools v2.31.1 对比（8/8 启动子序列一致），本报告记录为「CI 已完成、本机未重复」。
- **卡点 2**：IGV/TBtools 为 GUI 应用，本沙箱无显示环境，脚本化截图不可靠；按团队拍板走定性对照。
- **卡点 3**：Windows 无 `resource` 模块，gffutils 峰值 RSS 未测得（脚本已按 `None`/`not measured` 记录，未编造）。
- **已完成**：gffutils 全量索引构建单次约 58 s，3 轮共约 3.5 分钟，结果已回填 3.1 节（`status=PASS`）。

---

## 6. 已落盘产物

- `docs/fcs-phase3-comparison-plan.md`（本报告）
- `tools/compare_biopython_seqio.py`（新增，Biopython 对比脚本）
- `target/phase3-comparison/biopython/summary.json` + `{vigna-umbellata,andrographis-paniculata,hibiscus-syriacus}.json`
- `target/phase3-comparison/gffutils/summary.json`（3 轮对比，后台执行）
