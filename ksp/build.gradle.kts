plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.20"
    id("com.google.devtools.ksp") version "2.1.20-2.0.1"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.devtools.ksp:symbol-processing-api:2.1.20-2.0.1")
}
tasks.register<org.gradle.api.tasks.compile.JavaCompile>("lintJava") {
    dependsOn("testClasses")
    source(sourceSets["main"].allJava, sourceSets["test"].allJava)
    classpath = sourceSets["main"].compileClasspath + sourceSets["test"].compileClasspath + sourceSets["main"].output + sourceSets["test"].output
    destinationDirectory = layout.buildDirectory.dir("classes/java/lint")
    options.annotationProcessorPath = files()
    options.compilerArgs.addAll(listOf("-proc:none", "-Xlint:divzero,empty,fallthrough,finally,-removal", "-Werror"))
}
tasks.named("check") { dependsOn("lintJava") }
