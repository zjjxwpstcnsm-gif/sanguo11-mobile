# PC 地图导入（v131起，持续校准）

在 `agent/native-pc-visual` 当前分支上实现。输入是用户提供的 PC 整合版，不能当作未修改的零售原版。源游戏目录保持只读。现已取得PC实际D3D9地图与同镜头安卓帧缓冲；差异仍未验收。部分格式运算已用隔离机器码执行核对。原始归档及各资源的 SHA-256、偏移、转换结果见 [source.json](source.json)。

## 已完成

- 从 `Media/san11pkres.bin` 的 LINK 表读取 SHEX 地格，恢复完整 200 × 200 地理数据。与之前第 63 修订相比，19,642 格分类改变。
- 42 城、10 关中心与原表吻合；35 港中 11 港的位置校正。591 处开发地恢复原坐标，归属沿用现有行政父城表。
- 导入 K3ST 的 1025 × 1025 高度顶点、1024 × 1024 面水位数据；每游戏格由 4 × 4 个面构成。v131–v146先按细面裁切；v147按原版415e20/422980恢复完整粗水格和四角透明度，旧细面裁切保存在历史夹具中。
- 导入四季 GCOL 底色、36 种 WFTX 近景纹理、顶点纹理索引，以及每个面四层原始纹理 ID／角点掩码。资源顺序为秋、春、夏、冬，移动版按既有季度日历选择。索引以 RGB 保存，避免 Android 灰度转换；纹理图集使用显式梯度采样，避免材质索引跳变导致错误 mip。
- 地形、镜头拾取、城市高度和局部剧本裁区共享源坐标；PC 岸线不再套用旧版的程序扰动。
- 地理修订升为 64，新开局默认 3D。旧存档保持原地形、实体与修订，63 修订的 14 格禁行表单独保留。地图编辑后的自定义地理继续使用现有可编辑地表。

## 还原边界

这不是完成的逐像素 PC 移植。近景面的四层纹理 ID 与角点掩码已解析并用于三角面插值，但剩余低位标志、远景材质和特殊动画纹理仍未原样复现；PC 法线、光照、垂直比例、纹理 UV 比例和原水面着色还需对照 PC 实际画面。v145已按原版运算将高度统一为源字节×.025；v146按原版415bb0修复完整水位字节[44:52]及微小防重叠偏移。此前四位水位损坏14010个面、漏掉3797个面；PCMAP002拒绝旧包。v147已接入原粗水格、两张4844动画图、透明度与混合；原逐格时钟、外缘范围与PC视觉对照仍待完成。

静态模型、部队与设施后续进展以及未完成范围见 `../pc-visual/validation.json` 和 `../pc-visual/coverage.json`。v132 后续在同一工作区把 OBJS 类型 46/47/48 的 943 条源植被对象接入，PC 模式不再调用旧推定植被/长城/瀑布布置；城市、设施、单位与其他源对象及动画仍待恢复。面记录位 2–3 是有效覆盖层数，无效层的旧字节必须忽略；v132 修复其误读导致的块状接缝，全部 4,194,304 个面角与源顶点材质完全匹配。详细来源、格式证据和剩余差异见 `../pc-visual/`。地形规则继续使用现有枚举：土和荒地合并为平原，岸和崖映射为阻挡地形，原始 20 类 ID 则完整保存在 PC 数据包中。开发地归属未移植 PC 剧本规则。

## 验证

- `scripts/test-pc-map.sh`：源坐标奇偶、40,000 格高度／岸线、87 据点、591 开发地、35 港上下水校验、实际上传网格的原高度／水面裁切、拾取、所有裁区、新存档回读、真实修改前 63 修订存档与禁行表。
- `scripts/test-3d-interaction.sh`：32 项交互检查、131 项地图编辑器集成检查。编辑器测试显式创建港口岸边地格，适配原图不可通行的岸线，正式地图不因此修改。
- `scripts/test-native-r04.sh`：原地表机制和地图载入回归。
- `scripts/verify-map-release.py [APK]`：65 项资源逐字节摘要检查，含 PC 地形、贴图与编译后的 Filament 材质。
- `PcMapInstrumentation`：API 29 x86_64 模拟器、宿主 OpenGL、中画质；实际安装生产 APK，读取真实 Surface，检查洛阳、顿丘港、西南地形及完整权威状态不变。截图、报告与日志保存在 `out/pc-map/`。这不代表 ARM 手机性能已经验收。

