# 地图模式验收迁移

生产层只有MapHost/Filament；MapView/MapOverview留在androidTest历史资源夹具，主APK无类描述符。
以下是当前3D等效验收及尚未覆盖的范围，不以保留历史2D断言代表其已在3D通过。

| 原验收意图 | 当前3D验收 | 额外要求或缺口 |
|---|---|---|
| 地形、通行、RNG不可被显示修改 | test-pure3d-seams / PC map checks、terrain UIUX | 完整SaveCodec字节与StateToken、部队/运输/敌军/城港关邻接 |
| 屏幕点回真实地块、坐标定位 | terrain/march/combat、MapEditor67、原生地面射线 | 真实高度投影、射线命中校验、实际触控；版本58-61逐区域旧截图矩阵未全部重跑 |
| 城市占地每格选择同一实体 | MapEditor67及SceneInstrumentation七格断言 | 实装编辑器七格已过；最新骑兵规则落点由最终规则集成方复验 |
| 缩放、拖动、面板互不串手势 | mapEdges/mapNative | 单指拖动、取消、双指缩放、横屏、穿越面板边界、Home无延迟跳动 |
| 模式与相机恢复 | pure3dLifecycle/opening | false请求也保持3D、旧缩放迁移、错误释放引擎、真实像素成功后才清故障标志 |
| 开局预览及取消 | opening / mapNative | 原生加载遮罩、Scoped preview engine释放、手动档保持、真实新局及加载 |
| 编辑地图与选点 | MapEditor67 | 独立草稿、笔画/双指取消、撤销重做、真实实体移动、不可变库版本/旧档 |
| 正常部署/行军/战法/建设 | march/combat/build/criticalAudio | 真实命令、只读预览、取消、快速重复提交、费用/伤害/RNG参考一致 |
| 长时与暂停恢复 | longRun/userResume + renderer log/frames | 24完整回合、逐轮自动档等值、PSS与Java样本、真实墙钟；CPU提交数据不冒充GPU帧率 |

GameSmokeRunner与ReferenceXX的大量版本专属MapCamera/MapView UI助手仍属未迁移的历史夹具，
不能运行其旧2D点击坐标并声称当前原生3D验收通过。上述当前验收保留对应规则与资源断言并加强
实际Surface/射线/存档检查，但尚不等于所有旧版本Android套件全绿。
