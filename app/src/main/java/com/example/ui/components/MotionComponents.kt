package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.formatRupiah
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Emerald800
import com.example.ui.theme.Emerald950
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.Teal800
import kotlinx.coroutines.delay
import kotlin.math.roundToLong

val EmphasizedEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

fun directionalTabTransition(forward: Boolean): ContentTransform =
    (fadeIn(tween(durationMillis = 260, easing = EmphasizedDecelerate)) +
        slideInHorizontally(tween(durationMillis = 280, easing = EmphasizedEasing)) { width ->
            if (forward) width / 8 else -width / 8
        } +
        scaleIn(
            initialScale = 0.985f,
            animationSpec = tween(durationMillis = 260, easing = EmphasizedDecelerate)
        )) togetherWith
        (fadeOut(tween(durationMillis = 170, easing = FastOutSlowInEasing)) +
            slideOutHorizontally(tween(durationMillis = 210, easing = EmphasizedEasing)) { width ->
                if (forward) -width / 10 else width / 10
            })

fun portalSwitchTransition(): ContentTransform =
    (fadeIn(tween(durationMillis = 360, easing = EmphasizedDecelerate)) +
        scaleIn(
            initialScale = 0.96f,
            animationSpec = tween(durationMillis = 360, easing = EmphasizedDecelerate)
        )) togetherWith
        (fadeOut(tween(durationMillis = 220, easing = FastOutSlowInEasing)) +
            scaleOut(
                targetScale = 1.02f,
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
            ))

@Composable
fun AppLaunchSplashOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    val logoScale = remember { Animatable(0.76f) }
    val logoAlpha = remember { Animatable(0f) }
    val textOffsetY = remember { Animatable(18f) }

    LaunchedEffect(Unit) {
        logoAlpha.animateTo(1f, animationSpec = tween(320, easing = EmphasizedDecelerate))
    }
    LaunchedEffect(Unit) {
        logoScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = 0.68f,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }
    LaunchedEffect(Unit) {
        delay(90)
        textOffsetY.animateTo(0f, animationSpec = tween(360, easing = EmphasizedDecelerate))
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(150)),
        exit = fadeOut(tween(340, easing = FastOutSlowInEasing)) +
            scaleOut(
                targetScale = 1.06f,
                animationSpec = tween(340, easing = EmphasizedEasing)
            ),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Emerald950, Emerald800, Teal800)))
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Emerald500.copy(alpha = 0.28f), Color.Transparent),
                            center = Offset(size.width * 0.8f, size.height * 0.2f),
                            radius = size.maxDimension * 0.45f
                        ),
                        radius = size.maxDimension * 0.45f,
                        center = Offset(size.width * 0.8f, size.height * 0.2f)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.1f), Color.Transparent),
                            center = Offset(size.width * 0.18f, size.height * 0.82f),
                            radius = size.maxDimension * 0.36f
                        ),
                        radius = size.maxDimension * 0.36f,
                        center = Offset(size.width * 0.18f, size.height * 0.82f)
                    )
                }
                .testTag("app_launch_splash_overlay"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(horizontal = 32.dp)
            ) {
                Surface(
                    color = Color(0xFF09523E),
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.28f)),
                    shadowElevation = 20.dp,
                    modifier = Modifier
                        .size(92.dp)
                        .graphicsLayer {
                            scaleX = logoScale.value
                            scaleY = logoScale.value
                            alpha = logoAlpha.value
                        }
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_art),
                        contentDescription = "Logo Blok CE PSA",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.graphicsLayer {
                        translationY = textOffsetY.value
                        alpha = logoAlpha.value
                    }
                ) {
                    Text(
                        text = "Blok CE PSA",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Paguyuban Warga Perumahan Silih Asih",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Emerald100.copy(alpha = 0.92f)
                    )
                }

                Surface(
                    color = Color.White.copy(alpha = 0.14f),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                    modifier = Modifier.graphicsLayer { alpha = logoAlpha.value }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LivePulseDot(color = Emerald500, size = 7.dp)
                        Text(
                            text = "Menyiapkan portal warga...",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedRupiahText(
    targetAmount: Long,
    style: TextStyle = MaterialTheme.typography.headlineSmall,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight = FontWeight.Black,
    useMonospace: Boolean = false,
    maxLines: Int = 1,
    modifier: Modifier = Modifier
) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(targetAmount) {
        anim.animateTo(
            targetValue = targetAmount.toFloat(),
            animationSpec = tween(durationMillis = 680, easing = FastOutSlowInEasing)
        )
    }
    val displayed = anim.value.roundToLong().coerceAtLeast(0L)
    Text(
        text = formatRupiah(displayed),
        style = if (useMonospace) style.copy(fontFamily = JetBrainsMonoFamily) else style,
        color = color,
        fontWeight = fontWeight,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
fun AnimatedPercentText(
    targetPercent: Int,
    suffix: String = "%",
    style: TextStyle = MaterialTheme.typography.titleMedium,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight = FontWeight.Black,
    modifier: Modifier = Modifier
) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(targetPercent) {
        anim.animateTo(
            targetValue = targetPercent.toFloat(),
            animationSpec = tween(durationMillis = 720, easing = FastOutSlowInEasing)
        )
    }
    Text(
        text = "${anim.value.toInt()}$suffix",
        style = style.copy(fontFamily = JetBrainsMonoFamily),
        color = color,
        fontWeight = fontWeight,
        modifier = modifier
    )
}

@Composable
fun StaggeredEntrance(
    delayMillis: Int = 0,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (delayMillis > 0) delay(delayMillis.toLong())
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 300, easing = EmphasizedDecelerate)) +
            slideInVertically(
                initialOffsetY = { it / 6 },
                animationSpec = spring(
                    dampingRatio = 0.84f,
                    stiffness = Spring.StiffnessMediumLow
                )
            ),
        modifier = modifier
    ) {
        content()
    }
}

@Composable
fun LivePulseDot(
    color: Color,
    size: Dp = 8.dp,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "live_pulse")
    val pulseScale by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infinite.animateFloat(
        initialValue = 0.55f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = modifier.size(size * 2),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                    alpha = pulseAlpha
                }
                .clip(CircleShape)
                .background(color)
        )
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(color)
        )
    }
}

fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.97f
): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "press_scale"
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
