# 只读媒体输入契约请求

归媒体实现的选择器/播放器不接收规则命令或可写 World，不调用 RNG/earn/save。如下是请求输入，不是已存在接口，也不是给 core 添加音频编号。

## 普通音乐上下文（批十五补充）

已以原代码及全部16个来源核验4488项（music-context-source-native.json.gz）：原城市getter490a10只列42城、virtual40确为原47b2b0势力归属getter；491270把原势力pointer映为native force0..46，49d670以有效势力拥有至少10座原城为真；4825a0在已核验的calendarOffsetRaw=0日期将1–3/4–6/7–9/10–12月映为seasonRaw0/1/2/3。原caller57fc70调用virtual48=480fa0，只有势力+60的controlSlotRaw0..7才进入5880e0。nativeId不是项目player/owner ordinal，不能从名称推断。

建议由规则/metadata所有者提供同StateToken的已提交只读投影，媒体只负责原音乐选择和播放：

```
MusicSourceContext {
  state: {sessionId: string, generation: long, revision: long},
  id: string, parentId: string, presentationParentId: string,
  sourceVariant: string|null,
  sourceForceNativeId: int|null,
  sourceFactionValid: boolean|null,
  controlSlotRaw: int|null,
  sourceCalendar: {year: int, month: int, day: int,
                   calendarOffsetRaw: int|null, seasonRaw: int|null},
  sourceCityOwnership: [{sourceCityNativeId: int, sourceForceNativeId: int|null}],
  predicate587f00: boolean|null,
  predicate587d70: boolean|null,
  predicate587fb0: boolean|null,
  sourceSceneBinding: string|null
}
```

sourceForceNativeId/sourceCityNativeId由会话一批准的来源身份映射决定，sourceCityOwnership只计实际原城市0..41，关港与自定义城市不得悄悄混入原42城阈值。calendar必须取实际已提交时钟，不由媒体从turn重新计算规则日期。calendarOffsetRaw或scene未知保持null；sourceSceneBinding须对应真实原调用场景，不能填“菜单”来套用未证明的12/16分支。predicate字段是精确契约位置，三者尚未在移动端规则投影闭合；null不是false，不默认选择季节音乐。

批十六补充原分支所需下层只读事实（不是已接入DTO）：每个原城市地域的`selfSumRaw`、`relationBitSumRaw`、`otherRelationSumRaw`，各来源与587a00的原分类/地域判定一致；不要直接把屏幕可见部队或当前选中部队作为全地域汇总。587f00的城市分支要求有效己方原城、selfSumRaw<10000且3×(selfSumRaw+relationBitSumRaw)<otherRelationSumRaw；587d70允许己方或按`4b5cc0(cityOwner,queriedForce)`方向许可的原城，要求selfSumRaw>10000且3×(selfSumRaw+relationBitSumRaw)>otherRelationSumRaw。10000和比例等号均不成立。字段名称暂保留原关系位分类，不将所有非己方静默当敌方。

建筑优先分支需要原地点的`currentHpRaw`（原signed short）、`maxHpRaw`、`troopsRaw`、`energyRaw`、实际来源owner及`nearby587b50Raw/nearby587c00Raw`。原条件是耐久严格低于maxHpRaw/5（原整数除法）、troopsRaw<3000、energyRaw<30，再按归属/原方向关系及nearby条件决定；不是“本次战斗失败”或“当前按钮取消”。这些下层事实/587a00汇总与附近地域判定尚未全部闭合；本批只验证原条件及边界，未知继续null，不用媒体重算规则战斗/外交/RNG来补造。

批十七在真实原单位registry/vtable/getter/外交位/地域表上执行原587a00/587b50/587c00，736项无行为shim验证补充：`587a00`只汇总属于目标原城市地域的单位，依据queriedForce→unitOwner的原关系位分类并读取原ushort兵力；同势力优先，关系位组其次，原4b5cc0许可组随后，中立/计数不许可组不并入。`587c00`要求同queriedForce单位存在于目标地域，`587b50`要求按queriedForce→unitOwner方向许可单位存在。此前城市分支587d70使用的是cityOwner→queriedForce，不能把两个方向混用。

