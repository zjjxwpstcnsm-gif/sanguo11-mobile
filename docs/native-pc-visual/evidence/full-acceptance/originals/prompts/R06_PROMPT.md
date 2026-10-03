# sanguo11-mobile｜R06 可复现资产流水线与受控模型格式

你接手正式仓库 `https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile.git`，本轮只执行 **R06**。
这不是方案讨论、目录规划、素材提示词或独立演示任务。必须真正修改正式源码/资源，接入现有游戏运行路径，验证、构建本阶段 APK、commit、push 并创建/更新真实代码 PR。

## 先执行的约束

完整读取本包 `00_GLOBAL_RULES.md`、`01_ROADMAP.md`、`02_BASELINE_AUDIT.md`、`03_RENDER_CONTRACTS.md`、`04_VISUAL_ACCEPTANCE.md`、`05_ASSET_PIPELINE.md`、`06_TEST_AND_PERF.md`、`07_GIT_DELIVERY.md` 和本阶段全文。

读取远端 `agent/native-pc-visual` 最新提交与对应 PR；首次 R00 才从包含架构优化的最新 main 建分支。已知检查点 `ac29b458325b52d6e302ca44270d16552de4ed7f` 不是强制回退点。不要从旧 main/Unity 分支重启，不覆盖 S01–S13 和 game-api/game-runtime 成果，不重做已可用底层。

默认保留现有 Filament/OpenGL 原生后端；遵守 R00 的唯一后端决定。不要重启 Unity/Godot/Unreal，不无依据另写完整 GLES 引擎。保留 Java 唯一规则权威、现有地图拓扑/坐标/占地/数据与存档。

全程使用 `agent/native-pc-visual` 与同一个指向 main 的 PR。**暂不合并 main，不处理其他 PR，不删分支、不 force push。** GitHub 瞬时失败先读远端确认结果，再有限重试。

读取 `docs/native-pc-visual/PROGRESS.md`、`DEFECTS.md`、前阶段报告/交接和现有架构/原生美术报告。读取 R05 的远端交接及所依赖门槛；先修会阻塞本阶段的前置问题，再执行本阶段。

## 本阶段目标

把模型/纹理/材质从临时文件变成可追踪、可导出、可校验、能在正式地图加载的生产流水线。

## 必读源码与输入

读取 R05、tools/3d、SiteGlb、FieldAssets、现有 assets/3d 与各资产 manifest；参照 05_ASSET_PIPELINE.md 与官方 glTF/Filament 文档。

类名/目录仅作定位，必须查到真实文件与运行入口；不存在的路径说明并找对应职责，不能造空文件假装已有实现。

## 必须实际实施

1. 盘点现有 site/field/environment 资源的真实格式与加载子集，列可复用资产和明确占位物；不要先写第二个通用 glTF loader。
2. 固定资产目录、ID/版本/来源/许可/hash、LOD、pivot/单位/朝向、材质/动画支持和正式映射。原始源文件/脚本与 runtime 输出分离；记录可用建模/纹理工具及版本。
3. 实现可重复 CLI 导出/转换：实际支持静态网格与当前刚体动画子集；需要新格式/动画时定义必要最小扩展。保持现有 runtime 和离线材质工具版本匹配。
4. 增加 glTF 格式检查和项目子集检查：长度/索引/offset/stride/component/node transform/数值有界、贴图存在、primitive/预算、所需扩展。支持范围外离线转换或明确拒绝，不静默删骨骼/材质。
5. 加载流程异步解码、共享 CPU rest data/GPU 几何/贴图、按需上传、可取消并引用计数释放；异常资源有诊断和可识别回退，不能直接导致 native crash。
6. 做真实示范资产：至少一个有城门/屋顶轮廓的据点组件、一个带实际纹理的树族、一个带运动部件的部队/器械，替换其正式映射并截图；流水线不是只在检视器里可用。
7. 为生成资源/材质建立 clean checkout 可再现校验；若某外部美术工具非构建必需，应随源码提交必要运行时产物，使 Agent 能直接构建 APK。无许可/不支持文件不能偷偷打包。

## 必须验证

- 实际生成/导出→验证→打包→地图加载全链条；重复导出在约定归一化后 hash/结构稳定。
- 损坏 header/截断 buffer/越界 index/不支持 required extension/超限纹理/路径穿越输入全部安全拒绝。
- 多实例共用资源，删除/读档/切换世界后正确释放，不把共享缓存认作真正 instancing。
- 代表资源在实际 APK 不缺面、不翻转、不黑材质，锚点/比例正确，源文件可复查。
- 验证库/工具版本与 CI 实际可安装可执行，不只写 requirements。

仍需执行全局要求的受影响玩法/架构回归、实际 Android 构建与本阶段图形运行。继承失败和新失败分开，不能删除断言、跳过受影响用例或把 NOT_RUN 写为 PASS。

## 通过门槛

资产流水线实际跑通且产物进入正式地图；加载器支持范围、许可、预算清楚。三件示范资产不等于全部建筑/部队美术完成。

构建成功≠运行成功；模拟器通过≠真机通过；有模型≠美术达标；截图有颜色≠PC 对照通过。缺实际参考/资产/设备时继续完成可做工作，但状态诚实保留 PARTIAL/BLOCKED，不把计划当完成。

## 必须交付

导出/校验/加载正式工具与代码、实际示范资产、资产清单/许可/来源、失败输入测试、APK/截图、R06 交接。

交接文件落在 `docs/native-pc-visual/reports/R06.md` 和 `docs/native-pc-visual/handoffs/R06.md`，同步更新 PROGRESS/DEFECTS/资产清单。大制品用 CI artifacts 或实际可下载附件，避免 Git 膨胀。

本轮最终回复前，把全部必要生产源码/资产/测试实际 commit/push，复读远端 HEAD/PR。交付 source SHA 精确对应的独立 APK、SHA-256、应用 ID/版本/ABI、实际后端、证据包、测试与未完成项。若末尾只补报告，分别注明 source SHA 与 evidence-only HEAD；修改生产代码后必须重建。

无法构建/安装/推送时给出原始错误和实际完成范围，不伪造 APK/链接/远端提交，不只回复一份下一步计划。

## 本阶段特别禁止

禁止编造 GLB 文件存在；禁止自己写半个 loader 却称支持完整 glTF；禁止只交图片渲染不交模型；禁止让每次 assemble 必须开人工交互 Editor。
