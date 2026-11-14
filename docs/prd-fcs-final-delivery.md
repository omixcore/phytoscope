# PhytoScope · FCS 投稿 14 项收尾交付 · 增量 PRD

> 文档类型：**增量 PRD**（需求分析，不含实现、不含代码）· 编写人：产品经理 许清楚（Xu）· 日期：2026-11-14
> 上游文档：`docs/prd-fcs-submission.md`（投稿主 PRD）、`docs/fcs-submission-gap-audit.md`（只读差距审计）、`docs/fcs-phase2-verification-plan.md`、`docs/fcs-phase3-comparison-plan.md`、`docs/zenodo-deposit-checklist.md`、`docs/fcs-paper-outline.md`、`docs/fcs-paper-manuscript-draft.md`、`docs/input-format-specification.md`、`docs/paper-figures/README.md`。
> 本文件只做**需求拆解与验收口径定义**，不新增算法、不新增功能、不虚构未测数字、不改写既有代码。

---

## 0. 项目信息与状态分类定义

| 项 | 值 |
|---|---|
| Language | 中文（本 PRD）；论文正文按 FCS 要求为英文（`fcs-paper-outline.md` 已定） |
| Programming Language / 技术栈 | 沿用既有：Java 21 + JavaFX + Spring Boot（父 POM **3.2.5**，非应用版本）+ Maven Wrapper；**应用版本 0.3.0**；桌面打包 jpackage 21.0.6；论文图 Python 3.13 + matplotlib。**不新增技术栈** |
| Project Name | `phytoscope`（软件仓 `G:/WorkBuddy/2026-10-27-22-02-43/phytoscope`）；数据仓库 `phytoscope-reference-data`；桌面产物 `dist/` |
| 本次交付物 | 本文件 `docs/prd-fcs-final-delivery.md`（UTF-8 / 中文 / Markdown 表格） |
| 原始需求复述 | 用户下发「FCS 投稿 14 项收尾交付」P0 1–5 / P1 6–12 / P2 13–14，要求逐项转为需求条目；完成后代码/文档推 gitee + github，数据推 `phytoscope-reference-data` 两个镜像 |

### 0.1 三种状态分类（A / B / C，硬性口径，禁止虚报）

用户此前明确纠正过「全部完成」的不实表述。本次每条需求必须归入且只归入一类：

| 类 | 定义 | 「完成」的判定 |
|---|---|---|
| **A · 本机可完全闭环** | 在本机（仅 Windows x64 + JDK 21.0.6 / jpackage 21.0.6 / Python 3.13.14 / `mvnw.cmd`，gitee/github/API 实测 200）即可产出可验证的文件与通过的本地命令/测试 | **文件存在 + 命令/测试通过**为准 |
| **B · 只能备齐物料/协议，需作者在外部平台或真机执行** | 本机只能产出协议、清单、占位符、空白模板、待回填文档；真实动作需作者 ORCID 登录、真人参与者、macOS/Linux 真机、GitHub Actions 远端 | **物料齐备且如实标注「待作者执行 / 未执行」**为准；**禁止写成已完成** |
| **C · 受网络/权限限制可能只能给结论** | 可查证并成文结论/核查表，但「确证」程度受外部资源可达性或权利人权限限制；无法代替权利人授予许可 | **结论成文 + 逐条标注确证渠道与「无法确证」项**为准；不得把「可能」写成「确认」 |

> 主理人明确指定 **B 类至少包含 R2 / R3 / R5 / R6 / R13 / R14**。其中 **R3（许可）、R6（三工具对照）** 兼具 C 特征：条款查证 / 实机安装受网络与环境限制，本机只能给结论与定性对照；此项性质在 §7 风险中单列说明，但主分类仍按 B 登记，统计按主分类计。

**分类统计（主分类）：A = 7 项（R1/R4/R7/R8/R9/R10/R11）· B = 6 项（R2/R3/R5/R6/R13/R14）· C = 1 项（R12）· 合计 14 项。**

### 0.2 编号空间与「避免重复/冲突」声明

- 本 PRD 的 **R1–R14** 是本次收尾增量的独立编号空间，**与 `prd-fcs-submission.md` 的 R1.1–R31 不覆盖、不改写、不重排**；两者通过 §5 映射表对照。
- 已有成果**直接承接、不重做**：`LICENSES.md`/`MANIFEST.json` 已完成 9 文件条款查证（R3 只做结论定稿）；`zenodo-deposit-checklist.md` 已备好两个 deposit 物料（R5 只做交作者与占位符管理）；`fcs-phase3-comparison-plan.md` 已完成 gffutils/Biopython 实机基线 + 三工具定性对照（R6 只做定稿标注）；`docs/paper-figures/` 已有 Figure1–5 + S1 的 PNG/SVG（R8/R9 只做复现修复与图号统一）。
- `fcs-submission-gap-audit.md` 为 HEAD `8b6bf0b` 时代只读盘点，其 tag / pom 版本 / 许可状态字段已过时；本 PRD 的基线一律以 §1 主理人实测结论为准，仅引用该审计的**结构**，不引用其过时状态值。
- 继承主 PRD 红线：不新增算法、不虚构未测数字、不把历史（v0.2.0 / `8fd01ca`-era）性能值当当前实测。
- **CI 状态红线**：当前 HEAD `c79034e` 实跑为**三个平台 `validation` job 全部 failure**（主理人实测），旧文档（`research-module-validation.md` 等）「CI 三平台通过（bedtools 8/8、BLASTP 20/20）」一律视为数据外迁前历史留痕，本 PRD 及后续文档**不得沿用为当前状态**（详见 §1 / §1.1）。

---

## 1. 变更基线与项目信息（主理人已实测，直接采信）

