# ubuntu / surefire-reports（目录占位 · 明细未回收）

**状态：`not_collected`（仅指本目录的 XML 明细未回收）** · 结果**待主理人经 REST API 下载 artifact**。

> **注意区分层级**：ubuntu 这个 job 的**结论已经取得**——run `43605656726` / commit `49a5fac` 上
> `validation (ubuntu-latest)` = **failure**，失败步骤 `Java and JavaFX regressions`
> （14 个 JavaFX UI 测试无头环境 `Platform.startup` latch 超时）。
> 已确证的金标准计数：`ResearchModuleGoldSetTest 1/0/0/0`、`DomainCaseValidationTest 1/0/0/0`、
> `Gff2ParserSupportTest 5/0/0/0`、`ComparisonWorkflowUiTest 1/0/0/0`，`Skipped: 0`。
> **未回收的是逐类 surefire XML 明细与全局总计数**，不是 job 结论。见 `../job-summary.md`。

本目录用于存放主理人经 GitHub Actions REST API 回收的 ubuntu 平台 surefire XML 报告
（回收后 `Skipped` 须体现 `ResearchModuleGoldSetTest` 非 skipped，且与已知 `Skipped: 0` 一致）。

**本机不得生成或伪造本目录内的任何 surefire 报告内容**；明细真实回收前只有本占位文件。
**本地 `216 / 0 / 0 / 7` 不得复制进来充作远端报告。**
回收执行主体：主理人。目录契约见 `../README.md`（汇总）与 `../job-summary.md`（平台结论）。
