package `fun`.nekrutov.compose.animation

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.TargetBasedAnimation
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.LinkedList

@Composable
fun <P : Particle, C : FlightCalculator<P>> ParticleAnimator(
    particleChannel: ReceiveChannel<P>,
    calculatorFactory: FlightCalculator.Factory<P, C>,
    modifier: Modifier = Modifier,
    animationSpec: AnimationSpec<Float> = tween()
) = ParticleAnimator<P, C, Nothing>(
    particleChannel,
    calculatorFactory,
    sortSelector = null,
    modifier,
    animationSpec,
)

@Composable
fun <P : Particle, FC : FlightCalculator<P>, T : Comparable<T>> ParticleAnimator(
    particleChannel: ReceiveChannel<P>,
    calculatorFactory: FlightCalculator.Factory<P, FC>,
    sortSelector: FlightCalculator.SortSelector<P, FC, T>?,
    modifier: Modifier = Modifier,
    animationSpec: AnimationSpec<Float> = tween()
) {
    val context = LocalContext.current

    val animationChoreographer = remember {
        // Траектория полета и изменения прозрачности настраиваются в функциях ниже.
        // Тут указываем специфику проигрывания анимации целиком
        TargetBasedAnimation(
            animationSpec = animationSpec,
            typeConverter = Float.VectorConverter,
            initialValue = 0f,
            targetValue = 1f,
        )
    }

    var flightJob by remember { mutableStateOf<Job?>(null) }
    var flightTicker by remember { mutableLongStateOf(0) }
    val calculatorBuffer = remember { LinkedList<FC>() }
    val flightBuffer = remember { LinkedList<FC>() }
    val particleInFlight = remember {
        val sequence = flightBuffer.asSequence().filter { it.isShowed }
        when (sortSelector) {
            null -> sequence
            else -> sequence.sortedBy(sortSelector)
        }
    }

    LaunchedEffect(Unit) {
        fun startFlight() = launch {
            do {
                val currentTime = withFrameNanos { it }
                val iterator = flightBuffer.iterator()
                while (iterator.hasNext()) {
                    val calculator = iterator.next()
                    val playTime = calculator.getPlayTime(currentTime)
                    if (animationChoreographer.isFinishedFromNanos(playTime)) {
                        calculator.finish()
                        iterator.remove()
                        calculatorBuffer.add(calculator)
                    } else {
                        val progress = animationChoreographer.getValueFromNanos(playTime)
                        calculator.calculate(progress)
                    }
                }
                flightTicker = currentTime
            } while (flightBuffer.isNotEmpty())
        }

        launch {
            var currentTime: Long = 0
            launch {
                do currentTime = withFrameNanos { it } while (isActive)
            }
            for (particle in particleChannel) {
                val calculator =
                    calculatorBuffer.removeLastOrNull() ?: calculatorFactory.create()
                calculator.init(particle, currentTime, context)
                flightBuffer.add(calculator)

                if (flightJob?.isActive != true) {
                    flightJob = startFlight()
                }
            }
        }
    }

    Canvas(modifier = modifier) {
        @Suppress("UNUSED_EXPRESSION")
        flightTicker
        particleInFlight.forEach { it.draw(this) }
    }
}
