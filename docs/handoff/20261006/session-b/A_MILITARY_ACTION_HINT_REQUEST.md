# A适配请求：施工行动提示

实际 APK54 Source14普通新局→出征→营垒→中止→整旬→补修，截图 `out/session-b/fieldwork-repair-ui-54/04b-resumed-repair-live.png` 显示“补修阵·耐久512/1100”和“中止施工”。同一页面却显示“本旬已行动，攻击后不能再移动”，这次实际命令是补修。

A所有位置：当前 `MainActivity.java:1053` 的 `u.acted` 文案。B不编辑该文件，不改变行动规则。

现有已完成只读事实：`GameSession.sceneFacts()` 返回的 `SceneFactsSnapshot.military` 内含 `builderUnitId`、`hp`、`maxHp`、`complete`、`cell`。`StateToken` 必须与显示的单位事实一致。`builderUnitId == unitId` 表示该部队承担施工/补修；不能把所有 `acted` 解释为攻击。

建议本旬已行动且有施工事实时显示“本旬已用于施工/补修，施工将占用后续行动；可中止施工”。仅知道 acted 而无确切命令事实时显示“本旬已行动，不能再执行本旬行动；可安排下旬行军”。不要从渲染反写规则、恢复行动或重抽随机数。具体是否允许安排下旬行军继续消费现有权威校验。

依据为已提交 SceneFacts DTO，不依赖当前单挑/物品/raw-loyalty WIP。APK54完整七旬、冷重开及15内部/772外部文件SHA全恢复均已通过。B完成批次提交 `c6f966ce378619b5ebfdb1bb3725c6f4c494e4e4` 仅含两条军建生产路径；本提示适配仍由A修改和自行验收。
