# UI 分支的冻结规则依赖构建

## 当前第十六批：AA / 交易次数与据点限制

使用 `-I out/uiux/contract-v6.init.gradle`，三个模块指向本目录 `out/uiux/contract-20261003-aa`。冻结AA的396份模块逐项核对已完成的manifest/verified.json；不读取玩法会话当前WIP，不覆盖自己的UI15。伴随输入 `out/uiux/iteration-16/build-inputs.tar.gz` / `build-inputs-manifest.json` 共402项（396模块、来源manifest、v6 init、4个双ABI原生库）。

AA的每城每旬交易许可为买卖/武将共享：任意正的历史交易量使本旬可交易量为零；下一旬由核心重置；港口/关卡拒绝交易。UI仅展示quote字段和正式reasonCode/allowed，不维护使用次数或重置计数。原始存档正交易量保留，存档格式仍33。单次数量/价差仍为模型值，不能称PC价格已校准。V允许同旬买后卖的记录只属于历史版本。

## 历史第十五批：V / 生产与交易

使用 `-I out/uiux/contract-v5.init.gradle`，三个模块仅指向自己目录 `out/uiux/contract-20261003-v`。完整继承冻结V的394份模块，SHA/大小逐项一致；新增生产和贸易DTO并包含20AP、城市容量和旧档余额兼容修正。根目录规则源码仍保持初始继承内容，不能直接用旧模块构建新app。

伴随输入为 `out/uiux/iteration-15/build-inputs.tar.gz` / `build-inputs-manifest.json` 共400项：394模块、来源manifest、v5 init与4个双ABI原生库。解包到源码根目录后采用下方命令，将init替换为v5。SDK、缓存和输出都使用独立UI目录。本批验证明确针对V；玩法方后续“每城每旬一次交易”等尚未冻结规则不能套用V通过记录。合入方只取UI增量，不覆盖自己已更新的规则模块。

## 历史第十四批：Q / 运输与外交

第十四批改用 `-I out/uiux/contract-v4.init.gradle`，三个规则模块指向自己目录`out/uiux/contract-20261003-q`。377份模块逐项核对冻结Q manifest；与P仅Army/CampaignAi/Districts/CoreTest四文件不同，API/runtime接口兼容。本会话只复制冻结文件，没有改规则；原目录后续审计和未冻结改动没有被复制。

伴随输入为`out/uiux/iteration-14/build-inputs.tar.gz`/`build-inputs-manifest.json`383项，组成同第十三批；P/v3仅用于重现第十三批，不能将其Android通过结论直接当Q验证。Q包含玩法方寻路优化与军团处理修正，JVM性能数据不能代替Android实际回合记录。

## 历史第十三批：P / 城市治理与普通建设

第十三批app消费`CityActionCommand/Preview`与`ConstructionCommand/Preview`，改用 `-I out/uiux/contract-v3.init.gradle`，三个模块指向自己目录的`out/uiux/contract-20261003-p`。377份冻结模块文件逐项对照玩法checkpoint-20261003-p核验；root core/API/runtime仍保持初始继承内容。

伴随输入为 `out/uiux/iteration-13/build-inputs.tar.gz` 与 `build-inputs-manifest.json`，共383项：377模块文件、来源manifest、v3 init与4个双ABI原生库。解包到源码根目录，沿用下方构建命令并把init换成v3。P新建Lv1，巡察/征兵/训练基础AP20；不能沿用G的AP10/Lv3验收结果。最终集成方已拥有P规则时按通常项目构建，不应覆盖自己的core文件。

第十三批结束时运输与外交界面仍待消费P的相应DTO，不能因依赖P就宣称所有新契约已接入。首次同步预览仍可能复制World，需以Android实际记录评估，不能以缓存命中耗时冒充首次性能。

## 历史第九至十二批：G / 出征 v2 与战法上下文成本

第九批起，app消费UnitFacts完整详情、Army/War.tacticFormationError与Army.tacticCost，必须使用 `-I out/uiux/contract-v2.init.gradle`。它把core/game-api/game-runtime指向自己的 `out/uiux/contract-20261003-g`，346份文件逐项核对玩法checkpoint-20261003-g manifest，不指向原会话工作目录。

伴随构建输入是 `out/uiux/iteration-09/build-inputs.tar.gz` 与build-inputs-manifest.json，共352项：346文件、来源manifest、v2 init、4个双ABI原生库。解包到源码根目录后，沿用下方JAVA_HOME/GRADLE_USER_HOME命令并将init参数替换为v2脚本。旧C副本和init只用于重现第六至八批。root旧core仍不能构建当前app。

新依赖专项在独立目录实跑570+412=982项通过。v2在相同StateToken内缓存私有查询World，首次仍需复制；app不建立第二权威状态。UI删除旧临时部队查询，直接读取DTO详情。Android耗时见第九批验证说明，不能套用主机基准作为手机数据。

## 历史第六至八批：C / 出征 v1

本分支从完整共享基线1a968dc继承，core/game-api/game-runtime目录保持原样；出征UI从第六批开始消费玩法会话新接口，不能再使用旧模块直接构建。最终集成者已拥有新规则代码，按顺序合入UI提交后按通常方式构建即可。

独立验证采用玩法方 `out/parity/checkpoint-20261003-c/source` 的344份模块文件，复制到自己目录 `out/uiux/contract-20261003-c`；全部对照源manifest SHA，另核对ui-contract-files.json列出的11个接口文件SHA。没有修改规则文件，也没有指向玩法会话的可写构建目录。临时Gradle init仅把三个projectDir指向自己的冻结副本；编译输出留在副本内，SDK、Gradle缓存和AVD依旧独立。

第六批源码归档与 `out/uiux/iteration-06/build-inputs.tar.gz` 配套使用。后者含344文件、来源manifest、init脚本和4个双ABI原生库，共350项；`build-inputs-manifest.json`记录每项SHA。将其解包到源码根目录后执行：

```sh
GRADLE_USER_HOME="$PWD/out/uiux/gradle-home" \
JAVA_HOME=/Users/paopao/Library/Java/JavaVirtualMachines/temurin-17.0.17/Contents/Home \
./gradlew --offline --daemon --max-workers=2 \
  -I out/uiux/contract.init.gradle \
  :app:assembleDebug :app:assembleDebugAndroidTest -PtargetAbi=x86_64
```

SDK须通过local.properties或ANDROID_HOME配置；本任务现有SDK位于独立工作目录out/toolchain/android-sdk。离线构建需要本任务已存在的Gradle依赖缓存。不能将“有源码”误称为“无SDK/无缓存也能离线构建”。

依赖专项检查：同一init下执行 `:core:verifyDeploymentPlan :game-runtime:verifyDeploymentSession`，468+331项通过。UI原六项上限测试随已删除的UiModels.deployTroopCap重复规则一起移除；相同库存/统兵/船只规则由上述权威测试覆盖，实际安装UiUxInstrumentation进一步核对表单显示、空区间、缺兵装/舰船拒绝、typed提交与存档一致。其余UI模型回归保留。

目前预览仍为同步API。表单合并100ms连续输入，切页和搜索不触发查询，关闭取消待执行回调；主线程局面复制的实测停顿尚未解决，已将证据和异步/缓存契约需求交玩法会话。合法预览后的战法/特技/射程补充说明仍读取既有核心只读接口，没有自行计算伤害或建立新权威状态。
