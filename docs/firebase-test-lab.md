# 全国 3D 冷启动：Firebase Test Lab 接入

状态：CI 和测试适配；不是渲染修复，也不是已接通或已通过真机验收。
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

- Push 只构建；不会调用真机、不会消耗 Test Lab 设备额度。
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

当前范围只有：冷启动 → 剧本 → 全国预览 → 势力 → 确认开局。
日期、生命周期和开局后的完整触控链仍是后续任务；不跑 R00–R14 全量审计。
真机成功也不抹掉原 API29/API35 模拟器失败，需要区分驱动/环境差异。

官方依据：
- https://firebase.google.com/docs/test-lab/android/iam-permissions-reference
- https://firebase.google.com/docs/test-lab/usage-quotas-pricing
- https://docs.cloud.google.com/sdk/gcloud/reference/firebase/test/android/run
- https://github.com/google-github-actions/auth
