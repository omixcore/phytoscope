# PhytoScope FCS · 参考文献逐条核查表（R12，C 类）

> 批次：B03 · 执行人：工程师寇豆码（Kou）· 核查日期：2026-11-14（ISO-8601 UTC 口径同设计 §7.10）
> 权威规格：`docs/design-fcs-final-delivery.md` §5 B03 #3、§3.1、§7.3；增量 PRD R12。
> 核查对象：`docs/fcs-paper-outline.md` §7 的 **16 条** draft（✅/⏳ 标注）。
> **状态口径**：R12 属 **C 类**（结论成文 + 逐条标注确证渠道与无法确证项）。**不臆造文献、不把
> `unverifiable` 判为 `confirmed`**。本机本次可访问 Crossref REST / DataCite REST / ENA Browser API /
> figshare 与 Sequence Ontology 的 GitHub 规范页；凡访问成功且元数据匹配者才标 `confirmed` 并写明渠道，
> 凡无 DOI / 无出版商机读记录 / 网络未能确证者一律标 `unverifiable` 并写明原因。

---

## 1. 核查渠道与本次可达性（如实）

| 渠道 | 本次是否可达 | 用途 |
|---|---|---|
| Crossref REST `api.crossref.org/works/{doi}`、`/works?query.*` | ✅ 可达 | 期刊论文 DOI / 作者 / 卷期页 / 年份确证 |
| DataCite REST `api.datacite.org/dois/{doi}` | ✅ 可达 | figshare 数据集 DOI / 标题 / rights（CC BY 4.0）确证 |
| ENA Browser API `ebi.ac.uk/ena/browser/api/xml/PRJNA765494` | ✅ 可达 | BioProject 登录号元数据确证 |
| Sequence Ontology GFF3 规范 `github.com/The-Sequence-Ontology/Specifications/blob/master/gff3.md` | ✅ 可达（HTML 页） | 规范文档存在性；**无 DOI、无卷期页、无出版商记录** |
| `www.sequenceontology.org/resources/gff3.html` | ❌ HTTP 404 | 旧规范 URL 失效，不作为确证渠道 |
| JavaFX / OpenJDK 平台「文献」 | ❌ 无 DOI；本次 Crossref 查询 SSL 握手超时 | 平台引用，非期刊文献 |
| doi.org / 出版商全文页 | 本次未逐一打开 | 核查以 Crossref/DataCite 机读元数据为准；**未打开全文页不冒充「已读全文」** |

> 说明：本次**未访问付费出版商全文**，因此 `confirmed` 仅表示「DOI 在 Crossref/DataCite 解析成功、
> 标题/作者/卷期页/年份与 draft 一致」，**不表示已核对全文内容**。

---

## 2. 16 条逐条核查表

列定义：`编号 → 文献 → DOI → 作者 → 支撑的论文论断 → 核查渠道 → 结论`。

