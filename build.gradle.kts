plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.serialization") version "2.2.21"
    kotlin("plugin.compose") version "2.2.21"
    id("org.jetbrains.compose") version "1.11.1"
    application
}

repositories {
    mavenCentral()
    google()
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("darcy.veterinary.MainKt")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    runtimeOnly("org.xerial:sqlite-jdbc:3.46.1.0")
    runtimeOnly("org.slf4j:slf4j-nop:1.7.36")
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}

tasks.test {
    useJUnitPlatform()
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}

tasks.register<JavaExec>("runDesktop") {
    group = "application"
    description = "Run the Darcy Vet desktop GUI shell."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("darcy.veterinary.presentation.desktop.DesktopMainKt")
}
