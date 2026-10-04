# 批九：原人物语音选择与实际发言侧调用链

完整目标仍active。承接批八bc13ae3b，保持core/game-api/game-runtime及人物metadata与已完成9e171f2字节一致，未修改MainActivity、公共地图规则、人物文本数值或共享manifest。最新已安装且验证的生产包仍是批八1003574e...，本批原语音选择研究不得冒充正常语音/BGM播放验收。

inspect_pc_voice_policy.py加载校验过的PC EXE原PE节，在原0x4d1290、0x4d13b0、0x4d1490执行71组profile×8种actor voiceType；覆盖严格能力大小/相等/无符号字节极值、两种反馈及非法profile/type/actor/feedback边界，共8248项PASS。只有明确的actor validity和当前已计算能力getter为只读桩，所有原type/profile/variant表与选择指令保持不变。人物voiceType字段偏移为**十六进制0x100**。八种type并非人物共享一种声源；type6/7有原12/13 variant覆盖，且能力选择与反馈选择各自遵守不同原表。原机器码SHA和逐行结果保留于voice-native-policy.json.gz。

stage_pc_voice_policy.py从该实际执行结果生成纯媒体PcVoicePolicy。该类只接受明确nativeProfile/type/current unsigned byte能力或raw feedback，拒绝未知与非法输入；无World、规则命令、RNG、身份猜测、动作名称或自动播放。verify_pc_voice_policy.py将所有8240个原执行向量逐项与生产Java比较，全部PASS。native policy和生成Java重复转换字节一致；不是手写移动端预期测试。

inspect_pc_voice_callers.py进一步实际执行两条原演示调用链，共1850项PASS。战法链503b32→4fd630→505f70→50c0d0→4d1a40→4d13b0→4cffd0→4d1000覆盖832个13种tactic×两侧×四人物槽×八voiceType组合；侧与槽的原记录读取运行原机器码，八个独立人物指针/type用于排除借用主将/另一侧身份。tactic0..12的原表在本EXE中映射profile0..12。**同一个side0/1既选实际人物侧又进入语音反馈分支，不能将其全局当作success/failure。**

第二链51d002→517cd0→51e350→4d1aa0→4d1490→4cffd0→4d1000覆盖928个raw event0..57×两侧×八type；按原侧记录读取实际人物，并以event+13选profile。动作名称和上游侧生成仍待核实，不先归为计略/单挑。附90项边界/actor拒绝。显式构造原演示域、memory/actor validity/audio availability桩及最终voice/volume捕获完整列于voice-native-callers.json.gz；没有运行原规则/RNG/Wine，也没有实际PC后台播放。两份caller报告再次运行压缩字节完全一致。

初次probe按页逐个映射PE段，进程采样证明耗时在Unicorn内存拓扑；只终止自己的probe并改为同原PE字节的连续段映射，保留失败记录与采样SHA。第二链初次因原音频可用性守卫未派发；增加并明确记录backend availability=1桩后才接受，未跳过原选择逻辑。不得将这两个初次失败包装为通过。

MEDIA_INPUT_CONTRACT.md给出具体已提交VoicePresentationFact输入需求：实际speakerOfficerId、原side/slot/native动作或profile、完整StateToken和id/parent/presentationParent。现有actorCopy与CriticalHit不能自动等价于该原四槽位发言侧，不自行改core产生新语音事件。未知继续未绑定，NET与逐次事实不会双播。

本批没有新增正常语音触发，没有新的实际voice PCM、原BGM场景身份或共享音频focus验收。批八已安装APK及用户保存恢复证据仍按原范围保留；全部portrait战斗/对话/单挑形态、MOD优先级、其它15来源正常流程、原BGM/全部语音/其余9原SFX、完整多回合保存退出/ARM扬声器等仍未完成。下一步继续原演示记录生成与已提交事实的精确对应，并补齐真实播放器/正常流程。
