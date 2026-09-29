# v121 交接 — PARTIAL

本轮仅处理山地/关隘、网格卡顿和免费工具建模反馈。串行分支 agent/native-pc-visual / Draft PR67；main不合并，不开始下一阶段。先复读远端再续作。

正式路径、范围、参考与验收缺口见 reports/feedback-v121.md。模型制作可用 Blender4.2.3 `--background --python tools/3d/refine_blender121.py` 重现；7项正式资源身份在feedback-v121-blender-assets.json。旧资源和规则模块不覆盖。

旧版全几何hash必须保留，不为新增山体/岩石美术重写为PASS。初版坡度造成格心拾取失败后已收缓，原断言必须重新执行。四个无邻山关隘需由用户另行决定是否允许地图数据变更；当前不可假山封路。

安装对照使用真实v120与候选v121、相同测试APK、正常MainActivity存档恢复。固定camera API与明确测试场景不等于全触控；全国虎牢关单独执行。120秒ready不放宽，Quickstep恢复只识别精确外部桌面ANR并真实点击关闭。任何运行失败与未到达下游保留，不把软件模拟器计时当真机FPS。
