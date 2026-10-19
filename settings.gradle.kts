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
        mavenCentral() // <-- Indispensable para que encuentre yt-dlp y ffmpeg
    }
}

rootProject.name = "MusicDownloaderConverter"
include(":app")
