# 当前可复核媒体检查点（完整目标未完成）

当前最新菜单音乐APK：`out/media/menu-music-build-25-r7/frozen/app-debug.apk`，源81dd448ce7d263ae3fe29ddd6781151238f31635，307736130字节，SHA `8afbe618066681f8408ed2d49613e18523994281d6ade20a434f0afbed19341c`。测试包SHA `aecdcb9cbec240813c16b16544b7ef1d598d1df00fbfc02e29217f145863caf9`。原559220/558b20入口、载入/新局/教学/选项/退出原文字及32原wrapper组合闭合，实际建立后的正常菜单自动播放原music1/resource2238，未按按钮点击猜曲目；菜单音乐正常绑定现为1，地图BGM和人物voice仍0。

新包真实菜单116–121秒循环、刷新同实例、独立音乐65/语音30/音效75、Home/静音/系统耳机噪声/实际恢复当前曲、退出菜单停止、重入、真正destroy和owner0释放通过，Save/RNG及用户文件逐字节恢复。第一原循环AudioTrack欠载0、65536字节有界缓冲、首写394ms。实际未改混音四个固定原曲2秒窗口相关性最小0.999994605，正向可听gain，证明原曲声音真实出现；整首连续相关性0.6577545不达原0.995门槛，完整连续性仍未通过，失败录音/检查器未修改。详见MENU_MUSIC_INSTALLED_25.md。新包Android JSON桥接18项通过；无Unity Player、完整头像新包复验或ARM声明。旧APK24头像QA保持其原范围，不能转移到本包。

两处新的MainActivity入口仅菜单媒体和独立音量组件，先写确切SHA契约再顺序集成；不改人物数值/metadata、规则/API/runtime或公共manifest。当前生产音乐流增加音频优先级和单轨欠载统计，其余旧来源/身份/媒体成果完整继承。批25完整源码：`out/media/delivery-25/sanguo11-portrait-audio-source.tar.gz`，截点01dedb6e73dadf9ab3827a176d776428983f852a，9885文件及四JNI逐份字节一致，480663572字节，SHA `5283ad7d9970bebd57305621b3a5fd602836b32dbc0a100d9caa238bb8f8e421`；本后继指针不在归档截点内，实现/来源/验证记录均包含。

独立目录 `/Users/paopao/.codex/worktrees/portrait-audio-integration/sanguo11-mobile`，分支 `codex/portrait-audio-restoration-integration`。继承8400301/R40全部dirty成果、168输入和四JNI；仅顺序集成已完成metadata9e171f2。core/game-api/game-runtime与该提交字节一致，原目录和另一会话不动；人物数值/metadata或公共manifest未改；批25仅按已记录SHA契约追加两处MainActivity媒体入口。

上一批完整头像QA APK：`out/media/dynamic-portrait-build-24-r7/frozen/app-debug.apk`，源码9711f8d287a5be3105e45db4f9d9c8d47953802f，307734890字节，SHA256 `7e802733b0a74105f34c2178c8a7522e6d2d406b860841947225252c2fd02720`。测试包SHA `8eb929be4185cf16547cd39c83abc2b7b1ca51de492688e43fa6bd78a65e5700`。独占5582实际安装/读回；本批全部保存/库/偏好恢复字节一致，不清数据、不操作5554录音。

头像增量：62原动态选择器131..192、10656批准来源join/16版本，文件/原资源编号/尺寸/alpha/PNG及RGBA SHA/解码路径完整记录。两次64输出文件字节一致；Java原生年龄向量及来源拒绝159846项通过。主普通FCE2892图片保持不变。实际正常战法简雍10122按原face174→共享全屏190显示原头像和原效果层，真实PixelCopy可见，取消/双提交/暂停/恢复和完整Save/RNG对照通过。不能把原共享全屏190叫专属简雍绘图。

动态纹理按需worker解码，最多2请求/4张GPU缓存。首次原事件纹理4259840字节；额外原资源缓存fixture131..135实测上限7405568，LRU淘汰/材质引用解绑/实际renderer替换释放通过；不是五个正常人物绑定。暂停逐帧中段可见不代表原连续时长/色彩已验收，3.872/5.679秒准备至检查帧计时均保留。DYNAMIC_PORTRAITS_INSTALLED_24.md、ALPHA_24.json和各守卫列确切增量。

同一新APK完整正常新局/来源选择/656目录及656详情/保存读回/后台/退出重开646114项通过，652批准原像素+4未知；普通头像缓存16588800字节。新包Android JSON桥接18项通过，无Unity Player声明。详见DYNAMIC_PORTRAITS_NORMAL_QA_24.md。其它15来源及全部角色调用形态仍未覆盖。

同包未暂停正常战法实际PCM：原PC HUD33加两种明确标注的旧移动端合成干扰声，联合0.9999997589724375、原33扣除干扰后0.9999995458742712；对应真实fact.id/parent并保存RNG一致。较早暂停R6三声模型失败保留，未降低阈值；不能把heard/play返回值当录音通过。其余九种移动端合成仍不称PC音色。

原取消UI sound1正常BACK证据属于旧批20，不能移用成新包该路径也完成PCM验收。旧批19明确track/profile共享音乐语音adapter、ducking及PCM属于旧包，仍不是普通音乐/语音绑定。批21–23完整原主将/能力/有效性、显示选择和记录初始化来源工具已保留；两种原随机源均未执行或接入规则，原动作/选择/有效性契约仍见MEDIA_INPUT_CONTRACT.md。

普通BGM和人物voice正常绑定仍0；30原音乐/1997原voice及音效来源/转换/播放基础在源码中，但不是完整正常还原。原caller形态、MOD生效优先级、多回合头像/声音组合、完整原SFX、原连续表现/性能及ARM/手机扬声器证据继续未完成。不会静默标为移动端简化。

完整源码归档已在 `out/media/delivery-24/` 验证：截点b6180576e731c942d6d13808d0becdf8622281fe，9858文件及四JNI逐份一致，470353025字节，SHA `79378e4c6a48b6b00fefdd7945cea1724ae0b6860a32a72e184dc92b5ca733a5`。77路径增量SHA守卫在delta-guards.json；包含新QA的最终归档为 `out/media/delivery-24-final/sanguo11-portrait-audio-source.tar.gz`，截点7ca62580868d405fccd35579d3b8a0783e78d021，9866文件及四JNI逐份一致，471578059字节，SHA `fa7a3077a434ae1d00dfbb0fdfcfaeea233f4a57468b4bde6915f6028b568cc2`。本交付指针更新在归档截点之后；不影响归档内实现/新QA与当前APK的对应。旧包和旧归档全部保留。
