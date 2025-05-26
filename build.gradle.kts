import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage

val kotlinVersion = project.properties["kotlinVersion"]
val micronautVersion = project.properties["micronautVersion"]

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.8.22"
    id("org.jetbrains.kotlin.plugin.allopen") version "1.8.22"
    id("com.google.devtools.ksp") version "1.8.22-1.0.11"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("io.micronaut.application") version "4.0.4"
    id("io.micronaut.aot") version "4.0.4"
    id("com.bmuschko.docker-remote-api") version "9.4.0"
}

// Configure Docker daemon socket for com.bmuschko.docker-remote-api (Rancher Desktop)
docker {
    url.set("unix:///Users/scottdierbeck/.rd/docker.sock")
}

version = "0.1"
group = "com.dierbeck.kms"

repositories {
    mavenCentral()
}

dependencies {
    ksp("io.micronaut:micronaut-http-validation")
    ksp("io.micronaut.serde:micronaut-serde-processor")
    implementation("io.micronaut.kotlin:micronaut-kotlin-runtime")
    implementation("io.micronaut.serde:micronaut-serde-jackson")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.8.22")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.8.22")
    implementation("software.amazon.awssdk:dynamodb:2.20.68")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.7.3")
    implementation("io.micronaut.aws:micronaut-aws-sdk-v2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    
    // For CLI tools
    implementation("info.picocli:picocli:4.7.5")
    ksp("info.picocli:picocli-codegen:4.7.5")
    
    compileOnly("io.micronaut:micronaut-http-client")
    runtimeOnly("ch.qos.logback:logback-classic")
    runtimeOnly("com.fasterxml.jackson.module:jackson-module-kotlin")
    
    // Added snakeyaml dependency
    runtimeOnly("org.yaml:snakeyaml:2.0")
    
// Test dependencies
    testImplementation("io.micronaut:micronaut-http-client")
    testImplementation("io.mockk:mockk:1.13.5")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.micronaut.test:micronaut-test-kotest5")
    testImplementation("io.kotest:kotest-assertions-core:5.6.2")
    testImplementation("io.kotest:kotest-framework-engine:5.6.2")
    testImplementation("io.kotest:kotest-runner-junit5:5.6.2")
    testImplementation("org.testcontainers:testcontainers:1.19.1")
    testImplementation("org.testcontainers:junit-jupiter:1.19.1")
    testImplementation("io.micronaut.test:micronaut-test-kotest5")
    ksp("io.micronaut:micronaut-inject-java")  // This will handle both main and test sources
}

application {
    mainClass.set("com.dierbeck.kms.ApplicationKt")
}

tasks.withType<Test> {
    useJUnitPlatform()
    systemProperty("kotest.framework.extension.registry", "io.micronaut.test.extensions.kotest5.MicronautKotest5Extension")
}

graalvmNative.toolchainDetection.set(false)

micronaut {
    runtime("netty")
    testRuntime("kotest5")
    processing {
        incremental(true)
        annotations("com.dierbeck.kms.*")
    }
    aot {
        // Please review carefully the optimizations enabled below
        // Check https://micronaut-projects.github.io/micronaut-aot/latest/guide/ for more details
        optimizeServiceLoading.set(true)
        convertYamlToJava.set(true)
        precomputeOperations.set(true)
        cacheEnvironment.set(true)
        optimizeClassLoading.set(true)
        deduceEnvironment.set(true)
        optimizeNetty.set(true)
    }
}

// Define a standalone task to build the Docker image
tasks.register<DockerBuildImage>("buildDockerImage") {
    description = "Builds Docker image using the root Dockerfile"
    group = "docker"
    dependsOn("shadowJar")
    // Use project root as Docker context
    inputDir.set(project.projectDir)
    // Specify the Dockerfile at project root
    dockerFile.set(project.projectDir.resolve("Dockerfile"))
    images.add("${project.group}/${project.name}:${project.version}")
}