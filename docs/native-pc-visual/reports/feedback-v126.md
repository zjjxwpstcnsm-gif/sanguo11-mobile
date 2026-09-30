# v126 地标与地形反馈续作 — PARTIAL

承接实时远端 v125 58d7eb1cba2a45365d7fbee1beef61cc53ce8fc0；main ac29b458325b52d6e302ca44270d16552de4ed7f，架构PR66已合入。继续唯一 agent/native-pc-visual / PR67，不合main，不启动下一阶段。

## 正式修改

Blender4.2.3实际制作12个GLB：夯土墙段、汉风烽燧、砂岩/花岗岩/西南岩柱、壶口收束瀑布，均有近远LOD。原资源逐字节保留。512×64新景物atlas只改空闲panel3用于黄河泥沙色，保留树叶panel6等七面板，完整ETC2 mip和精确Filament1.56材质。新资源通过正常FieldAssets/Vegetation流式chunk合批进FilamentMapView，不增渲染引擎或全国常驻地标对象。

壶口候选 source(66,58) 使用既有不可航行水格/邻接山地的短视觉跌水，收束唇口、分束水面及泥沙泡沫；北方 source x86..99,y15 既有山地上放置贴地墙段和间隔烽燧。河床类型、地形高度、通行、六邻接、城市/港口、寻路、规则RNG、SaveCodec均不改。泰山和西南只在原山格岩石散布点使用区域崖壁模型，未全国铺设模板。

新增整幅瀑布/城墙三角面采样验证，任一脚印越出不可进入区域就拒绝整件，不只检查中心线。新缓存键含sourceMapWidth/sourceOrigin，以避免裁剪地图错用地标。定制mapId不继承全国地标。

## 资料依据与限制

- 中国网《走进山西黄河壶口瀑布》 https://jilu.china.com.cn/2024-07/26/content_42875772.htm ：宽河床收束入龙槽，基岩与侵蚀槽形貌，作为原创轮廓参考；检索摘录可用，原网页fetch受限。
- 中国旅游报《源远流长的甘肃长城文化》 https://changcheng.ctnews.com.cn/m/2024-02/18/content_156649.html ：汉代边塞墙/烽燧使用夯土及本地植物等形制参考；不是游戏路线证明，原网页fetch受限。
- 项目恢复全国图的实际地形和城市 source 坐标作为区域关系约束。壶口位于长安/晋阳间的地图走廊是项目推断，精确PC锚点/长城路线均 REFERENCE_MISSING；没有称原版精确还原，未提取官方游戏资源。

## 已执行主机验证

- LANDMARK126 556,693 checks PASS；真实GLB加载、两LOD近远景、不可通行脚印、cache身份和完整SaveCodec/RNG。
- 保留v125景物29,266检查PASS，两处原瀑布近990/远258顶点，不隐藏旧检查。
- S04/S05 7,462,388检查及真实行军/进驻/合法港口完整SaveCodec PASS；架构与投影119,600/GridLayout32 PASS。
- Khronos329GLB：0 errors / 0 warnings。
- CoreTest.logistics:75候选FAIL，独立输入复现待汇总，不改旧规则或断言。
- 初轮测试曾误选已有树叶panel6，被边界测试发现；已改独立panel3且核对其余七panel像素完全相同。初轮瀑布中心线模型不足以证明宽面安全，修正为整面验证。source(66,58)原长跌水没有安全完整脚印，缩短为1.15世界单位的短跌水；原两处2.1跨度保留。

## 构建与运行

生产源码SHA、APK身份及本轮CI/安装结果提交后填入 evidence/feedback-v126/manifest.json。原120秒SceneInstrumentation.ready不放宽，正常MainActivity/MapHost，3缩放×2方向×grid开关；失败保留原始截图/logcat，不用离线图冒充APK。

ARM64物理设备/30分钟性能、PC同镜头对照、历史纯触控/SAF/生命周期仍未通过。v125全国泰山/西南ready失败与回归归因UNKNOWN继续OPEN，不能将主机或构建通过当作美术完成。
