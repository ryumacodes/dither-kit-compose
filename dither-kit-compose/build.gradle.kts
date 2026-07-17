import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.maven.publish)
}

group = providers.gradleProperty("GROUP").getOrElse("io.github.ryumacodes")

version = providers.gradleProperty("VERSION_NAME").getOrElse("0.1.0-SNAPSHOT")

kotlin {
    jvmToolchain(21)

    @OptIn(ExperimentalAbiValidation::class) abiValidation()

    android {
        namespace = "com.ditherkit.compose"
        compileSdk = 36
        minSdk = 21
        compilerOptions.jvmTarget.set(JvmTarget.JVM_11)
        withHostTest {}
    }

    jvm("desktop") {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_11)
    }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(libs.compose.runtime)
            api(libs.compose.ui)
            api(libs.compose.foundation)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.compose.ui.test)
        }
        named("desktopTest").dependencies {
            implementation(compose.desktop.currentOs)
        }
    }
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    coordinates(group.toString(), "dither-kit-compose", version.toString())

    pom {
        name = "Dither Kit Compose"
        description = "Composable ordered-dithered charts and UI for Compose Multiplatform"
        inceptionYear = "2026"
        url = "https://github.com/ryumacodes/dither-kit-compose"
        licenses {
            license {
                name = "Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "https://www.apache.org/licenses/LICENSE-2.0.txt"
            }
        }
        developers {
            developer {
                id = "ryumacodes"
                name = "ryumacodes"
                url = "https://github.com/ryumacodes"
            }
        }
        scm {
            connection = "scm:git:git://github.com/ryumacodes/dither-kit-compose.git"
            developerConnection = "scm:git:ssh://git@github.com/ryumacodes/dither-kit-compose.git"
            url = "https://github.com/ryumacodes/dither-kit-compose"
        }
    }
}
