import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.ksp)

    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.10"
}

kotlin {
    jvm()

    js {
        browser()
        binaries.executable()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {

            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")

            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")

            implementation("org.jetbrains.androidx.navigation:navigation-compose:2.9.1")
            implementation("com.drewnoakes:metadata-extractor:2.19.0")

            var filekit_version = "0.13.0"
            implementation("io.github.vinceglb:filekit-core:$filekit_version")
            implementation("io.github.vinceglb:filekit-dialogs:$filekit_version")
            implementation("io.github.vinceglb:filekit-dialogs-compose:$filekit_version")
            implementation("io.github.vinceglb:filekit-coil:$filekit_version")

        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)

            val room_version = "2.8.4"
            implementation("androidx.room:room-runtime:${room_version}")
            implementation("androidx.sqlite:sqlite-bundled:2.5.0-alpha11")
        }
    }
}

dependencies {
    add("kspJvm", "androidx.room:room-compiler:2.8.4")
}

compose.desktop {
    application {
        mainClass = "com.bbm.multitask.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.bbm.multitask"
            packageVersion = "1.0.0"
        }
    }
}
