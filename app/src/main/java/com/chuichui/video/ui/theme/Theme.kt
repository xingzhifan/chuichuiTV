package com.chuichui.video.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ChuiChuiDarkColors = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    secondary = AccentBlue,
    onSecondary = OnPrimary,
    tertiary = AccentPurple,
    background = BlueBlack,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextOnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = Error,
    onError = OnPrimary,
)

/** 锤锤影视暗色主题：深蓝黑底 + 蓝紫高亮。全站 UI 统一入口。 */
@Composable
fun ChuiChuiTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = ChuiChuiDarkColors,
        typography = AppTypography,
        content = content,
    )
}
