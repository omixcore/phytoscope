# PhytoScope 0.3.0 — Release Notes（FCS 投稿候选，B05 收口）

> 批次：**B05（收口）** · 执行人：工程师寇豆码（Kou）· 日期：**2026-11-14**（ISO-8601 UTC）
> 权威规格：`docs/design-fcs-final-delivery.md` §5 B05 #1/#2/#5、§7；增量 PRD R4 第①段。
> **状态口径**：本文件主体记录**本机构建（A 类）**；并已按远端实测回填
> **推送 / GitHub Release 发布**两项 B 类事实（证据见 §0.1）。
> **Zenodo 发布、DOI 回填、macOS/Linux 真机验证仍未完成**，如实标注，不得写成已完成。
> 三平台 CI 已在远端实跑但**非全绿**（§5），不得概括为「三平台通过」。

---

## 0. 三个独立状态字段（与设计 §3.5 一致，禁止串写）

| 字段 | 含义 | 当前值（2026-11-14 实测回填） |
| --- | --- | --- |
| `push_state` | git push（M01，主理人 A 类动作） | **`pushed`（已推送）**，见 `docs/fcs-final-push-checklist.md` §1 第 10/11 项 |
| `release_state` | `v0.3.0` Release 资产上传（M02，主理人） | **`published`（已发布）**，证据见下方 §0.1 |
| `zenodo_state` | Zenodo 两个 deposit 发布（作者，B 类） | `not_published`（未发布，**DOI 未取得**） |

> 上述三个字段值来自**远端实测核对**（GitHub REST API + `git ls-remote`），不是推测。
> `release_state` 由 `pending_release` 变更为 `published` 的依据是 §0.1 的实测证据；
> 该字段在证据出现前不得预填为完成态，此约束仍然有效。

### 0.1 `release_state = published` 的实测证据（可复核）

| 证据项 | 实测值 |
| --- | --- |
| Release tag | `v0.3.0` |
| Release 标题 | `PhytoScope 0.3.0` |
| Release 状态 | `draft = false`、`prerelease = false`（**已公开发布**） |
| 发布时间（UTC） | `2026-11-14T14:30:37Z` |
| Release 页面 | https://github.com/omixcore/phytoscope/releases/tag/v0.3.0 |
| 资产 1 | `PhytoScope-Windows-0.3.0.zip` — 46,185,790 B |
| 资产 2 | `SHA256SUMS.txt` — 1,547 B |
| tag 指向提交 | `3e8b83ceebabb431da201357cedc421f213cbed6`（`3e8b83c`） |
| 上一个版本 tag | `v0.2.0` → `771b0992edcdec4b654b4ec5724ea4c49056294f`，**保留不删** |

> **两个动作必须区分**：`git push`（M01，把提交/tag 推到 gitee 与 github）与
> **Release 资产上传**（M02，GitHub Release API）是**两个独立动作**。本次发布由**主理人经 GitHub API 执行**
> M02，不能由「`git push` 成功」推导出「Release 已发布」，也不能由「Release 已发布」推导出
> 「Zenodo 已归档 / DOI 已取得」。工程师只产出 ZIP 与校验和，**不上传、不发布**。

> **归档前置条件**：**Zenodo 归档与 DOI 回填以数据许可落定为前置条件**。9 个参考数据文件
> 中仅 1 个许可已确认，许可未落定前 **Deposit B 不得公开**（`docs/fcs-data-license-decision.md`、
> `docs/zenodo-deposit-checklist.md` §5）。

---

## 1. 版本口径（B03 已统一，本批保持一致）

- **应用版本 0.3.0**：`pom.xml` `<version>0.3.0</version>`、tag `v0.3.0`、
  唯一权威 ZIP 命名 **`PhytoScope-Windows-0.3.0.zip`**。
- **`3.2.5` 是 Spring Boot 父 POM 版本**（`spring-boot:3.2.5:repackage`），
  **不是应用版本**（见 `README.md` / `CHANGELOG.md`）。
