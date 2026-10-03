# 第十四阶段：商人次数、跨旬与旧档兼容

继承完整X（3936文件290523319字节），生产代码与已安装W全等；UI基点保持9e30e45。没有覆盖UI15活动工作树，未改app。PC目录只读，未启动Wine。

## 原程序证据

商人5caaf0经481310读取city+a4 bit1，已用即拒绝；真实提交5cacd5置位。普通结算58051e调用4a1180推进10日、5805d0调用59c330，59c423再调用598630，循环87据点经487860清除行动标志。城市尾跳47b730清除+a4，关港48da10清除+68。598630没有月/季条件；没有将完整PC主循环或全部原命令宣称为已运行。

`test_pc_merchant_turn.py`执行180种日期推进，核对540个年月日结果；4种标志覆盖87据点，比较3MiB完整世界和原RNG不变，两项2.849秒通过。最初把关港字段假设为+6c导致严格比较失败；查真实48da10机器码确认+68，补机器码断言后通过。失败日志保留native-turn-reset-tests.log，没有缩小比较范围。可复现工具inspect_pc_merchant_turn.py输出merchant-turn-native.json，锁原EXE SHA和调用机器码。

价格更新4b3c60和原RNG另执行3项18.260秒通过：12月初始化192组、非法月份32组、月更新2816组、RNG边界85组。原RNG未经hook替换，精确核对最终状态与3MiB唯一价格字节变化；分支覆盖一次/两次随机消耗。月表和函数校验值由inspect_pc_merchant_prices.py输出merchant-prices-native.json。city+9c按mask3再mask4影响价格，其具体事件含义未核实。全部场景原始7c=50不能视为实际初始价，因为初始化会覆盖。价格新模型暂未接入，仍待保存价格状态、当前政治修正和环境事件；没有更换工程64位RNG。

## 实现和兼容

正常Campaign.trade和TradePlan采用同城每旬一次、买卖与执行武将共用状态；军团饥荒采购不再按剩余累计额度尝试。第二次失败TRADE_USED/field=city，不改资源、行动力、存档、RNG、revision或事件。预览effects仅成功时存在；quotaRemaining已用为0，未用为maximum。tradedBefore保存原成交量，任何正值表示已用。正常完整旬推进清除；不同城市仍可各交易一次。

没有存档格式升级和余额/成交量截断。PcMerchantUseCompatibilityProbe独立编译冻结X完整core及探针，实际执行旧规则两次成交写档；新core读取后逐字重新编码全等，再拒绝同旬买/卖，三次完整回合均恢复机会且正常交易/save/RNG回放全等。legacy-two-orders.sg11与运行日志保留于out/parity/merchant-native-20261003。

TradePlan922、TradeSession92、Territory674、容量137检查通过。旧TradePlan/Campaign中的三次累计成交断言按新原程序证据改为同城一次，并增强原子拒绝、跨旬真实再交易和不同城市检查；原断言可在X复现。Campaign全套仍在merge的设施位置冲突失败，新版第93行、冻结X自己的代码/测试第91行同样失败；未削弱断言，不能宣称整套通过。

价格、1000步进、20000单次数量上限、商人建筑类型许可、执行者功绩等仍待对齐。UI15当前冻结V的累计额度通过证据不能用于此版本；新规则必须在最终集成包重新构建、实际安装和完整验证，不借用W的437项。

## Y构建与安装

完整Y冻结3944文件290552774字节，全部逐SHA/大小/工作区校验通过。43秒76任务构建成功；主APK87019885字节，SHA83578d19c4bba0503f41d359e9bb8e0eb50e3dd270cfc4ee535b94e67d34817f；test696127字节，SHAdee60fe08c39ba1a4cad798a450e5c4233e990e116fdb6bd09ae48c62a98a8b0。168固定资源和4原生输入逐包验证一致。路径out/parity/merchant-native-20261003/apks；UI仍为9e30e45，没有集成UI15。

5554覆盖安装双包并回读SHA一致，y14-opening-01在97.37秒通过62项新开局、完整读写、取消覆盖、设置和旋转，12份附件已拉取。安装前外部完整备份，结束后7原文件全字节恢复、无新增，自动存档SHA仍82554269…99f0a9。08b截图仍显示3D准备覆盖，不能视作首次渲染性能通过；仅API29/x86_64模拟器证据，没有ARM真机。

同包y14-cargo-travel-01在81.77秒通过52项正常190混合运输、两旬推进、一次卸货、人员返程和完整读档，7份附件已拉取；再次7原文件全等、无新增。Y最终两套114项见installed-summary.json，不能将W437或旧年龄矩阵合并到Y结果。制造/交易的新typed UI还未集成，所以Y114不构成新交易UI全链验证。5554测试已结束，原用户局面恢复。

Y冻结后增加的是取证/复现工具，没有改变生产模块。verify_pc_merchant_compatibility.py以显式冻结manifest验证全部旧core源码和资源，再独立编译旧写入端和运行候选读取端；正式工具复跑通过，旧档2319字节SHA51511da05c2c232a61495d0a43f1268d8d66fd45830af14b72fca2357f8936d7。运行：先用Java17执行Gradle :core:testClasses，再以JAVA_HOME运行该脚本，--snapshot out/parity/checkpoint-20261003-x，--output 指定新的项目out目录。

另test_pc_merchant_site.py在原5ca985..5ca9c9入口执行87索引×3建筑类型共261组，真实486660类型检查与490a10城市索引检查，仅城市0..41且类型0进入后续许可。1项7.354秒通过，全部3MiB与RNG无变化。Y尚未实现关港交易拒绝，下一步应加入TRADE_SITE并核对AI/旧档/正常UI；这不是已完成对齐。原5ca7xx数量上限函数已发现，尚未验证；不把工程20000上限当原版。
