# v123 模型与性能 — PARTIAL（构建/安装待本轮结果）

基线94ab81f06c6ea24afbd491bf7d3cb3201e7e20f7，main ac29b458325b52d6e302ca44270d16552de4ed7f已合入架构PR66。承接PR67；保留Filament1.56/OpenGL、GameSession唯一权威；不改core/game-api/game-runtime/data/unity，不开始下一阶段，不合并main。

实际Blender4.2.3生成41个版本化城港关/13兵种LOD资源，保留v122及更早资源。城池内鼓楼/钟楼与栏杆，港口吊架横梁/撑杆，关隘脊饰，兵种盔脊/颈甲、骑兵鞍卷、器械加强梁。保持原关节、入口/尺度/LOD与材质；仍是程式化低多边形美术，非原版高精成品。模型细节PC参考REFERENCE_MISSING，离线渲染不是APK截图。

完整浮点属性元组合并同关节重复顶点，保留UV/硬法线接缝和所有三角面，不舍弃可见细节；+0/-0按数值相等处理，未做浮点舍入。重写rig的连续范围；生成时逐三角形核对全部属性数值相同。初版按浮点原始字节合并只减少10.2%，未过新增20%目标，原失败日志保留；修复为数值相同判定后重新执行相同断言。

正式性能路径：SiteGlb以primitive数组解码，移除每顶点Float/每索引Integer装箱；FieldAssets单代表姿态保留独立positions，复用不可变indices/UV，不再额外复制；Proxy按ground/位置/朝向/缩放/兵数/LOD/水陆状态缓存接地与GPU transform，snapshot刷新强制失效；去除animateUnits重复position调用。未降低兵力/AI/地图范围，未改变帧就绪门槛。

正常入口安装对照与10秒帧尝试记录由verify-feedback123.sh执行v122和v123同场景/视角/API29、35。原120s readiness、原始测试断言保留。主机CPU/资源缩减不等同于手机FPS；继承全国加载及API35超时、完整触控/生命周期/ARM64/30分钟性能缺口继续开放，等待本轮实测后记录，不提前关闭。