| 维度 | 基线结论 |
|---|---|
| 软件仓 HEAD | `c79034e`（2026-11-14 15:14）；工作区干净；与 `origin/dev` **0 ahead / 0 behind** |
| Git tag | 已有 `v0.2.0`、`v0.3.0`；`pom.xml` 应用版本 = **0.3.0**（`3.2.5` 是 Spring Boot 父 POM 版本，**勿混淆**） |
| 远端 | `origin` = `https://gitee.com/omixcore/phytoscope.git`；`github` = `git@github-w1996jy:omixcore/phytoscope.git`（SSH over 443）。gitee API 与 `api.github.com` 本机均返回 **200** |
| 数据仓库 | `phytoscope-reference-data` HEAD `2c6608b`，工作区干净；**9 文件 / 68,518,847 B**；双镜像 gitee + github |
| 数据许可现状 | `MANIFEST.json`：**8 项 `license_status: needs_confirmation`**，仅 `data/species/mint/mentha_longifolia_horsemint.gff3.gz` 为 `confirmed`（CC BY 4.0）。8 项 = E-MTAB-7978 表达矩阵；拟南芥 Chr1–Chr5 五份 GFF3.gz（TAIR 条款，可能限制再分发）；TAIR10 基因组 FASTA（Ensembl 第三方数据约束）；薄荷 4 叶 TPM 矩阵（来源与许可**均无法确证**） |
| CI 定义 | `.github/workflows/validate.yml`：`matrix.os = [ubuntu-latest, windows-latest, macos-latest]`；关键链路 `compare_promoters_bedtools.py` / `compare_homology_blastp.py` 消费 `target/research-validation/research-module-gold-set.json`（由 Java 金标准测试产出）；数据外迁后 CI 从 gitee 克隆 `phytoscope-reference-data` 取回完整数据到 `target/reference-data` |
| **CI 实跑现状（主理人从 GitHub Actions 最新 `c79034e` 运行拉取）** | **三个平台 `validation` job 全部 `failure`，不是 success**；请勿沿用旧文档「CI 三平台通过」的历史说法。逐平台失败步骤见下方 §1.1 表 |
| CI Java 测试实测 | `Tests run: 211, Failures: 0, Errors: 0, Skipped: 12`；其中 **`ResearchModuleGoldSetTest` = `Tests run: 1, Skipped: 1`** —— 这是用户第 1 项「确保研究模块金标准测试真正运行」的直接证据：金标准测试因条件门控被整体跳过，`target/research-validation/research-module-gold-set.json` 不产出，后续 `compare_promoters_bedtools.py` / `compare_homology_blastp.py` 两处对比因此形同虚设 |
| CI 代码类失败根因（R1 修复对象） | 数据外迁后，isoform / 研究模块验证链脚本仍硬编码主仓内置路径 `src/main/resources/data/species/arabidopsis/TAIR10_genomic.fna.gz`（ubuntu isoform 步 `FileNotFoundError`）；atlas 案例入口虽已移除，但 `Real case` 步仍用只存在于完整注释的基因 ID `AT1G67090`（windows/macos `Unknown exact gene ID`，退出码 3）。二者均需改走 `target/reference-data` 外部取回数据或改用小示例可解析位点；金标准门控需在 `validate.yml:51` 显式传 `-Dphytoscope.referenceDataDir` |
| CI 外部网络类失败（单独归类，不属代码失败） | `public-data-validation` job 的 `Audit public Arabidopsis splicing and proteomics resources` 失败（外部 PMC/PastDB/ENA 网络数据源），非代码逻辑；须与上述代码类失败分开归类、分开记录，不得混为一谈 |
| GitHub Release 现状 | **目前只有 `v0.2.0`**（资产 `PhytoScope-Windows-AMD64.zip` + `SHA256SUMS.txt`），**没有 `v0.3.0`** —— R4 发布目标即补建 v0.3.0 Release |
| 远端推送凭证 | 两个远端 `git ls-remote` 均可达，**推送凭证可用**（收尾推送可执行，非阻塞） |
| 绘图复现缺口（本次 R8 依据） | `docs/paper-figures/make_paper_figures.py` 的 `make_figureS1()` 读 `target/phase3-comparison/{gffutils,biopython}/summary.json`；二者是 `target/` 下运行时产物（干净 clone 下不存在）→ 复现命令会在 S1 处失败；且脚本 docstring 把这两个 JSON 错标为「Figure 5」数据源（图号残留） |
| 图号现状（本次 R9 依据） | `paper-figures/README.md`「Numbering note」记录最终编号 = 1 架构 / 2 数据模型与输入格式 / 3 七模块 / 4 跨物种验证 / 5 可复现性，工具对比降为 **Figure S1**；但 `fcs-paper-outline.md` §4 仍写「Figure 2 = cross-species portability、Figure 4 = fair comparison」，`make_paper_figures.py` 内并存两处「Figure 5」注释 |
| 本机工具链 | JDK 21.0.6、jpackage 21.0.6、Python 3.13.14、`mvnw.cmd` 可用；**仅 Windows x64**（无 macOS/Linux 真机、无显示环境脚本化 GUI 快照） |
| 既有论文图 | `docs/paper-figures/` 已含 Figure1–5 与 FigureS1 的 SVG/PNG + `make_paper_figures.py` + `README.md` |

### 1.1 逐平台 CI 失败清单（`c79034e` 最新实跑，主理人实测）

> 结论：三个平台 `validation` job **全部 failure**。R1 的「修复 CI」须覆盖下列**代码类失败**，使其在三平台转 success；**外部网络类失败单独归类**，不计入 validation job 的成败判定口径。

| 平台 / job | 失败步骤 | 报错 | 归类 | R1 修复要求 |
|---|---|---|---|---|
| `ubuntu-latest` / validation | `Reproduce frozen isoform screen and independent validation`（`tools/check_isoform_case.py`，`validate.yml:48`） | `FileNotFoundError: src/main/resources/data/species/arabidopsis/TAIR10_genomic.fna.gz` | **代码类**（数据外迁后脚本仍硬编码主仓内置路径） | 脚本改从 `target/reference-data/data/...` 取回外部数据路径，或加数据可用性守卫 |
| `windows-latest` / validation | `Real case, no display required`（`validate.yml:83-88`，`--loci AT1G67090,...`） | `error: Unknown exact gene ID: AT1G67090`，退出码 3 | **代码类**（atlas 案例入口已移除，CI 仍用仅存在于完整注释的基因 ID） | 改为小示例可解析的位点，或该步改走 `target/reference-data` 外部数据 |
| `macos-latest` / validation | 同上 `Real case, no display required` | 同上 `Unknown exact gene ID`，退出码 3 | **代码类** | 同 windows |
| 三平台 / validation（连带） | `Java and JavaFX regressions` → bedtools/BLASTP 对比 | `ResearchModuleGoldSetTest` = `Skipped: 1`，`research-module-gold-set.json` 不产出 | **代码类**（条件门控 `-Dphytoscope.referenceDataDir` 未传） | `validate.yml:51` 显式传 `-Dphytoscope.referenceDataDir=target/reference-data`，使金标准测试**实跑（非 skipped）**并产出报告 |
| `public-data-validation`（独立 job） | `Audit public Arabidopsis splicing and proteomics resources` | 外部 PMC / PastDB / ENA 网络数据源不可达 | **外部网络类（非代码逻辑）** | 不与代码失败混为一谈；单独记录、可加 `continue-on-error` 或标注，不得据此判定 validation job 失败 |

> R1 验收总口径（详见 §3 R1）：三平台 `validation` job 转 **success**；`ResearchModuleGoldSetTest` 在三平台均非 skipped（Skipped 数由 12 下降）；`target/research-validation/research-module-gold-set.json` 真实生成且被 `compare_promoters_bedtools.py`、`compare_homology_blastp.py` **两处** Python 对比成功消费。

---

## 2. 产品目标与本次范围

**定位不变**（承接 `prd-fcs-submission.md` §1）：PhytoScope = 本地 JavaFX 植物基因结构浏览器，卖点为「通用性 / 正确性 / 可复现性」；投稿类型 Code & Data Letter 优先。**本次不新增算法、不新增功能，只做投稿前的收尾闭环与如实标注。**

本次三条正交目标：

| # | 目标 | 可度量结果 |
|---|---|---|
| G1 | **打通证据链真实性** | 以 `c79034e` 三平台 `validation` 全 failure 为基线：修复三类代码类 CI 失败，金标准测试由 `Skipped: 1` 变实跑、产出金标准 JSON 并被 bedtools/BLASTP 两处消费；三平台由 failure 转 success 且报告可追溯落盘（`public-data-validation` 外部网络失败单独归类）；Figure S1 在干净 clone 下可复现；14 项交付物与需求一一对照 |
| G2 | **消除合规与一致性风险** | 8 项数据许可逐条定稿为「可分发 / 需替换 / 需作者授权」三选一且薄荷 TPM 不编造来源；论文图号、PRD、Gap Audit、Manuscript 四处统一；参考文献逐条可核查 |
| G3 | **如实区分「已闭环」与「待作者」** | 每条需求带 A/B/C 状态；B 类只交物料+协议并标注未执行；对外表述不得出现「14 项全部完成」 |

