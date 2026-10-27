package top.niunaijun.blackboxa.ui.components

import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun AppIconView(
    icon: Drawable?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    contentDescription: String? = null
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            AndroidView(
                factory = { context ->
                    ImageView(context).apply {
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        setImageDrawable(icon)
                    }
                },
                update = { imageView ->
                    imageView.setImageDrawable(icon)
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.Android,
                contentDescription = contentDescription ?: "App Icon",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(size * 0.65f)
            )
        }
    }
}
