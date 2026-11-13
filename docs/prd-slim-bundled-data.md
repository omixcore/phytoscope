# PhytoScope 增量 PRD：剥离内置完整数据、独立存储参考数据

> **文档类型**：增量 PRD（简单档，不含竞品分析）。本文只描述本次变更；未提及的功能、架构与交互沿用既有设计。
> **编写**：产品经理 许清楚　|　**状态**：v1.2 定稿，与 `docs/design-slim-bundled-data.md` 一致
> **语言**：中文（与用户需求一致）
>
> **v1.2 版本记录**：仅措辞收敛（R4 验收要点、Q2 / Q4 / Q5 / Q6 与 §6.2 标题统一改为主理人已决议口径），**技术方案与 v1.1 及 `docs/design-slim-bundled-data.md` 完全一致**；未改动产品目标、需求池结构、R1–R11 编号、6 条验收标准、数据清单与验收标准实质，也未新增任何实现行号。
>
> **v1.1 纠错登记（依据 `8fd01ca` 源码实测 + 主理人决议）**：① 待移走文件 **10 → 9 个**（字节数 68,518,847 B 不变，初稿为文件数笔误）；② `CATALOGUE` **7 → 6 条**（主理人 Q1 定稿，只移除 `mint-genome`）；③ §4.1 测试清单 **8 → 12 类**，补齐 3 个会编译失败的类；④ R3 明确「空状态已存在」，工作改为补文案 + 修 `afterDatasetChanged()` 刷新时机 + 补 `ResearchAnalysisPane` 第二处 UI；⑤ R8 因 `ProjectStore` 内嵌源字节而**天然满足**，降级为「清空 FASTA + 明确提示」；⑥ 新增数据仓库 `.gitattributes` 必须 `* -text`、删除顺序门禁与 `verify-against-baseline` 脚本。

---

## 0. 变更基线与项目信息

| 项 | 内容 |
|---|---|
| Project Name | `phytoscope` |
| Language | 中文（文档与交付物） |
| Programming Language | Java 21 + Spring Boot 3.2.5 + JavaFX（**既有技术栈，本次不变**；Web 前端默认栈不适用） |
| 代码基线 | `dev` 分支 @ `8fd01ca`（已核对） |
| 远端 | `origin` = `https://gitee.com/omixcore/phytoscope.git`；`github` = `git@github-w1996jy:omixcore/phytoscope.git`（均已同步） |
| 原始需求复述 | 「把内置完整数据去掉，只保留示例数据，完整数据重新建个仓库来储存。」 |
| 产品定位（不变） | *portable local queries of plant genome data* —— 本地、可移植、可复现、非模式物种友好 |

### 0.1 用户已拍板的四项决策（硬性约束，直接采信，不再开放讨论）

1. **剥离范围 = 全部大规模数据**：移走约 70 MB 的完整/真实规模数据，主仓库只保留几十 KB 级小型示例数据集。
2. **git 历史 = 只从当前版本移除**：不改写历史、不用 `filter-repo`、不强推。历史 pack（实测 209 MiB）保持不变，此成本已被用户接受。
3. **新数据仓库 = 两个平台都建**：gitee + github 各一份，互为镜像（同一提交、同一校验和）。
4. **代码适配 = 完全解耦**：默认示例改用小型数据；序列提取改为要求用户自带 FASTA；依赖大数据的测试改为条件跳过或换小 fixture；**不保留「按需下载」机制**。

### 0.2 现状核对结论（已实测，PRD 据此编写）

- `src/main/resources/data/` 下待移走的大文件共 **9 个，合计 68,518,847 字节（≈68.5 MB，十进制）**；保留的示例文件共 6 个，合计 ≈343 KB（单文件 5.5–99.7 KB）。
- 未使用 Git LFS（`.gitattributes` 仅含换行符与二进制标记）；`pom.xml` 无 `<resources>/<excludes>` 配置，`src/main/resources` 全部打进 jar。
- `GeneSequenceService.loadBundledForDataset()` 在资源缺失时 **静默 `return`**（已核对源码 130–133 行）；`ExpressionDataService.loadExample()` 对 `expressionResource == null` 会 `clear()` 后正常返回。二者都不会抛错，因此「不静默失败」需要主动设计用户可见状态，不能依赖现有行为。
- `File → Import genome FASTA…` 菜单项（`MainView:681`）、`GeneSequenceService.loadFromFile(Path)` / `loadFromContent()`、`MainView:299` 的 `describeFastaAlignment()` 染色体名比对警告、以及 `ui/component/GeneDetailPane.buildSequenceSection():165-171` 的「未加载 FASTA」hint 分支 **均已存在**。序列提取解耦是「移除内置自动加载 + 补文案 + 修刷新时机 + 补第二处 UI 面」，**不是新增解析能力、也不是新建空状态控件**。
- **文件数口径（实测纠正）**：`find src/main/resources/data -type f -size +100k` 正好 **9 条**，逐字节加总 = 68,518,847 B。初稿误记为 10 个（疑似把范围外的 `examples/arabidopsis-atlas/expression.tsv` 重复计数），**字节数正确、文件数错误**；本 PRD 统一为 9 个，且删除清单严格限定为 §2.1 的 9 条路径，禁止工程师为凑数自行补删范围外文件。

---

## 1. 产品目标

| # | 目标 | 说明与可度量口径 |
|---|---|---|
| **G1** | **当前版本与构建产物瘦身** | 软件仓库当前检出版本只携带可秒开的小示例；移走 68.5 MB 数据后，jar 与 jlink 安装包体积相应下降。**严谨边界**：因用户选择保留历史，git 仓库体积与全新克隆体积**不会下降**；减少的是「当前检出工作树 + 构建产物」，不得对外宣称「仓库变瘦」。README 中的体积数字须实测后更新。 |
| **G2** | **职责分离与可追溯** | 软件仓库 = 代码 + 小示例 + 元数据；完整参考数据独立版本化存储，逐文件保留 SHA-256、来源 URL、发布版本与许可说明，并与主仓库 commit 建立对应关系。研究者可按需取回、可校验、可复现。 |
| **G3** | **不偏离本地 / 可移植 / 非模式物种友好定位** | 应用**不新增任何网络下载能力、不新增第三方依赖、不新增账号或鉴权**；自带 GFF3/GTF + 表达矩阵 + FASTA 的离线工作流完整保留，且序列提取对任意物种一视同仁（不再对拟南芥特殊化）。 |

