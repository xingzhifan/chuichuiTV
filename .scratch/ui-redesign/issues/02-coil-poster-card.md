# 02 — 图片加载基础设施 + PosterCard 组件

**What to build:** 新增 Coil 依赖，并实现全站复用的海报卡片组件 `PosterCard`（渲染 `vodPic`），作为所有列表/网格/详情的基础。

**Blocked by:** 01

**Status:** resolved（build+单测已验证）

- [x] `app/build.gradle`：加 `implementation 'io.coil-kt:coil-compose:X'`（版本与 Compose BOM 兼容）。
- [x] 新建 `ui/PosterCard.kt`：固定 2:3 宽高比、`8.dp` 圆角、Coil 异步加载 `vodPic`、占位/失败态、渐变高亮边框或角标、标题 + 备注（`vodRemarks`）角标。
- [x] 暴露 `PosterCard(vod, ...)` 供 grid/list/详情复用。
- [x] 验证编译 + 单测通过。

## Progress

- `build.gradle`：`io.coil-kt:coil-compose:2.7.0`。
- 新增 `ui/PosterCard.kt`：Column（2:3 封面 Box + 标题 + 年份）；Coil `AsyncImage` `ContentScale.Crop`；封面圆角 `RoundedCornerShape(8.dp)` + 表面色兜底；`vodRemarks` 绿色(Success)左上角标；底部渐变遮罩承载片名。
- 验证：`assembleMobileDebug`+`assembleLeanbackDebug`+`testMobileDebugUnitTest` BUILD SUCCESSFUL。
