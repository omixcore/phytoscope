# PhytoScope 增量 PRD：自定义基因组导入体检（custom genome import health-check）

> **文档类型**：增量 PRD（简单档，不含竞品分析）。本文只描述本次变更；未提及的功能、架构与交互沿用既有设计与 `docs/prd-slim-bundled-data.md`（v1.2）、`docs/prd-reproducibility-health-report.md` 的既定约定。
> **编写**：产品经理 许清楚　|　**状态**：v1.0 初稿，待架构设计与主理人评审
> **语言**：中文（与用户需求一致）
>
> **增量主题**：面向更广泛的植物基因组用户，提供「导入自己的基因组」向导，把分散的本地导入、表达 ID 匹配、FASTA 序列名覆盖检查整合为一份可导出（JSON 机读 + Markdown 人读）的导入体检报告，并生成记录规范版本 / 组装标识 / 注释版本 / 文件路径 / SHA-256 / 序列别名映射的 manifest v2。

---

## 0. 变更基线与项目信息

| 项 | 内容 |
|---|---|
| Project Name | `phytoscope`（增量标识：`import-health-check`） |
| Language | 中文（文档与交付物） |
| Programming Language | Java 21 + Spring Boot 3.2.5 + JavaFX（**既有技术栈，本次不变**） |
| 代码基线 | `dev` 分支 @ `235b196`（已含「剥离内置完整数据」`8cef0c8` 与 provenance 回填 `235b196`；gitee origin 与 github 两远端均已同步） |
| 远端 | `origin` = `https://gitee.com/omixcore/phytoscope.git`；`github` = `git@github-w1996jy:omixcore/phytoscope.git`（均已同步） |
| 原始需求复述 | 「面向更广泛的植物基因组用户，下一版优先做『自定义基因组导入体检』。用户拿到一套 FASTA、GFF3 和表达矩阵后，最先要确认的是这些文件能否一起用，以及哪些基因或序列需要人工核对。PhytoScope 已有本地导入、表达 ID 匹配和 FASTA 序列名覆盖检查，可以把这些分散的检查整合成一份可导出的报告。」 |
| 产品定位（不变） | *portable local queries of plant genome data* —— 本地、可移植、可复现、非模式物种友好。与「剥离内置数据」形成连贯故事：应用默认是「带来你自己的数据」（bring-your-own-data）。 |

### 0.1 用户已拍板的设计决策（硬性约束，直接采信，不再开放讨论）

1. **报告产物 = JSON（机读，可进 CI 门禁）+ Markdown（人读）双产物**，风格与现有 `SHA256SUMS.txt` / `MANIFEST.json` 一致。
2. **严重度模型**：`ERROR`（阻断导入，如注释引用了 FASTA 不存在的序列、GFF3 解析失败）、`WARNING`（可用但需人工核对，如 FASTA 有序列无注释、表达 feature_id 不在注释里、序列名需要别名映射）、`INFO`。报告须能作为「放行 / 不放行」判据。
3. **新增独立入口**「导入自己的基因组」向导，与现有 Example datasets 流程明确分开；向导跑完体检报告后再决定是否加载项目。
4. **v1 范围 = FASTA + GFF3 + 表达矩阵**的联合导入 + 体检报告 + manifest v2；**BED / bedGraph 轨道放到 v2**。v1 的坐标归一化只做「FASTA-ID ↔ GFF3-ID 别名映射」，BED(0-based)↔GFF3(1-based) 真实换算留到 v2。
5. **别名映射持久化在 project manifest（按项目）**，不引入全局别名表（除非判断有必要，可在 PRD 列为可选）。
6. **gzip 处理复用现有 `Gff3Parser` 的 sniff 逻辑**，不重新发明。
7. 复用现有能力底座（见 §0.2），不重写已存在的解析 / 比对逻辑。

### 0.2 现状核对结论（已实测，PRD 据此编写）

> 以下结论基于 `dev @ 235b196` 工作树实测代码，引用真实文件路径与方法名；行号以本次读取为准。

