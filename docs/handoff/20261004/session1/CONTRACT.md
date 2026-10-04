# 人物 metadata 与媒体连接契约

Session 1 负责身份与文字/数值真值，Session 2 负责头像像素、年龄变体与加载、音频。

来源键 `sourceVariant` 由安装相对路径的安全 slug 和完整文件 SHA 组成；`sourceVariant + nativeId` 是不可合并的来源记录键。剧本稳定 ID 含文件 slot 与 SHA，不能用重复的内嵌编号去重。

`officerId` 只来自姓名、生年、性别的身份校验或另有明确身份依据；未映射为 null，不把候选 native 编号当项目 ID。Scen014 宋憲/徐榮换位逐源映射。原 64 条字形缺口保留，不能用 Big5HKSCS 的不同字形蒙混。

后续只读输出 `officer-metadata-manifest.json` 与 `portrait-requirements.json`，字段为 officerId/nativeId/sourceVariant/sourcePath/sourceSha256/recordSha256/nameBytes/identityStatus/faceNativeId/birth/sourceScenarioDate，并附字段 coverage/unknown。请求清单不决定像素和年龄阈值，媒体会话核实选择；未映射行不能覆盖现有人物身份。

媒体输出独立 media manifest，按三元组连接，不改本 metadata。媒体可读取本独立目录 `docs/handoff/20261004/session1/` 的已提交契约与清单。新增头像 API 如必要，在本文件先登记，由媒体实现。

人物列表、搜索、详情读取相同权威 DTO，携带完整 StateToken。查询不消耗 RNG、不生成 ID、不提交事件。旧档缺失来源字段保留 unknown，不能从新目录静默换将或补值；人物 base/growth/XP/current、各保存策略完全保留。

最终整合按双方基点/完成提交/前镜像 SHA 守卫顺序处理；不复制另一会话 WIP，不并写共同人物资产 manifest。

## 首批权威人物查询

新增 `game-api/.../OfficerSnapshot.java`、`game-runtime/.../query/OfficerQuery.java`、`game-runtime/.../OfficerQueryTest.java`；GameApi/GameSession 增加只读 officers()。DataTable.java 仅增加人物 DTO 重载，OverviewUi 仅武将表接入，MainActivity 仅增加人物查询方法和改写 officerDetail 的文字来源。game-runtime/build.gradle 注册实际 main-style 测试；修改前 SHA 在 identity-dto-before.json。

本批读取保存中的当前人物事实，不查询 ContentCatalog 追填原姓名/生卒/传记。旧档未知 base/growth/XP 的数组为空，不将旧 current 冒充原 base。sourceVariant/nativeId/courtesyName/biography 尚未持久化，一律未核实；这不是来源新局转换完成。正常武将一览搜索/排序与详情共同消费 DTO，头像和定位保留原只读 World 对象接口。