三个目标正交：G1 管交付体积，G2 管数据治理，G3 管产品边界。

---

## 2. 范围与边界

### 2.1 移走什么（9 个文件 → 独立数据仓库）

| 类别 | 文件（相对 `src/main/resources/data/`） | 实测体积 | 说明 |
|---|---|---|---|
| 全基因组序列 | `species/arabidopsis/TAIR10_genomic.fna.gz` | 36,462,703 B | 拟南芥 TAIR10 全基因组，Ensembl Plants r62 |
| 完整注释 | `species/arabidopsis/Chr1.gff3.gz` | 4,491,636 B | Araport11 全 5 染色体（5 文件合计 17,071,413 B ≈17 MB；上游给出的「~15.6 MB」为估算值，以实测为准） |
| 完整注释 | `species/arabidopsis/Chr2.gff3.gz` | 2,755,680 B | 同上 |
| 完整注释 | `species/arabidopsis/Chr3.gff3.gz` | 3,382,640 B | 同上 |
| 完整注释 | `species/arabidopsis/Chr4.gff3.gz` | 2,620,794 B | 同上 |
| 完整注释 | `species/arabidopsis/Chr5.gff3.gz` | 3,820,663 B | 同上 |
| 完整注释 | `species/mint/mentha_longifolia_horsemint.gff3.gz` | 10,744,463 B | 94,883 基因完整注释 |
| 表达矩阵 | `species/mint/mentha_longifolia_expression.tsv` | 3,394,837 B | 4 叶 TPM，94,890 行 |
| 表达矩阵 | `case-studies/arabidopsis-atlas/expression.tsv` | 845,431 B | E-MTAB-7978 表达图谱 |
| | **合计（9 条）** | **68,518,847 B ≈ 68.5 MB** | 与「约 70 MB」一致 |

> 上述文件以普通 `git rm` 从当前版本删除并产生一次新提交；历史中的二进制副本保留。
>
> **执行门禁**：`git rm` 清单**严格限定为本表 9 条路径**，逐条列全路径、不通配、不补第 10 个。初稿曾误记为 10 个，若工程师为凑数自行补删，必然误删 §2.5 的范围外文件。若后续确认另有第 10 个文件，必须先由主理人给出精确路径再执行。

### 2.2 保留什么（KB 级示例 + 元数据，均留在主仓库）

| 类别 | 文件 | 实测体积 |
|---|---|---|
| 示例 | `species/rice/rice_chromosomes.gff3` | 49,647 B |
| 示例 | `species/maize/maize_chromosomes.gff3` | 58,568 B |
| 示例 | `species/soybean/soybean_chromosomes.gff3` | 99,707 B |
| 示例 | `species/mint/mentha_longifolia_chromosomes.gff3`（样本子集） | 70,202 B |
| 示例 | `species/mint/mentha_spicata_chloroplast.gff3`（叶绿体） | 59,577 B |
| 示例 | `species/arabidopsis/arabidopsis_chromosomes.gff3.gz`（小型子集） | 5,554 B |
| 元数据 | `reference-provenance.json` | 11,195 B |
| 元数据 | `species/arabidopsis/manifest.json` | 1,821 B |
| 元数据 | `species/arabidopsis/genome-sequence-provenance.json` | 866 B |
| 元数据 | `case-studies/arabidopsis-atlas/{manifest.json, source-values.tsv, samples.tsv}` | 2,391 / 836 / 460 B |
| 配置 | `application.properties` | 保持，需复核是否含指向已移走资源的路径 |

> 元数据本身是 KB 级文本，**不随数据移走**；但其内容与措辞必须同步更新（见 R6）。

### 2.3 不做的事（明确排除）

1. **不改写 git 历史**：不执行 `git filter-repo` / `filter-branch` / BFG，不 `push --force`，不重建远端。
2. **不做「按需下载」**：应用内不新增下载器、不新增 HTTP 客户端依赖、不弹出下载提示、不读取远程数据清单。
3. **不引入 Git LFS**：主仓库与数据仓库均沿用现有 `.gitattributes`；单文件最大 36.5 MB，低于 GitHub 100 MB 硬限制，无需 LFS。
4. **不新增第三方依赖**：`pom.xml` 依赖不变；校验与推送脚本只用 `git` + 系统 `sha256sum` / PowerShell `Get-FileHash` / Python 标准库。
5. **不改动产品核心能力**：GFF3/GTF 解析、基因结构浏览、序列提取算法、比较、区间分析、`.phytoscope` 项目保存/打开等逻辑不重写。
6. **不在本次自动创建远端仓库**：环境限制见 §2.4。
7. **不静默降级或伪造数据**：序列提取在缺 FASTA 时不得返回空串、占位序列或借用其他组装的序列。
8. **不删除可追溯信息**：被移走文件的校验和、来源、许可不得以「删掉了事」的方式丢失。

### 2.4 本次交付 vs 需用户配合的后续动作（关键边界）

| | 内容 | 责任方 |
|---|---|---|
| **本次交付（可独立完成）** | ① 主仓库数据剥离与代码适配；② 测试调整；③ provenance 与文档同步；④ **一个本地独立数据仓库目录**：含全部被移走文件、`SHA256SUMS.txt`、`MANIFEST.json`、`LICENSES.md`、`README.md`、`git init` + 双远端推送脚本 + 校验脚本；⑤ 主仓库新提交（本地） | 开发团队 |
| **需用户配合的后续动作（本次不做，且不得假称会自动完成）** | 在两个平台**各新建一个空仓库**并提供地址，或提供可用的 GITEE / GITHUB token；随后由脚本完成 `git remote add` + 首次提交 + 双端推送，并回填主仓库文档中的仓库链接 | **用户** |

