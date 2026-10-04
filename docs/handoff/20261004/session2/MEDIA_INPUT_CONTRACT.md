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

批十八补充587fb0的完整原分支：对于查询势力自己的单位，在其进入的原城市地域，对`4b5cc0(queriedForce,regionOwner)`为真直接成立；若原关系位`4811b0(queriedForce,regionOwner)`为真，须同地域存在`587b50`许可的另一关系组单位才成立。对于非查询势力单位，必须`4b5cc0(unitOwner,queriedForce)`为真，且它处在查询势力拥有的原城市地域。后者是反向关系，不能替换为查询势力对该单位owner的关系；同地域陪伴单位来自完整原单位列表，不由当前选中或屏幕可见单位猜。空列表为false。513项原代码fixture已验证，上述依赖身份/地域/有方向关系必须来自同已提交state；尚无普通场景输入时仍null。

媒体按原5880e0顺序消费已证明谓词：587f00为真→9；否则587d70为真→城市数>=10选10、其余8；否则587fb0为真→城市数>=10选11、其余7；全部已知为假时seasonRaw0..3→3..6，其它原值→7。更早的未知分支必须等待/resync，不越过未知选后面的曲目。不要求core产生音频编号、不调用规则命令/RNG/earn/save。原repeat1、fade500ms、gain sentinel-1属于媒体来源策略，保留用户实际音量。

批十九已在 app-owned PcMusicPolicy/PcMapMusicDirective 实现上述选择，并由 PcMediaPlayback.sourceMapMusic 只读消费同当前 StateToken 的订阅 parent/fact.id。4096组完整原 selector dispatch 与4125项 Java 对照通过；未知输入停止旧曲、不推断新曲。此入口仍等待正常场景的已提交事实 producer，没有把示例 scene 字符串或测试 adapter 标成普通BGM绑定。源身份/地域/日历及真实场景契约要求保持不变。

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

批二十一静态核验已解除上述raw的部分未知：`5038b4`压入50，`5038d8`调用原`4721d0`百分比RNG，其正阈值路径读取并写回`8a5d44`（uint32乘0x6c078965加0x3039，取高16位%100与阈值比较）。返回只为0/1，因此这里是原随机选择11/12，不是已经证明的战斗成功/失败。此RNG从未执行；不能由媒体抽取规则RNG、重掷、默认11/12或用fact hash制造选择。若未来恢复该具体调用，需由权威提供**已提交、同state/id/parent的presentationChoiceRaw0/1及对应原调用身份**，缺失则未绑定；不要求规则所有者为了音频新增随机计算。旧raw2行只是批十明确fixture的分支演示，不是4721d0可返回2的证据。

批二十一另已完整执行只读`564c00 ->56dba0 ->495a40 ->490b00 ->4d19d0 ->4d1290 ->4d1000`：UI payload+8是原ushort部队index，原部队+c是主将nativeId；此具体调用的speaker是原主将，不能扩大为全部其它voice caller均由主将发言。原actor virtual4的有效性要求statusRaw(+a0)为0..8或actor17cRaw非零，不能仅凭批准身份/姓名/当前选中认定可发言；actor17c含义未命名，保留raw。原489030直接读取actor+170+index的已计算current byte，未触发重算或RNG。4020项原执行、3990次实际分派与现有Java政策一致，三MiB原世界/RNG/receiver前后字节一致，无人物有效性/能力getter shim。

这条unit caller的只读输入应提供实际已提交的`actorUnitId/speakerRole=unitLeader/speakerOfficerId/currentAbilityBytes[0..3]/sourceActorValidRaw`及原动作→profile调用证据；主将角色是具体源调用依据，native部队slot与项目unit.id不能直接等同。能力从同state的已提交query/事实读取，不由媒体重算官职、经验、伤病或成长。更上游仍可能选择profile时调用RNG，不能把下层readonly扩大为所有上游都无需随机事实。缺少原动作profile及实际parent时维持未绑定。

批二十二补充原显示callback564cb0：其读取renderer+4的raw动作域6..28（22..25静默），FSM阶段另在renderer+c；不可将两个域混用。活跃分支先用402310(2)的0/1结果选profile，再从具体renderer所指部队取主将。此helper进入4442a0/444150，使用8a5b68计数及6ed37f0的624word状态，与批二十一8a5d44标量百分比RNG不同；本批只读取指令，未执行任一随机源。需要同已提交演示fact中的`rendererActionRaw/rendererChoiceRaw/choiceSourceCall="402310->4442a0->444150"`以及实际该renderer的部队/主将身份，不能用War.Ordinal或当前选中部队补造。

