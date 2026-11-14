# PhytoScope FCS · 性能基准（R10，当前版本真实实测）

> 批次：**B05（收口）** · 执行人：工程师寇豆码（Kou）· 实测日期：**2026-11-14**（ISO-8601 UTC）
> 权威规格：`docs/design-fcs-final-delivery.md` §5 B05 #4、§3 性能口径、§7 共享知识；增量 PRD R10。
> **measurement_class 纪律**：本文件所有「本次实测」数字标 `current_measured`；凡引用
> `docs/performance.json`（2026-10-30）或 `docs/paper-figures/data/*.json`（`247ff11`）者一律标
> `historical (v0.2.0 / 8fd01ca)` 或 `historical_measured_not_new`。**禁止用 v0.2.0 历史值
> 冒充当前实测**；两套数据的输入规模完全不同（见 §3），不可直接比较为「更快/更慢」。

---

## 1. 硬件 / 软件环境（本机如实探测，非推测）

| 项 | 实测值 | 来源 |
| --- | --- | --- |
| OS | Microsoft Windows 11 家庭版中文版（10.0.26300，64 位） | `Get-CimInstance Win32_OperatingSystem` |
| CPU | AMD Ryzen 7 5800H with Radeon Graphics，8 核 / 16 逻辑处理器 | `Get-CimInstance Win32_Processor` |
| 内存 | 31.4 GB（TotalVisibleMemorySize） | 同上 |
| JDK | `21.0.6+8-LTS-188`（JRE build 21.0.6+8-LTS-188，mixed mode, sharing） | `java -version` |
| Python | 3.13.14 | `python --version` |
| JVM 参数 | `-Xmx3g`（对应 `max_heap_bytes = 3221225472`） | 命令与 `performance.json` 一致 |
| 构建 | `target/phytoscope-0.3.0.jar`（`sh mvnw -B -DskipTests package`，`BUILD SUCCESS`） | `target/b05-package.log` |

> 与历史 `docs/performance.json` 的 `java = 21.0.6+8-LTS-188`、`os = Windows 11 amd64`、
> `available_processors = 16`、`max_heap_bytes = 3221225472` 一致（同一台机器、同一 JDK）。

## 2. 固定数据 / 固定任务（当前 0.3.0 树）

- **数据**：内置 **KB 级 rice 示例** `data/species/rice/rice_chromosomes.gff3`
  （源文件 SHA-256 `0b81520bb1d9ca973dea8a68a1f1e17f0ff32b110dfb5a1d54617ff3ae66f20e`，
  三次实测 `sources[].sha256` 一致）。
  **当前 0.3.0 树只内置 6 个 KB 级示例**；完整参考数据（TAIR10 / Araport11 / horsemint /
  E-MTAB-7978 等 9 文件）已外迁到 `phytoscope-reference-data`，**不在本次实测输入中**。
- **任务**（`BenchmarkRunner` 固定流程，不改算法）：冷进程导入 → 遍历序列 → 100 次基因搜索
  → 100 次层级查询 → 10 基因座比较图布局 + SVG 写出 → 采集 G1 堆池峰值。
- **规模**（三次一致）：`genes=24`、`features=347`、`sequences=12`、`search_repetitions=100`。
- **查询基因**（自动取前 4 个基因）：`gene:g1` / `gene:g2` / `gene:g3` / `gene:g4`。

> **重要口径**：历史 `docs/performance.json` 用的是 94,883 基因 / 909,424 features 的
> 完整 horsemint 注释（v0.2.0 / `8fd01ca` 时代，数据已外迁）。本次 `current_measured`
> 用 24 基因 KB 级示例，**两者输入规模相差约 4000 倍，绝不可互相比作性能优劣**；
> 历史值只作 `historical` 留档，不重测、不冒充。

## 3. 轮次顺序与「交替冷启动」