| # | 文献 | DOI | 作者（第一作者） | 支撑的论文论断 | 核查渠道 | 结论 |
|---|---|---|---|---|---|---|
| 1 | TAIR — Lamesch P et al. (2012) *The Arabidopsis Information Resource (TAIR): improved gene annotation and new tools.* Nucleic Acids Res 40:D1202–D1210 | `10.1093/nar/gkr1090` | Lamesch | 拟南芥 TAIR10 参考基因组 / 注释来源；Materials §2.3 模型作物数据来源 | Crossref `works/10.1093/nar/gkr1090`：NAR 40:D1202–D1210 (2012)，标题、作者、卷页年一致 | `confirmed` |
| 1a | **TAIR 2024 更新**（outline §7 原为 ⏳「likely Reiser et al., Genetics 2024」）— *The Arabidopsis Information Resource in 2024.* GENETICS 227(1):iyae027 | `10.1093/genetics/iyae027` | Reiser | 补充 TAIR 资源最新引用 | Crossref `works?query.bibliographic=...` 命中 `10.1093/genetics/iyae027`，Reiser，GENETICS 227(1), 2024；`works/{doi}` 复核一致 | `confirmed`（原 ⏳ 已确证，卷期页为 `227(1):iyae027`） |
| 2 | Araport11 — Cheng CY et al. (2017) *Araport11: a complete reannotation of the Arabidopsis thaliana reference genome.* Plant J 89:789–804 | `10.1111/tpj.13415` | Cheng | 拟南芥 Araport11 注释来源；Materials §2.3 / §2.4 | Crossref `works/10.1111/tpj.13415`：The Plant Journal 89:789–804 (2017) | `confirmed`（原 ⏳ 已确证） |
| 3 | Ensembl Plants / Ensembl Genomes 2022 — Yates AD et al. (2022) *Ensembl Genomes 2022: an expanding genome resource for non-vertebrates.* Nucleic Acids Res 50(D1):D996–D1003 | **`10.1093/nar/gkab1007`** | Yates | 四模式作物 Ensembl Plants release 62 GFF3 来源；Materials §2.3 | Crossref `works?query.title=Ensembl Genomes 2022` 命中 `10.1093/nar/gkab1007`；`works/{doi}`：NAR 50(D1):D996–D1003 (2022)，Yates | `confirmed`，**但发现 draft 的 DOI 错误**（见 §3） |
| 3a | 「Ensembl release 62 映射到本仓库所用四物种 GFF3」这一**具体数据映射关系** | 无 DOI | — | Materials §2.3 表「Ensembl Plants release 62」 | Crossref/DataCite 无该映射的机读记录；仅仓库内 `docs/chromosome-validation.md` 有文字留痕 | `unverifiable`（受网络/资源限制未能确证：无 DOI/Crossref/出版商机读记录，仅仓库内证据） |
| 4 | EBI Expression Atlas / Mergner — Mergner J et al. (2020) *Mass-spectrometry-based draft of the Arabidopsis proteome.* Nature 579:409–414 | `10.1038/s41586-020-2094-2` | Mergner | E-MTAB-7978 表达图谱关联文献；Materials §2.3；数据仓库 `MANIFEST.json` | Crossref `works/10.1038/s41586-020-2094-2`：Nature 579:409–414 (2020)，Mergner | `confirmed` |
| 5 | Horsemint genome — Lieberman, Henry & Comai (2025), *Chromosome-scale diploid unphased assembly of horsemint (Mentha longifolia)*, figshare | `10.6084/m9.figshare.28457522`（`.v3` 同） | Lieberman（DataCite creators） | 薄荷完整注释来源；CC BY 4.0 许可；Figure 5(c) / 补表 S1 | DataCite `dois/10.6084/m9.figshare.28457522` 与 `.v3`：标题一致、publisher=figshare、rights=`Creative Commons Attribution 4.0 International` | `confirmed`（DOI + CC BY 4.0 均已确证） |
| 6 | Rice-bean RNA-seq — BioProject `PRJNA765494`（SRA） | **无 DOI** | 提交方：ICAR-National Bureau of Plant Genetic Resources, New Delhi | Materials §2.3 / §2.6 Salmon 重定量数据来源 | ENA Browser API `xml/PRJNA765494` 返回 PROJECT_SET：TITLE `Vigna umbellata Raw sequence reads`、SECONDARY_ID `SRP339385`、描述含 IC426787 / IC552985 5-DPA & 10-DPA pod | `unverifiable`（作为**期刊文献**无 DOI / 无原始出版物；登录号记录可达但**不据此判为文献 confirmed**；按登录号 + 提交方引用） |
| 7 | gffutils — Dale R（软件，PyPI/GitHub） | **无 DOI** | Dale | §2.6 / Figure S1 gffutils 0.13 结构导入基线 | 无 Crossref 记录；**注意** `10.12688/f1000research.23297.2` 是 Pertea & Pertea (2020) *GFF Utilities: GffRead and GffCompare*，属**另一款工具**，不可用来代引 gffutils | `unverifiable`（无 DOI；删除 draft 中「optionally Pertea & Pertea 2020」的代引，避免张冠李戴） |
| 8 | Biopython — Cock PJA et al. (2009) *Biopython: freely available Python tools for computational molecular biology and bioinformatics.* Bioinformatics 25:1422–1423 | `10.1093/bioinformatics/btp163` | Cock | §2.6 / Figure S1 Biopython 1.88 SeqIO 基线 | Crossref `works/10.1093/bioinformatics/btp163`：Bioinformatics 25:1422–1423 (2009) | `confirmed` |
| 9 | bedtools — Quinlan AR & Hall IM (2010) *BEDTools: a flexible suite of utilities for comparing genomic features.* Bioinformatics 26:841–842 | `10.1093/bioinformatics/btq033` | Quinlan | §2.4 / Figure 3 promoter 8/8 独立基线 | Crossref `works/10.1093/bioinformatics/btq033`：Bioinformatics 26(6):841–842 (2010) | `confirmed` |
| 10 | BLAST/BLAST+ — Camacho C et al. (2009) *BLAST+: architecture and applications.* BMC Bioinformatics 10:421 | `10.1186/1471-2105-10-421` | Camacho | §2.4 / Figure 3 homology 20/20 独立基线 | Crossref `works/10.1186/1471-2105-10-421`：BMC Bioinformatics 10(1):421 (2009) | `confirmed` |
| 11 | Salmon — Patro R et al. (2017) *Salmon provides fast and bias-aware quantification of transcript expression.* Nat Methods 14:417–419 | `10.1038/nmeth.4197` | Patro | §2.3 / §3.2 水稻豆重定量 34,914 基因 | Crossref `works/10.1038/nmeth.4197`：Nature Methods 14:417–419 (2017) | `confirmed` |
| 12 | JBrowse 2 — Diesh C et al. (2023) *JBrowse 2: a modular genome browser with views of synteny and structural variation.* Genome Biol 24:74 | `10.1186/s13059-023-02914-z` | Diesh | §3.4 / Figure S1 定性对照工具 | Crossref `works/10.1186/s13059-023-02914-z`：Genome Biology 24(1):74 (2023) | `confirmed`（原 ⏳ 已确证） |
| 13 | IGV — Robinson JT et al. (2011) *Integrative genomics viewer.* Nat Biotechnol 29:24–26 | `10.1038/nbt.1754` | Robinson | §3.4 / Figure S1 定性对照工具 | Crossref `works/10.1038/nbt.1754`：Nature Biotechnology 29:24–26 (2011) | `confirmed` |
| 14 | TBtools — Chen C et al. (2020) *TBtools: An Integrative Toolkit Developed for Interactive Analyses of Big Biological Data.* Mol Plant 13:1194–1202 | `10.1016/j.molp.2020.06.009` | Chen | §3.4 / Figure S1 定性对照工具 | Crossref `works/10.1016/j.molp.2020.06.009`：Molecular Plant 13(8):1194–1202 (2020) | `confirmed` |
| 15 | GFF3 specification — Stein L (2013) / Sequence Ontology | **无 DOI** | 无出版商机读署名 | §2.2 输入格式 / 补图1 坐标约定 | `github.com/The-Sequence-Ontology/Specifications/blob/master/gff3.md` 可达；`sequenceontology.org/resources/gff3.html` 返回 404；Crossref 无卷期页记录 | `unverifiable`（受网络/资源限制未能确证：无 DOI、无卷期页、无出版商机读记录；仅以规范 URL 形式引用，不作为期刊文献） |
| 16 | JavaFX / OpenJDK 平台引用（outline §7 注明 optional） | **无 DOI** | 无 | §2.1 实现技术栈 | Crossref 查询 SSL 握手超时；JavaFX 为平台/框架，非期刊论文，本无 DOI | `unverifiable`（受网络限制未能确证；**按 FCS 期刊风格决定是否保留，本次不作为编号文献保留**） |

