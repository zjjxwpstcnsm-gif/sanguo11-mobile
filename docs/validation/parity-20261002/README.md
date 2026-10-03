# 玩法/数据接续第一阶段验证（目标继续）

完整继承基线为 `out/parity/baseline-20261002/manifest.json`：3524文件、205142686字节，历史HEAD `52315bf070e5d29acb8f509230c5228e47c8d6ef`。完整源树及被忽略的双ABI worker已独立封存，UI目录已据此继承并校验；不能用旧HEAD替代本基线。本轮原目录app文件与封存基线逐项SHA一致，没有覆盖UI文件。

## 实际产物

`out/parity/age-validation-20261002/sanguo11-age-candidate.apk` 已构建、签名核对、覆盖安装并启动。

- SHA256：`80ca0c5a579814af1a9f6ea20359bfcc539806dba6d3bb20cb45ed35101d8510`。
- 开发签名SHA256：`8f64ee37f8ff58de8f5a199aac2ae745a5bc927d0d0eabac7540083a5e551f24`。
- APK包含交接时的六将年龄绑定/当前历年事实，未包含UI会话本轮界面修改。它不是完整玩法对齐发行版。
- 设备为保留userdata的 `san11-pc-map` / Android29 x86_64模拟器。没有ARM真机证据。

## 主机与数据

72,450项正常头像/事件/保存/RNG检查、209项BattleFeedback、150项PlotJournal通过。新增CriticalYearTest的50项覆盖跨年、非1月开局、不可变事实、拒绝命令与完整存档/RNG，已注册core:check。

新增PcCampaignFlowProbe通过423项：九份当前内置剧本各经生产GameSession执行真实巡察、旧token重复提交拒绝、六旬推进、逐旬存读和完整状态一致性。它不证明PC规则等价，也不代替Android操作。

实际用户存档副本在当前与冻结基线各推进六旬，七份完整快照逐字节一致。全量core/check与game-runtime/check的31个失败任务，在完整冻结源树上同样失败，首个异常文本亦一致；记录为既存失败，没有削弱断言。静态架构围栏通过，完整架构行为脚本仍在历史 `exact old-scenario starting state` 断言失败，不能写成“架构全部通过”。

人物源调查与工具详见 `docs/pc-data/README.md`。10720条原serializer记录、10656条身份确认、2条位置重映射、64条字形隔离、17处能力/适性字段差异；四项独立解析回归通过。源安装目录原1805文件、2780244569字节SHA复核无变更；未启动Wine。

## 实装覆盖与边界

实际通过功能菜单新开184/何進局，修订65、4处原堤防入档；正常点击确认推进两旬，存档解码分别turn1、turn2。第一旬界面显示24.8秒，性能未通过。第二旬有30秒局部录像（68个实际封装样本），不是完整连续流程视频。截屏已查看原地图与UI；记录过程中UI自动化出现一次无法取得idle状态，旧XML没有当作当前完成证据。

强制停止后重新启动，新档文件逐字节未变；离线解码验证turn2与完整重编码。冷启动后最终UI恢复画面没有再截图核验，不能扩展为全部加载交互通过。

安装前外部备份files/shared_prefs；结束后外部恢复并逐文件核对原有5份文件全等。用户auto.sg11仍168846字节、SHA256 `82554269b1920b5d67b8945d22a55f0253cab7ed2ea55bf520ef5b9b3b99f0a9`。新局流程额外生成 `files/customOfficers/library.json`，未删除其他数据。模拟器userdata未清除，启动排障前还留有完整AVD副本。

## 待接续

Android测试包首次构建在继承的PcPresentationsInstrumentation中失败：verifyGpu引用了作用域外fixture。最小补丁仅传递expectedSelector，不改断言；在 `out/parity/age-validation-20261002/ui-test-compile-fix.patch`。UI独立目录已经自行修复相同问题，本原目录尚未按顺序集成。六位武将年龄GPU/生命周期专项仍未通过本新包验证，不能继承旧APK结果。

继续完成该专项、原人物额外槽位/字形/关系/特技、全剧本其余记录与实际覆盖顺序，再把已核实数据接入独立正式剧本。玩法各组差异见 `docs/PC_PARITY_STATUS.md`，共享边界见 `docs/architecture/PARITY_UI_CONTRACT.md`。UI最终集成及全流程/ARM验证未完成。

机器结果、构建/安装日志、APK、存档、截图/局部视频在 `out/parity/age-validation-20261002/`；全量回归对照在 `out/parity/baseline-core-validation/comparison.json`；九剧本会话结果在 `out/parity/campaign-flows.log`。
