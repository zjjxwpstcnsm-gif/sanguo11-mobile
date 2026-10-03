# FAIL

## R00.I04 · R00

修复实际启动/入口/Surface/资源或构建阻塞；建立实际地图首帧可见性和后端诊断。禁止依靠强制进入 demo 场景、隐藏失败提示或只渲染背景骗过检测。

prompts/R00_PROMPT.md:33

两API独立正常冷启动全国预览FAIL；S01–S13逐项当前质量/完整环境门槛未闭合，不能把源码存在当完成。

## R00.V01 · R00

正常冷启动、新开剧本、读既有档，实际看见地形/至少一个据点和部队，拖动/缩放可以改变视野；运行日志与录屏同属本阶段 APK。

prompts/R00_PROMPT.md:41

两API独立正常冷启动全国预览FAIL；S01–S13逐项当前质量/完整环境门槛未闭合，不能把源码存在当完成。

## R00.G01 · R00

V0：实际正常游戏地图可见且可操作。无运行环境可继续提交环境/源码修复，但阶段为 BLOCKED/PARTIAL，不通过 V0；截图纯背景仍失败。后端决定必须锁定且具理由。

prompts/R00_PROMPT.md:51

两API独立正常冷启动全国预览FAIL；S01–S13逐项当前质量/完整环境门槛未闭合，不能把源码存在当完成。

## R01.I03 · R01

现有 Filament owner thread 不随意迁移；CPU 生成/解码使用有界执行队列，上传和销毁回原所有者；纯 GLES 分支依其已锁定线程模型。记录 CPU buffer 何时可以复用/释放，不能在异步上传结束前回收。

prompts/R01_PROMPT.md:32

本轮API29初始ready失败，切换0/前后台0；API35完成切换20/前后台1后background frame loop stopped断言失败。owner仍同步读材质、atlas和rig；晚到worker/故障恢复后半段NOT_REACHED。

## R01.V01 · R01

2D/3D 反复切换 20 次与前后台 20 次，只有一个有效渲染循环，计数回到稳定区间。

prompts/R01_PROMPT.md:40

本轮API29初始ready失败，切换0/前后台0；API35完成切换20/前后台1后background frame loop stopped断言失败。owner仍同步读材质、atlas和rig；晚到worker/故障恢复后半段NOT_REACHED。

## R01.G01 · R01

运行与所有权门槛通过；允许保持 R00 美术，但不得因拆分类破坏已有模型/水体/覆盖层。物理机未测必须单列，不能用计数稳定证明所有 native 泄漏都不存在。

prompts/R01_PROMPT.md:50

本轮API29初始ready失败，切换0/前后台0；API35完成切换20/前后台1后background frame loop stopped断言失败。owner仍同步读材质、atlas和rig；晚到worker/故障恢复后半段NOT_REACHED。

## R05.I03 · R05

对阶梯岸线采用约束简化/细化和过渡带；水域中心可航行性、关隘咽喉和最窄河道保持可读。极端窄口优先玩法清晰，不能用大曲率硬平滑。

prompts/R05_PROMPT.md:32

正常游戏Surface实际确认逐格阶梯岸线；几何仍是格子矩形并集。弯道上下船、增删水后实际航行与PC水岸完整对照不足。

## R05.G01 · R05

连续岸线和水体在正常玩法内成立，且没有一个视觉平滑改动导致通行/港口语义错误；纯蓝 Hex 或曲线穿陆不可通过。

prompts/R05_PROMPT.md:50

正常游戏Surface实际确认逐格阶梯岸线；几何仍是格子矩形并集。弯道上下船、增删水后实际航行与PC水岸完整对照不足。

## R06.I05 · R06

加载流程异步解码、共享 CPU rest data/GPU 几何/贴图、按需上传、可取消并引用计数释放；异常资源有诊断和可识别回退，不能直接导致 native crash。

prompts/R06_PROMPT.md:34

异步网格存在，但构造器同步loadAtlas/decodeStream未满足全部解码后台化；三示范资产正式画面完整验收不足。

## R12.I01 · R12

核查所有既有地图入口和操作是否完整走 3D，包括新剧本势力预览、选城选军、出征、路线/行动范围、战法位移、设施覆盖、围城范围、地图定位和小地图；不可缺项后偷偷退 2D 当实现。

prompts/R12_PROMPT.md:30

原NativeR12两API均初始ready失败。独立全国预览FAIL。API29月7/月4权威与widget正确，原整屏仍190年1月上旬；UI draw95/Window frames89冻结而3D颜色改变。完整纯触控尾链NOT_REACHED。