**统计**：outline §7 的 **16 条** draft 编号中，`confirmed` **12 条**（#1、#2、#3、#4、#5、#8、#9、#10、#11、#12、#13、#14）、`unverifiable` **4 条**（#6、#7、#15、#16）。本表另设 2 个核查子项：#1a（TAIR 2024，新增 `confirmed`）与 #3a（release-62 映射关系，`unverifiable`），合计 18 行。故全表 `confirmed` 13 行、`unverifiable` 5 行。#3a 为「映射关系」而非独立文献，#6/#7/#15/#16 无 DOI。以上均为本次真实渠道结果，**未把任何 `unverifiable` 写成 `confirmed`、未臆造卷期页或作者**。

---

## 3. 发现并修正的 draft 错误（如实）

1. **#3 DOI 错误**：`docs/fcs-paper-outline.md` §7 原写「Yates AD et al. (2022) … Nucleic Acids Res 50:D996–D1003 ⏳ (confirm release-62 mapping)」，但 draft/旧稿引用的 `10.1093/nar/gkab1042` 经 Crossref 实查为 **Dey et al. (2021) *In vivo architecture of the telomerase RNA catalytic core*, NAR 49:12445–12466**（完全不同论文）。正确 DOI 为 **`10.1093/nar/gkab1007`**。已在 outline §7 与本表更正；**未沿用错误 DOI**。
2. **#7 代引风险**：draft 写「optionally Pertea & Pertea 2020 *GFF utilities*」。实查 `10.12688/f1000research.23297.2` 为 **GffRead/GffCompare**（F1000Research 9:304），与 `gffutils`（Dale R，PyPI/GitHub）**不是同一软件**。已删去该代引，改为按软件名 + 仓库 URL 引用。
3. **#15 规范 URL 失效**：`sequenceontology.org/resources/gff3.html` 返回 HTTP 404；可用规范页为 Sequence Ontology 的 GitHub `gff3.md`。已如实记录。
4. **#1a 新增确证**：outline §7 的「TAIR 2024 *Genetics*」原标 ⏳ 待确认，本次经 Crossref 确证为 `10.1093/genetics/iyae027`（Reiser，GENETICS 227(1), 2024）。

