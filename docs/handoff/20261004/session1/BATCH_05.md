# Batch 05：原初始化、安装事件装配与字节码

目标保持 active，尚未完成正常 PC 来源新局、完整武将数值接入和新 APK 验收。本批从 d3c6ab94eed0e4777d4933592145c4222a04822b 继续；实现基点仍为 840030195e39cec3c0d352e010a3c3e614893ca2，不回拷旧核心。

用户确认只有已提供安装目录，外部覆盖保持未知。本批使用明确为空的虚拟 Documents 夹具，不把它写成实际 Windows 用户目录为空。原 Shell 路径后缀现场核实为 `Koei\San11 Tc\Expansion`；此前提问中的 `KOEI/San11PK/Expansion` 路径表述据此更正。安装目录候选与实际最终生效仍分开记录。

## 原进程边界与人物后处理

新增 StartupPlatform 在原只读文件平台上执行 CRT 线程初始化 70e5c0、CP950 初始化 709048(950)、FNINIT 后原浮点初始化 707075(1)，核实 x87 CW=0x23f。FLS、CPINFO、单线程锁、Shell Documents 和有限 NLS 字符表操作是显式平台夹具；PE 导入槽位逐一按原 EXE 核实，未知接口拒绝。原 4397d0 初始化配置 CString 对象，但没有读取那台 Windows 的用户配置或注册表。补映射原零 BSS 区域 6ee1000，使原后处理实际访问 6ee7950 能继续执行。

