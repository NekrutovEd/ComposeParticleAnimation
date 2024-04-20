package not.cool.ed.compose.animation.particle

import android.content.Context
import androidx.annotation.CallSuper
import androidx.compose.ui.graphics.drawscope.DrawScope

interface FlightCalculator<P : Particle> {

    val isShowed: Boolean

    fun init(particle: P, startTime: Long, context: Context)
    fun getPlayTime(currentTime: Long): Long
    fun calculate(progress: Float)
    fun finish()
    fun draw(drawScope: DrawScope)

    fun interface Factory<P : Particle, FC: FlightCalculator<P>> {
        fun create(): FC
    }

    fun interface SortSelector<P : Particle, FC: FlightCalculator<P>, T : Comparable<T>> : (FC) -> T
}

abstract class BasicFlightCalculator<P : Particle> : FlightCalculator<P> {

    private var state: State = State.Created

    private var startTime: Long = 0

    final override val isShowed: Boolean
        get() = state == State.Calculated

    @CallSuper
    override fun init(particle: P, startTime: Long, context: Context) {
        this.startTime = startTime
        state = State.Initiated
    }

    final override fun getPlayTime(currentTime: Long): Long {
        return currentTime - startTime
    }

    @CallSuper
    override fun calculate(progress: Float) {
        state = State.Calculated
    }

    @CallSuper
    override fun finish() {
        state = State.Finished
    }

    private enum class State {
        Created,
        Initiated,
        Calculated,
        Finished,
    }
}
