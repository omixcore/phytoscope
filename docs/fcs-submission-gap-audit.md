# PhytoScope 投稿 FCS 差距审计（只读现状盘点）

> 盘点人：software-architect-2（高见远）· 盘点日期：2026-11-14 · 仓库：`G:/WorkBuddy/2026-10-27-22-02-43/phytoscope`（HEAD=`8b6bf0b`，`git describe`=`v0.2.0-16-g8b6bf0b`）+ `phytoscope-reference-data`（HEAD=`5029713`）。
>
> **方法说明**：本次为**严格只读审计**，仅执行 `git log/status/describe/tag`、`grep`、`Read` 与目录列举，未改动任何文件、未 push、未运行会改变状态的命令。所有依赖运行时才能确认的项一律标注「待运行时验证」，不臆断。
>
> **口径说明**：团队主理人给出的「投稿 FCS 工作清单」30 项未以独立文件形式存在于仓库中，本表按主理人 7 组「重点核对资产」+ 任务面板中的 7 阶段路线（阶段1 软件规范与发布 / 阶段4+5 跨物种验证与核心模块金标准 / 阶段6 对比实验与性能·用户验证 / 阶段7 论文与投稿材料）重建为 30 项可落地核对项。若后续有官方 30 项原文，请以原文为准替换本表映射。

**状态图例**：✅ 已完成 · 🟡 部分完成 · ❌ 缺失 · ⏳ 待运行时验证（不计入❌，但尚未实测）

---

## 一、软件规范与发布（阶段1）

| # | 检查项 | 状态 | 证据 | 缺口 / 说明 |
|---|--------|------|------|-------------|
| 1 | 输入格式规范文档 | ✅ | `docs/input-format-specification.md`（提交 `4f401e1`，约 161 行）。覆盖：Reference FASTA 解析（空白分隔/去重/歧义碱基）、Annotation（GFF3 9 列 1-based inclusive、GTF、多 parent、不连续特征、越界=ERROR）、Expression（TSV/CSV/分号、unit/level、measured-zero vs missing）、Sequence aliases、Joint import/validation（JSON/MD/TSV 三种报告）、Project persistence + FASTA rebinding（哈希校验）、Acceptance criteria | 文档状态自标「proposed」。BED3/BED6 与 bedGraph 整段标记 **deferred（未实现）**；「nonstandard genetic codes / organellar translation」仅在验收项中要求「先测试再宣称」，未见实现。 |
| 2 | 应用版本号一致性 | ✅ | `pom.xml` `<artifactId>phytoscope</artifactId><version>0.2.0</version>`；README 发布链接「PhytoScope v0.2.0」；CHANGELOG「## 0.2.0」 | 三处一致，无冲突。 |
| 3 | pom「3.2.5」版本线澄清 | 🟡 | `pom.xml` 第 7–12 行 `spring-boot-starter-parent` `<version>3.2.5</version>` | **3.2.5 是 Spring Boot 父 POM 依赖管理版本，不是应用版本**（应用版本是 0.2.0）。主理人清单把「pom 版本 3.2.5」与应用 0.2.0 并列，属易混点——审计结论为「无版本冲突」，但建议在 README/设计文档显式写清「Spring Boot 3.2.5 是构建底座，非发布版本号」。 |
| 4 | 正式 Git tag | 🟡 | `git tag -l` 仅 `v0.2.0`；`git describe`=`v0.2.0-16-g8b6bf0b`（HEAD 领先 v0.2.0 **16 个提交**） | v0.2.0 tag 存在但严重滞后：其后的 gz 多文件解析修复、薄荷完整数据、瘦身剥离、intake 健康检查、输入规范等均**未打新 tag**（缺 v0.2.1 / v0.3.0）。README 仍指向 v0.2.0 下载，当前树实际领先 16 提交，**发布面与代码面已脱节**。 |
| 5 | GitHub Release 附件 + SHA-256 | ✅ | `desktop_program/SHA256SUMS.txt` 记录 `PhytoScope-Windows-AMD64.zip` SHA-256=`763b1c42…`；`docs/desktop-verification.md` 记录附件 110,405,267 字节、built from v0.2.0 | 附件是 v0.2.0 时代（含完整薄荷数据），**早于数据瘦身**；当前树的「KB 级示例」版尚未重建发布附件。 |
| 6 | 软件 DOI / 正式引用 | ❌ | README「Citation」段仅 `Song X, Chen P, Liang F. PhytoScope for portable local queries of plant genome data. Concise Report.`，无 DOI/Zenodo/版本化标识 | **缺软件 DOI**（Zenodo/DataCite 等）。引用里作者名与本仓库提交身份（gitee omixcore / github omixcore）的一致性也需复核。 |
| 7 | LICENSE | ✅ | 根目录 `LICENSE`（MIT，1065 B）；README 徽章 `license-MIT` | 齐全。 |
| 8 | CHANGELOG | ✅ | `CHANGELOG.md`：「## 0.2.0」（16 条）+「## 2026-10-16 — Validation hardening」（5 条） | 覆盖到 2026-10-16；**之后**（gz 修复、薄荷、瘦身、intake、输入规范）未回填 CHANGELOG。 |
| 9 | README 完整性 | ✅ | README 约 325 行：下载、6 个 KB 级示例表、完整参考数据说明、引用、许可、基准命令、序列细节 | 内容详尽；「完整参考数据」段落仍写「remote URLs are pending user provision」，与 reference-data README 的「已推送双镜像」**存在表述不一致**。 |
| 10 | CI 三平台工作流 | 🟡 | `.github/workflows/validate.yml`：`matrix.os=[ubuntu-latest, windows-latest, macos-latest]`，含 bedtools/BLASTP/gffutils/Vigna/四物种基准/打包 | 工作流**定义**了三平台与完整验证链；但当前 HEAD（瘦身+intake 之后）的**最近一次三平台实跑结果本仓库无落地文件**，判「待运行时验证」。另有独立 `public-data-validation` job 依赖外部网络（PMC/PastDB/ENA），历史上曾因网络不可达失败。 |

