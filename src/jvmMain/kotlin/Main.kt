import androidx.compose.material.MaterialTheme
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.material.darkColors
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.round
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.sun.javafx.application.PlatformImpl
import javafx.application.Platform
import javafx.concurrent.Worker
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.web.*
import kotlinx.coroutines.*
import netscape.javascript.JSObject
import java.awt.BorderLayout
import javax.swing.JPanel

@Composable
@Preview
fun App(frameWindowScope: FrameWindowScope) {

    MaterialTheme(colors = darkColors()) {
        Surface(color = MaterialTheme.colors.background) {

            Box(Modifier.fillMaxSize()) {
                val webViewConfig =
                    remember { WebViewConfig(frameWindowScope.window, "https://stackoverflow.com/") }.also {
                        it.configCallback = object : WebViewConfigCallback {}
                    }
                WebView(webViewConfig)

            }
        }
    }
}

fun main() = application(exitProcessOnExit = true) {
    val finishListener = object : PlatformImpl.FinishListener {
        override fun idle(implicitExit: Boolean) {}
        override fun exitCalled() {}
    }
    PlatformImpl.addListener(finishListener)


    Window(onCloseRequest = {
        PlatformImpl.removeListener(finishListener)
        exitApplication()
    }) {
        App(this)
    }
}

/*
* WebView should be called in the UI Thread
* */
@Composable
fun WebView(webViewConfig: WebViewConfig, modifier: Modifier = Modifier) {
    val jfxPanel = remember { JFXPanel() }
    var jsObject = remember<JSObject?> { null }
    ComposeJFXPanel(
        modifier = modifier,
        composeWindow = webViewConfig.composeWindow,
        jfxPanel = jfxPanel,
        onCreate = {
            Platform.runLater {
                val callBacks = webViewConfig.configCallback
                callBacks.onWebPageStarted()
                val root = WebView()
                val engine = root.engine

                webViewConfig.setEngine(engine)

                val scene = Scene(root)
                engine.loadWorker.stateProperty().addListener { _, _, newState ->
                    if (newState == Worker.State.SUCCEEDED) {
                        jsObject = root.engine.executeScript("window") as JSObject
                    }
                }
                jfxPanel.scene = scene
                engine.load(webViewConfig.url)
                engine.setOnError { error -> callBacks.onWebPageError(error) }
                engine.isJavaScriptEnabled = webViewConfig.isJavaScriptEnabled

                callBacks.onWebPageFinished()
                var title = engine.title
                callBacks.onTitleUpdated(title)
                engine.setOnStatusChanged {
                    callBacks.onStateChanged(engine, it)
                    if (title != engine.title) {
                        title = engine.title
                        callBacks.onTitleUpdated(title)
                    }
                }
                callBacks.onEngineReady(engine)

            }
        }, onDestroy = {
            Platform.runLater {
                jsObject?.let {

                }
            }
        }
    )
}

@Composable
fun ComposeJFXPanel(
    composeWindow: ComposeWindow,
    jfxPanel: JFXPanel,
    onCreate: () -> Unit,
    onDestroy: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val jPanel = remember { JPanel() }
    val density = LocalDensity.current.density

    Layout(
        content = {},
        modifier = Modifier.onGloballyPositioned { childCoordinates ->
            val coordinates = childCoordinates.parentCoordinates!!
            val location = coordinates.localToWindow(Offset.Zero).round()
            val size = coordinates.size
            jPanel.setBounds(
                (location.x / density).toInt(),
                (location.y / density).toInt(),
                (size.width / density).toInt(),
                (size.height / density).toInt()
            )
            jPanel.validate()
            jPanel.repaint()
        }.then(modifier),
        measurePolicy = { _, _ -> layout(0, 0) {} })

    DisposableEffect(jPanel) {
        composeWindow.add(jPanel)
        jPanel.layout = BorderLayout(0, 0)
        jPanel.add(jfxPanel)
        onCreate()
        onDispose {
            onDestroy()
            composeWindow.remove(jPanel)
        }
    }
}
