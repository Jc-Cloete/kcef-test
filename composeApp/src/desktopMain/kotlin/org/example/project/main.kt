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

        // print system properties (JVM args)
        println("----- SYSTEM PROPERTIES -----")

        System.getProperties().forEach { key, value -> println("$key: $value") }

        println("----- END SYSTEM PROPERTIES -----")

        // print JVM input arguments
        println("----- JVM INPUT ARGUMENTS -----")

        java.lang.management.ManagementFactory.getRuntimeMXBean().inputArguments.forEach(::println)

        println("----- END JVM INPUT ARGUMENTS -----")

        // check module descriptor for java.awt.Button
        println("----- MODULE DESCRIPTOR FOR java.awt.Button -----")
        val awtButtonModule = Class.forName("java.awt.Button").module

        println("java.awt.Button → module: ${awtButtonModule.name}")

        awtButtonModule.descriptor.exports().filter { it.source() == "sun.awt" }.forEach {
            println("export from descriptor: ${it.source()} -> ${it.targets()}")
        }

        println("----- END MODULE DESCRIPTOR FOR java.awt.Button -----")

        // try loading AWTAccessor via try/catch
        println("----- TRY LOADING AWTAccessor -----")
        try {
            Class.forName("sun.awt.AWTAccessor").also {
                println("✅ AWTAccessor loaded in module: ${it.module.name}")
            }
        } catch (iae: IllegalAccessError) {
            iae.printStackTrace()
            println("❌ Failed to access in module: ${iae.stackTrace.firstOrNull()?.moduleName}")
        }

        println("----- END TRY LOADING AWTAccessor -----")

        println("----- OS AND JAVA VERSION DETAILS -----")
        // print OS and Java version details
        println("os.name:        ${System.getProperty("os.name")}")

        println("java.vm.name:   ${System.getProperty("java.vm.name")}")

        println("java.version:   ${System.getProperty("java.version")}")

        println("----- END OS AND JAVA VERSION DETAILS -----")

        // list module readers of java.desktop
        println("----- MODULE READERS OF java.desktop -----")

        val javaDesktopModule = Class.forName("java.awt.Component").module

        ModuleLayer.boot().modules().filter { it.canRead(javaDesktopModule) }.forEach {
            println("  ${it.name}")
        }

        println("----- END MODULE READERS OF java.desktop -----")

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
                                onInitialized { initialized = true }
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
