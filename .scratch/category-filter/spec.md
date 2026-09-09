# 锤锤影视 — 分类白名单 + 两级渲染 规格

> 状态：resolved（2026-09-09 实现完成，全量回归通过；手动真机验证待完成）
> 关联：`CONTEXT.md` 词表、`.scratch/ui-redesign`（主题与大改已落地）
> 范围：`app/` 模块，mobile 与 leanback 共享同一套 Compose UI

## 背景 / 动机

用户实际使用中观察到：源返回的分类中有大量"敏感分类"以及**点进去没有视频的入口**。排查确认为两类问题：

1. **多源分类结构差异**：`MaccmsAdapter.home()` 返回 `class[]`，其中部分源（魔都、采集聚合）带 `type_pid`，是标准两级结构——一级「电影/电视剧/综艺/资讯/直播…」，二级「动作片/喜剧片/爱情片…」。而另一些源（天涯）**完全不带 pid**，所有分类平铺。

2. **程序统一按一级处理**：`MaccmsJson.categories()` 只读 `type_id`/`type_name`，丢弃 `type_pid`，把全部 40-60 个分类平铺渲染成一个网格。maccms 的片源实际挂在**叶子分类**下，父分类（如「电影」）直接请求 `videolist` 常返回空 → 用户点"电影"没视频、点"动作片"有视频。

同时，敏感分类（成人向/伦理等）需要隐藏。用户选择**白名单策略**：只显示想在词表中的分类，其余一次性全隐藏。

## 功能规格

### 1. 分类结构：补齐两级

- `Category` bean 增加 `typePid` 字段（`long`，缺省 `0`）。
- `MaccmsJson.categories()` 解析 `type_pid`；无该字段 → `0`。`JsSpiderAdapter` 复用同一解析函数，自动受益。
- 领域语义不变：**分类属于源**；`typeId` 跨源无意义，`typeName` 是白名单的匹配依据。

### 2. 首页三级处理

`Categories` 加载 `home()` 后按以下规则处理成"分组渲染"：

- **有 pid 递增结构**（至少一个分类 `typePid > 0`）：
  - 叶子 = `typePid > 0` 的分类；父级 = 叶子 `typePid` 对应的分类（按 `typeId` 关联）。
  - 渲染：父级名作为**分组标题**（不可点击、不产生请求）；组内叶子经白名单过滤后渲染为瓦片。
  - 命中规则：只看叶子自身 `typeName` 是否命中白名单；父级仅作分组标题、不参与叶子匹配（避免词表命中父级时把其下敏感分类一并放行）。
  - 某父级下无任何放行叶子 → 该分组整体隐藏。
- **平铺源**（所有分类 `typePid == 0`，如天涯）：
  - 全部按现状平铺渲染；每个分类 `typeName` 过白名单，不命中即隐藏。
- **pid=0 孤立分类**（有 pid 结构中，父级单独出现且无叶子挂靠，如魔都「伦理」）：按叶子处理，过白名单。

> 说明：两级模式中父级只做标题，绝不产生"空入口"。是否进入两级模式只看**该源是否存在叶子**，不依赖具体 pid 值。

### 3. 白名单词表与匹配

- **存储**：`CategoryBlockerRepo`（Java，仿 `SourceRepo`），`JsonPref<List<String>>`，prefs `chui_cat` / key `allowTerms`。
  - `load()`：无存储值 → 返回内置默认词表；有值 → 返回持久化值。
  - `save(List<String>)`。
- **匹配**（纯函数 `CategoryFilter.isAllowed(typeName, terms)`）：`terms` 任一 trim 后非空词，使 `typeName`（忽略大小写）包含该词 → 放行；否则隐藏。`typeName` 为空 → 放行（不误杀无分类入口）。
- **内置默认词表**：
  - 一级兼容（供平铺源/父级验证）：电影、电视剧、动漫、综艺
  - 叶子：动作片、喜剧片、爱情片、科幻片、奇幻片、冒险片、恐怖片、惊悚片、悬疑片、犯罪片、剧情片、战争片、纪录片、动画片、武侠片、灾难片、大陆剧、国产剧、港剧、台剧、韩剧、日剧、美剧、英剧、泰剧、海外剧、日漫、国漫、少儿

