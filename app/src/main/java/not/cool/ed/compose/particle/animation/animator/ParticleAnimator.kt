package not.cool.ed.compose.particle.animation.animator

import android.content.res.Configuration
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.TargetBasedAnimation
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random

data class Particle(
    val id: Long,
    @DrawableRes
    val image: Int,
)

private const val ParticleSize = 74

private const val AnimationDuration = 2100
private const val CubicBezierX0 = 0.5f
private const val CubicBezierY0 = 0.6f
private const val CubicBezierX1 = 0.4f
private const val CubicBezierY1 = 0.8f

private val animationChoreographer
    get() = TargetBasedAnimation(
        animationSpec = tween(
            durationMillis = AnimationDuration,
            // Траектория полета и изменения прозрачности настраиваются в функциях ниже.
            // Тут указываем места ускорения и замедления анимации
            easing = CubicBezierEasing(
                CubicBezierX0,
                CubicBezierY0,
                CubicBezierX1,
                CubicBezierY1,
            ),
        ),
        typeConverter = Float.VectorConverter,
        initialValue = 0f,
        targetValue = 1f,
    )

const val HalfPartDivider = 2

@Composable
private fun rememberStartCenterPosition(): IntOffset {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidth = configuration.screenWidthDp.dp
    val centerPositionX = screenWidth / HalfPartDivider

    val screenHeight = configuration.screenHeightDp.dp
    val centerPositionY = screenHeight / HalfPartDivider

    return remember(configuration) {
        with(density) {
            IntOffset(
                x = centerPositionX.toPx().roundToInt(),
                y = centerPositionY.toPx().roundToInt(),
            )
        }
    }
}

private fun randomTargetPosition(configuration: Configuration, density: Density): IntOffset {
    val screenWidth = configuration.screenWidthDp.dp
    val centerPositionX = with(density) { Random.nextInt(screenWidth.toPx().roundToInt()) }

    val screenHeight = configuration.screenHeightDp.dp
    val centerPositionY = with(density) { Random.nextInt(screenHeight.toPx().roundToInt()) }
    return IntOffset(
        x = centerPositionX,
        y = centerPositionY,
    )
}

