# Session A 当前进度

基点0e7b9bc2，隔离完整输入10,714文件/794,435,386字节，全部逐SHA一致，168固定输入与4JNI全等。原目录当前1305 dirty（交接609是旧快照），仅实时守卫无写入。

首批黑字源码修复：原有限33色耗尽返回0→确定性不透明后备色；地图填色与文字分离，标签4.5:1独立可读并使用不透明牌。UiTheme提供禁用/富文本文字/树与弹窗机制；AppTheme默认正文/次要/提示色。16安装源+9工程源实编译测试及2/33/47/128/1024/4096槽检查13,273项通过；原16源13源57个有效零色组合复现，修复后所有47槽零透明色。是主机证据；SDK35主题Java与aapt2新颜色/样式编译通过，新APK实际像素/正常界面待安装。

OOM：正在用实际生产地形路径执行384MiB桌面堆诊断，仅CPU证据，不等价截图手机根因。真实APK/设备Java/native/GPU峰值和分配栈未取得。现有释放与有界TerrainSurface已确认，不凭旧推断归因。

设备5554/API29/x86_64、5582/API待查；无ARM ADB。5554存档/偏好约416KiB，但外部用户目录2,679,460KiB，完整备份与新包构建空间不足；尚未安装、未清数据、未改设备用户文件。可用空间约336MiB，需要可用空间/外置盘后继续实际安装。

## 第二批：地形驻留和生命期

实际PC地形共享同位置顶点、只上传原unlit shaders读取的UV0（未读取UV1声明别名保留）、16种上限的同字节索引模板复用；保留源全部三角面/水/网格。旧窗口先筛为目标可见覆盖，不在近景任务捕获全图覆盖。MapHost.release清空detachedWorld/ground/投影缓存/回调；Filament释放原有CPU/GPU。增加实际Java堆/限制/nativeHeapAllocated及主机数日志，不将GPU估计称峰值。

384MiB桌面生产路径旧版全图→近景→全图OOM在Arrays.copyOf/SurfaceBuilder.mesh；修复后4次往返全Save/RNG不变。全图唯一CPU数组177,286,368→60,182,568B（约66%减少）。仅CPU桌面，真实截图设备栈、native/GPU/Android长生命周期仍待。16个PC/旧图窗口原indexed xyz/UV0/三角顺序、水、网格与旧图全部属性SHA相同；实际原PC地图622,065项mesh/picking/save检查通过。SDK35实际353个生产Java+AAPT2资源编译和架构静态边界通过。未做dex/R8/签名/完整APK构建和安装，不称手机OOM已关闭。

## 第三批：火状态信息、最新身份和原caller证据

PC地图直接消费真实FireState位置/剩余旬显示持久信息标记，全部可见格有标记、近景最多32文字，暂停/低画质/缩放/读档的实际效果仍待新包。保留旧工程火粒子禁用，没有将标记称为原火焰恢复。完成原EXE命名火計/滅火静态caller/原四参数opcode22与部分voice原profile70取证；真实持久火控制器/发言角色/准入/材质生命周期仍未知。

最新16来源实际保存媒体投影10720身份与manifest精确连接、未知0，四字形当前名孔伷/司馬伷/朱儁/劉璝准确native/recordSHA；695个独立正常头像资产被本轮source当前年解析，全部2892 PNG原SHA核对。正常Android目录/所有caller不能据此宣称通过。动态原年龄查找再次160806检查/32160向量/62selector通过。全source-map静态79条件库存已记录；原声producer/长曲与-22仍缺。

基线384MiB桌面复现重复轮OOM栈在TerrainMaterialField.attach数组申请，而首轮在SurfaceBuilder.mesh/Arrays.copyOf，两处栈均保留在Git批次证据，属于同大数组驻留峰值风险；不将它们当用户ARM栈。SDK35生产353 Java/资源编译是部分构建证据，没有可安装新APK；安装/完整用户2.6GiB备份及ARM仍待空间/设备。

