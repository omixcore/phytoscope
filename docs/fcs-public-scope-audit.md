# PhytoScope FCS · 数据仓库实际公开范围核查（P0）

> 批次：**B04-P0** · 执行人：工程师寇豆码（Kou）· 取证日期：**2026-11-14**
> 权威规格：主理人 P0 优先级表第 1 项「核查数据仓库实际公开范围」。
> 对应物料：`phytoscope-reference-data/`（HEAD `8906399`，双镜像 Gitee + GitHub 均指向同一 commit）。
> 核查口径：以**匿名（无 token、无登录）**的 HTTP HEAD/GET 请求判定「是否可公开下载」，
> 与「是否已获许可再分发」严格分离——本文件只回答前者（物理可下载性），不涉及许可结论。

---

## 0. 醒目结论（不得删改）

1. **GitHub 镜像：9 个文件全部匿名可下载（HTTP 200）**，含全部大文件；
   抽查 `Chr1.gff3.gz` 返回 `Content-Length: 4491636`（与本地逐字节一致），完整下载 `gzip -t` OK。
2. **Gitee 镜像：7 个「小文件」匿名可下载（HTTP 200），2 个「大文件」匿名不可下载（HTTP 403）。**
   403 原因为平台层限制，响应体明示 **`large file require login for access`**（需登录），
   与许可无关。这 2 个大文件是：
   - `data/species/arabidopsis/TAIR10_genomic.fna.gz`（36,462,703 B）
   - `data/species/mint/mentha_longifolia_horsemint.gff3.gz`（10,744,463 B）——**恰是 9 个文件中唯一许可已确证（CC BY 4.0）的那一个**。
3. **公开性 ≠ 许可**：GitHub 上 9 个文件「能下载」是**托管平台事实**，不代表「许可已落定」。
   其中 8 个仍为 `needs_confirmation`，许可未落定前**不得**据此声称「可公开再分发」。
   本核查的结论是：**当前 GitHub 仓库对全部 9 个文件做了物理公开暴露**，
   与 `MANIFEST.json`「8 个拟归档、其中仅 1 个可再分发」的许可口径之间，存在
   「物理可见」与「许可允许」的错配，需作者决策是否**收紧 GitHub 公开范围**
   （详见 §4 建议动作）。

---

## 1. 探测方法

| 项 | 取值 |
|---|---|
| 探测时间 | 2026-11-14（本机 UTC） |
| 网络通道 | 本机强制代理 `http://127.0.0.1:50797`，`curl --http1.1`（规避 TLS renegotiation 抖动） |
| 匿名性 | 请求**不带任何认证头**（无 `Authorization`、无 token、无 cookie） |
| GitHub 原始地址 | `https://raw.githubusercontent.com/omixcore/phytoscope-reference-data/main/<path>` |
| Gitee 原始地址 | `https://gitee.com/omixcore/phytoscope-reference-data/raw/main/<path>`（Gitee 会 302 到 `raw.giteeusercontent.com`，本表记录 `-L` 跟随后的最终码） |
| 判定 | 200 = 匿名可下载；403 = 平台拒绝（需登录 / 大文件限制）；302 = 需跟随重定向 |

> 复现提示：GitHub raw 在本机代理下偶发 `SSL: UNEXPECTED_EOF` / 连接重置，需重试 + `--http1.1`；
> 这不是仓库问题，已用 `Content-Length` 全量下载 + `gzip -t` 交叉验证排除。

---

## 2. 逐文件公开性结果（唯一权威）

| # | path | 字节 | license_status | GitHub | Gitee（跟随重定向） |
|---|---|---|---:|---|---:|---:|
| 1 | `data/case-studies/arabidopsis-atlas/expression.tsv` | 845,431 | needs_confirmation | **200** | **200** |
| 2 | `data/species/arabidopsis/Chr1.gff3.gz` | 4,491,636 | needs_confirmation | **200** | **200** |
| 3 | `data/species/arabidopsis/Chr2.gff3.gz` | 2,755,680 | needs_confirmation | **200** | **200** |
| 4 | `data/species/arabidopsis/Chr3.gff3.gz` | 3,382,640 | needs_confirmation | **200** | **200** |
| 5 | `data/species/arabidopsis/Chr4.gff3.gz` | 2,620,794 | needs_confirmation | **200** | **200** |
| 6 | `data/species/arabidopsis/Chr5.gff3.gz` | 3,820,663 | needs_confirmation | **200** | **200** |
| 7 | `data/species/arabidopsis/TAIR10_genomic.fna.gz` | 36,462,703 | needs_confirmation | **200** | **403**（大文件需登录） |
| 8 | `data/species/mint/mentha_longifolia_expression.tsv` | 3,394,837 | needs_confirmation | **200** | **200** |
| 9 | `data/species/mint/mentha_longifolia_horsemint.gff3.gz` | 10,744,463 | **confirmed**（CC BY 4.0） | **200** | **403**（大文件需登录） |

