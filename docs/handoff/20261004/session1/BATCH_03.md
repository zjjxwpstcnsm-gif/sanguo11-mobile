# Batch 03：原字/传记、逐人物字段台账与实装保存

实现基点8400301，完整继承提交0a6fb12，前批完成提交2910997516cec8aad4d0564688f6090e944eddc8。工作目录仍为独立scenario-officer-restoration工作树；没有复制旧核心覆盖当前代码，没有修改原目录、共同台账、头像、音频、AndroidGameBridge、Unity或3D文件。

## 实际交付与边界

正常新局势力配置增加明确的“人物文字资料”来源选择，16个PC文件分开列出。只连接字、原消息传记与来源身份；本局数值、人物身份/分布、开局事件仍来自原工程剧本。未选择不增加来源，旧档不追填。此项不是16个PC剧本完整开局转换，现有9个工程包仍不能称官方复刻。

PcOfficerSources严格连接来源身份及既有已批准名字/别名；PcOfficerInfo把选定文字及officerId/nativeId/sourceVariant/文件与记录SHA写入现有SaveExtensions的pc-officer-source-v1命名空间。SaveCodec未修改，v31–37数值/策略没有升级。查询只读保存的资料，不重新读当前目录；未来未知格式保留原字节并显示缺口，名字身份不符不连接另一人的传记。列表字检索、详情文字和正常保存都消费同一权威OfficerSnapshot。头像接口保持原样。

严格资料目录10656条身份核实来源记录，每源666人，其中每源354个非空字；空字记录为原资源未记，不补造。正常190工程新局因名字守卫等实际连接652人；184/群英包666，194/200/207分别619/565/520，中原/荆襄沙盘180/120。未连接的人物继续显示未记录来源，不能把目录总数当实际页面覆盖率。

原字段台账逐源13600个人物槽，原构造/serializer和73ca80/4c8720属性定义与读取器执行。每人列来源字段、载入边界字段、未执行属性、未知与完整标记；complete一律false。714560次值读取有独立来源字节，373440次仅为载入边界。体力等Getter共享的status字节100只证明有效性门禁，不证明返回值已序列化；已独立分开value/validity来源偏移。经验余数、当前能力缓存、年龄、完整开局处理仍不能从默认值推导。

## 原文取证

姓名/字实际getter为48e630/48e680/48e6d0/4905b0；原48aa50..48aa7b仅执行特殊槽标记前缀，不冒充完整postload。47a600对象有效性与47a630活动性分开：未登场人物仍可以有原姓名和字。原489690读人物的service据点名称，不是人物姓名；先前domains台账的该getter注释不能用作君主名字语义证明。旧共同报告未改，本目录记录纠正，force+4语义仍需实际消费者闭合。

原武将传记函数48e850与消息引擎498a90/4976a0/4988e0分开核实；属性93“說明”实际是特技说明。原S11MSG02解压后2428条消息，按原解释器读取前775条候选，保留原字节、原渲染SHA、格式及gaiji。标准Big5不使用替换字符，也不使用Big5HKSCS的错误字形。普通槽0–699索引10000+nativeId，古代槽800–849索引9925+nativeId，后者有独立合成有效槽测试，不能据此宣称实际开局已启用。

Scen014交换宋憲/徐榮身份后，原函数仍按槽位取传记，与人物身份不符，两条正文明确隔离；该源只连接664条传记，其余源666。四个未映射人物及64条姓名字形仍隔离。FA49/FA60等未解码字形在实际详情明确标注，不冒充原Unicode；687/775候选消息仍含未解码字形。Resource discovery、生效优先级与MOD覆盖链未执行，必须保留unknown。

最终字段报告SHA1542571b3c9f50a3751ac86078f825648d5be24efc772d3aac0f5e7b7780ec35；原传记报告SHAdf59adc7519fa577dfce34323e67daa4ce423334db3358c602f25aba2f195e90。两次完整运行对应报告字节一致。运行时目录压缩SHA77d42c74cc3c44e094354d624d5dff0e2f990c371be9c3f28e5f532d5d0f79d9，解压二进制SHA837d3eb6eb647edd55d594f432179fd45039cfb12fc13f090c3b060356d94133，两次转换一致。无数值/媒体资源导入。

## 验证

