plugins {
    kotlin("jvm") version "1.9.22"
    id("maven-publish")
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
    implementation("com.squareup:kotlinpoet-ksp:1.16.0")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = "io.delilaheve"
            artifactId = "dao-gen"
            version = "1.0.1"
            pom.packaging = "jar"
//            artifact("$buildDir/libs/ExposedDaoGeneratorLibrary.jar")
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/DelilahEve/ExposedDaoGenerator")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
