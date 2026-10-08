# 当前r27普通SEARCH→原舌战验收（r28本批完成）

当前生产冻结r27完整源SHA4b6a5009…8ad3，APK d2e3aa4b…bd58；只更新旧测试runner显式源选参数，独立重新构建注册SessionBDebateInstrumentation。生产无改动，规则/API/runtime/数据/SourceRoster/资源/JNI必须全部同r27 SHA。禁止沿用旧38/31APK成绩。

正常菜单Source0分别来源势力2/29（名称以该源新局DTO为准），通过现有原搜索→发现→招揽→可选原舌战。测试普通预览/返回/取消、拒绝招揽、拒绝可选舌战、合法人控牌/再考/怒气协议、自然胜败、全模型/两RNG保存与冷续行、正常战役终局一次性、全旬和全World读取。新局随机种子由实际菜单给出，不设置资源、人物、位置、结果，不注入VM/Host构造存档。模拟玩家选择的独立测试Random不调用规则RNG。

每轮安装前完整备份15内部/772外部保存/库/偏好，最终全SHA/路径集合恢复，5582先核空闲，不抢A5554。原认输、外交原回调、全部16源有效人物/事件与旧31–39/custom/ARM全矩阵保持未知。

## 本轮失败保留

v1真实安装游戏 d2e3aa4b8780634842555b1ead7969251a729b091ccd6c6de02909a5d518bd58，测试 ecd3c48e9197bc3c15abe0db38cdb5e347fe5b12e08ed69082857a282fba1257。普通发现/取消/拒绝招揽/整旬/存读通过，第二次菜单新局后的发现断言 `native first discovery dialogue no gold/AP/merit/action yet` 失败。v1没有记录逐资金数值，不能将该失败直接归因于生产扣费。失败截图原发现页显示AP51、人物吳國太/呂岱、允许尝试/拒绝招揽。

v1已终止并完成15内部/772外部全路径及SHA恢复，无新增用户文件残留。v2测试新增权威新局sessionId/generation变化及turn0/无遗留交涉等待，并在每次发现前后记录全存档与金/AP/功绩/行动值；保持原断言，没有改生产规则或注入世界。v2初次构建相对init路径重复导致构建失败，记录build-v2；改绝对路径build-v3成功（76任务）。尚未宣称v2实际通过。

## v2实际进行记录

游戏APK与r27原SHA完全一致，测试APK ebccdc6881cc24fac1cccd7682cecc968147a08854071ab282b3d4e3b89c5562。新局明确选择来源0/初级/战死多/史实寿命，随后等待sessionId或generation变化、turn0、sourceId及无遗留交涉；不改规则RNG/人物/结果。

实际发现阶段来源势力2：actor10198/native198吳國太，city20008，金3200→3200、AP51→51、功绩0→0、acted=false。来源势力29：actor10430/native430張邈，city20015，金5200→5200、AP65→65、功绩6000→6000、acted=false。全部来自正常菜单及源identity连接。

三场普通SEARCH产生自然结果：trial0 張邈落败/登用失败；trial1 吳國太落败/登用失败；trial2 張邈获胜/楊奉接受登用。原经验/功绩/技巧/伤病回调实际执行；终局搜索AP20只扣一次、无额外金费。每场中途及终局均普通槽3保存/读取，并逐整旬验证；冷进程续行及最终用户文件恢复尚在进行，因此此时不记整轮通过。


## 完成本批与边界

`out/session-b/search-debate-current-apk-acceptance-v2/results.json` 正常 instrumentationPassed=true、coldPassed=true、passed=true；5582 API29/x86_64真实装包/readbackSHA。冷进程自动续行及手动槽3读取均逐原字节一致，并完成合法人控、终局及下一旬。15内部/772外部文件全SHA/路径集合恢复，无测试文件残留，进程已停止、设备锁已释放。没有生产ARM验收。

