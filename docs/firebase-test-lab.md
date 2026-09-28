# 全国 3D 冷启动：Firebase Test Lab 接入

状态：OIDC 已接通，已执行真实设备审计；没有生产渲染修复，R00–R14 未全部通过。

2026-09-28 的当前结论、逐轮结果、原始证据和剩余额度边界见 [Firebase 验收报告](native-pc-visual/evidence/firebase-20260928/AUDIT.md) 与 [runtime-cases.json](native-pc-visual/evidence/firebase-20260928/runtime-cases.json)。以下首次配置步骤作为历史安装说明保留；本仓库无需重新提供密码、PAT、私钥或浏览器登录。
工作流：`.github/workflows/native-firebase.yml`。生产源码、规则、资源和版本号不变。

## 首次账号配置（项目所有者执行）

1. 在 Firebase 控制台建立**独立测试项目**，保持 Spark 免费计划。
   不启用结算、不升级 Blaze。先进入 Test Lab 完成服务初始化。
2. 在已登录该账号的 Google Cloud Shell 执行以下命令；替换项目 ID。
   不生成、不上传服务账号 JSON 私钥。

```bash
export FTL_PROJECT='替换为你的项目ID'
export FTL_REPO='zjjxwpstcnsm-gif/sanguo11-mobile'
gcloud services enable testing.googleapis.com toolresults.googleapis.com \
  iam.googleapis.com iamcredentials.googleapis.com sts.googleapis.com \
  cloudresourcemanager.googleapis.com --project="$FTL_PROJECT"
FTL_NUMBER=$(gcloud projects describe "$FTL_PROJECT" --format='value(projectNumber)')
FTL_SA="github-test-lab@$FTL_PROJECT.iam.gserviceaccount.com"
gcloud iam service-accounts create github-test-lab --project="$FTL_PROJECT"
gcloud iam workload-identity-pools create github-test-lab \
  --project="$FTL_PROJECT" --location=global
gcloud iam workload-identity-pools providers create-oidc sanguo \
  --project="$FTL_PROJECT" --location=global --workload-identity-pool=github-test-lab \
  --issuer-uri=https://token.actions.githubusercontent.com \
  --attribute-mapping='google.subject=assertion.sub,attribute.repository=assertion.repository' \
  --attribute-condition="assertion.repository == '$FTL_REPO' && assertion.ref == 'refs/heads/agent/native-pc-visual' && assertion.workflow_ref == '$FTL_REPO/.github/workflows/native-firebase.yml@refs/heads/agent/native-pc-visual'"
gcloud iam service-accounts add-iam-policy-binding "$FTL_SA" \
  --project="$FTL_PROJECT" --role=roles/iam.workloadIdentityUser \
  --member="principalSet://iam.googleapis.com/projects/$FTL_NUMBER/locations/global/workloadIdentityPools/github-test-lab/attribute.repository/$FTL_REPO"
# 默认 Test Lab 结果桶要求 Editor；只在没有其他业务数据的独立测试项目授予。
gcloud projects add-iam-policy-binding "$FTL_PROJECT" \
  --member="serviceAccount:$FTL_SA" --role=roles/editor
printf 'FIREBASE_PROJECT_ID=%s\nFIREBASE_WIF_PROVIDER=projects/%s/locations/global/workloadIdentityPools/github-test-lab/providers/sanguo\nFIREBASE_SERVICE_ACCOUNT=%s\n' \
  "$FTL_PROJECT" "$FTL_NUMBER" "$FTL_SA"
```

3. 将最后打印的三个值保存为 GitHub 仓库 Settings → Secrets and variables
   → Actions → Variables 中同名变量。这些是项目/身份标识，不是密码。
   等 IAM 配置生效后再运行；已创建的资源无需重复创建。

默认结果桶的 Editor 权限是 Google 官方列出的要求。若不能接受该权限，
先停在构建阶段：另行采用自有结果桶和更细权限；自有结果桶要求计费项目，
不属于这里的免费默认路径。不要把 Editor 授给包含生产数据的项目。

## 执行

- 普通 Push 只构建；精确消息控制的入口例外，见下表。没有普通提交自动提交真机的通配触发。
- `workflow_dispatch` 选择 `catalog`：认证并保存当前设备目录，不提交测试。
- 从目录选择 `form=PHYSICAL`、支持 API29 或 API35 的设备 ID。
  没有某 API 真机就记录覆盖阻塞，不替换成模拟器或其他 API。
- 选择 `test`，填 `model`、`api`、`repeats=1`。工作流重新构建配对 APK，
  然后提交一次独立安装测试。分支必须是 `agent/native-pc-visual`。
- 工作流尚未进入默认分支时，可用 GitHub CLI 显式指定分支：

