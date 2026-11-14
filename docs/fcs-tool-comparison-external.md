# PhytoScope FCS · 外部实机同任务对照协议（空白模板，B 类）

> 批次：B03 · 执行人：工程师寇豆码（Kou）· 日期：2026-11-14（ISO-8601 UTC 口径同设计 §7.10）
> 关联：`docs/fcs-tool-comparison-final.md`（定性对照定稿）、`docs/fcs-phase3-comparison-plan.md` §4、
> `docs/validation-and-benchmarks.md`（五任务协议）、增量 PRD R6 / §6.1 A8。

> ## 状态：**未执行**（B 类，只交协议，需作者执行）
>
> 本机仅 Windows x64、无显示环境脚本化快照、JBrowse2 未安装、IGV/TBtools 为 GUI 未脚本化。
> 本文件是**空白协议模板**，不含任何实测数据、计时、截图或结论。
> **GitHub Actions 三平台构建 artifact ≠ 外部实机对照**，二者不得互相替代。

---

## 1. 前置条件（由作者 / 外部机器满足）

| 项 | 要求 | 状态 |
|---|---|---|
| 带显示环境的机器 | 可运行 JBrowse2 / IGV / TBtools GUI | **未满足 / 未执行** |
| JBrowse2 | 安装并配置完成（text-index / 静态托管） | **未安装 / 未执行** |
| IGV | 安装并加载同一参考 FASTA + 注释 | **未执行** |
| TBtools-II | 安装并能运行 Gene Structure View / 热图等 | **未执行** |
| 同一份数据 | 与本仓库冻结金标同源（见 §2） | **未执行** |
| 记录人 / 日期 | 作者填写 | **未执行** |

## 2. 共同任务与固定输入（协议，不新增算法）

- 数据源：与本仓库冻结金标同源（例如 `src/test/resources/non-model/vigna-umbellata/`、
  `examples/arabidopsis-atlas/`、`phytoscope-reference-data/` 9 文件中的 TAIR10/Araport11）。
- 位点集：固定、跨工具一致（例如 `LOC124833352`、`AT1G67090`、`AT5G38410/38420/38430`）。
- 输出要求：跨工具一致的「基因结构 + 链向 + 坐标」可比对结果。
- 五个共同任务：按基因 ID 定位 / 查看异构体 / 关联表达 / 生成对比图 / 恢复分析（见
  `docs/fcs-tool-comparison-final.md` §2）。

## 3. 记录表（空白，作者执行时填写）

| 工具 | 任务 | 输入文件 / SHA-256 | 操作步骤数 | 结果是否与冻结金标一致 | 观察记录 | 记录人 | 日期 |
|---|---|---|---|---|---|---|---|
| JBrowse2 |  |  |  |  |  |  |  |
| IGV |  |  |  |  |  |  |  |
| TBtools-II |  |  |  |  |  |  |  |

## 4. 计时 / 性能记录（空白；仅在确有新实测时填写，并标 `current_measured`）

| 工具 | 冷启动 setup (ms) | 任务 (ms) | 轮次 | 硬件声明 | 数据类（`current_measured` / `historical`） |
|---|---|---|---|---|---|
| JBrowse2 |  |  |  |  |  |
| IGV |  |  |  |  |  |
| TBtools-II |  |  |  |  |  |

> 若填写，须声明硬件、数据、任务、≥3 次交替冷启动、setup/task 分离；RSS 在 Windows 无
> `resource` 模块时固定 `not measured`，**不得编造**。本模板当前**全部为空**。

## 5. 定性结论模板（空白）

```
（作者执行后填写：共同任务上的一致/差异观察；功能差异单列；不作更快/更好/更易用主张）
```

## 6. 如实口径

- 本文件**未执行**、**不含任何实测数据**。
- 不得把本空白协议写成「已完成三工具实机对照」；不得把定性对照冒充实机计时。
- 完成后交回团队时，须逐项标注记录人、日期与真实数据，并与
  `docs/fcs-tool-comparison-final.md` 的定性定稿区分开。
