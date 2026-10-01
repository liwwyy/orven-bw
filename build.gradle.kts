plugins {
    id("net.fabricmc.fabric-loom-remap") version "1.18.2"
    id("ploceus") version "1.18.1"
}

version = "${property("mod_version")}+mc${property("minecraft_version")}"
group = "io.github.liwwyy"
base.archivesName = "orven-bw"

ploceus { setIntermediaryGeneration(2) }

repositories {
    mavenCentral()
    maven("https://repo.polyfrost.org/releases")
}

val prepareOneConfig = tasks.register<Exec>("prepareOneConfig") {
    val betaJar = providers.gradleProperty("oneconfigJar").orElse(
        "/mnt/nvme/PrismLauncher/instances/OneClient Beta final/minecraft/mods/OneConfig-1.8.9-ornithe-${project.property("oneconfig_version")}.jar"
    )
    inputs.file(betaJar)
    inputs.file("scripts/prepare-oneconfig.py")
    outputs.dir(layout.projectDirectory.dir(".reference/oneconfig-beta"))
    commandLine("python3", "scripts/prepare-oneconfig.py", betaJar.get())
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings(ploceus.featherMappings(property("feather_build").toString()))
    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")

    // Beta 1.2.9 is not published on Maven. Compile against the exact installed APIs.
    // OneClient supplies these at runtime; no OneConfig code is bundled in this mod.
    compileOnly(files(fileTree(".reference/oneconfig-beta") { include("*.jar") }).builtBy(prepareOneConfig))
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib:2.4.20")
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(27)
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    dependsOn(prepareOneConfig)
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
