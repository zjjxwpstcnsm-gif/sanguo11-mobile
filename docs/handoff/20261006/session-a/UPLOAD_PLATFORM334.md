# 334 实际Filament版本的上传路径核查

游戏冻结依赖1.56.0，不用当前main源码代替该版本。已读官方同版本[VertexBuffer.cpp](https://raw.githubusercontent.com/google/filament/v1.56.0/android/filament-android/src/main/cpp/VertexBuffer.cpp)、[NioUtils.cpp](https://raw.githubusercontent.com/google/filament/v1.56.0/android/common/NioUtils.cpp)与[NioUtils.h](https://raw.githubusercontent.com/google/filament/v1.56.0/android/common/NioUtils.h)。之前猜测的BufferUtils路径404、错误src/common路径404、GitHub tree API403均为取证失败，不构成实现证据；CMakeLists明确../common/NioUtils.cpp后取得正确文件。

实际VertexBuffer JNI把AutoBuffer放入异步JniBufferCallback；wrapped float数组走GetFloatArrayElements，到AutoBuffer销毁时ReleaseFloatArrayElements并释放global refs。direct buffer走GetDirectBufferAddress。因此不能说这一路使用GetPrimitiveArrayCritical，也不能把进程concurrent GC全部归因为被该方法锁住；VM是否复制/固定数组与实际upload callback时序还需要设备证据。当前FloatBuffer.wrap节省第二份Java direct payload，但其native临时/全局引用必须计入独立生命周期调查。此处没有更改Filament JNI/原4JNI/共享Gradle，也没有认定唯一OOM/GC根因。

A原pc-ground.mat明确requires uv0/uv1，pc-ground-outline.mat仅uv0；两者当前源没有消费legacy vertexColor。但未修改现有材质、原几何或上传布局，不把静态shader检查当GPU像素验收。任何后继原流压缩或有界direct callback缓存需原始bits/布局/所有pass等价、实际设备callback/资源释放/正常多Host重选/全近缩放和内存预算证据；不通过减少原网格或永关3D解决。
