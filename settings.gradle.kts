pluginManagement {
    plugins {
        id("com.google.devtools.ksp") version "1.9.22-1.0.17"
    }
    repositories {
        gradlePluginPortal()
        google()
    }
}

rootProject.name = "ExposedDaoGenerator"

include("ExposedDaoGeneratorSample")
include("ExposedDaoGeneratorLibrary")
