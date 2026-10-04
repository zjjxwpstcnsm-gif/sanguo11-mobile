# 只读媒体输入契约请求

归媒体实现的选择器/播放器不接收规则命令或可写 World，不调用 RNG/earn/save。如下是请求输入，不是已存在接口，也不是给 core 添加音频编号。

## 人物来源

从已提交 StateToken 的人物查询取得：

```
OfficerMediaIdentity {
  state: {sessionId: string, generation: long, revision: long},
  officerId: int,
  nativeId: int|null,
  sourceVariant: string|null,
  faceNativeId: int|null,
  birth: int|null,
  currentYear: int,
  sex: source integer|null,
  identityStatus: string,
  customPortraitRef: string|null
}
```

nativeId/sourceVariant 必须来自保存的实际来源绑定或明确的已提交新局来源；旧档缺失保留 null。不从姓名猜 nativeId，不把 roster ID 当 Face ID，不将各版本合成单一人物源。自定义图像/ref 继续优先。媒体以三元组精确连接 session1 已提交清单，核验原 face/age/lookup，加载独立媒体 manifest 对应的原像素。sourceVariant 未知时不能静默选择 Scen000 或任一 MOD。

原 `48a450` 普通 face lookup 与 `48a5b0` 战法动态 slot lookup 分开输出。FCE 文件小图是交错存放，原 registry 索引为 `2400+2×faceId+form-1`；form 的实际 UI 调用语义仍在取证，不按职业/画像相似度推断。

## 语音选择所需事实

媒体源已经读出每份原 actor 的 `+0x100`（十六进制偏移）voiceTypeRaw，并执行原 `4d0010` 八类输入；不猜所有人物共用声音。现有只读人物 DTO 若有 sourceVariant/nativeId，即可由 media manifest 提供此值，不要求 core 写入播放字段。

战斗/战法/计略只消费已提交 TurnJournal/GameEvent：需要稳定 parentId/presentationParentId、实际 actorOfficerId、实际 tactic/plot 枚举、已算出的 success/critical/reflected/chained，以及对应已提交 state。actorOfficerId 必须是实际发言者，不能用当前选中按钮/目标部队猜；受击、设施或副将需要另外记录真实发言者，不假定主将包办。若当前事件已包含这些字段，直接复用，不新增重复规则事件。

原 `4d1290` 语音分支还比较实际 current 武力与统率/智力/政治；媒体只读同 state 的当前能力数值，不能重复计算成长/官职/经验。原 `4d13b0/4d1490` 使用实际事件类别及两种反馈索引，不凭文案判断成功。所需动作类别尚须与原正常调用逐项核对；未知类别保持未绑定。

批九新增原调用执行证据：`503b32 ->4fd630 ->505f70 ->50c0d0 ->4d1a40 ->4d13b0 ->4cffd0 ->4d1000`。`4fd630(sideRaw,officerSlotRaw,tacticIndexRaw)` 对 side0/1、slot0..3、tactic0..12做原范围检查，从实际演示域 `+0x24+side*0xec+slot*0x40` 的记录取得人物，原表 `0x838c94` 在本 EXE 中将 tactic0..12映射到 profile0..12。**同一个 sideRaw 同时进入人物侧选择和 feedbackA；它不是已经证明的成功/失败布尔量。** 当前 `TurnJournal.Event.actorCopy()` 可提供实际行动部队，但不能凭主将代替原演示域指定的四槽位发言者。CriticalHit 的 officerCopy 则只证明暴击头像事实，也不足以补造每条原语音发言侧。

需先核对原上游演示记录 `+0/+4/+0x98` 的生成，与现有已提交 tactic enum/strike/plotOutcome 的对应。如果现有事实不能表达，向规则所有者请求如下只读事实投影（此处为请求，不修改 core）：

```
VoicePresentationFact {
  id, parentId, presentationParentId,
  state: {sessionId, generation: long, revision: long},
  selectorCall: "4fd630" | "517cd0" | "4d19d0",
  nativeProfile: int | null,
  nativeTacticIndex: int | null,
  nativeEventIndex: int | null,
  speakerOfficerId: int,
  speakerSideRaw: int | null,
  speakerSlotRaw: int | null,
  sourceVariant, nativeId,
  currentAbilityBytes: int[4] | null,
  mediaPhase, sequence: long
}
```

只投影已有已提交事实；若没有可证的原 side/slot 或上游 event 映射，保持 null/未绑定，不调用规则命令/RNG来决定。`517cd0` 的另一链按 side0/1从原域 `+0x10+side*0xa0` 读取实际人物，并把原 eventIndex+13作为profile传给feedbackB；其0..57动作语义仍未知，不先标为计略/单挑或按成功失败推断。

## 场景音乐与生命周期

媒体入口接收显示场景身份（菜单/正常地图/单挑/舌战/原演示）、完整 state 和原已核实场景选择器参数。scene identity 的来源必须是实际已建立的显示场景，不能从“新游戏”按钮点击提前播放地图音乐。音乐编号/循环/语言/声源选择属于 media，不放进规则保存或 RNG。

新局/读档/退出变更 generation 时清所有待播瞬态；恢复仅设置场景音乐基线，不补播奖励或战斗。事实与 NET 奖励只选一条线路。音乐长流、短音效/语音分别调度；所有未知绑定以 manifest unknown 保留。

## 公共入口与顺序集成

MainActivity/MapHost 等入口已经在独立 integration 工作目录依守卫顺序接入头像、逐次技巧事实及原 CriticalHit.year。对应 `.patch`、基点及每路径前/后 SHA 保留在本目录；人物 metadata 仍精确继承已完成9e171f2，未拷其后来WIP。任何新的 BGM/voice 公共入口仍先提供确切补丁，由顺序集成处理。批九只有媒体选择器/工具/证据，没有再修改这些公共入口，也没有增加任何正常语音触发。
