package ai.sarj.agentsim.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import ai.sarj.agentsim.R
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.sarj.agentsim.model.Review
import ai.sarj.agentsim.ui.theme.SarjFaint
import ai.sarj.agentsim.ui.theme.SarjLine
import ai.sarj.agentsim.ui.theme.SarjMuted
import ai.sarj.agentsim.ui.theme.SarjOnDark
import ai.sarj.agentsim.ui.theme.Star

@Composable
fun StarRow(stars: Int, starSize: Int = 16) {
    Row {
        for (i in 1..5) StarIcon(filled = i <= stars, size = starSize.dp)
    }
}

@Composable
fun RatingChip(rating: Float, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StarIcon(filled = true, size = 18.dp)
        Spacer(Modifier.width(3.dp))
        Text(
            if (count == 0) "–" else "%.1f".format(rating),
            color = SarjOnDark, fontWeight = FontWeight.Bold, fontSize = 16.sp
        )
    }
}

@Composable
fun ReviewsTicker(reviews: List<Review>, modifier: Modifier = Modifier) {
    val r = reviews.firstOrNull()
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        if (r == null) {
            Text("no reviews yet — keep your stars up", color = SarjFaint, fontSize = 11.sp)
        } else {
            Text(r.customerName, color = SarjMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(6.dp))
            StarRow(r.stars, starSize = 11)
            Spacer(Modifier.width(6.dp))
            Text(
                "“${r.text}”", color = SarjFaint, fontSize = 11.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis, fontStyle = FontStyle.Italic
            )
        }
    }
}

/**
 * The "Last Star" reveal — a small, NON-BLOCKING banner at the top of the play area. No scrim, no tap-swallow,
 * so it never covers your screen or stops you working. Auto-dismisses when the ViewModel clears pendingReview.
 */
@Composable
fun ReviewReveal(review: Review) {
    Box(Modifier.fillMaxSize().padding(top = 8.dp), contentAlignment = Alignment.TopCenter) {
        Surface(
            shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 8.dp,
            modifier = Modifier.padding(horizontal = 12.dp).widthIn(max = 380.dp)
        ) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Avatar(review.customerName, review.accentColor, review.mood, size = 36)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    key(review.id) { AnimatedStars(review.stars, starSize = 18) }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${review.customerName}: “${review.text}”", color = SarjMuted, fontSize = 11.sp,
                        maxLines = 2, overflow = TextOverflow.Ellipsis, fontStyle = FontStyle.Italic
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedStars(stars: Int, starSize: Int = 34) {
    Row {
        for (i in 1..5) {
            val scale = remember { Animatable(0f) }
            androidx.compose.runtime.LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(110L + i * 110L)
                scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            }
            Image(
                painter = painterResource(if (i <= stars) R.drawable.icon_star_full else R.drawable.icon_star_empty),
                contentDescription = null,
                modifier = Modifier.size(starSize.dp).scale(scale.value)
            )
        }
    }
}