- **现有导入入口均已存在，且彼此解耦**：`MainView.importAnnotation()`（`MainView.java:255`，标题 `Import annotation`，`FileChooser` 接受 `*.gff3/*.gff/*.gtf/*.gff2`）、`MainView.importGenomeFasta()`（`:295`，`Import matching genome DNA FASTA`，接受 `*.fa/*.fasta/*.fna/*.gz`）、`MainView.importExpression()`（`:379`，需先有注释）、以及对应的非对话框入口 `loadAnnotation(Path)`（`:401`）/`loadGenomeFasta(Path)`（`:308`）/`loadExpression(Path)`（`:408`）。这些是单文件独立导入，尚无「三件套联合导入 + 联合体检」入口。
- **`describeFastaAlignment()` 实际位于 `MainView:339`**（用户描述的「`GeneSequenceService.describeFastaAlignment`」实为 `MainView` 私有方法，不在 `GeneSequenceService`）。它比对 FASTA 序列名与注释染色体，对「N of M 注释染色体没有 FASTA 记录」输出 `WARNING` 文本（`:364-367`），经 `sequenceStatusText()`（`:327`、`:331`）进入状态栏。这是**序列名覆盖检查的现有能力底座**，但它是导入后 UI 反馈，不是可导出、可定位到具体记录的报告。
- **「AnnotationHealthCheck」模块实为 `AnnotationHealthService`**（`service/AnnotationHealthService.java`）：提供 `assess(GenomeDataService, ExpressionDataService)` → `HealthReport`（`:149`）。`HealthReport` 含 `toText()`（`:84`）/`toJson()`（`:100`），每条 `HealthIssue`（`:52-55`）带 `severity / code / message / lineNumber / rawEvidence / geneId / transcriptId`；`Severity` 枚举为 `ERROR / WARNING / NOTICE / NOT_CHECKED`（`:29-34`）。它在 `MainView:469` 通过「Annotation health report…」菜单触发，是**导入后**的注释一致性体检（孤儿 Parent→ERROR、坐标倒置/越界→ERROR、CDS 帧→WARNING/NOTICE、表达 ID 不匹配→WARNING）。**本增量不改动它**，而是新增一份**导入时**的跨文件引用一致性体检；两者 Severity 口径需对齐（见 §7 Q1）。
- **FASTA 解析底座**（`GeneSequenceService.java`）：`loadFromFile(Path)`（`:146`）/`loadFromContent(String,String)`（`:154`）；标题行第一个空白前的字段作为序列 ID（`:178-180` `id = space<0 ? header : header.substring(0, space)`）；gzip sniff 在 `load()`（`:164` `first==0x1f && second==0x8b`）；`getSequenceNames()`（`:110`）；`normalizeSequenceName()`（`:226-233`，去 `chr` 前缀、去非字母数字）+ 别名构建 `addAlias/addChromosomeAliases`（`:216-224`，自动加 `ChrN`↔`N` 别名）；`resolveSequenceId()`（`:364-373`，先精确匹配再走别名）。**FASTA 标题解析规则与别名归一化是 v1「FASTA-ID ↔ GFF3-ID 别名映射」的权威实现依据**。
- **表达矩阵解析底座**（`ExpressionDataService.java`）：`readMatrix()`（`:243-305`）复用 `#` 注释行中的 `# unit=...` / `# level=...` 元数据（`:254-261`，unit→`valueUnit`、level→`gene`/`transcript`）；首列固定为 feature_id（`:281`）；空值 / `NA` / `N/A` / `.` / `NULL` → missing（`:287`），而 `0` 被解析为实测零值（区别于 missing）；`reconcile(GenomeDataService)`（`:149`）产出 `ImportReport`（`:122-133`，含 `unmatchedIds`/`matchedIds`/`annotationIds`/`missingCells`/`invalidCells`/`warnings`）；`serializeMatrix()`（`:426`）输出带 `# unit=` / `# level=` 的规范 TSV。`ImportReport` 已是「表达 ID 匹配」的现成能力底座。
- **GFF3 / GTF 解析底座**（`Gff3Parser.java`）：`parse()`（`:237`）含 gzip sniff（`:243`，与 FASTA 同一判断）；九列校验（`<9` 列 → 跳过并计入 `skippedRows`，`:289-296`）；ID / Parent 装配与 `OrphanFeature` 收集（`:358-363`）；`decompressPerPart()` 被 `ProjectStore.loadParts()`（`:182`）复用解压每个 part。`ParseDiagnostics`（同包）：`MAX_RECORDS = 10_000`（`:26`，明细封顶、总数不封顶 `skippedRowTotal`），`SkippedRow`（`:50`，含 lineNumber/rawLine/SkipReason）、`OrphanFeature`（`:64`，含 lineNumber/featureId/missingParentId）。`AnnotationFormat` 枚举 `GFF3` / `GTF`（`:16`/`:23`），`allExtensions()`（`:58`）覆盖 `*.gff3/*.gff/*.gtf/*.gff2/*.gz`。
- **项目持久化底座**（`ProjectStore.java`）：`Manifest` 记录（`:38-40`）含 `schemaVersion`（当前 **3**）、`applicationVersion`、`annotationFormat`、`annotations`（`List<Source>`）、`expression`（`Source`）、`view`、`importReport`、`research`；`Source` 记录（`:22`）为 `{entry, origin, sha256}`。`save()`（`:46-81`）把注释字节内嵌 zip（`annotation/part-N.txt`，`:60`）+ 逐文件 SHA-256（`:63`），并把 `Manifest` 写入 `project.json`（`:72-75`）；`load()` 校验 `schemaVersion` 1–3（`:90`）。**manifest v2 即在此基础上扩展**（规范版本 / 参考组装标识 / 注释版本 / 文件路径 / SHA-256 / 序列别名映射），须 bump `schemaVersion` 并在 `load()` 接受新版本。
- **参考清单样式**：`src/main/resources/data/reference-provenance.json` 的结构（`datasets` / `external_resources` / `baseline_commit` / 逐文件 `sha256` / `storage` 标记）是 manifest v2 的权威样式参考；其中 `external_resources` 已记录 `sha256` 与 `bytes`，可直接借鉴「路径 + 校验和 + 来源」三元组。

