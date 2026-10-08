# 256 正常 caller 同包后继驱动

仅修改 A 工具 run_remaining_normal_media.py；支持 --previous-completed 与显式 --initial-source，旧 --previous-source11 保持兼容。初始只计所给真实完成来源，其余 15 来源同一游戏/测试 APK 全部重新走正常菜单、名册、详情、保存、新 PID 冷读和完整每文件 SHA 恢复；不借旧176的五来源成绩。加入239完整构建回执登记，保留生产源与新 main 守卫。

已只读验证239整包配对回执可被正确识别；当前242尚未完成，accepted 明确拒绝其作为起点，未启动设备队列。观察版测试仍不能证明旧69键盘失败根因已修复；全16/最终B组合/普通堆回归/媒体/ARM仍待。若242失败则本驱动不得继续安装。

命令待真正完成且 observer/helper 全部退出后才可执行：

```sh
python3 docs/handoff/20261006/session-a/run_remaining_normal_media.py --previous-completed out/session-a/search-observation242/source04-diagnostic --initial-source 4 --output out/session-a/registered-normal239-all16
```
