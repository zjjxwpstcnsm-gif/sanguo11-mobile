# 20261003 纯3D UI 实装验证

程序源码提交为 9d5170699be51771e61c08df0e48519476f7f3ba，父增量 e009581。
完整AP基线 c19de2957127767e222776ee9dbdefb73e57673f，4078文件329563797字节及4份native输入。
本批只合入基线后的UI增量，不能将该基线core覆盖到最终规则会话。
core/game-api/game-runtime/data/unity 的510文件仍逐字节等于AP交接，168原始地图/美术资源SHA不变。
原项目dirty树与PC参照保持保留/只读，不运行Wine。

## 同一冻结 APK

- Android29 x86_64模拟器 emulator-5582，独立AVD/Gradle缓存；未操作5554/5580。
- 主 APK SHA256：fb98b5e1765ac405c0dc8efe81080b4c96451e88a2b70a6a30ccf448177c214b，82696095字节。
- 测试 APK：23463ddf60ca927f25005602117d184f938b1c98a5364e0f94a7554873d1dc36。
- ARM64 主 APK：115fd81d377e507fd7111f065332ce5c70bb1a698d5957dfda0a4b8e23c92948，82638936字节。
  ARM64只完成构建、三份native库ABI和168资源/9PCM校验，未在ARM真机安装或测试喇叭/热状态。
- 每次安装前保存files/shared_prefs；安装读回主/测试APK完整SHA，结束还原原文件并删除新增文件。
  所有结束流程字节恢复全等。设备原有两个测试局文件auto/manual3 SHA均为
  02ddb3d44d98fbebe763a82551b5cb5eb68da6e70b6087568c0a73943bde5d69。
  未复制或改动原用户设备存档，不把独立测试局称为用户8255自动档。

| 流程 | 实际检查数 | 秒 | 结果 |
|---|---:|---:|---|
| criticalAudio | 15 | 16.74 | PASS / 原文件字节全等 |
| terrain | 51 | 76.73 | PASS / 原文件字节全等 |
| audio | 47 | 31.06 | PASS / 原文件字节全等 |
| opening | 64 | 70.57 | PASS / 原文件字节全等 |
| march | 74 | 147.17 | PASS / 原文件字节全等 |
| combat | 72 | 59.0 | PASS / 原文件字节全等 |
| reducedMotion | 75 | 56.8 | PASS / 原文件字节全等 |
| build | 52 | 49.2 | PASS / 原文件字节全等 |
| mapEdges | 34 | 41.74 | PASS / 原文件字节全等 |
| mapNative | 22 | 35.3 | PASS / 原文件字节全等 |
| pure3dLifecycle | 28 | 46.28 | PASS / 原文件字节全等 |
| userResume | 40 | 30.21 | PASS / 原文件字节全等 |
| longRun | 342 | 938.09 | PASS / 原文件字节全等 |

以上13流程916检查。另在同一对冻结APK上：

- Pure3d coast：97检查、54.21秒；下邳/海陵港/小沛，两种跨度两种朝向，12组Surface/UI截图。
  保留PC四分之一格高度/法线/材质采样，去掉不一致粗补底，完整save/RNG不变。
- MapEditor67：61检查、76.91秒；实际3D画笔/第二指取消/撤销重做、城市七格触控、移动城市、
  AtomicFile backup恢复、库删改不破坏固定版本存档、90据点自定义新局。
- 专属设备混音区间：额外47检查、29.11秒音效流程，实际输出PCM区间3227648字节；九个音色
  独立匹配最低相关0.9874416539。原PCM单独保存，新的WAV只添加容器头，未改PCM。
  音源为原创可复现PCM合成，未声称PC事件/声音编号匹配。实际提交暴击与直接规则参考save/RNG全等，
  无专属全屏线索的简雍螺旋突刺CRITICAL非零流一次，预览取消不触发，快速双击一次扣费。
- 成功行军录像1710帧全部解码，155.09秒；视频无音轨，音频WAV另附。

同包Android合计1121实际检查，包括重复音效专项。不能把不同历史APK的旧通过合并成该包通过。

## 性能与已知缺口

24完整真实回合及24次Home、自动档和authority每轮等值，938.09秒。没有截断AI、重演命令或扣除
暂停/后台墙钟。CPU提交CSV、首提交/首像素校验、恢复耗时和主线程延迟日志随结果保存。
renderer的resume_verified仅是门控开启后的时间，不等同用户发起任务到看到地图的完整等待。

PSS 176704–211251KB，开始177840KB、最后211251KB；Java实际用量77953408–106577696字节。
姿态缓存14→96后保持预算96，纹理40、材质13、估算纹理75460576字节保持不变，GPU网格缓冲
约6.5→9MB后稳定。PSS/Java仍上升，未宣称长时间无泄漏，需继续分析缓存及规则/日志数据保留。

最新系统任务启动到新PixelCopy像素的六轮为3245/1142/1151/1161/1154/1215ms，1000ms目标全部false。
后台应用startActivity路径仍约3–5秒。Android10源代码存在5秒应用切换限制，与原路径现象相符，
这是结合代码与A/B结果的推断：[Android10 ActivityTaskManagerService](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android10-release/services/core/java/com/android/server/wm/ActivityTaskManagerService.java)。
实际完整墙钟保留；不扣掉该等待后宣称流畅。模拟器同一宿主存在其他独立AVD，数据不等同ARM GPU性能。

生产APK无MapView/MapOverview类，所有当前开局/预览/地图编辑/命令选点/报告/恢复入口由单一3D宿主负责。
历史GameSmoke/Reference版本专属2D UI助手尚未全部迁移，不宣称全部旧Android套件全绿；映射及缺口见
ACCEPTANCE_MAPPING.md。报表定位到3D由单宿主代码保证，版本专属旧截图矩阵仍待逐项替换。

最终规则会话需把UI增量接到最新规则，复验骑兵真实城市七格落点与可视边界。本包规则仍为完整AP基线。
异步只读save/显示DTO性能接口需求已记录CONTRACT，由规则负责人实现，UI本批没有越界实现。
完整architecture命令仍有既存GameSessionTest exact old-scenario starting state失败；510保护文件没改，
未删除/弱化断言。边界静态检查、原始资源pins、地图几何/坐标/编辑宿主测试均通过。

## 保留的失败证据

e009581第一次24回合逐轮状态校验全部完成，但最后从测试线程遍历UI资源集合触发并发修改异常，
整个套件保留FAIL/705.31秒。本次修复为UI线程取得不可变报告和逐轮写CSV，再严格重跑24回合通过。
新增暴击夹具首次因直接改当前能力缓存被正确拒绝，现保留原能力，通过既有枪神特技触发真实暴击。
误传通过标志、早期viewport/加载遮罩相关测试失败及部分录屏仍保留在out/pure3d，未换名冒充成功。

目标仍active，不把本次UI交付称为最新规则组合最终验收完成或ARM真机/流畅性验收通过。
