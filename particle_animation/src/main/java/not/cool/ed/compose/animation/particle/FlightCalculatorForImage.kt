package not.cool.ed.compose.animation.particle

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import not.cool.ed.compose.animation.particle.FlightCalculator.Factory
import not.cool.ed.compose.animation.particle.FlightCalculator.SortSelector
import kotlin.math.roundToInt

open class FlightCalculatorForImage(
    protected val function: Functions,
    private val imageBuffer: MutableMap<Int, ImageBitmap>,
) : BasicFlightCalculator<Particle.AsImage>() {

    protected var size: IntSize = IntSize.Zero
    protected var scale: Float = 0f
    protected var opacity: Float = 0f
    protected var position: IntOffset = IntOffset.Zero

    private lateinit var image: ImageBitmap
    private var startPosition: IntOffset = IntOffset.Zero
    private var targetPosition: IntOffset = IntOffset.Zero
    private var sourceSize: IntSize = IntSize.Zero

    final override fun init(
        particle: Particle.AsImage,
        startTime: Long,
        context: Context,
    ) {
        this.image = imageBuffer.getOrPut(particle.image) {
            ImageBitmap.imageResource(context.resources, particle.image)
        }
        this.sourceSize = particle.sourceSize ?: IntSize(width = image.width, height = image.height)
        this.startPosition = particle.startPosition
        this.targetPosition = particle.targetPosition
        super.init(particle, startTime, context)
    }

    override fun calculate(progress: Float) {
        val newScale = function.byScale(progress)
        scale = newScale

        val particleScaledSize = sourceSize * newScale
        size = particleScaledSize

        opacity = function.byOpacity(progress)

        val newPosition = function.byPosition(progress, startPosition, targetPosition)
        val diffSize = (particleScaledSize - sourceSize) / 2
        // смещаем позицию верхнего левого угла на смещение верхнего левого угла картинки после скейла
        val newScaledPosition = newPosition - diffSize
        position = newScaledPosition

        super.calculate(progress)
    }

    override fun draw(drawScope: DrawScope) {
        drawScope.drawImage(
            image = image,
            dstOffset = position,
            dstSize = size,
            alpha = opacity,
        )
    }

    companion object {
        fun Factory(
            function: Functions = DefaultFunctionsForImage(),
            imageBuffer: MutableMap<Int, ImageBitmap> = mutableMapOf(),
        ) = Factory {
            FlightCalculatorForImage(function, imageBuffer,)
        }

        fun SortSelector() = SortSelector { calc: FlightCalculatorForImage ->
            calc.scale
        }
    }

    interface Functions {
        fun byScale(x: Float): Float
        fun byOpacity(x: Float): Float
        fun byPosition(
            progress: Float,
            startPosition: IntOffset,
            targetPosition: IntOffset
        ): IntOffset
    }
}

private operator fun IntOffset.minus(subtrahend: IntSize) = IntOffset(
    x = x - subtrahend.width,
    y = y - subtrahend.height,
)

private operator fun IntSize.minus(subtrahend: IntSize) = IntSize(
    width = width - subtrahend.width,
    height = height - subtrahend.height,
)

private operator fun IntSize.times(scaleFactor: Float) = IntSize(
    width = (width * scaleFactor).roundToInt(),
    height = (height * scaleFactor).roundToInt(),
)
