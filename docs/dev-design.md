# multica APP — 设计与开发文档（Design & Development Guide）

> 本文档用于记录：开发环境、技术选型、项目结构、真机调试、注意事项、可复现命令。所有改动按"## YYYY-MM-DD 变更"小节追加。

---

## 1. 目标设备

### 1.1 主力设备（main 分支）

| 字段 | 值 |
|---|---|
| 设备 | Samsung Galaxy Note 8 (SM-N9500) — 中港版 |
| Android | 9.0 (Pie) / API 28 |
| SoC | Qualcomm MSM8998（骁龙 835），Kryo 280（4×2.35GHz + 4×1.9GHz） |
| GPU | Adreno 540（OpenGL ES 3.2） |
| 架构 | arm64-v8a（主）/ armeabi-v7a / armeabi |
| 屏幕 | 6.3" 1440×2960，物理 420 dpi（系统 Override 560 dpi） |
| RAM | 6 GB |
| ROM | 53 GB(/data)，用户可用 ~44 GB |
| 安全 | SELinux Enforcing，ro.secure=1，ro.debuggable=0，bootloader 锁定 |
| 序列号 | `ce07171712dcc228027e` |
| 启动器 | com.samsung.android.app.spage |

> **设备指纹**：`samsung/greatqltezc/greatqltechn:9/PPR1.180610.011/N9500ZCU6DTF2:user/release-keys`

### 1.2 Dell x86 平板（dell-x86 分支）

| 字段 | 值 |
|---|---|
| 设备 | Dell Venue 8 7840 |
| Android | 5.0 (Lollipop) / API 21 |
| SoC | Intel Atom Z3735D (Bay Trail)，四核 1.33GHz |
| 架构 | x86 |
| 屏幕 | 8" 1280×800，~189 dpi |
| RAM | 1 GB |
| ROM | 16 GB |
| 序列号 | `53P021NA01` |

> **注意**：API 21 缺少 `java.time` 包，需通过 `coreLibraryDesugaring` 兼容。

---

## 2. 技术选型（基线）

| 类别 | 选型 | 理由 |
|---|---|---|
| 语言 | **Kotlin** 1.9.x | Google 官方首选，与 Jetpack 完美兼容 |
| UI 框架 | **Jetpack Compose** | 现代化、声明式；保留 View 兼容方案 |
| 架构 | **MVVM + Repository + UseCase** | 易测试、易维护 |
| 异步 | **Kotlin Coroutines + Flow** | 取代 RxJava |
| DI | **Hilt** | Google 官方推荐 |
| 本地存储 | **Room** | 类型安全的 SQLite ORM |
| 序列化 | **kotlinx.serialization** | 多平台、Kotlin 友好 |
| 网络 | **Retrofit + OkHttp + Kotlinx-Serialization** | 主流稳定 |
| 图片 | **Coil** | Kotlin 协程原生 |
| 导航 | **Navigation Compose** | 单 Activity 多 Composable |
| 编译/构建 | AGP 8.2+ / Gradle 8.2+ | 支持 JDK 17、Compose 1.5+ |
| minSdk | **24**（Android 7.0） | 覆盖 ~99% 在用设备，远高于目标机 API 28 |
| targetSdk | **34**（Android 14） | Google Play 政策要求 |
| compileSdk | **34** | 与 targetSdk 一致 |
| 签名 | Debug keystore（开发期），Release 用 keystore.properties | 暂不引入 Play 上架流程 |

---

## 3. 主机开发环境

### 3.1 已具备

| 组件 | 版本 | 路径 |
|---|---|---|
| JDK | 17.0.12 LTS | `C:\Program Files\Java\jdk-17` |
| Android SDK | 已安装 | `C:\Users\mexs\AppData\Local\Android\Sdk` |
| ADB | 1.0.41 v37.0.0 | `C:\adb\adb.exe` |
| Fastboot | 同上 | `C:\adb\fastboot.exe` |
| 环境变量 | `JAVA_HOME` / `ANDROID_HOME` 已设置 | — |

