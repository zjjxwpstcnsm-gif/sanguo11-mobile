# 第十五阶段：商人城市许可与数量取证

继承Z完整3946文件290562590字节，1370模块/构建文件与已实装Y一致。起点Y包114项通过的范围保持原记录，不用于本轮新代码。当前UI基点9e30e45；未修改另一会话的活动app文件。

## 实现与验证

原5ca985入口调用486660要求building类型0，再经490a10要求有效城市索引0..41；261组合的完整3MiB和RNG纯净测试已通过。现在TradePlan统一拒绝关隘、港口，TRADE_SITE/field city，无effects；库存和输入仍原样提供，但quotaRemaining/availableMaximum为0。UI必须使用allowed，不能将quantityValid当完整许可。项目采用SiteKind.CITY映射原类型，不能把项目ID硬限0..41，自定义城市仍合法。

正常Campaign.trade、势力AI和军团采购共享此规则；AI不会在关港尝试商人采购，失败也不会假装已经完成订单。旧档关港金币/粮/既往交易记录不删除、不截断，SaveCodec格式保持33。

TradePlan964、TradeSession100、Territory674通过。容量137、经济成本319、制造计划756、制造Session216通过。测试覆盖城市/关港、买卖、0/1000/20000历史成交量、失败完整存档/RNG/revision/事件不变、两个正常回合。另verify_pc_merchant_compatibility.py分别独立编译冻结X完整core，真实写出城市两笔成交与港口两笔成交旧档；新核心逐字回读，全量状态和三旬回放一致。港口旧档2446字节，SHA c2eaebfdf2771a84233f7d73bf5da938214cd80857af861966ecf3df0a713cb9。日志out/parity/merchant-site-20261003。

## 数量证据与尚未接入内容

原5ca770交易容量320组加两个null输入、5ca450命令数量验证60组通过，两项5.598秒。没有替换原函数或随机函数；每次核对全3MiB世界和RNG不变。

原买入量为min(金×rate×400/((450−当前政治)×10)，粮上限−现粮)，卖出量为min((金上限−现金)×(450−当前政治)×rate/3200，max(0，现粮−1))，整数除法。最终函数做signed64与INT_MAX取小，没有假设下限钳制；本轮覆盖合法库存范围。原数量函数可返回超过20000和非整千数量，卖方保留至少1粮。

原5ca450只负责有效城市/武将、非零有符号粮变化和变更后粮≤1000000，库存/可支付范围在UI等其他入口约束。不能因这个局部validator弱校验，就让工程接受无库存卖出或越界溢出。6095ee..6096dc将当前粮−最大卖量/当前粮+最大买量交给数值输入框，并直接保留返回整数；该路径未见整千舍入，但未仿真整个数值对话框。原命令正常调用5ca950+5ca450后执行，旧20k/1000规则已明确存在差异。

本轮先落地城市许可；数量、价格、当前政治完整修正、功绩和经验仍待一并接入；city+9c环境状态已在下述新增取证中确认，不能在还用旧价格公式时宣称数量全对齐。导出工具inspect_pc_merchant_admission.py产出merchant-admission-native.json，包含原路径、EXE SHA、函数范围/SHA、规则映射和未完成项。PC目录保持只读，未启动Wine。

已封存AA完整3951文件290582548字节并逐项验证，再从9e30e45顺序合入UI15完成提交74072e9b0e68fb7efd191782f19e0e1b5dfd69a8，25文件只含app/UI资料/progress。preflight逐项前镜像一致，按固定commit blob应用，未覆盖core/API/runtime。架构边界检查通过，本侧整合包已完成构建与下述独立安装验证。UI15原V下交易75/制造138属于其自身构建，不能借用。旧trade脚本的同旬第二笔/累计额度断言与新规则冲突，应由UI后续针对TRADE_USED/TRADE_SITE及真实下一旬更新验证，不能改回已取证规则来使旧脚本通过。

## AB包与历史测试诊断

整合AB完整3968文件295941658字节逐SHA/大小/工作区验证通过。25秒80任务构建成功，TradeSession100/ProductionSession216通过；主APK87022841B SHAa29558effce346f50aa311137a0d5c47e3ae8af2d75d8624f9b8a7050fdbd309，test700707B SHA42ee55915bc820700e3d005a82cce74fbdc69f2624804aba5db38a281590d2b8。168固定资源及4原生输入一致。out/parity/ui-integration-74072e9包含固定增量审计/构建/安装记录。5554实际安装并回读主/测试APK SHA一致，制造138项/374.31秒与新开局存取62项/104.28秒，共200项通过。每套恢复全部7份原用户文件、无新增，自动档SHA82554269b1920b5d67b8945d22a55f0253cab7ed2ea55bf520ef5b9b3b99f0a9。制造从正常190曹操局面真实建锻冶所两旬、制造2500枪装；重载起点后建工房两旬、冲车制造三旬，库存2变3。共七次实际推进，分属两条重载路径，不是同一局连续七旬。日志/10+12附件及installed-summary.json均保存。仅API29/x86_64证据；无ARM真机结论。5580由UI16自己使用AA继续新交易规则测试，尚未集成其WIP。

AB冻结后仅调整了CampaignTest历史夹具，未改生产模块。旧市场(3,2)实际cityAt=10、development.contains=false，属于城市七格占地，(4,1)/(4,2)才是合法相邻开发格；铸币厂相邻格改(5,1)。增加开头SaveCodec.validate，保留全部合并、收益和中途读写断言，此后merge用例通过。完整main继续在migration旧地图版本兼容断言失败；直接执行冻结X原migration也同样被SaveCodec版本门拒绝，断言未削弱。

另将各私有case分别调用审计，原continuation的两盟友兵力恒定断言失败；完整战报证实乙部队移动至(7,9)后被第三方丙城自动射击78兵，并非盟友互攻，冻结X原continuation同样失败。将这段夹具改为仅两个盟约势力，保持双方兵力完全不变的原断言；其他三方外交测试仍保留第三方敌对校验。日志campaign-case-audit、baseline-x-migration、baseline-x-continuation、pact-inspect均保留。修正后逐case完整审计13通过，仅migration失败；continuation包含多势力正常回合/save/RNG续行。整体Campaign仍未通过，不能因局部通过就报绿。

## 价格环境字段命名闭合

新增test_pc_merchant_conditions.py执行原属性名称分支4c09ef、setter分支4a3abd和getter分支4c0cd3。原属性0x9a/0x9b/0x9c的Big5名称分别为瘟疫、蝗災、豐收，经原47b550/47b3c0对应城市+9c的bit0/bit1/bit2。三条真实标签及48组设置/读取检验通过（3.599秒），全3MiB只允许目标位变化，RNG不变。不是根据动画名称顺序猜测。

inspect_pc_merchant_prices.py现输出schema2，merchant-prices-native.json记录名称字节SHA、表地址、属性ID及读写分支。原月价因此可明确为瘟疫或蝗灾时30/40、否则丰收时60/70。灾害发生/持续/完整结算顺序、当前政治修正、原32位RNG如何与现有64位存档兼容仍待实现和验证；不能将字段已命名扩大为事件系统已对齐。原价格/数量仍未接入AB。
