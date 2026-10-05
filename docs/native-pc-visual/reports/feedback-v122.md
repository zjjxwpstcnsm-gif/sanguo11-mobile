# 用户反馈 v122 — PARTIAL

## 正式实现

延续远端串行分支，基线 447fc34c8db5759a5a762adb67ca97f31fa86fca，包含 main ac29b458325b52d6e302ca44270d16552de4ed7f 的架构优化。仍为 MainActivity → MapHost → FilamentMapView / Filament 1.56 OPENGL；未启用 Unity。

道路连接按两端各自的真实地形分材质，栈道木面不再蔓延到难所/普通道路半边或交汇点。SWAMP 已有格使用平滑的较暗泥地材质，仍是陆地；不更改类型、通行、存档或规则。新增 Blender 4.2.3 制作的 41 个 GLB：3种城池、港口、关隘各3级LOD，13种部队各2级LOD。正式站点加载器和 FieldAssets 使用版本化资源，保留旧307个资源的原始字节。部队保留分部件骨骼/动画逻辑，新范围写入 rigs-v122.json。总53014三角面，最大城池5940三角面，全部单材质；总8,591,748字节。增加实际几何成本，尚不能宣称手机性能提升或最终高精美术达标。

## 地图还原缺陷仍开放

全国权威地图仅5格SWAMP、158格PLANK_ROAD、161格MOUNTAIN_PATH。寿春周边 q125..155/r85..110 没有SWAMP或PLANK_ROAD，森林262格。用户参考 MAP_SAN11.jpg 可见寿春湿地区域，但当前映射数据缺失；本轮不把森林伪装湿地，不擅改地图或行军规则。具体计数及参考哈希见 evidence/feedback-v122/map-review.json。华东真正误标的地形仍需权威地图校正，渲染分材质修复不能替代数据修复。

## 验证

主机专项 PASS：24,858,362 检查（材质边界、湿地陆地性质、26模型5动画多姿态、87全国站点与全存档/RNG不变）。原3D资产/生命周期、grid/coast 和 architecture 主机专项通过。完整 core 在原447fc和候选均失败于 CoreTest.logistics:75 / AI uses deployment commands，原断言保留。旧R09/R10原断言、APK构建、正常入口API29/35和ARM64授权的最终结果见下文。Blender源文件和离线预览不是APK实拍。

本轮不合并main，不自动开始下一阶段。运行/视觉/真机未通过意味着整体保持PARTIAL。

## 可追溯 APK

- 源码：`a8a4eca79164a03f9e58ef379991c839be44968a`；在父36109f51973228028e030f6c6c862604aeb247b4上顺序提交，无force push。
- CI： https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/36590994238 （build成功）。Blender重现： https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/36590994234 （成功）。
- APK：`sanguo11-mobile-v122.apk`，38,727,710 bytes；SHA256 `8493e728a2fafe30cc9d6361548dad35a5c91ea3b4c171fb47b7a2a4e31fd9e6`。
- 包名 `game.sanguo.mobile.dev`，v122 / `0.122.0-native-paths-blender`，正常入口 `game.sanguo.mobile.MainActivity`；arm64-v8a、armeabi-v7a、x86、x86_64。
- 保留项目开发签名，证书SHA256 `8f64ee37f8ff58de8f5a199aac2ae745a5bc927d0d0eabac7540083a5e551f24`。四ABI ELF及ZIP16KB对齐通过，不等于16KB真机通过。
- 构建：`./gradlew --no-daemon :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug -PcandidateSource=$GITHUB_SHA -PnativePcVisual=true -PunityBridgeProbe=false -PmapEditorProbe=true`。
- 349个APK资源与提交源码匹配，41个新模型SHA逐一匹配；219个GLB Khronos校验0错误/0警告。旧307资源完整保留，其中202个在3d目录。

## 原测试记录与范围

完整core旧版/候选均FAIL于logistics:75；R09完整脚本在其依赖NativePreviewWorkTest:46的旧canonical snapshot断言FAIL，旧版hash=5d399ea4054e4e3792103fc73b4d709a7e40037c0d3a72d1f196c4777e56c433、候选=ac0848b52965d6cd118582891bb65b16fe18caa3eb5f39126ba24302b4da2668。本轮湿地材质确实进一步改变surface/macro字节，不能称两者画面完全不变，未修改原预期。另独立运行R09叶测试1602检查PASS，不能替代完整脚本失败。R10完整脚本两边PASS，候选4,233,776断言（13类/8clip/2LOD），覆盖分部件接地、迟滞、命令一致性。其他受影响主机专项日志见evidence。

真机授权检查在google-github-actions/auth@v3处FAIL：`unauthorized_client: The given credential is rejected by the attribute condition.` 容量查询、提交和ARM64实测均NOT_RUN，没有改IAM或寻找凭据。此为既有环境权限阻塞。

## 资源美术界限

已复核18张Blender LOD0预览，见offline-review.md。城港关和兵种虽增加几何细节，仍为程式化低多边形，不能将程序化变体称全国最终精美模型。真实APK参考对照必须另看运行证据；参考原图不随包重新分发。Blender工程使用项目既有atlas，证据包提供atlas副本和重链接说明。

## 正常入口实际安装与视觉结果（最终）

运行命令见scripts/verify-feedback122.sh：两API分别先安装v121基线再安装v122候选，`adb install -r`后pull已安装base.apk逐字节cmp；哈希匹配。调用MainActivity正常存档恢复，选择地块、切换正常3D；没有专用演示Activity或跳过原120s就绪门槛。相机/网格使用真实UI宿主API，**不是完整手指拖动/缩放触控流程**。

| API / 场景 | v121 | v122 | 范围 |
|---|---|---|---|
| API29 小场景 | PASS 85检查 | PASS 84检查 | 各4组真实Surface+UI截图、span5/10、网格关/开、完整存档字节相同 |
| API29 寿春 | FAIL | FAIL | 原120s scene readiness timeout，后续镜头/存档断言未达 |
| API29 港口 | FAIL | FAIL | 同一就绪门槛失败；候选发生1次窗口恢复仍未达到就绪 |
| API35 小场景/寿春/港口 | 全部FAIL | 全部FAIL | 原120s就绪超时，WAITING_FRAME；后续验收未达 |
| ARM64 真机 | NOT_RUN | NOT_RUN | WIF属性条件拒绝授权，无提交 |

API29候选小场景实际GPU日志：city0:0 = 5736三角面，gate = 1949三角面（包含动态山侧墙），无资产回退。证明正常加载路径使用新模型；并不能把全国城港关/13兵种均称为已完成实拍验收。检查的API29近景对照能看到左侧木面在边界停止，原木面跨入难所交点的情况已消除；湿地fixture在下方成为较暗泥地，仍有较明显色斑，美术融合待优化；关隘侧楼细节可见，默认镜头下步兵细节改善有限。见api29-baseline-surface.png和api29-candidate-surface.png（原始Surface文件，未做美化）。

API35失败截图仍显示“3D地形装载中”，候选关隘是不完整粉色轮廓，基线是不完整黑色轮廓；均不能通过材质/几何美术验收，加载未就绪时的颜色差异归因未确认。API29全国与两API稳定正常运行仍是OPEN。大量beginFrame拒绝与少量提交见runtime-summary.json，不能由软件模拟器推断真机FPS或性能改善。

整体技术实现PARTIAL、局部API29小场景运行PASS、整体视觉PARTIAL、ARM64设备NOT_RUN。模型专门的PC同镜头细节参考仍缺失；已有地图参考仅能支持地图缺陷调查。没有合并main，不开始下一阶段。