- 原读取器5项回归PASS：实际未登场姓名/字、特殊标记、原字节扰动、古代索引、原完整传记函数与消息引擎一致、原gaiji与格式重建、完整VM/RNG不变。
- 新保存资料109406项PASS：16来源在190正常工程新局，加其余8个工程包；实际巡察、每例三旬、存取重开、整份Save/RNG与未附资料的控制局完全一致；不是算术模型。额外660项验证未来命名空间/被改身份不追填且保存原字节。
- 原OfficerQuery515120、实际旧v32–36八份夹具33976、168固定资源PASS。v31独立夹具仍未取得。
- 本次APK实际安装并回读SHA相同：来源菜单→190新局→字检索→刘备/曹操/张辽正常行点击详情→原字/传记/缺口→实际巡察→三旬→槽3保存/读取，全Save/RNG701检查PASS（121.91秒，v4）。初次完整成功v3为125.79秒。过程失败日志保留：测试误点对话标题、巡察面板未展开/滚动，均修正测试触点，没有降低业务断言。
- 保存来自该实际流程：940046字节，SHA8a57a50663f2bb608c7e00f8bef6fb80946bf8f56e25d5631e389c0a44477531。主进程force-stop后在本次APK冷启动，38检查PASS（14.78秒），再次从正常列表/详情对照完整保存和RNG；不是JVM夹具冒充冷启动。
- 本次包默认新局/预览/取消/旋转/存取64检查PASS（60.54秒），原正常列表键盘/排序/旋转/Home恢复49检查PASS（43.34秒）；验证新增配置控件没有使原默认流程失效。
- 每次实装测试原七个内部文件均读回一致，auto SHA82554269b1920b5d67b8945d22a55f0253cab7ed2ea55bf520ef5b9b3b99f0a9；2902个原外部文件/库均设备SHA一致，未清数据。新证据文件独立保留。媒体设备仍为5582，5554由本批独占。

最后一次完整外部恢复/读回通过后5554锁释放，原应用保持force-stop状态；源流程保存和截图已留在本工作树out/session1/，不覆盖用户原保存。

主APK：out/session1/apks-batch03/app-debug.apk，91328469字节，SHA7c5b128be0fc42fe138323ff0274964a09f0ad08d40d4b0def9c06a7646d9e6b。测试APK SHA83a2f1c15272a4a492cae04d25f8099a0a2a67a1ab9d39932e99b5132e32580a；独立详情验收包v5 SHAec2d7214e5b28bec2a527d4bb75baca37d2cc6625b7977475fb03003441d1ba2。版本仍为继承159，以SHA和source-text-apk-source-guard.json识别。实际证据为emulator-5554 x86_64；ARM仅打包，未实机验证。

## 必须继续解决的旧基线差异

动态架构旧七状态基线未通过。分别隔离编译9548bb35原核心、前批2910997核心和当前核心，并从同一真实v33夹具运行同一征兵/巡察序列。前批与当前每个保存字节完全一致；原核心匹配原golden，前批/当前在征兵后仅CRC四字节和AP字段401不同（旧扣10，现扣20）。不是本批文字资料造成，也不能因前批记录的“架构通过”而当成动态旧基线已过。原golden未改，未回拷旧核心，具体证据inherited-v33-ap-baseline-difference.json。兼容策略及所有旧档正常续行必须在完整目标结束前闭合；静态架构守卫已过。

## 重现入口

使用Python3.9、Unicorn2.1.4与独立PYTHONPATH=out/session1/toolchain/pc-emulate:tools/content，原PC路径仅作为输入：

1. inspect_pc_officer_fields.py PC --output OUT/fields.json.gz
2. inspect_pc_biography_messages.py PC --output OUT/messages.json.gz
3. build_pc_officer_text_catalog.py PC --fields OUT/fields.json.gz --messages OUT/messages.json.gz --output OUT/catalog
4. PC_INSTALLATION=PC python3 tools/content/test_pc_officer_fields.py
5. 独立GRADLE_USER_HOME=out/session1/gradle-home、Java17、SDK原只读工具目录；gradlew --offline --no-daemon :game-runtime:verifyPcOfficerInfo :game-runtime:verifyOfficers :game-runtime:verifyOfficerLegacy :app:assembleDebug :app:assembleDebugAndroidTest。
6. verify_pc_officer_ui.py使用冻结APK/SHA守卫、独占serial、主机外部封存、ART夹具；--info-only --source-opening执行正常新局流程，--source-resume与实际流程保存执行冷启动读取。

接下来：完整实际剧本加载/开局后处理和事件、47势力/87据点/军团/资源/设施/外交、逐字段身份引用及特技效果、4字形身份、17能力/适性差异、旧兼容策略。当前仍未达到完整目标，goal保持active。
