plugins {
    alias(libs.plugins.jvm)
    alias(libs.plugins.ksp)
    alias(libs.plugins.serialization)
}

group = "io.delilaheve.edg.sample"

dependencies {
    implementation(libs.kotlin.serialize.json)
    implementation(libs.exposed.core)
    implementation(project(":ExposedDaoGeneratorLibrary"))
    ksp(project(":ExposedDaoGeneratorLibrary"))
    testImplementation(libs.kotlin.test)
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}
