# PhytoScope FCS · 参考数据许可结论定稿（R3，B 类）

> 批次：**B04** · 执行人：工程师寇豆码（Kou）· 定稿/取证日期：**2026-11-14**（ISO-8601 UTC 口径同设计 §7.10）
> 权威规格：`docs/design-fcs-final-delivery.md` §5 B04（817–849 行）、§3.3；增量 PRD R3 / §6.1 A2·A3·A4 / §8；主理人第三轮裁定 1/2/3。
> 对应物料：`phytoscope-reference-data/LICENSES.md`、`MANIFEST.json`（8 条逐条一一对应）、
> `docs/zenodo-deposit-checklist.md` §5。

---

## 0. 醒目声明（不得删改）

> **现有数据归档包不构成已具备公开再分发条件的证据；8 项许可未落定前，Deposit B 不得设为公开可下载。**
>
> **结论 ≠ 已获授权；本机不能代替权利人授予许可。**
> 本文件的 8 条「三选一结论」是**再分发决策定稿**，不是「已取得授权」「许可问题已解决」或
> 「已获书面许可」。每一条的 `conclusion_is_authorization = false`。取得书面许可、Zenodo 发布、
> DOI 回填、薄荷 TPM 的最终去留**均不由本机完成**，属作者 / 主理人动作（B 类）。
>
> **本次未删除任何文件、未改变基线**：9 个数据文件 / 68,518,847 B / `SHA256SUMS.txt` /
> `scripts/**` / 逐文件 `sha256` / `bytes` / `path` / `baseline_blob` 全部不动，
> `sha256sum -c SHA256SUMS.txt` 仍 9/9 OK；`MANIFEST.json` 的 `totals.files = 9`、
> `totals.bytes = 68518847`、`len(files) = 9` 保持不变（`totals_unchanged = true`）。
> **公开归档包为 8 个文件 / 65,124,010 B**（见 §1 #8 与 §4）——仓库口径与公开包口径是两件事。

---

## 1. 8 条结论主表（唯一权威，字段同设计 §3.3）

> `id` 1–8 对应 `MANIFEST.json` 中 8 条 `license_status = needs_confirmation` 的文件；
> `path` 与 `MANIFEST.json` 的 `path` **完全一致**。另有 1 条已确证（horsemint 注释 CC BY 4.0）
> 不列入本表，见 §2.3。

