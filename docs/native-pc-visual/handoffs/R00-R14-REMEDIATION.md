# R00–R14整改续作交接 / PARTIAL

先读当前reports/R00-R14-REMEDIATION.md、DEFECTS.md及evidence/R00-R14-remediation-checklist.json；原EXECUTE_PROMPT和原提示词在交付ZIP的input中。继续同一串行分支/PR67，先读取实时远端，不回退到这里的检查点，不合main、不force push、不开始R15。

已交付正式修改：TerrainSurface有界全国半格缓存、FilamentMapView静态势力层缓存；精确R13资产保护。保留SceneRenderGate、原生Filament、架构优化与全部规则/地图/资源内容。APK生产源码96719e971d74aa2b3f18d66a7f23832148a6dc8b；后续abca2dd2f97924528ce9d1cbc3a63d3571bf6ef5只增加原APK无录屏复验，最终文档HEAD以远端/交付REMOTE_DELIVERY.json为准。

## 先继续三个实际阻塞

1. 日期/UI窗口停止绘制。精确v102：fixture1/7/4权威与TextView均正确，onDraw9/WindowFrames8停住，Window PixelCopy无backing surface，Surface仍更新，两类整屏仍一月。原发布badd APK原文件、无screenrecord的独立控制同败（onDraw77/WindowFrames76）；不要再把它未经证明地归给录屏/单截图API/模拟器。排查MainActivity可见decor/窗口Surface及traversal/合成恢复，保留完整屏幕与原生Surface、帧标识。禁止测试专用重绘/硬编码月份/重启页面掩盖。之后补真实读档、跨月跨年、播放结束/跳过、Dialog、前后台、重建矩阵。
2. 正常冷启动入口相机。NativeColdStartInstrumentation从pm-clear后的正常启动页真实触点进入184剧本，第一次2D/3D后span15、局部视野；ScenarioFactionPicker.mapFit与MapHost首次切换需定位。原R12自行fit全国后通过，不能代替此入口。不要缩地图或用已有auto.sg11绕过。
3. 完整触控开局链。本轮cold探针在“UI overlay continues drawing live labels”严格断言失败，选势力确认之后均NOT_REACHED；保留断言并修正式UI路径。闭合后再延长现有真实触控探针至出征、移动、攻击、战报、下一旬、手动存/读档；原R12的commandFlow含API操作，不冒充全触控。

## 然后按原顺序继续

复用SceneAssetQueue/SceneWorkQueue补CPU纹理/图集/rig异步、引用/取消/上传回调/驱逐/过期worker回灌及20次真实生命周期；Filament对象仍owner线程。再做V1/V2真实地形/材质/城港关/景观/四季修复及PC近中远双向和连续运动验收。V1未闭合、V2 FAIL，不能全国扩散。R10允许刚体动画，但可见脚滑/漂浮/靠岸仍需修；R11补CALM/EXTINGUISH和全事件播放矩阵；R13做UI编辑/头像/新剧本链和DocumentProvider能力内的取消/撤权/断写/冷启恢复，不能把openOutputStream(uri,"wt")视为原子替换。

## 可直接复用的验证

- `bash scripts/test-native-r03.sh`：原R03及新增全国饱和缓存回归。输入47e5的负例为“national sweep evicts first sample”。
- `python3 scripts/check-native-protected-assets.py`：精确298资产+4历史材质源/编译物哈希；不得删成忽略assets。
- `.github/workflows/native-remediation.yml`：构建精确SHA、16主机、独立完整core输入/候选、原R12、聚合采集与冷启动。原ready120秒未改。
- `.github/workflows/native-date-control.yml`：直接下载badd已发布原APK并核对b4deb413…哈希，只重建测试APK，无录屏。当前机械断言绿色不等于日期像素通过。
- 最新APK CI36097729262：host成功；Android机械focused=0、R12=0、cold=1；整屏日期FAIL。原R12 checks60370包含轮询。Release是PARTIAL候选。
- 控制CI36098809236：旧APK原文件，机械PASS79、实际视觉FAIL。原始PNG和完整日志均在交付包。

全core两份exit1同CoreTest.logistics:75继承AI失败，不能改AI/删断言冲绿。无ARM64真机，本轮NOT_RUN；SwANGLE日志、视频、PSS结果不能替代手机，No process found不是零内存。

本轮没有留下待接入的渲染半重构；生产修复已接正常路径并构建安装，未闭合项明确保留。最终证据ZIP包含输入原包、源码patch、所有原始截图/录像（损坏段标记）、日志、退出码、APK身份、213条结果与远端复读记录。