- **`pom.xml` 本批未改动**；`src/main/java/**` 零改动；未引入新依赖、未改算法/解析器。
  > 注：上句指 **B05 批次**范围。若发布后另有修复提交改动 `pom.xml` 或测试代码，
  > 属**发布之后的新变更**，**不改变本 Release（tag `v0.3.0` → `3e8b83c`）的内容**，
  > 且不得据此声称「Release 资产已更新」——资产一经上传即固定，变更需走新版本/新 Release。
- **bundle 版本常量 ≠ 应用版本（如实标注）**：`tools/package_desktop.py:21` 里
  `--app-version 0.2.0`（macOS 分支 `1.0.0`）是 jpackage **bundle 版本历史常量**，
  归档名由 `platform.system()/machine()` 生成 `PhytoScope-Windows-AMD64.zip`（不含版本号）。
  按设计约束**本批不修改该脚本**，改用「独立 `--out` 目录 + 构建后重命名」得到权威命名。
  **应用版本一律以 `pom.xml` 0.3.0 / tag / ZIP 命名为准**，二者不等同、不得混淆。
- **tag 口径（分「历史」与「现状」两段写，勿混读）**：
  - **历史（B05 当时）**：`v0.3.0` 曾为 annotated tag，当时指向 `edae8433`（tag 对象）、落后 HEAD
    多个提交、且无 Release 附件关联，故当时**禁止**用 `git archive v0.3.0` 生成源码包。
  - **现状（主理人 M01/M02 执行后）**：`v0.3.0` 已由主理人**前移并同步两远端**，实测
    `git rev-parse v0.3.0^{}` = `3e8b83ceebabb431da201357cedc421f213cbed6`，且已绑定公开发布的
    GitHub Release（§0.1）。gitee `origin` 侧 `refs/tags/v0.3.0` 同为 `3e8b83c`（`git ls-remote --tags` 实测）。
  - **不变的部分**：Deposit A 的归档源**仍固定为 `git archive e920a12`**（B05 主 commit 显式 SHA）。
    理由不是「tag 不可用」，而是**归档源必须可复现**：tag 会随收尾文档提交移动，
    显式 SHA 不会。工程师**不动 tag**。

## 2. 瘦身说明（沿用 B03 口径）

当前 0.3.0 应用 JAR 只内置 **6 个 KB 级示例**（Arabidopsis / Rice / Maize / Soybean /
Mint / Mint plastid）。完整参考数据（TAIR10 FASTA、Araport11 Chr1–Chr5、horsemint 注释、
E-MTAB-7978 与 mint 4 叶 TPM 共 **9 文件 / 68,518,847 B**）已外迁到独立
`phytoscope-reference-data` 仓库，**不打包进 app image、无运行时下载**。
序列提取需用户导入 FASTA，表达轨道需用户导入矩阵。
git 历史 pack 与 fresh-clone 大小未变；只有当前 checkout 与构建产物变小。

## 3. 本机构建结果（A 类，真实实跑）

| 步骤 | 命令 | 结果 |
| --- | --- | --- |
| 构建 JAR | `sh mvnw -B -DskipTests package` | `BUILD SUCCESS`，产出 `target/phytoscope-0.3.0.jar` |
| 桌面包 | `JAVA_HOME=C:\Program Files\Java\jdk-21 python tools/package_desktop.py --jar target/phytoscope-0.3.0.jar --out target/desktop-v0.3.0` | `DESKTOP_EXIT=0`，产出 `target/desktop-v0.3.0/PhytoScope-Windows-AMD64.zip` |
| packaged smoke | 打包脚本内置 `PhytoScope.exe --benchmark --example rice` | `PASS: packaged runtime launched and parsed the rice example` |
| 权威命名 | `cp target/desktop-v0.3.0/PhytoScope-Windows-AMD64.zip ../dist/PhytoScope-Windows-0.3.0.zip` | `COPY_EXIT=0` |
| CLI smoke | `java -Xmx3g -jar target/phytoscope-0.3.0.jar --benchmark --example rice` | `SMOKE_EXIT=0`，stderr 空，合法 JSON（`import_ms=48.3893`、`genes=24`） |
| 校验和 | `sha256sum -c desktop_program/SHA256SUMS.txt` | **3/3 OK**（新 0.3.0 包 + 两个历史包） |

