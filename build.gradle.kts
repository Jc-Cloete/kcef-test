import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

group = "com.sieveo"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
    maven("https://jogamp.org/deployment/maven")
}

dependencies {
    implementation(compose.desktop.common)
    implementation(compose.runtime)
    runtimeOnly("org.jetbrains.skiko:skiko-awt-runtime-linux-x64:0.8.11")
}

kotlin {
    sourceSets {
        main {
            dependencies {
                api("io.github.kevinnzou:compose-webview-multiplatform:1.9.40")
            }
        }
        test{
            dependencies {
                api("io.github.kevinnzou:compose-webview-multiplatform:1.9.40")
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "kcef-test"
            packageVersion = "1.0.0"
        }

        jvmArgs("--add-opens", "java.desktop/sun.awt=ALL-UNNAMED")
        jvmArgs("--add-opens", "java.desktop/java.awt.peer=ALL-UNNAMED") // recommended but not necessary

        if (System.getProperty("os.name").contains("Mac")) {
            jvmArgs("--add-opens", "java.desktop/sun.lwawt=ALL-UNNAMED")
            jvmArgs("--add-opens", "java.desktop/sun.lwawt.macosx=ALL-UNNAMED")
        }
    }
}
