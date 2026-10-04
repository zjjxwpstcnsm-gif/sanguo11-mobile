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

媒体源已经读出每份原 actor 的 `+100` voiceTypeRaw，并执行原 `4d0010` 八类输入；不猜所有人物共用声音。现有只读人物 DTO 若有 sourceVariant/nativeId，即可由 media manifest 提供此值，不要求 core 写入播放字段。

战斗/战法/计略只消费已提交 TurnJournal/GameEvent：需要稳定 parentId/presentationParentId、实际 actorOfficerId、实际 tactic/plot 枚举、已算出的 success/critical/reflected/chained，以及对应已提交 state。actorOfficerId 必须是实际发言者，不能用当前选中按钮/目标部队猜；受击、设施或副将需要另外记录真实发言者，不假定主将包办。若当前事件已包含这些字段，直接复用，不新增重复规则事件。

原 `4d1290` 语音分支还比较实际 current 武力与统率/智力/政治；媒体只读同 state 的当前能力数值，不能重复计算成长/官职/经验。原 `4d13b0/4d1490` 使用实际事件类别及两种反馈索引，不凭文案判断成功。所需动作类别尚须与原正常调用逐项核对；未知类别保持未绑定。

## 场景音乐与生命周期

媒体入口接收显示场景身份（菜单/正常地图/单挑/舌战/原演示）、完整 state 和原已核实场景选择器参数。scene identity 的来源必须是实际已建立的显示场景，不能从“新游戏”按钮点击提前播放地图音乐。音乐编号/循环/语言/声源选择属于 media，不放进规则保存或 RNG。

新局/读档/退出变更 generation 时清所有待播瞬态；恢复仅设置场景音乐基线，不补播奖励或战斗。事实与 NET 奖励只选一条线路。音乐长流、短音效/语音分别调度；所有未知绑定以 manifest unknown 保留。

## 公共入口与顺序集成

MainActivity/MapHost 当前未改。媒体队列与接口完成后先提供具体 `.patch`、基点及每路径前/后 SHA；人物页面会话完成后按顺序合入，不能用媒体工作区的旧页面替换其新 DTO/UI。当前 INTEGRATION_REQUEST.md 是明确需要的入口位置和语义，尚不是可应用的最终补丁。
