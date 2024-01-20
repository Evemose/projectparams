plugins {
    id("java")
}

group = "org.projectparams"
version = "1.0-SNAPSHOT"

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
    annotationProcessor("com.google.auto.service:auto-service:1.1.1")
    implementation("com.google.auto.service:auto-service:1.1.1")
    // TODO: Remove this dependency
    implementation("net.bytebuddy:byte-buddy:1.14.11")
    implementation(project(":params"))
    // TODO: decide between spoon and javaparser or none
    implementation("com.github.javaparser:javaparser-symbol-solver-core:3.25.8")
    implementation("com.github.javaparser:javaparser-core:3.25.8")
    implementation("fr.inria.gforge.spoon:spoon-core:10.4.2")

    implementation("org.ow2.asm:asm-tree:9.6")
    implementation("org.ow2.asm:asm:9.6")
    implementation("org.ow2.asm:asm-commons:9.6")
    implementation("org.ow2.asm:asm-util:9.6")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED")
    options.compilerArgs.add("--add-exports=jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED")
    options.compilerArgs.add("--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED")
}