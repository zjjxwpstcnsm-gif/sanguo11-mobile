## 2026-09-22 S05 checkpoint — PARTIAL

Continue PR #62 / agent/3d-s01-renderer-foundation; main unchanged.
Delivered moving-unit pose/selection/labels and detached real equipment/state, plus
wounded playback clone correction. S04 final art and full S05 models/clips remain blocked.
See docs/3d/acceptance/s05-status.md for exact coverage, regression results and untested gates.

# v0.62 road-only candidate — incomplete overall task

- DONE: full v061 inherited by merging PR49 only; four production ordinary-road render paths changed; existing UI model regression passed.
- DONE: road-only candidate built from the BUILD_COMMIT recorded in last-run.json, installed and pixel-tested; standalone prerelease APK re-downloaded with identical hash. Full v0.62 remains BLOCKED.
- BLOCKED: all14 VOID repairs and generated runtime art; do not claim completion or use the generated poster as evidence.
- Geography61 / art56 / SaveCodec31 retained; branch PR must remain open.

---

# v0.61 — 两角水面与界外呈现；已安装 APK 已发布

## DONE
- 承接已合并PR48的main049652d45641ff247c964ec30f70f5cff5c629f3；本轮分支agent/map-corners-boundary-v061，不回退、不强推。实际解压读取本轮7200×6752原图并核验指纹，未分发原图/裁片。
- 206个唯一全国源格V→Q：东南41、西北56、北侧107、永安内部2。1051个原VOID仅添加明确范围的外景显示：东南海面869、西北荒原126/山体56。两类数字分开，不重复计算以前版本。
- 200×200 odd-q、World-aware坐标、87据点、591开发位、七格城、CityAtlas56、存档31及已有游戏功能保留；全国源坐标展示统一。外景不改变inside/sourceInside，不参与领土/寻路/开发/点击，不覆盖未知格或旧版存档。
- 正式功能提交9b69f6fd09cd15a75e09666ec12ef0f39575dd7e已提交推送；后续仅androidTest和CI触发修正。最终构建源9ad16d82c5a33211d28fa84e677b53e155f535b5；assembleDebug、模型、实际安装、发布、回下载核验在run35559224769全部SUCCESS。
- APK v0.61.0/code61，22339175 bytes，SHA256279290bc30c1c5f7e5f8b9d4b12d2cde951f8382cf975f71a585e2641d2d6905；独立Release资产已发布。安装、发布回下载及本会话本地复核字节一致。
- API29/x86_64模拟器实际签名覆盖安装，旧手动存档字节未改变；Reference61最终9866条PASS及native200操作92项。四剧本、两角完整视野/LOD/小地图/势力色/开局预览、行军、下一旬、出征、保存重读及12份真实58/59/60旧档通过。未将模拟器当物理ARM验证。
- 最终截图已实际查看；与旧v060同中心/相机/LOD/分辨率完整对照：东南旧整片黑洞消除，西北整片碎裂显著修复但14格局部黑孔仍可见。无裁切隐藏。
- PR49已创建指向main，保留待审未合并。最终文档提交与BUILD_COMMIT分开，不要求文档SHA冒充APK源SHA。

## IN PROGRESS — 地图考证，非构建状态
- 西北14格仍有视觉缺口，本区域尚未全部完成；源坐标x18–38,y14–24，逐格见docs/validation/v061/README.md及data/map/reference-v061/remaining-void.json。没有将它们标成外景或填成水/山。

## TODO / BLOCKER
- 缺无遮挡原图或原版地形/有效格表；精确原版边界/航行权限与部分栈道/地形细分待证。206新Q原版通行权限未确认，继续禁行；100外景材质估计不能冒充恢复原版陆地。
- 物理ARM/FPS、全部历史测试套件未认证。当前APK交付没有待执行阻塞；上述是考证/设备覆盖范围限制。
- 新开局使用revision61。旧档保留自身地形/revision/预算/单位，禁止删除或静默迁移，不把新版外景强套旧档。

[独立APK](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/releases/download/v0.61.0-9ad16d82c5a3/sanguo11-mobile-v0.61.0-9ad16d82c5a3.apk) · [PR49](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/pull/49) · [构建/安装/发布](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/35559224769)

详见docs/validation/v061/README.md、final-publication.json、visual-review.json。历史进度原文保存在docs/history/progress-through-v060.md。
