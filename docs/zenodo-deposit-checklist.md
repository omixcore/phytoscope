# PhytoScope Zenodo 归档上传清单（提交给作者的物料）

> ## ⚠️ 醒目结论
>
> **现有数据归档包不构成已具备公开再分发条件的证据；8 项许可未落定前，Deposit B 不得设为公开可下载。**
>
> 8 项许可全部落定（书面许可到手或完成替换）之前：**Deposit B 归档状态保持「待发布」**，
> **`{{DATA_DOI}}` / `{{SOFTWARE_DOI}}` / `{{PAPER_DOI}}` 占位符保持未回填**，
> 不得在 Zenodo 设为公开，也不得称「数据许可已理清」。逐条条件见
> `phytoscope-reference-data/LICENSES.md` §7（可执行再分发条件表）。

> 本文件是交给「作者」的投稿物料，指导其在 **Zenodo 网页端手动**完成软件与数据两仓库的
> 归档并取得 DOI。**Zenodo 注册 / 上传 / 发布动作由作者在网页端手动完成**，本文件不含任何
> 自动上传脚本。**B 类如实标注：DOI 未取得、Release 附件未公开、归档未发布**；本机只交物料。
>
> **许可定稿说明（R3 / R5 checklist）**：§5 License 已按 `docs/fcs-data-license-decision.md`
> 的 8 条三选一结论定稿；两个 deposit 的 Description / Related identifiers / Citation 模板已定稿；
> `docs/fcs-figure-numbering-map.md` 为图号唯一权威（本文件不涉及图号）。
>
> **数据包口径变更（本轮）**：Deposit B **公开归档包由 9 个文件改为 8 个文件**
> （`data/species/mint/mentha_longifolia_expression.tsv` 扣留，见 §2 Deposit B 说明）；
> 仓库 `totals` 仍为 **9 个文件 / 68,518,847 B**，`SHA256SUMS.txt` 仍为 9 行。

## 0. 占位符清单（全部未回填；集中登记于 `docs/fcs-zenodo-handoff.md`）

| 占位符 | 含义 | 状态 | 回填责任 |
| --- | --- | --- | --- |
| `{{SOFTWARE_DOI}}` | Deposit A（软件）Zenodo DOI | **未回填 / 未取得** | 作者发布后回填，见 `docs/fcs-zenodo-handoff.md` |
| `{{DATA_DOI}}` | Deposit B（数据）Zenodo DOI | **未回填 / 未取得** | 同上 |
| `{{PAPER_DOI}}` | 投稿论文 DOI | **未回填 / 未取得** | 同上 |
| `{{ORCID}}` | 作者 ORCID | **未回填** | 同上 |
| `{{AFFILIATION}}` | 作者机构 | **未回填** | 同上 |

> 以上 5 个占位符在本文件中**一律保持未回填**，不得预填、不得写成已取得。
> 集中登记、回填步骤与「DOI 未取得」声明见 `docs/fcs-zenodo-handoff.md`。

---

## 1. 建议的正式版本号

- **软件：v0.3.0**。应用版本以 `pom.xml`（0.3.0）/ tag / ZIP 命名为准；**3.2.5 是 Spring Boot
  父 POM 版本，不是应用版本**（见 `README.md` / `CHANGELOG.md`）。当前 GitHub Release 仍只有
  `v0.2.0`；`v0.3.0` Release **未发布**。
- **数据：以 B04 许可结论定稿提交后的实际 HEAD 为准**。数据仓库
  `phytoscope-reference-data` 已镜像于 gitee + github；`2c6608b` 是 B04 **之前**的基线 HEAD，
  **不含** B04 定稿的 `LICENSES.md` / `MANIFEST.json` 许可结论，故**不可**作为 Deposit B 归档源。
  B05 归档时用 `git -C ../phytoscope-reference-data rev-parse HEAD` 取实际 HEAD，并把该 SHA
  记入 `docs/fcs-zenodo-handoff.md` 与 `docs/fcs-final-push-checklist.md`。
