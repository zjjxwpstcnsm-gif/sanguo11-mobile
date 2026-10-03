# 原能力研究参数、培养准入与奖励块

2026-10-04。只读原程序离线受控执行，不启动Wine，不称完整PC流程或有效MOD身份已闭合。

来源：`san11pk.exe` SHA256 `30d33b44876b84a8e87570873a86de88c65d2491c7e1cdeeb5883dc4b12feefb`；`Media/scenario/Scenario.s11` 47928字节，SHA256 `dfca0316e019454e77bab5805e3d725ae35e34408e1699e869e5419e2f5fad6f`。98行位于共享尾表table_86dd8，每行保留原文件offset、字节数、record SHA、读取字段、native index和实际actor。原运行时表root+86dd8、stride6c；显式执行494f50传入序号，不冒称整个postload注册已验证。

原getter494f80将+44编码映射为次数，前15行实际为5／5／3；原始3／3／2不能直接导入。494fb0返回能力索引0—4，494fd0返回70／80／95；494ff0、495010返回适性索引／目标等级。98×5=490次原getter全部只读，完整3MiB世界及RNG不变，没有非栈写入。两次最终JSON SHA `2850fe6805d0054b4a512aa0d45c50716d68c45e593d704dcd1e6377d3e04841`。

原49dc60 STAT分支调用489180读officer+12a的XP，达到2000即拒绝；随后48a390取年龄成长与XP合成能力，必须小于档位上限。它不使用伤病、官职或配偶修正后的当前值。1080例覆盖5项×3档、年龄／成长输入、档位邻值、XP0／99／100／1999／2000／2100；完整世界和RNG不变。受控例：基础70、curve0、20岁、XP0的合成值67允许；基础69、curve0、70岁、XP100的合成值70拒绝。date-source flags为显式输入，不声称其设置名或默认值已核实。

在真正函数5d91a0中，5d9268至5d92ca的完成奖励块取得同一原能力索引、档位和合成能力，通过4a55a0→48a810写入：`newXP = min(3000, oldXP + 100 * min(5, cap - foundation))`。只执行准入允许的300例；完整3MiB世界对照仅该XP与两处当前缓存变化，基础五项和RNG保持。正常流程中的次数扣减、任务释放及UI位于停止边界之后，没有伪造返回值或绕过原setter。此结果证明奖励块，不能代替整个培养命令和回合完成验证。

当前安卓AbilityResearch写base、按base检查上限、维护独立gains20累计；与上述原语义存在差异。用户交接明确要求继承培养写base，本次保留全部实现和旧档含义，不反推基础值或迁移人物。未来必须以明确的独立版本策略处理新局原培养语义，并保留历史自定义身份、XP及v31—33旧数值；不能用替换目录或静默修改旧档解决。完整费用、研究／培养时间、次数扣减、隐藏选择RNG、AI、失败／取消／失守语义继续核实。

复现（输出须为新目录；依赖只读现有工具运行时）：

```sh
export PYTHONPATH=/Users/paopao/workspace/sanguo11-mobile/out/toolchain/pc-emulate:/Users/paopao/workspace/sanguo11-mobile/out/toolchain/pc-inspect
python3 tools/content/inspect_pc_ability_research_native.py --output out/parity/ability-parameters-new
python3 tools/content/verify_pc_ability_training_admission_native.py --output out/parity/ability-admission-award-new
```

最终观察保存在`out/parity/ability-research-source-20261004`，包括逐行映射、函数字节SHA、世界变更检查、最初准入观察与后续奖励观察。参数名称／描述用于来源核对，不作为完整奖励执行证据。没有修改Android规则、存档、UI或PC资源。