最终只读保护复核：原目录4301受守卫路径/HEAD52315bf0/dirtystatus1305完全不变，PC EXE SHA全等。第三批所有生产增量A拥有，699核心/B/Unity/桥接/JNI/配置路径与基点全等，168固定输入全等，无B WIP合入。小尺寸PC视窗的scratch容量按含外部32格的完整16×16块上限保留；正常200源输出容量相同。主题保留选中/焦点语义色，富文本只改变Span，不替换Editable字符/光标或触发重复TextWatcher。

## 第四批：原格子火 factory 与停止句柄

供给EXE的59fea0证明持续格子火模板13/资源138，真实火状态非零创建、清零413470 stop，使用原417880世界坐标。两次独立原controller/finalquad/material执行字节一致，纹理2/11/13与归档逐原像素相等，原混合1/5/2与1/5/6确认；真实413510/414670/413d20/413770/413470构造和停止两个实例通过独立VM，最终无绘制、无规则RNG调用。完全PC启动/GPU/正常Android不在证据范围。

冻结worker的8-template/126-SEFF协议没有动态火命令，当前map材质拒绝1/5/2，须依NATIVE_FIRE_SERIAL_CONTRACT.md最终串行扩展。四JNI和旧scene未改，状态标记不扩大为原火恢复。

存储优化只对72个同字节大输入作原子APFS clone，SHA未变，未获得足够空间，停止无收益重复。完整备份候选读取核验：旧3266文件均与设备相同，但当前多531文件，不能将旧archive当完整本轮备份。RAM16GiB约1.4GiB free、swap约6.0/6.4GiB已用，不建立大RAM构建卷。磁盘仍约300MiB，新APK与完整用户备份尚待空间。

## 第三个连续goal轮：受阻审计

main仍ef413be3，审计后继源仍0e7b9bc2；本分支四批到0c007271，工作区复核干净。仅5554/5582两台x86_64，无ARM，无可写外置卷，无B冻结交付。磁盘约362MiB，同一完整备份与新APK实装阻断未解除。完整目标未完成，具体门槛与恢复条件见BLOCKED.md及BLOCKED_AUDIT.json。

## 用户解除空间阻断后的实装批次（进行中）

2026-10-06用户要求继续；主机约190GiB可用，旧阻断已解除。独立Gradle缓存重新建立，普通非debuggable/R8包5431de8d实际构建/安装5554，APK SHA9bc6bff86e19be0309cccdc9ca5b5e52e00d1b3b1748ce7734c9645d6cf2cdaa；168资源及4JNI全等。完整9内部/3797外部文件重新备份、最终恢复逐SHA全等。正常菜单→Source0势力预览实际原地图像素与Save/RNG/StateToken纯预览检查通过，但取消后立即观察释放的验收断言失败，未进入缩放循环，不记整轮通过。实际Java堆限制512MiB（不是用户截图384MiB），原始日志峰值308260456B、native200545280B为局部且采样侵入的观察，不是ARM根因闭合。详情见INSTALLED_AUDIT.json。

630a4ce7将释放观察放在主线程等待完成，采样改为dumpsys meminfo --local避免向进程请求GC。接B精确接口请求，在47326188适配普通军事行军previewMove，保留明确自动攻击/接近、进驻、运输改道和routeMove保存；跨界正常入城格仍待B规则冻结组合。5个完成主题依赖在THEME_FROZEN.json提供前后SHA及独立只读导出，B在其16页消费，不复制A WIP。

47326188普通APK已构建，SHA7b65eee5e9132fd99192de3eb2862b5bba4fb3f2257695467df99784f0041ca8，签名/168资源/4JNI通过。第二次安装在PackageInstaller阶段失败（设备历史finalStatus=-4，/data6GiB仅余约0.9GiB）；未把这个包标成已实装。第二次完整备份与失败后9/3797恢复SHA全等。主机空间充足，改为复制原A独占5554的完整AVD并只扩容独立副本；原AVD/用户库/资源保留，具体新设备来源和SHA见后继AVD_CLONE.json。5582不操作。当前仍无ARM、无B完成冻结增量，完整目标未完成。

