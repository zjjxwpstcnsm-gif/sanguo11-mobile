# 用户反馈 v122 — PARTIAL

## 正式实现

延续远端串行分支，基线 447fc34c8db5759a5a762adb67ca97f31fa86fca，包含 main ac29b458325b52d6e302ca44270d16552de4ed7f 的架构优化。仍为 MainActivity → MapHost → FilamentMapView / Filament 1.56 OPENGL；未启用 Unity。

道路连接按两端各自的真实地形分材质，栈道木面不再蔓延到难所/普通道路半边或交汇点。SWAMP 已有格使用平滑的较暗泥地材质，仍是陆地；不更改类型、通行、存档或规则。新增 Blender 4.2.3 制作的 41 个 GLB：3种城池、港口、关隘各3级LOD，13种部队各2级LOD。正式站点加载器和 FieldAssets 使用版本化资源，保留旧307个资源的原始字节。部队保留分部件骨骼/动画逻辑，新范围写入 rigs-v122.json。总53014三角面，最大城池5940三角面，全部单材质；总8,591,748字节。增加实际几何成本，尚不能宣称手机性能提升或最终高精美术达标。

## 地图还原缺陷仍开放

全国权威地图仅5格SWAMP、158格PLANK_ROAD、161格MOUNTAIN_PATH。寿春周边 q125..155/r85..110 没有SWAMP或PLANK_ROAD，森林262格。用户参考 MAP_SAN11.jpg 可见寿春湿地区域，但当前映射数据缺失；本轮不把森林伪装湿地，不擅改地图或行军规则。具体计数及参考哈希见 evidence/feedback-v122/map-review.json。华东真正误标的地形仍需权威地图校正，渲染分材质修复不能替代数据修复。

## 验证

主机专项 PASS：24,858,362 检查（材质边界、湿地陆地性质、26模型5动画多姿态、87全国站点与全存档/RNG不变）。原3D资产/生命周期、grid/coast 和 architecture 主机专项通过。完整 core 在原447fc和候选均失败于 CoreTest.logistics:75 / AI uses deployment commands，原断言保留。旧R09/R10原断言、APK构建、正常入口API29/35和ARM64授权检查由本次CI继续记录，未有证据前均不标PASS。Blender源文件和离线预览不是APK实拍。

本轮不合并main，不自动开始下一阶段。运行/视觉/真机未通过意味着整体保持PARTIAL。
