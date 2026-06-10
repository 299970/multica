# multica APP — 部署文档

## 1. 概述

multica APP 是一个 Android 端的 multica 管理平台状态查看器，用于实时展示 agents、runtimes、issues 等工作状态。

## 2. 目标设备

| 设备 | 分支 | 架构 | Android | APK |
|---|---|---|---|---|
| Samsung Galaxy Note 8 (SM-N9500) | main | arm64-v8a | 9.0 (API 28) | `app-debug.apk` |
| Dell Venue 8 7840 | dell-x86 | x86 | 5.0 (API 21) | `app-x86-debug.apk` |

## 3. 前置条件

### 3.1 服务器
- multica 服务端已部署并运行（Docker compose 或其他方式）
- 内网地址示例：`http://172.26.28.80:9090`
- 域名地址示例：`https://multica.299970.xyz`

### 3.2 凭证
- PAT（Personal Access Token，`mul_` 前缀），从 multica 管理后台生成
- PAT 可通过以下方式注入 APP：
  1. **编译时注入**（推荐）：在 `local.properties` 中设置 `multica.pat=<your-token>`，编译后自动填入
  2. **运行时输入**：APP 内 Settings 页面手动填写

### 3.3 开发环境
- JDK 17
- Android SDK（compileSdk 34）
- ADB（用于真机安装）

## 4. 编译

### 4.1 主线版本（ARM 设备）

```bash
# 克隆仓库
git clone https://github.com/299970/multica-app.git
cd multica-app

# 配置 PAT（可选，不配置则首次启动需手动输入）
echo "multica.pat=mul_your_token_here" >> local.properties

# 编译
./gradlew assembleDebug

# APK 位置
# app/build/outputs/apk/debug/app-debug.apk
```

### 4.2 Dell x86 版本

```bash
# 切换到 dell-x86 分支
git checkout dell-x86

# 编译（会生成多架构 APK）
./gradlew assembleDebug

# APK 位置
# app/build/outputs/apk/debug/app-x86-debug.apk        ← Dell 平板用这个
# app/build/outputs/apk/debug/app-x86_64-debug.apk
# app/build/outputs/apk/debug/app-arm64-v8a-debug.apk
# app/build/outputs/apk/debug/app-armeabi-v7a-debug.apk
# app/build/outputs/apk/debug/app-universal-debug.apk   ← 通用版（体积最大）
```

## 5. 安装

### 5.1 USB 安装（推荐）

```bash
# 查看已连接设备
adb devices

# ARM 设备（Samsung）
adb -s ce07171712dcc228027e install -r app/build/outputs/apk/debug/app-debug.apk

# x86 设备（Dell）
adb -s 53P021NA01 install -r app/build/outputs/apk/debug/app-x86-debug.apk
```

### 5.2 手动安装
1. 将 APK 文件拷贝到手机存储
2. 在手机上用文件管理器打开 APK
3. 允许"未知来源"安装
4. 点击安装

## 6. 首次配置

1. 启动 APP
2. 如果编译时未注入 PAT，点击右上角设置按钮
3. 填写服务器地址（内网或域名）和 PAT
4. 返回主界面，APP 自动连接并加载数据

## 7. 分支同步（开发者）

当 main 分支有新版本发布后，同步到 dell-x86：

```bash
git checkout dell-x86
git merge main
# 如有冲突（主要是 build.gradle.kts），保留 dell-x86 的 minSdk/splits/desugaring 配置
./gradlew clean assembleDebug
adb -s 53P021NA01 install -r app/build/outputs/apk/debug/app-x86-debug.apk
```

## 8. 版本历史

| 版本 | 日期 | 分支 | 说明 |
|---|---|---|---|
| v0.3.39 | 2026-06-09 | main | Agents 卡片优化：任务数量分色、头像加大 |
| v0.3.35 | 2026-06-09 | main | Token 柱状图、多 workspace 切换、声音通知 |
| dell-x86-v0.3.39 | 2026-06-09 | dell-x86 | 基于 v0.3.39，支持 x86 + Android 5.0 |
