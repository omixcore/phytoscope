# PhytoScope 0.3.0 — Release Notes（FCS 投稿候选，B05 收口）

> 批次：**B05（收口）** · 执行人：工程师寇豆码（Kou）· 日期：**2026-11-14**（ISO-8601 UTC）
> 权威规格：`docs/design-fcs-final-delivery.md` §5 B05 #1/#2/#5、§7；增量 PRD R4 第①段。
> **状态口径**：本文件只记录**本机构建（A 类）**。Release 附件公开、Zenodo 发布、DOI 回填、
> 三平台 CI 报告回收、macOS/Linux 真机验证均属 **B 类待主理人/作者执行**，不得写成已完成。

---

## 0. 三个独立状态字段（与设计 §3.5 一致，禁止串写）

| 字段 | 含义 | 当前值 |
| --- | --- | --- |
| `push_state` | git push（M01，主理人 A 类动作） | `not_pushed`（未推送，见 `docs/fcs-final-push-checklist.md`） |
| `release_state` | `v0.3.0` Release 资产上传（M02，主理人） | **`pending_release`（待发布）** |
| `zenodo_state` | Zenodo 两个 deposit 发布（作者，B 类） | `not_published`（未发布） |

> **Release 附件公开动作由主理人齐活林用远端 API 尝试，未成功前不得称已发布。**
> 当前 GitHub Release **只有 `v0.2.0`**；**`v0.3.0` Release 不存在**。禁止把现有
> `v0.2.0` Release 写成「已发布 0.3.0」。工程师只产出 ZIP 与校验和，**不上传、不发布**。

---

## 1. 版本口径（B03 已统一，本批保持一致）

- **应用版本 0.3.0**：`pom.xml` `<version>0.3.0</version>`、tag `v0.3.0`、
  唯一权威 ZIP 命名 **`PhytoScope-Windows-0.3.0.zip`**。
- **`3.2.5` 是 Spring Boot 父 POM 版本**（`spring-boot:3.2.5:repackage`），
  **不是应用版本**（见 `README.md` / `CHANGELOG.md`）。
- **`pom.xml` 本批未改动**；`src/main/java/**` 零改动；未引入新依赖、未改算法/解析器。
- **bundle 版本常量 ≠ 应用版本（如实标注）**：`tools/package_desktop.py:21` 里
  `--app-version 0.2.0`（macOS 分支 `1.0.0`）是 jpackage **bundle 版本历史常量**，
  归档名由 `platform.system()/machine()` 生成 `PhytoScope-Windows-AMD64.zip`（不含版本号）。
  按设计约束**本批不修改该脚本**，改用「独立 `--out` 目录 + 构建后重命名」得到权威命名。
  **应用版本一律以 `pom.xml` 0.3.0 / tag / ZIP 命名为准**，二者不等同、不得混淆。
- **tag 修正（主理人裁定，覆盖设计原文）**：`v0.3.0` 是 annotated tag，当前指向
  `edae8433`（tag 对象）/ 落后于 HEAD 多个提交，且**无 Release 附件关联**，故
  **禁止**用 `git archive v0.3.0` 生成源码包（会漏掉本次全部改动）。
  Deposit A 一律用 **`git archive HEAD`（最终 commit SHA）**；`v0.3.0` tag 由主理人在
  推送阶段前移到最终提交并同步两远端，工程师**不动 tag**。

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
| `../dist/phytoscope-v0.3.0-source.zip` | 11,589,747 | 见 `docs/fcs-zenodo-handoff.md` | Deposit A（`git archive HEAD`） |
| `../dist/phytoscope-reference-data-v0.3.0.zip` | 64,329,519 | 见 `docs/fcs-zenodo-handoff.md` | Deposit B（数据仓库 B04 HEAD） |

> 新包约 **44 MiB**，**不是** 110 MB 历史 v0.2.0 包（瘦身生效）。两个历史 ZIP
> **字节未动、未删除、未重建**。
> **`SHA256SUMS.txt` 历史错位如实修正**：旧文件把 `763b1c42...` 挂在
> `PhytoScope-Windows-AMD64.zip` 名下；2026-11-14 逐字节重测表明 `763b1c42...`
> 实为 `PhytoScope-Windows-0.2.0.zip`，而 `AMD64.zip` 实为 `927b0002...`。
> 本次只更正 checksum 文件的「文件名↔哈希」关联，历史 ZIP 本身一字未动。

## 4. R5 Zenodo 上传包（B 类物料，本机只生成、不上传）

| Deposit | 归档源 | 包内必含项校验 |
| --- | --- | --- |
| A（软件源码） | `git archive HEAD`（最终 commit SHA，**不用 tag**） | `README.md` / `CITATION.cff` / `pom.xml` / `docs/**` / `src/**` / `tools/**` |
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
- 三平台 CI 报告回收属 **R2 远端目标态**（`docs/ci-reports/v0.3.0/` 骨架初值
  `not_collected`），**不计入本机 A 类闭环**。

## 6. 对外口径（禁止虚报）

- 固定表述：「**A 类 7 项本机已闭环、B 类 6 项物料齐备待作者执行、C 类 1 项给出核查结论**」。
- 禁止：「已发布 v0.3.0 Release」「已完成 Zenodo 归档/取得 DOI」「已完成三平台原生验证」
  「许可问题已解决」「已推送」「性能测试更快/更好」。
- `push_state=not_pushed` / `release_state=pending_release` / `zenodo_state=not_published`
  均为**如实初值**，不得在动作真实执行前改成完成态。
