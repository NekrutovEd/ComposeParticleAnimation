package `fun`.nekrutov.compose.animation.image

import androidx.compose.ui.unit.IntOffset
import kotlin.math.pow
import kotlin.math.roundToInt

internal class DefaultFunctionsForImage : FlightCalculatorForImage.Functions {
    /**
     * #### Function y=f(x) with size calculation formula
     * where X = 0..1
     *
     * View visualization via [Graphing Calculator](https://www.desmos.com/calculator/1kewhzh0a6).
     * You can think of the graph as a flight line from x=0 to x=1.
     */
    @Suppress("MagicNumber")
    override fun byScale(x: Float): Float {
        val offsetX = x.pow(3f)
        val y = -4f * offsetX + (3.7f * x) + 0.85f
        return y
    }

    /**
     * #### Function y=f(x) with formula for calculating transparency
     * where X = 0..1
     *
     * View visualization via [Graphing Calculator](https://www.desmos.com/calculator/3d1vbxikzc).
     */
    @Suppress("MagicNumber")
    override fun byOpacity(x: Float): Float {
        val y = -x.pow(8) + 1
        return y
    }

    /**
     * #### Linear function to calculate the position between start and target
     * where progress = 0..1
     *
     * @param progress 0..1
     * @param startPosition top left corner of the start point in pixels
     * @param targetPosition top left corner of the endpoint in pixels
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
     * Determining the position between the start and end points by progressing from 0 to 1
     */
    private fun getLinearProgress(start: Int, finish: Int, progress: Float): Float {
        val progressPath = (finish - start) * progress
        return start + progressPath
    }
}
