pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // RetroAchievements/api-kotlin is only published via JitPack - see CLAUDE.md's
        // RetroAchievements networking exception. repositoriesMode is FAIL_ON_PROJECT_REPOS,
        // so this has to live here rather than at the module level.
        maven(url = "https://www.jitpack.io")
    }
}

rootProject.name = "ES-DE Companion"
include(":app")
