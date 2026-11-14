# PhytoScope FCS · 收尾推送与发布核对表（裁定 6 / M01 / M02）

> 批次：B03 · 执行人：工程师寇豆码（Kou，只备清单，不执行推送）· 日期：2026-11-14（ISO-8601 UTC 口径同设计 §7.10）
> 权威规格：`docs/design-fcs-final-delivery.md` §3.5、§5 B03 #12、§0.2 裁定 6、§0.3、§7.8/§7.11；增量 PRD §3 收尾段 / §8。
> **执行主体（M01 / M02）：主理人齐活林**。工程师只备清单与前置产物，**不执行 git push、不上传 Release**。
>
> **项数口径说明**：第三轮下发文字写「11 项核对表」，但权威规格 §3.5 的核对表实为 **14 行**
> （§5 B03 #12 亦写「按 §3.5 建 14 项核对表」）。本清单按**设计 §3.5 全 14 项**落地，
> 覆盖下发要求的全部 11 项要点，不遗漏任何字段；差异已在 B03 回执中如实说明。

---

## 0. 三个独立状态字段（彼此独立，禁止串写）

| 字段 | 含义 | 初值 |
|---|---|---|
| `push_state` | git push（M01，**A 类**本机动作） | **`pushed`（已推送，2026-11-14 主理人执行）** |
| `release_state` | `v0.3.0` Release 资产上传（M02，主理人） | **`published`（已发布，2026-11-14 主理人执行）** |
| `zenodo_state` | Zenodo 两个 deposit 发布（作者 A1，B 类） | `not_published`（未发布，待作者执行） |

> **「推送凭证可用」≠「已推送」≠「已发布」**（PRD §8 第 4/5 条）。
> 当前实测：**`v0.3.0` 已推送、GitHub Release 已公开发布**（tag `v0.3.0` → `0638dbe`；
> 资产 `PhytoScope-Windows-0.3.0.zip` 46,185,966 B（`dcc2051f…`）+ `SHA256SUMS.txt` 2,158 B（`690905ad…`），
> 两资产已于 `2026-11-14T15:38Z` 替换为与发布提交同源的构建，旧资产 `747744359`/`747745997` 已 delete；
> https://github.com/omixcore/phytoscope/releases/tag/v0.3.0 ；`draft=false`、`prerelease=false`、
> `published_at=2026-11-14T14:30:37Z`）。**`v0.2.0` Release 与 tag 保留不删**。
> 不得把「Release 已发布」外推为「Zenodo 已归档 / DOI 已取得」；Zenodo 仍 `not_published`。

---

## 1. 核对表（14 项，按设计 §3.5）