---

## 1. 产品目标

| # | 目标 | 说明与可度量口径 |
|---|---|---|
| **G1** | **新用户带自己数据的第一步可信** | 用户拿到 FASTA + GFF3 + 表达矩阵后，向导能在加载前一次性回答「这些文件能否一起用」「哪些基因 / 序列需要人工核对」，并以「放行 / 不放行」判据交付。度量：v1 报告在三类组装下均能生成且 ERROR/WARNING/INFO 分类正确（见 §6 AC-1）。 |
| **G2** | **延续「本地、可移植、可复现、非模式物种友好」** | 导入体检全程离线、不新增网络能力与第三方依赖；序列 ID 与坐标处理以「跨物种兼容」为第一原则（FASTA `Chr1` ↔ GFF3 `1`、GFF3 1-based 闭区间 ↔ BED 0-based 半开区间），对任意植物基因组一视同仁。度量：不产生任何外发请求、不引入新 Maven 依赖（§6 AC-6）。 |
| **G3** | **与「剥离内置数据」形成连贯故事** | 应用默认即「带来你自己的数据」；导入向导与既有 Example datasets 明确分流，用户清楚「自带数据」与「内置小示例」是两条独立路径。度量：向导为独立入口，不改动 Example datasets / CATALOGUE（§6 AC-7）。 |

三个目标正交：G1 管用户价值，G2 管产品边界，G3 管叙事连贯。

---

## 2. 范围与边界

### 2.1 v1 做（本次交付）

1. **FASTA + GFF3 + 表达矩阵的联合导入向导**：新增独立入口「导入自己的基因组」，依次 / 一并选择三类文件，复用现有解析底座（`GenomeDataService.loadFromFile` / `GeneSequenceService.loadFromFile` / `ExpressionDataService.loadFromFile`）而非重写。
2. **联合质检**：
   - 文件内完整性（FASTA / GFF3 / TSV 各自能独立解析、无截断 / 编码错误）；
   - 文件间引用一致性（注释 `seqid` ⊆ FASTA 序列集；表达 `feature_id` 匹配注释或独立）；
   - 序列名覆盖与别名映射 UI（FASTA-ID ↔ GFF3-ID 差异列出、用户确认 1:1、写入项目）。
