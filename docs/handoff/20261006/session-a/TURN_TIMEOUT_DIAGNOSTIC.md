# 多旬超时线程诊断范围

最新53功能通过，但正常Source14多旬仍出现前台120秒等待截图，不能凭耗时直接判规则或渲染根因。后续正常fieldworks流程使用 `observe_turn_timeout_stacks.py`，只在自己5554锁对应的session内、实际新生成的 `*-foreground120s.png` 之后采集精确目标PID的进程/线程CPU计数及ART SIGQUIT栈。

工具不发送HOME、不构造World/snapshot、不改规则、RNG、存档、B文件或冻结JNI；不会删除Android trace。SIGQUIT会暂停ART，截图后的时序受到诊断影响，独立记录为诊断运行，不用于无干扰响应/FPS成绩。超时marker只证明原helper的前台等待分支，不能证明其后KEYCODE_HOME真的进入后台。真实Home须另以GLOBAL_ACTION_HOME及Activity/Map/Filament焦点暂停验证。

目前工具仅源码，尚未实际运行或定位新包长响应根因。trace必须核对实际目标PID/时间/线程栈后才归因，不能将其它设备进程trace归给游戏。原文件备份/每SHA最终恢复仍由device_session执行，独立守卫不放宽。