**环境依据（已核对）**：`gh` CLI 已安装但**未登录**，且无 `GITEE_TOKEN` / `GITHUB_TOKEN` 等环境变量；gitee 远期为 HTTPS + credential manager（可推送到**已存在**仓库），github 远期为 SSH 别名 `git@github-w1996jy:omixcore/phytoscope.git`（可推送到**已存在**仓库）。**两者均无通过 API 新建仓库的能力**。因此「两个平台各建一个仓库」在本增量的最后一步之前是阻塞项。

> 交付话术要求：文档与发布说明中应写「完整参考数据已迁移至独立数据仓库（本地目录已就绪，远端地址待用户提供）」，**不得**写「已同步至 gitee / github」。

### 2.5 范围外但需知悉的发现（不擅自扩大范围）

核对全仓 `git ls-files` 时发现，`src/main/resources/data/` 之外仍有被 git 跟踪的大文件：

| 文件 | 体积 | 性质 |
|---|---|---|
| `benchmark-results/parsed-structure.tsv` | 55,741,339 B（≈55.7 MB，全仓最大） | 基准测试**衍生输出**，不进 jar |
| `examples/arabidopsis-isoform-validation/uniprot-domains.tsv` | ≈2.57 MB | 案例研究输入数据 |
| `src/test/resources/non-model/vigna-umbellata/public-expression.tsv` | ≈1.38 MB | 测试资源 |
| `examples/arabidopsis-atlas/expression.tsv` | 845,431 B | 与待移走的 atlas 表达矩阵内容重复 |

这些文件**不在本次剥离清单**（用户决策 #1 的 68.5 MB 口径对应的是 `src/main/resources/data/`），但会影响全新克隆体积与「工作区瘦身」的观感。**已按决议登记为本次不动**，另立议题（见 §6 Q3）。PRD 不擅自将其纳入范围；`git rm` 清单一律以 §2.1 的 9 条路径为准。

---

## 3. 用户故事

| # | 角色 | 用户故事 |
|---|---|---|
| US-1 | 首次启动的用户 | 作为一个第一次打开 PhytoScope 的用户，我希望应用启动后立刻展示一个**秒开、功能完整**的小型示例数据集，以便我在不下载任何东西、不配置任何环境的情况下，马上能浏览基因结构、切换数据集并理解产品能做什么。 |
| US-2 | 首次启动的用户 | 作为一个点击「序列」标签的用户，我希望在未加载 FASTA 时看到**明确的说明和操作指引**（去哪里导入、格式要求、必须与当前注释组装版本匹配），而不是空白面板或来路不明的序列，以便我知道下一步该做什么、且不会误用错误数据。 |
| US-3 | 需要完整数据的研究者 | 作为一个要复现拟南芥 TAIR10 / horsemint 完整注释或 E-MTAB-7978 表达图谱的研究者，我希望从一个**独立、版本化、带 SHA-256 校验与来源许可说明**的数据仓库取回全部原始文件，以便我确认文件未被改动、能对应到具体软件版本，并可在两个平台任选镜像。 |
| US-4 | 需要完整数据的研究者 | 作为一个研究非模式物种的用户，我希望「导入自己的 GFF3 + 表达矩阵 + FASTA」这条路径**完全不依赖任何内置大文件**，以便我的离线工作流与是否取得参考数据仓库无关。 |
| US-5 | 维护者 | 作为维护者，我希望代码、测试、provenance 清单与文档中**不再有任何指向已移走文件的硬编码路径或断言**，以便全新构建与全量测试稳定运行，且未来发布时不会再把这些大文件打进安装包。 |

---

## 4. 需求池

优先级定义：**P0 = 必须有（本次上线阻塞）**；**P1 = 应该有（本次尽量完成，可显式登记为遗留）**；**P2 = 可以有（不阻塞本次）**。