- **tag 修正（主理人裁定，覆盖设计原文）**：`v0.3.0` 是 annotated tag、指向 `3f71c1e`，比当前
  HEAD 落后多个提交且**无 Release 附件关联**。**禁止**用 `git archive v0.3.0` 生成源码包；
  Deposit A 一律用 `git archive HEAD`（或最终 commit SHA）。`v0.3.0` tag 由主理人在推送阶段
  前移到最终提交并同步两远端，作者 / 工程师**不要动 tag**。

  > **2026-11-14 现状补注（不改上条规则）**：`v0.3.0` 已由主理人前移并同步两远端，
  > 现实测指向 `0638dbe67c7bb406cd6fbcbedc6d5e389ec4d8f4`。**归档命令仍按上条写显式 SHA**
  > （`git archive 0638dbe`）——tag 已移动过两次，若再随登记性文档前移，
  > `git archive v0.3.0` 就产不出同一个包。`../dist/phytoscope-v0.3.0-source.zip`
  > 即由该 SHA 生成，460 项，`sha256 4f1a2125…`。详见 `docs/fcs-zenodo-handoff.md` §3.1。

## 2. 上传文件清单（两个 deposit，各取一个 DOI；均在 `../dist/` 由 B05 产出）

### Deposit A：软件（PhytoScope software）

| 文件 | 描述 |
| --- | --- |
| `phytoscope-v0.3.0-source.zip` | 软件源码归档：`git archive HEAD`（或最终 commit SHA）导出，**不用** `v0.3.0` tag（见 §1）。含 README、CHANGELOG、CITATION.cff、docs/、src/、tools/、.github/ 等当前 HEAD 全部改动 |
| `PhytoScope-Windows-0.3.0.zip` | Windows x64 桌面安装包（jlink app-image，无需 JDK）。**这是唯一权威发布名**（B03 口径）；`PhytoScope-Windows-AMD64.zip` 仅为 `package_desktop.py` 的 raw jpackage 输出 / 历史 artifact，不是权威发布名。历史 `../dist/PhytoScope-Windows-AMD64.zip` 与 `-0.2.0.zip` 字节不动 |
| `SHA256SUMS.txt`（可选） | 上述软件包的 SHA-256 校验和（口径见 `desktop_program/SHA256SUMS.txt`） |

### Deposit B：数据（PhytoScope reference data）

> **公开归档范围 = 8 个数据文件 / 65,124,010 B**（仓库 `totals` 仍为 9 个文件 /
> 68,518,847 B）。扣留 1 个文件：
> `data/species/mint/mentha_longifolia_expression.tsv`（3,394,837 B）——
> `distribution_status: withdrawn_from_public_deposit`，
> `license_decision: withdrawn_pending_upstream_confirmation`。
> **该文件保留在仓库中供内部复现与审计，但不进入公开归档包**；其溯源已确证，见
> `phytoscope-reference-data/PROVENANCE-mint-tpm.md`。
> `SHA256SUMS.txt` **仍为 9 行**（仓库口径未变），打包时**不删任何字节**，
> 只在上传的 ZIP 中排除该 1 个文件，并在包内 `README.md` / `LICENSES.md` 顶部保留扣留声明。

| 文件 | 描述 |
| --- | --- |
| `phytoscope-reference-data-v0.3.0.zip` | 独立数据仓库 `phytoscope-reference-data` 在其实际 HEAD 的归档：**8 个完整参考数据文件**（9 个中扣留 1 个）+ `MANIFEST.json` + `SHA256SUMS.txt`（9 行）+ `LICENSES.md` + `PROVENANCE-mint-tpm.md` + `README.md` + `scripts/`。包内 `LICENSES.md` / `MANIFEST.json` 含**最终许可结论与扣留声明** |
| `SHA256SUMS.txt` | 数据仓库内 **9 行** SHA-256（`sha256sum -c SHA256SUMS.txt` 在**仓库内**应 9/9 OK；公开包内条目 8 个） |

> **不得**把「8 个文件进公开包」写成「仓库只有 8 个文件」，也不得把「9/9 OK」写成
> 「9 个文件均已具备公开再分发条件」——两件事必须分开表述。

> 两个 ZIP 由 **B05** 在 `../dist/`（工作区根、主仓上一级）生成，并用 `unzip -l` 校验含
> `MANIFEST.json` / `SHA256SUMS.txt` / `LICENSES.md` / `scripts/`。**上传与发布仍标 B 类待作者执行**；
> 本机只产出归档包，不上传、不发布、不取得 DOI。
>
> 软件与数据是两个独立仓库、两套 DOI，拆分为两个 Zenodo deposit；在「Related identifiers」
> 中按下表互相引用，论文 DOI 发布后回填 `{{PAPER_DOI}}`。