**本次范围边界**：
- 做：14 项需求拆解、验收口径、交付物映射、B 类物料/协议、文档一致性与许可结论定稿、本地可跑的测试/构建/复现。
- 不做：新增解析器/算法；代替权利人授予许可；代替作者登录 Zenodo 取 DOI；招募真人；macOS/Linux 真机；把历史性能值当当前实测。
- 收尾动作（用户最终要求）：代码/文档推 `origin`(gitee) + `github`；数据推 `phytoscope-reference-data` 两镜像。推送前须核对 HEAD、工作区干净、tag、两远端一致性（见 §5 末行）。**主理人已实测两远端 `git ls-remote` 可达、推送凭证可用**，git push 属本机 A 类动作；Release 资产上传 / Zenodo deposit 网页动作未执行前如实标注「未发布」，**不得称已推送或已发布**。

---

## 3. 需求池（R1–R14）

优先级：**P0 = 投稿硬门槛 / P1 = 影响论文质量 / P2 = 可选**。验收标准均带具体文件或命令；交付物路径为仓库相对路径（`phytoscope-reference-data/` 前缀表示数据仓库）。

| 编号 | 优先级 | 需求描述 | 验收标准（可验证 · 带具体文件/命令） | 交付物路径 | 状态 | 依赖 |
|---|---|---|---|---|---|---|
| **R1** | P0 | **修复 CI，确保研究模块金标准测试真正运行**：以 §1.1 的 `c79034e` 三平台全 failure 为修复基线，覆盖三类**代码类**失败——① `validate.yml:51` 显式传 `-Dphytoscope.referenceDataDir=target/reference-data`，使 `ResearchModuleGoldSetTest` 由 skipped 转实跑并产出报告；② ubuntu isoform 步脚本 `tools/check_isoform_case.py` 硬编码的 `src/main/resources/data/.../TAIR10_genomic.fna.gz` 改走 `target/reference-data` 外部数据路径（或加数据可用守卫）；③ windows/macos `Real case` 步仅存在于完整注释的 `--loci AT1G67090,...` 改为小示例可解析位点或改走外部数据；并为下游 bedtools/BLASTP 步加 report 缺失守卫。`public-data-validation` 为**外部网络类**失败，单独归类、不混入代码修复口径。仅改 CI 配置 / 脚本数据路径 / 守卫，不改算法 | **本机可闭环验收（A 类）**：① 本地 `sh mvnw -B -Pui-tests -Dphytoscope.referenceDataDir=target/reference-data test` 后 surefire 报告 `Skipped` 数由 **12 下降**、`ResearchModuleGoldSetTest` 三平台口径下均**非 skipped**，`target/research-validation/research-module-gold-set.json` **真实生成**；② `python tools/compare_promoters_bedtools.py --report <该JSON> ...` 与 `python tools/compare_homology_blastp.py --report <该JSON> ...` **两处**均成功消费该报告并产出比对 JSON，缺 report 时明确报错而非静默跳过；③ `tools/check_isoform_case.py` 与 Real-case 步用新数据路径/位点本地重跑不再 `FileNotFoundError` / `Unknown exact gene ID`。<br>**目标态（由 R2 远端实跑出具证据，不计入本机 A 类闭环）**：三个平台 `validation` job 由 `failure` 转 **success**；`public-data-validation` 单独标注为外部网络依赖，不与代码失败混为一谈 | 改 `.github/workflows/validate.yml`（`:51` 传参、`:48` isoform 步、`:83-88` Real case 步）；改 `tools/check_isoform_case.py`（数据路径）；必要时改 `tools/compare_promoters_bedtools.py`、`tools/compare_homology_blastp.py`（守卫）；新建 `docs/fcs-ci-goldset-fix-report.md`（本地实跑留痕 + 三平台报告链接） | **A** | 无（R2 前置） |
| **R2** | P0 | **重跑当前版本三平台完整 CI 并保存测试报告**：以 §1.1 的 `c79034e` **三平台全 failure** 为对照基线，在 R1 修复后的 HEAD 触发 GitHub Actions `Validate and package PhytoScope`，回收 ubuntu/windows/macos 三平台结果并落盘，确认逐平台转 success。依赖 GitHub Actions 远端执行与结果回收，本机不能替代 | ① 一次 `workflow_dispatch`/push 触发，记录 **run id + commit SHA**，并与 `c79034e` failure 基线逐平台对照；② `docs/ci-reports/v0.3.0/{ubuntu,windows,macos}/` 各含 `surefire-reports/`（`Skipped` 数须体现金标准非 skipped）与（Linux）`research-module-gold-set.json`、`bedtools-promoter-comparison.json`、`blastp-homology-comparison.json`；③ 汇总文档逐平台标 **failure → success** 的变化 + 金标准是否实跑 + 两处 Python 对比是否消费成功；④ `public-data-validation`（PMC/PastDB/ENA 外网依赖）失败须**单独如实记录为外部网络类**，不并入三平台 `validation` 矩阵结论、不据此判定代码失败；⑤ 报告中的 commit 与 §1 基线一致 | 新建 `docs/ci-reports/v0.3.0/**`（含 `c79034e` failure 基线留档）；新建/改 `docs/fcs-phase2-verification-report.md`（T06 汇总落点）；改 `docs/fcs-submission-gap-audit.md` #10 状态（仅在结果真实回收后） | **B** | R1 |
| **R3** | P0 | **逐项解决 8 个参考数据文件的许可问题**：承接 `LICENSES.md` 已完成的条款查证，把 8 项逐条定稿为「可分发 / 需替换 / 需作者授权」三选一结论；薄荷 4 叶 TPM 因来源无法确证，**必须给出删除或替换建议，不得编造来源**；同步 MANIFEST / SHA256SUMS；产出给作者的授权/替换请求清单。**本机不能代替权利人授予许可** | ① `phytoscope-reference-data/LICENSES.md` 8 条各带三选一结论 + 官方条款 URL + 取证日期；② 薄荷 TPM 明确写「删除」或「替换为可确证来源」，且**不出现任何臆造的 source_url/许可**；③ 若采纳删除：`MANIFEST.json` 的 `totals.files`、`totals.bytes`、`files[]` 与 `SHA256SUMS.txt` 同步，`sha256sum -c SHA256SUMS.txt` 全 OK；④ `docs/fcs-data-license-decision.md` 列出需作者完成的动作（向 Phoenix/TAIR 申请书面许可、向 Mergner/TUM 确认 E-MTAB-7978 所有者许可、追根或删除薄荷 TPM）；⑤ 文档明确「结论 ≠ 已获授权」 | 改 `phytoscope-reference-data/LICENSES.md`、`MANIFEST.json`、`SHA256SUMS.txt`；（待拍板）删 `data/species/mint/mentha_longifolia_expression.tsv`；新建 `docs/fcs-data-license-decision.md`；同步改 `docs/zenodo-deposit-checklist.md` §5 | **B**（含 C 特征） | 无（结果输入 R5/R11） |
| **R4** | P0 | **构建并发布 v0.3.0 Windows 安装包**：用当前树重新 `package_desktop.py` 生成瘦身版 ZIP，更新校验和、版本表述与本地 smoke 记录；统一 `dist/` 下历史命名；**GitHub Release 当前只有 `v0.2.0`（资产 `PhytoScope-Windows-AMD64.zip` + `SHA256SUMS.txt`）、没有 `v0.3.0`**，发布目标即补建 `v0.3.0` Release。构建与落盘本机可闭环；两个远端 `git ls-remote` 均可达、**推送凭证可用**（§1），但 Release 资产上传仍属网页/API 动作，须如实标注是否已发布 | ① `python tools/package_desktop.py --jar target/phytoscope-0.3.0.jar` 成功，`dist/` 生成唯一命名 ZIP（约 44 MiB 级，非 110 MB 历史包）；② `desktop_program/SHA256SUMS.txt` 更新且 `sha256sum -c` OK；③ 本地 runtime smoke（`--benchmark --example rice`）PASS 记录；④ `README.md` / `CHANGELOG.md` 下载链接与版本统一到 **0.3.0**，`pom.xml` 应用版本与 tag 一致；⑤ 补建 `v0.3.0` Release 并把新包 + `SHA256SUMS.txt` 作为资产上传（区别于现有 v0.2.0 Release）；若仅完成本机构建、Release 资产未上传，须标注「待发布 / 未公开」，**不得称已发布 v0.3.0 Release** | 改/生成 `dist/PhytoScope-Windows-0.3.0.zip`、`desktop_program/SHA256SUMS.txt`；改 `docs/desktop-distribution.md`、`docs/desktop-verification.md`、`README.md`、`CHANGELOG.md`；新建 `docs/fcs-v0.3.0-release-notes.md` | **A** | R1（构建链） |
| **R5** | P0 | **完成软件和数据的 Zenodo 归档，取得 DOI**：本机备齐两个 deposit 的上传包、metadata 文案、占位符与操作卡；**创建 deposit 与取得 DOI 必须由作者 ORCID 登录 Zenodo 网页端完成**，本地无法取得 DOI | ① 本机生成 `phytoscope-v0.3.0-source.zip`（`git archive v0.3.0`）与 `phytoscope-reference-data-v0.3.0.zip`（`2c6608b` 归档，含 MANIFEST/SHA256SUMS/LICENSES/scripts）；② `docs/zenodo-deposit-checklist.md` 定稿（两个 deposit 文件清单、Description、License 与 R3 结论一致、Related identifiers、Citation 模板）；③ `{{SOFTWARE_DOI}}` / `{{DATA_DOI}}` / `{{ORCID}}` / `{{AFFILIATION}}` 占位符集中登记于 `docs/fcs-zenodo-handoff.md`，**保持未回填**；④ README「Citation」、`CITATION.cff`、论文 §3.6 处仅留占位符并标注「待 Zenodo 发布后回填」；⑤ 文档明确「DOI 未取得」 | 改 `docs/zenodo-deposit-checklist.md`；新建 `docs/fcs-zenodo-handoff.md`、两个上传包；`README.md`/`CITATION.cff`/`docs/fcs-paper-outline.md` §3.6/`docs/fcs-paper-manuscript-draft.md` 仅加占位符标注 | **B** | R3、R4（上传包含安装包与许可结论） |
| **R6** | P1 | **完成 JBrowse2、IGV、TBtools 同任务对照**：承接 `fcs-phase3-comparison-plan.md` 已有成果（gffutils 0.13 / Biopython 1.88 实机基线 + 三工具定性矩阵），做定稿标注；三工具实机安装受本机环境与网络限制，**只做定性对照 + 既有实机基线，不得冒充实测计时** | ① `docs/fcs-tool-comparison-final.md` 明确三工具为「定性对照（基于公开文档，非本机实测）」，JBrowse2 未安装、IGV/TBtools 为 GUI 未脚本化；② 共同任务矩阵、功能差异单独列出，不作「更快/更好/更易用」主张；③ gffutils/Biopython 实机数据（3 轮交替、8/8 元组一致、3 物种 × 6 产物逐字节一致）仅引用已产出报告，不重造计时；④ 预留外部实机补充协议（空白、标注未执行） | 改 `docs/fcs-phase3-comparison-plan.md`（补定稿说明）；新建 `docs/fcs-tool-comparison-final.md`；`docs/fcs-tool-comparison-external.md`（空白协议模板） | **B**（含 C 特征） | 无（实机数据输入 R8） |
| **R7** | P1 | **补充 GFF2 专用测试，明确其支持范围**：为 GFF2 补最小 fixture 与专用测试，确认「走既有 importer、兼容级、非独立解析器、非独立验证宣称」的边界；不新增解析器 | ① 新增 `src/test/resources/gff2/` 最小 fixture（含 CDS phase、转录本边界、多 parent / 无 exon 的 CDS 等边界）；② 新增/扩展测试类（如 `Gff2ParserSupportTest`）且 `mvnw -Pui-tests -Dtest=<GFF2测试类> test` 绿，含至少 1 个负例；③ `docs/input-format-specification.md` 补「GFF2 支持范围」小节，明确与 `fcs-paper-outline.md` §3.2(b)、`fcs-paper-manuscript-draft.md` §2.2 措辞一致（无专用 fixture 时不宣称独立支持） | 新建 `src/test/resources/gff2/**`、`src/test/java/**/Gff2ParserSupportTest.java`；改 `docs/input-format-specification.md`、`docs/fcs-paper-outline.md` §3.2(b)、`docs/fcs-paper-manuscript-draft.md` §2.2、`docs/prd-fcs-submission.md` R1.1/R3.1 备注 | **A** | 无 |
| **R8** | P1 | **补齐 Figure S1 的原始 JSON 并修复绘图复现**：把 `target/phase3-comparison/{gffutils,biopython}/summary.json` 冻结到可提交位置，修脚本读取路径与错标图号的 docstring，保证干净 clone 下可复现 | ① 新建 `docs/paper-figures/data/gffutils-summary.json`、`biopython-summary.json`（附来源说明 + SHA-256 + 生成脚本名）；② `make_paper_figures.py` 的 `GFFUTILS_SUMMARY`/`BIOPYTHON_SUMMARY` 改为指向 `docs/paper-figures/data/`，清理 docstring 中「Figure 5」错标；③ 在**删除 `target/` 后**执行 `python docs/paper-figures/make_paper_figures.py` 一次成功，重生成 `Figure1–5` + `FigureS1` 共 6 组 PNG/SVG；④ `docs/paper-figures/README.md` 的 Data sources 表 Figure 5 / S1 归属修正、复现命令可用 | 新建 `docs/paper-figures/data/*.json`；改 `docs/paper-figures/make_paper_figures.py`、`docs/paper-figures/README.md`；重生成 `docs/paper-figures/Figure{1-5,S1}.{png,svg}` | **A** | R6（实机 JSON 来源） |
| **R9** | P1 | **统一论文图号、PRD、Gap Audit 和 Manuscript**：以 `paper-figures/README.md` 最终编号（1–5 + S1）为唯一权威，消除 `fcs-paper-outline.md` §4、旧 PRD §4、Manuscript、脚本注释中的图号错位 | ① 新建 `docs/fcs-figure-numbering-map.md` 唯一对照表：Fig1–5 + S1 → 内容 → 数据源 → 生成脚本 → 各文档引用位置；② `fcs-paper-outline.md` §4 的 Figure 2/4 标题改为「数据模型与输入格式 / 跨物种验证」，工具对比指向 S1；③ `fcs-paper-manuscript-draft.md` Results 各小节 Figure 引用逐一对齐；④ 旧 `prd-fcs-submission.md` §4 采用**最小改动**：加「图号以本增量 PRD 图号对照表为准」的修订说明，不大改旧编号；⑤ `paper-figures/README.md`「Residual mismatch to flag」标记已解决；⑥ `make_paper_figures.py` 两处「Figure 5」注释归一 | 新建 `docs/fcs-figure-numbering-map.md`；改 `docs/fcs-paper-outline.md` §4、`docs/fcs-paper-manuscript-draft.md`、`docs/prd-fcs-submission.md` §4（加说明）、`docs/fcs-submission-gap-audit.md`（涉及图表述处）、`docs/paper-figures/README.md`、`docs/paper-figures/make_paper_figures.py` | **A** | R8 |
| **R10** | P1 | **补充统一硬件性能测试和内存数据**：单台声明硬件、固定数据+任务、≥3 次交替冷启动，报告导入时间 / 峰值内存 / 搜索 p50·p95 / 比较布局耗时；历史值标 historical；Windows 无 `resource` 模块导致 RSS 不可测须**如实标注，不得编造** | ① `docs/fcs-performance-benchmark.md` 声明硬件（CPU/内存/OS/JDK/Python/`-Xmx`）、数据、任务、轮次与交替顺序；② `benchmarks/results/2026-11-14-v0.3.0/*.json` 落盘导入时间 / 堆池峰值 / 搜索 p50·p95 / 比较布局耗时，含 `java -Xmx3g -jar target/phytoscope-0.3.0.jar --benchmark --example rice --out ...` 等复现命令；③ 每项数字标「当前实测」或「historical（v0.2.0 / 8fd01ca）」；④ gffutils/PhytoScope RSS 项明确写 `not measured (Windows lacks resource)`；⑤ `docs/performance.json` 不覆盖历史值，仅追加指针 | 新建 `docs/fcs-performance-benchmark.md`、`benchmarks/results/2026-11-14-v0.3.0/**`；改 `docs/performance.json`（追加指针/说明）、`docs/fcs-paper-outline.md` §5.1 Sup Fig 5、`docs/fcs-paper-manuscript-draft.md` §3.4/§4.3 | **A** | R4（可运行 JAR） |
| **R11** | P1 | **完成 Abstract、Introduction 和 Data Availability**：按 `fcs-paper-outline.md` §2/§3.1/§3.5 把 skeleton 补成英文成文；只写实测与有证据的边界；DOI / 许可字段用占位符待 R3/R5 回填 | ① `docs/fcs-paper-manuscript-draft.md` 补齐 Abstract（五问结构）、Introduction、Data Availability、Code Availability 成文，删除末尾「End of draft sections…」待办说明或改为已完成段落；② Abstract/Introduction 不含新算法、新发现、更快/更好主张；③ Data Availability 写明 9 文件 / 68,518,847 B / `sha256sum -c` 9/9 OK / 双镜像 / 许可状态（1 confirmed + 8 待确认，按 R3 结论），DOI 处留 `{{SOFTWARE_DOI}}`/`{{DATA_DOI}}` 占位并标注「pending Zenodo」；④ 与 `fcs-paper-outline.md` §2/§3.1/§3.5 结构一致 | 改 `docs/fcs-paper-manuscript-draft.md`、`docs/fcs-paper-outline.md`（标记 skeleton→filled）；可选新建 `docs/fcs-paper-abstract-intro.md` | **A** | R3、R5（结论/DOI 占位）、R9、R12 |
| **R12** | P1 | **核查全部参考文献的 DOI、作者及支撑关系**：对 `fcs-paper-outline.md` §7 的 16 条 draft 逐条核对 DOI / 卷期页 / 作者 / 支撑哪个论断；无法核实条目删除或标注，不臆造文献 | ① 新建 `docs/fcs-reference-verification.md` 逐条核查表：编号 → 文献 → DOI → 作者 → 支撑的论文论断 → 核查渠道 → 结论（confirmed / unverifiable）；② §7 中 `⏳` 项（TAIR 2024 *Genetics*、Araport11、Ensembl r62 映射、PRJNA765494、gffutils、JBrowse2、GFF3 spec、JavaFX）逐条给结论，无法访问 DOI/Crossref/出版商时**标「受网络限制未能确证」而非判为已确证**；③ `fcs-paper-manuscript-draft.md` §Reference numbering key 状态同步；④ 删去任何无法核实的条目 | 新建 `docs/fcs-reference-verification.md`；改 `docs/fcs-paper-outline.md` §7、`docs/fcs-paper-manuscript-draft.md` 编号键 | **C** | 无（结果输入 R11） |
| **R13** | P2 | **开展科研人员用户测试**：招募 ≥3 名真人参与者，固定任务 + 随机工具顺序，记录成功/错误/外部编辑/时间，产出可用性定性结论。**本机无法招募真人**；只交协议与空白结果模板 | ① `docs/fcs-user-study-protocol.md` 含五任务协议、招募 ≥3 人、随机工具顺序、记录表、知情同意模板、定性结论模板；② `docs/fcs-user-study-results.md` 为**空白模板并显式标注「未执行：需作者招募 ≥3 名参与者」**；③ `fcs-paper-manuscript-draft.md` §4.2/§4.4 如实写「未做用户研究 / future work」，不得写成已开展；④ 不作省时/易用性主张 | 新建 `docs/fcs-user-study-protocol.md`、`docs/fcs-user-study-results.md`（空白+未执行标注）；改 `docs/fcs-paper-manuscript-draft.md` §4.2/§4.4 | **B** | R4（安装包）、R6（任务协议） |
| **R14** | P2 | **完成 macOS、Linux 原生桌面人工验证**：三平台真机「启动 → 导入 → 浏览 → 导出 → 存/读项目」手动清单 + 原生对话框/DPI/驱动核查。**本机仅 Windows x64**；只交清单与空白结果模板 | ① `docs/fcs-desktop-native-verification-checklist.md` 含 macOS / Linux 各一份手动清单（含 Monocle 无头测试不覆盖的原生对话框/DPI/SmartScreen 类项）；② 结果模板显式标注「未执行：需 macOS / Linux 真机」；③ `docs/desktop-verification.md` 补「Windows x64 已测 / macOS·Linux 待真机」状态；④ GitHub Actions 三平台构建 artifact **不等于**原生人工验证，须明确区分，不得互相替代 | 新建 `docs/fcs-desktop-native-verification-checklist.md`（含空白结果模板）；改 `docs/desktop-verification.md`、`docs/desktop-distribution.md` | **B** | R2、R4 |