原地域不是可见范围或附近六角半径：单位virtual3c指向+3c的signed-short坐标，按20×(x×200+y)读取格数据bits5..11，再经原4839f0的byte表映射到城市地域。150,150的远单位只要属于相同原地域也被计入。只读投影应提供每单位`sourceUnitNativeId/sourceOwnerNativeId/sourceRegionCityNativeId/troopsRaw`、源地域表身份与有方向的`relationBit/truceCounter`，或由规则所有者提供与这些事实严格对应的已提交原汇总；不由媒体按屏幕距离代替。fixture证明原算法，不证明当前Android地块或项目owner已具相同来源身份，缺失仍null。

媒体按原5880e0顺序消费已证明谓词：587f00为真→9；否则587d70为真→城市数>=10选10、其余8；否则587fb0为真→城市数>=10选11、其余7；全部已知为假时seasonRaw0..3→3..6，其它原值→7。更早的未知分支必须等待/resync，不越过未知选后面的曲目。不要求core产生音频编号、不调用规则命令/RNG/earn/save。原repeat1、fade500ms、gain sentinel-1属于媒体来源策略，保留用户实际音量。

当前GameSnapshot只含turn/player及BridgeEntity的项目owner等普通字段，缺少calendar和来源势力/城市身份，不能据此证明上述原音乐上下文。新增JSON应为app-owned加法字段，保持schema1和long精确整数；快照用于基线/resync而非重播音乐/语音事实。同一已建立scene/track不重开，restore或新局取消旧scene；最终需正常新局/多回合/读档实测，解码和adapter通过不能代替普通绑定。

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

批十进一步确认：上段旧探针名 `tacticIndexRaw` 实为未命名的十三路语音选择索引，**没有证明它等于工程War/Army战法枚举ordinal或原战法编号**。原`5038e0..503940`在已产生的上游raw结果为0时传12、非0时传11到record构造器；`4fe455..4fe4f7`执行原字段写入，参数1/2/10分别进入record+0/+4/+0x98，最后才由503b32传到4fd630。进入点位于4721d0调用之后，没有执行该调用/规则/RNG，raw含义仍未知。因此禁止按`SPIRAL.ordinal()`等选voice profile。voice-record-fields.json保留源指令/明确边界。

需先核对原上游演示记录 `+0/+4/+0x98` 的生成，与现有已提交 tactic enum/strike/plotOutcome 的对应。如果现有事实不能表达，向规则所有者请求如下只读事实投影（此处为请求，不修改 core）：

```
VoicePresentationFact {
  id, parentId, presentationParentId,
  state: {sessionId, generation: long, revision: long},
  selectorCall: "4fd630" | "517cd0" | "4d19d0",
  nativeProfile: int | null,
  selectorIndexRaw: int | null,
  actualAction: existing committed enum,
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

上述nativeProfile/selectorIndexRaw只在媒体端已有原绑定证据时产生，不要求core保存/生成音频编号；规则所有者所需提供的是实际提交的动作、结果、发言者/角色、完整state与逐次id。既有事实足够则直接消费，不能为音频重复求值成功、暴击或人物槽选择。

## 场景音乐与生命周期

媒体入口接收显示场景身份（菜单/正常地图/单挑/舌战/原演示）、完整 state 和原已核实场景选择器参数。scene identity 的来源必须是实际已建立的显示场景，不能从“新游戏”按钮点击提前播放地图音乐。音乐编号/循环/语言/声源选择属于 media，不放进规则保存或 RNG。

新局/读档/退出变更 generation 时清所有待播瞬态；恢复仅设置场景音乐基线，不补播奖励或战斗。事实与 NET 奖励只选一条线路。音乐长流、短音效/语音分别调度；所有未知绑定以 manifest unknown 保留。

## 公共入口与顺序集成

MainActivity/MapHost 等入口已经在独立 integration 工作目录依守卫顺序接入头像、逐次技巧事实及原 CriticalHit.year。对应 `.patch`、基点及每路径前/后 SHA 保留在本目录；人物 metadata 仍精确继承已完成9e171f2，未拷其后来WIP。任何新的 BGM/voice 公共入口仍先提供确切补丁，由顺序集成处理。批九只有媒体选择器/工具/证据，没有再修改这些公共入口，也没有增加任何正常语音触发。
