package com.chuichui.video.ui.theme

import androidx.compose.ui.graphics.Color

// ── 深蓝黑底 + 蓝紫渐变高亮 —— 腾讯视频风格暗色配色 ──────────────

// 背景/表面
val BlueBlack = Color(0xFF0F1115)      // 页面背景：深蓝黑
val Surface = Color(0xFF1A1B22)        // 卡片/列表表面
val SurfaceHigh = Color(0xFF23252E)    // 更高层级表面（输入框/选中底）
val SurfaceVariant = Color(0xFF2A2D38) // 置灰表面

// 主色 / 高亮（蓝紫渐变两端）
val AccentBlue = Color(0xFF2979FF)     // 渐变亮端：蓝
val AccentPurple = Color(0xFF5E35B1)   // 渐变暗端：紫
val Primary = Color(0xFF4D8DFF)        // 主色：介于蓝紫的可用蓝
val OnPrimary = Color(0xFFFFFFFF)

// 文字
val TextPrimary = Color(0xFFF2F3F5)    // 主文字：近白
val TextSecondary = Color(0xFF9CA3B0)  // 次要文字
val TextOnSurface = Color(0xFFF2F3F5)

// 语义色
val Success = Color(0xFF4CAF50)        // 备注/更新角标：绿
val Error = Color(0xFFD4506C)          // 错误/删除
val Warning = Color(0xFFFFB74D)        // 警告/提示

// 渐变遮罩（详情页封面 --> 背景）
val ScrimBottom = Color(0xE60F1115)    // 封面底部渐变到背景色

// 角标底色（半透明）
val BadgeBg = Color(0x99000000)
val BadgeText = Color(0xFFFFFFFF)
