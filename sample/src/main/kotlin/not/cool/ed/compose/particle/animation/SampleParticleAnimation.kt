package not.cool.ed.compose.particle.animation

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import not.cool.ed.compose.animation.particle.image.FlightCalculatorForImage
import not.cool.ed.compose.animation.particle.Particle
import not.cool.ed.compose.animation.particle.ParticleAnimator
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

private const val AnimationDuration = 2100
private const val CubicBezierX0 = 0.5f
private const val CubicBezierY0 = 0.6f
private const val CubicBezierX1 = 0.4f
private const val CubicBezierY1 = 0.8f

private const val HalfPartDivider = 2

private const val ParticleSize = 20

@Composable
fun SampleParticleAnimator(
    triggerChannel: ReceiveChannel<Unit>,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val startPosition = rememberStartCenterPosition()
    val sourceSize = remember(density) {
        val sizePx = with(density) { ParticleSize.dp.toPx().roundToInt() }
        IntSize(width = sizePx, height = sizePx)
    }
    val particleChannel = remember { Channel<Particle.AsImage>(10000) }
    val configuration = LocalConfiguration.current
    val screenSize = remember(configuration, density) {
        with(density) {
            IntSize(
                width = configuration.screenWidthDp.dp.toPx().roundToInt(),
                height = configuration.screenHeightDp.dp.toPx().roundToInt(),
            )
        }
    }
    LaunchedEffect(Unit) {
        launch {
            for (trigger in triggerChannel) {
                launch {
                    repeat(10) {
                        particleChannel.trySend(
                            Particle.AsImage(
                                image = R.drawable.particle_dog,
                                startPosition = startPosition,
                                targetPosition = randomPositionIn(screenSize),
                                sourceSize = sourceSize
                            )
                        )
                    }
                }
                delay(100.milliseconds)
            }
        }
    }

    ParticleAnimator(
        particleChannel = particleChannel,
        calculatorFactory = FlightCalculatorForImage.Factory(),
        sortSelector = FlightCalculatorForImage.SortSelector(),
        modifier = modifier,
        animationSpec = tween(
            durationMillis = AnimationDuration,
            easing = CubicBezierEasing(
                CubicBezierX0,
                CubicBezierY0,
                CubicBezierX1,
                CubicBezierY1,
            ),
        )
    )
}

@Composable
private fun rememberStartCenterPosition(): IntOffset {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidth = configuration.screenWidthDp.dp
    val centerPositionX = screenWidth / HalfPartDivider

    val screenHeight = configuration.screenHeightDp.dp
    val centerPositionY = screenHeight / HalfPartDivider

    return remember(configuration, density) {
        with(density) {
            IntOffset(
                x = centerPositionX.toPx().roundToInt(),
                y = centerPositionY.toPx().roundToInt(),
            )
        }
    }
}

private fun randomPositionIn(screenSize: IntSize) = IntOffset(
    x = Random.nextInt(screenSize.width),
    y = Random.nextInt(screenSize.height),
)
