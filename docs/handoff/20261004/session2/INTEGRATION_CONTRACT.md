# 顺序整合边界

本目录独立codex/portrait-audio-restoration-integration，只顺序合入人物已完成9e171f2，不读取其未提交文件。完整媒体父提交320ddda和所有固定输入/JNI保留；已完成增量逐文件与9e171f2相同，见integration-merge-guards.json。

公共入口先以完成metadata MainActivity的前镜像691665ef48b7b21decf4267d668105d12d7edc01d32cf7532289d4148169c1df核验，仅应用portrait-host.patch只读snapshot bind与fact-host.patch逐事实HUD/演示回调。三个公共路径、前后像和完整patch SHA记录integration-public-guards.json；人物文字/数值逻辑不改。正常流程验证只在本目录新构建APK进行。媒体原分支与人物工作目录均不覆盖。

不启动Wine、不写PC、不清设备数据；5582独占并完全恢复用户文件。此阶段是正常头像入口及逐事实音效集成，仍不代表完整原语音/场景音乐/形态覆盖。

普通DataTable原为纯文字列表。新增portrait-list-host.patch只在name column装饰28dp原头像，在每次复用清除旧drawable；MainActivity snapshot bind追加OfficerPortrait.bindView只读弱引用。人物名称/数值/排序/搜索不改，确切前后像见portrait-list-host-guards.json。

实际截图发现姓名列最小56dp容纳28dp图后文字不可见。portrait-name-width.patch只将人物姓名列最小宽度提高128dp，其它表格/数值未改；前后像见portrait-name-width-guards.json。首轮实装因此主动终止且全部用户文件恢复；不作为通过证据。
