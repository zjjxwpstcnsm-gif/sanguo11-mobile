# Agent 1 静态内容接入约定

本文件描述本分支已经存在、可调用和测试的接口，以及不能自动转为运行时状态的边界。不要求 Agent 1 使用尚未提交的接口。完整来源与缺口见 NATIONAL_CONTENT_V08.md。

## 当前可调用接口与实例

```java
ContentCatalog definitions = ContentCatalog.get(); // 本地资源；IOException 表示校验失败
ContentCatalog.Officer zhouYu = definitions.officer(3001);
assert zhouYu.sourceId == 245;                    // 来源表序号；非已核定的原安装编号
assert zhouYu.stat(0) == 97;                       // 统武智政魅：97,71,96,86,93
assert zhouYu.aptitudeText().equals("AASCCS");     // 枪戟弩骑兵器水军：2,2,3,0,0,3
assert zhouYu.birth == 175 && zhouYu.death == 210 && zhouYu.appearance == 189;
assert zhouYu.skillId.equals("skill-062");
World opening = ScenarioCatalog.load("officer-reference-drill", 2);
definitions.validateOpening(opening);             // 实际比对全部 18 人五能力和六适性
```

这里只有五能力/六适性用于开局；189 登场、175 生年、210 卒年和 `skill-062` 都只是社区资料。`cross-checked` 仅覆盖该人物的五能力与六适性，不能覆盖上述年份、关系或特技效果。

| 表 / API | 字段契约 | 当前用途 |
|---|---|---|
| `Officer` | id/sourceId 为整数；stat(0..4)、aptitude(0..5) 返回值；其余字段只读 | 不把目录对象当 World.Officer |
| `rows("sites")` | 0 id、1 name、2 kind、3 rawX、4 rawY、5 durability、6 coordinateStatus、7 source | 原坐标空间；禁止直接 new Hex(rawX,rawY) |
| `rows("skills")` | 0 id、1 name、2 source | stable ID，不等于规则枚举 ordinal |
| `rows("items")` | 0 id、1 name、2 kind、3 value、4 holderRaw、5 locationRaw、6 scenarioId、7 source | scenarioId 全为 `?`，不得写入宝物归属 |
| `rows("scenarios")` | 0 id、1 name、2 year、3 month、4 kind、5 status、6 source | status 全 incomplete，不可进入官方开局选择器 |
| `sourceUrl(id)` / `alias(id)` | 来源 URL / 18 个批准显示别名 | 别名不是按姓名猜测绑定 |

据点实例：`20000 / 襄平 / city / rawX=174 / rawY=16 / durability=2600 / collected / rlu-reference`。
其来源台账 `sourceId=rlu-site-000`，`originalId=null`；运行时 Hex 尚未知。此实例不能用来证明相邻、出口、岸线或开发地位置。

关系实例：周瑜的 `親近武將_2=孫策` 保留源槽位；报告中 `targetId=null`、`status=unresolved-source-name`。即使目录存在同名人物，也要审核来源编号再绑定，不用 String→Officer 的同名查找代替映射。

## 后续运行时工作具体边界

1. **人物规则**：Agent 1 为生卒、登场、身份与关系添加运行时字段并统一存档升级。不能在每次读档时重新从最新 ContentCatalog 覆盖 World 数值。存档中已成长的能力/适性继续以快照为准。其他旧沙盘中相同整数 ID 的含义也不能仅凭相同数字认定；需要剧本定义与显式映射上下文。
2. **特技/宝物映射**：将 `skill-062` 等文本 ID 显式映射到 Agent 1 的规则定义，记录所采来源和批准修订。`skill-000` 的 000 是本轮 ID 台账的一部分，未经原安装核对不能当作官方编号。宝物同名不同 ID 保留；原表持有人缺少剧本上下文，拒绝自动绑定归属。
3. **地图**：当前内核 Hex 为 axial 六方向 `(1,0),(1,-1),(0,-1),(-1,0),(-1,1),(0,1)`；原表 X/Y 的轴向、偏移和原点未知。拿到原版格点后应另交坐标变换元数据和至少三个锚点、全格地形类型与原始值、城/关/港占格、开发地归属和水陆出口。原始值无法被四类地形表达时保留原值并拒绝“原版一致”声明，不统一转 PLAIN。128 上限与存档布局由 Agent 1 统一扩展。
4. **官方剧本**：为每个 stable scenarioId 提供日期、势力和君主 ID、领地、全员身份/所属/所在地或未登场状态、资源/兵装/舰船、外交/研究、宝物与事件状态。必须完整引用独立定义；当前 14 条目录记录不满足这些条件。事件没有已采集定义，不能创造空事件然后标为覆盖完成。

## 必须保留的兼容与验收案例

- `ContentTest` 已证明 18 人实际字段、原工程地图/资源/位置与输入一致，三势力出征推进旬及 v6 往返，正常移动后玩家实际攻击 AI 部队。
- 同测试创建不含任何 scenarios/content 资源的 ClassLoader，读取已推进、修改资源/适性且引用已移除数据包的存档，编码字节完全一致。接入新字段后仍要保留这一行为。
- `ScenarioTest` 继续覆盖无效人员归属、重复驻城记录/ID、未知字段/地形、越界、行宽、阻断与旧存档；不可为全国地图删除原沙盘或放宽这些检查来绕过缺口。
- 导入器的 --check 与 15 项语义/异常测试检查数据修订输出、无效外键、重复字段/ID、未知适性/类型、坐标重叠、双表差异和排序不改 ID。
- 尚没有原版地图的拓扑验收样例。Agent 1 获得地图时应提供“陆路必须经关口”“水军经港出入、陆军不得跨水”“保留真实不可达区域”等从原版格点取样的正反案例；不能用自动铺路、删除山河或空白样例通过验收。
