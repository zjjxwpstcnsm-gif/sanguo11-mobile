# R00–R14 审计基线（追溯补齐）

本文件于 2026-09-25 总审计新增，不充当 R00 原始运行凭证。远端 main 为 ac29b458325b52d6e302ca44270d16552de4ed7f，架构 PR #66 已合并；读取到的串行分支输入为 eeb09b4b0037a482b440816ec2fe718c9701ab75。继续同一 agent/native-pc-visual / PR #67，未从旧 main 重建，未合并或删除任何分支。

源码恢复：GitHub CI36090118181 对精确 554c7f1948cbed9fd45415b1a40aadc561b00d30 生成 archive，下载拼接 SHA256 为 2ceef16123dcf13c6fc4610d57402cce23d16ba526c282bc3fe1064cfb65ab3c；本地树逐文件与远端 tree c4b299380de0f7981f3e07bf25b14c97803fd709 对齐。554 相对输入仅增加取证 workflow，没有重写生产代码。本轮可构建源码为 badd7e56308b4f6a47d60353d49023be8fc492a2，tree aa74d2420b271bc8a46bc11610e6f0147f98102e。

## 架构与数据保护

GameApplication / NativeGameHost 装配 GameSession；GameSession 仍是 World 的唯一权威。MainActivity 负责宿主，MapHost 提交不可变 MapSceneSnapshot，Filament 只消费投影。现有 app -> game-runtime -> game-api + core 依赖方向及白名单由 check-architecture.py 复检通过。

本轮从 eeb09 输入到 badd 源码，对 core、game-api、game-runtime、data、app/src/main/assets、unity 的 CI 差分严格为空；没有更改地图/Hex/七格/港口/AI/收入/兵源/伤兵/存档/随机数。不要把这个限定结论扩大为整个 R00–R14 从 main 起 core 从未变动：此前 R11 包含类型化展示事实的元数据调整，另有保存黄金样本回归。

## 旧 S01–S13 功能盘点与当前承接

以下是现在的承接路径，不宣称每个类首次创建于对应旧阶段，也不把旧阶段编号转换成 R 阶段通过。

| 旧功能阶段 | 当前可复用正式路径 | 结论 |
|---|---|---|
| S01 原生基础 | MapHost、FilamentMapView、MapSceneSnapshot、SceneCamera | 正常入口保留，新增焦点门控修复 |
| S02 连续地形 | TerrainSurface、TerrainTiles、SceneMesh | 连续采样/约束/串流复用；轮廓和岸线需修 |
| S03 城港关 | SiteGlb、SiteVisual、3d/sites | 纹理模型/映射存在；过渡美术未验收 |
| S04 设施/植被 | FieldAssets、Vegetation、设施投影 | 合批与排除区保留；非全设备性能通过 |
| S05 单位姿态 | UnitVisual、UnitMotion、UnitAnimation、UnitFormation | 刚体动画/编队承接；不称完整骨骼/脚部IK |
| S06 战斗显示 | CombatVisual、TurnPlayback、现 CombatSequence | 真实事件投影保留；全事件实拍缺项 |
| S07 交互 | ScenePicking、MapHost、Filament Overlay | 正式命令入口保留；全图预览仍失败 |
| S08 性能/资源 | SceneQuality、可见块缓存、诊断 | 有界策略存在；CPU估算不是真实GPU/PSS |
| S09 安全候选 | MapHost 异常启动标记、ES3预检、2D回退 | 保留；完整故障注入/真机未验收 |
| S10 地表材质 | TerrainMaterialField、TerrainArt、3d/terrain | 同源权重/贴图继续用；V1未闭合 |
| S11 地貌/水体 | TerrainSurface、现 WaterVisualField/水材质 | 不改水陆拓扑；阶梯岸线仍可见 |
| S12 环境景观 | 现 EnvironmentProfile、LandscapeProfile、植被窗口 | 已接常规场景；美术/阴影时序未通过 |
| S13 网格/全国 | MapHost 独立网格/势力开关、VOID非交互外景 | 保留；不把全国显示当全国PC美术完成 |

docs/3d/progress.md、docs/3d/world-art/progress.md、progress.md 和 Unity 文档内的旧 PR/推送失败文字是历史记录，当前合入与否以远端为准。旧原文不删；本轮前 PROGRESS/DEFECTS 原 blob 另保存在 history/。

## 失败基线

本轮 CI36091281604 分别在 eeb09 与 badd 完整执行 test-core.sh，两者均 exit 1，原始断言均为 `AI uses deployment commands`，CoreTest.logistics:75。比较任务成功只是证明失败被保留并分类，绝不是全 core 测试通过。

同一个新增 Android 探针编译到未经修改的 eeb09 生产树，真实全屏 Dialog 后 `covered map stops native frame callback` 断言失败，frames=4、queued=true。修复不改该断言。

R13 旧 workflow 在本轮 7625 CI36090968390 的保护检查失败：比较旧基线时发现 R14 已合法改过 ground/water/site/unit 四份 filamat。原始差分保留，后续步骤是未运行，不算 R13 新游戏回归；本轮审计 guard 使用实际 eeb09 输入且完整资产为空差分。
