# multica APP — 设计与开发文档（Design & Development Guide）

> 本文档用于记录：开发环境、技术选型、项目结构、真机调试、注意事项、可复现命令。所有改动按"## YYYY-MM-DD 变更"小节追加。

---

## 1. 目标设备

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
