import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

group = "${rootProject.group}"
val dependencyDir = "net.frankheijden.serverutils.velocity.dependencies"
version = rootProject.version
base {
    archivesName.set("${rootProject.name}-Velocity")
}

val generateBuildConfig by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/sources/buildConfig/main/java")
    val projectVersion = rootProject.version.toString()
    outputs.dir(outputDir)
    doLast {
        val pkg = "net.frankheijden.serverutils.velocity"
        val dir = outputDir.get().dir(pkg.replace('.', '/'))
        dir.asFile.mkdirs()
        dir.file("BuildConfig.java").asFile.writeText(
            """package $pkg;

/** Auto-generated build configuration - do not edit manually. */
public final class BuildConfig {

    public static final String VERSION = "$projectVersion";

    private BuildConfig() {}
}
"""
        )
    }
}

sourceSets {
    main {
        java {
            srcDir(generateBuildConfig.map { it.outputs.files.singleFile })
        }
    }
}

tasks.compileJava {
    dependsOn(generateBuildConfig)
}

repositories {
    maven("https://nexus.velocitypowered.com/repository/maven-public/")
    maven("https://libraries.minecraft.net")
}

dependencies {
    implementation("cloud.commandframework:cloud-velocity:${VersionConstants.cloudVersion}")
    implementation("org.bstats:bstats-velocity:${VersionConstants.bstatsVersion}")
    implementation(project(":Common"))
    implementation("net.kyori:adventure-text-minimessage:${VersionConstants.adventureMinimessageVersion}") {
        exclude("net.kyori", "adventure-api")
    }
    compileOnly("com.velocitypowered:velocity-api:3.1.2-SNAPSHOT")
    compileOnly("com.velocitypowered:velocity-brigadier:1.0.0-SNAPSHOT")
    compileOnly("com.electronwill.night-config:toml:3.6.3")
    annotationProcessor("com.velocitypowered:velocity-api:3.1.2-SNAPSHOT")
}

tasks.withType<ShadowJar> {
    relocate("org.bstats", "${dependencyDir}.bstats")
}
