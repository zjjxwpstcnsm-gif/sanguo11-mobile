# R21 原官职来源接入与安装验证

## 来源与转换

原安装只读，无 Wine。`san11pk.exe` SHA256 `30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb`，共享 `Media/scenario/Scenario.s11` SHA256 `dfca0316e019454e77bab5805e3d725ae35e34408e1699e869e5419e2f5fad6f`。

原serializer 0x48e2e0..0x48e35c 将81×46字节读入root+0x7d9dc、stride0x3c；首记录源偏移17410。原名称初始化0x73cfa0和属性getter0x4cb550确认指挥、上升能力、上升值、俸禄、格、功绩。16份type22剧本在此分支都不消费官职数据，不能把其零缓冲表当覆盖；共享type24实际消费3726字节。所有候选源SHA复核，不由文件存在推断MOD激活。

`tools/content/export_pc_officer_ranks.py`复用原解码、再执行81×11=891个属性/加成读取，禁止非栈写并验证完整3MiB及RNG不变。加成getter返回AL，初次错误比较完整EAX的失败保留，已按实际ABI修正。显式`pc-rank-project-ids.tsv`将80个旧项目ID逐名连接原编号；native80無保留为来源项，不列为可任命项。

产物`core/src/main/resources/content/pc-officer-ranks.tsv`10262B SHA256 `78ce6ffb93c7164d81858df7d5ca5387ffe830f2fabb821ece3b3fb8122f1288`，每项含源路径、源SHA、记录偏移/校验。详细审计`docs/pc-data/officer-ranks-native.json`20366B SHA256 `1973144cc589da894c58ae4c9fda4cdcd4b2e16f62f28f08afd48cc15445e097`。独立二次转换均字节相同，见out/parity/officer-state-20261003/rank-reproducibility.json。

复现命令（Python需现有Unicorn工具目录）：

```sh
PYTHONPATH=out/toolchain/pc-emulate:tools/content python3 tools/content/export_pc_officer_ranks.py --installation "/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版" --bridge tools/content/pc-rank-project-ids.tsv --output out/ranks.tsv --audit out/ranks-audit.json
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew --offline :core:verifyPcOfficerRanks :core:verifyRulerTitles
```

## 正常命令与兼容

Government移除手写列表/数值，读取SHA固定资源；缺失或损坏即失败，禁止回退到另一套无来源参数。80旧ID、顺序、统兵/薪俸/功绩和爵位门槛与冻结AK已编译core.jar逐项一致；兼容夹具及旧jar/class的SHA见tools/content/fixtures/pc-rank-legacy-ak.json。

PcOfficerRankTest共2244检查：每官职正常任命、零RNG、保存往返、下一旬真实出征至统兵上限、保存后连续三旬回放、无官职对照的月俸差。JVM第二次通过，第一次fixture填入不存在的剑库存失败保留；修正fixture而未弱化断言。能力算术4813、商人功绩421也通过。既有RulerTitles540检查通过。架构检查通过。

requiredTitle仍是旧工程规则；无官职源薪俸5未接月俸，不能宣称该两项PC一致。能力加成仅提供来源字段，尚未接人物基础/当前能力与经验持久化；存档格式33和所有旧数值保持，不反推或改写原用户人物。纯能力oracle通过仍不是正常交易经验/价格已对齐。app/UI、API和runtime未改。

## 实装验证

离线构建24秒/78任务成功。冻结主包`out/parity/officer-state-20261003/apks/app-debug.apk`87030287B SHA256 `d47da9b4ec8d7394a609681bccaa513387da44c5c4d888844ed0637be3e192f1`，测试包737034B SHA256 `4b5ff8c3437e0f134cded374ced36e218038ea0ca5fd325c84a898dd66da00ef`。168固定资源、4原生输入、新官职资源逐SHA相同。app/API/runtime1027份源与AK全等，未修改UI会话文件。

5554 API29 x86_64实际安装后两包回读一致：`al21-opening-01`新局/设置/取消/存取62项通过，69.85秒；12附件已提取。原用户7文件逐字节恢复，无新增，auto仍SHA256 `82554269b1920b5d67b8945d22a55f0253cab7ed2ea55bf520ef5b9b3b99f0a9`。08b截图仍显示3D准备覆盖层，不作为首帧或流畅性通过。此为本包独立运行，未沿用AK结果。

独立测试DEX仅含7测试类及测试数据，不含生产类或生产官职表；生产实现/资源来自同一实际安装APK。ART：商人功绩421/1.81秒、商人纯算术11595/0.39秒、能力纯算术4813/0.40秒、正常官职流程2244/4.49秒全部通过。完整APK前后回读与7用户文件均不变，三门控总通过。probe SHA256 `090eb5c39eab91e369b607421f0fecf11b56d5070e6a9b8d605c224e8a723910`。它们是生成局面上的正常命令/回合/存档验证，不是触控数量。脚本门控5种结果回归通过，首次遗漏JAVA_HOME调用失败日志另存，未修改断言。

同一冻结包`al21-deploy-01`在有来源的正常190曹操局面真实触控出征、取消/提交及读档恢复65项通过，44.64秒，11附件；本包UI合计127项。两套各自恢复起点，不拼成同一局连续流程；连续三旬由独立ART正常命令套件验证。每套结束原7文件全等无新增。详见round21-installed.json。没有ARM真机证据；保留UI17正在进行的独立改动，未拷入WIP，下一完成增量基点13ff5b9。全规则/全剧本导入仍未完成，历史失败不因此结案。
