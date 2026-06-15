# multica APP — 需求文档

> 记录我提出来的所有需求。会持续更新，我自己会修改。

## 目的

老板搭建了多agent的管理平台multica，但是需要登录web 去查看状态，比较麻烦，
老板系统通过有一个显示屏，专门显示multica中不同daemons，agent，issuse的状态，方便了解agent的工作情况。
老板有闲置的旧安卓手机，可以通过安装APK来实现实时展示。

## 参考资料

## 这个是multica 的github项目地址

<https://github.com/multica-ai/multica>

## 这个是闲置安卓手机的配置参考

这里请AI帮我获取后填写

## 环境

| 项                  | 值                                              | 用途                                                  | <br /> |
| ------------------ | ---------------------------------------------- | --------------------------------------------------- | :----- |
| multica 域名服务器      | `https://multica.299970.xyz`                   | 远程访问，HTTPS                                          | <br /> |
| multica 本地服务器      | `http://172.26.28.8:9090`                      | 内网访问，HTTP（需 `network_security_config` 放行 cleartext） | <br /> |
| multica PAT（Token） | `mul_f9829a0a990116580236c1c8914cc144a302afb6` | <br />                                              | <br /> |
| github 项目地址        | <https://github.com/299970/multica-app.git>    | 库                                                   |  |
| github path       |  (redacted)   |            |  |

## 功能

### 基本要求

1. 打开APP 后，屏幕保持常亮；
2. 整体风格用暗色，苹果风格；
3. 整体风格紧凑型
4. 整体页面要具有响应式能力，即在不同屏幕尺寸下都能正常显示；
5. 整体的页面相关卡片，会实时根据状态调整 ，比如颜色变化，状态变化，顺序排序；
6. 打开APP， 默认界面显示agents；
7. 打开APP时，优先连接内网地址（连接成功工作区标题背景用蓝色），如果内网访问  ，再访问域名（连接成功工作区标题用蓝色）

### 页面展示

1. 用黑色背景；
2. 请以全屏显示，隐藏状态栏；

#### 页面展示

1. 用黑色背景；
2. 内容的卡片，需要做成自适应高度和宽度的


#### 顶部标题栏
1. 具备工作区标题，点击工作区标题，弹出已连接的multica的不同工作区，点击可以切换到该工作区；
2. 工作区标题背景根据网络连接情况显示，内网连接=绿色，域名连接=蓝色，无法连接=红色
3. 有一个刷新页面的按钮；
4. 有一个设置按钮，点击可以打开设置页面，设置页面包括
   b. 连接的服务器内网地址
   c. 连接的服务器域名地址
   f. 连接的服务器PAT（Token）
5. 请在 工作区标题右边，增加当前服务器端 multica 的版本号，显示白色，并定期检测是否有新版本，如果有，则显示红色

### 功能

#### runtimes

1. 顶部显示消耗的每日token汇总，柱状图显示，每一个柱子1天，显示30天的，参考http://172.26.28.80:3000/jimiiot/usage
2. 现在已经接入multica 的daemons，竖排展示，每个daemon 有一个状态图标，点击图标可以查看该daemon 的详细状态。
3. 以runtimes 主机一行即可，不用显示具体里面的AI，意思是有几个主机接入，就显示几个卡片即可，不用按照接入主机的AI 来划分卡片；
4. 每个卡片显示 一个runtimes，，包括
   a. 第一行：runtimes名称，状态（红圆点表示，绿色在线，红色离线，黄色异常）
   b. 第二行：runtimes 的IP地址和端口
   c. 第三行：runtimes 下有几个 AI；
5. 点击runtimes卡片，会弹出详情页

##### 要求

1. 卡片紧凑排列（间距 ≤ 8dp），适合大屏一屏展示多个 runtime
2. 状态圆点用 4 档颜色：绿=在线、红=离线、黄=异常/启动中、灰=未知
3. 多余字段（PID / device / provider）作为副信息在小字灰阶展示，不抢主标题
4. 排序先按优先级在线状态，在线>异常>离线
5. 排序其次再按照名称排序
6. 当有runtiems上线，或者离线，就发出叮一声

