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
import java.lang.ModuleLayer
import java.lang.management.ManagementFactory
import kotlin.jvm.java
import kotlin.math.max
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        var restartRequired by remember { mutableStateOf(false) }
        var downloading by remember { mutableStateOf(0F) }
        var initialized by remember { mutableStateOf(false) }
        val download: Download = remember { Builder().github().build() }
        val state = rememberWebViewState("https://google.com")

        // 0) Catch anything you missed
        Thread.setDefaultUncaughtExceptionHandler { t, ex ->
            println("***** UNCAUGHT EXCEPTION in thread ${t.name} *****")
            ex.printStackTrace()
        }

        // 1) System props
        runCatching {
            println(">>> SYSTEM PROPERTIES")
            System.getProperties().forEach { k, v -> println("$k = $v") }
        }
                .onFailure { it.printStackTrace() }

        // 2) JVM args
        runCatching {
            println(">>> JVM INPUT ARGUMENTS")
            ManagementFactory.getRuntimeMXBean().inputArguments.forEach(::println)
        }
                .onFailure { it.printStackTrace() }

        // Prepare the current (unnamed) module to use for opens/exports checks
        val currentModule = this::class.java.module

        // 2.5) Module exports/opens for sun.awt
        runCatching {
            println(":>> MODULE EXPORTS/OPENS FOR sun.awt by $currentModule")
            ModuleLayer.boot().modules().sortedBy { it.name }.forEach { mod ->
                println(
                        "  ${mod.name.padEnd(30)} opens: ${mod.isOpen("sun.awt", currentModule)} exports: ${mod.isExported("sun.awt", currentModule)}"
                )
            }
            println(":>> END MODULE EXPORTS/OPENS FOR sun.awt")
        }
                .onFailure { it.printStackTrace() }

        // 3) Module descriptor
        runCatching {
            println(">>> MODULE DESCRIPTOR FOR java.awt.Button")
            val mod = Class.forName("java.awt.Button").module
            println("Module: ${mod.name}")
            mod.descriptor.exports().filter { it.source() == "sun.awt" }.forEach {
                println("export: ${it.source()} -> ${it.targets()}")
            }
        }
                .onFailure { it.printStackTrace() }

        // 4) AWTAccessor load
        runCatching {
            println(">>> LOADING sun.awt.AWTAccessor")
            val cls = Class.forName("sun.awt.AWTAccessor")
            println("Loaded in module: ${cls.module.name}")
        }
                .onFailure { it.printStackTrace() }

        // 4.5) OS and Java version details
        runCatching {
            println(":>> OS AND JAVA VERSION DETAILS")
            println("os.name:        ${System.getProperty("os.name")}  ")
            println("java.vm.name:   ${System.getProperty("java.vm.name")}  ")
            println("java.version:   ${System.getProperty("java.version")}  ")
            println(":>> END OS AND JAVA VERSION DETAILS")
        }
                .onFailure { it.printStackTrace() }

        // 4.6) Module readers of java.desktop
        runCatching {
            val javaDesktopModule = Class.forName("java.awt.Component").module
            println(":>> MODULE READERS OF java.desktop")
            ModuleLayer.boot().modules().filter { it.canRead(javaDesktopModule) }.forEach {
                println("  ${it.name}")
            }
            println(":>> END MODULE READERS OF java.desktop")
        }
                .onFailure { it.printStackTrace() }

        // 5) Coroutines + KCEF.init
        LaunchedEffect(Unit) {
            val handler = CoroutineExceptionHandler { _, e ->
                println(">>> UNHANDLED COROUTINE EXCEPTION")
                e.printStackTrace()
            }
            withContext(Dispatchers.IO + handler) {
                runCatching {
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
                        .onFailure { it.printStackTrace() }
            }
        }

        // 6) WebView / UI
        runCatching {
            if (restartRequired) {
                Text(text = "Restart required.")
            } else {
                if (initialized) {
                    WebView(state, modifier = Modifier.fillMaxSize())
                } else {
                    Text(text = "Downloading $downloading%")
                }
            }
        }
                .onFailure { it.printStackTrace() }

        DisposableEffect(Unit) { onDispose { KCEF.disposeBlocking() } }
    }
}