**收尾统一推送（用户最终要求，非独立编号）**：代码/文档 → `origin`(gitee) + `github`；数据 → `phytoscope-reference-data` 两镜像。**主理人已实测两个远端 `git ls-remote` 均可达、推送凭证可用（§1），故该动作不再受写权限阻塞，属本机可执行的 A 类动作**（区别于 §6.1 中仍需作者完成的 Zenodo/真机/许可动作）。验收：`git status --porcelain` 为空、`git rev-parse HEAD` 与汇总文档记录一致、tag `v0.3.0` 存在、两远端 `git ls-remote` 指向同一提交；新建 `docs/fcs-final-push-checklist.md` 记录推送前后状态与两端 commit 对照。注意：Release 资产上传 / Zenodo deposit 仍属网页或 API 动作，不在 git push 范畴；任一动作未实际执行须如实标注「未推送 / 未发布」，**不得称已推送或已发布**。

---

## 4. 用户故事（简要）

1. 作为**投稿作者**，我希望 CI 里的研究模块金标准测试真正运行并有可追溯报告，以便论文「正确性」主张有当前版本的真实证据，而不是外迁前的历史留痕。
2. 作为**投稿作者**，我希望 8 个数据文件的许可逐条有明确结论和请求清单、薄荷 TPM 的来源缺口被如实处理，以便 Data Availability 与 Zenodo 归档不会因合规问题被审稿人质疑。
3. 作为**论文写作者**，我希望论文图号、PRD、Gap Audit、Manuscript 与绘图脚本指向同一套编号且 Figure S1 能在干净 clone 下复现，以便图表引用不出现自相矛盾。
4. 作为**投稿作者**，我希望清楚知道哪些事项本机已闭环、哪些必须我本人到 Zenodo / 真机 / 招募参与者去完成，以便对外表述准确、不出现「全部完成」的不实结论。
5. 作为**审稿人/复现者**，我希望 Windows 安装包、双镜像数据仓库、Zenodo 上传包与校验和彼此一致，以便按声明的命令复现软件与数据。