3. **可导出的 JSON + Markdown 报告**：含严重度（ERROR / WARNING / INFO）、能定位到具体记录行号 / 坐标，风格与 `SHA256SUMS.txt` / `MANIFEST.json` 一致。
4. **manifest v2**：记录规范版本、参考组装标识、注释版本、文件路径、SHA-256，以及用户确认的序列别名映射。
5. **验证集**：三类组装（染色体级组装、碎片化 scaffold 组装、带细胞器序列的组装）+ 负链基因 + 跨格式坐标边界测试。

### 2.2 v2 做（明确排除出 v1，本次不实现）

- **BED3 / BED6 导入与批量查询**；
- **bedGraph 连续信号轨道**；
- **BED(0-based) ↔ GFF3(1-based) 真实坐标换算**（v1 仅做 FASTA-ID ↔ GFF3-ID 别名映射，不做 0-based/1-based 区间换算）。

### 2.3 不做（明确排除）

1. **不新增网络下载**：不新增 HTTP 客户端、不弹下载提示、不读取远程清单；与「剥离内置数据」决议一致。
2. **不新增第三方依赖**：`pom.xml` 依赖列表不变；报告 / manifest 仅用 Jackson（已在用）、`java.security.MessageDigest`（SHA-256，已在 `ProjectStore` 使用）。
3. **不改动核心解析算法**：`Gff3Parser` / `GeneSequenceService` / `ExpressionDataService` 的解析与比对逻辑一行不重写，仅调用。
4. **不复用「按需下载」**：v1 完全离线。
5. **不引入全局别名表**（默认不建；仅在 §7 Q2 被列为可选 P2，且不影响 v1 交付）。

---

## 3. 输入规范（权威契约）

> 以下 6 行规范表为**用户提供的物种无关输入规范，原样收录为权威契约**；其后逐行补充「PHY 内部如何归一化 / 校验什么」要点（依据 §0.2 实测代码）。

| 数据 | 推荐格式 | 关键约定 |
|---|---|---|
| 参考基因组 | FASTA（.fa、.fasta、.fna，可 gzip 压缩） | 用 FASTA 标题行第一个空白前的字段作为序列 ID。允许染色体、scaffold、contig 和细胞器序列。 |
| 基因注释 | GFF3（.gff3，可 gzip 压缩） | 使用标准九列、1-based 闭区间，并按 GFF3 规则解析 ID / Parent。继续兼容现有 GTF 导入，示例和新项目统一推荐 GFF3。 |
| 表达矩阵 | UTF-8 TSV（.tsv） | 复用现有 `# unit=TPM`、`# level=gene` 等元数据声明。首列固定为 feature_id，后续列为样本。空值表示缺失，`0` 表示实测零值。 |
| 区间注释与批量查询 | BED3 / BED6（.bed） | BED3 用于批量查询，BED6 用于带名称和链方向的注释。两者均使用 0-based 半开区间。 |
| 连续信号轨道 | bedGraph（.bedgraph） | 固定四列 seqid、start、end、value，使用 0-based 半开区间。 |
| 项目清单 | JSON（manifest.json，随 .phytoscope 项目保存） | 记录规范版本、参考组装标识、注释版本、文件路径和 SHA-256。可记录用户确认的序列别名映射。 |

### 3.1 逐行「PHY 内部归一化 / 校验」要点

- **参考基因组（FASTA）**：
  - 标题解析：`GeneSequenceService.load()`（`:158-198`）取标题行第一个空白前的字段为序列 ID（`:178-180`），其余为描述；空标题 / 空记录 / 重复 ID / 非核苷酸字符均抛 `IOException`（`:177`/`:183`/`:204`/`:186`）。
  - gzip：`:164` 以 `0x1f 0x8b` 魔数 sniff，自动解压，**v1 直接复用，不重新发明**。
  - 序列名归一化：`normalizeSequenceName()`（`:226-233`）小写、去非字母数字、去 `chr` 前缀；导入映射时以归一化名做 1:1 比对（兼容 `Chr1`/`chr1`/`1`）。
  - 校验：记录数 > 0（`:194`）、记录 ID 唯一（`:206`）。
