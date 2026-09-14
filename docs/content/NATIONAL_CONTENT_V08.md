# 全国内容资料边界与数据管线

目标安装：Windows 繁体中文版《三国志11＋威力加强版》1.1。目标程序和数据文件哈希尚未取得，不能认定任何社区表已经等同该安装。排除 PS2/Wii 专有剧本、PK2.2 和 MOD；不使用“修改劇本”文件作为原版输入。

## 覆盖清单

| 类别 | 本轮实际资料 | 对目标原版的核验 | 游戏接入 |
|---|---|---|---|
| 城市 | 42 名称、耐久、原表 X/Y、基础收入 | 待核验；没有六角坐标约定 | 目录与来源坐标预览 |
| 关隘、港口 | 10 / 35 名称和原表字段 | 坐标疑似错配，45 条全部隔离 | 名录，禁止投影进游戏 |
| 全国格点 | 0 原版地形格 | 未取得地形、开发地、道路、水系、岸线、出口、连接 | 没有伪造全国战场 |
| 历史武将 | 来源序号 0–669，共 670 人，62 列原始资料 | 4 人能力/适性双表对照；全部尚未对原安装核验 | 670 人可检索；18 人能力/适性实际开局 |
| 其他人物 | 原表另 180 行保留 | 包含古代人物、空位、NPC及版本可疑行，未核定 | 全部排除运行时 |
| 势力 / 君主 / 开局人员 | 未取得完整剧本初始状态 | 未完成 | 仅保留工程沙盘原有三势力 |
| 历史 / 假想剧本 | S1–S14 名称日期元数据 | 14 条均不完整，确切 PC 1.1 总数也待安装核对 | 官方完整可玩：0 |
| 特技 | 100 名称，人物表特技引用 | 具体原版编号未取得，效果未核验 | 目录；不覆盖运行中特技 |
| 宝物 | 43 行名称/类别/价值与表内持有人/所在地 | 表没有剧本上下文，归属不绑定；同名保持不同 ID | 目录，效果/归属未生效 |
| 事件 | 0 已核验定义；完整事件总数未知 | 条件、结果、剧本引用均缺 | 不伪造事件记录 |

新增可玩 `officer-reference-drill`（武将资料演练）使用原 `regional-sandbox` 的 28×23 地形、9 城、资源、三势力和领地。18 个稳定整数 ID 保持原样，能力与六类适性来自已锁定社区表。它是工程演练，不是官方历史开局。三个原场景原字节与索引校验保留。

## 来源与复核

- 社区人物表：[san11/man.md](https://github.com/reganlu007/reganlu007.github.io/blob/master/san11/man.md)，Git blob `1d81a16658885473db2f0f245528ae73b6bcb7d0`。具体发行平台、补丁未声明。
- 社区资料表：[san11/README.md](https://github.com/reganlu007/reganlu007.github.io/blob/master/san11/README.md)，Git blob `89d4de6bffc8d5ef591c32b8b3a431378848594d`。仅采集数值事实与名称；不复制攻略/特技描述。
- [Steam Windows PK 产品页](https://store.steampowered.com/app/628070/)仅用于产品范围，不能证明繁中 1.1 数值。
- 双表对照：[刘备](https://w.atwiki.jp/sangokushi11/pages/339.html)、[曹操](https://w.atwiki.jp/sangokushi11/pages/338.html)、[诸葛亮](https://w.atwiki.jp/sangokushi11/pages/123.html)、[周瑜](https://w.atwiki.jp/sangokushi11/pages/456.html)。逐项值在 `data/content/cross-checks.json`。社区表本身也链接该 wiki，因此不宣称两份来源互相独立。
- 剧本元数据取 [刘表剧本表 S1–S14](https://w.atwiki.jp/sangokushi11/pages/840.html)，不从少数人物归属推演其他开局记录。
- 关港疑点例：来源表高唐 `(79,155)`、孟津 `(108,123)` 与同表城市分布呈错配；不擅自移动、补平山河或推测相邻关系。

核验状态：`cross-checked` 仅表示指定字段与另一公开表逐项一致；`collected` 为已采集待原安装复核；`unknown` 为未知。当前没有“对目标原版安装已核验”的完整数据包。校验摘要只能证明资源字节一致，不能证明内容还原。

## 可重复运行

```
python3 tools/content/import_snapshots.py --officers /path/man.md --reference /path/README.md
python3 tools/content/build_content.py
python3 tools/content/build_content.py --check
python3 tools/content/test_content.py
bash scripts/test-core.sh
bash scripts/test-ui-models.sh
```

第一步核对本地源快照的精确 Git blob，校验 850×62 人物字段与数值资料的来源行；`--write` 可重建相同人物输入。不在构建或运行时抓网页。规范化 JSON 是版本控制下的审核输入和 ID 台账；所有运行时 TSV、索引、演练开局、坐标 SVG、差异报告由离线工具确定生成。

`verification-report.json` 包含输入 SHA-256、18 人旧值/新值差异、同名物品、969 条未解析关系和缺口。关系中诸如 `20 02` 保留原文、目标 ID 为 null，不按同名自动联结。原表重复“亲近/厌恶”列按 `_2`…`_5` 保留槽位。未知 JSON 值为 null，TSV 使用 `?`；不以 0 冒充未知。

## ID 与接口

- 人物表来源编号与项目 ID 分离；18 个已有 ID 使用显式 `legacy-bridge.json`，其余首次分配为 `10000 + 来源编号` 后固化进输入台账。显示排序不会重分配 ID。来源编号尚不能证明是目标安装内部编号。
- 城关港为 20000–20086；`skill-000`、`item-000`、`pcpk-huangjin` 等稳定文本 ID 固化在输入。原表没有编号的 `originalId=null`，不是按当前列表顺序推断原版编号。
- `ContentCatalog.get()` 返回只读定义；`Officer.stat(i)` 顺序统武智政魅，`aptitude(i)` 顺序枪戟弩骑兵器水军，数值 C/B/A/S→0/1/2/3，与基线一致。
- `ScenarioData` 只新增来源类型 `community-reference` 和该类型必填 `reference=rlu-officers`。加载时逐人核对显式 ID、批准别名、五能力、六适性。失败时拒绝新局，不回退伪造数据。
- 保存只使用现有 v6 的 World 快照；资料目录不是第二套游戏状态。旧存档读取不依赖目录、安装中的剧本、当前数据修订。

Agent 1 需要后续决定：原版地形类型与可通行语义、200 格级地图和 SaveCodec 128 边界、城/关/港独立模型、开发地/出口/水陆连接模型；人物生卒登场、身份、关系 ID 的审核与规则；特技/宝物/事件 stable ID 到运行时定义映射。不能仅扩大尺寸或将来源 X/Y 当作 axial 坐标。现有四类地形不足表达原版信息。

## 手机导航

保留原生命令入口、取消、保存、任务、三将和水陆战 UI。新增城市/任务/势力检索、资料分类检索、来源坐标预览；查询条件和分类进入 Activity Bundle。当前战场导航图支持点按拖动定位、保留缩放，开关状态可恢复。资料预览与实际战场明确分开。

地形按相机可视行列绘制；城市/部队/设施/任务/火/建筑按 8×8 分块索引，只在 `setWorld` 更新时扫描状态。人物名称查表；小地图地形缓存。触控保持 axial 舍入，出征命令不使用扩大后的城市命中格。城市名随缩放分级显示。最大 128×128 压测是工程测试，不是全国地图。