## 二、桌面分发与三系统验证（阶段1）

| # | 检查项 | 状态 | 证据 | 缺口 / 说明 |
|---|--------|------|------|-------------|
| 11 | Windows 打包文档 | ✅ | `docs/desktop-distribution.md`（jpackage app-image + `tools/package_desktop.py`、布局表、发布清单） | 文档与脚本齐备；`dist/` 下遗留 `PhytoScope-Windows-0.2.0.zip` / `PhytoScope-Windows-AMD64.zip`（两个命名版本，需统一）。 |
| 12 | Windows x64 桌面实测 | ✅ | `docs/desktop-verification.md`：ZIP CRC PASS、布局 PASS、打包 runtime smoke（`--benchmark --example rice`）PASS、SHA-256 自检 OK、149 测试 0 失败 | 仅 **Windows x64 一套**实测；Monocle 无头测试不认证原生对话框/DPI/驱动/SmartScreen（文档自述）。 |
| 13 | macOS / Linux 桌面实测 | ⏳ | CI `validate.yml` 会构建三平台 app-image 并上传 artifact | 无 `desktop-verification` 级别的 macOS/Linux **原生启动/导入/导出**人工核查记录 → **待运行时验证**。 |

## 三、参考数据仓库可追溯性

| # | 检查项 | 状态 | 证据 | 缺口 / 说明 |
|---|--------|------|------|-------------|
| 14 | 独立数据仓库 + 双镜像 | ✅ | `phytoscope-reference-data/`：2 提交（`acac2cf` 归档 9 文件、`5029713` 回填镜像 URL）；README 记录 gitee/github 同名镜像 | 主仓库 `reference-provenance.json` 的 `external_repository`/`moved_at_commit` 已回填；但主仓库 README 段落仍写「remote URLs pending」。 |
| 15 | 机读清单（MANIFEST + SHA256SUMS） | ✅ | `MANIFEST.json`：9 文件、totals 68,518,847 B、逐文件 bytes/sha256/source_url/source_release/license/baseline_blob；`SHA256SUMS.txt`：9 行 `<64hex>  <path>` | 实测 `data/` 总字节 68,518,847 与 MANIFEST 完全一致，9 文件逐一存在且大小吻合。 |
| 16 | 数据来源 / 许可 | 🟡 | `LICENSES.md` 逐条 9 项；MANIFEST 中 9 项 `license_status` **全部为 `needs_confirmation`** | 许可**全部待确认**，尤其：薄荷 4 叶 TPM（`mentha_longifolia_expression.tsv`）无独立下载 URL/文库编号（README §7 自认），figshare 条目 CC BY 4.0 需复核原发布页。**投稿前必须逐条落定许可**。 |
| 17 | 校验 / 取回脚本 | ✅ | `scripts/verify.sh|ps1`、`verify-against-baseline.sh|ps1`（读 git 历史 blob 比对 8fd01ca）、`restore-to-app.sh|ps1`、`init-and-push.sh|ps1` | 齐备；README §2 提供 `sha256sum -c SHA256SUMS.txt` 自助校验。 |

