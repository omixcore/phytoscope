# PhytoScope FCS · 参考数据许可结论定稿（R3，B 类）

> 批次：**B04** · 执行人：工程师寇豆码（Kou）· 定稿/取证日期：**2026-11-14**（ISO-8601 UTC 口径同设计 §7.10）
> 权威规格：`docs/design-fcs-final-delivery.md` §5 B04（817–849 行）、§3.3；增量 PRD R3 / §6.1 A2·A3·A4 / §8；主理人第三轮裁定 1/2/3。
> 对应物料：`phytoscope-reference-data/LICENSES.md`、`MANIFEST.json`（8 条逐条一一对应）、
> `docs/zenodo-deposit-checklist.md` §5。

---

## 0. 醒目声明（不得删改）

> **结论 ≠ 已获授权；本机不能代替权利人授予许可。**
> 本文件的 8 条「三选一结论」是**再分发决策定稿**，不是「已取得授权」「许可问题已解决」或
> 「已获书面许可」。每一条的 `conclusion_is_authorization = false`。取得书面许可、Zenodo 发布、
> DOI 回填、薄荷 TPM 删除决策**均不由本机完成**，属作者 / 主理人动作（B 类）。
>
> **本次未删除任何文件、未改变基线**：9 个数据文件 / 68,518,847 B / `SHA256SUMS.txt` /
> `scripts/**` / 逐文件 `sha256` / `bytes` / `path` / `source_url` / `baseline_blob` 全部不动，
> `sha256sum -c SHA256SUMS.txt` 仍 9/9 OK；`MANIFEST.json` 的 `totals.files = 9`、
> `totals.bytes = 68518847`、`len(files) = 9` 保持不变（`totals_unchanged = true`）。

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
| 8 | `data/species/mint/mentha_longifolia_expression.tsv` | `mint-4leaf-TPM` | `replace_required` | `N/A（来源与许可均无法确证，不臆造 source_url / 条款 URL）` | 2026-11-14 | **来源与许可无法确证 → 不得再分发，需作者追根或删除**。figshare 条目文件清单中不存在任何 TPM/表达矩阵文件，MANIFEST 的 `source_url` 指向基因组组装页而非本矩阵；**不删除、字节不动、不臆造 `source_url`**，删除与否留给作者 | A4 | `false` | `false` | 同 #1；若作者拍板删除，须同步数据仓库 / MANIFEST / 论文补表 S1 / Figure 5(c) / Zenodo Deposit B |

**统计**：`author_authorization_required` **7 条**（#1–#7）、`replace_required` **1 条**（#8）、
`redistribute` **0 条**（8 条待确认中无可直接分发项）；另有 1 条 `confirmed`（CC BY 4.0，见 §2.3）。
合计 **1 confirmed + 8 待确认**，与论文 Data Availability 稳定口径一致。

---

## 2. 三选一决策口径与 `MANIFEST.json` 字段映射

### 2.1 `decision` 枚举（设计 §3.3）

| `decision` | 含义 | 条数 |
|---|---|---|
| `redistribute` | 可分发（仅已确证 CC BY 4.0 的 horsemint 注释适用） | 0（8 条待确认中） |
| `replace_required` | 需替换 / 追根；薄荷 4 叶 TPM 专用于「来源无法确证」 | 1（#8） |
| `author_authorization_required` | 需作者向权利人申请书面许可，或后续版本替换 | 7（#1–#7） |

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
| **A4** | #8（mint 4 叶 TPM） | 追根该矩阵**真实来源与许可**（原始 RNA-seq 文库编号、生成脚本、发布页）；**或** 拍板**删除**该文件 | 无（来源无法确证，**禁止编造 `source_url` / 许可**） | 若删除：需从数据仓库 9 文件 / `MANIFEST.json` / `SHA256SUMS.txt` / 论文补表 S1 / Figure 5(c) / Zenodo Deposit B **一并移除**，多处既定数字随之改变，须作者拍板；**本次不删除、字节不动** |

> 另：WSU Mint Genome Resources 小示例（KB 级、随主仓保留、**不在本数据仓库**）许可仍需确认；
> 论文署名作者（Song X, Chen P, Liang F）与 git 身份（`omixcore`）一致性需作者核对（沿用既有清单，非本批新增）。

---

## 4. 对 Zenodo / 论文 / 图表的连锁影响（裁定 3）

- **Deposit B license**：选 **Other（多许可）+ 引用 `LICENSES.md`**，逐条列 1 confirmed（CC BY 4.0）
  + 8 待确认。**8 条未定稿前不得归并成 CC BY 4.0**；即使本批三选一结论已定稿，
  `license_status` 仍为 `needs_confirmation`，故**仍不得归并**（`docs/zenodo-deposit-checklist.md` §5）。
- **Deposit A license**：软件 **MIT**（仓库根 `LICENSE`），与数据许可分开。
- **论文 Data Availability**：稳定口径「9 文件 / 68,518,847 B / `sha256sum -c` 9/9 OK / 双镜像 /
  **1 confirmed + 8 待确认** / DOI 占位 pending Zenodo」，见
  `docs/fcs-paper-manuscript-draft.md`。本批**不改变**该口径中的任何数字。
- **Figure 5(c) / 补表 S1**：9 文件 / 68,518,847 B 的实数据不变，本批不动 `docs/paper-figures/**`。

---

## 5. 基线不变声明（验收硬门槛）

- **9 文件** / **68,518,847 B** / `totals.files = 9` / `totals.bytes = 68518847` / `len(files) = 9` 全部保持。
- `SHA256SUMS.txt` 与 `scripts/**` **未改动**；`sha256sum -c SHA256SUMS.txt` 应 **9/9 OK**。
- 逐文件 `bytes` / `sha256` / `path` / `source_url` / `baseline_blob` **一律不动**（`baseline_changed = false`）。
- **本次未删除任何文件、未改变基线**（`totals_unchanged = true`）；薄荷 TPM 保留、字节不动，
  删除与否留给作者 A4。
- `git -C phytoscope-reference-data diff --stat` 只应出现 `LICENSES.md` / `MANIFEST.json`；
  `git status --porcelain` 不应出现 `data/` 或 `SHA256SUMS.txt`。

---

## 6. 对外口径（禁止虚报）

- 固定表述：「**A 类 7 项本机已闭环、B 类 6 项物料齐备待作者执行、C 类 1 项给出核查结论**」。
- 本项（R3）属 **B 类**：本机只做「结论定稿 + 请求清单」；**禁止**写「许可问题已解决」
  「已获授权」「许可齐全」「全部确证」「已完成 Zenodo 归档」。
- **结论 ≠ 已获授权**：取得书面许可、Zenodo 发布、DOI 回填、薄荷 TPM 删除决策均须作者 / 主理人执行。