```bash
gh workflow run native-firebase.yml --repo zjjxwpstcnsm-gif/sanguo11-mobile \
  --ref agent/native-pc-visual -f mode=catalog
# 查询到设备后执行；MODEL_ID 必须替换为目录中真实 ID。
gh workflow run native-firebase.yml --repo zjjxwpstcnsm-gif/sanguo11-mobile \
  --ref agent/native-pc-visual -f mode=test -f model=MODEL_ID -f api=29 -f repeats=1
```

每次冷启动单独提交矩阵，不在同一进程重跑；不自动重试失败。
Spark 当前每天限 5 次真机测试；API29/35 各 3 次需跨天安排，并扣除当天其他测试。
10 分钟是整条脚本外层上限；每个场景原始 `ready()` 期限仍为 120 秒。
没有 root、框架重启、堆参数覆盖、预热或自动存档 fixture。

## 证据与通过边界

`firebase-apks` 保存配对 APK、源码 SHA、哈希、编译日志和测试入口元数据。
`firebase-results` 保存设备目录、每次 gcloud 日志及原始退出码。
视频、logcat、JUnit 结果和拉取文件在日志链接指向的 Test Lab 结果中。
请求拉取 `.../files/s01`，其中含整屏截图、Surface PixelCopy 和 cold-runtime.txt；
API29+ 的 scoped storage 可能限制拉取，首次真机必须实测确认文件完整。

适配器把原测试转换成一个命名测试的开始/成功/失败事件。
只认原探针明确的 PASS；崩溃、超时、断言失败不能算通过。
首次连接必须确认 JUnit 记录正好有 1 个测试，不能以零测试的绿色矩阵验收。
还须检查视频、整屏与 Surface 图像；CI 绿色不能独立证明画面完整。

原 FirebaseColdStartInstrumentation 的范围只有：冷启动 → 剧本 → 全国预览 → 势力 → 确认开局。
新增 FirebaseAcceptanceInstrumentation 才执行后半段触点、日期与20+20生命周期专项；最后一轮另有独立存读档和30分钟混合操作。实际失败/未运行保留，不能把新入口存在当通过。
真机成功也不抹掉原 API29/API35 模拟器失败，需要区分驱动/环境差异。

官方依据：
- https://firebase.google.com/docs/test-lab/android/iam-permissions-reference
- https://firebase.google.com/docs/test-lab/usage-quotas-pricing
- https://docs.cloud.google.com/sdk/gcloud/reference/firebase/test/android/run
- https://github.com/google-github-actions/auth

## 本轮受控入口与配对边界

| 精确提交消息 | 实际范围 | 额度 |
|---|---|---|
| `ci(firebase): recover first matrix and quota 20260928` | 只读目录/额度/全项目执行记录及首轮GCS回收 | 0次测试 |
| `ci(firebase): recover audit evidence 20260928` | 历史API35第二轮原始产物回收、拆分无损压缩包 | 0次测试 |
| `ci(firebase): audit cold api29 once 20260928` | 当前目录确认后starlte/API29，原冷启动探针一次 | 1次独立安装 |
| `ci(firebase): acceptance api35 second 20260928` | shiba/API35，一次安装，4个独立专项case | 1次独立安装 |
| `ci(firebase): acceptance api29 second 20260928` | starlte/API29，一次安装，横/竖屏及日期/生命周期 | 1次独立安装 |
| `ci(firebase): acceptance api35 third 20260928` | houji/API35，一次安装，计划7个case；实际首例启动被MIUI拒绝，1失败、余6例未运行 | 1次独立安装 |

设备/次数在每个入口明确限定。使用前保存实时physical目录和容量，守卫检查当前API支持、无结算证据、完整滚动24h执行记录小于5、GITHUB_RUN_ATTEMPT=1。没有自动flaky重试。后续新增任务必须另定范围并复查额度，不盲用这次设备/次数。

所有审计真机任务下载首轮36362991199的app并校验SHA256 `98edd7f758d41b89826d03e6fd1be3366bbea173d679a58bdd5f157b8e9479f4`。只重编测试APK；PAIR.txt同时记录appSource、testSource和两份hash。外层43分钟只供30分钟专项使用，原场景ready120秒没有放宽。

失败也尝试回收JUnit、instrumentation、logcat、视频、s01整屏/Surface与记录。前四轮原始连续视频已保留；第三轮API35仅返回JUnit/超时输出/logcat/DEVICE记录，无视频、截图或cold-runtime，按缺失记录，不能重建为PASS。重复per-case片段在RAW_FILES.json中保留原路径/大小/hash。大证据压缩包以20MiB无损分片上传，重组后核对ARCHIVE_SHA256.txt；summary或绿色Actions本身不能代替完整原始包。

本轮最终结论及459条原始要求、4条历史补充定位见 [实际验收报告](native-pc-visual/evidence/firebase-20260928/AUDIT.md)。API35两次冷启动PASS、第三次启动FAIL；API29两次PASS、第三次额度BLOCKED。完整触控链FAIL；30分钟/独立手动存读档NOT_RUN。没有将本轮写成P0修复或全R通过。
