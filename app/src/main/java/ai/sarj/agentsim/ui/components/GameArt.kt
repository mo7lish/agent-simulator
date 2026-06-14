package ai.sarj.agentsim.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ai.sarj.agentsim.R

/** Central registry mapping game state to the generated art drawables. */

fun rankDrawableRes(rankId: String): Int = when (rankId) {
    "trainee" -> R.drawable.rank_1_trainee
    "junior" -> R.drawable.rank_2_junior
    "agent" -> R.drawable.rank_3_agent
    "senior" -> R.drawable.rank_4_senior
    "specialist" -> R.drawable.rank_5_specialist
    "lead" -> R.drawable.rank_6_lead
    "lead_star" -> R.drawable.rank_7_lead_star
    else -> R.drawable.rank_1_trainee
}

/** Returns the medal drawable for a grade, or null if there's no art yet (e.g. "B", "—"). */
fun medalDrawableRes(grade: String): Int? = when (grade) {
    "S" -> R.drawable.medal_s
    "A" -> R.drawable.medal_a
    "B" -> R.drawable.medal_b
    "C" -> R.drawable.medal_c
    else -> null
}

@Composable
fun CoinIcon(size: Dp = 18.dp) =
    Image(painterResource(R.drawable.icon_coin), "tokens", Modifier.size(size))

@Composable
fun StarIcon(filled: Boolean, size: Dp = 16.dp) =
    Image(painterResource(if (filled) R.drawable.icon_star_full else R.drawable.icon_star_empty), null, Modifier.size(size))

@Composable
fun XpIcon(size: Dp = 16.dp) =
    Image(painterResource(R.drawable.icon_xp), "xp", Modifier.size(size))

@Composable
fun ComboFlameIcon(size: Dp = 16.dp) =
    Image(painterResource(R.drawable.icon_combo_flame), null, Modifier.size(size))

@Composable
fun BrandLogo(size: Dp = 88.dp) =
    Image(painterResource(R.drawable.logo_mark), "Agent Simulator", Modifier.size(size))

@Composable
fun Mascot(size: Dp = 120.dp) =
    Image(painterResource(R.drawable.mascot), null, Modifier.size(size))

@Composable
fun RankEmblem(rankId: String, size: Dp = 64.dp) =
    Image(painterResource(rankDrawableRes(rankId)), null, Modifier.size(size))

@Composable
fun MedalImage(grade: String, size: Dp) {
    val res = medalDrawableRes(grade) ?: return
    Image(painterResource(res), "grade $grade", Modifier.size(size))
}
