import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import com.grd.dom.App

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    CanvasBasedWindow("DOM Client") {
        App()
    }
}
