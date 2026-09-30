# v120 用户反馈交接 — PARTIAL

本轮止于用户三项反馈，不自动开始新阶段。保持agent/native-pc-visual和Draft PR67，main未合并。下一次先复读远端，不能重建旧分支覆盖串行成果。

- APK源码931a298e5c06b5fd56ccfcf067defa8dd840d17e；生产修改8198ed9cbf9f558d67f17e6fca9494ce68134370。最终远端证据HEAD在交付FINAL_READBACK.json和PR正文中给出；相对APK源码只能有docs/native-pc-visual差异。
- 正式入口MainActivity → MapHost → FilamentMapView。复用Filament1.56.0/OpenGL ES、现有GPU owner/worker、UnitFormation、相机和模型；不改权威GameSession、玩法、地图、存档、RNG、v119智力规则、签名、300资源，不启动Unity。
- 拖动影子是当前模型/编队的半透明Canvas战术投影和脚下影子，非新纹理GPU分身。森林不再按部队动态清空；选中部队有淡金透冠轮廓。选格使用深外沿/金中线/浅内线与填色。不是新增行军动画或最终美术资源。
- 主机真实旧生成器复现缺陷；候选保留4/3放置、移动前后6块原对象复用，完整Save/RNG不变。R10/R02/R09/R12/架构受影响门禁通过；完整core.logistics:75输入和候选独立同败。不得删断言或把continue-on-error当PASS。
- 首轮API29旧PASS99、新120秒ready FAIL；首轮API35双边Quickstep ANR遮挡、零提交。追加测试驱动仅识别精确外部桌面ANR并真实点击Close app，保留原120秒门槛和全部首轮失败。
- 最终API29候选PASS134，取消分支/单次drop/完整存档一致/森林保持及四镜头已取得；原图的有效/无效预览与截图命名状态滞后、标签遮挡仍未验收。先处理ready/呈现稳定性和新旧差异，再验证影子及时跟手及无遮挡；API35依然FAIL。不得用内部状态、主机或构建PASS替代实际画面门槛。
- 真机WIF attribute condition仍拒绝，设备提交0；需在合法访问恢复后验证ARM64/Adreno/Mali、30分钟性能热稳。没有改IAM或绕过限制。PC参考缺失，V1–V4、全国美术、完整纯触控新开局、旧档SAF和历史缺口全部保留。

复现：bash scripts/test-feedback120.sh；构建参数见reports/feedback-v120.md。scripts/verify-feedback120.sh安装精确v119/v120，先拉回安装字节比较，再以合法forest fixture运行正常MainActivity。程序选中/设镜头，真实长按拖动/抬手。录屏180秒上限，不是完整纯触控或手机性能证明。

最终设备结果见reports/feedback-v120.md与evidence/feedback-v120/manifest.json；逐条状态以实际最终证据为准。交付APK为universal v120，SHA256 172a482f76d4dc7300403d692a9085c0545b82784076ae8ca17f479d6a78b30c。
