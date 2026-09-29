# v121 交接 — PARTIAL

继续agent/native-pc-visual / Draft PR67，main不合并，不启动下一阶段；先复读远端，不能回退重建旧基线。最终APK源码b2352ed199fddada917ea224295773c4f9250506，SHA256 06ebe774afffb7f3022ec9ae6a6d2f191fdb6dbff814935e2e001aa5fbbaf4d1，最终远端证据HEAD见交付FINAL_READBACK.json和PR正文，与源码差异只能是docs/native-pc-visual。

- MainActivity→MapHost→FilamentMapView正常入口，Filament1.56/OpenGL/Java17不变；core/game-api/game-runtime/data/unity、地图通行/火计/RNG和原300资源保持。
- 共享山地高度场、坡脚斜率限制与6关护坡墙；四关无邻山不造假。普通网格worker静态批次/深度检测/原上传预算，关闭及释放有生命周期。Blender4.2.3实际导出7新GLB；资源身份和作者源脚本已提交，工程在证据包。
- 首轮平原显式高度2.4被压0.3的NEW回归已修：自动坡脚与水/道路/地基硬限制分开，新增断言通过。无手绘场景完整几何hash与首轮相同。不得用首轮中间APK替代最终包。
- 首轮API29局部旧171/候选151 PASS，12组UI/Surface、实际火焰与GPU网格、全存档一致；全国旧900秒总预算超时、新原120秒ready失败。最终API29旧局部106 PASS、候选局部及全国FAIL ready；最终API35四例FAIL ready。最终新旧差异OPEN，不能只归环境，首轮通过不能替代最终稳定性。
- 最终追加focused单镜头范围保留原120秒ready/3新帧/全部功能断言和20步采样；默认完整镜头仍保留，首轮原任务未取消。软件owner计时不包含完整Canvas，也不是呈现/GPU/手机帧率，不能宣称性能提升比例。
- core双方logistics:75继承失败；原旧全几何hash保留候选失败，不能改hash/删断言。R09主体单独1602通过，不覆盖完整脚本失败。构建lint/四ABI/签名/16KB/307资源通过，主机72734专项与受影响套件通过。
- 美术仍PARTIAL：实体烟火/尖锐岩石、装载中黑关、PC V1–V4/全模型未过。WIF条件两轮拒绝，物理提交0。全触控、旧档SAF、影子同步、ARM64/Adreno/Mali/30分钟保持开放。

下一次优先正常ready/呈现稳定性和最终候选新旧差异；设备访问合法恢复后再测真机。范围、完整CI/产物/原图/失败见reports/feedback-v121.md和evidence/feedback-v121/manifest.json。此次停止于反馈修复，不自动新阶段。
