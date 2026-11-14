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

## 3. 上传文件清单（B05 产出，本机只生成包、不上传）

> **B05 增量补注**：下方 §1 的 5 个占位符**仍全部保持未回填**、DOI 未取得；
> 本节只登记 B05 已产出的归档包与**归档源 SHA**（team-lead 第三轮续作指令）。
> **上传与发布仍标 B 类待作者执行**。

| 文件（`../dist/`，工作区根、主仓上一级） | 大小（B） | 归档源 | 产出批次 |
|---|---|---|---|
| `phytoscope-v0.3.0-source.zip` | 11,589,747 | **`git archive HEAD`**（B05 最终 commit SHA，见 §3.1） | B05 |
| `PhytoScope-Windows-0.3.0.zip` | 46,185,790 | 本机构建（jpackage）+ 重命名 | B05 |
| `phytoscope-reference-data-v0.3.0.zip` | 64,329,519 | 数据仓库 **B04 定稿后实际 HEAD** | B05 |
| `desktop_program/SHA256SUMS.txt`（主仓内） | — | 0.3.0 包校验和（M02 上传资产之一） | B05 |

### 3.1 归档源 SHA（记录；`v0.3.0` tag 不用）

- **Deposit A（软件源码）**：用 **`git archive HEAD`（或最终 commit SHA）** 生成，
  **禁止** `git archive v0.3.0` —— 该 annotated tag 指向 `edae8433`（tag 对象）、
  落后当前 HEAD 多个提交且**无 Release 附件关联**，会漏掉本次全部改动。
  `v0.3.0` tag 由**主理人**在推送阶段前移到最终提交并同步两远端，工程师**不动 tag**。
  - Deposit A 归档 SHA（B05 commit 前的 HEAD，首次生成时）：`7eead0e032edbbe4b370c827733794d9e47ac94a`。
  - **B05 commit 后需用最终 HEAD 重新生成一次**（使包内含 B05 的 release-notes /
    performance / desktop / 本 handoff 等文档）；最终 SHA 记入 B05 回执与
    `docs/fcs-final-push-checklist.md` 第 2/11 项，由主理人 push 前核对。
- **Deposit B（参考数据）**：用 `git -C ../phytoscope-reference-data rev-parse HEAD`
  取 **B04 定稿后实际 HEAD = `249e771a09b28312f46df0f1a02eb7d34f14897f`**，
  使包内 `LICENSES.md` / `MANIFEST.json` 含最终三选一许可结论（与 Zenodo dataset
  license 字段如实一致）。**不用**基线 `2c6608b`（不含 B04 定稿）。

### 3.2 `unzip -l` 内容留痕（B05 实跑）

- Deposit A：含 `README.md`、`CITATION.cff`、`pom.xml`、`docs/**`、`src/**`、`tools/**`、
  `benchmarks/**`；`unzip -t` → `DEP_A_ZIP_OK`。
- Deposit B：含 `MANIFEST.json`、`SHA256SUMS.txt`、`LICENSES.md`、`scripts/**`（10 个脚本）；
  且 `LICENSES.md` 内含「B04 三选一结论定稿表」§0、`MANIFEST.json` 含 `license_decision`
  字段；`unzip -t` → `DEP_B_ZIP_OK`。
- 两包 `unzip -t` 完整性校验均通过；**本机未上传、未创建 deposit、未取得 DOI**。

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