整套历史 core 测试未全绿。CoreTest 的 AI 出兵断言，以及 ScenarioTest/ContentTest、DomesticTest、ArmyTest、CampaignTest、RulesParityTest 的失败，已在修改前 HEAD 的隔离源码上复现为同样错误；没有改断言去掩盖这些既有问题。软件显卡模拟器首轮渲染过慢，画面验收改用宿主显卡；没有将软件显卡的黑屏当作通过。

旧存档夹具 `core/src/test/resources/pc-map-v063.sg11.gz` 由修改前 HEAD 的 `heroes-250` 开局生成；测试读取后重新编码与原始字节完全相同。

## 重建

Python 3.11+，依赖 numpy、Pillow：

```sh
python3 tools/content/import_pc_map.py '/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版'
bash scripts/test-pc-map.sh
bash scripts/test-3d-interaction.sh
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest
python3 scripts/verify-map-release.py app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w game.sanguo.mobile.dev.test/game.sanguo.mobile.PcMapInstrumentation
```

导入器针对这套 LINK/SHEX/K3ST 布局校验资源长度、顶点索引、开发地数量和据点对应；不兼容的 MOD 会报错。原始 63 修订的参考地图保存在 `data/map/reference-pc/`。材质源码为 `tools/3d/pc-ground.mat`，用 Filament 1.56.0 的 matc 编译为移动 OpenGL 格式；更改材质后还需更新 `VerifiedMaterial` 与资源清单的摘要。

## v146水位校准验证

`check_pc_water.py`在原EXE执行1020组/256种水位及邻接地高，读取完整8位而非4位；可输出独立PCWTR001参考，交给`bash scripts/test-pc-map.sh out/pc-visual/v146-water-source-reference.bin`核对全部源面与实际运行时采样。此次1687046项主机检查通过，原1e−5岸线断言未放宽。完整原水面材质、粗拓扑、动画、PC画面对照、ARM性能仍未验收。

## v147原粗水面接入（持续验证）

`tools/content/import_pc_water.py`只读源归档和EXE，在隔离源内存执行粗水格初始化，输出独立`maps/pc-water.bin.gz`和两套原RGBA贴图。`tools/3d/build_pc_water_material.sh`重建材质。原415e20选4×4细面中第一个非零水位，415d80生成TL/TR/BL/BR透明度255/32；422980绘制完整四角，41db10关闭水面深度写入后恢复。源4844为两张256×256、8×8帧图。

`bash scripts/test-pc-water.sh`核对全部源粗格、全国唯一上传、源高度/透明度/UV/拓扑/相位周期、原细面字段、拾取、权威/RNG/存档。现全国上传19496粗格，其中13017覆盖可玩格，恢复38027个此前漏水的细面。外缘580粗格尚未进入当前背景范围，原逐格可见列表时钟、PC画面对照、MOD运行覆盖及ARM性能仍待完成。安装、录像及限制见`../pc-visual/validation-v147-working.json`。


v156地面图144张保持原64/128/256 RGB texels；以原41dcb0/41dc00执行UV核对，尺寸表36×4按秋/春/夏/冬绑定正式材质，不再统一放大256或统一周期。转换`pc_ground_palette.py`，独立像素/边界检查`check_pc_ground_palette.py`，原UV检查`check_pc_ground_uv.py`。来源及逐图摘要见`../pc-visual/ground-palette-source.json`。正确matc56编译后实际APK验证与剩余差异见`../pc-visual/validation-v156-working.json`；本阶段不能作为完整还原验收。


v157地表原明暗/法线/环境：`import_pc_ground_passes.py --output app/src/main/assets/3d/pc-map --install`转换K3ST数值法线及四季RGBA明暗、4807描边；`build_pc_ground_material.sh`编译两个材质并更新摘要。`check_pc_ground_environment.py`核对生产Java、原441d30和实际秋冬c26/c27。原网格4804虽已正确保留RGBA，尚未运行时绑定。安装、对照及未完成项见`../pc-visual/validation-v157-working.json`。