| ID | 优先级 | 需求 | 验收要点 |
|---|---|---|---|
| **R1** | **P0** | **数据文件剥离**：从 `src/main/resources/data/` 删除 §2.1 的 9 个文件，`git rm` 后产生一次新提交；保留 §2.2 全部小示例与元数据 | `find` 结果中无任何 ≥1 MB 文件；`git status` 干净；普通 `git push` 成功，无 `--force` |
| **R2** | **P0** | **`ExampleDataset.CATALOGUE` 与默认示例重选**（主理人 Q1 已定稿）：**只移除 `mint-genome`**；保留 `arabidopsis` 条目但把 `annotationResource` 改为已有的 5.5 KB `arabidopsis_chromosomes.gff3.gz`、`expressionResource` 置 `null`；删除 `annotationResources()` 中硬编码 Chr1–Chr5 五部分的分支；`defaultDataset()`（`CATALOGUE.getFirst()`）仍为 `arabidopsis` | 目录从 **7 条收敛到 6 条**，6 条一一对应小示例；`assertNull(byId("mint-genome"))`；每条 `annotationResource` 在 classpath 存在且为 KB 级；启动默认示例可秒开，基因结构浏览 + 数据集切换均正常 |
| **R3** | **P0** | **序列提取解耦为用户自带 FASTA**：删除 `ARABIDOPSIS_TAIR10_RESOURCE` 常量与 `loadBundledForDataset()` 的内置资源加载语义；移除 `MainView:173`、`:207` 两处自动加载调用，改为显式 `sequenceService.clear()`；复用既有 `File → Import genome FASTA…` + `loadFromFile(Path)`。**空状态无需新建控件**（`GeneDetailPane.buildSequenceSection():165-171` 已有 hint 分支），实际工作为：①按新文案补全提示（导入入口、`*.fa/*.fasta/*.fna/*.gz`、须与当前注释组装版本及序列名匹配）②修刷新时机 —— `afterDatasetChanged():530-541` 不刷新 `detailPane`，切换数据集会残留上一次（可能来自已卸载 FASTA）的序列视图，必须在末尾追加 `detailPane.showGene(selectedGene)` / `clear()` ③补齐 `ResearchAnalysisPane` 第二处序列提取 UI（`:149` promoter / `:159` interval）的提示，其 catch 目前只输出简略的 `"No genome FASTA loaded."` | 无内置 FASTA 时给出清晰提示，不抛异常、不空白、不伪造序列；**切换数据集后 Gene Details 不残留旧序列视图**；用户自带 FASTA 后可正常提取基因/转录本/CDS/UTR/蛋白序列；`MainView:299` 的 `describeFastaAlignment()` 染色体名警告仍生效 |
| **R4** | **P0** | **MainView 表达加载移除/降级**：删除 `MainView:175` 与 `:442` 两处 `loadFromResource("/data/case-studies/arabidopsis-atlas/expression.tsv", …)`；同步处理 `:712–714` 的 "Real expression case: Arabidopsis RBCS atlas…" 菜单项、`loadAtlasCase()` 及 `pinAtlasGenes()`（其 `AT1G67090` 等基因在小示例中不存在，禁止保留）；`startExampleLoad(dataset, atlas)` 的 atlas 分支不得再引用已移走资源 | 全仓 `grep "arabidopsis-atlas/expression"` 在 `src/main` 无命中；切换数据集、启动应用不出现 `IOException: resource not found`；**atlas 相关入口一律移除，不得保留任何入口（含说明性入口）；相关说明只放 README / 数据仓库 README / provenance** |
| **R5** | **P0** | **测试调整策略**：逐一处理依赖大数据的测试类，采用「换小 fixture 优先、条件跳过兜底」；跳过必须带显式条件与原因注释，不得用空测试或直接删除断言蒙混 | 见 §4.1 明细；`mvn test` 全绿或按约定条件跳过，**无任何因缺资源导致的意外失败** |
| **R6** | **P0** | **provenance 清单与文档同步**：见 §4.2 明细 | 见 §4.2 明细 |
| **R7** | **P0** | **独立数据仓库目录**：见 §4.3 明细 | 见 §4.3 明细 |
| **R8** | **P1** | **旧 `.phytoscope` 项目兼容**（实测后降级）：`ProjectStore` 把注释/表达矩阵**字节内嵌进 zip**（`save():57-71` 写 `annotation/part-N.txt`，`load():98-101` 用 `loadParts()` 还原），`Source.origin()` 只是元数据展示 —— **旧项目文件天然不依赖 classpath 数据，「能开则开」已满足**。剩余工作量仅为：`MainView.applyRestoredProject():469` 打开项目时清空 `sequenceService`，并补一条「数据已外迁 / 需自行导入 FASTA」的明确提示 | 构造一个旧项目文件可正常打开；打开后 Gene Details 显示明确的未加载 FASTA 提示，不残留任何旧序列视图 |
| **R9** | **P1** | **`tools/` Python 脚本解耦**：`fetch_tair10_genome.py`（`DEST` 指向 `TAIR10_genomic.fna.gz`）、`build_mint_horsemint_gff3.py`、`screen_domain_isoforms.py`（两处 `TAIR10_genomic.fna.gz`）改为接受外部路径参数，或迁入数据仓库 `scripts/` | 主仓库 `tools/` 中无指向已删除文件的**默认**路径；脚本 `--help` 说明数据来源 |
| **R10** | **P1** | **新增回归守卫测试**：遍历 `ExampleDataset.CATALOGUE`，断言每个条目的 `annotationResource` / `expressionResource`（非空时）在 classpath 存在，且单文件体积低于阈值（建议 256 KB） | 今后若有人把 MB 级数据放回 `src/main/resources/data/`，测试立即失败 |
| **R11** | **P2** | **旧案例的可选复现说明**：在独立数据仓库 README 中说明「如需复现旧完整案例，如何将文件放回本地并导入」，不提供应用内一键下载 | 文档完整、可执行、不引入网络能力 |

### 4.1 R5 测试调整明细（12 个类，必须逐项落实）

> **纠错说明**：初稿只列了 6 个大数据依赖类 + 2 个间接类，漏了 3 个会因为「删常量 / 删方法 / 改签名」而**直接编译失败**的类（已实测补齐，见 #7–#9）。`CATALOGUE` 由 7 → **6 条**，不是 5 条。以下与架构设计 `docs/design-slim-bundled-data.md` §7.1 一致。

