# 基于 catvod 开源系 fork，但重建轻量原生 View UI

底座选自**猫影视 / catvod 开源系**，**具体 fork `github.com/FongMi/TV`**（基于 CatVod 的开源 Android 影音应用，自带 `leanback`(电视) + `mobile`(手机) 双 flavor，及 `catvod/`(Spider 抽象层+OkHttp)、`quickjs/`(JS 引擎) 模块），直接复用其"JS 爬虫(spider)"源解析模型与配置生态——这是最抗"源失效/站点结构变"的方案（改脚本无需重装 App）。UI 采用**原生 Java + 经典 View（XML + RecyclerView / leanback）**，而非 Compose：离 catvod 原栈最近、fork 代价最小，且在低配 SOC 上经典 View 的渲染开销最可预测。

理由：同一份源头是"低配小米电视用过 OK影视 卡顿"。卡顿主要来自重 UI/WebView、解析在主线程无缓存、默认分辨率不封顶。因此定位为：原生轻量 leanback UI、所有源解析放后台线程并加两级缓存、电视端默认较低码率、ExoPlayer(Media3) 硬解。这样既拿到 JS 爬虫的抗失效能力，又保住低配电视的流畅度。