主机直接读取本轮实际mid/finished存档与实际人控日志，重放发现→拒绝招揽、三场人控→原终局→完整旬；每步整World/native模型/全部RNG往返相同，重复旧ContestCommand拒绝并逐全存档字节不变。冷续行依据实际冷日志，从原mid到归档的真实auto.sg11重放，再完整旬，与真实冷终局保存全World/全部RNG一致。日志：actual-search-debate-replay-v4.log、actual-search-debate-cold-replay-v1.log。原始编译v1接口错误保留；v2仅编译；v3/v4实际通过，不能将编译当流程通过。

跨Android/JDK四对原始存档：仅嵌入战报GZIP OS标记 Android0/JDK255 一字节及外层CRC不同；两侧CRC有效，解压完整战报字节相同，其余全部载荷原字节完全相同。审计工具session_b_audit_search_save_boundary.py，receipt search-debate-current-raw-boundary.json SHA0087daf2930f8e3d4b391c2d9b92d377eb33ceff16fd06d4ab50b218b1b84295。没有删字段或忽略RNG/扩展；Android自身的保存/读取/冷启动仍要求原字节一致。

共同main ef413be3653820dd6449ba7f02aa60bed5b26ef5；最新完整main 0e7b9bc2df90249a50851baeda58c7d183ea6059仍未回退。生产依旧r27 cdd7f949f076dd7106aaefeff7fcbf86314f05cf，core/API/runtime三JAR与游戏APK均同r27原SHA；168固定输入、原4+新增2JNI守卫继承且APK逐SHA相同。只交付两个测试输入路径的确切r27前像→本轮后像补丁，不提交整个长期dirty测试runner，不合A或另一会话WIP。根MainActivity/MapHost等A文件、桥序列化、Unity、4JNI均未编辑。

旧根HEAD52315bf070e5d29acb8f509230c5228e47c8d6ef，609dirty状态字节31896/SHA618527c6227f137aa9371cd15949d9659ee57db29ae5c146fc4b29254da89dd0复核一致。PC EXE及Media/san11pkres.bin继续原SHA只读。首次冻结脚本参数与acceptance字典重名导致失败（freeze-v1），部分目录保留且不可当交付；修复参数后另建frozen-r28-search-v2。

该批为Source0两势力的普通搜索发现分支验收，不等于全部16源/全部有效武将/原全部特殊准入。原宝物/未发现分支、完整关系准入/优先级、舌战中途认输和外交仍未知；放弃可选舌战不等于中途认输。独立工程“舌战登用”的100金10AP不因本批变成原真值。全部旧31–39/custom/ARM/Unity地图黄金与剩余必要core失败仍未关闭，整体goal继续active。

## 冻结交付

确切目录：`/Users/paopao/.codex/worktrees/f55b/sanguo11-mobile/out/session-b/native-opening-combined61-frozen-r28-search-v2`。

- 游戏APK SHA d2e3aa4b8780634842555b1ead7969251a729b091ccd6c6de02909a5d518bd58
- 测试APK SHA ebccdc6881cc24fac1cccd7682cecc968147a08854071ab282b3d4e3b89c5562
- 完整构建源archive SHA 1f3683daaae0d3bbbfb2a882a2b4bc465e0cc90cffa3b5018d16216c42d79bc0（11355输入，逐SHA/长度/mode/归档读回核验）
- 原r27→本批两个测试输入补丁SHA 0195bdc1b7d7e9181a5bf45ba21922893a578d0d2bb91c291a9e7ccc0b7ab414；逐前像/后像在increment-manifest.json，git apply --check/apply及后SHA核验均通过。

生产变化为空，无A/WIP冲突。APK保留168固定输入和6JNI原字节，源守卫6172条均与r27相同。新增工具/验收文档/实际流程证据另放delivery-evidence-tools.tar.gz，不伪装为编译输入。首次冻结部分目录frozen-r28-search不作交付。
