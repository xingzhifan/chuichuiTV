# 自建极简壳，不依赖成熟 fork

经过对多款成熟"观影壳"的实测评估后，决定**从零自建一个极简壳**，而不是 fork 现成项目。

理由（均经实测验证）：
- **FongMi/TV**：绑定私有**定制 media3**（含弹幕 `androidx.media3.ui.danmaku`、MPV 引擎 `media3.mpvplayer`、libass 字幕、自定义预加载等），公开仓库拉不到（`androidx.media3:media` 404），用公开依赖需大改播放器层。
- **takagen99/Box**：公开 media3（1.3.1），但 Gradle 7.5/AGP 7.x 需 JDK 17（本机为 JDK21）、compileSdk 34（本机只装了 36）、targetSdk 28 老旧、且偏电视端、带 chaquo/Python 可选 mode。
- 每款成熟 fork 都各有无法绕开的工具链/依赖绑定，与"低配电视流畅 + 手机/电视 + 抗源失效 + 精简可控"的目标冲突。

而**自建极简壳**完全按我们已打通的现代工具链（SDK36 + Gradle9.7.1 + JDK21 + 公开 media3 + 阿里/腾讯镜像）与领域模型（片源→源→线路→播放地址）构建，无任何隐藏依赖、可预测、最精简、可控，且能原生实现"苹果 CMS 采集 API + JS 蜘蛛"适配层与双形态（leanback+mobile）。

代价：需自行实现源适配层与 UI 业务，工作量大于 fork；换取的是构建可靠性与完全可控。