voice-renderer-dispatch-summary.json列完整raw分派，例如raw6在choice0/1为profile37/40，而raw7为37/39；这不是THRUST/SPIRAL对应关系已经成立的证据。原同回调还按renderer240Raw的零/非零分派：零值effect46/sound49，非零effect78/sound78；该字段语义仍未知，不改名critical/success。156项原指令执行将已产生的随机结果作为明确输入边界、捕获voice/effect/sound sink；不执行规则/RNG或将此fixture当正常动作绑定。完整动作身份、阶段条件和已提交choice仍需上游来源闭合。

批二十三进一步执行原570490的初始化部分及四个初始化器的实际字段写入：record+50 raw0/1/2/17→rendererActionRaw6/7/8/27。renderer从record+58的原坐标，经原格表unit slot取manager中的对应对象；不是从屏幕选择或直接把记录+5c当发言部队。+5c在57050c之后用于另一阶段，该阶段还有位移路径/效果输入，未执行。需要已提交原演示记录的`record50Raw/record58CoordinateRole/record5cCoordinateRole/actualRendererUnitId/speakerOfficerId`或已有同等事实；坐标角色、record50命名空间尚需原记录生成链，不因其数值与原战法表相近就命名THRUST等。四条显示路由及其profiles具备原执行证据，仍不是工程战法ordinal绑定。

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

对原5038d8随机路，补充nullable `presentationChoiceRaw: 0|1|null`及`choiceSourceCall: "5038d8->4721d0"|null`；这两项必须来自已提交记录，不能从success/critical/sideRaw转换，也不能由媒体创建。原unit caller则需要nullable `sourceActorValidRaw`或精确原statusRaw/actor17cRaw投影；未知拒绝普通voice，不以演员身份已批准替代原有效性。

只投影已有已提交事实；若没有可证的原 side/slot 或上游 event 映射，保持 null/未绑定，不调用规则命令/RNG来决定。`517cd0` 的另一链按 side0/1从原域 `+0x10+side*0xa0` 读取实际人物，并把原 eventIndex+13作为profile传给feedbackB；其0..57动作语义仍未知，不先标为计略/单挑或按成功失败推断。

上述nativeProfile/selectorIndexRaw只在媒体端已有原绑定证据时产生，不要求core保存/生成音频编号；规则所有者所需提供的是实际提交的动作、结果、发言者/角色、完整state与逐次id。既有事实足够则直接消费，不能为音频重复求值成功、暴击或人物槽选择。

## 场景音乐与生命周期

媒体入口接收显示场景身份（菜单/正常地图/单挑/舌战/原演示）、完整 state 和原已核实场景选择器参数。scene identity 的来源必须是实际已建立的显示场景，不能从“新游戏”按钮点击提前播放地图音乐。音乐编号/循环/语言/声源选择属于 media，不放进规则保存或 RNG。

新局/读档/退出变更 generation 时清所有待播瞬态；恢复仅设置场景音乐基线，不补播奖励或战斗。事实与 NET 奖励只选一条线路。音乐长流、短音效/语音分别调度；所有未知绑定以 manifest unknown 保留。

## 公共入口与顺序集成

批二十提出唯一公共补丁`ui-close-host-20/ui-close-host.patch`，基点4994350dd1f0ad661c97379f33997f8fca904e58，守卫见同目录guards.json：只在MainActivity.trackDialog注册真实AlertDialog.OnCancel回调，交给SoundEffects.cancelledDialog。不在普通dismiss、按钮文案、战斗失败或规则命令上触发；按钮确认关闭不走OnCancel。现有文件没有OnCancel listener，若新增监听或SHA变化则停止直接套补丁并顺序审计。

原4dd8c0的flags8且flags2未设时发sound1、关闭返回值−2；63b270的child1237同样sound1/−2，而child1236发sound0/10000。69项完整原音效dispatch执行及原PCM已证明。Android的真实取消回调适配原负向关闭声音；这是明确的平台UI适配，不推导规则结果。原OS事件路由/完整子控件布局尚未执行，通用按钮仍保留移动端合成，不能宣称全部原UI矩阵已闭合。该补丁先记录并提交，再在自有integration工作目录按before/after SHA顺序应用；不触碰会话一MainActivity或原目录WIP。

MainActivity/MapHost 等入口已经在独立 integration 工作目录依守卫顺序接入头像、逐次技巧事实及原 CriticalHit.year。对应 `.patch`、基点及每路径前/后 SHA 保留在本目录；人物 metadata 仍精确继承已完成9e171f2，未拷其后来WIP。任何新的 BGM/voice 公共入口仍先提供确切补丁，由顺序集成处理。批九只有媒体选择器/工具/证据，没有再修改这些公共入口，也没有增加任何正常语音触发。
