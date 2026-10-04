# 首轮小头像索引退回（2026-10-04）

`a74b8c6` / `b1f7c24` 中的 `portrait-pixels-manifest.json.gz`、`portrait-source-join.json.gz`、`portrait-voice-source-join.json.gz` 所携带的 **小头像路径映射不可用于集成**。大头像区段、原像素字节、源文件 SHA、人物身份/出生/年龄/voiceType 输入的原代码检查仍有效；小头像的 faceId/imageGroup 分组错误。

原 `46e41b..46e45a` 先读取 count 个大头像，再把 count×2 个小头像交错项加载到 registry+2400×8+start×16。文件布局为 `large[count]`，随后 `(small1,small2)[count]`。原 registry 号为大图 faceId，小图 `2400+2×faceId+form-1`。

`inspect_pc_face_registry.py` 实际执行原 `46e360`，只供应原文件的 fopen/fseek/fread/字节读取；6,600 图像 registry 项和完整 descriptor 一共 6,601 检查通过。每个非空 Face 实际均包含 3 张图：三个组各 964 非空图，共 2,892 张、964 Face；1,236 Face 三项全空。两种小图的 UI 语义仍须原调用证明，不按图像相似度猜“战斗/文官”等角色。

修正转换正在全量重跑，并重新对照两次全部文件字节和人物连接。旧结果与失败解释保留；后续将另发 `portrait-pixels-current.json.gz`、`portrait-source-join-current.json.gz` 和原 registry 证据，不把旧重复转换通过当作正确映射通过。

尚未把本批 FCE 像素接入 APK；当前 fbc998c9 组合包只更改原 HUD33 样本，技巧点/音频/开局/JSON 实装结果不依赖错误小头像映射。