- **逐次独立进程**：每轮都是一条全新的 `java -Xmx3g -jar ...` 命令（新 JVM、重新导入、
  `resetPeakUsage()`），轮次顺序固定为 `run-1 → run-2 → run-3`，各进程退出码均为 `0`、
  stderr 均为空。**没有 warm 复用、没有进程间共享状态**。
- 「交替」在本机 R10 语境指**与历史/其他基线不交替、不与 gffutils 交替**——本批只做
  PhytoScope 当前版本的独立冷启动重复测量（3 次），用于给出分布而非单次偶然值。
  gffutils / Biopython 的 3 轮「工具交替执行顺序」是 `247ff11` 时代的
  `historical_measured_not_new` 基线（冻结于 `docs/paper-figures/data/*.json`），**本批不重跑**。

## 4. 当前版本真实实测结果（`measurement_class: current_measured`）

三次独立冷启动逐次原始值（单位 ms；`heap_pool_peaks_bytes` 单位 byte）：

| 指标 | run-1 | run-2 | run-3 | measurement_class |
| --- | --- | --- | --- | --- |
| `import_ms` | 50.4111 | 46.4282 | 47.0384 | `current_measured` |
| `select_all_sequences_ms` | 0.2406 | 0.1567 | 0.1637 | `current_measured` |
| `search_median_ms` (p50) | 0.0099 | 0.0094 | 0.0090 | `current_measured` |
| `search_p95_ms` | 0.0194 | 0.0196 | 0.0182 | `current_measured` |
| `hierarchy_query_median_ms` | 0.0057 | 0.0055 | 0.0057 | `current_measured` |
| `hierarchy_query_p95_ms` | 0.0135 | 0.0135 | 0.0198 | `current_measured` |
| `comparison_10_loci_layout_ms` | 81.2309 | 78.2296 | 78.7359 | `current_measured` |
| `comparison_svg_write_ms` | 20.7592 | 23.1870 | 21.1614 | `current_measured` |
| `G1 Old Gen` 峰值 | 0 | 0 | 0 | `current_measured` |
| `G1 Survivor Space` 峰值 | 0 | 0 | 0 | `current_measured` |
| `G1 Eden Space` 峰值 | 16777216 | 16777216 | 18874368 | `current_measured` |

- **导入时间**（`import_ms`）三次中位数 = **47.0384 ms**，范围 46.4282–50.4111 ms。
- **搜索 p50/p95**（三次各自 100 次查询的 median/p95）：p50 0.0090–0.0099 ms，
  p95 0.0182–0.0196 ms。
- **比较布局**（10 基因座）：78.2296–81.2309 ms；**SVG 写出**：20.7592–23.1870 ms。
- **JVM 堆池峰值**：仅 `G1 Eden Space` 有非零峰值（16–18 MiB），`G1 Old Gen` 与
  `G1 Survivor Space` 峰值均为 **0** —— 因为 KB 级示例在年轻代即可容纳，未触发晋升。
  这是**堆池峰值，不是进程 RSS**。
- **RSS（常驻集大小）**：`not measured (Windows lacks Python resource module; JVM heap pool peak only)`。
  与设计 §7 / §5 B05 #4 固定口径一致；**不编造 RSS 数字**。
- 三次 `timestamp_utc`：`2026-11-14T14:00:47.586974Z` / `14:00:48.417545100Z` /
  `14:00:49.234879400Z`（逐次独立进程，间隔约 0.8 s）。

## 5. 历史基线（`historical`，只留档、不冒充、不重测）

