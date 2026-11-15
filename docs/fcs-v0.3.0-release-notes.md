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
| 资产 1 | `PhytoScope-Windows-0.3.0.zip` — 46,185,966 B，`sha256:dcc2051f…`（asset `747931335`） |
| 资产 2 | `SHA256SUMS.txt` — 2,158 B，`sha256:690905ad…`（asset `747935209`） |
| tag 指向提交 | `0638dbe67c7bb406cd6fbcbedc6d5e389ec4d8f4`（`0638dbe`） |
| 上一个版本 tag | `v0.2.0` → `771b0992edcdec4b654b4ec5724ea4c49056294f`，**保留不删** |

> **资产替换记录（如实）**：Release 首次发布（`2026-11-14T14:30:37Z`）挂的是从 `3e8b83c`
> 构建的包（46,185,790 B，`8f7953eb…`）。commit 信息英文化 + ubuntu CI 修复后，发布提交前移到
> `0638dbe`，故于 `2026-11-14T15:38Z` **删除并重传两个资产**，使「tag / 源码包 / 二进制 /
> 校验和」指向同一棵树。旧包的两个 asset（`747744359` / `747745997`）已 delete，现仅存上表两条；
> 两个新资产的大小与 SHA-256 由 GitHub API 返回的 `digest` 字段逐一核对一致。

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
  > 注：上句指 **B05 批次**范围。发布后另有提交改动 `pom.xml`（UI 测试 profile）与 7 个 UI
  > 测试类、`.github/workflows/validate.yml`、`tools/run_validation.py`；这些提交随
  > **发布提交前移**一并进入 `v0.3.0`（= `0638dbe`），故 §0.1 的两个资产也在
  > `2026-11-14T15:38Z` **重传**（见该节「资产替换记录」）。`src/main/java/**` 仍零改动，
  > 因此应用行为与本批构建一致，差异仅限内嵌构建时间戳。
- **bundle 版本常量 ≠ 应用版本（如实标注）**：`tools/package_desktop.py:21` 里
  `--app-version 0.2.0`（macOS 分支 `1.0.0`）是 jpackage **bundle 版本历史常量**，
  归档名由 `platform.system()/machine()` 生成 `PhytoScope-Windows-AMD64.zip`（不含版本号）。
  按设计约束**本批不修改该脚本**，改用「独立 `--out` 目录 + 构建后重命名」得到权威命名。
  **应用版本一律以 `pom.xml` 0.3.0 / tag / ZIP 命名为准**，二者不等同、不得混淆。
- **tag 口径（分「历史」与「现状」两段写，勿混读）**：
  - **历史（B05 当时）**：`v0.3.0` 曾为 annotated tag，当时指向 `edae8433`（tag 对象）、落后 HEAD
    多个提交、且无 Release 附件关联，故当时**禁止**用 `git archive v0.3.0` 生成源码包。
  - **现状（2026-11-14 重新发布后）**：`v0.3.0` 已**两次前移并同步两远端**，现实测
    `git rev-parse v0.3.0^{}` = `0638dbe67c7bb406cd6fbcbedc6d5e389ec4d8f4`（含本轮 CI 修复与许可修正），
    且绑定公开发布的 GitHub Release（§0.1）。gitee `origin` 侧 `refs/tags/v0.3.0` 同为 `0638dbe`
    （`git ls-remote --tags` 实测）。
  - **归档源（现状）**：Deposit A 以**显式 SHA `0638dbe`** 生成（`git archive 0638dbe`；
    当时 `git archive v0.3.0` 与该 SHA 指向同一提交，产出逐字节相同，但不作为命令写入，
    因为归档源**必须可复现**、不得依赖 tag 的当前位置）。若日后 tag 再随登记性文档前移，
    显式 SHA 方案不受影响。

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
| `../dist/PhytoScope-Windows-0.3.0.zip` | 46,185,966 | `dcc2051f63228ad93f5954a326c157f8b04ba82556d67b1311a2964025a7ed6c` | **2026-11-14 从发布提交 `0638dbe` 重建**（旧构建 `8f7953eb…` 作废），唯一权威发布名，已重传 Release |
| `../dist/PhytoScope-Windows-AMD64.zip` | 113,094,544 | `927b000219cd9b1d08f577c4fb315ff86538656cfc11451872f05c7e4911aa2c` | `historical`，字节不动 |
| `../dist/PhytoScope-Windows-0.2.0.zip` | 110,405,267 | `763b1c42350872047f36eb6c944ea3ebfd10ffaaca7a1fb8a59645a2392b3cce` | `historical`，字节不动 |
| `../dist/phytoscope-v0.3.0-source.zip` | **11,658,264** | `4f1a212591e9e0b32ac41882ed0b656a0c663587afc5ae8cd925709933b1c9f9` | Deposit A（`git archive 0638dbe`，460 项，ZIP 注释即该 SHA） |
| `../dist/phytoscope-reference-data-v0.3.0.zip` | 63,580,701 | `e88a9cfa172244a71b88fe580991160f536bf6e75a91213960e69aadaa180416` | Deposit B（数据仓 `66054b7`，**29 项 / 8 个数据文件**，已剔除扣留的薄荷 TPM；其中可公开再分发仅 1 个 horsemint） |

