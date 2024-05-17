import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
    kotlin("jvm") version "1.9.23"
    id("xyz.jpenilla.run-paper") version "2.2.4"
    id("net.minecrell.plugin-yml.bukkit") version "0.6.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "io.github.grilledcheeselovers"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-releases/")
}

dependencies {
    testImplementation(kotlin("test"))
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    implementation("com.github.stefvanschie.inventoryframework:IF:0.10.13")
    implementation("net.dv8tion:JDA:5.0.0-beta.23") {
        exclude(module = "opus-java")
    }
}

tasks {
    runServer {
        minecraftVersion("1.20.4")
    }

    build {
        dependsOn(shadowJar)
    }

    shadowJar {

        archiveFileName.set("${project.name}-${project.version}.jar")

        relocate("com.github.stefvanschie.inventoryframework", "io.github.grilledcheeselovers.inventoryframework")
        relocate("net.dv8tion.jda", "io.github.grilledcheeselovers.jda")

        dependencies {
            exclude(dependency("org.yaml:snakeyaml"))
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

bukkit {
    // Default values can be overridden if needed
    name = "GrilledCheeseLoversPlugin"
    version = "${getVersion()}"
    description = "GrilledCheeseLovers"
    author = "GrilledCheeseLovers"

    // Plugin main class (required)
    main = "io.github.grilledcheeselovers.GrilledCheeseLoversPlugin"


    apiVersion = "1.17"

    load = BukkitPluginDescription.PluginLoadOrder.POSTWORLD
    prefix = "GrilledCheeseLovers"

    commands {
        register("grilledcheese") {
            description = "Grilled Cheese Command"
            aliases = listOf("gc")
        }
        register("village") {
            description = "Village command"
        }
    }
}