| # | 测试类 / 位置 | 现有依赖 | 处理策略 |
|---|---|---|---|
| 1 | `GeneSequenceServiceTest:49-62` | `ARABIDOPSIS_TAIR10_RESOURCE`（**删常量会编译失败**）；`:61` 断言 `sourceLabel` 含 `TAIR10_genomic.fna.gz` | 换用 `loadFromContent()` 构造短 FASTA fixture，断言 `isLoaded()` / 正负链 `extract()` / 自定义 label；**新增**「未加载 FASTA 时 `isLoaded()==false`、`extract()` 抛含明确文案的 `IOException`、不返回任何序列」用例 |
| 2 | `ReferenceDatasetIntegrityTest:14-79, :140-161` | `assertEquals(7, CATALOGUE.size())`；arabidopsis 5 资源 / 5 染色体 / 37,898 基因；`:36` atlas 路径；`:145` mint 表达路径；`mint-genome` 完整断言 | 改为 `assertEquals(6, …)` + `assertNull(byId("mint-genome"))`；删/改写五部分与完整注释断言；删 atlas / mint 表达路径断言；`arabidopsis` 分支改 `assertNull(expressionResource())` + `loadExample()` 后 `isEmpty()`。**保留** rice/maize/soybean 逐字节比对、mint-plastid 134 基因、mint 12 染色体等断言 |
| 3 | `ResearchModuleGoldSetTest:66, :75, :255, :613-634` | `:66` 也调用了 `loadBundledForDataset("arabidopsis")`（初稿漏记）；`Chr1.gff3.gz`、atlas expression、TAIR10 FASTA | 条件跳过（首选）：`Assumptions.assumeTrue(System.getProperty("phytoscope.referenceDataDir") != null, 中文原因)`；断言体**原样保留**，数据路径改指向外部目录。备选换小 fixture，本次不采用 |
| 4 | `MintTranscriptomeUiTest` | 端到端完整 horsemint 转录组 UI | 优先**改写为 `mint` 小示例的等价 UI 流程**（保住 UI 覆盖）；否则在既有 UI 条件外再加 `referenceDataDir` 条件跳过，不得删断言 |
| 5 | `ExpressionDataServiceTest:39-60` | `:42` `loadExample(defaultDataset())` 期望 20000+ profiles；`:52` atlas expression 期望 32833 profiles / 6 组织 | `:39-47` 改为断言 `isEmpty()` + `"No data loaded"`（验证**显式空状态**而非静默）；`:49-60` 改为**内联小 TSV fixture**，保留「6 组织 / unit=TPM / 具体数值 / `getProfile()`」的同等断言结构 |
| 6 | `AtlasProvenanceTest` | `expression.tsv` + 24 格数值校验 | 优先**改写为元数据一致性校验**：删 `expression.tsv` 加载与 24 格比对，保留 `manifest.json` / `samples.tsv` / `source-values.tsv` 的 SHA-256 与结构校验；把 `outputs.expression.tsv` 哈希断言改为「key 存在且标记 external」。备选条件跳过 |
| 7 | **`DomainCaseValidationTest:26`（初稿漏列）** | `sequence.loadBundledForDataset("arabidopsis")` 取 TAIR10 全基因组验证 13 个候选蛋白序列 → **删方法会编译失败** | 条件跳过或改为 `loadFromFile(Path.of(referenceDataDir, "data/species/arabidopsis/TAIR10_genomic.fna.gz"))`；`examples/arabidopsis-isoform-validation/*` 属范围外文件仍在仓库中，**不改**；16 候选 / 蛋白序列逐字符比对等断言全部保留 |
| 8 | **`ComparisonWorkflowUiTest:26`（初稿漏列）** | `view.loadAtlasCase()`（方法将删除 → **编译失败**）；`AT1G67090` 等小示例不存在的基因；atlas expression | 优先**改写为小示例流程**：`startExampleLoad(ExampleDataset.defaultDataset())` + 5.5 KB 子集中实际存在的基因 ID；删文件名中的 atlas 措辞；`:183` 表达断言改为「无表达数据时显示明确缺失状态」。保留导出 / 项目往返断言结构 |
| 9 | **`BackgroundLoadingUiTest:33`（初稿漏列）** | `view.startExampleLoad(ExampleDataset.byId("rice"), false)` 双参 → **改签名会编译失败** | 同步改为 `view.startExampleLoad(ExampleDataset.byId("rice"))`；「不阻塞 FX 线程 / 取消不提交 staged data」断言全部保留 |
| 10 | `SvgExporterTest:32`（间接） | `defaultDataset().annotationResources()` | 不改代码，默认示例改选后全量回归；可顺手把 `parseResources(...)` 换成语义等价的单资源 `parseResource(...)` |
| 11 | `AdaptiveChromosomeTest:77`（间接） | 遍历 `CATALOGUE` | 不改代码，全量回归确认 **6 条**均无 NPE / 空资源 |
| 12 | `SpeciesAgnosticTest` / `LegacyAnnotationFixtures` | 独立历史 fixture，与 `CATALOGUE` 无关 | **保持原样**，`assertEquals(4, LegacyAnnotationFixtures.CATALOGUE.size())` 不变 |

> 另：`FullAnnotationValidationTest` 已有 `@EnabledIfSystemProperty`，属既有条件测试，**不改**；`ExpressionIntegrityTest` / `GoldenRegressionTest` 等在全量 grep 中未发现外迁资源引用，归入全量回归确认。
>
> **不假绿三条硬规则**：① 条件跳过必须用 `Assumptions.assumeTrue` + 显式系统属性 + 中文原因，且**断言体原样保留**，仅在条件满足时运行；② 禁止用空测试、`assertNotNull` 或删断言蒙混；③ 被跳过的用例须在报告中可枚举并逐条对应本表。

### 4.2 R6 provenance 与文档明细

**provenance（拆分为「主仓库索引 + 数据仓库权威清单」）**：

- `reference-provenance.json`（主仓库保留）：包含全部大文件校验和，**不得删除追溯信息**。为已外迁条目补充 `"storage": "external"`、`"repository": "<待回填>"` 与 `"moved_at_commit": "<本次提交>"` 之类的标记，使其成为索引而非「内置清单」；小示例条目保持 `bundled`。
- `species/arabidopsis/manifest.json`、`genome-sequence-provenance.json`、`case-studies/arabidopsis-atlas/manifest.json`：KB 级元数据保留，但其中 `scope: "complete nuclear Chr1-Chr5"`、`bundled` 等措辞需标注「对应完整数据已归档至独立数据仓库」，避免读者误以为主仓库仍内置。
- `application.properties` / `pom.xml`：**已实测确认无需功能性改动** —— `application.properties` 仅 5 行 Spring 配置、无任何数据路径；`pom.xml` 无 `<resources>` / `<testResources>` / `<excludes>` / `maven-resources-plugin`，`src/main/resources` 由 Spring Boot parent 默认全量打包，删除 9 个文件后 jar 自动变小，无需加排除规则（仅 `pom.xml:21` 的 `<description>` 中 "four version-pinned reference examples" 措辞过时，列为可选 P2）。

**文档（必须同步，不得残留「内置完整数据」表述）**：

| 位置 | 需处理的内容 |
|---|---|
| `README.md:26` | 体积数字「about 105 MiB」须**实测后**更新，不得沿用无验证的旧值；不得宣称下载/克隆体积下降 |
| `README.md:23/26` 附近 | 「browser opens with the Arabidopsis example」「including the complete horsemint genome with a 4-leaf TPM panel」需按新 CATALOGUE 改写 |
| `README.md:253` | 「The Arabidopsis TAIR10 example bundles … TAIR10_genomic.fna.gz」整段改为「序列提取需用户通过 File → Import genome FASTA 自带与当前注释组装匹配的 FASTA」 |
| `docs/desktop-verification.md`、`docs/portability-and-reproducibility.md`、`docs/validation-and-benchmarks.md`、`docs/chromosome-validation.md`、`docs/research-module-validation.md` | 移除内置大数据描述，说明完整数据从独立数据仓库获取；基准/验证数字若依赖完整数据，标注「历史结果，需外迁数据复现」 |
| `docs/performance.json`、`benchmark-results/performance.json` | 核对是否含依赖大数据的指标；如有，标注数据来源与复现前提，不伪造新数值 |
| 新增 | README 或 `docs/` 增加「完整参考数据获取」小节：说明独立数据仓库用途、目录结构、校验方式、双平台镜像关系、远端地址待回填；**不提供应用内下载** |

