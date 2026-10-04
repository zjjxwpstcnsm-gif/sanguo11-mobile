# 人物 metadata 与媒体连接契约

Session 1 负责身份与文字/数值真值，Session 2 负责头像像素、年龄变体与加载、音频。

来源键 `sourceVariant` 由安装相对路径的安全 slug 和完整文件 SHA 组成；`sourceVariant + nativeId` 是不可合并的来源记录键。剧本稳定 ID 含文件 slot 与 SHA，不能用重复的内嵌编号去重。

`officerId` 只来自姓名、生年、性别的身份校验或另有明确身份依据；未映射为 null，不把候选 native 编号当项目 ID。Scen014 宋憲/徐榮换位逐源映射。原 64 条字形缺口保留，不能用 Big5HKSCS 的不同字形蒙混。

后续只读输出 `officer-metadata-manifest.json` 与 `portrait-requirements.json`，字段为 officerId/nativeId/sourceVariant/sourcePath/sourceSha256/recordSha256/nameBytes/identityStatus/faceNativeId/birth/sourceScenarioDate，并附字段 coverage/unknown。请求清单不决定像素和年龄阈值，媒体会话核实选择；未映射行不能覆盖现有人物身份。

媒体输出独立 media manifest，按三元组连接，不改本 metadata。媒体可读取本独立目录 `docs/handoff/20261004/session1/` 的已提交契约与清单。新增头像 API 如必要，在本文件先登记，由媒体实现。

人物列表、搜索、详情读取相同权威 DTO，携带完整 StateToken。查询不消耗 RNG、不生成 ID、不提交事件。旧档缺失来源字段保留 unknown，不能从新目录静默换将或补值；人物 base/growth/XP/current、各保存策略完全保留。

最终整合按双方基点/完成提交/前镜像 SHA 守卫顺序处理；不复制另一会话 WIP，不并写共同人物资产 manifest。

## 首批权威人物查询

新增 `game-api/.../OfficerSnapshot.java`、`game-runtime/.../query/OfficerQuery.java`、`game-runtime/.../OfficerQueryTest.java`；GameApi/GameSession 增加只读 officers()。DataTable.java 仅增加人物 DTO 重载，OverviewUi 仅武将表接入，MainActivity 仅增加人物查询方法和改写 officerDetail 的文字来源。game-runtime/build.gradle 注册实际 main-style 测试；修改前 SHA 在 identity-dto-before.json。

本批读取保存中的当前人物事实，不查询 ContentCatalog 追填原姓名/生卒/传记。旧档未知 base/growth/XP 的数组为空，不将旧 current 冒充原 base。sourceVariant/nativeId/courtesyName/biography 尚未持久化，一律未核实；这不是来源新局转换完成。正常武将一览搜索/排序与详情共同消费 DTO，头像和定位保留原只读 World 对象接口。

验证工具新增 `tools/content/verify_pc_officer_ui.py`，复用现有正常 widget runner，另封存/恢复所有原外部文件与库；新增文本取证工具 `tools/content/inspect_pc_message_resources.py`，只在独立 VM 执行原 LS11 解压函数，输出隔离文本二进制及取证摘要，不修改任何媒体文件。

补充确切测试文件：tools/content/OfficerUiOpeningWriter.java、ArtOfficerFixtureCanonicalizer.java、canonicalize_pc_officer_fixture.py、android/OfficerInfoInstrumentation.java、build_officer_info_ui_probe.py。仅独立测试 APK/host 夹具，生产规则类全部引用本次实装 APK；不替换或改写现有共享测试 runner。详情截图和每次失败/成功的原日志在 out/session1/，本目录保存摘要及 SHA。

## Batch 03 来源字段闭合

先拥有 `tools/content/inspect_pc_officer_fields.py`、`tools/content/test_pc_officer_fields.py` 与本目录对应 source-fields-native.json.gz/summary/差异说明。执行原73ca80属性名初始化、4c8720读取器及真实人物构造/serializer，逐源记录字段名称、ID、返回值与具体来源字节；经验/当前缓存等无剧本输入字段仍区分初始化/开局未闭合。新增文本getter、字形核查工具及持久化接入前再追加确切文件与SHA，不修改共同人物manifest或媒体文件。

本批追加确切所有权：tools/content/inspect_pc_biography_messages.py、build_pc_officer_text_catalog.py、test_pc_officer_fields.py；core/src/main/java/game/sanguo/core/PcOfficerInfo.java、PcOfficerSources.java；core/src/main/resources/pc-officers/source-text.bin.gz、index.txt；game-api OfficerSnapshot、game-runtime query/OfficerQuery 与 PcOfficerInfoTest、game-runtime/build.gradle；app MainActivity 的新局参数及文字详情、ScenarioFactionPicker 的可选文字资料来源控件。修改前 SHA 在 source-text-before.json。

新局可显式选择本地 PC 文件的文字资料（字与原消息传记）；未选择保持原流程。此选项不称官方剧本还原、不写能力/身份/归属，不决定头像或音频。资料固定写入现有长度分隔 SaveExtensions 独立命名空间，无来源资料的旧档保持 unknown，不升级 v31–37 策略、不从新目录追填。字形缺口按原字节明确标注。实际开局事件和官方/MOD 生效性尚未闭合。

已实装的稳定连接：OfficerSnapshot.Officer.source可为空，非空SourceInfo含nativeId/sourceVariant/sourcePath/sourceSha/recordSha/courtesy/biography及缺口；外层id就是经过身份校验的officerId。PcOfficerInfo.saved(World)也可读取保存中的只读连接。媒体可以消费已提交DTO/独立metadata清单，不能从source为空的旧档猜测来源，也不能用nativeId未经身份连接当项目ID。肖像像素/变体/年龄选择仍由媒体所有者实现，本批未修改头像调用或AndroidGameBridge线格式。

## Batch 06 显式来源保存入口

PcScenarioIdentity.Source 的 sourceVariant/path/SHA/SharedSHA/date/unknown 固定保存在新来源存档38；只有明确 PC 来源工厂与38头启用47势力容量。旧31–37同名opaque扩展不激活、不目录追填。普通新工程局仍原保存版本；媒体DTO读取原已存来源连接，头像接口与manifest不变。BasicCityPolicy仅记录新未管理作者局native20策略；旧31–33无标记按历史10AP/巡察规则续行，34–37保持原已存模型。此入口不是16来源开局完成。

## Batch07 来源世界及原字形身份

新增正常PC来源新局候选：每份16独立源的一份才建立世界。SourceInfo新增identityStatus（canonical-identity-verified/source-only-gaiji）和originalInformation；既有officerId/nativeId/sourceVariant保持稳定。source-only四身份以原姓名字节/生年/性别SHA分配，不能当标准人物映射：156234/844857/598828/850922。逐人原记录与未绑定引用、NPC/模板/古代槽保存在PcScenarioPeople独立metadata；媒体可按此三元组读，不并改人物媒体manifest。本批未改头像调用/像素/年龄选择或桥序列化。