| # | 核对项 | 命令 | 期望 / 失败口径 | 状态 |
|---|---|---|---|---|
| 1 | 主仓工作区干净 | `git status --porcelain` | 输出为空（须先 `git add` 全部新增文档） | ☑ 通过 |
| 2 | HEAD | `git rev-parse HEAD` | 与汇总文档 `docs/ci-reports/v0.3.0/README.md` / 本清单记录一致 | ☑ **发布提交 = `0638dbe`**（= `v0.3.0` tag）；dev tip 为含本文件的登记提交，见 §1.1 |
| 3 | tag | `git tag -l v0.3.0` + `git rev-parse v0.3.0^{commit}` | 存在且指向预期提交；**`v0.2.0` 保留不删** | ☑ `v0.3.0` → `0638dbe`（含远端 `refs/tags/v0.3.0`）；`v0.2.0` → `771b099` 保留 |
| 4 | 版本口径 | `grep -m1 '<version>' pom.xml` | 应用版本 **0.3.0**；**3.2.5 = Spring Boot 父 POM，非应用版本**（README 已显式澄清） | ☑ 0.3.0 |
| 5 | 两远端一致性 | `git ls-remote origin HEAD` / `git ls-remote github HEAD` | 指向同一 SHA | ☑ 改写后重推，三方均 **`0638dbe`**：gitee 用 `git ls-remote` 实测；github 用 GitHub REST API `/repos/omixcore/phytoscope/commits/dev` 实测（`git ls-remote` 走 SSH over 443 曾被 reset，改用 API 绕过） |
| 6 | 数据双镜像一致性 | 两数据仓库远端 `git ls-remote ... HEAD` | 同一提交 | ☑ 均 `66054b7` |
| 7 | 数据仓库工作区 | `git -C <reference-data> status --porcelain` | 输出为空 | ☑ 通过 |
| 8 | 数据校验 | `sha256sum -c SHA256SUMS.txt`（数据仓库） | **9/9 OK**；totals **9 文件 / 68,518,847 B** 不变 | ☑ 9/9 OK |
| 9 | 本地测试 | `sh mvnw -B -Pui-tests -Dphytoscope.referenceDataDir=target/reference-data test` | 绿、`ResearchModuleGoldSetTest` 非 skip、Skipped 总数由 12 下降（B01b 本机实测 `216 / 0 / 0 / 7`） | ☑ `216 / 0 / 0 / 7` BUILD SUCCESS |
| 10 | **git push（M01，A 类）** | 推送代码/文档 → `origin` + `github`；数据 → 双镜像 | **凭证已实测可用，按 A 类本机执行**；成功记 `push_state = pushed`；**失败如实标注「未推送」，不得称已推送** | ☑ **pushed**（2026-11-14） |
| 11 | **两端 commit 对照** | `git rev-parse HEAD` 与两远端 `git ls-remote ... HEAD` 三方比对 | 记录三方实际 SHA 于本清单；**一致才算推送闭环** | ☑ 改写后重推，三方一致于 **`0638dbe`**（发布提交）；登记性文档提交在其之后前进，见 §1.1 |
| 12 | **Release 前置产物（B05 工程师产出）** | `ls -l ../dist/PhytoScope-Windows-0.3.0.zip` + `sha256sum -c desktop_program/SHA256SUMS.txt` | 两个文件必须存在且校验 OK，**M02 才能启动**；不得由主理人代建。**B05 已产出**：`PhytoScope-Windows-0.3.0.zip` 46,185,966 B / SHA `dcc2051f...`，`sha256sum -c` **3/3 OK**；`docs/fcs-v0.3.0-release-notes.md` 已记录 smoke 与 bundle 版本常量说明 | ☑ 通过 |
| 13 | **Release 资产上传（M02，主理人）** | 推送后用**本机缓存凭据**走 GitHub Release API 尝试：补建 `v0.3.0` Release，上传 #12 两个资产 | **成功**才记 `release_state = published` + Release URL/时间；**权限不足/失败**记 `release_state = pending_release（待发布）` + 如实原因；**未执行前一律 `pending_release`，不得称已发布 v0.3.0** | ☑ **published**（Release id 445353264，2026-11-14） |
| 14 | 执行主体 | 固定字段 | M01/M02 均为**主理人齐活林**；工程师只备清单与前置产物；Zenodo deposit 仍属作者 A1 | 主理人已执行 M01/M02 |

### 两端 commit 对照记录（2026-11-14 提交信息英文化之后重推的实测）

> 下表是**本次 force push（commit message 英文化之后）**的实测结果。改写前的旧提交
> （旧 `3e8b83c` / 旧 `49a5fac`）已不在远端可达历史中，其 old→new 映射见 §1.2 的映射表。

| 端 | 重推后 SHA | 核对时间 | 是否一致 |
|---|---|---|---|
| 本机 `git rev-parse HEAD` | `0638dbe67c7bb406cd6fbcbedc6d5e389ec4d8f4` | 2026-11-14 | — |
| `origin`（gitee）`git ls-remote ... HEAD` | `0638dbe67c7bb406cd6fbcbedc6d5e389ec4d8f4` | 2026-11-14 | ✅ 一致 |
| `github`（SSH over 443）`... HEAD` | `0638dbe67c7bb406cd6fbcbedc6d5e389ec4d8f4` | 2026-11-14 | ✅ 一致 |

数据仓库三方一致：本机 / gitee / github 均为 `66054b700d902fdd8171c3565dc50482d5fa6f25`。

**当前三方核对（2026-11-14 改写后重推实测）**

| 端 | 当前 SHA | 获取方式 |
|---|---|---|
| 本机 `git rev-parse v0.3.0^{}` | `0638dbe67c7bb406cd6fbcbedc6d5e389ec4d8f4` | `git` |
| `origin`（gitee） | `0638dbe67c7bb406cd6fbcbedc6d5e389ec4d8f4` | `git ls-remote origin HEAD` |
| `github` | `0638dbe67c7bb406cd6fbcbedc6d5e389ec4d8f4` | **GitHub REST API** `/repos/omixcore/phytoscope/commits/dev` |