## 四、验证与金标准（阶段4+5）

| # | 检查项 | 状态 | 证据 | 缺口 / 说明 |
|---|--------|------|------|-------------|
| 18 | 金标准验证总览 | ✅ | `docs/validation-and-benchmarks.md`：4 层验证（源身份 / 独立 Python 坐标核对 / JavaFX 行为 / 性能）、gffutils 0.13 同数据提取基线、四物种匹配提取基准、bedtools/BLASTP 模块级核对、PIF6 异构体身份门 | 文档自述大量 MB 级数字为 `8fd01ca` 时代**历史值**，当前树复现需外迁数据。 |
| 19 | 染色体验证 | ✅ | `docs/chromosome-validation.md`：Arabidopsis/Rice/Maize/Soybean 四物种 Ensembl Plants release 62 完整 GFF3 导入（核染色体 5/12/10/20，特征 79.1万~132.9万）、参考验收审计（assembly accession 表）、`AdaptiveChromosomeTest`/`AdaptiveChromosomeUiTest`/`FullAnnotationValidationTest` | `FullAnnotationValidationTest` 需 `-Dphytoscope.fullAnnotationDir` 才真正跑全注释（0 个 `@Test` 注解，条件门控）；默认小夹具可离线跑。 |
| 20 | 非模式物种可移植性 | ✅ | `docs/non-model-portability-evidence.md`：rice bean *Vigna umbellata* GCF_029865625.1 完整 GFF3/GTF + 5 位点序列金标 + Salmon 2.8.0 重定量（34,914 基因，0 未匹配 ID）；另 2 个 RefSeq（Andrographis、Hibiscus）；25 条多基因座金标 | 自认边界清晰：表达验证 genotype（IC426787）≠ 参考栽培种（VRB3）；金标覆盖 5 位点/物种非全基因组；manual/IGV/JBrowse 同任务对比**未执行**。 |
| 21 | 研究模块金标准 | ✅ | `docs/research-module-validation.md`：7 模块（启动子/QC/同源/跨物种/表达分组/domain-CDS）显式参考计算 + `ResearchModuleGoldSetTest` + CI 三平台通过记录 | 自认「单一家族烟雾测试」，非普适精度/召回；表达仅单 assay，非生物学结论。 |
| 22 | 可移植性与可复现 | ✅ | `docs/portability-and-reproducibility.md`：可移植矩阵（8 数据集）、可复现 `.phytoscope` 项目（schemaVersion/SHA-256/导入报告）、表达关联、数据质量报告表、性能证据表 | 性能数字为历史值（mint-genome 已外迁）；「未来工作」明确列多实验分组/时间序列未实现。 |
| 23 | 独立金标准工具链 | ✅ | `tools/`：`compare_gffutils.py`、`validate_structure_inventory.py`、`compare_homology_blastp.py`（BLASTP 2.12.0+）、`compare_promoters_bedtools.py`（bedtools 2.31.1）、`compare_public_case.py`、`build_nonmodel_sequence_gold.py`、`validate_pif6_isoform_identity.py` | 工具链完整；`benchmarks/results/2026-10-18/` 有 cross-species-extraction/homology/research-module-validation 三份报告落地。 |

## 五、核心分析模块实现与验证

