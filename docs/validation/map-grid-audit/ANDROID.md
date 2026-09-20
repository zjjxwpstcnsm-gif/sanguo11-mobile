# 地图专题 Android 验证记录

基线：v0.52.0 `63a3d5e3f946145fb8b5e4a52b29d5d3cccaf140`。
实际构建源码：`70b1c16544061ea44949ebcf1bb6e20fbbf6712f`。
版本：`0.52.1-map-review`，versionCode 53，应用ID `game.sanguo.mobile.dev`。

Actions run [35480644428](https://github.com/zjjxwpstcnsm-gif/sanguo11-mobile/actions/runs/35480644428) 已全部成功。
制品：`map-grid-review-verified`，artifact ID `10595792093`。
APK文件：`sanguo11-mobile-map-review.apk`，20,437,346字节。
APK SHA-256：`e10e3d79c6ac376840a026ef29e51d5031a6965a56fcfac59375991d7aeac3c6`。
签名沿用仓库开发证书，SHA-256 `8f64ee37f8ff58de8f5a199aac2ae745a5bc927d0d0eabac7540083a5e551f24`。

## 实际运行范围

- Java UI/摄像机、道路接缝、参数化方格点选及六邻接检查通过。
- `test-realm52.sh`既有玩法及地图/港口/关口回归通过。
- 18项地图及美术指纹保留；87据点图册索引验证通过。
- 在Android API29 x86_64模拟器中安装实际APK，运行`MAP50`、`MAP51`正式地图探针，两者均返回PASS。
- 下载并核验20张唯一Android窗口截图；覆盖关洛、巴蜀、江淮、东北、南中等区域，以及横竖屏全国视角。截图由UiAutomation捕获，不是数据SVG或参考图替代。
- 检查镜头移动/缩放/旋转与重复刷新未改变世界状态；未进行ARM真机或真实手机帧率测量。

## 构建历史

首轮35480421053已编译成功、MAP50返回PASS，但工作流错误查找该自定义runner不输出的`INSTRUMENTATION_CODE`字符串，流程因误判失败且漏收截图。第二轮改为核验真实`MAP50 PASS:`/`MAP51 PASS:`标记，重新构建、安装、运行并保留完整证据。不要引用首轮作为完整通过记录。

## 范围限制

本轮只完成错列方格显示、点选、缓存和接缝，以及私人逐据点对照工具。原生列错列迁移、城池/关卡多格碰撞、新一轮据点位置与地形修订均未完成；现行剧本地理数据保持不变。42个城市裁图使用既有像素锚点，45个港关裁图中心为由现行坐标反推的估计，不能当作新完成的测量。

源图及裁片未上传至公开仓库或APK。离线图册右图是正式剧本数据示意，和本记录的真实Android截图是两类不同材料。地图专题在独立分支交付，尚未合并main。