### 4.3 R7 独立数据仓库目录（本次交付）

**位置要求**：必须是**主仓库工作树之外的同级独立目录**（建议 `G:/WorkBuddy/2026-10-27-22-02-43/phytoscope-reference-data/`）。放在主仓库内会被同一个 git 跟踪，与「独立版本化存储」目标冲突。

```
phytoscope-reference-data/
├── README.md                 # 用途 / 与主仓库版本对应关系 / 如何校验与取回 / 双平台镜像说明 / 远端地址（待回填）
├── LICENSES.md               # 逐数据集来源、发布版本、许可与使用条款
├── MANIFEST.json             # 权威清单：相对路径、字节数、SHA-256、来源 URL、许可、对应主仓库 commit
├── SHA256SUMS.txt            # 逐文件 SHA-256（相对路径），可机读，供 sha256sum -c 校验
├── .gitattributes            # 必须写 `* -text`；**禁止照抄主仓库的 `* text=auto eol=lf`**
├── data/                     # 保持与主仓库原相对路径一致，便于脚本回填（9 个文件）
│   ├── species/arabidopsis/  # TAIR10_genomic.fna.gz、Chr1–Chr5.gff3.gz
│   ├── species/mint/         # mentha_longifolia_horsemint.gff3.gz、mentha_longifolia_expression.tsv
│   └── case-studies/arabidopsis-atlas/expression.tsv
└── scripts/
    ├── verify.(sh|ps1)              # 校验 SHA256SUMS.txt；输出逐文件 OK/FAIL
    ├── verify-against-baseline.(sh|ps1) # 用 `git show 8fd01ca:<path>` 逐文件比对历史字节，9/9 OK 才允许删主仓库文件
    ├── init-and-push.(sh|ps1)       # git init + 首次提交 + 以参数接收 gitee/github 两个远端 URL + 双端推送
    └── restore-to-app.(sh|ps1)      # 可选：把数据拷回本地主仓库以复现旧完整案例（仅研究用途，默认 --dry-run，不执行 git add/commit）
```

**内容要求**：

- `data/` 下**包含 §2.1 的全部 9 个文件**，字节与主仓库移走前一致；`SHA256SUMS.txt` 逐文件列出校验和（可直接复用 `reference-provenance.json` / `genome-sequence-provenance.json` 中已记录的 SHA-256，但**必须重新计算并与文件比对**，不得照抄）。
- `LICENSES.md` 至少覆盖：TAIR / Araport11（June 2016 release）、Ensembl Plants release 62（TAIR10 基因组 FASTA）、EBI Expression Atlas E-MTAB-7978（含 DOI `10.1038/s41586-020-2094-2`）、horsemint 基因组（figshare `10.6084/m9.figshare.28457522`）、WSU Mint Genome Resources。**逐条注明原始 URL 与许可/使用条款；不确定处如实标注「需确认」，不得编造许可**。
- `MANIFEST.json` 记录每个文件对应的主仓库基线 commit `8fd01ca` 及本次迁移提交号，实现软件版本 ↔ 数据版本可追溯。
- `init-and-push` 脚本：**只接收已存在的空仓库 URL 作为参数**，不调用任何平台 API、不尝试创建仓库；两远端推送同一提交（互为镜像）；脚本头部用注释写明「远端仓库需用户事先在网页创建或提供 token」。
- **跨仓库字节一致性（实测补充）**：主仓库 `.gitattributes` 含 `* text=auto eol=lf`，数据仓库若照抄，检出时 git 可能对 `.tsv` 做换行转换从而改变字节、破坏 SHA-256 校验。因此数据仓库 `.gitattributes` **必须写 `* -text`**；`SHA256SUMS.txt` 用二进制模式重算；`verify-against-baseline` 用 `git show 8fd01ca:<path>` 读历史 blob 而非检出文件比对。
- **删除顺序门禁**：先产出数据仓库目录 → `verify` + `verify-against-baseline 8fd01ca` 全部 9/9 OK → 才允许 `git rm` 主仓库文件。这是防止数据零丢失窗口的硬约束。
- 脚本语言使用系统自带工具（`git`、`sha256sum` / `Get-FileHash`、Python 标准库），不引入新依赖；提供 `.sh` 与 `.ps1` 两种以适配 Windows / Linux。

### 4.4 UI 变更草图（仅列变化点，其余沿用既有布局）

| 区域 | 变更 |
|---|---|
| 数据集下拉框 / Example datasets 菜单 | 条目由 **7 → 6**；移除 "Real expression case: Arabidopsis RBCS atlas…" 菜单项；小示例条目文案中不再出现「complete annotation」「94,883 genes」「4-leaf TPM panel」等描述 |
| Gene Details → Sequences 区 | **不新建控件**：`GeneDetailPane.buildSequenceSection():165-171` 已有 hint 分支，本次只补文案（导入入口 + `*.fa/*.fasta/*.fna/*.gz` + 组装版本/序列名匹配要求）并修刷新时机；**不显示任何序列** |
| 数据集切换后的 Gene Details | `afterDatasetChanged():530-541` 目前不刷新 `detailPane`，会残留上一次的序列视图；末尾须追加 `detailPane.showGene(selectedGene)` / `clear()` |
| Research / 区间分析面板 | `ResearchAnalysisPane:149/:159` 的第二处序列提取 UI，未加载 FASTA 时只输出简略 `"No genome FASTA loaded."`；需统一到与 Gene Details 一致的完整提示 |
| 状态栏 / 加载流程 | 移除「Loading bundled sequences…」进度文案，改为「序列提取需自行导入 FASTA」类提示或不再提示；导入成功后沿用 `MainView:299` 的 `describeFastaAlignment()` 反馈 |
| 主窗口整体 | 布局、颜色、组件不变；不新增对话框、不新增下载/登录入口 |