> dev 分支 tip 在本次登记性提交之后，等于「包含本文件的提交」。为避免自指（文档无法记录
> 自身所在提交的 SHA），其值以 `git rev-parse HEAD` 为准，不写入本文件。

> **github 侧的获取方式说明（如实）**：`git ls-remote github HEAD` 走 SSH over 443 当时被 reset
> （`Connection reset ... 20.205.243.160 port 443`），故改用 **GitHub REST API** 取 `dev` 分支 HEAD，
> 结果与本地、gitee 一致。**不是**用「凭证可用」代替实测。
>
> github tag 侧同法核对：`/git/ref/tags/v0.3.0` → **commit `0638dbe67c7bb406cd6fbcbedc6d5e389ec4d8f4`**，
> 与本地 `git rev-parse v0.3.0^{}`、gitee `refs/tags/v0.3.0` 一致。

### 1.1 发布提交 vs 分支最新提交（消除口径混淆）

**一句话**：`v0.3.0` tag 打在**内容提交** `0638dbe` 上（含本轮 CI 修复、许可与文档修正）；
本文件所在的**登记性文档提交**在其之后前进，**不含任何代码变更**，因此 tag 不随之移动。

| 项 | 提交 | 说明 |
| --- | --- | --- |
| `v0.3.0` tag → 提交 | `0638dbe67c7bb406cd6fbcbedc6d5e389ec4d8f4` | 实测 `git rev-parse v0.3.0^{}`；本地 / gitee `refs/tags/v0.3.0` / github API `/git/ref/tags/v0.3.0` 三处一致 |
| dev 分支 tip → 提交 | 含本文件的登记提交（SHA 不写入自身） | 为避免自指：文档无法记录自己所在提交的 SHA，以 `git rev-parse HEAD` 为准；三方一致性见上表 |
| 两者关系 | 登记提交是 `0638dbe` 的**后继**（线性历史，无分叉、无 merge） | 提交链：`c79034e` → … → `0638dbe` → 登记提交 |
| 差异内容 | 仅登记性文档更新；**`src/**`、`pom.xml`、`tools/**`、测试零改动** | 可用 `git diff --stat 0638dbe..HEAD -- src pom.xml tools` 验证输出为空 |
| 为何 tag 不随登记提交前移 | **归档可复现性**：tag 一旦跟随「记录发布结果的文档」移动，就会形成「文档描述 tag、tag 又包含文档」的循环，且同一 tag 名在不同时间指向不同内容，破坏 `git archive` 的可复现性。发布单元应由**内容提交**定义，发布后的登记性文档单独前进 | Deposit A 因此固定为 `git archive 0638dbe`（显式 SHA，§2） |
| 因此的读法 | 「发布提交 = `0638dbe`」「dev tip 在登记提交上」两句**同时为真**，不是矛盾 | 下游文档提及「HEAD」时须注明是哪一种口径 |

### 1.2 提交信息英文化（2026-11-14，已完成）

`c79034e..HEAD` 区间的 commit message 由中文统一改写为英文。**提交内容（tree）逐字节不变**，
仅 message 与由之变化的 SHA 不同；改写前后已逐对校验 tree 相同，并 force push 到 gitee 与
github，`v0.3.0` tag 同步前移到 `0638dbe`。

| 旧 SHA | 新 SHA | 备注 |
| --- | --- | --- |
| `b26dbac` | `d99c3f4` | B01 + B02 |
| `ca8743e` | `6349006` | B01b |
| `638fe31` | `c7b29a7` | B03 |
| `3659754` | `ee8d632` | B04 |
| `7eead0e` | `c97bce1` | 第一轮 QA 改进 |
| `e920a12` | `f18ba05` | B05 收口 |
| `3e8b83c` | `adb730d` | B05 收尾登记 |
| `49a5fac` | `76712bd` | 推送清单回填 |
| （新增） | `1aab03f` | 许可决策与薄荷 TPM 溯源文档 |
| （新增） | `148e986` | 发布状态、CI 报告、论文定性表述 |
| （新增） | `e293f7e` | ubuntu CI 修复：每个 UI 测试类独立 JVM + FX 启动超时放宽 |
| （新增） | `0638dbe` | ubuntu CI 修复：恢复被数据外迁打断的校验步骤（= 最终 `v0.3.0`） |
| 数据仓 `249e771` | `f96e375` | 新 tip `66054b7` |

