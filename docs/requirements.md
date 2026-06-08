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

## 功能

### 基本要求

1. 打开APP 后，屏幕保持常亮；
2. 整体风格用暗色，苹果风格；
3. 整体风格紧凑型
4. 整体页面要具有响应式能力，即在不同屏幕尺寸下都能正常显示；
5. 整体的页面相关卡片，会实时根据状态调整 ，比如颜色变化，状态变化，顺序排序；

### 页面展示

1. 用黑色背景；
2. 请以全屏显示，隐藏状态栏；

#### 页面展示

1. 用黑色背景；
2. 内容的卡片，需要做成自适应高度和宽度的

#### runtimes

1. 现在已经接入multica 的daemons，竖排展示，每个daemon 有一个状态图标，点击图标可以查看该daemon 的详细状态。
2. 以runtimes 主机一行即可，不用显示具体里面的AI，意思是有几个主机接入，就显示几个卡片即可，不用按照接入主机的AI 来划分卡片；
3. 每个卡片显示 一个runtimes，，包括
   a. 第一行：runtimes名称，状态（红圆点表示，绿色在线，红色离线，黄色异常）
   b. 第二行：runtimes 的IP地址和端口
   c. 第三行：runtimes 下有几个 AI；

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
b. 第二行：agent的头像， 工作状态，当前的任务；
c. 第三行：agent 上一次工作的时间，年月日时分秒；

##### 要求
1. 排序先按优先级在线状态，工作>空闲>离线
2. 排序其次再按照名称排序
3. 卡片以灰色展示
4. 工作中的卡片使用边框闪烁蓝色
5. 当任何一个agents有任务开始，就发出叮一声，结束任务， 就发出嘟一声；

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

- [ ] Push 通知（agent 状态变化时手机推送）
- [ ] 多个 workspace 切换
- [ ] 直接在 APP 上 pause / resume agent
- [ ] 历史趋势（agent 今日跑了多少任务）
- [ ] 多语言（中英）
- [ ] Tablet 适配 / 横屏

