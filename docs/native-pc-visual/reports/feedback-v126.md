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
- CoreTest.logistics:75「AI uses deployment commands」与DisplacementTest.boundaries:27「city deterministic replay」分别在输入58d7与候选独立复现同点FAIL/exit1，分类 inherited，未改旧规则或断言。
- 初轮测试曾误选已有树叶panel6，被边界测试发现；已改独立panel3且核对其余七panel像素完全相同。初轮瀑布中心线模型不足以证明宽面安全，修正为整面验证。source(66,58)原长跌水没有安全完整脚印，缩短为1.15世界单位的短跌水；原两处2.1跨度保留。

## 构建与产物

生产源码 `25e6c121cba1ae36947c520dfccc48e13e0e2171`；交付为CI中实际安装的APK，不用本地签名容器代替。SHA256 `fe048dee1734a23d156fd56bd3ea2cbf65280daf29c5660e3ced0dc5311bc383`，41,987,766字节。应用 `game.sanguo.mobile.dev`，versionCode126 / `0.126.0-native-landmarks`；Filament1.56.0 OPENGL，四ABI arm64-v8a/armeabi-v7a/x86/x86_64，16KB与467项资产一致核验PASS，无libunity。证书SHA256 `8f64ee37f8ff58de8f5a199aac2ae745a5bc927d0d0eabac7540083a5e551f24`。

本地和CI均执行 `./gradlew --no-daemon :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug -PcandidateSource=25e6c121cba1ae36947c520dfccc48e13e0e2171 -PnativePcVisual=true -PunityBridgeProbe=false -PmapEditorProbe=true`；构建PASS，lint0错误/83警告。CI [36659459089](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/36659459089) build PASS、Blender/PNG/ETC2与固定1.56材质逐字节复现PASS。主机329GLB/架构/行军等专项通过，完整core仍保留以上两项失败，不能把整体规则套件写PASS。

本地APK与CI APK解包后的每个文件完全一致，压缩容器/签名块不同；独立APK交付只用CI原文件。`delivery-apk-identity.json` 与 `delivery-apk-audit.json` 是实际交付包复核结果。

## 正常运行验收范围

`adb install -r` 主APK与test APK；`pm path` 回读并pull、cmp及SHA核验每次实际安装包；`am instrument -w -e mode ... game.sanguo.mobile.dev.test/game.sanguo.mobile.NativeLandmarks126Instrumentation`。探针经 `MainActivity` / `MapHost` 打开默认全国剧本的原生3D，保留原120秒ready和900秒外部总超时，不降断言。每地区3种缩放(span3/6/10)、2方向(yaw0/180)、网格开/关，12对实际Surface/UI截图。相机由探针调用正式控制入口，未完成纯手指拖动/缩放全链。

通过场景还核对实际生产GPU景物、missingAssets空、2D到3D并变化视图后的完整SaveCodec/RNG。泰山/西南GPU断言数的是已有瀑布顶点，并非证明每种新崖壁均显示；新岩壁资产解码由主机真实GLB测试核对，未逐种GPU可见验收；完整面安全检查主要针对新瀑布和墙体。

## 美术和设备限制

已人工复看原始APK PNG，不以像素统计或离线Blender预览冒充美术通过。泰山/西南瀑布仍像蓝色条纹带，存在明显横向接缝；长城墙体转折尖锐、共享图集显砖石感，三类新岩壁仍有层状多边形/砌块感。壶口失败图是部分装载画面，不能证明新瀑布完整呈现。新模型是原创局部迭代，不是最终高精全国美术，视觉线FAIL；PC同镜头与精确路线/锚点 REFERENCE_MISSING。

无连接物理设备，本地无KVM；远端x86_64/SwiftShader模拟器证据不能代表ARM64。真机30分钟、稳定FPS/GPU时、完整纯触控开局/SAF/生命周期未运行，保持NOT_RUN/历史缺口OPEN。R00–R18已执行不代表所有旧验收自动通过。没有启动下一阶段。

## 最终安装结果（首轮失败完整保留）

| API / 区域 | 首轮 | 仅失败项复验一次 | 最终判定 |
|---|---|---|---|
|29 壶口|FAIL ready|FAIL ready|FAIL|
|35 壶口|FAIL ready|FAIL ready|FAIL|
|29 长城|FAIL ready|FAIL ready|FAIL|
|35 长城|FAIL ready|FAIL screenshot unavailable|FAIL；10 Surface/9 UI 已保留|
|29 泰山|FAIL ready|FAIL ready|FAIL|
|35 泰山|PASS 169检查|NOT_APPLICABLE 首轮成功未重跑|PASS 局部运行范围|
|29 西南|FAIL ready|PASS 173检查|PASS 本次复验；稳定性仍OPEN|
|35 西南|PASS 142检查|NOT_APPLICABLE 首轮成功未重跑|PASS 局部运行范围|

14次实际安装均回读同一交付APK并逐字节一致；3个成功运行各12对Surface/UI、完整183,198字节SaveCodec独立比较相同（SHA256 `1092dd42e8ae7c6875853b5a097294694a7f5aba0986a8a58a255893360579e5`）。其余失败未达到末尾存档/GPU检查，不能补记PASS。API35长城复验已经完成多个视角，span10/yaw0/gridtrue的Surface存在，但UI截图接口失败；该失败不能误写为第二轮ready超时。首轮和复验失败共11项，原始证据不覆盖。总体CI仍FAIL。

壶口API35首轮beginFrame520尝试/10获准、pending15，复验619/12、pending12，源自实际report，不是GPU耗时/FPS证据。加载/截图失败归因UNKNOWN，没有输入同锚点安装对照，不能根据历史相似日志自动称 inherited。ARM64最终发布门槛、美术同镜头、加载稳定性与历史触控SAF生命周期仍是后续工作阻塞；本轮不自动执行后续阶段。
