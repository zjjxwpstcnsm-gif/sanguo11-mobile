# R22 能力变动、年龄来源与原日期边界

继承完整AL4029文件301219621B，manifest SHA256 a1b21e8de197d793832e9a11b8fa9bba3b2a22068bed8ca27a26a4c27b2ea1f7。本轮先核对实际工作区、最新交接和UI17活跃状态，保留全部修改；未修改app/API/runtime或合入UI17 WIP。

## 已消除的未知项

`export_pc_officer_date_oracle.py`只读固定SHA原EXE及共享/16剧本，未启动Wine。实际执行原设置标签绑定5460d3、选项绑定546160和事件分发表545350，确认“能力變動”控件12b、选择11d有效→0、11e无效→1。只在原代码进入设置持久化/平台函数前停止，不称为PC窗口操作验证。

原新局4a4373从配置+8写global+28；4a443b在global+18非零时强制growthDisabled=1，同时按原分支设置另两个字段。六组原分支用完整3MiB对比、RNG和严格写地址守卫通过。未推断用户保存偏好或默认选项。

原年龄getter488a20：固定模式使用剧本起年−出生年+1；普通模式经4824b0按360日历和10天/旬求当前年，再减出生年+1。没有把负年龄归零；现有Lifecycle.age的Math.max(0,...)不能直接作未来原能力计算入口。能力变动无效仅绕过成长百分比，经验、伤病、官职、内助仍参与此前核实的组合顺序。

16源global header均重新经483120原serializer从非零哨兵缓冲读入，与既有元数据逐字段核对；文件偏移16190的4字节对应global+18。SCEN007《英雄集結》、Scen013《女流之戰》、Scen014《英雄集結PK新版》为1，其余13份为0。来源身份/官方或MOD生效、项目heroes剧本的对应关系仍不能据此自行认定。

## 可复现产物与核心检查

新增pc-officer-date-native.tsv：1040行29093B SHA256 7d4ba634b99db13c40aecb67dea28a1aefee2d2d49b1070e9a0c720e171b1514。4组起始日期（含12月21/30日）×13旬数×5出生年×2年龄模式×2成长选择，全部年龄和当前政治结果均执行原函数取得；具体夹具为基础80、曲线8、经验105、伤病1、丞相政治+5、配偶内助+1。禁止原计算任何非栈写，逐组完整3MiB和RNG不变。不得将这组固定能力夹具当作完整人物导入。

审计docs/pc-data/officer-date-native.json：9888B SHA256 2f56032caa2623914580eed822ebb96ad376e741a8f8848d2958b3c0e1910954。第二次转换的TSV/JSON均逐字节相同。

PcOfficerAbilityRules增加原日期/成长模式计算。JVM旧能力4813行继续通过，新日期1040×3=3120项对照通过；官职2244继续通过。这里仍是内部纯算术，尚未接入正常人物持久化、交易经验或成长设置。不能为已有存档五值猜测原基础值，也不能把开局默认设为某值而冒称已查明。

复现：
```sh
PYTHONPATH=out/toolchain/pc-emulate:tools/content python3 tools/content/export_pc_officer_date_oracle.py --installation "/Users/paopao/Downloads/San11pk整合版/San11pk 超级整合版" --output out/date-oracle.tsv --audit out/date-audit.json
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew --offline :core:verifyPcOfficerAbilityArithmetic :core:verifyPcOfficerRanks
```

## 本包验证

29秒/80任务构建完成，架构和168固定资源通过，4原生输入/官职资源全等；门控脚本5种结果回归通过。新APK87030395B SHA256 d0187e6330cebc4d16eadc3264338f01cc0b907588192b7c9bdfd294fa692dd3，test740592B SHA256 ac804a8ae85027c302dd3333d1d1ba108c7020a370e7b7414e61bcef7f7e0f08，位于out/parity/officer-state-20261003/growth-settings/apks。5554实际安装后双APK完整回读一致，am22-opening-01新局/设置/取消/保存读档62项通过，87.99秒，12附件保留。原7文件逐字节恢复无新增，自动档仍SHA82554269…99f0a9。08b截图仍显示3D准备覆盖层，不能作为首帧或流畅性通过；不沿用AL127检查。

同一实际安装包生产类ART：功绩421/1.66秒、商人算术11595/0.28秒、能力4813行和日期3120检查/0.28秒、官职2244/3.50秒通过。只含7测试类+3份原输出/兼容夹具的独立DEX，生产类与官职资源均来自安装包；前后7用户文件与完整APK相同，三门控总通过。完整结果见round22-installed.json。本轮能力仍是纯计算，正常多旬/存档继续由官职套件验证，不能称原经验/交易全流程已接通。

app/API/runtime1027文件与AL全等。UI17仍在独立运行最终包，未有完成提交，未发送消息或复制WIP；后续按13ff5b9增量集成。无ARM真机或全规则完成结论。
