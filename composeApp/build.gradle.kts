import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            
            // Architecture
            implementation(libs.decompose)
            implementation(libs.decompose.extensions)
            implementation(libs.essenty.lifecycle)
            implementation(libs.koin.core)
            implementation(libs.mvikotlin)
            implementation(libs.mvikotlin.main)
            implementation(libs.mvikotlin.extensions.coroutines)
            implementation(libs.redis.jedis)

            // Network
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.syntax.area)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.ktor.client.cio)
            implementation("io.ktor:ktor-server-netty-jvm:3.0.3")
            implementation("io.modelcontextprotocol:kotlin-sdk:0.6.0")
            implementation("org.jetbrains.kotlinx:kotlinx-io-core:0.8.0")
            implementation("org.slf4j:slf4j-simple:2.0.9")
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.example.mindweaverstudio.MainKt"

        nativeDistributions {

            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.example.mindweaverstudio"

            // Если передали -Pversion → берём его, иначе дефолт
            val appVersion: String = (project.findProperty("version") as String?)
                ?.takeIf { it.matches(Regex("""\d+(\.\d+){0,2}""")) } // валидация формата
                ?: "1.0.0"

            packageVersion = appVersion

            macOS {
                packageVersion = appVersion // для DMG
                dmgPackageVersion = appVersion
            }
            windows {
                packageVersion = appVersion // для MSI
                msiPackageVersion = appVersion
            }
            linux {
                packageVersion = appVersion // для DEB
                debPackageVersion = appVersion
            }
        }
    }
}
