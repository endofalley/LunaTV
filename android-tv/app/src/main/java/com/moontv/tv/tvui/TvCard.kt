package com.moontv.tv.tvui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Text
import com.moontv.tv.R

@Composable
fun TvPosterCard(
    imageUrl: String?,
    contentDescription: String?,
    title: String? = null,
    showTitleOverlay: Boolean = true,
    topLeftBadge: String? = null,
    topRightBadge: String? = null,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(2f / 3f)
            .clip(RoundedCornerShape(8.dp))
            .onFocusChanged { focused = it.isFocused }
            .border(
                width = if (focused) 2.dp else 0.dp,
                brush = SolidColor(Color.White),
                shape = RoundedCornerShape(8.dp)
            )
            .graphicsLayer {
                val scale = if (focused) 1.08f else 1f
                scaleX = scale; scaleY = scale
            }
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = contentDescription,
            placeholder = painterResource(id = R.drawable.poster_placeholder),
            error = painterResource(id = R.drawable.poster_error),
            modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f)
        )

        if (!topLeftBadge.isNullOrBlank()) {
            Text(
                text = topLeftBadge,
                color = Color.White,
                modifier = Modifier
                    .align(androidx.compose.ui.Alignment.TopStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0x88000000))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        if (!topRightBadge.isNullOrBlank()) {
            Text(
                text = topRightBadge,
                color = Color.White,
                modifier = Modifier
                    .align(androidx.compose.ui.Alignment.TopEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0x88000000))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        if (showTitleOverlay && !title.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(androidx.compose.ui.Alignment.BottomCenter)
                    .height(48.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xAA000000))
                        )
                    )
            )
            Text(
                text = title,
                color = Color.White,
                modifier = Modifier
                    .align(androidx.compose.ui.Alignment.BottomStart)
                    .padding(8.dp)
            )
        }
    }
}