---

## 5. 14 项 → 交付物清单映射表

> 「拟新建」= 本次新增文件；「拟修改」= 在既有文件上增量编辑；「待回填」= 仅占位、由 B 类外部动作触发后才填值。列出主仓相对路径，`phytoscope-reference-data/` 前缀为数据仓库。

| 需求 | 拟新建 | 拟修改 | 待回填 / 外部产生 |
|---|---|---|---|
| R1 | `docs/fcs-ci-goldset-fix-report.md` | `.github/workflows/validate.yml`（`:48` isoform 路径、`:51` 金标准传参、`:83-88` Real case 位点）；`tools/check_isoform_case.py`；`tools/compare_promoters_bedtools.py`、`tools/compare_homology_blastp.py`（守卫，视实现） | 三平台 `validation` 由 failure 转 success 的远端 run（R2 回收） |
| R2 | `docs/ci-reports/v0.3.0/{ubuntu,windows,macos}/**`；`docs/fcs-phase2-verification-report.md` | `docs/fcs-submission-gap-audit.md` #10（仅在结果真实回收后） | GitHub Actions run id / 三平台报告（远端） |
| R3 | `docs/fcs-data-license-decision.md` | `phytoscope-reference-data/LICENSES.md`、`MANIFEST.json`、`SHA256SUMS.txt`；`docs/zenodo-deposit-checklist.md` §5 | （待拍板）删 `data/species/mint/mentha_longifolia_expression.tsv`；权利人书面许可（作者） |
| R4 | `dist/PhytoScope-Windows-0.3.0.zip`；`docs/fcs-v0.3.0-release-notes.md` | `desktop_program/SHA256SUMS.txt`；`docs/desktop-distribution.md`、`docs/desktop-verification.md`、`README.md`、`CHANGELOG.md` | 新建 `v0.3.0` Release 并上传资产（当前 Release 仅 `v0.2.0`；网页/API 动作，推送凭证已可用） |
| R5 | `docs/fcs-zenodo-handoff.md`；`phytoscope-v0.3.0-source.zip`；`phytoscope-reference-data-v0.3.0.zip` | `docs/zenodo-deposit-checklist.md`；`README.md` Citation、`CITATION.cff`、`docs/fcs-paper-outline.md` §3.6、`docs/fcs-paper-manuscript-draft.md`（仅加占位符标注） | `{{SOFTWARE_DOI}}`、`{{DATA_DOI}}`、`{{ORCID}}`、`{{AFFILIATION}}`、`{{PAPER_DOI}}`（作者 Zenodo 发布后） |
| R6 | `docs/fcs-tool-comparison-final.md`；`docs/fcs-tool-comparison-external.md`（空白协议） | `docs/fcs-phase3-comparison-plan.md`（定稿标注） | 外部实机补充结果（若有） |
| R7 | `src/test/resources/gff2/**`；`src/test/java/**/Gff2ParserSupportTest.java` | `docs/input-format-specification.md`；`docs/fcs-paper-outline.md` §3.2(b)；`docs/fcs-paper-manuscript-draft.md` §2.2；`docs/prd-fcs-submission.md` R1.1/R3.1 备注 | — |
| R8 | `docs/paper-figures/data/gffutils-summary.json`、`biopython-summary.json` | `docs/paper-figures/make_paper_figures.py`、`README.md`；重生成 `Figure{1-5,S1}.{png,svg}` | — |
| R9 | `docs/fcs-figure-numbering-map.md` | `docs/fcs-paper-outline.md` §4；`docs/fcs-paper-manuscript-draft.md`；`docs/prd-fcs-submission.md` §4（加修订说明）；`docs/fcs-submission-gap-audit.md`；`docs/paper-figures/README.md`、`make_paper_figures.py` | — |
| R10 | `docs/fcs-performance-benchmark.md`；`benchmarks/results/2026-11-14-v0.3.0/**` | `docs/performance.json`（追加指针）；`docs/fcs-paper-outline.md` §5.1；`docs/fcs-paper-manuscript-draft.md` §3.4/§4.3 | — |
| R11 | 可选 `docs/fcs-paper-abstract-intro.md` | `docs/fcs-paper-manuscript-draft.md`（Abstract/Introduction/Data & Code Availability 成文）；`docs/fcs-paper-outline.md`（skeleton→filled） | DOI / 最终许可结论（R3/R5 后回填） |
| R12 | `docs/fcs-reference-verification.md` | `docs/fcs-paper-outline.md` §7；`docs/fcs-paper-manuscript-draft.md` 编号键 | DOI/Crossref 确证结果（网络可达时升级） |
| R13 | `docs/fcs-user-study-protocol.md`；`docs/fcs-user-study-results.md`（空白+未执行标注） | `docs/fcs-paper-manuscript-draft.md` §4.2/§4.4 | ≥3 名参与者实测数据（作者） |
| R14 | `docs/fcs-desktop-native-verification-checklist.md`（含空白结果模板） | `docs/desktop-verification.md`、`docs/desktop-distribution.md` | macOS/Linux 真机人工记录（作者） |
| 收尾推送 | `docs/fcs-final-push-checklist.md` | — | 两端 commit 对照 / push 记录（**凭证已实测可用，属本机 A 类动作，非 B 类阻塞**） |

