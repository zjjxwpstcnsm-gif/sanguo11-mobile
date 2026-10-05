# 批二十一：语音原随机依赖与完整部队发言者链

基点7aed34ca9836a7f83655ad5d1ac7fb8c631f8921。本批补足直接影响普通语音接入的原证据，不修改规则/API/runtime、人物metadata或Android生产代码，不操作设备或存档；批二十已实装包继续保留其明确验收范围。

## 原11/12不能代替成功/失败

inspect_pc_voice_random_gate.py仅静态读取原PE并核验指令/字节SHA，不使用Unicorn或执行RNG。5038b4压50，5038d8调用4721d0；原正阈值路径会读写8a5d44，以uint32(state*0x6c078965+0x3039)更新，取高16位%100与50比较，返回0/1；503919再把0转12、1转11。因此该voice index分支来自原百分比RNG，非War/Army tactic ordinal，也非已证的战斗成功/失败。

voice-random-gate-static.json.gz保留完整指令、原EXE SHA及来源边界。没有为了获取语音执行原规则命令/RNG，未新增移动端重掷、hash选择或默认voice。MEDIA_INPUT_CONTRACT要求权威已提交presentationChoiceRaw和原caller身份；若没有已有事实，保持未绑定，不要求core为了声音另消耗RNG。批十的raw2只是人为输入的分支fixture，原4721d0不会返回2。

## 原部队主将与current cache

inspect_pc_unit_voice_caller.py在真实Scenario.s11 registry/vtable上完整执行564c00、56dba0、495a40、490b00、47a600/virtual4、4d19d0、4d1290、489030和原table选择。只供给音频可用性、捕获最终4d1000；没有人物有效性、主将、能力或选择器行为shim。472150/4721d0入口若被进入立即失败。

4020项通过：71原profile×8voice type×7能力严格比较/相等/unsigned-byte边界3976项，另覆盖原actor status−2/−1/0/8/9/255、actor17c0/1/−1、真实音频不可用、主将−1/1100及profile−1/71。每次完整3MiB原世界、原RNG和fixture UI receiver/payload前后字节一致；两次完整原执行报告字节一致。3990次实际原分派与现有生产PcVoicePolicy做host Java对照全部一致；30次静默/无效分支只由原caller证明，不造为有效播放。

具体源路径：receiver指向UI payload，payload+8 ushort为原部队index；56dba0取原部队，495a40读取unit+c主将nativeId，490b00查原人物。原演员有效性为statusRaw0..8或actor17cRaw非零；后者含义仍raw，身份批准不能替代动态有效性。489030直接读actor+170+index的已计算current byte，没调用成长/经验/官职重算。只证明此caller取主将，不能把四槽/双侧其它声音也套成主将。

unit-voice-caller-native.json.gz、unit-voice-policy-host.json、unit-voice-reproducibility.json及voice-source-incremental-guard-21.json保存全部输入/实际dispatch、比较和SHA。该unit caller的更上游动作→profile仍待原调用核对；其它caller可能先用RNG选profile，故下层readonly不等于整个语音系统没有随机依赖。

普通voice和BGM自动绑定仍0；无新APK/安装/混音声明，未移用批二十UI原sound1或更早adapter测试当voice正常还原。全人物动态形态、完整原音效/语音/BGM事件、正常多回合保存退出及ARM继续未完成，完整目标active。
