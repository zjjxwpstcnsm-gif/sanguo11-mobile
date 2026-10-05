# PC 运行与参考采集状态

2026-10-02，已取得并检查原版实际 D3D9 帧缓冲：1650×1050 的启动页、主菜单、剧本选择页均有完整有效像素。此前 GDI 全黑是采集路径失败，不能据此推断原版实际黑屏。地图、动态效果与安卓匹配验收仍待继续；启动页成功不代表全部美术对照通过。

原安装目录保持只读。运行目录为 `out/pc-visual/pc-runtime-copy`，由 macOS APFS `cp -cR` 独立复制；没有硬链接，也没有在原目录执行游戏。前缀 `out/pc-visual/pc-runtime-prefix` 的用户目录为项目内真实目录；移除了自动生成的系统根盘及外部卷映射，只保留 c: 前缀、g: 游戏副本。

便携包来自 [Gcenx macOS Wine builds](https://github.com/Gcenx/macOS_Wine_builds/releases/tag/11.18)，文件 `wine-devel-11.18-osx64.tar.xz` 为190,974,384字节，SHA256 `aa0ea4c82e636ae7bca2076387cb0a5affa26509ad13f119ecd0d62bd7ba6f82`，与发布资产校验值核对。缓存位于忽略的 `out/toolchain/wine`，命令返回 `wine-11.18`，未安装系统工具链或修改原 PC 文件。

当前采集尝试：

- 系统窗口信息确认 Wine 的游戏窗口，但 `screencapture -l` 无法取得窗口像素。
- `tools/pc-runtime/capture_window.c` 在隔离前缀内枚举 Wine 窗口，并使用 GDI/WM_PRINT；BMP 的全部 RGB 为0，保留为失败证据，不能用于视觉结论。
- 早期 `d3d9_capture.c` 代理加载失败，保留旧失败记录。新运行显式使用 `WINEDLLOVERRIDES=d3d9=b`，原版正常加载 Wine 内置 D3D9 并创建着色器。
- `build_readback_observer.py` 构建 Win32 `readback_observer.c` / `inject_capture.c`。在私有副本进程中确认固定 EXE 校验值及基址，通过已核对的原版设备槽 `0x6ed6f74` 找到设备，复制其 vtable 并观察 Present。它会改变副本进程的 Present 函数入口，但原始 Present 参数、矩阵、材质、着色器、资源、游戏规则、随机数和存档没有被替换。
- 观察工具在原版渲染线程调用 D3D9 GetBackBuffer →（多采样时独立解析面）→ GetRenderTargetData → 只读 LockRect，输出原版 32 位像素。读取会增加 GPU 同步及文件写入开销；采集后的帧率不能直接作为未插桩 PC 性能。命令文件请求下一实际帧，只保存截图不会替代动态录像验收。
- 首张有效启动页 `out/pc-visual/v152/pc-reference/source-gpu-96a037fd7b08.png`，BMP SHA256 `96a037fd7b08a2c8970e6b4e01eec64f8d341516e14751deb6236ede0d6fb22c`，1,731,471 个非黑 RGB 像素。真实图像已检查，不再只有 API 返回成功。
- `ui_command.c` 只在同一私有前缀的原版窗口发送正常鼠标/键盘输入；坐标必须根据已检查截图选择并检查客户区边界。`collect_reference.py` 保存每次输入、实际帧、校验和和观察日志。已通过正常菜单点击进入剧本选择；没有修改场景数据或游戏内存来伪造事件。
- WineDbg 调试连接出现 WoW64 异常；没有从中推导游戏美术结论。运行日志保存在 `out/pc-visual/pc-runtime-*.log/txt`。
- 第一次 v134 安卓录制期间帧提交停滞；停止并行 Wine 后重新提交恢复。这是宿主环境中的关联证据，原因尚未隔离，不能声称安卓真机存在相同问题。

安卓录制和安装验证期间停止 Wine。继续通过正常游戏操作采集同地点、季节、镜头和事件的实际截图/录像；当前成功证据是 Wine 中运行提供的原版 EXE，不是原生 Windows 系统验收，目前也没有 ARM 真机性能证据。


实测镜头证据（2026-10-02）：207年9月1日秋季，新野；正常滚轮缩放4份快照保存至 `app/src/test/fixtures/native-v153`，来源/摘要见 `camera-live-source.json`。SENV原5a2530经renderer provider `0x32602b0`调用相机访问器；加载代码414fd0实读为 `lea eax,[ecx+40]; ret`，相机地址 `0x32602f0`。`read_camera.c`仅OpenProcess查询/ReadProcessMemory，未远程执行访问器或改写数据。截图前后512字节全等。30°垂直视场，16近裁面及目标高度，远面/眼点距离比约1.7。暂只接受这些实测视角下的数值镜头合同；其他地点目标高度和PC旋转状态还应补充运行证据。

`record_reference.py` 保存原版Present编号、单调时钟请求/完成时间与逐帧BMP；`encode_reference.swift`将其原RGB及实际间隔编码H.264，不插值生成过渡帧。首段 `out/pc-visual/v152/pc-reference/xinye-autumn-water-01.mp4` 3,000,340字节，SHA256 `a0c046419b73cebd01469f4c54f7c0849d0b7f9b68b066b7e9f01abd33658662`，29帧，movie15.382秒。所有帧摘要不同，首末水域12224像素差>8；不能仅由此宣称整个水面动画/时序还原正确。截图、视频、相机来源证据已具备，安卓对应像素与快速动态还须继续验收。


2026-10-02 v153 实际PC鼠标定位缺陷：SetCursorPos目标126,147时返回1，但GetCursorPos为1048,729，前景PID仍为私有副本游戏。单独WM_MOUSEMOVE/LBUTTON消息也不能修复地图自行轮询的错误位置。失败的菜单/保存操作截图保留，尚无原版保存成功证据。新mouse_input.c只在私有进程中适配原EXE的GetCursorPos/GetAsyncKeyState/GetKeyState三个OS导入槽，输入仍由原版UI循环处理；不写游戏状态/RNG/场景/相机/资源。按钮按下1秒自动释放；send_mouse_input.py结合标准Windows鼠标消息及轮询输入，ack仅证明输入已接收，不能证明目标菜单动作成功。此适配属于采集环境改动，必须随参考画面记录。readback_observer新增按请求只读观察原DrawIndexedPrimitive的256组顶点常量、3个固定功能矩阵及可读取顶点缓冲前512字节，原绘制参数/状态保持原样；读取失败与同步开销须保留，不能作为原版性能证据。


输入适配初步实际证据：仅改变虚拟按钮轮询并发送WM鼠标消息可退出标题但未激活新游戏菜单。保持虚拟光标、按钮轮询为0并通过原mouse_event产生实际鼠标按下/松开后，原版新游戏动画及207年9月剧本势力选择成功，截图source-menu-real-button/source-scenario-207保留。两种路径分开记录，不能将仅输入ack当作已完成事件；后续游戏内按钮和连续操作仍须画面核对。source-ui-input-update.txt记录原版435ac0/435b20及43d7d4输入路径，只读反汇编，不修改游戏控制器。


v158全屏人物选择调查增加`tools/pc-runtime/read_portraits.c`，只对当前隔离Wine游戏进程执行两份一致的ReadProcessMemory读取，读取原480320使用的2400×4视觉描述表0x6fae8b8；不写人物、贴图、规则或随机状态。`build_readback_observer.py --only read_portraits`可复现3072B helper，摘要dc7c6a1448eb49cc07c2e2c24c168c26a86f3c39ba6a23a8389c9a3bc55232df。只能在已加载真实PC场景后于私有前缀运行`g:\pc-read-portraits.exe`，输出到项目私有副本`pc-portrait-descriptors.bin`。当前仅编译，尚未取得实际表快照；不得把源480320默认39或内容库sourceId假定为整合版实际人物图映射。指令/摘要见portrait-lookup-source-working.json。

v158动态演出观察器增加显式`pc-texture-512.request`范围选择：须与`pc-texture.request`、`pc-draw.request`一起提交，最多64张managed ARGB原贴图、512平方；默认仍为256，完整帧后删除两个纹理请求标记。header保留实际范围，inspect_pc_live_textures.py兼容256/512，`--presentations`增加原124及369—715完整RGB候选核对；组合子矩形不等于完整图，不能由无匹配推断选择编号。新增工具inspect_pc_portrait_snapshot.py严格核对9632B/PTR1/2400×4/原EXE摘要，只导出原480320有符号slot及实际permuted模板；不绑定officers.tsv人物。
