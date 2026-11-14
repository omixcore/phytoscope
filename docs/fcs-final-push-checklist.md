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
> 当前实测：**`v0.3.0` 已推送、GitHub Release 已公开发布**（tag `v0.3.0` → `3e8b83c`；
> 资产 `PhytoScope-Windows-0.3.0.zip` 46,185,790 B + `SHA256SUMS.txt` 1,547 B；
> https://github.com/omixcore/phytoscope/releases/tag/v0.3.0 ；`draft=false`、`prerelease=false`、
> `published_at=2026-11-14T14:30:37Z`）。**`v0.2.0` Release 与 tag 保留不删**。
> 不得把「Release 已发布」外推为「Zenodo 已归档 / DOI 已取得」；Zenodo 仍 `not_published`。

---

## 1. 核对表（14 项，按设计 §3.5）

| # | 核对项 | 命令 | 期望 / 失败口径 | 状态 |
|---|---|---|---|---|
| 1 | 主仓工作区干净 | `git status --porcelain` | 输出为空（须先 `git add` 全部新增文档） | ☑ 通过 |
| 2 | HEAD | `git rev-parse HEAD` | 与汇总文档 `docs/ci-reports/v0.3.0/README.md` / 本清单记录一致 | ☑ **dev tip = `49a5fac`**（发布提交为 `3e8b83c`，两者区别见 §1.1） |
| 3 | tag | `git tag -l v0.3.0` + `git rev-parse v0.3.0^{commit}` | 存在且指向预期提交；**`v0.2.0` 保留不删** | ☑ `v0.3.0` → `3e8b83c`（含远端 `refs/tags/v0.3.0`）；`v0.2.0` → `771b099` 保留 |
| 4 | 版本口径 | `grep -m1 '<version>' pom.xml` | 应用版本 **0.3.0**；**3.2.5 = Spring Boot 父 POM，非应用版本**（README 已显式澄清） | ☑ 0.3.0 |
| 5 | 两远端一致性 | `git ls-remote origin HEAD` / `git ls-remote github HEAD` | 指向同一 SHA | ☑ 三方均 **`49a5fac`**：gitee `origin` 用 `git ls-remote` 实测；github 用 GitHub REST API `/repos/omixcore/phytoscope/commits/dev` 实测（`git ls-remote` 走 SSH over 443 当时被 reset，改用 API 绕过） |
| 6 | 数据双镜像一致性 | 两数据仓库远端 `git ls-remote ... HEAD` | 同一提交 | ☑ 均 `249e771` |
| 7 | 数据仓库工作区 | `git -C <reference-data> status --porcelain` | 输出为空 | ☑ 通过 |
| 8 | 数据校验 | `sha256sum -c SHA256SUMS.txt`（数据仓库） | **9/9 OK**；totals **9 文件 / 68,518,847 B** 不变 | ☑ 9/9 OK |
| 9 | 本地测试 | `sh mvnw -B -Pui-tests -Dphytoscope.referenceDataDir=target/reference-data test` | 绿、`ResearchModuleGoldSetTest` 非 skip、Skipped 总数由 12 下降（B01b 本机实测 `216 / 0 / 0 / 7`） | ☑ `216 / 0 / 0 / 7` BUILD SUCCESS |
| 10 | **git push（M01，A 类）** | 推送代码/文档 → `origin` + `github`；数据 → 双镜像 | **凭证已实测可用，按 A 类本机执行**；成功记 `push_state = pushed`；**失败如实标注「未推送」，不得称已推送** | ☑ **pushed**（2026-11-14） |
| 11 | **两端 commit 对照** | `git rev-parse HEAD` 与两远端 `git ls-remote ... HEAD` 三方比对 | 记录三方实际 SHA 于本清单；**一致才算推送闭环** | ☑ 当前三方一致于 **`49a5fac`**；推送时刻为 `3e8b83c`，其后 `49a5fac` 为纯文档提交（§1.1） |
| 12 | **Release 前置产物（B05 工程师产出）** | `ls -l ../dist/PhytoScope-Windows-0.3.0.zip` + `sha256sum -c desktop_program/SHA256SUMS.txt` | 两个文件必须存在且校验 OK，**M02 才能启动**；不得由主理人代建。**B05 已产出**：`PhytoScope-Windows-0.3.0.zip` 46,185,790 B / SHA `8f7953eb...`，`sha256sum -c` **3/3 OK**；`docs/fcs-v0.3.0-release-notes.md` 已记录 smoke 与 bundle 版本常量说明 | ☑ 通过 |
| 13 | **Release 资产上传（M02，主理人）** | 推送后用**本机缓存凭据**走 GitHub Release API 尝试：补建 `v0.3.0` Release，上传 #12 两个资产 | **成功**才记 `release_state = published` + Release URL/时间；**权限不足/失败**记 `release_state = pending_release（待发布）` + 如实原因；**未执行前一律 `pending_release`，不得称已发布 v0.3.0** | ☑ **published**（Release id 445353264，2026-11-14） |
| 14 | 执行主体 | 固定字段 | M01/M02 均为**主理人齐活林**；工程师只备清单与前置产物；Zenodo deposit 仍属作者 A1 | 主理人已执行 M01/M02 |