## R12.I05 · R12

新开剧本势力着色遵守现有君主/国号、军师等显示语义；城市标签/部队条/目标优先级防重叠，原生 UI 不因 3D Surface 遮挡失效。

prompts/R12_PROMPT.md:34

原NativeR12两API均初始ready失败。独立全国预览FAIL。API29月7/月4权威与widget正确，原整屏仍190年1月上旬；UI draw95/Window frames89冻结而3D颜色改变。完整纯触控尾链NOT_REACHED。

## R12.I06 · R12

顶部日期/年月旬、行动力、常用导航和底部操作条在不同屏幕可读；保留现有现代 UI，修布局不大改功能树。面板打开不吞掉必要地图视域或漏触到地图。

prompts/R12_PROMPT.md:35

原NativeR12两API均初始ready失败。独立全国预览FAIL。API29月7/月4权威与widget正确，原整屏仍190年1月上旬；UI draw95/Window frames89冻结而3D颜色改变。完整纯触控尾链NOT_REACHED。

## R12.V04 · R12

实際新剧本选势力→开局→选军→移动/攻击→战报→下一旬流程。

prompts/R12_PROMPT.md:43

原NativeR12两API均初始ready失败。独立全国预览FAIL。API29月7/月4权威与widget正确，原整屏仍190年1月上旬；UI draw95/Window frames89冻结而3D颜色改变。完整纯触控尾链NOT_REACHED。

## R12.G01 · R12

本阶段范围内的既有地图交互全部可用，不依靠暗中回退 2D。因明确硬件不支持而回退可以，但不能计入 3D 功能通过。

prompts/R12_PROMPT.md:50

原NativeR12两API均初始ready失败。独立全国预览FAIL。API29月7/月4权威与widget正确，原整屏仍190年1月上旬；UI draw95/Window frames89冻结而3D颜色改变。完整纯触控尾链NOT_REACHED。

## X03.L009 · GLOBAL

Filament 分支维持经验证的 engine owner thread；耗时网格/解码放后台，GPU 创建/上传/销毁回 owner thread。若纯 GLES，GLSurfaceView 的 GL thread 是 GPU 唯一所有者，用有界消息交接；两者不混用。

03_RENDER_CONTRACTS.md:9

原文及历史全部保留。当前共享源码/HOST/安装证据只证明明示子范围，未执行的必要条件不自动升级。

## X04.L040 · GLOBAL

**V0 / R00：可见且可操作。** 实际全国剧本打开、新 3D 路径有地面/实体、可移动镜头；纯背景不通过。此时允许旧低质资源。

04_VISUAL_ACCEPTANCE.md:40

原文及历史全部保留。当前共享源码/HOST/安装证据只证明明示子范围，未执行的必要条件不自动升级。

## X06.L007 · GLOBAL

3. **模拟器实际渲染**：明确 API、ABI、图形驱动（硬件/ANGLE/SwiftShader 等）、画质和内部尺寸，正常启动、剧本、3D Surface、手势、回合、存读档、编辑。

06_TEST_AND_PERF.md:7

原文及历史全部保留。当前共享源码/HOST/安装证据只证明明示子范围，未执行的必要条件不自动升级。

## X06.L030 · GLOBAL

应用前后台 20 次、2D/3D 切换 20 次、支持的方向/布局变化、低内存回调、读档/换剧本/编辑导入、用户退出重进、进程被系统杀后恢复。测试只有一条帧循环、Surface/engine 不泄漏、旧 worker 不回灌。

06_TEST_AND_PERF.md:30

原文及历史全部保留。当前共享源码/HOST/安装证据只证明明示子范围，未执行的必要条件不自动升级。

## H.L068 · R12

正常冷启动 → 新剧本 → 全图势力预览 → 切换/选择势力 → 确认开局 → 选城/出征 → 选军/移动/攻击 → 战报 → 下一旬 → 保存/读档。

EXECUTE_PROMPT.txt:68

真实触点在全国预览超时；势力确认、出征、移动、攻击、战报、下一旬、手动保存/读档NOT_REACHED。当前cold探针本身也不实现全部后半段。

## H.L078 · R12

用同一检查点的权威日期、控件内容、UI绘制/帧标识、3D Surface捕获、整屏原图和连续录屏交叉确认。不能仅凭 getText()、可访问性文本或 PixelCopy 的局部结果宣布屏幕正确。

EXECUTE_PROMPT.txt:78

API29独立整屏/Surface/录像交叉确认UI陈旧FAIL；控件实例ID和bounds、真实手动读档/跨月跨年完整链仍需采集。