### 4. 过滤点

- **首页 `Categories`**：`home()` 结果走「两级分组 + 白名单」渲染（规格 2）。
- **搜索 `SearchScreen`**：每页 `search(pg)` 结果按 `Vod.typeName` 过白名单；**整页滤空时自动顺延请求下一页**，直到拿到非空页或到达该页总数上限（pageCount）。保证合法结果不被"敏感整页"截断。
- **`VodListScreen`（分类列表）**：入口已被首页过滤，不在列表层重复过滤（不变量：真正被屏蔽的分类进不来）。
- **详情 / 播放 / 历史**：不过滤（"眼不见"式屏蔽；用户明确点进详情后仍可播放）。

### 5. UI

- `SourcesScreen` 顶栏增加「分类屏蔽」文字入口（位于「批量导入」左侧），点击打开 `AllowTermsDialog`：
  - 多行 `OutlinedTextField`，每行一个词；
  - 底部「恢复默认」「取消」「保存」；
  - 保存后立即持久化并（在返回首页后）生效。
- 术语：用户可见文案用「分类屏蔽」，内部措辞为白名单/allowTerms。

### 6. 边界与错误处理

- 词表为空（用户全删）→ 全部隐藏；`AllowTermsDialog` 保存时若结果为空则提示"词表为空将隐藏全部分类"并允许继续。
- `Category.typeName` 为空 → 不过滤（放行），见规格 3。
- 内置默认词表仅在用户从未保存时生效；保存一次后以持久化值为准。「恢复默认」恢复内置词表。
- 解析异常（网络错误、`home()` 抛异常）维持现有 `loadFromSource` 行为（落空列表，`Categories` 显示空态）。

### 7. 测试

- `CategoryFilterTest`（JVM 单测）：
  - 命中 / 未命中；大小写无关；空词表（全部隐藏）；空 typeName（放行）；trim 词。
- 两级分组析构：已抽为纯函数 `CategoryFilter.buildGroups(raw, terms)`（返回 `CategoryGroup` 列表，`title==null` 表示无标题组），`CategoryFilterTest` 对平铺 / 两级 / 孤立 pid=0 / 父命中不放开叶子等形态均有单测覆盖。
- 全量验证命令：`.\gradlew.bat assembleMobileDebug assembleLeanbackDebug testMobileDebugUnitTest`（JDK21，`JAVA_HOME=C:\Program Files\Java\jdk-21`）。

## 明确不做（Non-goals）

- 不做 per-source 白名单（词表全局、跨源按文本匹配）。
- 不做敏感内容识别（如片名/详情关键词）、不做导入源时的自动分类探测。
- 不做分页 total 修正（`VodPage.total` 保持源原始值；被滤空自动顺延以 pageCount 为界）。
- 不做解禁到单次会话的历史交互（如"临时显示"）。

## 字段 / 接口备忘

- 新增：`Category.typePid`（long）
- 新增：`category/CategoryBlockerRepo.java`（`chui_cat`/`allowTerms`）
- 新增：`category/CategoryFilter.java`（`isAllowed(typeName, terms)` + `buildGroups(raw, terms)`）
- 新增：`category/CategoryGroup.java`（分组 VO：`title` 可 null，`categories`）
- 修改：`api/MaccmsJson.java:categories()` 解析 `type_pid`
- 修改：`ui/Categories.kt`（分级分组渲染 + 白名单）
- 修改：`ui/SearchScreen.kt`（逐页过滤 + 滤空顺延）
- 修改：`ui/SourcesScreen.kt`（「分类屏蔽」入口 + `AllowTermsDialog`）