---

## 6. 外部依赖项专章

### 6.1 必须由作者完成的动作（B 类，本机不可替代）

| # | 动作 | 关联需求 | 为什么本机做不了 |
|---|---|---|---|
| A1 | ORCID 登录 Zenodo 网页端，创建 **software** 与 **dataset** 两个 deposit，上传 §6.3 物料，填 metadata / license / related identifiers，发布取得两个 DOI，并回填各占位符 | R5 | 需作者账号与网页端发布，本地无法取得 DOI |
| A2 | 向 Phoenix/TAIR 申请 Araport11 Chr1–Chr5 与 TAIR10 FASTA 再分发的书面许可，**或**拍板改用 NCBI RefSeq（CP002684–CP002688 / INSDC） | R3 | 权利人授权不可代替；替换会改变 SHA-256 与金标准基线 |
| A3 | 向 Mergner/TUM 确认 E-MTAB-7978 数据所有者许可，或拍板按「EBI 无额外限制 + 引用 DOI 10.1038/s41586-020-2094-2」使用 | R3 | 所有者许可缺省，需权利人确认 |
| A4 | 追根薄荷 4 叶 TPM 矩阵真实来源与许可，**或**拍板删除该文件并从数据仓库 / MANIFEST / 论文补表 S1 / Figure 5(c) / Zenodo Deposit B 中一并移除 | R3 | 来源无法确证，禁止编造；删除影响多处既定数字，需拍板 |
| A5 | 在 GitHub Actions 触发修复后 HEAD 的三平台完整 CI，并把 run id 与三平台报告交回团队落盘 | R2 | 依赖远端执行与结果回收 |
| A6 | 招募 ≥3 名科研人员参与者，按协议执行用户测试并交回记录 | R13 | 需真人参与者 |
| A7 | 在 macOS、Linux 真机按手动清单执行原生桌面验证并交回记录 | R14 | 本机仅 Windows x64 |
| A8 | （可选）在外部机器安装 JBrowse2 / IGV / TBtools 补做实机同任务对照 | R6 | 本机环境/网络受限，且 GUI 无脚本化快照 |
| A9 | 在网页/API 完成 **v0.3.0 Release 资产上传**（当前仅 v0.2.0）与 **Zenodo 两个 deposit 发布**（同 A1），并确认收尾推送的两远端 commit 一致 | 收尾 / R4 / R5 | Release 资产与 Zenodo deposit 属网页/API 动作，非 git push；**git push 凭证已实测可用（§1），已不属 B 类阻塞** |