---

## 5. 关键验收标准（逐条可验证）

| # | 验收标准 | 验证方式 |
|---|---|---|
| **AC-1** | `src/main/resources/data/` 下**不再有任何 MB 级文件**，仅保留 KB 级示例与元数据；`git status` 干净，新提交可用普通 `git push` 推送到 `origin` 与 `github`，**未改写历史、未强推** | `find src/main/resources/data -type f -size +1M` 无输出；`git log --oneline -3` 显示 `8fd01ca` 仍在历史中；`git status --porcelain` 为空；`git push` 与 `git push github` 均成功且无 `--force` |
| **AC-2** | 全新构建可启动，默认示例可秒开且功能完整（基因结构浏览、数据集切换） | 干净环境执行 `mvn clean package`（或既有构建命令）后启动应用；记录默认示例加载耗时（定性「秒开」）；逐一切换 `CATALOGUE` 中所有条目，无异常、无空视图 |
| **AC-3** | 序列提取在无内置 FASTA 时给出**清晰提示**，用户自带 FASTA 后正常工作；**不静默失败、不伪造序列**；切换数据集后不残留旧序列视图 | 启动后不导入 FASTA，打开 Gene Details → Sequences，确认显示说明性空状态且无任何序列文本；在 Research / 区间分析面板触发 promoter / interval 提取，确认同样得到完整提示而非简略异常；**先选中一个基因并导入 FASTA，再切换数据集，确认 Gene Details 不残留上一次的序列视图**（对应 `afterDatasetChanged()` 刷新修复）；随后导入匹配 FASTA，确认基因/转录本/CDS/UTR/蛋白序列正常输出且 `sourceLabel` 为该 FASTA 路径；再导入不匹配的 FASTA，确认 `MainView:299` 的染色体名警告生效 |
| **AC-4** | 全量测试通过或按约定条件跳过，**无因缺资源导致的意外失败**，且**无编译失败** | `mvn test` 输出：0 failures / 0 errors / **编译成功**（重点验证 `DomainCaseValidationTest:26`、`ComparisonWorkflowUiTest:26`、`BackgroundLoadingUiTest:33` 三处签名/方法删除后的编译）；被跳过的用例在报告中可枚举且与 §4.1 **12 类清单**一致，每条带中文原因且断言体保留；不得出现 `NullPointerException`、`IOException: Genome FASTA resource not found`、断言找不到资源等失败 |
| **AC-5** | 独立数据仓库目录含**全部 9 个被移走文件**、逐文件 SHA-256 校验清单、来源/许可说明与推送脚本 | `sha256sum -c SHA256SUMS.txt`（或 PowerShell 等价）全部 OK；`MANIFEST.json` 条目数 **= 9** 且与 `data/` 实际 9 个文件一一对应；`verify-against-baseline 8fd01ca` 逐文件 9/9 OK（用 `git show` 读历史 blob，不依赖检出）；`LICENSES.md` 覆盖 §4.3 列出的全部来源；`.gitattributes` 为 `* -text` 而非 `* text=auto eol=lf`；`scripts/init-and-push.*` 存在且可执行，脚本中不出现任何创建远端仓库的 API 调用 |
| **AC-6** | 代码、测试、文档、脚本中**无残留指向已移走文件的默认路径**；`README.md` 体积数字已实测更新；provenance 可追溯 | `grep -rIn "TAIR10_genomic\|Chr1.gff3\|horsemint\|arabidopsis-atlas/expression\|mentha_longifolia_expression" src/main src/test README.md docs` 命中项仅为「历史说明 / 外迁指针 / 条件跳过注释」，无有效加载路径；`reference-provenance.json` 中大文件条目带 `storage: external` 标记；文档中明确「远端地址待用户提供」 |

> **AC-1 的措辞底线**：验收报告与发布说明中只能写「当前版本检出内容 / 构建产物体积下降」，**不得**写「仓库体积下降」「克隆更快」。历史 pack 实测 209 MiB 保持不变。

---

## 6. 风险与待确认问题

### 6.1 首要待办（阻塞完整闭环，需用户授权）

> **远端仓库创建需用户协助。** 环境已核对：`gh` CLI 未登录，无 GITEE / GITHUB token；gitee 为 HTTPS + credential manager、github 为 SSH 别名，二者**只能推送到已存在仓库，无法通过 API 新建仓库**。用户已拍板「两个平台都建」，但本次增量**只能交付本地独立数据仓库目录 + 推送脚本**，无法自动完成远端创建与推送。
>
> **需用户二选一**：
> - (a) 在 gitee 与 github 网页各创建一个**空仓库**（不要初始化 README），把两个地址提供给团队；或
> - (b) 提供具备 `create_repo` 权限的 token（建议用完即弃）。
>
> 拿到地址/授权后，由 `scripts/init-and-push` 完成首次提交与双端推送，团队回填主仓库文档中的仓库链接。**在此之前，文档不得宣称数据已同步至远端。** 建议仓库命名 `phytoscope-reference-data`，两平台同名、公开/私有属性由用户决定。

### 6.2 决议记录（Q1–Q6 均由主理人定稿，非开放问题）