- **基因注释（GFF3 / GTF）**：
  - 九列校验：`<9` 列的行被跳过并计入 `skippedRows`（`Gff3Parser:289-296`），视为文件内完整性 WARNING/INFO。
  - ID / Parent：两遍装配，孤儿 Parent 收集为 `OrphanFeature`（`Gff3Parser:358-363`，见 `ParseDiagnostics.OrphanFeature`）；`AnnotationFormat` 同时支持 GTF 的 `gene_id`/`transcript_id`（`:23`）。
  - 坐标约定：1-based 闭区间（与 `GeneSequenceService.extractInterval` `:251` 一致）；seqid 即第 1 列。
  - gzip：`Gff3Parser.parse()` `:243` 同魔数 sniff。
- **表达矩阵（TSV）**：
  - 元数据：`# unit=...` / `# level=...`（`ExpressionDataService:254-261`）；须在表头前（否则抛错，`:258`）。
  - 首列 feature_id，后续列为样本（`:281`）；列数必须等于表头列数（`:280`）。
  - 空值语义：空 / `NA` / `N/A` / `.` / `NULL` → `missingCells`（`:287`）；`0` → 实测零值（区别于 missing）。负值在 TPM/FPKM/COUNTS 单位下判为非法（`:291`）。
  - ID 匹配：经 `reconcile()`（`:149`）产出 `unmatchedIds`，v1 报告据此发 WARNING。
- **区间注释 / 连续信号（BED3 / BED6 / bedGraph）**：**v1 不导入**（见 §2.2），规范仅收录以备 v2；v1 不做 0-based↔1-based 换算。
- **项目清单（manifest v2）**：在 `ProjectStore.Manifest`（当前 `schemaVersion=3`）基础上扩展，新增「规范版本 / 参考组装标识 / 注释版本 / 文件路径 / SHA-256 / 序列别名映射」，并 bump `schemaVersion`；校验沿用 `ProjectStore` 既有「嵌入字节 + SHA-256 比对」（`:63`/`:142`）。

### 3.2 跨物种兼容核心（PRD 必须体现）

- **序列 ID 与坐标是跨物种兼容的重点**。例：FASTA 用 `Chr1`、GFF3 用 `1` 时，导入界面应列出差异，由用户确认一对一映射并写入项目（依据 `GeneSequenceService.normalizeSequenceName` 与别名机制）。
- **坐标换算示例**：GFF3 的 `Chr1:1–100`（1-based 闭区间）与 BED 的 `Chr1 0 100`（0-based 半开区间）应定位到同一段 100 bp。**v1 仅做 FASTA-ID ↔ GFF3-ID 别名映射；BED↔GFF3 真实换算留 v2**（见 §2.2）。
- **容错而不丢信息**：FASTA 中没有基因注释的序列可以正常保留（不报错）；注释或轨道引用了不存在的序列，则应在导入报告中**定位到具体记录（行号 / 坐标）**（依据 `AnnotationHealthService.HealthIssue.lineNumber` / `ParseDiagnostics.OrphanFeature` 的既有追溯能力）。

---

## 4. 用户故事

| # | 角色 | 用户故事 |
|---|---|---|
| US-1 | 第一次带自己数据的用户 | 作为一个第一次打开 PhytoScope 并想分析自家物种基因组的用户，我希望通过「导入自己的基因组」向导一次性选择 FASTA、GFF3 和表达矩阵，并在加载前得到一份报告，告诉我这些文件能否一起用、哪些序列或基因需要人工核对，以便我不必先碰运气式地导入再逐个排查。 |
| US-2 | 需要人工核对别名的研究者 | 作为一个手头的 FASTA 用 `Chr1` 而 GFF3 用 `1` 命名的研究者，我希望向导列出序列名差异、让我逐对确认 1:1 映射，并把确认结果写入项目文件，以便我重新打开项目时无需再次手动核对，且后续序列提取能正确匹配。 |
| US-3 | 维护者 / CI | 作为一个维护者或 CI 流水线，我希望导入体检同时产出机器可读的 JSON（含 ERROR/WARNING/INFO 与每条问题的行号/坐标）和给人看的 Markdown，以便 CI 能以「存在 ERROR 即不通过」作为门禁，且我能从报告直接定位到需要修改的原始记录。 |

