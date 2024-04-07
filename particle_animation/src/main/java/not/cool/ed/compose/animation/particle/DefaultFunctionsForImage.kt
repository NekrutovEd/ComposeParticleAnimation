package not.cool.ed.compose.animation.particle

import androidx.compose.ui.unit.IntOffset
import kotlin.math.pow
import kotlin.math.roundToInt

internal class DefaultFunctionsForImage : FlightCalculatorForImage.Functions {
    /**
     * #### Функция y=f(x) с формулой для вычисления размера
     * где X = 0..1
     *
     * Посмотреть визуализацию через [Графический калькулятор](https://www.desmos.com/calculator/1kewhzh0a6)
     * Можно рассматривать график как линию броска от x=0 до x=1
     */
    @Suppress("MagicNumber")
    override fun byScale(x: Float): Float {
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
    override fun byOpacity(x: Float): Float {
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
    override fun byPosition(
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
}