> 说明：主理人已实测两个远端 `git ls-remote` 均可达、**推送凭证可用**，故「收尾统一推送」的 git push 部分属本机可执行的 **A 类动作**（见 §3 收尾段、§8 第 4 条），不再列入本节 B 类外部动作；本节仅保留仍需网页/API 或真人/真机完成的动作。

### 6.2 所需账号 / 权限 / 机器

| 类别 | 具体 |
|---|---|
| 账号 | Zenodo（建议 GitHub 账号关联）+ **ORCID**；gitee 与 github（SSH over 443）**写凭证已实测可用（§1）**；机构/署名信息（`{{AFFILIATION}}`，与 `Song X, Chen P, Liang F` / git 身份 `omixcore` 一致性需核对） |
| 权限 | GitHub Actions 触发与 artifact 下载；gitee/github Release 附件上传（网页/API）；两个仓库 push（**凭证已可用**）；Zenodo deposit 创建/发布；TAIR/Phoenix 与 E-MTAB-7978 所有者的许可确认 |
| 机器 | **macOS 真机**、**Linux 真机**（原生桌面验证）；GitHub Actions 三平台 runner（CI 实跑）；可选带显示环境的机器（三工具 GUI 实机） |
| 人 | ≥3 名科研人员参与者（用户测试） |

### 6.3 团队已备 / 本机可备物料清单

| 物料 | 状态 | 关联 |
|---|---|---|
| `docs/zenodo-deposit-checklist.md`（两个 deposit 文件清单、Description、Citation 模板、Related DOI 占位、作者动作 5 步） | **已存在**，待按 R3 结论定稿 | R5 |
| `phytoscope-reference-data/LICENSES.md` + `MANIFEST.json`（9 文件逐条条款查证，1 confirmed / 8 needs_confirmation） | **已存在**，待结论定稿 | R3 |
| `docs/fcs-phase3-comparison-plan.md` §3/§4（gffutils/Biopython 实机数据 + 三工具定性矩阵） | **已存在**，待定稿标注 | R6、R8 |
| `target/phase3-comparison/{gffutils,biopython}/summary.json`（原始 JSON） | 运行时产物，需冻结到 `docs/paper-figures/data/` | R8 |
| `docs/paper-figures/`（Figure1–5 + S1 的 PNG/SVG + `make_paper_figures.py` + README） | **已存在**，待复现修复 | R8、R9 |
| `docs/fcs-paper-outline.md` §7（16 条参考文献 draft，✅/⏳ 标注） | **已存在**，待逐条核查 | R12 |
| `docs/validation-and-benchmarks.md`（五任务用户协议）、`docs/desktop-verification.md`（手动清单） | **已存在**，可复用为协议底稿 | R13、R14 |
| 待本机生成 | `phytoscope-v0.3.0-source.zip`（`git archive v0.3.0`）、`phytoscope-reference-data-v0.3.0.zip`（`2c6608b` 归档）、`dist/` 安装包、`SHA256SUMS.txt`、各空白模板 | R4、R5、R13、R14 |

---

## 7. 风险与待确认问题（需作者/主理人拍板）

