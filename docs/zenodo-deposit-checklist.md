# PhytoScope Zenodo 归档上传清单（提交给作者的物料）

> 本文件是交给「作者」的投稿物料，指导其在 **Zenodo 网页端手动**完成软件与数据两仓库的
> 归档并取得 DOI。**Zenodo 注册 / 上传 / 发布动作由作者在网页端手动完成**，本文件不含任何
> 自动上传脚本。所有 `{{DOI}}` / `{{...}}` 均为占位符，待 Zenodo 归档后回填。

## 1. 建议的正式版本号

- **软件：v0.3.0**。软件仓库本地已打 tag `v0.3.0`，对应「数据瘦身 + 导入向导体检（D1–D6）+
  Manifest v2 / schemaVersion 4 + 跨物种可移植性加固」的功能集。
- **数据：以提交 `2c6608b` 为准**（数据仓库当前 HEAD，含最终许可结论：horsemint 注释
  CC BY 4.0 已确证，其余 8 条 needs_confirmation；9 文件 / 68,518,847 B）。数据仓库已镜像于
  gitee + github。数据仓库目前无 tag，建议作者在 Zenodo 归档前先为其打 tag（或直接以提交
  `2c6608b` 为准）。

## 2. 上传文件清单（建议拆分为两个 deposit，各取一个 DOI）

### Deposit A：软件（PhytoScope software）

| 文件 | 描述 |
| --- | --- |
| `phytoscope-v0.3.0-source.zip` | 软件源码归档：`phytoscope` 仓库在 tag `v0.3.0` 的导出（含 README、CHANGELOG、docs/、src/、tests、tools/）。建议用 `git archive` 或平台「Download ZIP」生成 |
| `PhytoScope-Windows-AMD64.zip` | Windows x64 桌面安装包（jlink app-image，瘦身后约 44.01 MiB，无需 JDK）。**注意**：当前树尚未发布新 GitHub Release，请使用瘦身后经 `tools/package_desktop.py` 重新构建的包，勿用历史 v0.2.0 110 MB 包 |
| `SHA256SUMS.txt`（可选） | 上述软件包的 SHA-256 校验和（软件包参考 `desktop_program/SHA256SUMS.txt`） |

### Deposit B：数据（PhytoScope reference data）

| 文件 | 描述 |
| --- | --- |
| `phytoscope-reference-data-v0.3.0.zip` | 独立数据仓库 `phytoscope-reference-data` 在提交 `2c6608b` 的归档（9 个完整参考数据文件 + `MANIFEST.json` + `SHA256SUMS.txt` + `LICENSES.md` + `scripts/`）。已镜像于 gitee/github |
| `SHA256SUMS.txt` | 数据仓库内 9 文件 SHA-256（`sha256sum -c SHA256SUMS.txt` 应 9/9 OK） |

> 软件与数据是两个独立仓库、两套 DOI，建议拆分为两个 Zenodo deposit，并在
> 「Related identifiers」中互相引用（software ↔ dataset：`isSupplementTo` /
> `isRequiredBy`），论文 DOI 发布后再回填 `{{PAPER_DOI}}`。

## 3. 每项描述（Zenodo "Description" 建议文案）

- **软件**：PhytoScope 是一个本地运行的 JavaFX 桌面端植物基因结构浏览器，面向无需专属物种门户、
  无需上传数据、结果可溯源的植物研究者；以统一「导入 → 浏览 → 查询 → 比较 → 导出」管线，跨物种
  完成基因结构查看、表达矩阵挂载、序列提取、蛋白质相似性与结构域映射等分析，并以 SHA-256 溯源
  与版本化 Manifest 保证结果可复现。本版本（v0.3.0）内置瘦身后的 KB 级示例，完整参考数据见
  配套数据归档（{{DATA_DOI}}）。
- **数据**：PhytoScope 完整参考数据归档，含 TAIR10 基因组 FASTA、Araport11 Chr1–Chr5 注释、
  horsemint（*Mentha longifolia*）染色体级基因组注释、E-MTAB-7978 表达图谱与 horsemint 4 叶
  TPM 矩阵；附逐文件 SHA-256 校验和与来源 / 许可清单（见 `MANIFEST.json` 与 `LICENSES.md`）。

## 4. 贡献者 / 机构

- 作者：**Song X, Chen P, Liang F**（与仓库 CITATION / 论文署名保持一致；请作者核对其与 git
  提交身份 `omixcore` 的一致性）
- 机构：`{{AFFILIATION}}`（待作者填写）
- ORCID：`{{ORCID}}`（待作者填写）

## 5. License

- 软件：**MIT**（仓库根 `LICENSE`）。
- 数据：逐文件许可见 `phytoscope-reference-data/LICENSES.md`。已确证：horsemint 注释
  **CC BY 4.0**。待作者确认：Araport11 Chr1–Chr5（TAIR 再分发风险，建议换 NCBI RefSeq
  CP002684–CP002688）、TAIR10 FASTA（Ensembl 第三方数据约束责任在用户）、E-MTAB-7978
  （所有者许可）、mint 4 叶 TPM（来源未明确，硬缺口）。
  Zenodo 数据 deposit 的 license 字段建议在作者完成许可核查后再选（若全部落定可归并为 CC BY 4.0，
  否则选「Other / 多许可」并引用 `LICENSES.md`）。

## 6. Related DOI（占位，归档后回填）

- 软件 DOI：`{{SOFTWARE_DOI}}`
- 数据 DOI：`{{DATA_DOI}}`
- 论文 DOI：`{{PAPER_DOI}}`（投稿后回填）

## 7. Keywords（建议）

plant genomics · gene structure browser · GFF3 / GTF · FASTA · expression matrix ·
JavaFX desktop application · reproducibility · data provenance · cross-species ·
reference data

## 8. Citation 模板（带 `{{DOI}}` 占位）

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

## 9. 待作者在网页端完成的动作

1. 注册 / 登录 Zenodo（建议用 GitHub 账号登录，便于自动关联仓库）。
2. 新建 software、dataset 两个上传，分别上传上述文件。
3. 填写 metadata（title / description / authors / license / keywords / related identifiers）。
4. 发布并取得两个 DOI。
5. 将 DOI 回填到 README「Citation」段与本清单第 6 节占位符。
