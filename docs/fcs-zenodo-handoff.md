# PhytoScope FCS · Zenodo 交接卡（占位符集中登记，B 类）

> 批次：B03 · 执行人：工程师寇豆码（Kou）· 日期：2026-11-14（ISO-8601 UTC 口径同设计 §7.10）
> 权威规格：`docs/design-fcs-final-delivery.md` §5 B03 #8、§3.1、§7.5；增量 PRD R5。
> **状态口径**：本文件属 **B 类（只交物料、需作者执行）**。**DOI 未取得**。本机不能代替作者登录
> Zenodo、不能创建 deposit、不能取得 DOI、不能回填占位符。以下 5 个占位符**全部保持未回填**，
> 不得填入任何真实值、不得新增别名（设计 §7.5）。

---

## 1. 占位符登记表（唯一集中登记处）

| 占位符 | 含义 | 由谁 / 何时回填 | 当前值 |
|---|---|---|---|
| `{{SOFTWARE_DOI}}` | 软件 deposit（Deposit A）的 Zenodo DOI | 作者 ORCID 登录 Zenodo 发布 Deposit A 后 | **未回填（DOI 未取得）** |
| `{{DATA_DOI}}` | 数据 deposit（Deposit B）的 Zenodo DOI | 作者 ORCID 登录 Zenodo 发布 Deposit B 后 | **未回填（DOI 未取得）** |
| `{{PAPER_DOI}}` | 论文正式 DOI | FCS 录用/发表后由作者回填 | **未回填（DOI 未取得）** |
| `{{ORCID}}` | 作者 ORCID iD | 作者在 Zenodo / CITATION.cff 回填 | **未回填** |
| `{{AFFILIATION}}` | 机构署名 | 作者核对 `Song X, Chen P, Liang F` 与 git 身份 `omixcore` 一致性后回填 | **未回填** |

> 允许出现这些占位符的位置（仅作为占位符）：`README.md`「Citation」、`CITATION.cff`、
> `docs/fcs-paper-outline.md` §3.6、`docs/fcs-paper-manuscript-draft.md`（Data/Code Availability）、
> `docs/zenodo-deposit-checklist.md`。**本文件是集中登记处**，其余位置只引用、不另立别名。

**醒目声明**：**DOI 未取得**。截至本批次，软件与数据两个 Zenodo deposit **均未创建、均未发布**，
`{{SOFTWARE_DOI}}` / `{{DATA_DOI}}` / `{{PAPER_DOI}}` 均无真实值。当前 GitHub Release **只有 `v0.2.0`**，
**没有 `v0.3.0` Release**，不得把「现有 v0.2.0 Release」写成「已发布 v0.3.0」。

---

## 2. 交作者的 5 步动作（A1，B 类，本机不执行）

1. ORCID 登录 Zenodo 网页端，创建 **software**（Deposit A）与 **dataset**（Deposit B）两个 deposit。
2. 上传 §3 的两个归档包与 §4 的物料，按 `docs/zenodo-deposit-checklist.md` 填 metadata
   （title / description / authors / **license** / keywords / related identifiers）。
3. 发布两个 deposit，取得 `{{SOFTWARE_DOI}}` 与 `{{DATA_DOI}}`。
4. 回填本表 §1 及 README「Citation」、`CITATION.cff`、outline §3.6、manuscript Data/Code Availability
   中的占位符；`{{PAPER_DOI}}` 待 FCS 发表后回填。
5. 在 Zenodo 两个 deposit 的「Related identifiers」中互相引用（software ↔ dataset：
   `isSupplementTo` / `isRequiredBy`），并在论文 DOI 发布后回填 `{{PAPER_DOI}}`。

> **许可前置**：Deposit B 的 license 字段在 **8 条待确认许可全部定稿前不得归并为 CC BY 4.0**，
> 按 `docs/zenodo-deposit-checklist.md` §5 选「Other（多许可）+ 引用 `LICENSES.md`」。

---

## 3. 上传文件清单（B05 产出，本批不生成）

| 文件 | 说明 | 产出批次 |
|---|---|---|
| `dist/phytoscope-v0.3.0-source.zip` | `git archive --format=zip --output ../dist/... v0.3.0` | B05 |
| `dist/PhytoScope-Windows-0.3.0.zip` | 唯一权威 Windows 安装包命名（B05 生成） | B05 |
| `dist/phytoscope-reference-data-v0.3.0.zip` | 数据仓库归档（含 MANIFEST / SHA256SUMS / LICENSES / scripts） | B05 |
| `desktop_program/SHA256SUMS.txt` | 主仓内 0.3.0 包校验和（M02 上传资产之一） | B05 |

> 本批（B03）**不生成任何 ZIP / 上传包**、不触碰 `dist/`、不触碰 `desktop_program/SHA256SUMS.txt`。

---

## 4. Description / License / Citation 定稿物料的指针

- 两个 deposit 的 Description、Related identifiers、Citation 模板：**`docs/zenodo-deposit-checklist.md`**（B04 独占定稿）。
- 逐文件许可结论（1 confirmed + 8 待确认）：**`phytoscope-reference-data/LICENSES.md`** 与
  **`docs/fcs-data-license-decision.md`**（B04 独占定稿）。
- 软件许可证：**MIT**（仓库根 `LICENSE`）。

---

## 5. 如实口径

- 本文件只交付**占位符登记 + 交接步骤**；**Zenodo 归档未完成、DOI 未取得**。
- 对外表述固定为「**A 类 7 项本机已闭环、B 类 6 项物料齐备待作者执行、C 类 1 项给出核查结论**」；
  禁止写「已完成 Zenodo 归档 / 取得 DOI」「14 项全部完成」「已发布 v0.3.0」。
