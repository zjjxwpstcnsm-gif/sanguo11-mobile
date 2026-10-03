# v118 用户反馈专项交接 — PARTIAL

继续 agent/native-pc-visual / PR67，不合 main，不启动新阶段。承接v117网格岸线和全部R阶段成果。

生产改动：3D选中部队长按拖放接回正常MainActivity.dropUnit；森林追加3个稳定候选且保留旧树、灌木及保护留白；普通扰乱与螺旋控制按本轮用户要求下调，特技必定/免疫保留。规则数值详见docs/RULES_V0_8.md；不得再声称core完全没改。

所有地图/规则RNG算法/存档格式与300份资源未变。拖放的规则预览留在MapHost，执行仍为GameSession事务，Filament不拿World。

报告和manifest见 ../reports/feedback-v118.md、../evidence/feedback-v118/。主机概率分布与森林数量、原固定哈希控制、原core失败已归档。最终设备与APK结果在报告收尾段追加。

继续时先复读远端与实际产物。正常全国开局、真机性能、PC参考/最终美术等旧缺口没有因局部专项自动关闭。森林密度提高带来额外网格内存，不能用JVM检查代替手机热稳。

最终source23103d9278d9898316ec212113a68abeb94c73fc / CI36499443015已构建APK（37,403,426字节，SHA d0bdbcb1b2acb68a809a62ad3ec4961c75f7fb89ba00bbf35a0b50d923f6d5a0）。两轮API29输入PASS/候选ready FAIL，两轮API35双方FAIL；候选拖动与森林视觉NOT_REACHED，严禁标记PASS。WIF仍拒绝、真机0。旧版PASS意为成功复现缺失拖动，不是拖动正常。完整原始证据见交付包，Git文本/文件hash在evidence/feedback-v118。
