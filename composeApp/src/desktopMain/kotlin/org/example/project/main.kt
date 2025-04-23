package org.example.project

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewState
import dev.datlag.kcef.KCEF
import dev.datlag.kcef.KCEFBuilder.Download
import dev.datlag.kcef.KCEFBuilder.Download.Builder
import java.io.File
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        var restartRequired by remember { mutableStateOf(false) }
        var downloading by remember { mutableStateOf(0F) }
        var initialized by remember { mutableStateOf(false) }
        val download: Download = remember { Builder().github().build() }
        val state = rememberWebViewState("https://google.com")

        // print all jvm args
        println("----- JVM ARGS -----")
        System.getProperties().forEach { key, value -> println("$key: $value") }
        println("----- JVM ARGS -----")

        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                KCEF.init(
                        builder = {
                            installDir(File("kcef-bundle"))

                            progress {
                                onDownloading {
                                    println("Downloading KCEF: $it")
                                    downloading = max(it, 0F)
                                }
                                onInitialized {
                                    println(
                                            File("kcef-bundle").listFiles()?.joinToString("\n") {
                                                    file ->
                                                file.absolutePath
                                            }
                                    )
                                    initialized = true
                                }
                            }
                            settings {
                                cachePath = File("cache").absolutePath
                                windowlessRenderingEnabled = false
                            }

                            download(download)
                        },
                        onError = { it?.printStackTrace() },
                        onRestartRequired = { restartRequired = true }
                )
            }
        }

        if (restartRequired) {
            Text(text = "Restart required.")
        } else {
            if (initialized) {
                WebView(state, modifier = Modifier.fillMaxSize())
            } else {
                Text(text = "Downloading $downloading%")
            }
        }

        DisposableEffect(Unit) { onDispose { KCEF.disposeBlocking() } }
    }
}
