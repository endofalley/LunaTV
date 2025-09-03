# Android TV 构建与签名指南

本指南帮助你在本地构建 LunaTV Android TV 客户端并生成可安装的 APK（或 AAB）。

## 前置
- 安装 Android Studio（含 SDK Platform 34、Build-Tools）。
- JDK 17（Android Studio 自带即可）。

## 本地调试构建
1. 打开 `android-tv/` 目录。
2. 配置后端地址：`app/src/main/java/com/moontv/tv/ApiConfig.kt` 的 `BASE_URL`。
3. 选择设备（Android TV 模拟器或真机）并运行。

## Release 签名（示例）
1. 生成签名证书（如无）：
   keytool -genkeypair -v -keystore tv-release.keystore -alias tv -keyalg RSA -keysize 2048 -validity 3650

2. 在 `~/.gradle/gradle.properties` 写入（勿提交到仓库）：
   TV_STORE_FILE=/absolute/path/to/tv-release.keystore
   TV_STORE_PASSWORD=your_store_password
   TV_KEY_ALIAS=tv
   TV_KEY_PASSWORD=your_key_password

3. 在 `app/build.gradle.kts` 中添加（示例，保持注释或在本地修改）：
   android {
     signingConfigs {
       create("release") {
         storeFile = file(System.getenv("TV_STORE_FILE") ?: findProperty("TV_STORE_FILE") as String)
         storePassword = System.getenv("TV_STORE_PASSWORD") ?: findProperty("TV_STORE_PASSWORD") as String
         keyAlias = System.getenv("TV_KEY_ALIAS") ?: findProperty("TV_KEY_ALIAS") as String
         keyPassword = System.getenv("TV_KEY_PASSWORD") ?: findProperty("TV_KEY_PASSWORD") as String
       }
     }
     buildTypes {
       getByName("release") {
         isMinifyEnabled = false
         signingConfig = signingConfigs.getByName("release")
       }
     }
   }

4. 生成签名包：
   ./gradlew :app:assembleRelease
   输出：`app/build/outputs/apk/release/app-release.apk`

## CI 构建（GitHub Actions）
- 已提供工作流：`.github/workflows/android-tv.yml`
- Debug Job：默认触发，产出 `app/build/outputs/apk/debug/*.apk`
- Release Job（可选）：配置以下仓库机密后生效：
  - `TV_STORE_FILE`：将 keystore 二进制 Base64 编码后的字符串
  - `TV_STORE_PASSWORD`：keystore 密码
  - `TV_KEY_ALIAS`：Key 别名
  - `TV_KEY_PASSWORD`：Key 密码
  工作流会在 `android-tv/` 下生成 `tv-release.keystore`，并传给 Gradle 构建签名包。

## 提示
- TV 首次启动请使用主页“登录”入口，登录成功后会持久化 Token；如 401 会自动清空 Token 需重新登录。
- 生产部署请在服务端设置 `CORS_ALLOW_ORIGIN` 为明确来源，避免 `*`。
