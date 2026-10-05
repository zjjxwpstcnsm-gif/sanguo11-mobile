# Batch10：原舌战内核的有界移植与界面回调差异

完整目标保持active；单挑和舌战均须完成正式触发、操作、结算、中途保存与实际APK，不能以本批函数通过宣布完成。前提交d3d82f05，原完整继承基点8400301不变。

新增PcDebateRules/PcDebateState仅分阶段移植，本批生产Contests/ContestSave/正式页面仍未调用这两个类。旧31–37和已存38策略不升级、不追填、不静默切换对局公式。本批没有新APK；Batch09 APK仍在5554独占串行验证，ARM仍仅打包。

## 原值和独立对照

固定原EXE SHA30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb。只读执行原手牌容量、牌分类/大小、比较、伤害和RNG、心理/怒气夹紧；原规则固定夹具5125行，SHAea8ddf41b6d4b53a8a1e3e29d863b5a2f7a304c95d295d0ee1fc5839b0a492b4。16组原初始化全部心理/怒气/憤激、智力修正、7手牌槽、18牌库、游标、5特殊牌池和原RNG逐字段一致，夹具SHAe0e20a686ed43e95ec7ffe415ab6ead7c1b0d7112c72be0946fc65f9e8f3fd90。

inspect_pc_debate_effects实际执行51e3c0出牌资格与51eb30/51eb70/51ebd0/51ec90/51ecd0/51ed30即时效果：2176组资格、2400组心理/怒气/反射/夹紧、17个普通怒气getter值；全部原RNG不变。两次压缩原输出字节相同，SHA37922ad1c3529fcefe81ad34b5e49dd67dbb800d86d5e5a719582616fee96dbc，移植4593行原输出通过。原普通小/中/大怒气10/15/20、大喝15；之前静态初猜0/5/10作废，未生产接入。

## 原界面反制不是无界面分支

51ed90在无界面路径复制对方speaker到栈上，仅从临时副本移除反制牌；有界面路径调用519fd0排入队列。Batch08无界面终局轨迹仍有效，但其证据边界不能扩大为正式有界面玩法。519fd0包含原4721d0随机抽取，不能省略它后声称完整Save/RNG还原。

inspect_pc_debate_ui_counters使用明确合成UI队列存储、原49b490空消息上下文及原人物构造，完整执行原519fd0。它生成type8消耗手牌记录，再执行原switch case51cedb..51cf3c，停止在3D/音频调用4d0570之前；没有替换规则/牌移除/随机函数。96组原四性格、无反制/镇静/愤怒、左右侧、四seed全部匹配；无反制1次原随机，镇静或愤怒反制2次，实际对方手牌13或14移除并压紧。两次原输出SHA均bf1d77cfce20c2a857869e60177ab06384fc01e33d01653b1e5b2ef67da339b8，固定夹具SHAc8ec9e71f332368c0a01c582ba8965240a2bdb92475096b2ec3ec9b2b8786bb7。

这是有界原队列/消耗指令证据：未运行原UI构造、有效消息发现、动画排队调度、完整有界面PC流程或战役结算。首次合成调用缺原消息上下文而停于49ae93，失败保留out；补映射并调用真实49b490构造后原函数返回。没有靠填假资源/拦截函数放行。具体渲染/媒体实现不在本会话所有权。

## 可复现验证和冻结守卫

tools/content/build_pc_debate_oracle_fixtures.py以逐SHA固定的原报告生成四组夹具，重复输出字节一致。tools/content/test_pc_debate_port.py独立编译全部core源码和两个针对原输出的测试，缓存仅out/session1/contest10；原5125规则、16初始化、4593资格/即时效果、96反制队列/手牌/动作选择/RNG通过。精确命令：

```sh
python3 tools/content/test_pc_debate_port.py --java-home /Users/paopao/Library/Java/JavaVirtualMachines/temurin-17.0.17/Contents/Home --output out/session1/contest10/standalone-core-a
```

本批测试尚未增加Gradle入口，避免改变已构建Batch09 APK的冻结1024输入。之前暂加core/build.gradle入口导致第4源安装前守卫拒绝；未触发该轮设备写入，原失败记录保留，不记恢复失败。已恢复精确原文件并另起-r1验证，第4源已通过。冻结输入1024项复验无变化；已有设备套件每轮仍恢复原内部文件/偏好及3177原外部文件/库。

## 后续仍必须完成

完整舌战回合、憤激、胆小连击、原AI、原有界面随机次序、话题切换、战役结算/经验/登用/外交、显式新保存策略和中途冷续行。单挑已定位原模型50ab90、原团队50cc50和战员50ce00等入口，仍只是定位，未称其数值/完整流程通过。完整剧本事件/部队/设施/外交/军团和其余人物原字段、四字形标准身份/传记、表外关系仍在目标内。

文件所有权和媒体边界沿用PLAN.md；不改头像、音频、AndroidGameBridge、Unity/3D和共同台账。按稳定officerId/nativeId/sourceVariant连接文字/数值和只读媒体需求，来源身份未知继续明示。
