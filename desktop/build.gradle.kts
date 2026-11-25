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

    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(compose.components.resources)

    implementation(libs.coil)
    implementation(libs.coil.network.okhttp)
    implementation(libs.coroutines.swing)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)

    testImplementation(libs.junit)
}

compose.desktop {
    application {
        mainClass = "com.nikhil.yt.desktop.MainKt"
        jvmArgs += listOf("-Djava.awt.headless=false", "-Xmx1024m")

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe, TargetFormat.Dmg, TargetFormat.Deb)
            packageName = "Velune"
            packageVersion = "1.1.2"
            description = "Velune YouTube Music Desktop Client"
        }
    }
}
