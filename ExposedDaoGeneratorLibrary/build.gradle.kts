plugins {
    alias(libs.plugins.jvm)
    alias(libs.plugins.serialization)
    alias(libs.plugins.ksp)
    id("maven-publish")
}

group = "io.delilaheve.edgl"

dependencies {
    implementation(libs.kotlin.serialize.json)
    implementation(libs.ksp.api)
    implementation(libs.exposed.core)
    implementation(libs.bundles.kotlin.poet)
    testImplementation(libs.kotlin.test)
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
            version = libs.versions.self.get()
            pom.packaging = "jar"
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
