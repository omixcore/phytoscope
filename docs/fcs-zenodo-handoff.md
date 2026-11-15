# PhytoScope FCS · Zenodo 交接卡（占位符集中登记，B 类）

> ## ⚠️ 醒目结论
>
> **现有数据归档包不构成已具备公开再分发条件的证据；8 项许可未落定前，Deposit B 不得设为公开可下载。**
>
> 因此：**Zenodo 归档状态保持「待发布」**；§1 五个占位符（含 `{{DATA_DOI}}`）**保持未回填**；
> 不得在 Zenodo 设为公开。逐条条件见 `phytoscope-reference-data/LICENSES.md` §7。
>
> **Deposit B 拟归档清单（候选）= 8 个文件 / 65,124,010 B**（仓库 `totals` 仍为
> **9 个文件 / 68,518,847 B**、`SHA256SUMS.txt` 仍为 9 行）。扣留
> `data/species/mint/mentha_longifolia_expression.tsv`（见 §3.3）。
> **其中可合法再分发的仅 1 个（horsemint CC BY 4.0）；其余 7 个 `author_authorization_required`
> 在取得书面许可前不得公开下载。**

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
`{{SOFTWARE_DOI}}` / `{{DATA_DOI}}` / `{{PAPER_DOI}}` 均无真实值。GitHub Release 已发布 **`v0.3.0`**
（2026-11-14，软件包 `PhytoScope-Windows-0.3.0.zip`）与历史 **`v0.2.0`**；但**软件 Release ≠ 软件
Zenodo 归档**，二者是不同动作，`{{SOFTWARE_DOI}}` 仍未回填。

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

| 文件（`../dist/`，工作区根、主仓上一级） | 大小（B） | 归档源 | 说明 |
|---|---|---|---|
| `phytoscope-v0.3.0-source.zip` | **11,658,264** | **`git archive 0638dbe`**（显式 SHA；当时 tag `v0.3.0` 指向同一提交，见 §3.1） | 460 项，ZIP 注释即该 SHA |
| `PhytoScope-Windows-0.3.0.zip` | 46,185,966 | 从发布提交 `0638dbe` 的 tree 构建（jpackage）+ 重命名 | packaged smoke PASS；旧构建 `8f7953eb…` 作废 |
| `phytoscope-reference-data-v0.3.0.zip` | 63,581,789 | 数据仓库 **`8906399`** | 29 项 / **8 个数据文件**，已剔除扣留文件 |
| `desktop_program/SHA256SUMS.txt`（主仓内） | 2,158 | 0.3.0 包校验和（Release 资产之一） | 已更新为重建后的值，`sha256=690905ad…` |

> **拟归档包口径（2026-11-14 重建后已落实）**：数据包已按 **8 个数据文件**（未压缩合计
> 65,124,010 B）重新打包，`data/species/mint/mentha_longifolia_expression.tsv` 已从 ZIP 中剔除
> （理由见 §3.3）。**仓库内 `totals` 仍为 9 / 68,518,847 B、`SHA256SUMS.txt` 仍为 9 行**——
> 「仓库口径」「拟归档清单口径」「可公开再分发口径」必须分开表述，不得互相替代。
> **注意**：这 8 个数据文件里仅 1 个（horsemint CC BY 4.0）可合法再分发，其余 7 个
> `author_authorization_required` 在取得书面许可前不得公开下载——「8 个文件」是拟归档清单，
> 不是「8 个可公开文件」。

两包 SHA-256（2026-11-14 重建后实测，上传时供作者核对）：
- `phytoscope-v0.3.0-source.zip` = `4f1a212591e9e0b32ac41882ed0b656a0c663587afc5ae8cd925709933b1c9f9`
- `phytoscope-reference-data-v0.3.0.zip` = `dce268f14ee3e8de6f8c4d636e1b569c0f45cd32cf59613bb9076333ba3dd72a`
  （**2026-11-15 从数据仓 `8906399` 重建**，以纳入「拟归档清单 vs 可再分发」三口径措辞修正；
  旧包 `66054b7` / `e88a9cfa…` 作废）

### 3.1 归档源 SHA

- **Deposit A（软件源码）**：用 **`git archive 0638dbe`** 生成（**显式 SHA，不用 tag 作为命令**），
  实测 `git rev-parse v0.3.0^{}` = **`0638dbe67c7bb406cd6fbcbedc6d5e389ec4d8f4`**，460 项。
  - **历史说明**：B05 阶段该 annotated tag 曾指向 `edae8433`（tag 对象）、落后 HEAD 多个提交
    且无 Release 附件关联，当时**禁止**用 tag 生成源码包，只能用显式 SHA `e920a12`。
  - **现状（2026-11-14）**：tag 经主理人两次前移后已指向**含本轮全部修复的内容提交**
    `0638dbe`；此刻 `git archive v0.3.0` 与 `git archive 0638dbe` 产出**逐字节相同**
    （包内 ZIP 注释即 `0638dbe…`）。但归档命令**仍以显式 SHA 书写**，理由见下条。
  - **可复现性约束（为什么不写 tag）**：tag 会随收尾/登记性文档移动（本次已移动两次），
    一旦再移动，`git archive v0.3.0` 就产不出同一个包。显式 SHA 永不移动。
    归档快照本身也无法包含它的 SHA 登记，因此**登记性文档提交不纳入 Deposit A**。
  - 核对命令：`git cat-file -t 0638dbe67c7bb406cd6fbcbedc6d5e389ec4d8f4`、
    `git archive 0638dbe | sha256sum`；推送闭环的三方 HEAD 比对见
    `docs/fcs-final-push-checklist.md`。
