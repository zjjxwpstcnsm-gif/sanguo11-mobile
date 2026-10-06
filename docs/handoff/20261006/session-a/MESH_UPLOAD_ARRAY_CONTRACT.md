# Primitive array mesh upload candidate

Actual army combination38 independently passed all16 normal384 UI and cold restore, but sampled Source11 zoom used402573928B/402653184B Java, only79256B sampled headroom. This is not an accepted memory budget. Original26 OOM allocation stack remains grid CPU-array allocation; this candidate addresses a separately verified additional Java copy and does not claim the unknown user ARM stack.

Current GpuMesh uploads vertex/surface/UV/tangent streams by allocateDirect then put(float[]). Android10 DirectByteBuffer.MemoryRef backs that with newNonMovableArray byte[] (prior HEAP_PROFILE original live holders). The pinned Filament1.56 NioUtils.cpp expressly accepts wrapped FLOAT arrays, calls GetFloatArrayElements, retains global buffer/array refs and releases with JNI_ABORT after JniBufferCallback completes. VertexBuffer.cpp attaches that callback to BufferDescriptor. Change only the four completed immutable SceneMesh float streams to FloatBuffer.wrap; no read-only buffer wrapper, no array mutation or pooling before upload consumption.

Pinned official implementation evidence freshly read2026-10-07:
https://raw.githubusercontent.com/google/filament/v1.56.0/android/common/NioUtils.cpp
https://raw.githubusercontent.com/google/filament/v1.56.0/android/filament-android/src/main/cpp/VertexBuffer.cpp

This removes the explicit additional Java float-to-direct-byte copy. JNI may copy to native heap; it is not zero-copy/native/GPU memory saving proof. Mesh source arrays continue retained by source and JNI global refs until consumption; geometry/attributes/offsets/order/index encoding/resourceSHA/rules unchanged. Existing16pattern index buffer encoding stays separately validated. Need new source geometry parity, fresh APK build/install/normal matrix, CPU/native/GPU evidence and actual ARM; cannot borrow current39 fire or38 peaks. Default largeHeap remains, ordinary384 regression remains required.