> 新包约 **44 MiB**，**不是** 110 MB 历史 v0.2.0 包（瘦身生效）。两个历史 ZIP
> **字节未动、未删除、未重建**。
> **`SHA256SUMS.txt` 历史错位如实修正**：旧文件把 `763b1c42...` 挂在
> `PhytoScope-Windows-AMD64.zip` 名下；2026-11-14 逐字节重测表明 `763b1c42...`
> 实为 `PhytoScope-Windows-0.2.0.zip`，而 `AMD64.zip` 实为 `927b0002...`。
> 本次只更正 checksum 文件的「文件名↔哈希」关联，历史 ZIP 本身一字未动。

## 4. R5 Zenodo 上传包（B 类物料，本机只生成、不上传）

| Deposit | 归档源 | 包内必含项校验 |
| --- | --- | --- |
| A（软件源码） | `git archive 0638dbe`（显式 SHA；当时 tag `v0.3.0` 指向同一提交） | `README.md` / `CITATION.cff` / `pom.xml` / `docs/**` / `src/**` / `tools/**` |
| B（参考数据） | 数据仓库 **当前 HEAD `66054b7`**（旧 `249e771` 因 commit 信息英文化已不可达，映射见 `docs/fcs-final-push-checklist.md` §1.2） | `MANIFEST.json` / `SHA256SUMS.txt` / `LICENSES.md` / `scripts/**`；且 `LICENSES.md` 含 B04 §0 三选一结论表、`MANIFEST.json` 含 `license_decision` 字段 |

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
- **三平台 CI 在远端实跑两次，须按「两次」口径引用**：
  - **第一次（run `43605656726`，commit `49a5fac`，历史记录；其失败已修复）**：
    `validation (windows-latest)` success（job `132479612528`）、
    `validation (macos-latest)` success（job `132479612388`）、
    `validation (ubuntu-latest)` **failure**（job `132479612471`，失败于
    **step 10 `Java and JavaFX regressions`**，14 个 JavaFX UI 测试在无头环境下
    `Platform.startup` latch 超时）；`public-data-validation` 失败（job `132479612205`，
    **step 5 `Audit public Arabidopsis splicing and proteomics resources`**）——归因已更正为
    「脚本 XLSX 魔数 bug + PastDB 数据源退役 + 网络环境」（见
    `docs/ci-reports/v0.3.0/public-data-validation/job-summary.md` §归因复核），不计代码失败。
  - **第二次（run `43609281554`，`run_attempt=1`，commit `0638dbe` = 发布提交，三平台绿）**：
    `validation (ubuntu-latest)` **success**（job `132490554008`，`15:16:31Z → 15:32:41Z`）、
    `validation (macos-latest)` **success**（job `132490554359`，`→ 15:19:23Z`）、
    `validation (windows-latest)` **success**（job `132490554399`，`→ 15:24:16Z`）；
    同 run 内 `public-data-validation`（job `132490554373`，step 5 同上）仍失败（归因同上），
    是独立 job、不在三平台矩阵内。
  - **口径**：可以说「**三平台矩阵在 commit `0638dbe` 上通过**」；**不得**说「整条 workflow 全绿」。
- 金标准链路**确已在远端执行**：`ResearchModuleGoldSetTest 1/0/0/0`、
  `DomainCaseValidationTest 1/0/0/0`、`Gff2ParserSupportTest 5/0/0/0`、
  `ComparisonWorkflowUiTest 1/0/0/0`，全部 `Skipped: 0`；ubuntu 全量
  `Tests run: 216, Failures: 0, Errors: 0, Skipped: 7` + `BUILD SUCCESS`。
  详见 `docs/ci-reports/v0.3.0/README.md` §6/§7。
- 本机本地通过记录（`216/0/0/7`）与远端结果是**两条独立证据**，口径不同，不得互相替代。

## 6. 对外口径（禁止虚报）

- 固定表述：「**A 类 7 项本机已闭环、B 类 6 项物料齐备待作者执行、C 类 1 项给出核查结论**」。
- 已可正面陈述（有实测证据）：**`v0.3.0` 已推送、GitHub Release 已公开发布（tag `v0.3.0` →
  `0638dbe`，两个资产 46,185,966 B + 2,158 B，已于 `2026-11-14T15:38Z` 替换为与发布提交同源的构建）**；
  **三平台矩阵（ubuntu / macos / windows）在 `0638dbe` 上通过**。
- 仍禁止：「已完成 Zenodo 归档/取得 DOI」「已完成三平台原生验证」
  「许可问题已解决」「整条 workflow 全绿」「性能测试更快/更好」
  「已做三款 GUI 软件同任务实机计时对照」。
- `zenodo_state = not_published` 为**如实值**，不得在动作真实执行前改成完成态；
  Deposit B 公开以数据许可落定为前置条件。
- 禁止把「Release 已发布」外推为「已取得 DOI / 已归档」；禁止把 `public-data-validation`
  外部网络失败计入代码失败或说成「CI 全绿」。