### 2.1 Related identifiers 模板（定稿）

| Deposit | Relation | Identifier | 状态 |
| --- | --- | --- | --- |
| A（软件） | `isSupplementedBy`（数据补充软件） | `{{DATA_DOI}}` | 未回填 |
| B（数据） | `isSupplementTo`（数据补充软件） | `{{SOFTWARE_DOI}}` | 未回填 |
| A（软件） | `isRequiredBy`（论文依赖软件） | `{{PAPER_DOI}}` | 未回填 |
| B（数据） | `isRequiredBy`（论文依赖数据） | `{{PAPER_DOI}}` | 未回填 |

> 关系类型按 Zenodo「Related identifiers」下拉项填写；`{{...}}` 全部保持未回填，
> 见 `docs/fcs-zenodo-handoff.md`。

## 3. 每项描述（Zenodo "Description" 建议文案，定稿）

- **软件**：PhytoScope 是一个本地运行的 JavaFX 桌面端植物基因结构浏览器，面向无需专属物种门户、
  无需上传数据、结果可溯源的植物研究者；以统一「导入 → 浏览 → 查询 → 比较 → 导出」管线，跨物种
  完成基因结构查看、表达矩阵挂载、序列提取、蛋白质相似性与结构域映射等分析，并以 SHA-256 溯源
  与版本化 Manifest 保证结果可复现。本版本（v0.3.0）内置瘦身后的 KB 级示例，完整参考数据见
  配套数据归档（{{DATA_DOI}}）。**软件许可 MIT**。
- **数据**：PhytoScope 参考数据归档**公开包**，含 **8 个文件 / 65,124,010 B** —— TAIR10 基因组
  FASTA、Araport11 Chr1–Chr5 注释、horsemint（*Mentha longifolia*）染色体级基因组注释、
  E-MTAB-7978 表达图谱；附逐文件 SHA-256 校验和、双 gitee/github 镜像与来源 / 许可清单
  （见 `MANIFEST.json` 与 `LICENSES.md`）。**许可为多许可（Other）**：1 条已确证 CC BY 4.0，
  8 条待确认，逐条见 `LICENSES.md`；不得归并成单一 CC BY 4.0。
  **说明**：数据仓库共 9 个文件 / 68,518,847 B；其中 horsemint 4 叶 TPM 矩阵
  （`mentha_longifolia_expression.tsv`）**不进入本公开包**（溯源已确证但上游读段许可未落定，
  处置为 `withdrawn_from_public_deposit`），保留在仓库中供内部复现。

## 4. 贡献者 / 机构

- 作者：**Song X, Chen P, Liang F**（与仓库 `CITATION.cff` / 论文署名保持一致；请作者核对其与 git
  提交身份 `omixcore` 的一致性）
- 机构：`{{AFFILIATION}}`（**待作者填写**，见 `docs/fcs-zenodo-handoff.md`）
- ORCID：`{{ORCID}}`（**待作者填写**，见 `docs/fcs-zenodo-handoff.md`）

## 5. License（许可定稿，裁定 3）

> **现有数据归档包不构成已具备公开再分发条件的证据；8 项许可未落定前，Deposit B 不得设为公开可下载。**

