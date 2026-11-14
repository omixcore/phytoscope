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
> 当前 GitHub Release **只有 `v0.2.0`**，禁止写成「已发布 v0.3.0」。
> 禁止把「推送凭证可用」写成「已推送」；任一动作失败时**如实标注「未推送 / 待发布」**，不得称已完成。

---

## 1. 核对表（14 项，按设计 §3.5）

| # | 核对项 | 命令 | 期望 / 失败口径 | 状态 |
|---|---|---|---|---|
| 1 | 主仓工作区干净 | `git status --porcelain` | 输出为空（须先 `git add` 全部新增文档） | ☑ 通过 |
| 2 | HEAD | `git rev-parse HEAD` | 与汇总文档 `docs/ci-reports/v0.3.0/README.md` / 本清单记录一致 | ☑ `3e8b83c` |
| 3 | tag | `git tag -l v0.3.0` + `git rev-parse v0.3.0^{commit}` | 存在且指向预期提交；**`v0.2.0` 保留不删** | ☑ 前移至 `3e8b83c`（`v0.2.0` 保留） |
| 4 | 版本口径 | `grep -m1 '<version>' pom.xml` | 应用版本 **0.3.0**；**3.2.5 = Spring Boot 父 POM，非应用版本**（README 已显式澄清） | ☑ 0.3.0 |
| 5 | 两远端一致性 | `git ls-remote origin HEAD` / `git ls-remote github HEAD` | 指向同一 SHA | ☑ 均 `3e8b83c` |
| 6 | 数据双镜像一致性 | 两数据仓库远端 `git ls-remote ... HEAD` | 同一提交 | ☑ 均 `249e771` |
| 7 | 数据仓库工作区 | `git -C <reference-data> status --porcelain` | 输出为空 | ☑ 通过 |
| 8 | 数据校验 | `sha256sum -c SHA256SUMS.txt`（数据仓库） | **9/9 OK**；totals **9 文件 / 68,518,847 B** 不变 | ☑ 9/9 OK |
| 9 | 本地测试 | `sh mvnw -B -Pui-tests -Dphytoscope.referenceDataDir=target/reference-data test` | 绿、`ResearchModuleGoldSetTest` 非 skip、Skipped 总数由 12 下降（B01b 本机实测 `216 / 0 / 0 / 7`） | ☑ `216 / 0 / 0 / 7` BUILD SUCCESS |
| 10 | **git push（M01，A 类）** | 推送代码/文档 → `origin` + `github`；数据 → 双镜像 | **凭证已实测可用，按 A 类本机执行**；成功记 `push_state = pushed`；**失败如实标注「未推送」，不得称已推送** | ☑ **pushed**（2026-11-14） |
| 11 | **两端 commit 对照** | `git rev-parse HEAD` 与两远端 `git ls-remote ... HEAD` 三方比对 | 记录三方实际 SHA 于本清单；**一致才算推送闭环** | ☑ 三方一致（见下） |
| 12 | **Release 前置产物（B05 工程师产出）** | `ls -l ../dist/PhytoScope-Windows-0.3.0.zip` + `sha256sum -c desktop_program/SHA256SUMS.txt` | 两个文件必须存在且校验 OK，**M02 才能启动**；不得由主理人代建。**B05 已产出**：`PhytoScope-Windows-0.3.0.zip` 46,185,790 B / SHA `8f7953eb...`，`sha256sum -c` **3/3 OK**；`docs/fcs-v0.3.0-release-notes.md` 已记录 smoke 与 bundle 版本常量说明 | ☑ 通过 |
| 13 | **Release 资产上传（M02，主理人）** | 推送后用**本机缓存凭据**走 GitHub Release API 尝试：补建 `v0.3.0` Release，上传 #12 两个资产 | **成功**才记 `release_state = published` + Release URL/时间；**权限不足/失败**记 `release_state = pending_release（待发布）` + 如实原因；**未执行前一律 `pending_release`，不得称已发布 v0.3.0** | ☑ **published**（Release id 445353264，2026-11-14） |
| 14 | 执行主体 | 固定字段 | M01/M02 均为**主理人齐活林**；工程师只备清单与前置产物；Zenodo deposit 仍属作者 A1 | 主理人已执行 M01/M02 |

### 两端 commit 对照记录（主理人 2026-11-14 已填写）