> **删除 / 降级处理**：#3a、#6、#7、#15、#16 五条无法核实项**已从「可作为已确证期刊文献引用」的集合中移除**：
> - #6 改按 BioProject 登录号 + 提交方引用（不冒充论文）；
> - #7 改按软件名 + 仓库 URL 引用（不代引 Pertea & Pertea）；
> - #15 改按规范 URL 引用（不冒充 Stein 2013 期刊文献）；
> - #16 平台引用不在编号文献中保留；
> - #3a 只在正文写「仓库内留痕」，不宣称 Crossref 确证。
> 为保持正文 in-text 引用编号稳定，`outline §7` 与 manuscript「Reference numbering key」**保留 [1]–[16] 编号槽位**，
> 但把上述条目显式标注为 `unverifiable / not used as a confirmed citation`，并写明原因。

---

## 4. 与 outline §7 / manuscript 编号键的同步

- `docs/fcs-paper-outline.md` §7：✅/⏳ 图例改为「✅ = Crossref/DataCite 机读元数据已确证 · ⚠️ = `unverifiable`（无 DOI / 网络未能确证）」；逐条回填上表结论；删除错误 DOI 与 Pertea & Pertea 代引；新增 #1a。
- `docs/fcs-paper-manuscript-draft.md`「Reference numbering key」：状态列由 `to confirm` / `confirmed` 改为
  `confirmed` / `unverifiable`，与本表一一对应；正文 in-text 引用编号不变。
- 两处均**不新增任何未经核查的文献**，不把 `unverifiable` 写成 `confirmed`。

---

## 5. C 类边界声明

- 本表是**核查结论 + 逐条渠道标注**，不是「全部文献已确证」。
- `confirmed` 仅指 DOI 元数据在 Crossref/DataCite 解析成功且与 draft 一致；**未阅读出版商全文**，
  不表示已核对论文内部结论与本文论断的逐字支撑关系。
- 「支撑的论文论断」一列描述的是**本文拟引用它支撑什么**，属于引用规划，不是「该论文已证明本文结论」。
- 网络/权限限制导致 #3a、#6、#7、#15、#16 未能确证；若作者后续可访问出版商页面 / 取得原始出版物，
  可升级为 `confirmed`，**本次不预先写入**。
