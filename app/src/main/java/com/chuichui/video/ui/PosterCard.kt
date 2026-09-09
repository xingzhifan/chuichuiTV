package com.chuichui.video.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.chuichui.video.bean.Vod
import com.chuichui.video.ui.theme.BadgeBg
import com.chuichui.video.ui.theme.BadgeText
import com.chuichui.video.ui.theme.Success
import com.chuichui.video.ui.theme.Surface
import com.chuichui.video.ui.theme.TextSecondary

/**
 * 海报卡片：固定 2:3 封面 + 标题 + 备注角标。全站列表/网格/详情的通用组件。
 * 封面异步加载（Coil），占位/失败用表面色兜底，避免布局抖动。
 */
@Composable
fun PosterCard(
    vod: Vod,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(8.dp))
                .background(Surface)
        ) {
            AsyncImage(
                model = vod.vodPic,
                contentDescription = vod.vodName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f),
            )
            if (!vod.vodRemarks.isNullOrEmpty()) {
                Text(
                    vod.vodRemarks,
                    style = MaterialTheme.typography.labelSmall,
                    color = BadgeText,
                    maxLines = 1,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .background(Success, RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                )
            }
            // 底部淡入背景，承载片名的可读性
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(36.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, BadgeBg)
                        )
                    )
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            vod.vodName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        vod.vodYear?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(2.dp))
            Text(
                it,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
            )
        }
    }
}
