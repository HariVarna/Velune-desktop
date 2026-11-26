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

val windowsX64ReleaseDir = layout.buildDirectory.dir("releases/windows-x64")

tasks.register("verifyWindowsX64Environment") {
    group = "compose desktop"
    description = "Verifies that the build host and JDK target Windows x64 (amd64)."
    doLast {
        val osName = System.getProperty("os.name").lowercase()
        val osArch = System.getProperty("os.arch").lowercase()
        require(osName.contains("win")) { "Windows x64 packaging requires Windows (detected: $osName)" }
        require(osArch == "amd64" || osArch == "x86_64") {
            "Windows x64 packaging requires an x64/amd64 JDK (detected: $osArch)"
        }
    }
}

tasks.register<Zip>("packageWindowsX64PortableZip") {
    group = "compose desktop"
    description = "Packages the Windows x64 portable app image and bundled runtime into a ZIP archive."
    dependsOn("verifyWindowsX64Environment", "createDistributable")
    from(layout.buildDirectory.dir("compose/binaries/main/app/Velune"))
    into("Velune")
    archiveFileName.set("Velune-Portable-$desktopVersion-x64.zip")
    destinationDirectory.set(windowsX64ReleaseDir)
}

tasks.register("packageWindowsX64Release") {
    group = "compose desktop"
    description = "Builds Windows x64 EXE, MSI, portable image, and portable ZIP into build/releases/windows-x64."
    dependsOn("verifyWindowsX64Environment", "packageExe", "packageMsi", "packageWindowsX64PortableZip")
    doLast {
        val outDir = windowsX64ReleaseDir.get().asFile
        outDir.mkdirs()

        val exeSrc = layout.buildDirectory.dir("compose/binaries/main/exe").get().asFile
            .listFiles { f -> f.extension.equals("exe", ignoreCase = true) }?.firstOrNull()
            ?: error("Generated EXE installer not found under build/compose/binaries/main/exe")
        val msiSrc = layout.buildDirectory.dir("compose/binaries/main/msi").get().asFile
            .listFiles { f -> f.extension.equals("msi", ignoreCase = true) }?.firstOrNull()
            ?: error("Generated MSI installer not found under build/compose/binaries/main/msi")

        val exeDst = File(outDir, "Velune-Setup-$desktopVersion-x64.exe")
        val msiDst = File(outDir, "Velune-$desktopVersion-x64.msi")
        exeSrc.copyTo(exeDst, overwrite = true)
        msiSrc.copyTo(msiDst, overwrite = true)

        println("Windows x64 release artifacts written to: ${outDir.absolutePath}")
        println(" - ${exeDst.name}")
        println(" - ${msiDst.name}")
        println(" - Velune-Portable-$desktopVersion-x64.zip")
    }
}
