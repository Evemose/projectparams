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
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("org.mockito:mockito-core:5.9.0")
    testImplementation("org.assertj:assertj-core:3.25.1")
    annotationProcessor(project(":annotationprocessors"))
    compileOnly(project(":annotationprocessors"))
    compileOnly(project(":params"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(listOf("-Xdiags:verbose"))
}
