# 地图模式验收迁移

生产层只有MapHost/Filament；MapView/MapOverview留在androidTest历史资源夹具，主APK无类描述符。
以下是当前3D等效验收及尚未覆盖的范围，不以保留历史2D断言代表其已在3D通过。

| 原验收意图 | 当前3D验收 | 额外要求或缺口 |
|---|---|---|
| 地形、通行、RNG不可被显示修改 | test-pure3d-seams / PC map checks、terrain UIUX | 完整SaveCodec字节与StateToken、部队/运输/敌军/城港关邻接 |
| 屏幕点回真实地块、坐标定位 | terrain/march/combat、MapEditor67、原生地面射线 | 真实高度投影、射线命中校验、实际触控；四地图的原来源坐标/现存地形/边角已实装；不声称旧版本逐像素相同 |
| 城市占地每格选择同一实体 | MapEditor67及SceneInstrumentation七格断言 | 实装编辑器七格已过；最新骑兵规则落点由最终规则集成方复验 |
| 缩放、拖动、面板互不串手势 | mapEdges/mapNative | 单指拖动、取消、双指缩放、横屏、穿越面板边界、Home无延迟跳动 |
| 模式与相机恢复 | pure3dLifecycle/opening | false请求也保持3D、旧缩放迁移、错误释放引擎、真实像素成功后才清故障标志 |
| 开局预览及取消 | opening / mapNative | 原生加载遮罩、Scoped preview engine释放、手动档保持、真实新局及加载 |
| 编辑地图与选点 | MapEditor67 | 独立草稿、笔画/双指取消、撤销重做、真实实体移动、不可变库版本/旧档 |
| 正常部署/行军/战法/建设 | march/combat/build/criticalAudio | 真实命令、只读预览、取消、快速重复提交、费用/伤害/RNG参考一致 |
| 长时与暂停恢复 | longRun/userResume + renderer log/frames | 24完整回合、逐轮自动档等值、PSS与Java样本、真实墙钟；CPU提交数据不冒充GPU帧率 |

默认GameSmokeRunner已迁到实际纯3D矩阵，旧地图/导航/资源参数进入明确标记的当前3D套件。
ReferenceXX及GameSmoke的版本专属MapCamera/MapView助手仅在historical2d=true归档分支保留，
其原始断言没有弱化，也不把当前UIUX结果标成REFERENCE旧版本通过。旧版本像素/版本号断言
与缺失的v57-60原始save仍未逐项复跑；当前来源坐标、玩法与只读状态要求由更严格的3D检查承接。

本轮迁移：UiUx legacyNavigation 使用真实3D小导航，当前官方地形没有旧VOID洞，
故以明确标记且通过SaveCodec校验的一格VOID夹具保留拒绝约束。原始版本断言/文件保留。
legacyNavigation03 四个实际剧本/裁剪图37检查通过25秒，全文件恢复全等。
reportLocate3D01 真实攻击战报定位与返回通过12.25秒，镜头/选择恢复且全save/RNG不变。
区域坐标/全部现存地形/裁剪边角矩阵已在3D射线和真实触控下通过：190/250各241检查，
中原103、荆襄97。当前包导航37、原生手势/恢复22、攻击战报定位/返回14检查均通过。
渲染驻留改动只影响资源创建/释放：预取镜头周边，额外固定活动演示参与者/路径格；
不改变快照实体、权威位置、导航数据或规则。主APK a3e8d9c...770ea的本轮完整玩法/24回合、模型平移、真实v32旧存档、退出重开和默认GameSmoke均已实装通过，
选定成功流程共2868检查（包含重复），明细及保留失败批次见RESIDENCY_VALIDATION.md。
各新结果独立记录，旧9d51706的1121检查不能直接算作这个新包通过。
