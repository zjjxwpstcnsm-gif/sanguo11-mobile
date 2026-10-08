# 274 原头像完整边界候选

当前正常名册/详情 OfficerPortrait.draw 在已加载原PC bitmap上先以宽度10%圆角clipPath裁去四角，再覆盖金色圆角框。之前263等 fullBitmapSameAs 只证实源Bitmap完全相同，不证明该Drawable实际显示完整像素；这个差距不能用旧670人通过掩盖。

本独立补丁只对已成功由PcPortraitLoader提供且无customImage覆盖的原bitmap使用clipRect(area)，并在原图绘制后跳过工程金边。不改group0、来源/年龄/性别/形态lookup、源PNG、异步缓存、原已有线性缩放/滤波、全屏头像、custom/fallback样式，也不推断原PC64×80小图caller或真实原窗口边框。小图caller109仍未证明，不能把240×240全图缩小当该问题已经闭合。

独立Android35/JDK17编译与patch逐字节回读通过，规范生产及已经冻结/待装269均未改。后继APK必须正常菜单/人物名册/详情检查完整矩形四角、实际像素/比例/色彩、滑动复用/新局切档与全Save/RNG/Token纯性；本补丁的静态编译不是正常UI验收，也不转用269或263成绩。
