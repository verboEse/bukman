import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("net.minecrell.plugin-yml.bukkit") version "0.5.0"
}

group = rootProject.group
val rootDependencyDir = "net.frankheijden.serverutils.dependencies"
val dependencyDir = "net.frankheijden.serverutils.bukkit.dependencies"
version = rootProject.version
base {
    archivesName.set("${rootProject.name}-Bukkit")
}

dependencies {
    implementation("org.zhdev.oblak:cloud-paper:${VersionConstants.cloudVersion}")
    implementation("net.kyori:adventure-api:${VersionConstants.adventureVersion}") {
        exclude("net.kyori", "adventure-text-minimessage")
    }
    implementation("net.kyori:adventure-platform-bukkit:${VersionConstants.adventurePlatformVersion}") {
        exclude("net.kyori", "adventure-api")
        exclude("net.kyori", "adventure-text-minimessage")
    }
    implementation("net.kyori:adventure-text-minimessage:${VersionConstants.adventureMinimessageVersion}") {
        exclude("net.kyori", "adventure-api")
    }
    implementation("org.bstats:bstats-bukkit:${VersionConstants.bstatsVersion}")
    implementation(project(":Common"))
    compileOnly("com.destroystokyo.paper:paper-api:1.16.5-R0.1-SNAPSHOT") {
        exclude("net.kyori", "*")
    }
}

tasks.withType<ShadowJar> {
    relocate("org.bstats", "${dependencyDir}.bstats")
}

bukkit {
    name = "Bukman"
    main = "net.frankheijden.serverutils.bukkit.ServerUtils"
    description = "A server utility"
    apiVersion = "1.13"
    website = "https://git.zhira.net/zhdev/bukman"
    softDepend = listOf("ServerUtilsUpdater")
    authors = listOf("FrankHeijden", "rozhur")
}
