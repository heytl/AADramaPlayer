<div align="center">
  <img src="app/src/main/res/drawable/new_icon.png" width="128" height="128" style="border-radius: 20%">
  <h1>阿阿短剧 (AA Drama Player) 🎬</h1>
  <p>一只糯叽叽的小猪，带你开启极致简单的本地短剧观看体验。</p>
</div>

**阿阿短剧** 是一款专为本地短剧爱好者设计的极简播放器。它采用了“适老化”的设计理念，去除了所有复杂的社交和广告功能，回归播放本质。无论是年轻人还是长辈，都能一秒上手。

---

## ✨ 核心特性

- 🐷 **软萌品牌形象**：由专属“糯叽叽小猪”代言，界面温馨治愈。
- 📂 **极速本地扫描**：采用底层 `DocumentsContract` 技术，秒级扫描数千集本地短剧，支持无限层级文件夹识别。
- 🔄 **全自动无限连播**：支持跨剧集自动跳转，看毕一集自动下一集，播完一剧自动下一剧，享受无感换剧体验。
- 📱 **抖音式沉浸交互**：
  - **单击暂停/播放**：随点随停，同步显现进度条与标题，交互直观。
- 🕒 **精准进度续播**：实时同步播放记录，剧库首页“继续观看”卡片智能置顶，数据多端（列表、选集、卡片）强一致性。
- 🛠️ **目录管理系统**：支持多文件夹导入与管理，一键移除或新增资源目录。

---

## 🛠️ 技术架构

- **UI 框架**：Jetpack Compose (声明式 UI，高性能渲染)
- **播放引擎**：GSYVideoPlayer (深度定制，支持多种视频格式)
- **数据库**：Room (支持 Flow 响应式查询与关联关系映射)
- **图片/视频加载**：后台预生成 WebP 视频封面，首页由 Coil 仅加载普通图片，避免滚动时进行视频解码。
- **系统适配**：完美适配 Android 8.0 至 Android 15/16，全面兼容 SAF 存储访问框架。

---

## 🚀 快速开始

1. **环境要求**：
   - Android Studio Koala 或更高版本。
   - JDK 17+。
   - 设备 Android 8.0 (API 26) 以上。

2. **编译运行**：
   - 克隆仓库后，执行 `Gradle Sync`。
   - 点击 `Run` 部署至您的安卓设备（推荐 Android 11+ 以获得最佳存储访问体验）。

3. **导入短剧**：
   - 点击顶部导航栏的 **“+”** 图标。
   - 选择存储短剧的根文件夹，点击“允许访问”。
   - 系统将自动为您识别、分类并抓取封面。

---

## 📦 GitHub Actions 正式打包

正式打包工作流位于 `.github/workflows/android-release.yml`，使用现有 JKS 生成签名 Release APK。

1. 在仓库 **Settings → Secrets and variables → Actions → Repository secrets** 配置四项：

   | Secret | 内容 |
   | --- | --- |
   | `KEYSTORE_BASE64` | `app/JKS/my-release-key.jks` 文件的 Base64 |
   | `KEYSTORE_PASSWORD` | 本地 `signing.storePassword` 的值 |
   | `KEY_ALIAS` | 本地 `signing.keyAlias` 的值 |
   | `KEY_PASSWORD` | 本地 `signing.keyPassword` 的值 |

2. 进入 **Actions → Android Release → Run workflow**，选择 `main` 后执行。推送代码到 `main` 也会自动打包；同一分支的新运行会取消尚未完成的旧运行。
3. 成功后，在该次运行的 **Artifacts** 下载 `AA-Drama-release-apk`，解压得到 APK。`AA-Drama-release-mapping` 保存 R8 混淆映射，用于分析对应版本的异常堆栈。产物保留 30 天。

工作流使用 JDK 17、Gradle Wrapper、Android SDK 35 和 Build Tools 34.0.0，执行 `:app:assembleRelease`。构建后必须通过 APK 验签，且证书 SHA-256 与现有本地正式包一致，才会上传产物。签名配置缺失会直接失败，临时签名文件始终清理。

发布新版本前，在 `app/build.gradle.kts` 更新 `versionCode` 和 `versionName`。打包结果及源代码提交号显示在运行摘要中。

---

## 📖 版本记录

- **v1.0.0** (Initial Commit)
  - 完成品牌重塑（阿阿短剧）。
  - 实现抖音式交互逻辑与长按倍速。
  - 优化扫描性能与数据同步一致性。
  - 加入目录管理面板。

---

## 📜 许可证

本项目采用 [MIT License](LICENSE) 许可。

---

_“看短剧，就要简单点。” —— 阿阿短剧_