---

## 5. 需求池

优先级定义：**P0 = 必须有（本次上线阻塞）**；**P1 = 应该有（本次尽量完成，可显式登记为遗留）**；**P2 = 可以有（不阻塞本次，含 v2 预留）**。

| ID | 优先级 | 需求 | 验收要点 |
|---|---|---|---|
| **R1** | **P0** | **联合导入向导与流程**：新增独立入口「导入自己的基因组」，与 Example datasets 明确分流；依次选择 FASTA / GFF3 / 表达矩阵（均允许 .gz），复用 `GenomeDataService.loadFromFile` / `GeneSequenceService.loadFromFile` / `ExpressionDataService.loadFromFile`，不重写解析 | 入口独立、菜单文案与 Example datasets 区分；三类文件均能经现有解析底座载入；不改动 `CATALOGUE` 与既有 `importAnnotation/importGenomeFasta/importExpression` 流程（仅新增，不耦合） |
| **R2** | **P0** | **文件内完整性校验**：FASTA / GFF3 / TSV 各自能独立解析、无截断 / 编码错误；暴露 `GeneSequenceService` 的解析异常、`Gff3Parser` 的 `skippedRows` / `OrphanFeature`、`ExpressionDataService` 的 `invalidCells` / 编码错误 | 任一文件解析失败 → 报告 ERROR 并定位；`<9` 列 GFF3 行 / 非法值计入报告；不会因单个坏文件而静默成功 |
| **R3** | **P0** | **文件间引用一致性**：注释 `seqid` ⊆ FASTA 序列集（引用不存在的序列 → ERROR）；表达 `feature_id` 匹配注释或独立（不在注释里 → WARNING，独立表达矩阵可正常放行 → INFO） | 注释引用缺失序列 → ERROR 且定位到具体记录行号；表达 ID 不匹配 → WARNING 且列出未匹配 ID；纯表达矩阵（无注释）不报错 |
| **R4** | **P0** | **序列名覆盖与别名映射 UI**：FASTA-ID ↔ GFF3-ID 差异列出；用户逐对确认 1:1；确认结果写入 project manifest（按项目） | 差异列表可见、可逐对确认；确认后映射进入 manifest v2；重新打开项目后序列名解析与导入时一致（AC-2）；不引入全局别名表（默认） |
| **R5** | **P0** | **报告生成**：JSON（机读，可进 CI 门禁）+ Markdown（人读）双产物；含严重度 ERROR/WARNING/INFO；能定位到具体记录行号 / 坐标；风格与 `SHA256SUMS.txt` / `MANIFEST.json` 一致 | 双产物可导出；ERROR/WARNING/INFO 分类正确；每条问题带可追溯证据（行号/坐标/seqid）；JSON 可被 CI 以「存在 ERROR 即失败」消费 |
| **R6** | **P0** | **manifest v2**：在 `ProjectStore.Manifest`（schemaVersion 3）基础上扩展，记录规范版本 / 参考组装标识 / 注释版本 / 文件路径 / SHA-256 / 序列别名映射；bump schemaVersion 并被 `load()` 接受 | manifest v2 含上述全部字段；`load()` 兼容新版本；保存后重算 SHA-256 与 manifest 记录一致（AC-5） |
| **R7** | **P0** | **验证集**：三类组装（染色体级、碎片化 scaffold、带细胞器序列）+ 负链基因 + 跨格式坐标边界（v1 仅别名映射边界）测试 | 见 §6 AC-1 / AC-3；三类组装 fixture 在仓库内（KB 级，不引入大文件） |
| **R8** | **P1** | **复用 `describeFastaAlignment` 现有 WARNING 逻辑到报告时的口径统一**：把 `MainView:339` 的 FASTA 覆盖率 WARNING 语义，映射到新报告的 ERROR/WARNING/INFO 模型；明确是扩展 `AnnotationHealthService.Severity` 还是新增独立枚举，避免 WARNING 与 NOTICE/NOT_CHECKED 混用 | 报告与现有 `AnnotationHealthService` 的 Severity 口径有书面约定；不造成「同一现象两种严重度」歧义（见 §7 Q1） |
| **R9** | **P2** | **可选：全局别名预设**（仅当用户判断有必要） | 若实现，须明确与「按项目别名」的优先级；默认不实现、不影响 v1 |
| **R10** | **P2** | **v2 预留设计空间**：BED3/BED6 导入与查询、bedGraph 轨道、BED↔GFF3 0-based/1-based 换算的接口占位与测试规划（**明确不在 v1**） | 仅在 PRD 标注为 v2；v1 代码中不实现换算（AC-8） |
| **R11** | **P2** | **CI 门禁集成示例**：提供以「JSON 报告含 ERROR 即以非零退出」消费报告的样例脚本（仅示例，非应用内功能） | 样例脚本可用 `sha256sum`/系统工具，不新增依赖；不联网 |

