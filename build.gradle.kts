import java.nio.file.Files

plugins {
    application
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.0"
}

group = "com.stackspot.labs"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val acpVersion: String by project
val ktorVersion: String by project
val logbackVersion: String by project

dependencies {
    testImplementation(kotlin("test"))
    implementation("com.agentclientprotocol:acp:$acpVersion")
    implementation ("com.agentclientprotocol:acp-ktor:${acpVersion}")
    implementation ("com.agentclientprotocol:acp-ktor-server:$acpVersion")

    implementation("io.ktor:ktor-server-netty:${ktorVersion}")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-websockets:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    implementation("ch.qos.logback:logback-classic:$logbackVersion")
}

val userBinDir = System.getenv("HOME") + "/.local/bin"
val scriptName = application.applicationName

tasks.register("install") {
    group = "Installation"
    description = "Symlinks the installed script to \$HOME/.local/bin"
    dependsOn("installDist")

    doLast {
        val buildDir = layout.buildDirectory.dir("install").get().asFile.path
        val installBin = file("$buildDir/${project.name}/bin/$scriptName")
        val targetDir = file(userBinDir)
        val link = File(targetDir, scriptName)

        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        if (link.exists() || Files.isSymbolicLink(link.toPath())) {
            link.delete()
        }

        Files.createSymbolicLink(link.toPath(), installBin.toPath())
        println("Symlinked $installBin to $link")
    }
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(24)
}

application {
    mainClass.set("com.stackspot.labs.acpserver.MainKt")
}