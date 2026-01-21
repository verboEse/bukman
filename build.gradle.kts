import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    `java-library`
    `maven-publish`
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "org.zhdev.bukman"
val dependencyDir = "net.frankheijden.serverutils.dependencies"
version = "3.5.7"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "maven-publish")
    apply(plugin = "checkstyle")
    apply(plugin = "com.github.johnrengelman.shadow")

    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    repositories {
        mavenLocal()
        mavenCentral()
        maven("https://jitpack.io")
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://libraries.minecraft.net")
        maven("https://maven.zhira.net/repository/zhdev/")
    }

    dependencies {
        implementation("org.zhdev.oblak:cloud-core:${VersionConstants.cloudVersion}")
        implementation("org.zhdev.oblak:cloud-brigadier:${VersionConstants.cloudVersion}")
        implementation("org.zhdev:megareflection:1.0.2-SNAPSHOT")
        implementation("com.google.code.gson:gson:2.8.6")
        implementation("me.lucko:commodore:2.2")
        compileOnly("com.mojang:brigadier:1.0.18")

        testImplementation("org.assertj:assertj-core:3.18.1")
        testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
        testImplementation("org.junit.jupiter:junit-jupiter-params:5.7.0")
        testImplementation("org.junit.jupiter:junit-jupiter-engine:5.7.0")
    }

    tasks {
        build {
            dependsOn("shadowJar", "checkstyleMain", "checkstyleTest", "test")
        }

        compileJava {
            options.encoding = Charsets.UTF_8.name()
            options.isDeprecation = true
        }

        javadoc {
            options.encoding = Charsets.UTF_8.name()
        }

        processResources {
            filteringCharset = Charsets.UTF_8.name()
        }

        test {
            useJUnitPlatform()
        }
    }

    tasks.withType<Checkstyle>().configureEach {
        configFile = file("${rootDir}/config/checkstyle/checkstyle.xml")
        ignoreFailures = false
        maxErrors = 0
        maxWarnings = 0
    }

    tasks.withType<ShadowJar> {
        exclude("com/mojang/**")
        exclude("javax/annotation/**")
        exclude("org/checkerframework/**")
        relocate("com.google.gson", "${dependencyDir}.gson")
        relocate("dev.frankheijden.minecraftreflection", "${dependencyDir}.minecraftreflection")
        relocate("cloud.commandframework", "${dependencyDir}.cloud")
        relocate("me.lucko.commodore", "${dependencyDir}.commodore")
        relocate("io.leangen.geantyref", "${dependencyDir}.typetoken")
        if (project.name != "Velocity") {
            relocate("net.kyori", "${dependencyDir}.kyori")
        }
        relocate("net.kyori.adventure.text.minimessage", "${dependencyDir}.adventure.text.minimessage")
        relocate("dev.frankheijden.minecraftreflection", "${dependencyDir}.minecraftreflection")
        relocate("org.zhdev", "${dependencyDir}.zhdev")
    }

    publishing {
        repositories {
            maven {
                name = "zhdev"
                url = uri("https://maven.zhira.net/repository/zhdev/")
                credentials {
                    username = System.getenv("MAVEN_USERNAME")
                    password = System.getenv("MAVEN_PASSWORD")
                }
            }
        }

        publications {
            create<MavenPublication>("Bukman") {
                artifact(tasks["shadowJar"]) {
                    classifier = ""
                }
                artifactId = "Bukman-$artifactId"
            }
        }
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":Common", "shadow"))
    implementation(project(":Bukkit", "shadow"))
    implementation(project(":Bungee", "shadow"))
    implementation(project(":Velocity", "shadow"))
    implementation("net.kyori:adventure-text-serializer-gson:${VersionConstants.adventureVersion}") {
        exclude("net.kyori", "adventure-api")
        exclude("com.google.code.gson", "gson")
    }
}

tasks {
    clean {
        dependsOn("cleanJars")
    }

    build {
        dependsOn("shadowJar", "copyJars")
    }
}

tasks.withType<ShadowJar> {
    relocate("net.kyori.adventure.text.serializer.gson", "${dependencyDir}.impl.adventure.text.serializer.gson")
}

fun outputTasks(): List<Task> {
    return listOf(
        ":Bukkit:shadowJar",
        ":Bungee:shadowJar",
        ":Velocity:shadowJar",
    ).map { tasks.findByPath(it)!! }
}

tasks.register("cleanJars") {
    delete(file("jars"))
}

tasks.register<Copy>("copyJars") {
    outputTasks().forEach {
        from(it) {
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }
    }
    into(file("jars"))
    rename("(.*)-all.jar", "$1.jar")
}

publishing {
    repositories {
        maven {
            name = "zhdev"
            url = uri("https://maven.zhira.net/repository/zhdev/")
            credentials {
                username = System.getenv("MAVEN_USERNAME")
                password = System.getenv("MAVEN_PASSWORD")
            }
        }
    }

    publications {
        create<MavenPublication>("Bukman") {
            artifact(tasks["shadowJar"]) {
                classifier = ""
            }
            artifactId = "Bukman"
        }
    }
}