| `id` | `path` | `group` | `decision` | `official_terms_url` | `evidence_date` | `conclusion` | `author_action_id` | `conclusion_is_authorization` | `baseline_changed` | `zenodo_license_effect` |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | `data/species/arabidopsis/Chr1.gff3.gz` | `Araport11-Chr1-5` | `author_authorization_required` | `https://phoenixbioinformatics.atlassian.net/wiki/spaces/COM/pages/42216430/TAIR+Terms+of+Use` | 2026-11-14 | 需向 Phoenix/TAIR 申请书面许可，或后续版本再替换为 NCBI RefSeq（CP002684–CP002688 / INSDC）；**本次不替换** | A2 | `false` | `false` | Deposit B 选 Other（多许可）+ 引用 `LICENSES.md`；不得归并 CC BY 4.0 |
| 2 | `data/species/arabidopsis/Chr2.gff3.gz` | `Araport11-Chr1-5` | `author_authorization_required` | `https://phoenixbioinformatics.atlassian.net/wiki/spaces/COM/pages/42216430/TAIR+Terms+of+Use` | 2026-11-14 | 同 #1；需向 Phoenix/TAIR 申请书面许可，或后续版本再替换 NCBI RefSeq CP002684–CP002688；**本次不替换** | A2 | `false` | `false` | 同 #1 |
| 3 | `data/species/arabidopsis/Chr3.gff3.gz` | `Araport11-Chr1-5` | `author_authorization_required` | `https://phoenixbioinformatics.atlassian.net/wiki/spaces/COM/pages/42216430/TAIR+Terms+of+Use` | 2026-11-14 | 同 #1；需向 Phoenix/TAIR 申请书面许可，或后续版本再替换 NCBI RefSeq CP002684–CP002688；**本次不替换** | A2 | `false` | `false` | 同 #1 |
| 4 | `data/species/arabidopsis/Chr4.gff3.gz` | `Araport11-Chr1-5` | `author_authorization_required` | `https://phoenixbioinformatics.atlassian.net/wiki/spaces/COM/pages/42216430/TAIR+Terms+of+Use` | 2026-11-14 | 同 #1；需向 Phoenix/TAIR 申请书面许可，或后续版本再替换 NCBI RefSeq CP002684–CP002688；**本次不替换** | A2 | `false` | `false` | 同 #1 |
| 5 | `data/species/arabidopsis/Chr5.gff3.gz` | `Araport11-Chr1-5` | `author_authorization_required` | `https://phoenixbioinformatics.atlassian.net/wiki/spaces/COM/pages/42216430/TAIR+Terms+of+Use` | 2026-11-14 | 同 #1；需向 Phoenix/TAIR 申请书面许可，或后续版本再替换 NCBI RefSeq CP002684–CP002688；**本次不替换** | A2 | `false` | `false` | 同 #1 |
| 6 | `data/species/arabidopsis/TAIR10_genomic.fna.gz` | `TAIR10-FASTA` | `author_authorization_required` | `https://www.ensembl.org/info/about/legal/disclaimer.html` | 2026-11-14 | Ensembl 官方 disclaimer 明示第三方数据约束责任在用户；需向 Phoenix/TAIR 申请书面许可，或后续版本再替换为 NCBI RefSeq（INSDC）；**本次不替换、不改 SHA-256 与金标准基线** | A2 | `false` | `false` | 同 #1 |
| 7 | `data/case-studies/arabidopsis-atlas/expression.tsv` | `E-MTAB-7978` | `author_authorization_required` | `https://www.ebi.ac.uk/about/terms-of-use` | 2026-11-14 | 需向数据所有者确认许可（Mergner / TUM）；或作者拍板按「EBI 无额外限制 + 引用 DOI 10.1038/s41586-020-2094-2」使用。EBI 层「无额外限制」可确证，但记录无 License 字段、所有者许可缺省 | A3 | `false` | `false` | 同 #1 |
| 8 | `data/species/mint/mentha_longifolia_expression.tsv` | `mint-4leaf-TPM` | `withdrawn_pending_upstream_confirmation` | `N/A（读段层无 license 字段；不臆造条款 URL。上游 CC BY 4.0：https://creativecommons.org/licenses/by/4.0/）` | 2026-11-14 | **溯源已确证、许可未落定 → 暂不进入公开归档包**。已查明为本项目用 SRP565976 的 4 个叶片文库（SRR32469939/940/941/942，PI 557769）+ 本项目 CC BY 4.0 horsemint 注释自行计算（minimap2 2.28 -ax splice → featureCounts 2.0.6 -s 0 → make_tpm.py），见 `PROVENANCE-mint-tpm.md`；上游 INSDC 读段无明示许可，故 `distribution_status = withdrawn_from_public_deposit`。**不删除、字节不动** | A4 | `false` | `false` | 不进 Deposit B 公开包；须同步数据仓库公开范围 / Zenodo 打包清单 |

**统计**：`author_authorization_required` **7 条**（#1–#7）、
`withdrawn_pending_upstream_confirmation` **1 条**（#8）、
`redistribute` **0 条**（8 条待确认中无可直接分发项）；另有 1 条 `confirmed`（CC BY 4.0，见 §2.3）。
合计 **1 confirmed + 8 待确认**，与论文 Data Availability 稳定口径一致。
**公开归档包 = 8 个文件 / 65,124,010 B**；仓库 `totals` 仍为 9 / 68,518,847 B。

---

## 2. 三选一决策口径与 `MANIFEST.json` 字段映射

### 2.1 `decision` 枚举（设计 §3.3）

| `decision` | 含义 | 条数 |
|---|---|---|
| `redistribute` | 可分发（仅已确证 CC BY 4.0 的 horsemint 注释适用） | 0（8 条待确认中） |
| `author_authorization_required` | 需作者向权利人申请书面许可，或后续版本替换 | 7（#1–#7） |
| `withdrawn_pending_upstream_confirmation` | **溯源已确证、上游许可待确认，暂不进入公开归档包**（承接原 `replace_required` 的 A4 槽位；因溯源查清而更名） | 1（#8） |

> **枚举变更说明（本轮）**：`replace_required` 不再使用，因 #8 的「来源无法确证」前提已不成立
> （溯源已确证）。更名保留 A4 语义，仅新增枚举值、不删除历史记录。

### 2.2 `MANIFEST.json` 追加字段（仅追加，不覆盖既有值）

