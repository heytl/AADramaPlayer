pluginManagement {
    includeBuild("build-logic")
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
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "AADuanJu"
include(
    ":app",
    ":core:model",
    ":core:database",
    ":core:media",
    ":core:designsystem",
    ":domain",
    ":data",
    ":feature:library",
    ":feature:player",
    ":benchmark",
)