- **`../dist/` 在工作区根（主仓上一级）**，主仓内**未**误建 `dist/`。
- 首次 `package_desktop.py` 因 `JAVA_HOME` 未设置报
  `A full JDK 21 with jpackage is required`（`DESKTOP_EXIT=1`）；定位到
  `C:\Program Files\Java\jdk-21\bin\jpackage.exe` 后显式设置 `JAVA_HOME` 重跑成功
  （**未修改脚本**）。本机 `jpackage` 不在 PATH，属环境事实，如实记录。

### 3.1 产物清单（真实大小 / SHA-256）

| 产物 | 大小（B） | SHA-256 | 口径 |
| --- | --- | --- | --- |
| `../dist/PhytoScope-Windows-0.3.0.zip` | 46,185,790 | `8f7953eba6c04922be9b2faf21b717ff3fe36cc15a5bd1f3393b38da7716bfa6` | **本批新构建（唯一权威发布名）** |
| `../dist/PhytoScope-Windows-AMD64.zip` | 113,094,544 | `927b000219cd9b1d08f577c4fb315ff86538656cfc11451872f05c7e4911aa2c` | `historical`，字节不动 |
| `../dist/PhytoScope-Windows-0.2.0.zip` | 110,405,267 | `763b1c42350872047f36eb6c944ea3ebfd10ffaaca7a1fb8a59645a2392b3cce` | `historical`，字节不动 |
| `../dist/phytoscope-v0.3.0-source.zip` | **11,636,868** | `e8ca52b88451d207673f6be1f1c62fed0f1c7ca5a515c25ebd502a3109e17b6a` | Deposit A（`git archive e920a12`，B05 主 commit / 归档源固定，已含 B05 全部实质产物） |
| `../dist/phytoscope-reference-data-v0.3.0.zip` | 64,329,519 | `c7eb904c1e8a7977bb751f9b412ec9386d977d6062a4a179e3b4aac7f1361e9b` | Deposit B（数据仓库 B04 HEAD `249e771`） |

> 新包约 **44 MiB**，**不是** 110 MB 历史 v0.2.0 包（瘦身生效）。两个历史 ZIP
> **字节未动、未删除、未重建**。
> **`SHA256SUMS.txt` 历史错位如实修正**：旧文件把 `763b1c42...` 挂在
> `PhytoScope-Windows-AMD64.zip` 名下；2026-11-14 逐字节重测表明 `763b1c42...`
> 实为 `PhytoScope-Windows-0.2.0.zip`，而 `AMD64.zip` 实为 `927b0002...`。
> 本次只更正 checksum 文件的「文件名↔哈希」关联，历史 ZIP 本身一字未动。

## 4. R5 Zenodo 上传包（B 类物料，本机只生成、不上传）

| Deposit | 归档源 | 包内必含项校验 |
| --- | --- | --- |
| A（软件源码） | `git archive e920a12`（B05 主 commit 显式 SHA，归档源固定，**不用 tag**） | `README.md` / `CITATION.cff` / `pom.xml` / `docs/**` / `src/**` / `tools/**` |
| B（参考数据） | 数据仓库 **B04 定稿后实际 HEAD `249e771`** | `MANIFEST.json` / `SHA256SUMS.txt` / `LICENSES.md` / `scripts/**`；且 `LICENSES.md` 含 B04 §0 三选一结论表、`MANIFEST.json` 含 `license_decision` 字段 |

