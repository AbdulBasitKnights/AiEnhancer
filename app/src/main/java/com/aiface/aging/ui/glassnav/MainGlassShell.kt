package com.aiface.aging.ui.glassnav

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.aiface.aging.R
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

/**
 * Main tab shell with Kyant Backdrop liquid-glass bottom nav (blur + lens + vibrancy).
 * Closest Android match to iOS Liquid Glass; [LiquidGlassKMP] Android side is Material only.
 */
@Composable
fun MainGlassShell(
    selectedTab: Int,
    onHomeClick: () -> Unit,
    onLibraryClick: () -> Unit,
    onAiVideoClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val homeBackground = remember { Color(0xFFFDFDFD) }
    val backdrop = rememberLayerBackdrop {
        drawRect(homeBackground)
        drawContent()
    }
    val navShape = NavShape

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop),
        ) {
            content()
        }

        LiquidGlassBottomNav(
            selectedTab = selectedTab,
            onHomeClick = onHomeClick,
            onLibraryClick = onLibraryClick,
            onAiVideoClick = onAiVideoClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth()
                .height(NavHeight)
                .clip(navShape)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { navShape },
                    effects = {
                        vibrancy()
                        blur(with(density) { 6.dp.toPx() })
                        lens(
                            with(density) { 18.dp.toPx() },
                            with(density) { 36.dp.toPx() },
                        )
                    },
                    onDrawSurface = {
                        drawRect(Color.White.copy(alpha = 0.30f))
                    },
                )
                .glassEdgeBorder(navShape),
        )
    }
}

@Composable
private fun LiquidGlassBottomNav(
    selectedTab: Int,
    onHomeClick: () -> Unit,
    onLibraryClick: () -> Unit,
    onAiVideoClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activeColor = remember {
        Color(ContextCompat.getColor(context, R.color.bottom_nav_active))
    }
    val inactiveColor = remember {
        Color(ContextCompat.getColor(context, R.color.bottom_nav_inactive))
    }

    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.22f),
                        Color.White.copy(alpha = 0.05f),
                        Color.Transparent,
                    ),
                ),
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavItem(
                iconRes = R.drawable.ai_home,
                labelRes = R.string.home,
                selected = selectedTab == TAB_HOME,
                activeColor = activeColor,
                inactiveColor = inactiveColor,
                onClick = onHomeClick,
            )
            NavItem(
                iconRes = R.drawable.ai_video,
                labelRes = R.string.ai_video,
                selected = selectedTab == TAB_AI_VIDEO,
                activeColor = activeColor,
                inactiveColor = inactiveColor,
                onClick = onAiVideoClick,
            )
            NavItem(
                iconRes = R.drawable.ai_library,
                labelRes = R.string.library,
                selected = selectedTab == TAB_LIBRARY,
                activeColor = activeColor,
                inactiveColor = inactiveColor,
                onClick = onLibraryClick,
            )
        }
    }
}

@Composable
private fun NavItem(
    iconRes: Int,
    labelRes: Int,
    selected: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    onClick: () -> Unit,
) {
    val label = stringResource(labelRes)
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        animationSpec = spring(),
        label = "navScale",
    )
    val tint by animateColorAsState(
        targetValue = if (selected) activeColor else inactiveColor,
        label = "navTint",
    )

    Column(
        modifier = Modifier
            .widthIn(min = 64.dp)
            .semantics { contentDescription = label }
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
            )
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    activeColor.copy(alpha = 0.28f),
                                    activeColor.copy(alpha = 0.08f),
                                    Color.Transparent,
                                ),
                            ),
                        ),
                )
            }
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = tint,
                modifier = Modifier
                    .size(22.dp)
                    .scale(scale),
            )
        }
        Text(
            text = label,
            color = tint,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun Modifier.glassEdgeBorder(shape: RoundedCornerShape): Modifier = border(
    width = Dp.Hairline,
    brush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.85f),
            Color.White.copy(alpha = 0.22f),
        ),
    ),
    shape = shape,
)

const val TAB_HOME = 0
const val TAB_AI_VIDEO = 1
const val TAB_LIBRARY = 2

private val NavHeight = 64.dp
private val NavShape = RoundedCornerShape(32.dp)