- **Deposit A（软件）：MIT**（仓库根 `LICENSE`）。与数据许可**分开**，不得互相套用。
- **Deposit B（数据）：Other（多许可）+ 引用 `phytoscope-reference-data/LICENSES.md`**。
  Zenodo 的 license 字段选 **「Other」** 并在 Description / 备注中写明「多许可，逐文件见
  `LICENSES.md`」，**不得**选 CC BY 4.0 作为整个 deposit 的单一许可。

  逐条口径（与 `LICENSES.md` §0 / §7、`MANIFEST.json` 一一对应）：

  | 条 | 数据 | 是否进公开包 | 许可状态 | `decision` |
  | --- | --- | --- | --- | --- |
  | 1（**confirmed**） | horsemint 完整注释 `mentha_longifolia_horsemint.gff3.gz` | ✅ 进 | ✅ **CC BY 4.0**（figshare 官方 API 确证，DOI 10.6084/m9.figshare.28457522.v3） | `redistribute` |
  | 2–6（5 条） | Araport11 `Chr1–Chr5.gff3.gz` | ⚠️ 进（需许可） | ⚠️ 待确认：需向 Phoenix/TAIR 申请书面许可，或后续版本替换为 NCBI RefSeq CP002684–CP002688 | `author_authorization_required` |
  | 7 | TAIR10 `TAIR10_genomic.fna.gz` | ⚠️ 进（需许可） | ⚠️ 待确认：Ensembl 第三方数据约束责任在用户，需向 Phoenix/TAIR 申请书面许可或后续版本替换 | `author_authorization_required` |
  | 8 | E-MTAB-7978 `expression.tsv` | ⚠️ 进（需许可） | ⚠️ 待确认：需向数据所有者（Mergner / TUM）确认许可 | `author_authorization_required` |
  | 9 | mint 4 叶 TPM `mentha_longifolia_expression.tsv` | ❌ **不进公开包** | ⚠️ **溯源已确证、许可未落定**：本项目自行计算的衍生数据，上游注释/基因组 CC BY 4.0，但 INSDC 读段无明示许可 | `withdrawn_pending_upstream_confirmation` |

  **合计 1 confirmed（CC BY 4.0）+ 8 待确认；公开包 8 个文件 / 65,124,010 B。**

- **8 条未定稿 / 未获权利人书面许可前，不得归并成 CC BY 4.0**：即使三选一结论已定稿，
  `license_status` 一律保持 `needs_confirmation`，`conclusion_is_authorization = false`，
  **结论 ≠ 已获授权**；本机不能代替权利人授予许可。取得书面许可、Zenodo 发布、DOI 回填、
  mint 4 叶 TPM 的最终去留均属**作者 / 主理人动作（B 类）**。
- 权威决策表：`docs/fcs-data-license-decision.md`；逐条取证与**可执行再分发条件表**：
  `phytoscope-reference-data/LICENSES.md`（§0 / §7）。

## 6. Related DOI（占位，归档后回填）

- 软件 DOI：`{{SOFTWARE_DOI}}`
- 数据 DOI：`{{DATA_DOI}}`
- 论文 DOI：`{{PAPER_DOI}}`（投稿后回填）
- 具体 Related identifiers 关系见 §2.1；全部占位符集中登记于 `docs/fcs-zenodo-handoff.md`。

## 7. Keywords（建议）

plant genomics · gene structure browser · GFF3 / GTF · FASTA · expression matrix ·
JavaFX desktop application · reproducibility · data provenance · cross-species ·
reference data

## 8. Citation 模板（定稿；带 `{{DOI}}` 占位）

软件：

```text
Song X, Chen P, Liang F. (2026). PhytoScope: a local desktop browser for
portable queries of plant genome structure data (v0.3.0). Zenodo.
https://doi.org/{{SOFTWARE_DOI}}
```

数据：

```text
Song X, Chen P, Liang F. (2026). PhytoScope reference data (v0.3.0) [Data set].
Zenodo. https://doi.org/{{DATA_DOI}}
```

> 两个模板的 `{{SOFTWARE_DOI}}` / `{{DATA_DOI}}` **保持未回填**；DOI 取得后由作者回填到本文件、
> `README.md` 的 Citation 段、`CITATION.cff` 注释与 `docs/fcs-zenodo-handoff.md`。

## 9. 待作者在网页端完成的动作（B 类，本机不执行）

1. 注册 / 登录 Zenodo（建议用 GitHub 账号登录，便于自动关联仓库）。
2. 新建 software、dataset 两个上传，分别上传 §2 的两个 ZIP（由 B05 在 `../dist/` 产出）。
3. 填写 metadata（title / description / authors / **license：软件 MIT、数据 Other（多许可）** /
   keywords / related identifiers，按 §5 与 §2.1）。
4. 发布并取得两个 DOI。
5. 将 DOI 回填到 `README.md`「Citation」段、`CITATION.cff` 注释、本文件 §6/§8 占位符与
   `docs/fcs-zenodo-handoff.md`。
6. GitHub Release 附件的公开动作由**主理人**用远端 API 尝试；**未成功前不得称已发布**，
   见 `docs/fcs-v0.3.0-release-notes.md`。

> **禁止虚报**：截至本清单定稿，**DOI 未取得、Zenodo 归档未发布、v0.3.0 Release 附件未公开**；
> 三平台 CI 报告回收 / macOS·Linux 真机 / 真人用户研究亦属 B 类待执行。