## Run05: 16-source installed flow; 384MiB risk remains open

Normal non-debuggable fd9f455a APK daecedbb0436e5eebd5423b4b607d7f8926b544ce411409edb4448c8c6719d1e installed with equal readbackSHA. All16 real source menus, new games, first/last enabled faction selections, real full-near/pan and officer directories completed;51 actual full-near crossings. Home and actual orientation menu kept complete Save/RNG/StateToken. Final save assertion failed: no overall PASS. Run06 transport timeout before instrumentation, no operation credit. All original9 internal/3797 external files restored exactSHA each round.

411 sampled Java maxima402559744B (383.91MiB) vs max402653184B (384MiB), only93440B free at source8 zoom. No observed OOM, but blocking Alloc GCs: memory closure requires allocation/live-object evidence. Native sampled max284132928B; totalPss624606KiB. These maxima are not simultaneous/additive; graphicsPss0 is not measured GPU VRAM. Renderer values remain estimates. INSTALLED_16_SOURCE_MATRIX.json records exact limits.

5554 is the full inherited private expanded AVD; original retained/stopped. API29/x86_64/hostGPU/384MiB is not ARM acceptance. B fees/construction still candidate; no WIP or results incorporated into A APK.

Run07 supplementary normalSource15 / cancel / preview / newgame / actualfull-near / Home / orientation / save3 / load / Activity exit-reopen PASS314. Slot3 and capturedWorld SHA c615c8999a730ccc36c40201a2135986b0ad7510bfb535b91eec3d68f4ac8799. SamegameAPK daecedbb0436e5eebd5423b4b607d7f8926b544ce411409edb4448c8c6719d1e Full exactSHA is in INSTALLED_16_SOURCE_MATRIX.json (do not use abbreviated prose).9/3797 restored final exactSHA. Activity reopen is same-process; true fresh-process and ARM remain pending. Heap08 diagnostic planned on unchanged installed game bytes, separate testAPK520fb407...; not acceptance memory sampling.

Heap08 actual Source7/8 normal menus/newgame/gestures/storage flow completed under explicit heap-diagnostic mode;9/3797 original files restored exactSHA, only4 changed internal originals rewritten from complete guarded archives, external0 originals rewritten after every-file hashes. Actual199444332B post-GC dump host/device SHA695f477ae6fcedd20d5ffc1e5a29f67ed8f1f4d9fda937fc1ada58cd3efc4819. SceneMesh.vertices62877724B/418 arrays dominates large holders; DirectByteBuffer.MemoryRef6475188B/16 arrays (same arrays also hb, not additive). One FilamentMapView,2 MapHost (preview release fields null proven),4 World including observed/decode copies. No sole direct-buffer root or accumulating-renderer leak claimed. Allocation stacks/userARM still pending.
Cold-process comparison prepared with testfd4c78e6f9bbc2d5a0fdb96c36e109fcb230d63e506b901695447edaa2a42233; uses only actual autosave SHA then OS force-stop and normal MainActivity startup, compares complete capturedSave/RNG to expectedSHA, no state injection. Not yet installed/accepted.

Cold10 corrected actual PID observation completed on samegame daecedbb...: normalSource15 workflow314 checks + distinct newprocess79 checks PASS. PID17291->24237; normalautosave and newruntime completeSave/RNG SHA92dd0ae7ee541c6348a07e0f3b53c4ba283d8fb6c5cd4d10721bbc135878373e. ExpectedSHA only, no state/snapshot injection. Original9/3797 final SHAequal; lock released. Final source/original/core/API/runtime/Unity/resource168/JNI4 guards all equal, no conflicts/BWIP. Full goal still incomplete:ARM/Bfrozencombination/originalfire/native protocol/media producer and full gameplay matrix.