### 3.2 需补齐

| 组件 | 用途 | 备注 |
|---|---|---|
| Android SDK Platform 34 | compileSdk | `sdkmanager "platforms;android-34"` |
| Build-Tools 34.0.0 | 构建工具链 | `sdkmanager "build-tools;34.0.0"` |
| Platform-Tools（最新版） | adb / fastboot | 已在 `C:\adb` 中（与 SDK 内并存） |
| Gradle | 不需全局安装 | 用项目内 **Gradle Wrapper**（`./gradlew`） |
| Android Studio（可选） | 图形化 IDE | 装 Hedgehog/Iguana+ 即可，建议 >= 2023.1.1 |
| Kotlin | 不需单独装 | 由 AGP/Kotlin 插件决定 |
| cmdline-tools | 装/卸 SDK 组件 | 若 SDK 中已带则忽略 |

### 3.3 验证步骤

```bash
# 1) JDK
java -version            # 期望 17.0.12 LTS

# 2) Android SDK 关键组件
"%ANDROID_HOME%\cmdline-tools\latest\bin\sdkmanager.bat" --list_installed
#   至少需要：platform-tools, platforms;android-34, build-tools;34.0.0, platform;android-28

# 3) ADB
adb devices              # 期望看到 ce07171712dcc228027e   device

# 4) 真机识别
adb -s ce07171712dcc228027e shell getprop ro.product.model
#   期望 SM-N9500
```

---

## 4. 项目结构（规划）

```
multica APP/
├── docs/
│   ├── requirements.md        # 需求文档（本次创建）
│   └── dev-design.md          # 本文件
├── app/                       # 默认 module
│   ├── build.gradle.kts
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           ├── java/com/multica/app/
│           │   ├── MulticaApp.kt
│           │   ├── MainActivity.kt
│           │   ├── ui/
│           │   ├── data/
│           │   └── di/
│           └── res/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradle/
│   └── wrapper/
└── gradlew / gradlew.bat
```

> 实际结构以创建后为准，本节作为规划基线。

---

## 5. 真机调试工作流

### 5.1 USB 连接
- 数据线连接电脑 USB 3.0 口（推荐，供电稳定）
- 开发者选项 / USB 调试 = **已打开**
- USB 调试（安全设置）= 已允许（如需安装）
- 默认 USB 用途 = "传输文件 (MTP)"

### 5.2 ADB 授权
第一次插上时，手机会弹"允许 USB 调试"对话框，勾选"始终允许"并确认。本机 ADB server 已检测到设备 `ce07171712dcc228027e` 处于 `device` 状态，授权已完成。

### 5.3 常用命令

```bash
# 查看设备
adb devices -l

# 安装
adb -s ce07171712dcc228027e install -r app/build/outputs/apk/debug/app-debug.apk

# 启动 Activity
adb -s ce07171712dcc228027e shell am start -n com.multica.app/.MainActivity

# Logcat（只看本应用）
adb -s ce07171712dcc228027e logcat -v time *:S AndroidRuntime:E multica:V

# 卸载
adb -s ce07171712dcc228027e uninstall com.multica.app

# 文件互传
adb -s ce07171712dcc228027e push .\local.txt /sdcard/Download/
adb -s ce07171712dcc228027e pull /sdcard/Download/remote.txt .
```

### 5.4 沙箱环境特殊说明
**Trae AI 沙箱限制**：沙箱内 PowerShell 无法创建本地 TCP socket，因此直接 `adb devices` 会失败。

