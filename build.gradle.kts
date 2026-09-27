import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.HexFormat

plugins {
    idea
    id("java-library")
    id("fabric-loom") version "1.11-SNAPSHOT"
    id("org.jetbrains.kotlin.jvm") version "2.1.20"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.20"
    id("com.google.devtools.ksp") version "2.1.20-2.0.1"
}

version = "${project.property("mod_version")}-mc${project.property("minecraft_version")}"
group = "com.atsuishio.superbwarfare"

base {
    archivesName.set(project.property("mod_id") as String)
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}

kotlin {
    jvmToolchain(21)
}

loom {
    mixin {
        defaultRefmapName.set("mixins.superbwarfare.refmap.json")
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://maven.parchmentmc.org") }
    // Только этот репозиторий отдаёт maven.modrinth: иначе gradle перебирает репозитории
    // по порядку и валит сборку на первом недоступном (тот же parchment).
    exclusiveContent {
        forRepository { maven { url = uri("https://api.modrinth.com/maven") } }
        filter { includeGroup("maven.modrinth") }
    }
    maven { url = uri("https://maven.createmod.net") }
    maven { url = uri("https://raw.githubusercontent.com/Fuzss/modresources/main/maven/") }
    maven { url = uri("https://maven.shedaniel.me/") }
    maven {
        url = uri("https://maven.blamejared.com/")
        content { includeGroup("mezz.jei") }
    }
    maven { url = uri("https://mvn.devos.one/snapshots/") }
    maven { url = uri("https://jitpack.io") }   // Fabric-ASM, транзитивная у Porting Lib
    maven { url = uri("https://maven.wispforest.io") }   // Accessories
    maven {
        name = "GeckoLib"
        url = uri("https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/")
        content {
            includeGroupByRegex("software\\.bernie.*")
            includeGroup("com.eliotlash.mclib")
        }
    }
}

// Входы без Maven-репозитория: качаются один раз в .gradle/pinned-libs и сверяются по sha256
// при каждой конфигурации. Происхождение и лицензии -- в libs/README.md.
fun pinnedJar(url: String, sha256: String): File {
    val jar = rootProject.file(".gradle/pinned-libs/" + url.substringAfterLast('/'))
    fun sha(f: File) = HexFormat.of().formatHex(
        MessageDigest.getInstance("SHA-256").digest(f.readBytes()))
    if (jar.isFile && sha(jar) == sha256) return jar
    jar.parentFile.mkdirs()
    val part = File(jar.path + ".part")
    URI(url).toURL().openStream().use { input -> part.outputStream().use { input.copyTo(it) } }
    val actual = sha(part)
    check(actual == sha256) { "$url: sha256 $actual, ожидался $sha256" }
    Files.move(part.toPath(), jar.toPath(), StandardCopyOption.REPLACE_EXISTING)
    return jar
}

val simpleBedrockModelJar = pinnedJar(
    "https://github.com/netherg-io/simplebedrockmodel-fabric/releases/download/bf4/simplebedrockmodel-fabric-2.5.1+mc1.21.1-bf4.jar",
    "d32a0232a63bbc73561bd2d6aa18837911bcdeffa166d7bb6c4f524d4f5c80c6",
)
val rhinoJar = pinnedJar(
    "https://raw.githubusercontent.com/Mercurows/SuperbWarfare/676af9f44fce5e0c12e5206c663b976898ced1a8/libs/rhino-1.8.1-SNAPSHOT.jar",
    "4aab6124356e91e16263fa065556444b96bf5d9438e1399914cd971ef8b73616",
)

dependencies {
    minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
    // Те же маппинги, что у апстрима на NeoForge: имена классов и методов при переезде не меняются,
    // поэтому миксины и весь ванильный слой переносятся почти дословно.
    mappings(loom.layered {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-${project.property("parchment_minecraft_version")}:${project.property("parchment_mappings_version")}@zip")
    })
    modImplementation("net.fabricmc:fabric-loader:${project.property("loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${project.property("fabric_version")}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${project.property("flk_version")}")

    ksp(project(":ksp"))
    implementation(project(":ksp"))

    // Fabric-порт SimpleBedrockModel 2.5.1 (Sh1roCu, LGPL-3.0), пакеты те же, что у neoforge-версии, включая v2.
    // Тот же релиз bf4 нашего форка, что стоит в паке.
    modImplementation(files(simpleBedrockModelJar))
    // Только для dev-запуска: в проде MAE приезжает вложенным jar внутри SimpleBedrockModel,
    // а loom вложенные jar не разворачивает, и клиент падает на NoClassDefFoundError.
    modRuntimeOnly("com.maydaymemory:mae:1.1.4")

    modImplementation("software.bernie.geckolib:geckolib-fabric-1.21.1:4.7.5")
    modImplementation("me.shedaniel.cloth:cloth-config-fabric:${project.property("cloth_config_version")}")

    // JEI опционален: плагин compat/jei подхватывается через entrypoint jei_mod_plugin.
    // common-api лежит в mojmap-именах, как и наши маппинги, поэтому обычный compileOnly (как у апстрима).
    compileOnly("mezz.jei:jei-1.21.1-common-api:${project.property("jei_version")}")

    // Отдаёт net.neoforged.neoforge.common.ModConfigSpec под Fabric с тем же именем пакета,
    // поэтому весь пакет config едет без правок: ModConfigBuilder там -- typealias на его Builder.
    modImplementation("fuzs.forgeconfigapiport:forgeconfigapiport-fabric:21.1.6")

    // javax.annotation.ParametersAreNonnullByDefault: у NeoForge приходил транзитивно.
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")

    // Скриптовый движок техники: апстрим зовёт shaded-пакет org.mozillaa из этого jar.
    implementation(files(rhinoJar))

    // Curios под 1.21.1 существует только для NeoForge, готового слоя совместимости нет.
    // Accessories -- живой fabric-аналог той же идеи (слоты аксессуаров).
    modImplementation("io.wispforest:accessories-fabric:1.1.0-beta.53+1.21.1")

    // Porting Lib даёт события в форме Forge/NeoForge поверх Fabric: LivingFallEvent,
    // LivingKnockBackEvent, LivingDropsEvent, MobEffectEvent и прочие, которых в Fabric API нет.
    // Иначе под каждое пришлось бы писать свой миксин -- три десятка штук.
    val portingLib = "3.1.0-beta.90+1.21.1"
    for (module in listOf("core", "entity", "level_events", "client_events", "transfer", "items")) {
        modImplementation("io.github.fabricators_of_create.Porting-Lib:$module:$portingLib")
    }

    // Только для compat/tacz: у TaCZ признак хедшота живёт в его событии, а не в типе урона.
    // В рантайме мод не обязателен, класс грузится под isModLoaded("tacz").
    modCompileOnly("maven.modrinth:tacz-refabricated:sqMweCpe") // версия из mods/tacz-refabricated.pw.toml

    compileOnly("com.maydaymemory:mae:1.1.2") {
        exclude("com.google.code.findbugs", "jsr305")
        exclude("it.unimi.dsi", "fastutil")
        exclude("org.joml", "joml")
    }
}

sourceSets.main.get().resources {
    srcDir("src/generated/resources")
}

// Датаген не портируем: его выход (src/generated/resources, 1694 файла) уже лежит в репозитории,
// а ссылок на пакет извне нет. Это 7581 строка и 41 % всех ошибок компиляции.
val portExcludes = listOf("**/datagen/**")

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    exclude(portExcludes)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    exclude(portExcludes)
}

// Значение снимается здесь, а не внутри filesMatching: с configuration cache тело действия
// выполняется без доступа к project и project.version отдаёт "unspecified".
val modVersion = version.toString()

// Rhino шейднут в пакет org.mozillaa и модом не является, поэтому ни include (jar-in-jar,
// нужен fabric.mod.json), ни отдельная запись в паке не годятся -- классы кладутся внутрь jar.
tasks.jar {
    from(zipTree(rhinoJar)) {
        exclude("META-INF/**")
    }
}

tasks.processResources {
    from("COPYING", "COPYING.LESSER")
    // fabric.mod.json держит ${version}; без подстановки загрузчик не может проверять
    // зависимости от мода и ругается на несемвер.
    val v = modVersion
    inputs.property("version", v)
    filesMatching("fabric.mod.json") {
        expand("version" to v)
    }
}

// Быстрая проверка компиляции без упаковки jar.
tasks.register("devBuild") {
    description = "Fast dev build: compile + resources only, no JAR packaging"
    group = "build"
    dependsOn("classes", "processResources")
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}


tasks.register<JavaExec>("killFeedCheck") {
    group = "verification"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath + sourceSets.main.get().compileClasspath
    mainClass.set("com.atsuishio.superbwarfare.KillFeedChecksKt")
}

tasks.named("check") { dependsOn("killFeedCheck") }
