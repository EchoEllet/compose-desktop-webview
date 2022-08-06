import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.window.FrameWindowScope
import javafx.scene.web.WebEngine
import javafx.scene.web.WebErrorEvent
import javafx.scene.web.WebEvent
import javafx.scene.web.WebHistory
import kotlinx.coroutines.*

class WebViewConfig(
    var composeWindow: ComposeWindow,
    var url: String,
    var configCallback: WebViewConfigCallback = object : WebViewConfigCallback {},
    var isJavaScriptEnabled: Boolean = true,
    var webHistory: WebHistory? = null,
) {
    private var engine: WebEngine? = null
    constructor(frameWindowScope: FrameWindowScope, url: String) : this(frameWindowScope.window, url)
    constructor(frameWindowScope: FrameWindowScope, url: String, configCallback: WebViewConfigCallback) : this(frameWindowScope.window, url, configCallback)

    fun runScript() {

    }

    fun loadUrl(url: String) {
        this.url = url
    }

    fun refresh() {

    }

    suspend fun getEngine(): WebEngine? {
        return withContext(currentCoroutineContext()) {
            while (engine == null) {
                if (!isActive) break
                runBlocking {}
            }
            engine
        }
    }

    fun setEngine(engine: WebEngine) {
        this.engine = engine
    }
}


interface WebViewConfigCallback {
    fun onTitleUpdated(title: String?) = Unit
    fun onWebPageStarted() = Unit
    fun onWebPageFinished() = Unit
    fun onWebPageError(error: WebErrorEvent) = Unit
    fun onEngineReady(engine: WebEngine) = Unit
    fun onStateChanged(engine: WebEngine, handler: WebEvent<String>) = Unit
}