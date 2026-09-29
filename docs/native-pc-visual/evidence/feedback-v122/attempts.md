# 本轮失败账本

- Blender首次CI36589213354：libEGL.so.1缺失，环境FAIL。补齐libegl1等依赖并开启--python-exit-code 1；36589538831成功生成，36590994234在最终源码成功重现，模型文件字节一致。首次日志在独立证据包保留。
- 新增主机测试首次使用空World做SaveCodec比较，输入不满足有效势力/城市约束，测试夹具FAIL。改为有效势力/城市输入后原完整SaveCodec断言通过，没有改弱或删除断言。
- 原core两版均FAIL：CoreTest.logistics:75，inherited。
- R09完整脚本两版均在NativePreviewWorkTest旧快照断言FAIL：inherited gate + intended new material bytes；原预期保留。另独立R09叶检查1602 PASS不替代脚本失败。
- ARM64授权检查：WIF attribute condition拒绝，environment；没有真机提交。
- 运行与视觉的最终结果另见runtime-summary.json，不以前述主机PASS替代。