### 两端 commit 对照记录（推送时刻快照，主理人 2026-11-14 填写）

> 下表是**推送动作发生时刻**的快照，**不是**当前分支 tip。推送之后又产生了一个纯文档提交
> `49a5fac`（仅回填本清单），因此「当前 dev tip ≠ 下表 SHA」属正常，不构成不一致。

| 端 | 推送时 SHA | 核对时间 | 是否一致 |
|---|---|---|---|
| 本机 `git rev-parse HEAD` | `3e8b83ceebabb431da201357cedc421f213cbed6` | 2026-11-14 | — |
| `origin`（gitee）`git ls-remote ... HEAD` | `3e8b83ceebabb431da201357cedc421f213cbed6` | 2026-11-14 | ✅ 一致 |
| `github`（SSH over 443）`... HEAD` | `3e8b83ceebabb431da201357cedc421f213cbed6` | 2026-11-14 | ✅ 一致 |

数据仓库三方一致：本机 / gitee / github 均为 `249e771a09b28312f46df0f1a02eb7d34f14897f`。

**当前 dev tip 三方核对（2026-11-14 补充实测）**

| 端 | 当前 SHA | 获取方式 |
|---|---|---|
| 本机 `git rev-parse HEAD` | `49a5fac43c1e7d2c915a9aac4abfca7b2356b581` | `git` |
| `origin`（gitee） | `49a5fac43c1e7d2c915a9aac4abfca7b2356b581` | `git ls-remote origin HEAD` |
| `github` | `49a5fac43c1e7d2c915a9aac4abfca7b2356b581` | **GitHub REST API** `/repos/omixcore/phytoscope/commits/dev` |

> **github 侧的获取方式说明（如实）**：`git ls-remote github HEAD` 走 SSH over 443 当时被 reset
> （`Connection reset ... 20.205.243.160 port 443`），故改用 **GitHub REST API** 取 `dev` 分支 HEAD，
> 结果与本地、gitee 一致。**不是**用「凭证可用」代替实测。
>
> github tag 侧同法核对：`/git/ref/tags/v0.3.0` → **commit `3e8b83ceebabb431da201357cedc421f213cbed6`**，
> 与本地 `git rev-parse v0.3.0^{}`、gitee `refs/tags/v0.3.0` 一致。

### 1.1 发布提交 vs 分支最新提交（消除口径混淆）

**一句话**：`v0.3.0` tag 打在**内容提交** `3e8b83c` 上；`49a5fac` 是它之后的**纯文档提交**，
**不含任何代码变更**，因此 tag 不随文档提交前移。

| 项 | 提交 | 说明 |
| --- | --- | --- |
| `v0.3.0` tag → 提交 | `3e8b83ceebabb431da201357cedc421f213cbed6` | 实测 `git rev-parse v0.3.0^{}`；本地 / gitee `refs/tags/v0.3.0` / github API `/git/ref/tags/v0.3.0` 三处一致 |
| dev 分支 tip → 提交 | `49a5fac43c1e7d2c915a9aac4abfca7b2356b581` | 实测 `git rev-parse HEAD`；gitee `origin HEAD` 与 github API `/commits/dev` 同为 `49a5fac`（三方一致） |
| 两者关系 | `49a5fac` 是 `3e8b83c` 的**直接后继**（线性历史，无分叉、无 merge） | 提交链：`c79034e` → … → `3e8b83c` → `49a5fac` |
| 差异内容 | 仅 `docs/fcs-final-push-checklist.md`（回填 M01/M02 实况）；**`src/**` 零改动、`pom.xml` 零改动、无新增依赖、无测试改动** | 提交标题：`docs(fcs): 回填收尾推送清单 M01/M02 实况 …` |
| 为何 tag 不随文档提交前移 | **归档可复现性**：tag 一旦跟随「记录发布结果的文档」移动，就会形成「文档描述 tag、tag 又包含文档」的循环，且同一 tag 名在不同时间指向不同内容，破坏 `git archive` 的可复现性。发布单元应由**内容提交**定义，发布后的登记性文档单独前进 | 与 Deposit A 固定 `git archive e920a12` 同理（§2） |
| 因此的读法 | 「发布提交 = `3e8b83c`」「当前 dev tip = `49a5fac`」两句**同时为真**，不是矛盾 | 下游文档提及「HEAD」时须注明是哪一种口径 |

> **本节是「当前有效 SHA」的权威出处**（`docs/ci-reports/v0.3.0/README.md` §6 明确指向本节）。
> **历史改写预告**：主理人将把 `c79034e..HEAD` 区间的中文 commit message 统一改写为英文，
> **tree 内容逐字节不变**，仅 message 与 SHA 变化。改写后本节两行的 SHA 会失效，
> 须由主理人重新回填（并同步 `docs/ci-reports/v0.3.0/README.md` §6 的 `rewritten_head_sha`）。
> 改写**不影响**：`v0.3.0` 的内容、Release 两个资产（46,185,790 B / 1,547 B）、
> 以及 `docs/ci-reports/v0.3.0/` 里以 **run id `43605656726`** 为键的 CI 事实记录。