> **本节是「当前有效 SHA」的权威出处**（`docs/ci-reports/v0.3.0/README.md` §6 指向本节）。
> 改写**不影响**：`v0.3.0` 的内容，以及 `docs/ci-reports/v0.3.0/` 里以 **run id
> `43605656726`** 为键的 CI 事实记录（run 与 job 记录不可变）。

Release 实测：`v0.3.0` 已公开发布（`draft=false`、`prerelease=false`、`published_at=2026-11-14T14:30:37Z`；
Release id `445353264`），页面 https://github.com/omixcore/phytoscope/releases/tag/v0.3.0 ，
资产 `PhytoScope-Windows-0.3.0.zip`（46,185,966 B）+ `SHA256SUMS.txt`（2,158 B）。

> **发布动作说明**：本次 **Release 公开动作由主理人经 GitHub API 执行**；
> `git push`（推提交与 tag）与 **Release 资产上传**是**两个不同动作**，
> 不能互相替代，也不能由其中一个推导出另一个已完成。

> **归档前置条件**：**Zenodo 归档与 DOI 回填以数据许可落定为前置条件**；许可未落定前
> **Deposit B 不得公开**（8/9 文件 `needs_confirmation`，见 `docs/fcs-data-license-decision.md`）。
> `zenodo_state` 保持 `not_published`。

> **失败时如实标注**：若第 10/13 项失败，`push_state` / `release_state` 须回退为
> `not_pushed` / `pending_release` 并写明真实原因，禁止用「凭证可用」冒充已推送。

---

## 2. 依赖与前置（如实）

- 第 12 项的两个前置产物（`dist/PhytoScope-Windows-0.3.0.zip`、`desktop_program/SHA256SUMS.txt`）
  已由 **B05** 产出（本批），`sha256sum -c desktop_program/SHA256SUMS.txt` **3/3 OK**；
  `desktop_program/SHA256SUMS.txt` 头部已改 0.3.0 口径，历史两 ZIP 字节不动。
  **第 13 项（M02 Release 上传）由主理人执行，已完成**：`release_state = published`（§0、§1.1）。
  工程师侧的边界不变 —— 工程师**不上传、不发布**，只产出 ZIP 与校验和。
- **B05 归档源 SHA（team-lead 续作指令要求登记）**：
  - Deposit A（软件源码）：`git archive 0638dbe`，**不用 `v0.3.0` tag**。
    理由：归档源必须**可复现**——tag 会随收尾文档提交移动（B05 当时它指向 `edae8433`，
    现已被主理人前移到 `0638dbe`），显式 SHA 永不移动。
    **B05 主 commit（Deposit A 归档源，固定）=
    `0638dbe67c7bb406cd6fbcbedc6d5e389ec4d8f4`**；Deposit A 已用该显式 SHA 生成，
    `unzip -l` 核对含 B05 全部实质文档与最终设计文档，`cmp` 复现逐字节一致。
    **归档源固定、不随其后 HEAD 前移**：本清单的 SHA 登记由 B05 收尾 commit 写入，
    该收尾 commit 不纳入归档快照（快照无法自含 SHA 登记）。主理人 push 前可用
    `git cat-file -t 0638dbe` 复现核对归档源；`git archive v0.3.0` 当时与该 SHA 指向同一提交，
    两者产出的包逐字节相同（ZIP 注释即 `0638dbe…`），但**以显式 SHA 为准**以免 tag 再移动。
    第 2/11 项的**推送闭环**三方 HEAD 比对是另一动作，推送时刻 SHA 已记为 `0638dbe`（§1 表），
    其后仅为登记性文档提交（§1.1）。
  - Deposit B（参考数据）：数据仓库 **当前实际 HEAD =
    `66054b700d902fdd8171c3565dc50482d5fa6f25`**（改写前的旧 SHA `249e771` 已不可达，
    映射见 §1.2），包内含最终 `LICENSES.md` / `MANIFEST.json`，且**已剔除扣留的薄荷 TPM 矩阵**
    （9 文件 → 公开 8 文件 / 65,124,010 B，见 `docs/fcs-data-license-decision.md`）。
  - 两包 `unzip -l` / `unzip -t` 留痕见 `docs/fcs-zenodo-handoff.md` §3.2 与
    `docs/fcs-v0.3.0-release-notes.md` §4。**上传与发布仍属 B 类待作者执行**。