| # | 问题 | 决议口径（已拍板） |
|---|---|---|
| **Q1（已定稿）** | **默认示例改选哪个小数据集？** | **主理人已拍板**：保留 `arabidopsis` 条目，`annotationResource` 改为已有的 5.5 KB `arabidopsis_chromosomes.gff3.gz`（`reference-provenance.json` 中已有其 SHA-256，属版本固定的合法子集），`expressionResource` 置 `null`，删除 `annotationResources()` 的 Chr1–Chr5 硬编码分支；**CATALOGUE 7 → 6 条（只移除 `mint-genome`）**，`defaultDataset()` 仍为 `arabidopsis`。已在 R2 / §4.1 / §4.4 全文对齐，不再开放讨论。 |
| **Q2（已决议）** | **provenance 清单如何拆分？** | **已决议采用「索引 + 权威清单」双层**：主仓库 `reference-provenance.json` 保留全部条目（含大文件 SHA-256）作为可追溯索引，为外迁条目加 `storage: external` / `repository` / `moved_at_commit` 标记；数据仓库 `MANIFEST.json` 为权威清单、`SHA256SUMS.txt` 为可机读校验；保留的 `manifest.json` / `genome-sequence-provenance.json` / atlas 元数据只改措辞不删条目。避免「删掉大文件就丢掉来源」的追溯断层。 |
| **Q3（已登记）** | **§2.5 范围外大文件是否本次一并处理？**（尤其 55.7 MB 的 `benchmark-results/parsed-structure.tsv`，它是全仓最大跟踪文件） | **已按决议登记为本次不动**：不在用户拍板的 68.5 MB 口径内、不进 jar；四个文件（`parsed-structure.tsv` / `uniprot-domains.tsv` / `public-expression.tsv` / `examples/arabidopsis-atlas/expression.tsv`）另立议题。**执行门禁**：删除清单严格限定 §2.1 的 9 条路径，若后续确认另有第 10 个文件，必须先给出精确路径再执行，**禁止工程师自行补数**。 |
| **Q4（已决议）** | **旧 `.phytoscope` 项目的兼容策略？** | **已决议：能开则开、不做兼容层。** `ProjectStore` 内嵌全部源字节，旧项目文件天然可打开；旧示例 id / 旧 `sourceLabel` 仅作元数据展示，**不按旧 classpath 重新加载**。仅需按 R8（P1）在 `applyRestoredProject()` 清空 FASTA 并给「数据已外迁 / 需自行导入 FASTA」的明确提示。 |
| **Q5（已决议）** | **`atlas` 案例入口如何处置？** | **已决议：彻底移除，不保留任何入口。** 移除 "Real expression case: Arabidopsis RBCS atlas…" 菜单项、`loadAtlasCase()`、`pinAtlasGenes()`，以及 `startExampleLoad` 的 atlas 参数与分支；**不得保留任何说明性入口**，不得留下指向已删资源的死路径。完整案例的相关说明只放 README / 数据仓库 README / provenance。按 R4（P0）执行。 |
| **Q6（已决议）** | **README 体积数字如何更新？** | **已决议：实测后更新。** 剥离后重新打包并实测 jar / jlink ZIP 体积后填写；**禁止**直接沿用「about 105 MiB」或凭 68.5 MB 做减法推算。历史 pack 与克隆体积不变这一点须在发布说明中写明。 |

### 6.3 主要风险

| 风险 | 影响 | 缓解 |
|---|---|---|
| 保留历史导致「瘦身」预期落差 | 用户看到克隆仍约 209 MiB，误以为变更无效 | 文档中前置说明「仅当前版本与安装包变小」，AC-1 明确措辞底线 |
| 静默降级遗留 | `loadBundledForDataset()` 原本资源缺失即静默返回，容易被误当作「已完成适配」 | AC-3 强制验证用户可见空状态；R3 要求显式 `clear()` + 补文案 + 统一 Research 面板提示 |
| **切换数据集残留旧序列** | `afterDatasetChanged():530-541` 不刷新 `detailPane`，此前选中过基因时可能残留来自已卸载 FASTA 的序列视图（AC-3 真实风险点） | R3 明确要求末尾追加 `detailPane.showGene(selectedGene)` / `clear()`；AC-3 增加「先导入 FASTA → 再切换数据集」验证步骤 |
| **删方法/改签名导致编译失败** | `DomainCaseValidationTest:26` / `ComparisonWorkflowUiTest:26` / `BackgroundLoadingUiTest:33` 三处直接引用将被删除的 API | §4.1 已补齐至 12 类；AC-4 增加「编译成功」硬门槛 |
| **跨仓库字节不一致** | 数据仓库 `.gitattributes` 若照抄主仓库 `* text=auto eol=lf`，检出时 `.tsv` 换行转换会改变字节、破坏 SHA-256 | §4.3 强制 `* -text`；AC-5 增加 `verify-against-baseline 8fd01ca` 9/9 OK 校验（用 `git show` 读历史 blob） |
| 为凑文件数误删范围外文件 | 初稿「10 个」笔误可能诱导工程师自行补第 10 个 | §2.1 / Q3 执行门禁：删除清单严格 9 条，补数须主理人先给精确路径 |
| 测试「假绿」 | 直接删断言或整类跳过会掩盖回归 | §4.1 逐类指定策略 + 不假绿三条硬规则；AC-4 要求跳过可枚举、断言体保留 |
| 文档/脚本残留死路径 | 用户按 README 操作找不到数据 | AC-6 全仓 grep 验收；R9 处理 `tools/` 脚本 |
| 数据来源许可表述不实 | 合规风险 | `LICENSES.md` 逐条标注来源与许可，不确定处写「需确认」，不编造 |
| 远端创建阻塞 | 交付看似完整但数据无处可取 | §2.4 与 §6.1 明确区分「本次交付」与「需用户配合」，README 标注地址待回填 |

---

## 7. 交付清单（本次）

1. 主仓库：`data/` 剥离提交 + `ExampleDataset` / `GeneSequenceService` / `MainView` 适配 + 测试调整 + provenance 与文档同步（R1–R6、R8–R10 按优先级）。
2. 独立数据仓库目录（工作树外）：9 个数据文件 + `README.md` + `LICENSES.md` + `MANIFEST.json` + `SHA256SUMS.txt` + `.gitattributes` + `scripts/`（R7）。
3. 验收记录：对应 §5 六条 AC 的验证结果。
4. **待用户配合事项单**：两个空仓库地址或 token → 双端推送 → 回填文档链接（§6.1）。

> 本文为增量 PRD，不替代既有产品文档；变更落地后请同步更新 `README.md` 与相关 `docs/` 文件。
