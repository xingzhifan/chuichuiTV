# 技术栈重定：Kotlin + Jetpack Compose（含 Compose for TV）

自建极简壳（ADR-0004）落地后，把应用技术栈从早先定的"Java + 经典 View"重定为 **Kotlin + Jetpack Compose（电视端含 Compose for TV）**。

理由：
- 早期"Java + 经典 View"的选择建立在 **fork 重 UI 在低配电视上卡顿**的前提上；那个性能问题源于重 UI / 无缓存，而非 UI 框架本身。自建壳是全新、精简的工程，此前提不再成立。
- Compose 是官方主推的现代 Android UI 方案，`Compose for TV` 对 D-pad / 焦点 / leanback 形态支持已成熟；与领域模型驱动的声明式 UI（状态→界面）契合，配合"抗源失效"的聚合层更易表达加载/空态/故障态。
- 全新工程没有历史包袱，趁 UI 层尚未铺开时切换成本最低。

代价与约束：
- 已写好的 Java+View UI（分类首页/列表/详情/播放）需重写为 Compose。
- 领域/适配层仍是 Java（Kotlin 可直接调用），后续随重构 Kotlin 化。
- 低配电视上须遵守性能红线：克制动画与层级、列表虚拟化，渲染开销回到可预测范围。

决策记录时间：2026-08（fork 否定、ADR-0004 落地之后，由用户重新决策）。
