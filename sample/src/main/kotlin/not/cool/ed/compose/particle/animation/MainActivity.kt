package not.cool.ed.compose.particle.animation

import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import not.cool.ed.compose.animation.particle.Particle
import not.cool.ed.compose.particle.animation.ui.theme.ComposeParticleAnimationTheme
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration.Companion.milliseconds

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ComposeParticleAnimationTheme {
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
    var isButtonPress by remember { mutableStateOf(false) }
    val triggerChannel = remember { Channel<Unit>() }
    val requestDisallowInterceptTouchEvent = remember { RequestDisallowInterceptTouchEvent() }

    LaunchedEffect(isButtonPress) {
        if (isButtonPress) {
            launch {
                do {
                    triggerChannel.trySend(Unit)
                    delay(100.milliseconds)
                } while (isButtonPress)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        SampleParticleAnimator(
            triggerChannel = triggerChannel,
            modifier = Modifier.fillMaxSize(),
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
            Text(text = stringResource(id = R.string.main_button))
        }
    }
}