| 端 | 实际 SHA | 核对时间 | 是否一致 |
|---|---|---|---|
| 本机 `git rev-parse HEAD` | `3e8b83ceebabb431da201357cedc421f213cbed6` | 2026-11-14 | — |
| `origin`（gitee）`git ls-remote ... HEAD` | `3e8b83ceebabb431da201357cedc421f213cbed6` | 2026-11-14 | ✅ 一致 |
| `github`（SSH over 443）`... HEAD` | `3e8b83ceebabb431da201357cedc421f213cbed6` | 2026-11-14 | ✅ 一致 |

数据仓库三方一致：本机 / gitee / github 均为 `249e771a09b28312f46df0f1a02eb7d34f14897f`。

Release：`v0.3.0` 已发布（Release id `445353264`，https://github.com/omixcore/phytoscope/releases/tag/v0.3.0），
资产 `PhytoScope-Windows-0.3.0.zip`（46,185,790 B）+ `SHA256SUMS.txt`（1,547 B）。

> **失败时如实标注未推送**：若第 10 项未执行或失败，`push_state` 保持 `not_pushed`，
> 并在回执写明「未推送」及真实原因，禁止用「凭证可用」冒充已推送。

---

## 2. 依赖与前置（如实）

- 第 12 项的两个前置产物（`dist/PhytoScope-Windows-0.3.0.zip`、`desktop_program/SHA256SUMS.txt`）
  已由 **B05** 产出（本批），`sha256sum -c desktop_program/SHA256SUMS.txt` **3/3 OK**；
  `desktop_program/SHA256SUMS.txt` 头部已改 0.3.0 口径，历史两 ZIP 字节不动。
  **第 13 项（M02 Release 上传）仍不可由工程师执行**，`release_state` 保持
  `pending_release`。第 12 项状态为「B05 已产出（待主理人核对）」，不等于 M02 已启动。
- **B05 归档源 SHA（team-lead 续作指令要求登记）**：
  - Deposit A（软件源码）：`git archive e920a12`，**不用** `v0.3.0` tag（指向 `edae8433`、
    落后多提交、无 Release 附件）。**B05 主 commit（Deposit A 归档源，固定）=
    `e920a1211904709dc61776bf091322a73f7d2010`**；Deposit A 已用该显式 SHA 生成，
    `unzip -l` 核对含 B05 全部实质文档与最终设计文档，`cmp` 复现逐字节一致。
    **归档源固定、不随其后 HEAD 前移**：本清单的 SHA 登记由 B05 收尾 commit 写入，
    该收尾 commit 不纳入归档快照（快照无法自含 SHA 登记）。主理人 push 前用
    `git cat-file -t e920a12` 复现核对归档源；第 2/11 项的**推送闭环**三方 HEAD
    比对是另一动作，实际 SHA 仍待主理人填写。
  - Deposit B（参考数据）：数据仓库 **B04 定稿后实际 HEAD =
    `249e771a09b28312f46df0f1a02eb7d34f14897f`**，包内含最终 `LICENSES.md` / `MANIFEST.json`。
  - 两包 `unzip -l` / `unzip -t` 留痕见 `docs/fcs-zenodo-handoff.md` §3.2 与
    `docs/fcs-v0.3.0-release-notes.md` §4。**上传与发布仍属 B 类待作者执行**。
- 第 9 项本地测试的本机绿跑证据在 `docs/fcs-ci-goldset-fix-report.md` 与 B05 全量复跑；
  **三平台 CI 报告回收**属 **R2 远端实跑的目标态，不计入本机 A 类闭环**
  （见 `docs/ci-reports/v0.3.0/README.md`）。
- Zenodo 两个 deposit 发布与 DOI 回填属作者 A1（B 类），见 `docs/fcs-zenodo-handoff.md`；
  **DOI 未取得**。`zenodo_state = not_published`。

---

## 3. 对外口径（强制）

- 固定表述：「**A 类 7 项本机已闭环、B 类 6 项物料齐备待作者执行、C 类 1 项给出核查结论**」。
- 禁止：「14 项全部完成」「已完成 Zenodo 归档/取得 DOI」「已完成用户测试」
  「已完成三平台原生验证」「许可问题已解决」「已推送」「已发布 v0.3.0 Release」。
- 禁止把「现有 `v0.2.0` Release」说成「已发布 v0.3.0」；把 `public-data-validation` 外部网络失败
  计入代码失败。
- `not_collected` / `not_pushed` / `pending_release` / `not_published` 均为**如实初值**，
  不得在动作真实执行前改成完成态。
