# LunaTV Android TV 客户端（M1 骨架）

本目录为 Android TV 客户端最小可运行骨架，包含：
- 工程结构：`app` 模块（Compose for TV）
- 入口页与页面：主页演示、搜索、详情、直播、我的收藏、播放器
- 后端对接规划：调用 `/api/login/token` 获取 Bearer Token，后续接口统一携带 `Authorization: Bearer <token>`

## 快速开始
- 使用 Android Studio 打开 `android-tv/` 目录。
- 在 `app/src/main/java/.../ApiConfig.kt` 配置后端 `BASE_URL`。
- 运行到 Android TV 模拟器或真机（Android TV 10+）。首次请在主页选择“登录”。
- 构建与签名说明见 `android-tv/BUILD.md`。

## 约定
- 登录：`POST {BASE_URL}/api/login/token`，返回 `{ token, user, exp }`。
- 所有 API：在请求头添加 `Authorization: Bearer <token>`。
- 已对接：`/api/search`、`/api/detail`、`/api/playrecords`、`/api/skipconfigs`、`/api/favorites`、`/api/live/*`。
- TV 端支持：登录/登出、搜索海报网格、详情海报、收藏列表海报、播放器操作层（倍速/清晰度/音轨/信息/跳片/下一集/重试）。

## 后续里程碑（M2/M3）
- DPAD 确认/返回/焦点高亮与回到顶部；错误重试与换源；加密存储 Token。
