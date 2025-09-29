plugins {
    id("java")
}

group = "ru.nsu.badin.javaconcurrency.taskj1"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.bouncycastle:bcpkix-jdk18on:1.77")
}

tasks.test {
    useJUnitPlatform()
}