@Composable
fun ParticleAnimator(
    particleFlow: SharedFlow<List<Particle>>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    var flightJob by remember { mutableStateOf<Job?>(null) }
    var flightTicker by remember { mutableLongStateOf(0) }
    val flightBuffer = remember { mutableStateMapOf<Particle, FlightCalculator>() }
    val particleInFlight = remember(flightBuffer, flightTicker) {
        flightBuffer.asSequence()
            .mapNotNull { (_, calc) -> calc.takeIf { it.state == FlightCalculator.State.Show } }
            .sortedBy { it.scale }
    }

    val startPosition = rememberStartCenterPosition()
    val calculatorBuffer = remember {
        mutableListOf<FlightCalculator>()
    }

    LaunchedEffect(Unit) {
        val bitmapBuffer = mutableMapOf<Int, ImageBitmap>()

        fun startFlight() = launch {
            do {
                val currentTime = withFrameNanos { it }
                for ((key, calculator) in flightBuffer) {
                    val playTime = currentTime - calculator.startTime
                    if (animationChoreographer.isFinishedFromNanos(playTime)) {
                        flightBuffer.remove(key)
                        calculator.finish()
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
            particleFlow.collect { particleList ->
                val currentTime = withFrameNanos { it }
                particleList.forEach { particle ->
                    val image = bitmapBuffer.getOrPut(particle.image) {
                        ImageBitmap.imageResource(context.resources, particle.image)
                    }
                    val flightCalculator = calculatorBuffer.removeLastOrNull() ?: FlightCalculator()
                    val targetPosition = randomTargetPosition(configuration, density)
                    flightCalculator.init(currentTime, image, startPosition, targetPosition)
                    flightBuffer[particle] = flightCalculator
                }

                if (flightJob?.isActive != true) {
                    flightJob = startFlight()
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {

        Canvas(modifier = Modifier) {
            particleInFlight.forEach { param ->
                drawImage(
                    image = param.image,
                    dstOffset = param.position,
                    dstSize = param.size,
                    alpha = param.opacity,
                )
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 50.dp)
        ) {
            Text(text = "Count particle: ${flightBuffer.size}")
            Text(text = "Calculator buffer: ${calculatorBuffer.size}")
        }
    }
}

private class FlightCalculator {
    var state: State = State.Init

    var startTime: Long = 0
        private set
    lateinit var image: ImageBitmap
        private set
    private var startPosition: IntOffset = IntOffset.Zero
    private var targetPosition: IntOffset = IntOffset.Zero
    private var sourceSize: IntSize = IntSize.Zero
        private set

    var position: IntOffset = IntOffset.Zero
        private set
    var size: IntSize = IntSize.Zero
        private set
    var scale: Float = 0f
        private set
    var opacity: Float = 0f
        private set

    fun init(
        startTime: Long,
        image: ImageBitmap,
        startPosition: IntOffset,
        targetPosition: IntOffset,
    ) {
        this.startTime = startTime
        this.image = image
        this.startPosition = startPosition
        this.targetPosition = IntOffset(
            x = targetPosition.x,
            y = targetPosition.y,
        )
        this.sourceSize = IntSize(
            width = ParticleSize,
            height = ParticleSize,
        )
        this.state = State.Init
    }

    fun calculate(progress: Float) {
        val newScale = functionByScale(progress)
        val particleScaledSize = sourceSize * newScale
        val diffSize = (particleScaledSize - sourceSize) / 2
        val newPosition = functionByPosition(
            progress = progress,
            startPosition = startPosition,
            targetPosition = targetPosition,
        )
        scale = newScale
        size = particleScaledSize
        opacity = functionByOpacity(progress)
        // смещаем позицию верхнего левого угла на смещение верхнего левого угла картинки после скейла
        position = newPosition - diffSize
        state = State.Show
    }

    fun finish() {
        state = State.Finish
    }

    enum class State {
        Init,
        Show,
        Finish,
    }
}

/**
 * #### Функция y=f(x) с формулой для вычисления размера
 * где X = 0..1
 *
 * Посмотреть визуализацию через [Графический калькулятор](https://www.desmos.com/calculator/1kewhzh0a6)
 * Можно рассматривать график как линию броска от x=0 до x=1
 */
@Suppress("MagicNumber")
private fun functionByScale(x: Float): Float {
    val offsetX = x.pow(3f)
    val y = -4f * offsetX + (3.7f * x) + 0.85f
    return y
}

/**
 * #### Функция y=f(x) с формулой для вычисления прозрачности
 * где X = 0..1
 *
 * Посмотреть визуализацию через [Графический калькулятор](https://www.desmos.com/calculator/3d1vbxikzc)
 */
@Suppress("MagicNumber")
private fun functionByOpacity(x: Float): Float {
    val y = -x.pow(8) + 1
    return y
}

/**
 * #### Линейная функция для вычисления позиции между start и target
 * где progress = 0..1
 *
 * @param progress 0..1
 * @param startPosition верхний левый угол начальной точки в px
 * @param targetPosition вехний левый угол конечной точки в px
 */
private fun functionByPosition(
    progress: Float,
    startPosition: IntOffset,
    targetPosition: IntOffset,
): IntOffset {
    val x = getLinearProgress(startPosition.x, targetPosition.x, progress)
    val y = getLinearProgress(startPosition.y, targetPosition.y, progress)
    return IntOffset(x = x.roundToInt(), y = y.roundToInt())
}

/**
 * Определение позиции между начальной и конечной точками по прогрессу от 0 до 1
 */
private fun getLinearProgress(start: Int, finish: Int, progress: Float): Float {
    val progressPath = (finish - start) * progress
    return start + progressPath
}

operator fun IntOffset.minus(subtrahend: IntSize): IntOffset = IntOffset(
    x = x - subtrahend.width,
    y = y - subtrahend.height,
)

operator fun IntSize.minus(subtrahend: IntSize): IntSize = IntSize(
    width = width - subtrahend.width,
    height = height - subtrahend.height,
)

operator fun IntSize.times(scaleFactor: Float): IntSize = IntSize(
    width = (width * scaleFactor).roundToInt(),
    height = (height * scaleFactor).roundToInt(),
)