`unzip -t` 两包均 OK（`DEP_A_ZIP_OK` / `DEP_B_ZIP_OK`）；`unzip -l` 留痕见
`docs/fcs-zenodo-handoff.md`。具体归档 SHA 记入 `docs/fcs-zenodo-handoff.md` 与
`docs/fcs-final-push-checklist.md`。

> **上传与发布仍标 B 类待作者执行**；本机未创建任何 Zenodo deposit、**DOI 未取得**。
> Deposit B 的 license 必须选 **Other（多许可）+ 引用 `LICENSES.md`**，
> 8 条 `needs_confirmation` 未获书面许可前不得归并成 CC BY 4.0
> （见 `docs/zenodo-deposit-checklist.md` §5）。

## 5. 测试与验证状态（如实）

- 本机全量回归 `sh mvnw -B -Pui-tests -Dphytoscope.referenceDataDir=target/reference-data test`
  结果见 `docs/fcs-ci-goldset-fix-report.md` 与最终回执（`216 / 0 / 0 / 7`、`BUILD SUCCESS`）。
- `ResearchModuleGoldSetTest` 实跑 `skipped=0`；gold-set JSON 产出并由两个 `compare_*.py`
  消费（守卫顺序已在 QA 复核批次修正）。
- **Windows x64 已测**（本机构建 + packaged smoke + CLI smoke）。
- **macOS / Linux 待真机**：`docs/desktop-verification.md` 与
  `docs/fcs-desktop-native-verification-checklist.md` 为空白清单。
  **GitHub Actions 三平台构建 artifact ≠ 原生人工验证**：Monocle headless 测试不能认证
  原生对话框、DPI 缩放、图形驱动、签名/公证或 SmartScreen 提示。
- **三平台 CI 已在远端实跑，但结果不是全绿**（run `43605656726`、`event=push`、`head_branch=dev`、
  `run_started_at=2026-11-14T14:35:25Z`、commit `49a5fac`）：
  `validation (windows-latest)` success（job `132479612528`）、
  `validation (macos-latest)` success（job `132479612388`）、
  `validation (ubuntu-latest)` **failure**（job `132479612471`，失败于
  **step 10 `Java and JavaFX regressions`**，14 个 JavaFX UI 测试在无头环境下
  `Platform.startup` latch 超时）；`public-data-validation` 失败（job `132479612205`，
  **step 5 `Audit public Arabidopsis splicing and proteomics resources`**）属**外部网络数据源**，不计代码失败。
- 金标准链路**确已在远端执行**：`ResearchModuleGoldSetTest 1/0/0/0`、
  `DomainCaseValidationTest 1/0/0/0`、`Gff2ParserSupportTest 5/0/0/0`、
  `ComparisonWorkflowUiTest 1/0/0/0`，全部 `Skipped: 0`。详见 `docs/ci-reports/v0.3.0/README.md`。
- ubuntu 的修复**已提交但尚未经远端重跑确认**；确认前**不得写「三平台通过」**。
  本机本地通过记录（`216/0/0/7`）**不能替代远端结果**，两者口径不同。

## 6. 对外口径（禁止虚报）

- 固定表述：「**A 类 7 项本机已闭环、B 类 6 项物料齐备待作者执行、C 类 1 项给出核查结论**」。
- 已可正面陈述（有实测证据）：**`v0.3.0` 已推送、GitHub Release 已公开发布（tag `v0.3.0` →
  `3e8b83c`，两个资产 46,185,790 B + 1,547 B）**。
- 仍禁止：「已完成 Zenodo 归档/取得 DOI」「已完成三平台原生验证」
  「许可问题已解决」「CI 三平台全绿」「性能测试更快/更好」
  「已做三款 GUI 软件同任务实机计时对照」。
- `zenodo_state = not_published` 为**如实值**，不得在动作真实执行前改成完成态；
  Deposit B 公开以数据许可落定为前置条件。
- 禁止把「Release 已发布」外推为「已取得 DOI / 已归档」；禁止把 `public-data-validation`
  外部网络失败计入代码失败或说成「CI 全绿」。
