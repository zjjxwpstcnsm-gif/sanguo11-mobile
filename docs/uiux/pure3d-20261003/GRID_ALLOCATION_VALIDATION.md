# 格线分配改造：尚未计入安卓通过

冻结组合3ed13e92的cold3D记录：真实新像素11780ms，初始化分段714.30ms。
地形工作线程10629.32ms/CPU2590.03ms，同期进程GC计时增加8665ms、进程分配272564104字节。
重建对应10351.29ms/CPU2166.26ms、GC8406ms、分配273913768字节。
GC与分配统计属于整个进程，不能全部归因于该工作线程或某个方法。

SceneMesh.grid仍以List<Float>/List<Integer>及varargs装箱输出每条格线。
本候选仅改为有上界的原生数组、在同一CPU任务内复用scratch；发布的顶点与索引均为独立数组。
顺序、宽度、颜色计算、岸线投影、坐标、法线准备和landIndexCount=-1均保留。
不修改地形、通行、PC资源、规则、RNG或存档；未降低网格精度或取消格线。

保留旧装箱算法作独立参照：39组源地图/方格/错列地图、三LOD、海岸/难所/边缘/空块，
并在同一实际批次构建多个格线块后核对其未被scratch覆盖。
4869930次核对通过；位置/颜色每个float原始位、索引顺序、边界、完整存档/RNG全等。
JVM当前线程配对分配计量：旧146428080B、新47936336B。
该数字包含独立grid调用的scratch分配，未代表ART、GPU、实际首帧或持续内存改善。
既有PC地图553985与海岸拼接1491927检查均通过。

当前完整组合回归仍使用未改网格的3ed13e92冻结包。不能把其通过记入本候选。
下一步在独立组合候选中只叠加此改造，实装测量ART分配、GC、首帧，并覆盖纯3D玩法、格线切换、
后台/存取/退出、真实地块只读和多回合；原包结果和慢样本均保留。

已编译独立GridAllocationInstrumentation，仅验收类进入其DEX，生产SceneMesh与core仍来自目标APK。
两种AB/BA顺序都先预热，比较旧装箱参照和实际生产方法，记录墙钟/当前线程CPU、进程分配、
GC总时间与blocking-GC时间；不将进程GC总时间解释为主线程暂停时长。
GC单位依据Android Debug接口定义：
https://android.googlesource.com/platform/frameworks/base/+/71bcd2aa5cefa767e76bc3117ed8534b67de21bc%5E2..71bcd2aa5cefa767e76bc3117ed8534b67de21bc/
基线探针及候选探针需在当前独占队列结束后分别实装；此处仅编译完成，未标记ART通过。

## 新组合包已完成局部ART实装

组合源码b6f016214ec03b13ab02e456b0967cd5d4536b98，只叠加SceneMesh格线改造。
主包f081dee4eb224e41f418bb58926eb6406a4d9706215802ec449f11c24d6b0216，
测试包540a5ed5d42f1855f0c8da9645fbbf4dfca5236e94e2c559b1f7e5b8a2f9411b。
ARM64构建c91200f3ef26c43c948ae8bf1db5fbc94f324e5ea8a099b934a4413fd003fdc7，未实装。
两ABI647资源全等，521保护输入和4native输入全等。

新包ART探针2376247检查/1.95秒通过，安装前原文件逐字节恢复。
同一进程AB/BA预热后的18组结果：实际生产36052992B/89.66ms/CPU85.13ms，
旧装箱参照134116720B/408.51ms/CPU378.63ms。两路本次进程GC与blocking-GC增量均0。
这证明该构建方法的局部分配改善，不代表整图首帧、GPU呈现、后台返回或长期无泄漏。
旧目标包独立测量结果和旧慢样本另存，不把跨运行宿主差异当因果。

同包19流程、24回合、全部原始演出、编辑器、PCM、海岸与城市边界的完整实装队列已启动，
当前尚未结束，不将旧3ed包结果算作新f081包通过。

新f081冷地图专项20.98秒通过：真实初次提交3396ms/新像素3530ms，重建2714ms/2730ms。
此前同输入3ed专项新像素11780/11560ms，两次运行宿主调度不同，保留全部样本，未作严格整体因果对照。
新工作线程首次地形准备1803.38ms/CPU601.78ms，重建1221.31ms/CPU422.61ms；
同期进程GC503/809ms、进程分配2458256/65334032B。这些进程计量包含并发分配/GC和缓存影响。
分配改善的直接证据是上述同进程旧/新方法AB/BA比较及逐位等值，不用最快冷帧代替完整性能验收。
新包冷地图、暴击音效、开局已完成并原文件恢复，完整后续仍在执行。

Completed f081 queue: 19 regular flows (1478), 24 turns (342), 15 source presentations (1946), editor (61), actual PCM/audio (47), coast (91), city (70): 4035 UI checks including repeated waits, all original files restored. ART 2376247 is counted separately. Full architecture remains FAIL on unchanged old-scenario baseline; see CONTRACT. Earlier running statements above are historical.
