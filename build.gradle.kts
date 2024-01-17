plugins {
    id("java")
}

group = "org.projectparams"
version = "unspecified"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    annotationProcessor(project(":annotationprocessors"))
    compileOnly(project(":annotationprocessors"))
}

tasks.test {
    useJUnitPlatform()
}