#### Agents

1. 每一行显示 两个个agent，左右各一个
   卡片；
2. 卡片中显示如下
   a. 第一行：agent名称，状态（圆点表示，绿色在线，红色离线，黄色异常）
   b. 第二行：agent的头像(48dp)， 工作状态，右侧负载矩形(高度=头像48dp)
      - 负载矩形内容："当前负载/待办合计"（如 1/3），当前负载=in_progress数，合计=in_progress+todo数
      - 负载矩形颜色：有负载(合计>0)=蓝色(#0A84FF)，无负载=灰色(#3A3A3C)
      - 工作状态：工作中(蓝)、待办(橙)、空闲(灰)、runtime离线(红)
   c. 第三行：agent 上一次工作的时间，年月日时分秒；
3. 点击agent 卡片，可以查看该agent 的详细状态
4. 卡片列数可在设置页配置（1~4列），默认竖屏2列、横屏3列

##### 要求

1. 排序先按优先级在线状态，工作中>待办>空闲>离线
2. 排序其次再按照名称排序
3. 卡片以灰色展示
4. 工作中的卡片使用边框闪烁蓝色
5. 当任何一个agents有任务开始，就发出叮一声，结束任务， 就发出嘟一声；
6. 新增点击agents 卡片，可以看到详情，包括
- 名称
- runtime
- 当前使用模型
- 任务情况列表：进行任务，待办，审批

#### Issues

1. 每一行显示 一个issues卡片；
2. 卡片中显示如下
   a. 第一行：issuseID，名称，状态（文字显示，如规划，待办，进行中，审核中，done，阻塞）
   b. 第二行：issuse 的所属项目，分配的agent
   c. 第三行：issuse 的开始时间，和截止时间

##### 要求

1. 排序先按照issuse 的状态排序(阻塞>审核中>进行中>待办>规划>done)
2. 排序其次按优先级排序
3. 卡片以灰色展示
4. 工作中的卡片使用边框闪烁蓝色
5. 阻塞的卡片使用边框红色闪烁
6. 画面按照紧凑型显示；

#### Boss

这一个页面，是展示需要boss，就是这个工作区域的拥有者处理的issues列表；

1. @will 的，展示在这里
2. 需要审核的，展示这里
3. 有chat未读的消息的，展示在这里

## 验收标准（新增 2026-06-08 老板更新版）

### 基本要求

- [x] 1\. 屏幕常亮（`FLAG_KEEP_SCREEN_ON`）
- [x] 2\. 暗色 + 苹果风（黑底 + iOS system 色板）
- [x] 3\. 紧凑布局（小 padding + 小圆角 + 紧凑行高）

### runtimes Tab

- [x] 竖排卡片
- [x] 每卡 3 行：name+状态圆点 / IP·端口·provider / AI chip 列表
- [x] 状态圆点 = 绿/红/黄/灰 4 档
- [x] AI 列表来自 `/api/agents` 按 `runtime_id` 过滤

### 连接稳定性

- [x] OkHttp WS `pingInterval=5s`（每 5s 发 ping 帧）
- [x] 应用层 `{"type":"ping"}` 每 20s 发一次（防 server 主动 close）
- [x] 失败自动重连（指数退避 1s → 30s）

## 目录

- [2026-06-04 需求 0：项目初始化](#2026-06-04-需求-0项目初始化)
- [2026-06-04 需求 1：原型参照 Multica — Android 端状态查看 APP](#2026-06-04-需求-1原型参照-multica--android-端状态查看-app)
- [2026-06-05 需求 2：自用部署 / 参数内置到 APP](#2026-06-05-需求-2自用部署--参数内置到-app)

***

## 2026-06-04 需求 0：项目初始化

| 项      | 内容                                                  |
| ------ | --------------------------------------------------- |
| 目标平台   | Android（手机）                                         |
| 设备     | Samsung Galaxy Note 8 (SM-N9500)，Android 9 (API 28) |
| 形式     | 真机调试（已 USB 连接到开发电脑）                                 |
| 需求 0.1 | 在电脑上连接好 ADB 工具，可直接控制手机（**不**让用户手输命令）                |
| 需求 0.2 | 新建需求文档（本文件）                                         |
| 需求 0.3 | 新建设计与开发文档（`docs/dev-design.md`），记录开发流程、注意点          |
| 需求 0.4 | 部署好开发环境（JDK、SDK、Gradle），并构建一个空壳 APP 真机跑通            |

### 验收标准

- [x] ADB 已连接，可执行 `adb shell`、`pm install`、`am start` 等
- [x] `docs/requirements.md`、`docs/dev-design.md` 已建
- [x] 第一个空壳 APP 已成功 build + install + 在手机上跑起来

***

## 2026-06-04 需求 1：原型参照 Multica — Android 端状态查看 APP

| 项    | 内容                                      |
| ---- | --------------------------------------- |
| 原型项目 | <https://github.com/multica-ai/multica> |
| 目标用户 | 我自己（开发 + 运维）                            |
| 目标场景 | 在手机上**实时看到 agent 的工作状态**                |

### 1.1 服务器部署

- 部署方式：**Self-host**（本地 Docker compose）
- Server URL 形如：`http://<host-ip>:9090`（如 `http://172.26.28.80:9090`）

### 1.2 凭证

- 使用 **PAT**（`mul_` 前缀）在 APP 内手输（**不**写入代码 / 仓库）
- APP 首次启动引导用户填 Server URL + PAT → 加密存到 EncryptedSharedPreferences

### 1.3 功能范围（MVP，3 项 Tab）

- **Daemons Tab**：当前 workspace 的 daemon 状态（在线/离线/启动中/已停止、uptime、当前 agents）
- **Agents Tab**：agent 列表（名字、runtime、provider/CLI、当前状态）
- **Issues Tab**：按状态分组的 issue 看板（`todo` / `in_progress` / `in_review` / `done` / `cancelled`）

### 1.4 数据刷新

- **实时优先**：WebSocket `/ws?token=<PAT>` 推变更 → APP 秒级更新
- **兜底**：拉取 `/api/me`、`/api/workspaces`、`/api/workspaces/{id}/agents|issues|runtimes` 三个 REST 端点

### 1.5 UI

- Material 3
- 顶部 AppBar：标题 + 刷新按钮 + 设置入口
- 中间 3 个 Tab：Daemons / Agents / Issues
- 设置页：Server URL / Personal Access Token / Workspace ID（留空自动选第一个）

### 验收标准

- [x] 仓库已 clone 调研、API 路径已确认
- [x] 真实 API 路径：`/api/me`、`/api/workspaces`、`/api/workspaces/{id}/{agents,issues,runtimes}`、`/api/daemon`、`/ws`（已修正）
- [x] 三个 Tab UI 已实现
- [x] Settings 页可填 URL + PAT + Workspace ID
- [x] WebSocket client + 自动重连
- [x] Mock fallback（未配置时显示示例数据）
- [ ] **真实数据连通**（HTTP 200 + 显示 daemon/agent/issue 实际数据 — 进行中）

***

## 2026-06-05 需求 2：自用部署 / 参数内置到 APP

> 调整自 1.2 凭证：因为是自用 + 不想每次启动输 token，把 PAT 直接写进 APP。

| 项          | 内容                                                                       |
| ---------- | ------------------------------------------------------------------------ |
| 凭证存储       | 改为 **`BuildConfig.DEFAULT_PAT`**（从 `local.properties` 的 `multica.pat` 读） |
| Server URL | 同样写进 `BuildConfig.DEFAULT_SERVER_URL`                                    |
| 仓库策略       | `local.properties` 已 `.gitignore`，不进 git                                 |
| 加密策略       | **不变**：运行时仍走 EncryptedSharedPreferences（用户之后改用 Settings 页能正常覆盖）          |
| Settings 页 | 保留，PAT 框仍可见（用户可手动改覆盖）                                                    |

### 验收标准

- [x] PAT 通过 `local.properties` 注入 BuildConfig
- [x] 重 build + 重装 APP 后，Dashboard 自动跑真实连接，**不再**需要手输
- [ ] 真实数据成功显示（进行中）

***

## 未来需求（占位）

## 2026-06-09 需求 3：封板 v0.3.35 — Token 柱状图 + 多项优化

老板 2026-06-09 当天累积多项新需求 / 优化 / 修 bug，全部在 v0.3.35 一次性封板。

### 🆕 runtimes 顶部 30 天 token 用量柱状图

**老板需求 2026-06-09**：
1. **顶部柱状图**（30 根柱子，1 天 1 根），**不卡片化**（只占顶部小条）
2. 数据源：从 multica 端取（参考 `http://172.26.28.80:3000/jimiiot/usage` Grafana）
3. 老板 2026-06-09 优化 1：颜色渐变（用量越大柱顶越深蓝）
4. 老板 2026-06-09 优化 2：3 个日期 label（左/中/右）
5. 老板 2026-06-09 优化 3：runtime 卡片自适应高度（不被柱状图挤压）

### 🆕 多 workspace 切换 dropdown

- 顶部标题栏可点击 → 弹 workspaces 列表
- 老板 2026-06-09 需求 1：内网连接=绿色 / 域名连接=蓝色 / 无法连接=红色
- NetworkManager 启动时 probe 内网 1 分钟，超时切域名

### 🆕 默认 Tab = Agents

- 老板 2026-06-09 需求：打开 APP 默认显示 Agents Tab（之前是 Boss）

### 🆕 任务开始/结束声音

- 任务开始=ding（系统通知音，RingtoneManager）
- 任务结束=dong（自合成 600Hz 衰减"咚"音 + 250ms 震动）
- 两者声音明显不同

### 🐛 关键 bug 修复

- **绿色标题 v0.3.33**：老板反馈"内网能连也正常使用但显示红色"
  - 真根因：之前用 `/api/health` 做 probe（server 没这个 endpoint → 404）
  - v0.3.32 改为"任何 HTTP 响应 = 通"
  - **v0.3.33 终极方案**：`refresh()` 调 `repo.me()` 成功 → **强制标 Internal(绿)**
  - 不依赖 NetworkManager probe 是否成功（保证老板能立即看到绿色）

### 📐 验收标准

- [x] 启动 app 默认显示 Agents Tab
- [x] 顶部柱状图（30 根，渐变深蓝→浅蓝）
- [x] 柱状图下面 3 个日期 label（左/中/右）
- [x] runtime 卡片不被柱状图挤压
- [x] 内网连接 → 标题绿色
- [x] 任务开始/结束 听到不同声音
- [x] GitHub release v0.3.35 + APK 上传完成

### 📊 Release 状态

- **Release URL**: https://github.com/299970/multica/releases/tag/v0.3.35
- **APK**: multica-v0.3.35-debug.apk (19.37 MB)
- **Commit**: d6cbd5f on main
- **Tag**: v0.3.35

***

## 未来需求（占位）

- [ ] Push 通知（agent 状态变化时手机推送）
- [ ] 多个 workspace 切换
- [ ] 直接在 APP 上 pause / resume agent
- [ ] 历史趋势（agent 今日跑了多少任务）
- [ ] 多语言（中英）
- [ ] Tablet 适配 / 横屏

***

## 2026-06-09 需求 4：Agents 卡片优化 — 任务数量分色 + 头像加大

### 背景
老板反馈 Agents 卡片任务数量显示异常（闲的显示1，工作的显示0），以及头像过小、任务数量不够醒目。

### 变更内容
1. **头像尺寸**：24dp → 48dp
2. **任务数量圆圈尺寸**：20dp → 28dp
3. **任务数量位置**：移到工作状态文字右边
4. **任务数量颜色**（圆圈背景/文字）：
   - 0 → 灰色(#3A3A3C / #8E8E93)
   - 1 → 蓝色(#0A84FF / White)
   - 2~4 → 橙色(#FF9F0A / White)
   - 5+ → 红色(#FF3B30 / White)
5. **任务数量计算修正**：仅统计状态为 `in_progress` 和 `todo` 的 issue，修复之前错误统计 `in_review` / `blocked` 的问题
6. **assigneeName 兜底匹配**：当 `assigneeId` 为空时，通过 `assigneeName` 与 agent `name` 匹配

### 验收标准
- [x] 头像加大至 48dp
- [x] 任务数量圆圈加大至 28dp，位于状态文字右边
- [x] 任务数量按 0/1/2~4/5+ 分色
- [x] 闲的 agent 任务数量为 0（灰色），工作的 agent 任务数量正确
- [x] GitHub release v0.3.39 + APK 上传完成

***

## 2026-06-09 需求 5：Dell x86 平板适配

### 背景
老板有一台 Dell Venue 8 7840 平板（x86 架构，Android 5.0 / API 21），需要 APP 也能安装运行。

### 设备信息
| 字段 | 值 |
|---|---|
| 设备 | Dell Venue 8 7840 |
| Android | 5.0 (Lollipop) / API 21 |
| SoC | Intel Atom Z3735D (Bay Trail)，四核 1.33GHz |
| 架构 | x86 |
| 屏幕 | 8" 1280×800 |
| RAM | 1 GB |

### 需求
1. 不影响主线版本（main 分支 minSdk=24），创建 `dell-x86` 独立分支
2. `dell-x86` 分支 minSdk 降至 21，支持 Android 5.0
3. 添加 x86 架构 APK 编译（ABI splits）
4. 解决 `java.time.OffsetDateTime` 在 API 21 上不存在的问题（coreLibraryDesugaring）

### 分支管理策略
- **主线优先**：新需求/bug 修复先在 main 开发 + 封板
- **Dell 同步**：每次主线发版后，`dell-x86` 分支 `git merge main` 同步新功能
- **Dell 独有修复**（如 desugaring 兼容）直接在 `dell-x86` 修，不合回 main
- **封板**：两条分支各自封板，版本号一致但 build 不同

### 验收标准
- [x] `dell-x86` 分支创建，minSdk=21
- [x] ABI splits 生成 x86/x86_64/arm64-v8a/armeabi-v7a + universal APK
- [x] coreLibraryDesugaring 解决 java.time 兼容性
- [x] Dell 平板安装 x86 APK 后 APP 启动正常
- [x] Runtimes / Agents / Issues / Boss 各 tab 不崩溃

***

## 2026-06-10 需求 6：v0.3.41 — 负载矩形 + 列数可配置 + 横屏适配

### 背景
老板反馈：1) 任务数量圆圈不够直观，需要改为"当前负载/合计"的矩形显示；2) 横屏时 agents 卡片应自动切换为 3 列；3) 列数应可在设置页自定义。

### 变更内容

1. **负载矩形**（替代原任务数量圆圈）
   - 位置：卡片右侧，高度=头像高度(48dp)
   - 内容：`当前负载/合计`（如 `1/3`）
   - 当前负载 = in_progress issue 数量
   - 合计 = in_progress + todo issue 数量
   - 颜色：有负载(合计>0) → 蓝色(#0A84FF)，无负载 → 灰色(#3A3A3C)

2. **"待办"状态**
   - 当 agent state=idle 但有 todo/in_progress 任务时，显示"待办"（橙色）
   - 排序优先级：工作中 > 待办 > 空闲 > 离线

3. **列数可配置**
   - 设置页新增"Agents 卡片列数"选项（1~4 列）
   - 默认：竖屏 2 列，横屏 3 列
   - 数据持久化到 SharedPreferences

4. **横屏适配**
   - 屏幕宽度 > 600dp 时自动使用横屏列数配置

### 验收标准
- [x] 负载矩形显示"当前/合计"，靠右，高度与头像齐平
- [x] 有负载蓝色，无负载灰色
- [x] 设置页可配置竖屏/横屏列数（1~4）
- [x] 横屏自动切换列数
- [x] 三台设备安装验证通过（三星 Note8 / Dell Venue 8 / 小米 MI 5X）