- **Deposit B（参考数据）**：用数据仓库 **`8906399`**
  （= `8906399071c4ac6ac120cfb7f952846fb534a9f4`，在 B04 许可定稿 + 薄荷 TPM 溯源基础上，
  进一步**修正「拟归档清单 vs 可再分发」三口径措辞**），
  包内含 `LICENSES.md` / `MANIFEST.json` / `PROVENANCE-mint-tpm.md` 的最终结论；
  **不用**基线 `2c6608b`（不含 B04 与溯源定稿），**也不用**旧的 `66054b7`（其
  `public_deposit_files=8` 措辞把「拟归档清单」误当「可公开文件」，已被 `8906399` 修正）。

### 3.2 `unzip -l` 内容留痕（B05 实跑）

- Deposit A：含 `README.md`、`CITATION.cff`、`pom.xml`、`docs/**`、`src/**`、`tools/**`、
  `benchmarks/**`；`unzip -t` → `DEP_A_ZIP_OK`。
- Deposit B：含 `MANIFEST.json`、`SHA256SUMS.txt`、`LICENSES.md`、`scripts/**`（10 个脚本）；
  且 `LICENSES.md` 内含「三选一结论定稿表」§0 与「可执行再分发条件表」§7、`MANIFEST.json`
  含 `license_decision` / `distribution_status` 字段；`unzip -t` → `DEP_B_ZIP_OK`。
- 两包 `unzip -t` 完整性校验均通过；**本机未上传、未创建 deposit、未取得 DOI**。

### 3.3 Deposit B 扣留文件（本轮新增，**必须遵守**）

| 项 | 值 |
|---|---|
| 扣留文件 | `data/species/mint/mentha_longifolia_expression.tsv`（3,394,837 B，SHA-256 `8000e27d…`） |
| `distribution_status` | `withdrawn_from_public_deposit` |
| `license_decision` | `withdrawn_pending_upstream_confirmation` |
| `license_status` | `needs_confirmation`（**不变**） |
| 处置理由 | 溯源**已确证**（本项目用 SRP565976 的 4 个叶片文库 + 本项目 CC BY 4.0 horsemint 注释自行计算，见 `phytoscope-reference-data/PROVENANCE-mint-tpm.md`）；但上游 INSDC 读段**无明示许可**，故按保守原则**不进入拟归档清单** |
| **不做什么** | **不删除磁盘文件**（否则 `MintTranscriptomeUiTest` 与既有基线失效）；**不改 `data/**` 字节**；**不改 `SHA256SUMS.txt` 的 9 行**；**不改 `totals`（9 / 68,518,847）** |
| 拟归档清单结果 | **8 个文件 / 65,124,010 B**（= 68,518,847 − 3,394,837；其中可公开仅 1 个 horsemint） |

> 打包时的正确做法：**在仓库内不删任何文件**，只在上传的 ZIP 中排除该 1 个路径，
> 并保留包内 `README.md` / `LICENSES.md` 顶部的扣留声明。
> **不得**对外表述为「数据仓库只有 8 个文件」或「9 个文件都可公开下载」。

---

## 4. Description / License / Citation 定稿物料的指针

- 两个 deposit 的 Description、Related identifiers、Citation 模板：**`docs/zenodo-deposit-checklist.md`**。
- 逐文件许可结论（1 confirmed + 8 待确认）：**`phytoscope-reference-data/LICENSES.md`**
  （含 §7 **可执行再分发条件表**，逐文件给出权利方 / 条款 URL / 能否公开再分发 / 还缺什么 / 建议动作）
  与 **`docs/fcs-data-license-decision.md`**。
- 薄荷 4 叶 TPM 的**可复核溯源**：**`phytoscope-reference-data/PROVENANCE-mint-tpm.md`**
  （含证据链、取证命令、输入文件、输入数据许可、复现步骤与复现性边界）。
- 软件许可证：**MIT**（仓库根 `LICENSE`）。

---

## 5. 如实口径

- 本文件只交付**占位符登记 + 交接步骤**；**Zenodo 归档未完成、DOI 未取得、归档保持「待发布」**。
- **现有数据归档包不构成已具备公开再分发条件的证据；8 项许可未落定前，Deposit B 不得设为公开可下载。**
- 对外表述固定为「**A 类 7 项本机已闭环、B 类 6 项物料齐备待作者执行、C 类 1 项给出核查结论**」；
  禁止写「已完成 Zenodo 归档 / 取得 DOI」「14 项全部完成」「已发布 v0.3.0」「数据许可已理清」。
- **仓库口径 vs 拟归档清单口径 vs 可公开再分发口径**：仓库 9 个文件 / 68,518,847 B /
  `SHA256SUMS.txt` 9 行（不变）；拟归档清单 8 个文件 / 65,124,010 B，其中可公开再分发仅 1 个
  （horsemint）。**三者不得混用**。
