import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    idea
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
}

group = "ru.pogosian.kazik"
version = "1.0-SNAPSHOT"

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

sourceSets {
    create("integrationTest") {
        compileClasspath += sourceSets.main.get().output + configurations.testRuntimeClasspath.get()
        runtimeClasspath += output + compileClasspath
    }
}

val integrationTestImplementation: Configuration by configurations.getting {
    extendsFrom(configurations.testImplementation.get())
}

configurations["integrationTestRuntimeOnly"].extendsFrom(configurations.testRuntimeOnly.get())

dependencies {
    implementation(platform(libs.spring.boot.bom))
    implementation(libs.spring.boot.webmvc)
    implementation(libs.spring.boot.jdbc)
    implementation(libs.spring.boot.security)
    implementation(libs.spring.boot.oauth2.resource.server)
    implementation(libs.spring.boot.liquibase)
    implementation(libs.spring.boot.restclient)
    implementation(libs.spring.boot.actuator)
    implementation(libs.springdoc.webmvc.ui)
    implementation(libs.jackson.kotlin)
    implementation(libs.kotlin.reflect)
    runtimeOnly(libs.postgresql)

    testImplementation(platform(libs.spring.boot.bom))
    testImplementation(libs.spring.boot.test)
    testImplementation(libs.spring.boot.webmvc.test)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.spring.security.test)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.wiremock)
    testRuntimeOnly(libs.junit.launcher)
}

springBoot {
    mainClass.set("ru.pogosian.kazik.KazikApplicationKt")
}

tasks.register<Test>("integrationTest") {
    description = "Runs integration tests (Testcontainers)."
    group = "verification"
    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath
    shouldRunAfter(tasks.test)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

idea {
    module {
        testSources.from(sourceSets["integrationTest"].kotlin.srcDirs)
        testResources.from(sourceSets["integrationTest"].resources.srcDirs)
    }
}

tasks.named<Jar>("jar") {
    enabled = false
}
