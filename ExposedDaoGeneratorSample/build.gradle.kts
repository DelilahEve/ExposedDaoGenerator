plugins {
    kotlin("jvm") version "1.9.22"
    id("com.google.devtools.ksp")
}

group = "io.delilaheve.edg.sample"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":ExposedDaoGeneratorLibrary"))
    implementation("org.jetbrains.exposed:exposed-core:0.48.0")

    ksp(project(":ExposedDaoGeneratorLibrary"))

    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}