| # | 风险 / 问题 | 影响 | 建议处置 |
|---|---|---|---|
| Q1 | **许可结论影响 Data Availability 与 Zenodo**：8 条 `needs_confirmation` 未定稿前，Zenodo dataset deposit 的 license 字段不能选「CC BY 4.0」归并；论文 §3.5 不能写「许可齐全」 | R3 → R5 → R11 连锁 | 先定稿 R3 结论；Zenodo license 暂选「Other/多许可 + 引用 LICENSES.md」；Data Availability 如实列 1 confirmed / 8 待确认 |
| Q2 | **薄荷 4 叶 TPM 来源无法确证（硬缺口）**：figshare 条目文件清单中无 TPM 矩阵，MANIFEST 的 `source_url` 实为基因组组装页 | 若删除：数据仓库 9 文件 / 68,518,847 B、`SHA256SUMS.txt`、Figure 5(c)、补表 S1、论文 §2.5、Zenodo Deposit B 全部要改 | **必须给删除或替换建议，不得编造来源**；删除与否需作者拍板（见 §6.1 A4） |
| Q3 | **Araport11/TAIR10 是否替换为 NCBI RefSeq**：替换可消除再分发风险，但会改变所有 SHA-256 与 promoter 8/8、四物种 fixture 等金标准基线，需重跑 CI 与论文数字 | R3 → R1 → R2 → R11 | 两方案二选一（申请书面许可 vs 替换 INSDC 开放数据）需作者拍板；替换则本 PRD 验收数值需整体修订 |
| Q4 | **CI 报告真实性（已由实测确认的硬缺口）**：`c79034e` 最新实跑 **三平台 `validation` 全 failure**（ubuntu isoform 步 `FileNotFoundError`；windows/macos `Real case` 步 `Unknown exact gene ID: AT1G67090` 退出码 3）；Java `Tests run: 211, Skipped: 12`，其中 `ResearchModuleGoldSetTest = Skipped: 1` → gold-set JSON 不生成 → bedtools/BLASTP 两处对比形同虚设。旧文档「2026-10-18 三平台通过（bedtools 8/8、BLASTP 20/20）」是数据外迁前留痕，**禁止沿用为当前状态** | R1/R2；论文 Figure 3 数字 | R1 按 §1.1 修三类代码失败并本地留痕；R2 只认修复后 HEAD 的真实 run、逐平台确认 failure→success；`public-data-validation`（PMC/PastDB/ENA 外网）**单独归类为外部网络类失败**，不并入三平台代码结论；**禁止复用历史 CI 结论冒充当前实测** |
| Q5 | **论文图号一致性**：`fcs-paper-outline.md` §4、旧 PRD §4、Manuscript、脚本 docstring/README 四处编号错位（Figure 2/4 定义、两处「Figure 5」、S1 定位） | R8/R9/R11；审稿人易发现自相矛盾 | 以 `fcs-figure-numbering-map.md` 为唯一权威；旧 PRD 采用「加修订说明」最小改动，不重排旧编号 |
| Q6 | **Figure S1 复现**：原始 JSON 在 `target/`（运行时、干净 clone 缺失），脚本路径与 docstring 图号错标 | R8 | 冻结到 `docs/paper-figures/data/`，删除 `target/` 后跑脚本验证 6 图全出 |
| Q7 | **内存数据口径**：Windows 无 `resource` 模块，gffutils/PhytoScope RSS 不可测；历史 `docs/performance.json` 为 runner-specific | R10；论文 §4.3 | 只报 JVM 堆池峰值 + 标 `not measured`；历史值一律标 historical；不得编造 RSS |
| Q8 | **三平台 CI ≠ 原生桌面人工验证**：CI artifact 是构建产物，Monocle 不覆盖原生对话框/DPI/SmartScreen | R2 与 R14 不得互相替代 | 两份文档分别标注「CI 构建」「真机人工」 |
| Q9 | **B 类被误写成已完成**：用户曾明确纠正「全部完成」不实表述 | 全项目对外表述 | 严格执行 §8 口径；回执与汇总文档用「A 已闭环 / B 物料齐备待作者 / C 给结论」措辞 |
| Q10 | **版本号与命名 + Release 现状**：tag `v0.2.0`/`v0.3.0` 并存、`dist/` 两个历史 ZIP 命名、pom 3.2.5 易被误当应用版本；**GitHub Release 目前只有 `v0.2.0`、无 `v0.3.0`** | R4、R5 | 统一到 `v0.3.0`；README/CHANGELOG 明确「3.2.5 = Spring Boot 父 POM」；`dist/` 只保留一个 0.3.0 命名；补建 `v0.3.0` Release（勿误用现有 v0.2.0 Release） |
| Q11 | **用户测试 / 三工具实机 / 参考文献确证的范围**：是否纳入本次投稿，还是明确列为 future work | R6/R12/R13 | 建议：用户测试与三工具实机列 future work 并如实标注；参考文献若 DOI/Crossref 可达则 R12 从 C 升级为逐条确证 |
| Q12 | **收尾推送 / 发布动作区分**：主理人实测两远端 `git ls-remote` 均可达、**推送凭证可用**（风险已解除）；但 Release 资产上传与 Zenodo deposit 仍属网页/API 动作 | 用户最终要求 | git push 按 A 类本机执行并留痕；Release/Zenodo 网页动作未执行前如实标注「未发布」，**不得把「凭证可用」写成「已推送 / 已发布」** |
| Q13 | **`public-data-validation` 失败归类**：外部 PMC/PastDB/ENA 网络数据源不可达导致 job failure，非代码逻辑 | R1/R2 验收口径 | R1 不将其纳入代码修复对象；R2 单独如实记录为外部网络类失败，不得据此判定三平台 `validation` 代码失败、也不得混入「CI 转 success」结论 |

---

## 8. 「完成」的判定口径（禁止把 B 类写成已完成）

1. **A 类（R1/R4/R7/R8/R9/R10/R11）**：以**文件存在 + 命令/测试通过**判定。例如 R1 = 三类代码类 CI 失败已修 + 本地 `ResearchModuleGoldSetTest` 非 skipped（Skipped 数由 12 下降）+ `research-module-gold-set.json` 真实生成且被 bedtools/BLASTP 两处 Python 对比成功消费（**「三平台 validation job 转 success」的目标态由 R2 远端实跑出具证据，不计入本机 A 类闭环**）；R8 = 删除 `target/` 后 `python docs/paper-figures/make_paper_figures.py` 一次成功出 6 图；R4 = `dist/` ZIP + `SHA256SUMS.txt` `sha256sum -c` OK + smoke PASS（Release 资产上传属网页/API 动作，未做须标「待发布」）。文档类 A 项以「成文且与对照表一致」判定。
2. **B 类（R2/R3/R5/R6/R13/R14）**：以**物料齐备且如实标注为准**。判定 = 协议/清单/上传包/空白结果模板/占位符登记齐全 + 文档明确写「待作者执行 / 未执行 / 未取得 DOI / 未获授权 / 未实测」。**取得 DOI、招募真人、macOS/Linux 真机记录、GitHub Actions 三平台真实报告、权利人书面许可、实机 GUI 计时 —— 任一未由作者完成时，对应项一律记为「物料齐备（待执行）」，禁止记为「已完成」。**
3. **C 类（R12）**：以**结论成文 + 逐条标注确证渠道与「无法确证」项**判定；受网络/权限限制未访问到 DOI/Crossref/出版商的条目标「未能确证」，不判 confirmed，不臆造文献。
4. **收尾推送**：主理人已实测两远端 `git ls-remote` 可达、**推送凭证可用**，故 git push 按 **A 类**执行并以「两远端指向同一提交 + 工作区干净 + `docs/fcs-final-push-checklist.md` 记录」判定；Release 资产上传 / Zenodo deposit 属网页/API 动作，未实际执行前记「未发布」。**「凭证可用」不等于「已推送 / 已发布」**。
5. **对外表述口径（强制）**：本次可写「**A 类 7 项本机已闭环、B 类 6 项物料齐备待作者执行、C 类 1 项给出核查结论**」；**不得写「14 项全部完成」「已完成 Zenodo 归档/取得 DOI」「已完成用户测试」「已完成三平台原生验证」「许可问题已解决」等表述**。另特别禁止：把 `c79034e` 三平台全 failure 说成「CI 三平台通过」；把「推送凭证可用」说成「已推送」；把「现有 v0.2.0 Release」说成「已发布 v0.3.0」；把 `public-data-validation` 外部网络失败计入代码失败。禁止把历史 CI / 历史性能值 / 外迁前留痕冒充当前实测。

---

> 本 PRD 为需求基线，交架构师据此做技术方案与任务分解。所有验收数值以 §1 基线及 R1/R3 定稿后的实际结果为准；若 Q3（替换为 RefSeq）被采纳，需整体重跑并修订相应验收数字。
