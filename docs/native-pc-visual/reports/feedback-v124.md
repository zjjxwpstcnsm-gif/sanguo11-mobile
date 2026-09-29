# v124 Blender 地形装饰、城港关和部队 / 性能续作 — PARTIAL

本轮承接 `agent/native-pc-visual` 的实际远端输入 `13fa313e39a9bc9ff157701b721d79c62144ce1e`。读取附件全局规则和现有架构/失败账本后，复读 main 为 `ac29b458325b52d6e302ca44270d16552de4ed7f`（架构 PR66 已合入），PR67 open draft。不重建 R00–R18，不启动下阶段，不合 main。

## 正式改动

Blender 4.2.3 LTS 实际导出 49 个新版本 GLB：城池三个规模、港口、关卡各三个 LOD；13 类步骑/攻城/水军/运输部队各两个 LOD；阔叶树、山地树、灌木、岩层各两个 LOD。增加院落廊亭、城门砌石、码头木构梯子、兵甲裙/盾饰/马具/船舱细节与自然植被。沿用原项目 CC0 图集和原创几何，记录轴向、尺寸、关节、预算、哈希。保留全部旧资源。

在正常 `MainActivity -> MapHost -> FilamentMapView` 路径使用 `sites/v124`、`field/v124` 与 `rigs-v124.json`。地形装饰沿用现有连续地表、摆放种子/排除入口/道路逻辑；没有改高度场、地形类型、六邻接、城池七格或地图。不是宣称全部全国 PC 高精美术完成。

分块植被流式生成改用 primitive 数组，保持逐顶点/UV/三角形数据和顺序。部队姿态直接旋转 Blender authored tangent frame，保留制作法线，复用不变拓扑/UV。解码重试不再反复重置既有单位位置和运动状态。保留 Filament 1.56.0 OPENGL、Choreographer、原 frame gate 和 GPU backpressure，不降画质、帧预算、部队数或 ready 门槛。

## 验证与证据边界

主机独立编译原输入算法，对相同新资产精确比较两种 staggered 地图、植被近远 LOD、所有 13兵种×2LOD×8动作×12帧的顶点/索引/UV，验证完整存档/RNG不变、缓存复用和制作法线旋转。主机分配/CPU测量不等于 Android FPS；保存原 CSV，首次法线实现变慢记录也保留，已据此优化。

Khronos 全量 GLB 检查、架构隔离/权威测试与本轮受影响测试记录在构建产物。原 core logistics:75 双边失败已独立复现；旧 R09/R10 断言继续运行并原样留证，不改规则和预期哈希来遮盖。

APK、严格原120秒ready、实际Surface/屏幕、API29/35同区域小场景/官方寿春/港口、新触控拖动缩放及静态提交采样正在验证。当前提交不预写 PASS，不使用 Blender 离线预览冒充 APK。工作流先复现逐字节一致模型、串行提交正式资源，再按该完整源码 SHA 构建；最终源码/产物和结果随后补充。

PC同区域参考未提供：`REFERENCE_MISSING`。ARM64 Adreno/Mali 和30分钟真机 `NOT_RUN`；历史 Firebase WIF attribute condition 拒绝仍待实际复验。新开局完整纯触控链、SAF旧档、20次生命周期等历史缺口继续开放。本轮主机通过不能关闭这些缺口。