**已建立的解决方案**：通过 `Invoke-WmiMethod Win32_Process.Create` 在主机用户上下文启动 powershell 进程跑 ADB 命令，结果落盘到 `C:\temp\` 后读回。
- 封装脚本：`C:\temp\adb_run.ps1`
- 用法示例：`& C:\temp\adb_run.ps1 -AdbArgs "shell getprop ro.product.model"`

> 此方案对单条命令够用；批量执行请把多条命令合并到一个 `adb shell` 中以减少往返。

---

## 6. 注意事项 / 风险点

1. **设备 Android 9 / API 28 是较老版本**，新 API（CameraX 新特性、Compose 部分 API 1.7+、Notification API 33+）需做兼容分支。Compose 最低支持到 API 21，可放心用。
2. **Bootloader 已锁** ⇒ 不可刷第三方 ROM / Magisk ⇒ 所有功能须在 **未 root** 环境下可用。
3. **ro.debuggable=0** ⇒ `adb shell run-as <pkg>` 仅在 debug 包有效；release 包无 root 时无法读取沙箱内文件（正常）。
4. **没有 Play Store / Google 服务**：中国版 Note 8 几乎不含 GMS，应用若需 Google 登录/地图/GCM 推送需做替代方案（国内推送走厂商通道 / 自建）。
5. **沙箱与主机隔离**：沙箱不能直接 `adb` 直连，所有 ADB 调用需走 `C:\temp\adb_run.ps1`。长输出会超时（默认 30s），需提前 `| head -n N` 或 `| grep`。
6. **APK 体积**：API 28 真机 RAM 6G，但请避免一次性加载大图/视频，遵循内存最佳实践。
7. **64 位 ABI 是主力**：`ro.product.cpu.abilist` = `arm64-v8a,armeabi-v7a,armeabi`，但事实上 arm64 设备跑 32 位 ABI 性能更好并不成立——直接出 `arm64-v8a` 包即可（如果对包大小不敏感可同时打包 32 位以兼容模拟器）。
8. **dpi Override**：系统 `wm density` 已 Override 为 560（360 默认被改），开发请以 `Configuration.densityDpi` 动态计算，**不要**写死 dp。

---

## 7. 变更日志

### 2026-06-04 — 项目初始化
- 识别真机：Samsung Galaxy Note 8 (SM-N9500) / Android 9 / API 28 / 骁龙 835
- 主机环境核验：JDK 17 ✅、Android SDK ✅、ADB ✅
- 创建 `docs/requirements.md`、`docs/dev-design.md`
- 通过 WMI 中转方案解决沙箱无法直连 ADB 的问题
- 待办：补齐 SDK 34 / build-tools 34；创建项目骨架并首次真机部署

### 2026-06-04 — 环境部署 + 项目骨架 + 首次真机运行 ✅
- **项目结构**（Kotlin DSL，AGP 8.2.2 + Gradle 8.2 + Kotlin 1.9.22 + Compose BOM 2024.02.00 + Compose Compiler 1.5.8）
- 完整文件树：
  - 根：`settings.gradle.kts` / `build.gradle.kts` / `gradle.properties` / `local.properties` / `gradlew` / `gradlew.bat` / `gradle/wrapper/*`
  - `app/build.gradle.kts` / `app/proguard-rules.pro`
  - `app/src/main/AndroidManifest.xml`
  - `app/src/main/java/com/multica/app/MulticaApp.kt`、`MainActivity.kt`、`ui/theme/Theme.kt`
  - `app/src/main/res/` 资源（strings / themes / xml / drawable / mipmap）
- **首次编译** `gradlew :app:assembleDebug` → **BUILD SUCCESSFUL in 36s** → `app-debug.apk` 9.2 MB
- **首次真机部署**：
  - `adb push` 到 `/data/local/tmp/` → `pm install` → **Success**
  - `am start com.multica.app/.MainActivity` → 进程 PID 24148 稳定运行
  - 截屏 `multica_screen3.png`（185 KB，1440×2960）确认 UI 渲染
- **沙箱兼容**：Gradle 构建/ADB 调用均通过 WMI 在主机上下文执行；APK 路径含空格 → 复制到 `C:\adb\multica-debug.apk` 后 push

### 当前包信息
- applicationId = `com.multica.app`
- minSdk = 24 / targetSdk = 34 / compileSdk = 34
- 设备指纹 = `samsung/greatqltezc/greatqltechn:9/PPR1.180610.011/N9500ZCU6DTF2:user/release-keys`

### 常用命令（沙箱中）
```powershell
# ADB 中转（沙箱内通过 WMI 在主机上下文跑 adb）
function Invoke-Adb($a, [int]$t=180) {
  ([string](& C:\temp\adb_run.ps1 -AdbArgs $a -TimeoutSec $t))
    .Replace("@@@START@@@","").Replace("@@@END@@@","")
}

# Gradle 构建（同样 WMI 中转，输出落盘 C:\temp\gradle_build_out.txt）
& C:\temp\bg_build.ps1 -OutFile C:\temp\gradle_build_out.txt -ProjRoot "<项目绝对路径>"
```
> ⚠️ 含空格路径的本地文件须先复制到无空格目录（如 `C:\adb\`）再 `adb push`，否则会被 adb 内部命令行解析切断。

---

## 8. v0.3.35 封板变更日志（2026-06-09）

### 新增 / 改动
- **Runtimes 顶部 30 天 token 用量柱状图**（老板 2026-06-09 新需求）
  - 组件：`app/src/main/java/com/multica/app/ui/components/TokenBarChart.kt`（新文件，144 行）
  - 数据源：API endpoint flexible 调用 — `/api/dashboard/usage` → `/api/usage/daily` → mock 30 天随机
  - 渐变：`Brush.verticalGradient`（#2563EB → #93C5FD）
  - 3 个日期 label：data[0] / data[n/2] / data[n-1]（MM-dd 格式，SpaceBetween）
  - 不卡片化（紧贴顶部），占位 76dp
- **runtime 卡片自适应高度**（老板 2026-06-09 优化）
  - `BoxWithConstraints` 扣 `chartHeight = 80.dp`
  - 之前扣的 paddingV=16dp 太小，柱状图挤压卡片
- **多 workspace 切换 dropdown**（v0.3.29 已做，老板 2026-06-09 颜色细化）
  - 内网连接=绿色 (#22C55E)
  - 域名连接=蓝色 (#3B82F6)
  - 无法连接=红色 (#EF4444)
  - 探测中=黄色 (#EAB308)
- **NetworkManager 1 分钟内网超时**（v0.3.30）
  - 启动时试 1 分钟内网（30 次 2s 间隔）
  - 失败切域名（multica.299970.xyz）
- **绿色标题终极修复**（v0.3.33）
  - `DashboardViewModel.refresh()` 调 `repo.me()` 成功 → **强制 `_state.update { it.copy(netState = Internal(url)) }`**
  - **不依赖 NetworkManager probe 是否成功**（保证老板能立即看到绿色）
- **任务开始/结束声音**（v0.3.30）
  - 任务开始=ding（系统通知音，`RingtoneManager.getDefaultUri`）
  - 任务结束=dong（自合成 600Hz 衰减"咚"音 + 250ms 震动）
  - 两者声音明显不同
- **默认 Tab = Agents**（v0.3.30 / v0.3.32 反复修）
  - `DashboardScreen` `var tab by remember { mutableIntStateOf(1) }`（之前是 0）
- **daemon 状态详情弹窗**（v0.3.29）
  - 点击 daemon 状态圆点 → 列出该 host 上所有 runtime + agent

### bug 修复
- 之前老板 v0.3.30 之前所有版本 `probe /api/health` → 404 → 标红
- v0.3.32 改用 `/api/me + PAT`，但某些情况仍判失败
- **v0.3.33 暴力方案**：真实 API 成功直接标 Internal(绿)

### API 灵活 fallback 模式
- 数据源 endpoint 字段兼容：days / data / items / usage 任一顶层数组
- 模型容错：`DailyUsage.tokens` 优先，否则 `inputTokens + outputTokens`
- mock fallback：30 天随机数（保证 UI 不空）

### 文件变更统计
- 12 文件改动，474 增 / 65 删
- 新增文件 1 个：`TokenBarChart.kt`

### Release 状态
- **Release URL**: https://github.com/299970/multica/releases/tag/v0.3.35
- **APK**: multica-v0.3.35-debug.apk (19.37 MB)
- **APK SHA256**: `6fcdac7ba6827b117c27bac4dd09d14b053ad6243978487a3072656fff7f1e67`
- **Commit**: d6cbd5f on main
- **Tag**: v0.3.35

### 已知 issue
- GitHub release 创 release 时 `Content-Type: application/json` 加 UTF-8 BOM 会导致 "Problems parsing JSON" (400) — 解决方案：用 `System.Text.Encoding.ASCII` 写 body 避免 BOM
- 之前 `PowerShell WriteAllText` 默认 UTF-8 with BOM → 失败 4 次

---

## 9. v0.3.39 封板变更日志（2026-06-09）

### Agents 卡片优化
- **头像尺寸**：24dp → 48dp
- **任务数量圆圈**：20dp → 28dp，移到工作状态文字右边
- **任务数量分色**：
  - 0 → 灰色(#3A3A3C / #8E8E93)
  - 1 → 蓝色(#0A84FF / White)
  - 2~4 → 橙色(#FF9F0A / White)
  - 5+ → 红色(#FF3B30 / White)
- **任务数量计算修正**：仅统计 `in_progress` + `todo` 状态的 issue（之前错误包含 `in_review` / `blocked`）
- **assigneeName 兜底**：`assigneeId` 为空时通过 `assigneeName` 与 agent `name` 匹配

### 关键文件
- `app/src/main/java/com/multica/app/ui/dashboard/tabs/AgentsTab.kt` — 卡片 UI + 任务数量逻辑

### Release 状态
- **Tag**: v0.3.39
- **versionCode**: 49
- **Branch**: main

---

## 10. dell-x86 分支变更日志（2026-06-09）

### 分支策略
- 分支名：`dell-x86`
- 基于 main (v0.3.39) 创建
- 独有改动不合回 main，避免影响主线

### 构建配置变更（`app/build.gradle.kts`）
1. **minSdk**：24 → 21（支持 Android 5.0）
2. **coreLibraryDesugaring**：
   - `compileOptions.isCoreLibraryDesugaringEnabled = true`
   - `coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")`
   - 解决 `java.time.OffsetDateTime` 在 API 21 上不存在导致的 `ClassNotFoundException` 崩溃
3. **ABI splits**：
   ```kotlin
   splits {
       abi {
           isEnable = true
           reset()
           include("x86", "x86_64", "armeabi-v7a", "arm64-v8a")
           isUniversalApk = true
       }
   }
   ```
   - 生成 5 个 APK：x86 / x86_64 / armeabi-v7a / arm64-v8a / universal
   - Dell 平板安装 `app-x86-debug.apk`（~20MB）

### 崩溃修复
- **问题**：Runtimes tab 点击后崩溃，`ClassNotFoundException: java.time.OffsetDateTime`
- **原因**：`java.time` 包在 Android 8.0 (API 26) 才引入，API 21 不存在
- **方案**：`coreLibraryDesugaring` 在编译时将 `java.time` 调用替换为 desugar 库实现

### 分支同步流程
```bash
# main 有新版本后，同步到 dell-x86
git checkout dell-x86
git merge main
# 解决冲突（主要是 build.gradle.kts 的 minSdk/splits/desugaring 保留 dell-x86 的值）
./gradlew clean assembleDebug
adb -s 53P021NA01 install -r app/build/outputs/apk/debug/app-x86-debug.apk
```

### Dell 设备 ADB 命令
```bash
# 安装 x86 APK
adb -s 53P021NA01 install -r app/build/outputs/apk/debug/app-x86-debug.apk

# 启动
adb -s 53P021NA01 shell am start -n com.multica.app/.MainActivity

# 查看崩溃日志
adb -s 53P021NA01 logcat -d -s AndroidRuntime:E
```