Release 实测：`v0.3.0` 已公开发布（`draft=false`、`prerelease=false`、`published_at=2026-11-14T14:30:37Z`；
Release id `445353264`），页面 https://github.com/omixcore/phytoscope/releases/tag/v0.3.0 ，
资产 `PhytoScope-Windows-0.3.0.zip`（46,185,790 B）+ `SHA256SUMS.txt`（1,547 B）。

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
  - Deposit A（软件源码）：`git archive e920a12`，**不用 `v0.3.0` tag**。
    理由：归档源必须**可复现**——tag 会随收尾文档提交移动（B05 当时它指向 `edae8433`，
    现已被主理人前移到 `3e8b83c`），显式 SHA 永不移动。
    **B05 主 commit（Deposit A 归档源，固定）=
    `e920a1211904709dc61776bf091322a73f7d2010`**；Deposit A 已用该显式 SHA 生成，
    `unzip -l` 核对含 B05 全部实质文档与最终设计文档，`cmp` 复现逐字节一致。
    **归档源固定、不随其后 HEAD 前移**：本清单的 SHA 登记由 B05 收尾 commit 写入，
    该收尾 commit 不纳入归档快照（快照无法自含 SHA 登记）。主理人 push 前可用
    `git cat-file -t e920a12` 复现核对归档源；第 2/11 项的**推送闭环**三方 HEAD
    比对是另一动作，推送时刻 SHA 已记为 `3e8b83c`（§1 表），其后 `49a5fac` 为纯文档提交（§1.1）。
  - Deposit B（参考数据）：数据仓库 **B04 定稿后实际 HEAD =
    `249e771a09b28312f46df0f1a02eb7d34f14897f`**，包内含最终 `LICENSES.md` / `MANIFEST.json`。
  - 两包 `unzip -l` / `unzip -t` 留痕见 `docs/fcs-zenodo-handoff.md` §3.2 与
    `docs/fcs-v0.3.0-release-notes.md` §4。**上传与发布仍属 B 类待作者执行**。
- 第 9 项本地测试的本机绿跑证据在 `docs/fcs-ci-goldset-fix-report.md` 与 B05 全量复跑；
  **三平台 CI 已在远端实跑（run `43605656726`、`event=push`、`head_branch=dev`、
  `run_started_at=2026-11-14T14:35:25Z`、commit `49a5fac`），但结果非全绿**：
  windows success（job `132479612528`）/ macos success（job `132479612388`）/
  **ubuntu failure（job `132479612471`，step 10 `Java and JavaFX regressions`，
  14 个 JavaFX UI 测试无头 latch 超时）**；`public-data-validation`（job `132479612205`，
  step 5 `Audit public Arabidopsis splicing and proteomics resources`）
  为外部网络类失败，单列不计代码失败。
  金标准测试已在远端实跑（`ResearchModuleGoldSetTest 1/0/0/0` 等，`Skipped: 0`）。
  **本地通过记录不能替代远端结果**；ubuntu 修复尚未经远端重跑确认，确认前不得写「三平台通过」。
  详见 `docs/ci-reports/v0.3.0/README.md`。
- Zenodo 两个 deposit 发布与 DOI 回填属作者 A1（B 类），见 `docs/fcs-zenodo-handoff.md`；
  **DOI 未取得**。`zenodo_state = not_published`。

---

## 3. 对外口径（强制）

- 固定表述：「**A 类 7 项本机已闭环、B 类 6 项物料齐备待作者执行、C 类 1 项给出核查结论**」。
- 已可正面陈述（有实测证据）：`v0.3.0` **已推送**、GitHub Release **已公开发布**
  （tag `v0.3.0` → `3e8b83c`，资产 46,185,790 B + 1,547 B，Release 页面见 §1.1）。
- 仍禁止：「14 项全部完成」「已完成 Zenodo 归档/取得 DOI」「已完成用户测试」
  「已完成三平台原生验证」「许可问题已解决」「CI 三平台全绿」
  「已做三款 GUI 软件同任务实机计时对照」。
- 禁止把「现有 `v0.2.0` Release」与「`v0.3.0` Release」混为一谈；禁止把 `public-data-validation`
  外部网络失败计入代码失败；禁止把「tag 在 `3e8b83c`」与「dev tip 是 `49a5fac`」写成互相矛盾（§1.1）。
- `not_collected` / `not_pushed` / `pending_release` / `not_published` 为**尚未执行时的如实值**；
  动作真实执行并取得证据后须回填实测值 + 证据，不得只改状态不写证据。当前回填情况：
  `push_state=pushed`、`release_state=published`、`zenodo_state=not_published`（不变）。
