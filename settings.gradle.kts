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

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "Traduya"

include(":app")

// Núcleo: modelos, reglas de negocio e infraestructura compartida
include(":core:model")
include(":core:domain")
include(":core:data")
include(":core:designsystem")
include(":core:telecom")
include(":core:translation")
include(":core:audio")
include(":core:session")
include(":core:callui")
include(":core:callcapture")
include(":core:directory")

// Funcionalidades: cada pantalla vive en su propio módulo
include(":feature:onboarding")
include(":feature:dialer")
include(":feature:incall")
include(":feature:history")
include(":feature:settings")
include(":feature:internetcall")
include(":feature:wifinumber")
