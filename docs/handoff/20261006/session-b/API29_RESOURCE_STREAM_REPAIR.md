# 本版实际Android29资源读取阻断

5582组合61-r2已安装，游戏APK逐SHA9b9d4cd5…3e119f、测试包8d013054…68ee。实际设备SDK29；首新局选项目录查询抛 `NoSuchMethodError: No virtual method readNBytes(I)[B in class java.io.InputStream`，trace精确在PcSourceOpeningOptions.source→sourceFlag→PcOpeningOptionsQuery.newGame→GameSession.previewNewSourceOptions。测试在metadata读取时失败，尚未进入来源选择页面，不能称普通菜单或单挑通过；A正常来源选择回调使用同一查询，该加载路径因此必须修复。

本次安装前备份15内部/772外部文件。失败后结束进程、归档现场、恢复全部原文件并读回SHA：两个域mismatches均空、preservedAdditionalFiles均空。只移除本次已归档且liveSHA一致的自身PCM临时文件和测试evidence；原用户资源没有删除。完整报告 `out/session-b/native-duel-apk61-r2-acceptance-v1/results.json`，失败完整trace与现场保存/PNG在对应evidence。

全局core/game-api/game-runtime检索只有PcSourceOpeningOptions与PcDuelMenuOptions两处InputStream.readNBytes；A闭合app没有同类stream新API（两处Files.readAllBytes不是该API）。没有盲改A代码或整个Java库策略。

新增包内部PcResourceBytes.readUpTo，使用旧平台已有InputStream.read(byte[],off,len)/read/ByteArrayOutputStream实现原读取至上限或EOF，保留caller关闭所有权；碎片/零进度stream不会死循环。仅替换两个调用，原count16385/4097、大小拒绝、原文件SHA、字段/标志/选项域/解码/加载优先级完整不变。没有改资源、save schema、规则数值、SourceFlags、默认值、StateToken或任一RNG；旧策略不升级/不补填。缺源/坏SHA/超限仍拒绝。

PcResourceBytesTest覆盖实际两份原资源碎片读取完全一致、4096/4097/16384/16385界限、零长度/EOF/剩余字节不多读、IO异常和流所有权；APK实际SDK29页面与完整流程必须重新跑，JDK通过不能替代该证据。

必要后继B增量逐before-after SHA见 `api29-stream-repair-guards.json`。组合stage仍完整继承A227，生产guard新增helper变为6165。r1/r2源码/产物/失败报告不覆盖；r3新APK独立构建，core JAR仅准新增PcResourceBytes.class与改变两原加载类，所有其它B JAR条目必须等candidate60；资源168与原4+A2JNI继续字节守卫。保存/RNG与旧format5/39/API/session/bridge重新检查后再备份实装。本项只是实证阻断修复，不宣布完整16来源/人物/事件/单挑舌战/旧档矩阵/ARM/U01已完成。

## 后继SDK29列表终结方法

r3实际APK SHA0a7dd54f…140f3安装并核SHA成功，InputStream阶段已通过，随后同目录previewGroups出现NoSuchMethodError Stream.toList。r3也未进入来源选择页面。工具再次恢复15/772原SHA、mismatch/额外文件空。全局仅5直接调用：core PcDuelCampaign.Facts的disposition、runtime PcOpeningOptionsQuery两处choices/ContestQuery的继承候选、自己的NativeDuel Android test enemies；其它已是Collectors.toList，A app无直接调用。

逐调用改为现有PcDebateCampaign已经使用的collectingAndThen(Collectors.toList(),Collections::unmodifiableList)，保持encounter order、null允许、不可修改，不把替代可变List当Stream.toList同义。未改map/filter/sort谓词、战役规则或任一RNG/Save。确切5调用/4路径before-after见api29-stream-list-guards.json；r4核JAR仅允许新增资源helper、两资源加载类、PcDuelCampaign$Facts和两runtime query变动，GameApi及其它所有字节仍等候选。不会通过放宽ABI/minSdk、旧系统排除或工程规则替代绕过正常玩法验收。

后继r4实际普通菜单/选择/取消、Source0明确新局、正常native503携金出征/一次扣账、完整旬/3D行军全部pointer通过，两API阻断已实际跨过。导航测试超时尚无单挑结论，另有A已完成PcEffectProcess新派生cache，按before absent/归档/live/APKasset全SHA精确证实后仅删除自身cache，原15/772整个路径集合/字节恢复；详原r4报告加authored-source-cache-recovery.json。r5仅测试导航触控重试和守卫工具后继，生产同r4；任何旧World/数值/RNG不改变。


## 普通相邻单挑候选页第三处实证兼容阻断

r11精确普通manual3读取真实18旬保存、保留同部队16/原损失/全部World与RNG后，普通nativeDuel准入actor16与target25相邻且duelError为空，候选列表实际throw NoSuchMethodError Collectors.toUnmodifiableList；完整15/772原文件SHA/pathset恢复。全B production检索仅core Contests.nativeDuelCandidates一处此API。仅替换收集器为旧平台collectingAndThen(toList, finisher)，显式逐项requireNonNull后unmodifiableList，保持顺序、值、不可修改与null拒绝，未改map/chance/原准入/费用/规则/Save/RNG。实际18旬before/fresh-after Host候选稳定10503裴元紹chance100、不可clear/set、全World-allRNG-StateToken/原文件纯性输出逐字节相同。Guard记录新增api29-native-candidate-collector-guards.json；JAR白名单只增加允许Contests.class该必要兼容差异，仍须新独立APK实际候选/人控/终局/cold/设备restore。不宣称Host等价证明Android已通过。新probe应在runtime/test读取GameSession，初始临时core/test已在stage/build前纠正移位，无依赖反向变更。
