# 单挑第15合人控等待循环：原确认桥接

本增量仍为 B 原单挑 WIP，未完成菜单与实际 APK 验收，不替换已交付的军建54或旧39独立58批次。

## 实际故障

4参数新局显式 PDU3/PGO3，源0，人控 native517 对 AI native365，声明相邻单将部队。seed1 的自然合法输入在第15合循环300次；保留完整存档 `out/session-b/duel-ruler-ai-seed1-nonterminal-v1.sg11`，2816918字节，SHA `d13cc26c0b5bd05ec56cfcfb3f1d6264cf769f028ecdb7f9d40fd5d9d30c2b7b`。

该存档frames1778的实际模型：人控HP74，AI HP11/斗志300，pending字段0x14=1。阶段3选择AI必杀，阶段7因pending1取消并回到11→3；合数和HP没有推进。不得通过替换种子或编辑终局避开该样本。

## 原证据

固定原EXE SHA `30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb`，完整源0加载、日期/地理、493400后运行。

原阶段5入口为509fa0。其sub2读取5116f0（view+3ccc），确认非零后50a0ed将model+14从1置0；下一帧sub2转sub3。原阶段7/50a180对pending既非0也非2时转sub5取消。NULL-view阶段5/sub1直接到sub3，恰好绕过这次确认。

工具 `tools/content/session_b_pc_duel_pending_input.py` 运行原5116f0的6个storage值，并执行原509fa0/sub2的pending0/1/2 × ready0/1矩阵。只有pending1且确认非零时清零。全源世界3MiB和原RNG均未改变。结果 `out/session-b/duel-pending-input-source0-v2.json` SHA `40ac5f7582bb2fddead6d033d6c72ee153e3db354c76f0eda064dc1c655e5c75`。声明UI storage/subphase样本，不称PC GUI验收。

## 兼容与边界

`PcDuelCampaign.submit`在已合法验证、完整候选复制之后，对已保存commandBoundary且pending1执行这次平台确认；原NULL-view取证帧驱动保留原行为。确认仅写pending字段，不决定结果、不抽随机数、不改人物/政策。

已有commandBoundary故障档不迁移、不静默读写。读取/预览原字节不变；玩家正常输入才确认并续行。旧31–39非native单挑不进入此路径。本批已正式重建并运行针对性回归，但完整31–39/历史自定义及真实APK矩阵仍未闭合，不能据此声称全兼容已完成。

## 当前验证

同一故障档经typed GameSession正常选择继续，一次输入自然终局：winner0，第15合。完成玩家释放选择、一次性战役结算、4次全World回合及每次保存冷解码，原故障档字节保持不变（resume-v1）。resume-v2已通过非法输入与旧令牌双击纯性；6份完整原sub2模型字节对照已通过。正式Gradle重编译（8任务）通过，使用正式产物重跑同一故障档也通过。原连续帧168988、人控127682、换将67111完整模型/管理器/RNG检查通过，架构检查通过。GameSession通用回归1690项通过；首次误写SessionTest类名失败日志保留，正确GameSessionTest另用v2日志。

新局重跑种子0–3自然玩家胜利，种子4原拒绝，种子5自然玩家败北。typed FINISH执行AI RELEASE、删除败方单将部队、保留君主身份/task37返城，4旬及每次全World冷读通过；541项检查，日志duel-ruler-ai-session-v4.log SHA 3975efcd9eee8936117235b1dc797a34e603c9d459ea7b493f24d240a1a3abaf。声明相邻部队，不能称普通出征或菜单/APK验收。

天子行政驻点与帝号分支仍明确阻断，不能用source0初始化占有者代替当前状态。

当前Android应用编译通过（1m11s，24任务，4执行），仍无本增量新APK安装。精确当前文件/evidence/JNI与旧dirty守卫见PENDING_INPUT_GUARDS.json；所有生产native增量仍未提交，不混入完成58源码。
