# PhytoScope FCS · Zenodo 交接卡（占位符集中登记，B 类）

> ## ⚠️ 醒目结论
>
> **现有数据归档包不构成已具备公开再分发条件的证据；8 项许可未落定前，Deposit B 不得设为公开可下载。**
>
> 因此：**Zenodo 归档状态保持「待发布」**；§1 五个占位符（含 `{{DATA_DOI}}`）**保持未回填**；
> 不得在 Zenodo 设为公开。逐条条件见 `phytoscope-reference-data/LICENSES.md` §7。
>
> **Deposit B 公开归档范围 = 8 个文件 / 65,124,010 B**（仓库 `totals` 仍为
> **9 个文件 / 68,518,847 B**、`SHA256SUMS.txt` 仍为 9 行）。扣留
> `data/species/mint/mentha_longifolia_expression.tsv`（见 §3.3）。

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
| `phytoscope-v0.3.0-source.zip` | **11,636,868** | **`git archive e920a12`**（B05 主 commit，归档源固定，见 §3.1） | B05 |
| `PhytoScope-Windows-0.3.0.zip` | 46,185,790 | 本机构建（jpackage）+ 重命名 | B05 |
| `phytoscope-reference-data-v0.3.0.zip` | 64,329,519（**9 文件旧口径**，须重打） | 数据仓库 **`249e771`** | B05 |
| `desktop_program/SHA256SUMS.txt`（主仓内） | — | 0.3.0 包校验和（M02 上传资产之一） | B05 |

> **⚠️ 数据包必须重打后再上传**：上表 `phytoscope-reference-data-v0.3.0.zip`（64,329,519 B）
> 是**含 9 个数据文件的旧口径**产物。公开归档范围已改为 **8 个文件 / 65,124,010 B**
> （扣留 `data/species/mint/mentha_longifolia_expression.tsv`，见 §3.3），
> 因此**该 ZIP 与其 SHA-256 不得用于公开发布**，须按 8 文件口径重新打包后另行登记。
> 本节只登记既有产物与归档源，**不虚报新包的字节数与校验和**。
> **仓库内 `totals` 仍为 9 / 68,518,847 B、`SHA256SUMS.txt` 仍为 9 行**——
> 「仓库口径」与「公开包口径」必须分开表述。

两包 SHA-256（B05 定稿后实测，上传时供作者核对）：
- `phytoscope-v0.3.0-source.zip` = `e8ca52b88451d207673f6be1f1c62fed0f1c7ca5a515c25ebd502a3109e17b6a`
- `phytoscope-reference-data-v0.3.0.zip` = `c7eb904c1e8a7977bb751f9b412ec9386d977d6062a4a179e3b4aac7f1361e9b`

### 3.1 归档源 SHA（记录；`v0.3.0` tag 不用）

- **Deposit A（软件源码）**：用 **`git archive e920a12`（B05 主 commit 显式 SHA）** 生成，
  **禁止** `git archive v0.3.0` —— 该 annotated tag 指向 `edae8433`（tag 对象）、
  落后当前 HEAD 多个提交且**无 Release 附件关联**，会漏掉本次全部改动。
  `v0.3.0` tag 由**主理人**在推送阶段前移到最终提交并同步两远端，工程师**不动 tag**。
  - **B05 主 commit（Deposit A 归档源，固定）= `e920a1211904709dc61776bf091322a73f7d2010`**
    （team-lead 续作指令要求登记）。Deposit A 已用 `git archive e920a12` **生成**，
    包内含 B05 的 `fcs-v0.3.0-release-notes.md` /
    `fcs-performance-benchmark.md` / `benchmarks/results/2026-11-14-v0.3.0/**` /
    架构师 3 份设计文档（`unzip -l` 已核对）。已实测 `git archive e920a12` 与交付 ZIP
    **逐字节一致**（`cmp` BYTE_IDENTICAL=YES），归档可确定复现。
  - 首次生成时 HEAD 为 `7eead0e032edbbe4b370c827733794d9e47ac94a`（B05 commit 前），
    已被 `e920a12` 归档版覆盖；**不得**把首次生成的 `7eead0e` 当作归档源。
  - **归档源固定为 `e920a12`，不随其后 HEAD 前移**：本文件的归档源 SHA / 两包校验和
    登记由 **B05 收尾 commit** 写入；归档快照无法包含其自身的 SHA 登记，故该收尾
    commit **不纳入 Deposit A**，但 `e920a12` 已含 B05 全部实质产物（代码 / 文档 /
    性能实测 / 设计定稿），满足「不用 `v0.3.0` tag、不漏改动」的裁定要求。
    主理人 push 前用 `git cat-file -t e920a1211904709dc61776bf091322a73f7d2010`
    与 `git archive e920a12 | sha256sum` 复现核对归档源；第 2/11 项**推送闭环**的
    三方 HEAD 比对（另一动作）见 `docs/fcs-final-push-checklist.md`。
- **Deposit B（参考数据）**：用 `git -C ../phytoscope-reference-data rev-parse HEAD`
  取 **B04 定稿后实际 HEAD = `249e771a09b28312f46df0f1a02eb7d34f14897f`**，
  使包内 `LICENSES.md` / `MANIFEST.json` 含最终三选一许可结论（与 Zenodo dataset
  license 字段如实一致）。**不用**基线 `2c6608b`（不含 B04 定稿）。

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
| 处置理由 | 溯源**已确证**（本项目用 SRP565976 的 4 个叶片文库 + 本项目 CC BY 4.0 horsemint 注释自行计算，见 `phytoscope-reference-data/PROVENANCE-mint-tpm.md`）；但上游 INSDC 读段**无明示许可**，故按保守原则**不进入公开归档包** |
| **不做什么** | **不删除磁盘文件**（否则 `MintTranscriptomeUiTest` 与既有基线失效）；**不改 `data/**` 字节**；**不改 `SHA256SUMS.txt` 的 9 行**；**不改 `totals`（9 / 68,518,847）** |
| 公开包结果 | **8 个文件 / 65,124,010 B**（= 68,518,847 − 3,394,837） |

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
- **仓库口径 vs 公开包口径**：仓库 9 个文件 / 68,518,847 B / `SHA256SUMS.txt` 9 行（不变）；
  公开包 8 个文件 / 65,124,010 B。**两者不得混用**。
