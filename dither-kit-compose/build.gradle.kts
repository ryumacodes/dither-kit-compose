import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    `maven-publish`
    signing
}

group = providers.gradleProperty("GROUP").getOrElse("io.github.ryumacodes")

version = providers.gradleProperty("VERSION_NAME").getOrElse("0.1.0-SNAPSHOT")

kotlin {
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
        }
    }
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            name.set("Dither Kit Compose")
            description.set("Composable ordered-dithered charts and UI for Compose Multiplatform")
            url.set("https://github.com/ryumacodes/dither-kit-compose")
            licenses {
                license {
                    name.set("Apache License, Version 2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    distribution.set("repo")
                }
            }
            developers {
                developer {
                    id.set("ryumacodes")
                    name.set("ryumacodes")
                }
            }
            scm {
                connection.set("scm:git:git://github.com/ryumacodes/dither-kit-compose.git")
                developerConnection.set(
                    "scm:git:ssh://git@github.com/ryumacodes/dither-kit-compose.git"
                )
                url.set("https://github.com/ryumacodes/dither-kit-compose")
            }
        }
    }
    repositories {
        maven {
            name = "MavenCentral"
            url =
                uri(
                    if (version.toString().endsWith("SNAPSHOT")) {
                        "https://central.sonatype.com/repository/maven-snapshots/"
                    } else {
                        "https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/"
                    }
                )
            credentials {
                username =
                    providers.gradleProperty("mavenCentralUsername").orNull
                        ?: System.getenv("MAVEN_CENTRAL_USERNAME")
                password =
                    providers.gradleProperty("mavenCentralPassword").orNull
                        ?: System.getenv("MAVEN_CENTRAL_PASSWORD")
            }
        }
    }
}

signing {
    val key = providers.gradleProperty("signingKey").orNull ?: System.getenv("SIGNING_KEY")
    val password =
        providers.gradleProperty("signingPassword").orNull ?: System.getenv("SIGNING_PASSWORD")
    if (key != null) {
        useInMemoryPgpKeys(key, password)
        sign(publishing.publications)
    }
}
