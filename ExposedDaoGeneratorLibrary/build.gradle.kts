plugins {
    kotlin("jvm") version "1.9.22"
}

group = "io.delilaheve.edgl"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    implementation("com.google.devtools.ksp:symbol-processing-api:1.9.21-1.0.15")
    implementation("org.jetbrains.exposed:exposed-core:0.48.0")
    implementation("com.squareup:kotlinpoet:1.16.0")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}