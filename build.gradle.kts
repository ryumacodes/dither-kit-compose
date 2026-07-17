plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.kmp.library) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.spotless)
    alias(libs.plugins.maven.publish) apply false
}

spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**")
        ktfmt().kotlinlangStyle()
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude("**/build/**")
        ktfmt().kotlinlangStyle()
        trimTrailingWhitespace()
        endWithNewline()
    }
    format("repositoryFiles") {
        target(
            "*.md",
            ".editorconfig",
            ".gitattributes",
            ".gitignore",
            "*.properties",
            "gradle/*.toml",
            ".github/**/*.yml",
            ".github/**/*.yaml",
            "docs/**/*.css",
            "docs/**/*.html",
            "docs/**/*.js",
        )
        trimTrailingWhitespace()
        endWithNewline()
    }
}