- 第 9 项本地测试的本机绿跑证据在 `docs/fcs-ci-goldset-fix-report.md` 与 B05 全量复跑；
  **三平台 CI 已在远端实跑两次，须按下述「两次」口径引用，不得只引其中一次**：
  - **第一次（run `43605656726`，commit `49a5fac`，历史记录；其失败已修复）**：windows / macos success，
    **ubuntu failure**（job `132479612471`，step 10 `Java and JavaFX regressions`，
    14 个 JavaFX UI 测试无头 latch 超时）。该失败已定位并修复（每个 UI 测试类独立 JVM
    + FX 启动超时放宽），修复记录见 `docs/fcs-ci-goldset-fix-report.md`。
  - **第二次（run `43609281554`，`run_attempt=1`，commit `0638dbe` = 发布提交，绿）**：
    `validation (ubuntu-latest)` **success**（job `132490554008`，`15:16:31Z → 15:32:41Z`）/
    `validation (macos-latest)` **success**（job `132490554359`，`→ 15:19:23Z`）/
    `validation (windows-latest)` **success**（job `132490554399`，`→ 15:24:16Z`）。
    同一次 run 内 `public-data-validation`（job `132490554373`，step 5
    `Audit public Arabidopsis splicing and proteomics resources`）失败，属**外部网络类失败**，
    是独立 job、不在三平台矩阵内，**单列不计入代码失败**。
    金标准测试已在远端实跑且 `Skipped: 0`（`ResearchModuleGoldSetTest 1/0/0/0`、
    `DomainCaseValidationTest 1/0/0/0`、`Gff2ParserSupportTest 5/0/0/0`、
    `ComparisonWorkflowUiTest 1/0/0/0`）；ubuntu 全量 `Tests run: 216, Failures: 0,
    Errors: 0, Skipped: 7` + `BUILD SUCCESS`。
  - **口径**：可以说「**三平台矩阵（ubuntu / macos / windows）在 commit `0638dbe` 上通过**」；
    **不得**说「整条 workflow 全绿」，也不得用本地通过记录替代远端结果。
  详见 `docs/ci-reports/v0.3.0/README.md` §7。
- Zenodo 两个 deposit 发布与 DOI 回填属作者 A1（B 类），见 `docs/fcs-zenodo-handoff.md`；
  **DOI 未取得**。`zenodo_state = not_published`。

---

## 3. 对外口径（强制）

- 固定表述：「**A 类 7 项本机已闭环、B 类 6 项物料齐备待作者执行、C 类 1 项给出核查结论**」。
- 已可正面陈述（有实测证据）：`v0.3.0` **已推送**、GitHub Release **已公开发布**
  （tag `v0.3.0` → `0638dbe`，资产 46,185,966 B + 2,158 B，已于 `2026-11-14T15:38Z`
  替换为与发布提交同源的构建，Release 页面见 §1.1）；**三平台矩阵在 `0638dbe` 上通过**。
- 仍禁止：「14 项全部完成」「已完成 Zenodo 归档/取得 DOI」「已完成用户测试」
  「已完成三平台原生验证」「许可问题已解决」「整条 workflow 全绿」
  「已做三款 GUI 软件同任务实机计时对照」。
- 禁止把「现有 `v0.2.0` Release」与「`v0.3.0` Release」混为一谈；禁止把 `public-data-validation`
  外部网络失败计入代码失败；禁止把「发布提交 = `0638dbe`」与「dev tip 在其后登记提交上」写成互相矛盾（§1.1）。
- `not_collected` / `not_pushed` / `pending_release` / `not_published` 为**尚未执行时的如实值**；
  动作真实执行并取得证据后须回填实测值 + 证据，不得只改状态不写证据。当前回填情况：
  `push_state=pushed`、`release_state=published`、`zenodo_state=not_published`（不变）。