---

## 6. 关键验收标准（逐条可验证）

| # | 验收标准 | 验证方式 |
|---|---|---|
| **AC-1** | v1 报告能在**三类组装**（染色体级、碎片化 scaffold、带细胞器序列）下生成，且 ERROR / WARNING / INFO 分类正确 | 用 3 个 KB 级 fixture：① 正确数据 → 0 ERROR；② 注释引用 FASTA 不存在的 seqid → ERROR 且定位到记录；③ FASTA 有序列无注释 / 表达 feature_id 未匹配 / 需别名映射 → WARNING 或 INFO。运行向导生成报告并断言分类 |
| **AC-2** | **别名映射写入项目 manifest 后，重新打开 `.phytoscope` 项目仍生效** | 构造 FASTA 用 `Chr1`、GFF3 用 `1` 的 fixture，向导确认 1:1 映射并保存项目；`ProjectStore.load()` 还原后用 `GeneSequenceService.resolveSequenceId` 断言映射生效，序列提取能正确匹配 |
| **AC-3** | **报告能定位到「第 N 行引用了不存在的 seqid」** | 注入一条 GFF3 引用未知 seqid 的记录，断言报告的对应 `HealthIssue` 含正确的 `lineNumber` 与 seqid 证据（复用 `ParseDiagnostics.OrphanFeature` / `AnnotationHealthService.HealthIssue` 既有追溯字段） |
| **AC-4** | **表达 feature_id 缺失能报 WARNING 而非崩溃** | 表达矩阵含不在注释里的 ID → 报告 WARNING 并列出未匹配 ID（`ExpressionDataService.reconcile` 的 `unmatchedIds`）；空表达矩阵 / 纯表达矩阵不报 ERROR、不抛异常 |
| **AC-5** | **Manifest v2 含 SHA-256 且能被重新校验** | 保存项目后，对嵌入字节重算 SHA-256 与 manifest 中记录比对一致（复用 `ProjectStore` 既有 `MessageDigest.getInstance("SHA-256")` / `Source.sha256` 校验逻辑，`:63`/`:142`）；`load()` 接受新 `schemaVersion` |
| **AC-6** | **不引入新依赖、不联网** | `pom.xml` 依赖增量 = 0；全仓 grep 新增代码中无 HTTP 客户端 / 下载入口；向导无「下载 / 获取远程」按钮；构建 `./mvnw -Pui-tests test` 在离线环境通过 |
| **AC-7** | **向导独立于 Example datasets 流程** | 新增独立入口；`ExampleDataset.CATALOGUE` 与既有 `importAnnotation/importGenomeFasta/importExpression` 未被改动或耦合；体检报告在加载项目**之前**生成，存在 ERROR 时阻断加载 |
| **AC-8** | **坐标归一化边界：v1 仅做 FASTA-ID ↔ GFF3-ID 别名映射** | 验证 v1 不实现 BED(0-based)↔GFF3(1-based) 真实换算；相关换算测试归 v2（R10）；v1 别名映射只解决 ID 差异、不做区间换算 |

---

## 7. 风险与待确认问题

### 7.1 已确认事项（非开放问题）

- **报告格式 = JSON + Markdown 双产物**：主理人已确认，v1 直接采用（R5）。
- **跨格式坐标换算留 v2**：明确为已知范围边界，v1 不做 BED↔GFF3 0-based/1-based 换算（§2.2 / AC-8）。
- **别名映射按项目持久化**：默认不引入全局别名表（R4 / R9）。

