package not.cool.ed.compose.animation.particle

import androidx.annotation.DrawableRes
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

interface Particle {

    class AsImage(
        @get:DrawableRes
        val image: Int,
        val startPosition: IntOffset,
        val targetPosition: IntOffset,
        val sourceSize: IntSize? = null,
    ) : Particle
}
