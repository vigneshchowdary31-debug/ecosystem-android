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
    }
}

rootProject.name = "ecosystem-android"
include(":app")
include(":core:common")
include(":core:security")
include(":core:identity")
include(":core:trusteddevices")
include(":core:networking")
include(":core:discovery")
include(":core:pairing")
include(":core:designsystem")
include(":core:navigation")
include(":feature:pairing")
include(":feature:settings")