| # | 检查项 | 状态 | 证据 | 缺口 / 说明 |
|---|--------|------|------|-------------|
| 24 | Promoter / interval 提取 | ✅ | 实现：`research/IntervalAnalysisService.java` + `GenomicInterval.java`；独立金标准：`tools/compare_promoters_bedtools.py`（8 个 TAIR10 启动子区间 vs bedtools 2.31.1，8/8 精确匹配） | 金标准仅 8 区间/单染色体，规模小，非性能结论（文档自认）。 |
| 25 | Sequence QC | ✅ | 实现：`research/SequenceQualityService.java`；金标准：`ResearchModuleGoldSetTest`（4 条真实 RBCS CDS，GC/碱基/帧/起止/flag 重建） | 仅 4 条 CDS 正例；缺负例/异常碱基大规模覆盖。 |
| 26 | Homology / 蛋白相似性 | ✅ | 实现：`research/ProteinHomologyService.java`；金标准：`compare_homology_blastp.py` + `ResearchModuleGoldSetTest`（4×RBCS 同家族 + 2 无关对照；与 BLASTP 12/12 + 8/8 = 20/20 一致） | 单一 RBCS 家族、负例分类不平衡，非 orthology 基准、非普适敏感度/特异度（文档自认）。 |
| 27 | Domain 映射 | ✅ | 实现：`research/DomainLinkService.java`；金标准：`DomainCaseValidationTest` + `tools/validate_domain_isoforms.py`（16 条 domain/deletion 记录/13 基因，含多外显子 junction） | 覆盖坐标与 provenance，不证明异构体功能（文档自认）。 |
| 28 | Expression | ✅ | 实现：`service/ExpressionDataService.java` + `research/SampleGroupingService.java` + `model/ExpressionProfile.java`；测试：`ExpressionDataServiceTest`/`ExpressionDelimiterTest`/`ExpressionIntegrityTest` + rice bean Salmon 重定量 reconciliation + E-MTAB-7978 | 金标准表达验证=单 assay/单条件，无生物学差异分析；「多实验分组/时间序列」未实现。 |
| 29 | Comparison | ✅ | 实现：`comparison/ComparisonFigure.java` + `cli/ComparisonRunner.java` + `ui/component/ComparisonPane.java`；测试：`ComparisonFigureTest`/`ComparisonRunnerTest`/`ComparisonWorkflowUiTest` | 已覆盖 PNG/SVG/TSV 导出与布局；用户同任务对比研究未做。 |
| 30 | Project 持久化 | ✅ | 实现：`project/ProjectStore.java` + `project/ReproduceExporter.java`；测试：`ProjectStoreTest`/`ProjectStoreAliasRestoreTest`/`ReproduceExporterTest` + `IntakeManifestMeta`/`Assembly`/`AliasMapping`（Manifest v2 schemaVersion 4） | 重开/别名恢复/哈希校验已覆盖；缺跨机器移动/修改引用的端到端原生 UI 实测（可复现矩阵中文档已列）。 |

---

## 六、核心结论

1. **已基本就绪、只需复核（约 20/30）**：输入规范（#1）、版本一致性（#2/#3）、LICENSE（#7）、Windows 打包与实测（#11/#12）、参考数据机读清单（#14/#15/#17）、六份验证文档（#18–#23）、7 个分析模块实现+金标准（#24–#30）均已落地且有**具体文件/提交/测试类可查**。这些属于「材料齐、待作者复核措辞与许可」。

2. **明确缺口、需补齐（4 项）**：① **软件 DOI 缺失（#6）**——需挂 Zenodo/DataCite 并版本化；② **tag/Release 严重滞后（#4/#5）**——HEAD 领先 v0.2.0 共 16 提交，当前树（瘦身+intake）无新 tag、无新版发布附件；③ **许可全待确认（#16）**——9 个数据文件 `license_status` 全为 `needs_confirmation`，薄荷 TPM 甚至无来源 URL，投稿前必须逐条落定；④ **CHANGELOG 未回填（#8）** 与 **README「remote URLs pending」表述过时（#9）**。

3. **需新增实测（⏳/🟡 类，约 6 项）**：macOS/Linux 桌面原生启动实测（#13）、当前 HEAD 的 CI 三平台实跑结果落地（#10）、BED/bedGraph 规范项（#1 内 deferred，如需宣称则需实现+测试）、非标准遗传密码/细胞器翻译（#1 验收项，尚未实现）、更大规模的多家族/负例金标准（#25/#26 的泛化）、以及对比实验与用户研究（#20 自认的 manual/IGV/JBrowse 同任务对比未执行）。

4. **执行优先级建议（相对当前任务面板）**：把「阶段1」内 **tag + Release + DOI + 许可** 四项提为 P0（投稿硬门槛、纯物料操作、不涉代码）；**CHANGELOG 回填 + README 表述统一** 为 P1（低成本消除不一致）；**macOS/Linux 原生实测、当前树 CI 绿、BED/bedGraph 决策** 为 P2（需要运行时或实现投入）；**用户研究/更大金标准/对比实验**维持阶段6/7，不阻塞投稿但需在文中如实标注「未执行」而非留白。

> 待运行时验证汇总：`#10`（当前 HEAD 三平台 CI 实跑）、`#13`（macOS/Linux 原生桌面）、`#1` 中的 BED/bedGraph 与遗传密码项、`#25/#26` 的更大金标准泛化、`#20` 的 manual/浏览器对比。以上均未在本仓库落地可查结果，故不标 ✅。
