import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

group = rootProject.group
version = "${rootProject.version}"
base {
    archivesName.set("${rootProject.name}-Common")
}

val generateBuildConfig by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/sources/buildConfig/main/java")
    val projectVersion = rootProject.version.toString()
    outputs.dir(outputDir)
    doLast {
        val pkg = "net.frankheijden.serverutils.common"
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
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("net.kyori:adventure-platform-api:${VersionConstants.adventurePlatformVersion}") {
        exclude("net.kyori", "adventure-api")
        exclude("net.kyori", "adventure-text-minimessage")
    }
    compileOnly("net.kyori:adventure-text-minimessage:${VersionConstants.adventureMinimessageVersion}")
    compileOnly("com.github.FrankHeijden:ServerUtilsUpdater:5f722b10d1")

    testImplementation("net.kyori:adventure-text-serializer-plain:${VersionConstants.adventureVersion}")
}

tasks.withType<ShadowJar> {
    exclude("plugin.yml")
    exclude("bungee.yml")
}