| 来源 | 值（摘录） | measurement_class |
| --- | --- | --- |
| `docs/performance.json`（2026-10-30，v0.2.0 / `8fd01ca`，94,883 基因完整 horsemint） | `import_ms=2162.2928`、`search_median_ms=24.7289`、`search_p95_ms=32.2652`、`comparison_10_loci_layout_ms=89.648`、`comparison_svg_write_ms=22.1948`、Old Gen 峰值 899018608、Eden 390070272 | `historical (v0.2.0 / 8fd01ca)` |
| `docs/paper-figures/data/gffutils-summary.json`（`247ff11`，726,726 行水稻豆 GFF3） | `median_phytoscope_full_import_ms=2670.7585`、`median_gffutils_full_index_build_ms=57801.9512` | `historical_measured_not_new` / `not_new_measurement=true` |
| `docs/paper-figures/data/biopython-summary.json`（同上） | 3 物种 SeqIO 逐字节一致 | `historical_measured_not_new` |

> 上表历史值**一律不得改写为当前实测**；`docs/performance.json` 本批**只追加**
> `_fcs_v0_3_0_pointer` 指针与说明，原有历史字段**一字不动**（见 §7）。

## 6. 冒烟测试（R4，A 类）

| 命令 | 结果 | measurement_class |
| --- | --- | --- |
| `java -Xmx3g -jar target/phytoscope-0.3.0.jar --benchmark --example rice` | `exit 0`，stderr 空，输出 23 字段合法 JSON（`import_ms=48.3893`、`genes=24`） | `current_measured`（smoke，不计入 §4 三轮分布） |
| `python tools/package_desktop.py --jar ... --out target/desktop-v0.3.0` 内置 packaged smoke | `PASS: packaged runtime launched and parsed the rice example` | `current_measured`（构建期 smoke） |

## 7. 完整复现命令（当前版本）

从主仓根执行（`JAVA_HOME` 需指向含 `jpackage.exe` 的完整 JDK 21）：

```sh
# 构建
sh mvnw -B -DskipTests package
# 3 次逐次独立 JVM 冷启动（--out 是目录，各写 performance.json / parsed-structure.tsv /
# benchmark-comparison.svg；三条命令是独立进程，顺序固定，不可合并为一个进程）
java -Xmx3g -jar target/phytoscope-0.3.0.jar --benchmark --example rice --out benchmarks/results/2026-11-14-v0.3.0/rice/run-1
java -Xmx3g -jar target/phytoscope-0.3.0.jar --benchmark --example rice --out benchmarks/results/2026-11-14-v0.3.0/rice/run-2
java -Xmx3g -jar target/phytoscope-0.3.0.jar --benchmark --example rice --out benchmarks/results/2026-11-14-v0.3.0/rice/run-3
# 汇总校验
python -c "import json,glob; fs=glob.glob('benchmarks/results/2026-11-14-v0.3.0/**/*.json',recursive=True); print('result files=',len(fs)); assert len(fs)>=3; [json.load(open(f,encoding='utf-8')) for f in fs]; print('CURRENT_MEASURED_OK')"
```

产物目录：`benchmarks/results/2026-11-14-v0.3.0/rice/run-{1,2,3}/performance.json`
（配套 `parsed-structure.tsv` 与 `benchmark-comparison.svg`；每轮 stdout 留痕于
`run-N.stdout.json`，stderr 为空日志）。另见 `benchmarks/results/2026-11-14-v0.3.0/summary.json`
（三轮汇总 + 每个数字的 `measurement_class`）。

## 8. 对外口径（禁止虚报）

- 本批 R10 是**当前 0.3.0 树、KB 级 rice 示例**的真实实测；**不是**完整参考数据基准，
  **不是** v0.2.0 历史值，也**不是**与 gffutils/IGV/TBtools 的性能优劣对照。
- **RSS 未测**：`not measured (Windows lacks Python resource module; JVM heap pool peak only)`。
- 三工具对照固定「定性对照（基于公开文档，非本机实测）」；冻结 Figure S1 JSON 固定
  `historical_measured_not_new`。**不作「更快 / 更好 / 更易用」主张**。
- macOS / Linux 原生性能与真机验证属 **B 类待作者执行**，见
  `docs/fcs-desktop-native-verification-checklist.md`；本机只覆盖 Windows x64。
