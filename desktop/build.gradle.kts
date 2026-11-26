import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":innertube"))
    implementation(project(":lrclib"))
    implementation(project(":kugou"))
    implementation(project(":betterlyrics"))
    implementation(project(":simpmusic"))
    implementation(project(":canvas"))

    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(compose.components.resources)

    implementation("com.github.Kyant0:m3color:2025.4")
    implementation(libs.coil)
    implementation(libs.coil.network.okhttp)
    implementation(libs.coroutines.swing)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.serialization.json)

    testImplementation(libs.junit)
}

val desktopVersion = providers.gradleProperty("veluneDesktopVersion").getOrElse("1.1.2")

compose.desktop {
    application {
        mainClass = "com.nikhil.yt.desktop.MainKt"
        jvmArgs += listOf("-Djava.awt.headless=false", "-Xmx1024m")

        buildTypes.release.proguard {
            configurationFiles.from(project.file("proguard-rules.pro"))
        }

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe, TargetFormat.Dmg, TargetFormat.Deb)
            packageName = "Velune"
            packageVersion = desktopVersion
            description = "Velune YouTube Music Desktop Client"
            vendor = "HariVarna (Fork of Velune by Nikhil)"
            copyright = "Copyright (C) 2026 Velune Contributors - GPL-3.0"
            licenseFile.set(rootProject.file("LICENSE"))

            includeAllModules = true

            val appResourcesDir = project.layout.projectDirectory.dir("appResources")
            if (appResourcesDir.asFile.exists()) {
                appResourcesRootDir.set(appResourcesDir)
            }

            windows {
                iconFile.set(project.file("icons/velune.ico"))
                menuGroup = "Velune"
                menu = true
                shortcut = true
                dirChooser = true
                perUserInstall = true
                upgradeUuid = "8f4b2c1a-7d3e-4e9a-9b2f-6c1d8e5a3f01"
            }

            macOS {
                bundleID = "com.nikhil.yt.desktop"
                val icns = project.file("icons/velune.icns")
                if (icns.exists()) {
                    iconFile.set(icns)
                }
            }

            linux {
                packageName = "velune"
                iconFile.set(project.file("icons/velune.png"))
                menuGroup = "AudioVideo"
            }
        }
    }
}
