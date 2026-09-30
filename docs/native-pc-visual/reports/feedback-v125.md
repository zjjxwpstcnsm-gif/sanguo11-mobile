# v125 用户反馈续作 — PARTIAL

## 基线与边界

远端 main `ac29b458325b52d6e302ca44270d16552de4ed7f` 已合入架构 PR66；从串行分支输入 `42230940e267b985b6bcd2d0cefec0fae49cca88` 续作，沿用 PR67。不合并 main、不重启 Unity、不启动下一阶段。
保留 Filament 1.56.0 OPENGL、GameSession 唯一权威及原地图/剧本/高度场/城港关；441个旧资产逐字节不变。规则修改仅限用户明确授权的骑兵战法释放和强制单挑、适性菜单显示。

## 正式路径

- 实际 Blender 4.2.3 LTS 制作8个GLB（层状岩台/碎石坡/窄瀑/宽瀑各两级LOD，共1676三角形），新独立景物atlas及精确1.56.0编译材质。FieldAssets → Vegetation原有流式合批 → FilamentMapView景物材质/原帧时钟；不为瀑布另开绘制循环。旧树木/设施atlas不变。模型为本项目原创CC0，非提取PC资源，不能称最终高精美术。
- 更替原山地岩石装饰为岩台/碎石坡，原分布种子、数量和基础地形不变。泰山候选源坐标(147,59)及西南(31,183)仅在既有不可航行水格且具有可贴山安全走廊时放瀑布。主机实际全国近景990/远景258瀑布顶点。庐山候选(119,146)安全走廊不成立，NO_GEOMETRY；不伪造山或河。
- BREAKTHROUGH后方障碍不再否决施放；碰撞检查仍阻止实际位移，伤害/命中/气力/行动结算保持既有公式；CHARGE、ADVANCE原可施放路径保留。新强制单挑判定在伤害结算后独立于位移，接入原Contests/Duel和SaveCodec；玩家交互、AI沿原有限单挑结算，无额外10气力。
- WarUi/ArmyUi适性不足的战法名称显示灰色并禁用。执行层仍核验适性，不可绕过。沿用原ChoiceDialog/InlineChoices，不改目标或射程规则。

## 强制单挑研究及准确性边界

原输入没有骑兵强制单挑入口。此次添加社区记载的基础概率及部队兵力差、性格/健康筛选，三种战法共享概率；TIMID不触发。现有World没有PC战场外持续体力，采用原Duel同一伤势HP；具体宝物增量未建立精确PC对照，不猜测。开场武将按非胆小且有效武力最高选择；该选择和完整PC公式忠实度仍PARTIAL，不将本项目实现声称为原版逐项一致。伤害后目标已灭、异常、位移后不相邻等不满足原存档单挑约束时概率为零。

研究来源（已读取，非PC截图）：
- https://blog.sina.com.cn/s/blog_c0971fbb0102va19.html ：玩家地图景物记录提及下邳西北山瀑、西南瀑布及庐山；泰山/黄果树的具体归属为作者推测。项目坐标是当前地图候选锚点，不能当成PC精确坐标。
- https://www.xycq.org.cn/forum/viewthread.php?authoruid=374759&extra=&page=2&tid=209820 ：San11 Sire社区概率研究。
- https://www.newton.com.tw/wiki/san11%20sire/10760602 ：基础概率和筛选记载的二次整理。
- https://www.9game.cn/news/9061888.html ：骑兵障碍与强制单挑说明。
美术线 REFERENCE_MISSING：未获得同区域/近镜头的PC原版实拍参考。

## 当前验证

PASS：Cavalry125 51,711检查/192实际强制单挑会话；8类障碍×3骑兵战法，阻挡不位移、真实命令伤害和命中、适性拒绝无副作用、完整存档/RNG回放、触发后存读档及一次结算、AI不悬挂。全国景物5,947检查；原S04/S05资产7,462,388；架构/权威、地图投影119,600、GridLayout32；地形海岸R02/R05/S11；战斗表现S06 25,470。Khronos317 GLB零error/warning。
FAIL inherited：输入及候选独立复现CoreTest.logistics:75（AI uses deployment commands）、DisplacementTest city deterministic replay。原断言未删除/放宽；BREAKTHROUGH旧拒绝断言按用户授权有意更改，独立新测试覆盖，不用原失败停止点冒充新断言已运行。
APK、安装运行、实际截图、设备性能结论待对应源码提交后构建/CI结果补入。当前不是运行PASS。

## 验收状态

实现主机 PASS；安装运行 NOT_RUN（待本轮CI）；视觉 REFERENCE_MISSING/NOT_RUN；ARM64真机、30分钟温升/FPS、纯触控全链/SAF/生命周期 NOT_RUN。历史v124 ready、API35、拖动和全流程失败继续OPEN，不能由host成功关闭。整体PARTIAL。