每条 `files[]` 追加以下 6 个字段，与 `LICENSES.md` 汇总表逐条一一对应：

| 字段 | 类型 | 说明 |
|---|---|---|
| `license_decision` | enum | 三选一结论，见 §2.1 |
| `license_decision_date` | ISO date | 定稿日期 `2026-11-14`（即本文件的 `evidence_date`） |
| `official_terms_url` | string | 官方条款 URL；#8 为显式 `N/A`（不臆造） |
| **`author_action_required`** | string | `A2` / `A3` / `A4`（PRD §6.1）；已确证的 CC BY 4.0 条为 `A1-无` |
| `conclusion_is_authorization` | bool | **固定 `false`**（结论 ≠ 已获授权） |
| `baseline_changed` | bool | **固定 `false`**（裁定 1/2：SHA-256 与 9 文件基线不变） |

顶层追加 `license_decision_summary`：
`{ "confirmed": 1, "needs_confirmation": 8, "decisions_finalized": true, "totals_unchanged": true }`
（附 `note` 说明「三选一结论已定稿，但结论 ≠ 已获授权，8 条许可状态仍为 `needs_confirmation`，
Deposit B license 必须选 Other（多许可），**不得归并 CC BY 4.0**」）。

> **`decisions_finalized: true` 的含义**：指 8 条「再分发决策」已定稿、不再悬而未决；
> **不表示已获授权、不表示 `license_status` 变为 `confirmed`**。8 条的 `license_status`
> 一律保持 `needs_confirmation`（避免虚报），`conclusion_is_authorization = false` 显式兜底。

### 2.3 已确证项（不计入 8 条）

| 条 | `path` | 许可 | 决策 |
|---|---|---|---|
| — | `data/species/mint/mentha_longifolia_horsemint.gff3.gz` | **CC BY 4.0**（figshare 官方 API `license` 字段确证，DOI 10.6084/m9.figshare.28457522.v3） | `redistribute`；`license_status` 保持 `confirmed` |

---

## 3. 给作者的授权 / 替换请求清单（A2 / A3 / A4，B 类）

| 动作 | 关联条目 | 具体动作（作者执行） | 联系人 / 条款 URL | 预计影响（若采纳替换 / 删除） |
|---|---|---|---|---|
| **A2** | #1–#6（Araport11 Chr1–5 + TAIR10 FASTA） | ① 向 Phoenix Bioinformatics / TAIR 申请「再分发 substantial subsets」的**书面许可**；**或** ② 拍板在**后续版本**改用 NCBI RefSeq 染色体序列 **CP002684–CP002688**（INSDC 开放数据、无再分发限制） | TAIR Terms of Use <https://phoenixbioinformatics.atlassian.net/wiki/spaces/COM/pages/42216430/TAIR+Terms+of+Use>；Ensembl disclaimer <https://www.ensembl.org/info/about/legal/disclaimer.html> | 替换会**改变 6 个文件的 SHA-256 与字节数**，从而改变 9 文件 / 68,518,847 B 基线、`SHA256SUMS.txt`、金标准测试基线、Figure 5(c)、补表 S1、论文 Data Availability 数字与 Zenodo Deposit B；故**本次不替换**，须整体重跑并修订（设计 §10 末注记） |
| **A3** | #7（E-MTAB-7978） | 向数据所有者确认许可；**或** 拍板按「EBI 无额外限制 + 引用 DOI **10.1038/s41586-020-2094-2** 并标注 EBI 来源」使用 | 提交者 `julia.mergner@tum.de`（Mergner / Kuster, TUM）；EBI 条款 <https://www.ebi.ac.uk/about/terms-of-use> | 仅需补齐所有者许可声明 / 引用，**不改变文件字节与基线** |
| **A4** | #8（mint 4 叶 TPM） | **溯源已由本机查清**（本项目用 SRP565976 的 4 个叶片文库 + 本项目 CC BY 4.0 注释自行计算，见 `PROVENANCE-mint-tpm.md`），**不再需要"追根来源"**，只需就许可三选一：① 向 SRP565976 提交者确认是否允许公开再分发该 TPM 汇总值；② 拍板按「仅分发汇总统计量 + 完整引用 SRP565976 + 标注 CC BY 4.0 上游」处理；③ 维持现状（不进公开包） | SRP565976（NCBI/ENA）；上游 CC BY 4.0 <https://creativecommons.org/licenses/by/4.0/>；**禁止编造许可结论** | 现状：不进 Deposit B 公开包，公开包为 8 个文件 / 65,124,010 B；**本次不删除、字节不动**，仓库 `totals` 与 `SHA256SUMS.txt` 不变 |