CP950 转换夹具仅用于原 CRT 的单字节分类表初始化，不用于替换人物名字、传记或字形真值。缺少严格标志时的 U+FFFD 行为采用明确 Vista 夹具；依照 [Microsoft MultiByteToWideChar 文档](https://learn.microsoft.com/en-us/windows/win32/api/stringapiset/nf-stringapiset-multibytetowidechar)，它不是那台 PC 的 OS 版本证明。CPINFO 来源范围读自原 CRT 的 950 表；接口结构依照 [Microsoft GetCPInfo 文档](https://learn.microsoft.com/en-us/windows/win32/api/winnls/nf-winnls-getcpinfo)。中文 CompareStringW 排序明确失败，不用 ordinal、Unicode 或猜测笔画顺序代替 Windows NLS。

每份候选各自构造原世界，Shared 和剧本在同一注册表读取。随后执行原 679cb0 安装事件管理器及直接原 493400 世界后处理；这不是完整 4937b0 wrapper 或原菜单开局。Shared 的 NLS 排序、菜单设置、事件条件与效果仍未执行。38 个原人物 getter 保留处理前后值；基础/成长/经验/伤病和两套当前缓存分别保留，活动人物缓存逐项对照原 48a110，而非 Python 公式。每份 query 后整份 3MiB 世界及 RNG 字节必须一致。

首轮缓存验证错误地将 47a600“有效”当作 47a630“活动”，失败日志保留。实际原 488430 对状态 6/8 跳过缓存刷新：Scen000 native0 有效但不活动，缓存为五个零，原能力函数为 65/74/26/33/44。最终工具保留这两组原值并列出未知，不把零缓存当零能力，不给未登场人物虚构刷新。700–799 原特殊槽位 flag 的初始化会改变活动判定；没有借此生成项目身份或扩大 666 严格身份覆盖。

## 原事件装配、正文及字节码

原 679cb0 在“安装目录＋明确空虚拟外部目录”夹具返回 1，完成 20 段装配。326 条记录来自 100/102 两份资源，段计数为 0/0/0/0/31/116/20/9/19/0/1/28/80/0/0/2/4/1/13/2，合并元数据 216908 字节。这里是原资源装配，不是 326 个开局事件执行成功。

每条原 12 字节 lazy descriptor 由 678480 读取，原 678870 形成文件路径；保留资源编号、原文件 SHA、偏移、长度。原 46dc70 逐条读回正文并与原文件精确跨度比较，326 条合计 1096968 字节。稳定事件 ID 使用文件 SHA 和偏移，原管理器顺序另列；不同来源不强行合并，也不认为夹具目录排序证明原 Windows 的加载顺序。

原 73fd70/672e90 构造字节码容器、658e40 绑定头，659ae0/659b30/659b50/659b60/659b70 读取指令，659ba0/659bd0 读取字符串。共 90110 条正文指令和 15033 条条件指令，每条原操作码与两个操作数逐字节对照其来源跨度。原 CRT709c0e 在实际线程/CP950 初始化后执行，未替换多字节比较；它与依赖 Windows NLS 的中文名称排序不同。652 个原字符串槽均为空，不能据此声称已提取剧情对白。所有 opcode、引用编号和原始字节保留，不把 operand/nativeId 直接当 Android 人物 ID。

字节码只读 getter 不执行事件条件、规则、UI、消息或效果，世界/RNG 字节保持一致。它为后续核实原事件分派、条件和开局生成部队提供实际指令依据，但不能证明载入边界无部队意味着开局无部队。

## 验证与复现

新增原启动/事件/后处理回归八项、原字节码回归三项：实际 CRT 上下文、原装配及正文、外部路径未知、活动/非活动当前缓存、特殊槽位、未知排序拒绝、PC 写入拒绝；字节码精确字段、非法 opcode 保留、越界和未知头拒绝。初始栈参数误序与缓存判定失败记录另存，不覆盖为成功。架构静态边界通过。最终两次完整来源转换、字节码转换 SHA 与计数见 BATCH_05_VALIDATION.json；只有实际完成的重复比较才记录通过。

依赖为本机 Python3.9 与 Unicorn2.1.4，导入工具不需要 Capstone。独立环境可用 `python3 -m pip install --no-cache-dir --target OUT/pc-emulate unicorn==2.1.4` 准备，不写 PC 或系统缓存；安装环境不属于源码 checkpoint。使用独立 `PYTHONPATH=tools/content:out/session1/toolchain/pc-emulate`，PC_INSTALLATION 指向提供的只读目录：

1. `inspect_pc_started_scenario.py PC --output OUT/started.json.gz`，逐份 16 候选；可用 `--source` 作单源诊断。
2. `inspect_pc_event_bytecode.py PC --input OUT/started.json.gz --output OUT/bytecode.json.gz`。
3. `test_pc_started_scenario.py`；`test_pc_event_bytecode.py` 另设 PC_STARTED_REPORT 指向对应启动报告。

本批仅修改新 Python 工具与 session1 文档，生产 1000 文件仍按 Batch03 source-text APK 守卫核对。旧的 Batch02 守卫含四份已知 Batch03 改动，不能用它反推当前生产树漂移；诊断保留，正确守卫单列。本批没有新 APK、安装或设备流程结论，不移用 Batch03 包通过为 PC 完整新局证明。未占用设备或修改设备数据。

## 仍须完成

实际菜单候选筛选/优先级、官方/MOD/自定义/备用身份；Shared 完整后处理及用户菜单配置；事件条件/效果及实际开局生成；四个未映射人物、64 字形隔离及额外槽真实身份；17 处数值/适性差异；完整逐人物字段/引用/特技效果与传记缺口；来源新局转换、同一 DTO 正常页面、独立 APK 的各剧本多势力操作/多旬/保存冷启动。旧 v33 征兵 AP10/20 的继承差异仍待显式兼容策略和旧档完整续行；v31–37 保存策略不追填、不升级。ARM 实机仍无新增证据。

最终两次16来源转换逐字节一致，压缩SHA a387d385c25c5cb28bb616d138166ebf5e23d0b4ed11016a5d2bf48f383b64f6，解压SHA e6fdefe819e28162698877677b93ba1362b3d4b420dd4bdbec573e11416a64f9；字节码压缩SHA 824a3a6f07ca51ba7a3290900accf5c6b37296f224ed6afa2a980026974d0037，解压SHA 0cf943422dd906ce259176c188cc2accc978eba50bc7ef5fb7a6d43f8c529d83。13600读取槽位、10656严格身份连接、6760活动缓存对照，38个getter共516800个返回值，其中只有有效槽位的468160个单列；这些数字不是全部有效武将还原覆盖。逐人列出本阶段调用/未调用的141属性范围，完整性均为false。1806个已登记PC安装文件共2780250717字节与原SHA全等；完整来源JSON、逐人物覆盖、失败及通过日志、生产守卫在本目录封存。
