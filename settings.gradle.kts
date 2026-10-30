pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // TDLib (Telegram) — готовая сборка для Android
        maven {
            url = uri("https://jitpack.io")
            content { includeGroup("com.github.tdlibx") }
        }
    }
}
rootProject.name = "PvzList"
include(":app")
