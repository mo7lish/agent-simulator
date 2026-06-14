package ai.sarj.agentsim.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale

/**
 * A custom, no-ripple clickable: instead of Android's default grey ripple it gives a tactile
 * spring "squish" on press. Used everywhere a button/chip is tapped for a consistent game feel.
 */
fun Modifier.pressClickable(enabled: Boolean = true, scaleDown: Float = 0.92f, onClick: () -> Unit): Modifier =
    composed {
        val interaction = remember { MutableInteractionSource() }
        val pressed by interaction.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (pressed && enabled) scaleDown else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
            label = "press"
        )
        this
            .scale(scale)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled) { onClick() }
    }