> 另：WSU Mint Genome Resources 小示例（KB 级、随主仓保留、**不在本数据仓库**）许可仍需确认；
> 论文署名作者（Song X, Chen P, Liang F）与 git 身份（`omixcore`）一致性需作者核对（沿用既有清单，非本批新增）。

---

## 4. 对 Zenodo / 论文 / 图表的连锁影响（裁定 3）

> **现有数据归档包不构成已具备公开再分发条件的证据；8 项许可未落定前，Deposit B 不得设为公开可下载。**

- **Deposit B license**：选 **Other（多许可）+ 引用 `LICENSES.md`**，逐条列 1 confirmed（CC BY 4.0）
  + 8 待确认。**8 条未定稿前不得归并成 CC BY 4.0**；即使本批三选一结论已定稿，
  `license_status` 仍为 `needs_confirmation`，故**仍不得归并**（`docs/zenodo-deposit-checklist.md` §5）。
- **Deposit B 公开归档范围**：**8 个文件 / 65,124,010 B**（9 个中扣留 #8 的 3,394,837 B）。
  仓库 `totals` 仍为 9 / 68,518,847 B、`SHA256SUMS.txt` 仍为 9 行。
  **归档状态保持「待发布」，DOI 占位符保持未回填。**
- **Deposit A license**：软件 **MIT**（仓库根 `LICENSE`），与数据许可分开。
- **论文 Data Availability**：稳定口径「9 文件 / 68,518,847 B / `sha256sum -c` 9/9 OK / 双镜像 /
  **1 confirmed + 8 待确认** / DOI 占位 pending Zenodo」**依然属实**（这是**仓库口径**），
  但**不得**据此声称「9 个文件均可公开下载」——公开包为 8 个文件。本批**不改变**该口径中的任何数字。
- **Figure 5(c) / 补表 S1**：9 文件 / 68,518,847 B 的实数据不变，本批不动 `docs/paper-figures/**`。

---

## 5. 基线不变声明（验收硬门槛）

- **9 文件** / **68,518,847 B** / `totals.files = 9` / `totals.bytes = 68518847` / `len(files) = 9` 全部保持。
- `SHA256SUMS.txt` 与 `scripts/**` **未改动**；`sha256sum -c SHA256SUMS.txt` 应 **9/9 OK**。
- 逐文件 `bytes` / `sha256` / `path` / `baseline_blob` **一律不动**（`baseline_changed = false`）。
- **唯一例外（如实登记）**：`#8` 的 `source_url` 已被**更正**为实际读段下载 URL
  （`https://ftp.sra.ebi.ac.uk/vol1/fastq/SRR324/039/SRR32469939/SRR32469939_1.fastq.gz`），
  原值保留在 `previous_source_url`（`https://doi.org/10.6084/m9.figshare.28457522`）。
  理由：溯源已确证该矩阵由本项目自行计算，原 figshare URL 是**上游基因组/注释页**、并非本矩阵来源，
  属**错误溯源**；更正用的是**实测下载过的真实 URL**，未臆造任何地址。`bytes` / `sha256` 未动。
- **本次未删除任何文件、未改变基线**（`totals_unchanged = true`）；薄荷 TPM 保留、字节不动，
  最终去留留给作者 A4。
- `git -C phytoscope-reference-data diff --stat` 只应出现
  `LICENSES.md` / `MANIFEST.json` / `README.md` / 新增 `PROVENANCE-mint-tpm.md`；
  `git status --porcelain` 不应出现 `data/` 或 `SHA256SUMS.txt`。

---

## 6. 对外口径（禁止虚报）

- 固定表述：「**A 类 7 项本机已闭环、B 类 6 项物料齐备待作者执行、C 类 1 项给出核查结论**」。
- 本项（R3）属 **B 类**：本机只做「结论定稿 + 请求清单」；**禁止**写「许可问题已解决」
  「已获授权」「许可齐全」「全部确证」「已完成 Zenodo 归档」。
- **结论 ≠ 已获授权**：取得书面许可、Zenodo 发布、DOI 回填、薄荷 TPM 删除决策均须作者 / 主理人执行。