### 7.2 待确认（Q1–Q2）

| # | 问题 | 现状与建议 |
|---|---|---|
| **Q1（待确认）** | **复用 `describeFastaAlignment` 现有 WARNING 逻辑到报告时的口径统一**：现有 `AnnotationHealthService.Severity` 为 `ERROR/WARNING/NOTICE/NOT_CHECKED`（`:29-34`），而本增量报告模型为 `ERROR/WARNING/INFO`。`describeFastaAlignment()`（`MainView:339`）把「N of M 注释染色体没有 FASTA 记录」判为 WARNING 文本。如何把这条既有 WARNING 语义映射到新模型的 ERROR/WARNING/INFO？是否扩展 `AnnotationHealthService.Severity` 还是为导入报告定义独立枚举？ | 建议：导入报告定义**独立**的严重度枚举（ERROR/WARNING/INFO），不污染既有 `AnnotationHealthService` 的四级模型；`MainView:339` 的 FASTA 覆盖率判断在导入报告中归为 WARNING（可用但需人工核对），并补充「FASTA 有序列无注释」为 INFO 或 WARNING（取决于是否视为异常）。需在架构设计阶段书面约定，避免「同一现象两种严重度」 |
| **Q2（可选 P2）** | **是否要全局别名预设** | 默认不建（按项目即可）。若非模式物种常出现固定命名惯例（如 `chr` 前缀），可后续作为 P2 可选；不影响 v1 |

### 7.3 主要风险

| 风险 | 影响 | 缓解 |
|---|---|---|
| 严重度口径不统一 | `AnnotationHealthService` 四级模型与本报告三级模型混淆，CI 门禁误判 | Q1 在架构设计阶段书面约定；导入报告用独立枚举（建议） |
| 别名映射写入 / 还原不一致 | 重新打开项目后序列名解析失效，序列提取回退到「找不到序列」 | R4 与 AC-2 强约束；复用 `GeneSequenceService.resolveSequenceId` + `normalizeSequenceName` 做还原验证 |
| 大文件进仓库 / 联网 | 偏离「本地、可移植、非模式物种友好」与剥离决议 | AC-6 门禁：依赖增量 = 0、离线构建通过；验证集用 KB 级 fixture |
| 静默成功（坏文件被忽略） | 用户以为导入成功，实则引用缺失 | R2/R3 强制暴露 `ParseDiagnostics` 与解析异常；ERROR 阻断加载（AC-7） |
| 向导与既有流程耦合 | 改动 Example datasets / CATALOGUE，回归旧能力 | R1 仅新增入口，不改动既有 `importAnnotation/importGenomeFasta/importExpression`；AC-7 验证 |
| 报告无法定位具体记录 | 用户拿到报告仍无法修数据 | R5/AC-3 复用 `HealthIssue.lineNumber` / `OrphanFeature` 既有追溯字段，每条问题带行号 / 坐标 |

---

## 8. 交付清单（本次 v1）

1. 新增「导入自己的基因组」向导入口（独立，不与 Example datasets 耦合）。
2. 联合质检逻辑：文件内完整性（复用解析底座 + `ParseDiagnostics`）、文件间引用一致性（seqid ⊆ FASTA、表达 ID 匹配）、序列名覆盖与别名映射 UI。
3. 可导出报告：JSON（机读，CI 门禁）+ Markdown（人读），含 ERROR/WARNING/INFO 与可追溯证据。
4. manifest v2：规范版本 / 参考组装标识 / 注释版本 / 文件路径 / SHA-256 / 序列别名映射（`ProjectStore.Manifest` 扩展，bump schemaVersion）。
5. 验证集与测试：三类组装 + 负链 + 坐标边界（v1 仅别名映射边界）的 fixture 与断言。
6. 验收记录：对应 §6 八条 AC 的验证结果。

> 本文为增量 PRD，不替代既有产品文档；变更落地后请同步更新 `README.md` 与相关 `docs/`（尤其「完整参考数据获取」小节应补充「导入自己的基因组」向导说明）。
