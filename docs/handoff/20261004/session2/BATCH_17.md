# 批十七：原音乐地域汇总与存在判定

新增tools/audio/inspect_pc_music_region_aggregation.py，通过已有原Scenario.s11解码器建立真实原单位/武将/势力/district registry，以明确fixture字段执行完整原587a00/587b50/587c00；没有替换行为/getter/分类条件。原56dba0取payload+8单位索引，registry stridef4；原496030返回unit+3c坐标，4955a0经实际主将/district取得势力，496010读ushort兵力；原4839f0地域byte表、4811b0关系位和4b5cc0有方向许可函数也真实执行。

736项通过：160组合×三个原函数，128独占存在组合×两个原函数。覆盖多个单位、自势力/关系位/另一许可关系/不许可组、反向位与计数不同、同/异地域、0/1/9999/10000/65535兵力。同地域远位置150,150仍汇总，异地域不汇总，不是hex距离/屏幕可见单位判断。每次原世界3MiB、格数据1MiB和原RNG逐字节不变；聚合测试还核对外部上下文1MiB只读。

方向：汇总与587b50是queriedForce→unitOwner，587c00同queriedForce；不是把city-ratio分支587d70的cityOwner→queriedForce方向抄过来。早期独占case的反向假设被真实输出否定后，依据原push参数顺序和完整正反组合校正；未用于生产播放。单位位置用原x*200+y格索引、bits5..11和原byte表的城市地域，表SHA保留。关系组名称暂用原位/许可组，不把所有非己方归成敌人。

完整music-region-aggregation-native.json.gz保留输入、原输出、source EXE SHA、原方法及限制。META_INPUT_CONTRACT实际文件名为MEDIA_INPUT_CONTRACT.md，其中增加精确单位地域/身份/兵力/有方向关系字段。fixture状态与正常场景启动不同；当前Android地域与native owner映射仍需会话一的已提交事实，不能从此声称正常BGM绑定完成。

本批没有改规则/API/runtime、metadata、MainActivity或公共manifest/入口；不操作设备、用户存档或偏好，不启动Wine，PC目录只读。生产APK仍是批十四检查点；普通BGM/人物voice绑定仍0，完整目标active。下一步补齐剩余情势587fb0/完整场景和输入来源，再顺序接入普通播放并实际PCM/存档/多回合验证。
