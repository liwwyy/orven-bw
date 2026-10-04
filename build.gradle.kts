plugins {
    id("net.fabricmc.fabric-loom-remap") version "1.18.2"
    id("ploceus") version "1.18.1"
}

val modVersion = property("mod_version").toString()
require(modVersion.matches(Regex("\\d+\\.\\d+\\.\\d+"))) { "mod_version must use x.x.x" }
version = "$modVersion+mc${property("minecraft_version")}"
group = "io.github.liwwyy"
base.archivesName = "orven-bw-Ornithe"

ploceus { setIntermediaryGeneration(2) }

repositories {
    mavenCentral()
    google()
    maven("https://repo.polyfrost.org/releases")
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings(ploceus.featherMappings(property("feather_build").toString()))
    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")

    // Published SDK: reproducible on GitHub runners without local beta extraction.
    val oneconfigVersion = property("oneconfig_version").toString()
    for (module in listOf("config-impl", "hud", "ui", "utils", "events", "internal")) {
        compileOnly("org.polyfrost.oneconfig:$module:$oneconfigVersion")
        testImplementation("org.polyfrost.oneconfig:$module:$oneconfigVersion")
    }
    modCompileOnly("org.polyfrost.oneconfig:1.8.9-ornithe:$oneconfigVersion") { isTransitive = false }
    testImplementation("org.polyfrost.oneconfig:1.8.9-ornithe:$oneconfigVersion") { isTransitive = false }
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib:2.4.20")
    testImplementation("org.jetbrains.kotlin:kotlin-stdlib:2.4.20")
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(27)
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    // Compile with the newest JDK; target the Java 25 runtime used by OneClient.
    options.release = 25
    options.encoding = "UTF-8"
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") { expand("version" to project.version) }
}

tasks.test { useJUnitPlatform() }
loom {
    runConfigs.remove(runConfigs.getByName("server"))
}
