# v0.43 设施产能与编队表现

基线：远端 main `4f192e205295a0d34976605b8ab41929f63b9501`（v0.42.0）。版本统一为 `0.43.0` / `43`。

## 设施规则

| 设施 | 每座已建成设施每旬提供 |
| --- | --- |
| 兵舍 | 征兵1次 |
| 锻冶所 | 枪、戟、弩共用1次生产 |
| 厩舍 | 战马生产1次，产量不再受到锻冶所加成 |
| 工房 | 攻城器械开工1次，保留原跨旬制造工期 |
| 造船厂 | 高级舰船开工1次，保留原跨旬制造工期 |

没有对应设施不能执行；新建设施施工期间不提供次数，已有设施升级期间保留原设施能力。设施等级影响产量，不增加操作次数。市场、农场、造币和谷仓仍按原月/季收入时点结算，不属于主动生产命令。剑兵无需生产兵装。

额度记在实际设施的 `lastUseTurn` 上，下一旬自然恢复；移除设施立即减少可用次数，城池易主不会额外刷新已经使用的设施。失败命令不消耗次数，取消已开工制造不返还本旬次数。存档格式 v25 增加设施使用旬字段，存读不会刷新次数。旧格式只沿用已有读取路径，开发验收以新开局为准。

玩家、电脑和委任军团的正式命令共用约束。AI 不再把无设施/次数耗尽的征兵、生产当作可用指令，并会建设缺少的兵舍或基础兵装设施。界面在选执行武将前显示设施与剩余次数。

## 编队与地图

- 出征总览：已选主将/副将头像放在对应行前方；兵装和舰船显示真实选中图标，修改后同步刷新，保留数量草稿和最终确认流程。
- 新单兵图集每格为一个士兵、骑手、器械或舰船。地图服饰与旗帜按势力色映射，皮肤、金属、木材和马匹保留原色。重用一份低分辨率解码图集和着色蒙版，不为每个势力复制整份位图。
- 兵力1–2500显示1个模型，2501–5000显示2个，5001–7500显示3个，7501–10000显示4个，10001以上显示5个；伤亡后随实际兵力重绘。器械/舰船模型数是兵力表现，不代表库存件数。全国远景继续采用清晰的势力标记，近景显示阵列。
- 栈道保留与山径、出口相连的六方向规则，缩窄原高亮平面带，增加木板接缝、纹理、立体阴影、栏杆和斜支撑；山体底纹继续使用现有地形图集。连接位图仍按方向掩码缓存。

## 本次打包与推送

按用户要求继续本地未提交的 v0.43 改动，不运行测试，直接构建开发 APK 并推送 main。提交使用 `[skip ci]`，不触发额外 CI 测试。版本 `0.43.0` / `43`，包名 `game.sanguo.mobile.dev`，沿用仓库固定开发签名；应用版本页记录本次源码提交。

## 接续前已有验证记录

以下为上一轮开发留下的结果，本次打包不重新执行，也不将其表述为本轮新测试。

- 新设施行为专项73条断言通过：缺设施/施工中拒绝、多座次数、同厂多兵种共享、不同城独立、失败原子性、制造取消、读档/下一旬、设施毁坏和AI先建设。
- UI投影/相机51,837条、道路连接451条断言通过，包含全部兵力分档边界。
- 全部正式 Android Java 源码使用 Android API35 的 `android.jar` 编译通过；此检查不等同于 APK 构建或设备运行。
- 完整核心回归已通过（本地 `sanguo-v043-core.log`），包含长局经营、运输、战斗、144条 v42 新开局检查和73条设施专项；v42与设施专项已纳入 Gradle `check` 入口。
- 本次仅构建 APK 并核对版本与签名；未运行 Android 模拟器或 ARM 真机验收。

## 图集来源

新增 `app/src/main/assets/map/units-v043.png`，使用内置 imagegen，以仓库现有 `units-v040.png` 为风格参考生成，原图保留。透明间隔已按列检查并在 `VisualAssets.UNIT_ROWS` 中记录，避免裁断枪尖和马蹄；运行时二分之一采样。新资源 SHA-256 纳入构建资源清单。

生成提示词：

> Use case: style-transfer. Asset type: production sprite atlas for an existing historical Three Kingdoms strategy Android game. Reference image is STYLE reference only. Create ONE genuinely transparent RGBA PNG atlas, square 1024x1024, strict 4 columns x 4 rows of equal 256x256 cells. Match the supplied realistic miniature isometric 3D game sprites, dark steel armor, rich cloth, brown wood, overhead left light, same three-quarter front camera. Every sprite must be fully separate within its cell with generous transparent margins, no contact between cells, no floor tiles, no labels, no lettering, no borders, no checkerboard. Critical: each infantry cell contains EXACTLY ONE single soldier, never a group; cavalry EXACTLY ONE rider on ONE horse. Row1 left to right: ONE sword-and-round-shield infantryman; ONE long-spear infantryman; ONE halberd infantryman; ONE crossbow infantryman. Row2 left to right: ONE mounted cavalryman; ONE covered wheeled battering ram; ONE siege tower; ONE flame wooden beast siege engine. Row3 left to right: ONE catapult; ONE simple boat; ONE tower ship; ONE large warship. Row4 left to right: ONE walled Chinese city; ONE fortified mountain gate; ONE riverside port; ONE cultivated farm. Preserve realistic proportions, fine painted materials, consistent isometric style with supplied reference. All humans adult historical soldiers, full helmets and boots and all weapons fully visible. Soldiers wear saturated MEDIUM ROYAL BLUE cloth tabards and skirt panels over armor, and blue spear pennants; mounted rider wears blue cloth, siege engines/ships blue banners, for programmatic team recoloring. Leave faces, steel, wood, horse coats natural colors. Infantry should fill approximately 80% cell height including weapons. Alpha must be truly transparent.
