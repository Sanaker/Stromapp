pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            name = "JitPack" // Optional, but good for clarity in logs
            url = java.net.URI.create("https://jitpack.io/") // Using java.net.URI directly
            // You could also add credentials if JitPack ever required them for private repos,
            // but for public ones like MPAndroidChart, it's not needed.
            // credentials {
            //     username = "yourUsername"
            //     password = "yourPassword"
            // }
            // Forcing it to look for releases (MPAndroidChart v3.1.0 is a release)
            // metadataSources {
            //     mavenPom()
            //     artifact()
            //     gradleMetadata() // If available
            // }
        }
    }
}

rootProject.name = "Stromapp"
include(":app")