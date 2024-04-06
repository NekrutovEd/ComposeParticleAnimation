package not.cool.ed.compose.particle.animation

import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.RequestDisallowInterceptTouchEvent
import androidx.compose.ui.input.pointer.pointerInteropFilter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import not.cool.ed.compose.particle.animation.animator.Particle
import not.cool.ed.compose.particle.animation.animator.ParticleAnimator
import not.cool.ed.compose.particle.animation.ui.theme.ComposeParticleAnimationTheme
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration.Companion.milliseconds

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ComposeParticleAnimationTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Content()
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Content(
    modifier: Modifier = Modifier
) {
    val particleFlow = remember {
        MutableSharedFlow<List<Particle>>(
            extraBufferCapacity = 100
        )
    }

    var isButtonPress: Boolean by remember {
        mutableStateOf(false)
    }


    val particleCounter = remember { AtomicLong(0) }
    val requestDisallowInterceptTouchEvent = remember { RequestDisallowInterceptTouchEvent() }

    LaunchedEffect(isButtonPress) {
        if (isButtonPress) {
            launch {
                do {
                    particleFlow.emit(
                        buildList {
                            repeat(1) {
                                add(
                                    Particle(
                                        id = particleCounter.getAndIncrement(),
                                        image = R.drawable.particle_dog
                                    )
                                )
                            }
                        }
                    )
                    delay(100.milliseconds)
                } while (isButtonPress)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        ParticleAnimator(
            particleFlow = particleFlow.asSharedFlow()
        )
        Button(
            onClick = {},
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .pointerInteropFilter(requestDisallowInterceptTouchEvent) { event ->
                    if (event.pointerCount > 1) isButtonPress = false
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> isButtonPress = true
                        MotionEvent.ACTION_UP -> isButtonPress = false
                        MotionEvent.ACTION_CANCEL -> isButtonPress = false
                    }
                    true
                }
        ) {
            Text(text = "Go")
        }
    }
}