**统计**：
- GitHub：9/9 匿名 200。
- Gitee：7/9 匿名 200，2/9 匿名 403（均为 >10 MB 的大文件，Gitee 平台「大文件需登录」限制）。

---

## 3. 关键证据（逐源取证）

### 3.1 GitHub raw 匿名可下载 + 字节一致性

- `HEAD` 返回 `HTTP/1.1 200 OK`、`Content-Length: 4491636`、`Content-Type: application/octet-stream`，
  且**无任何认证头**。
- 全量下载 `Chr1.gff3.gz` 得 **4,491,636 B**（与本地 `ls -l` 及 `SHA256SUMS.txt` 记录完全一致），
  `gzip -t` 通过。
- 结论：GitHub 侧 9 个文件**物理上全部公开**，可匿名完整下载。

### 3.2 Gitee 大文件 403「需登录」

- 对 `TAIR10_genomic.fna.gz`（36 MB）与 `horsemint.gff3.gz`（10 MB）发起匿名请求，
  响应体明示：**`large file require login for access`**。
- 对 7 个 <10 MB 文件，匿名请求 302 跟随后最终 **200**，小文件（`expression.tsv`）返回实际内容。
- 结论：Gitee 对「大文件」设了登录墙（平台机制，非本项目设置）；`repository` 页面本身 200 公开。

### 3.3 双镜像一致性

- `git ls-remote` 校验：Gitee HEAD = `8906399071c4ac6ac120cfb7f952846fb534a9f4`，
  GitHub HEAD = 同一值。**两镜像内容与本地 HEAD 完全一致**。

---

## 4. 发现与建议动作（B 类，作者 / 主理人执行）

### 发现 A：GitHub 对 8 个 `needs_confirmation` 文件做了物理公开暴露

当前 GitHub 仓库（公开仓库）的 9 个文件均可匿名下载，其中包括 **8 个许可未落定的文件**
（7 个 `author_authorization_required` + 1 个 `withdrawn_pending_upstream_confirmation` 薄荷 TPM）。
这与「许可未落定前不得公开下载」的自我约束相冲突。

**建议（三选一，作者拍板）：**
1. **收紧公开范围**：将 GitHub 数据仓库设为**私有**，仅通过 Zenodo 授权后公开拟归档清单；
2. **维持现状 + 显著免责**：仓库 README/LICENSE 显著声明「本仓库文件可下载 ≠ 已获再分发许可，
   8 个文件禁止再分发」，并承担 TAIR/Ensembl/EBI 条款下的合规风险；
3. **替换来源后公开**：将 Araport11/TAIR10 替换为 NCBI RefSeq（INSDC 开放数据）后，
   再整体重跑金标准 / 图件 / 论文数字（改动大，见 `fcs-data-license-decision.md` §3 A2）。

### 发现 B：Gitee 大文件登录墙不影响 GitHub 通道

Gitee 2 个大文件 403 属**平台机制**，不构成许可障碍，也不影响 GitHub 通道的公开性。
对外口径应表述为「GitHub 匿名可下载 9/9，Gitee 匿名可下载 7/9（2 个大文件需登录）」，
**不得**写成「Gitee 有 2 个文件未公开」。

### 发现 C：公开范围与许可口径的错配须在论文中明确

论文 Data Availability 若写「数据可公开获取」，必须区分：
- **物理可下载**（GitHub 9/9、Gitee 7/9）——平台事实；
- **许可允许再分发**（仅 1 个 horsemint CC BY 4.0）——许可事实。

建议 Data Availability 段落明确两句并列，避免审稿人将「仓库公开」误读为「许可齐全」。

---

## 5. 结论

| 问题 | 答案 |
|---|---|
| GitHub 上 9 个文件是否匿名可下载？ | **是（9/9，200）** |
| Gitee 上 9 个文件是否匿名可下载？ | **7/9 是（200），2/9 否（403，大文件需登录）** |
| 403 是否许可问题？ | **否**，Gitee 平台「大文件需登录」机制 |
| 「可下载」是否等于「许可已落定」？ | **否**，8 个文件仍 `needs_confirmation` |
| 需作者决策的事项 | 是否收紧 GitHub 公开范围（§4 发现 A） |

> 本文件只回答「物理公开范围」，许可结论以 `fcs-data-license-decision.md` 为权威；
> 二者口径不同、不得混用。
