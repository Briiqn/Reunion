plugins {
    java
    application
    `maven-publish`
    id("com.gradleup.shadow") version "9.3.2"
    id("com.google.protobuf") version "0.9.4"
}

group = "dev.briiqn"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://repo.viaversion.com")
    maven("https://repo.codemc.io/repository/maven-public/")
}

application {
    mainClass.set("dev.briiqn.reunion.Main")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(22))
    }
}

dependencies {
    compileOnly("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("net.kyori:adventure-nbt:4.26.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("io.netty:netty-all:4.2.12.Final")
    implementation("io.github.classgraph:classgraph:4.8.184")
    implementation("io.netty:netty-transport-native-epoll:4.2.10.Final")
    implementation("org.apache.logging.log4j:log4j-core:2.25.3")
    implementation("org.jline:jline:4.0.0")
    implementation("com.alibaba.fastjson2:fastjson2:2.0.61")
    implementation("org.yaml:snakeyaml:2.6")
    implementation("net.raphimc:MinecraftAuth:5.0.0")
    implementation("com.viaversion:viaversion-common:5.8.1")
    implementation("com.viaversion:viaversion:5.8.1")
    implementation("com.viaversion:viaversion-api:5.8.1")
    implementation("com.viaversion:viabackwards-common:5.8.1")
    implementation("com.viaversion:viabackwards:5.8.1")
    implementation("com.viaversion:viarewind-common:4.1.0-SNAPSHOT")
    implementation("com.viaversion:viarewind:4.1.0-SNAPSHOT")
    implementation("com.google.code.gson:gson:2.13.2")
    implementation("com.google.guava:guava:33.5.0-jre")
    implementation("it.unimi.dsi:fastutil:8.5.18")
    implementation("tools.profiler:async-profiler:4.3")
    implementation("com.google.protobuf:protobuf-java:3.25.3")
    implementation("com.zaxxer:HikariCP:7.0.2")
    implementation("org.xerial:sqlite-jdbc:3.51.3.0")
    implementation("org.jdbi:jdbi3-core:3.52.0")
    implementation("org.jdbi:jdbi3-sqlobject:3.52.0")
}

tasks.named<ProcessResources>("processResources") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.25.3"
    }
}

sourceSets {
    main {
        proto {
            srcDir("src/main/proto")
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

val apiJar by tasks.registering(Jar::class) {
    group = "build"
    archiveBaseName.set("reunion-api")
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("")
    destinationDirectory.set(layout.buildDirectory.dir("libs"))

    val mainOutput = sourceSets.main.get().output

    from(mainOutput) {
        include("dev/briiqn/reunion/api/**")
        include("dev/briiqn/reunion/core/plugin/**")
        include("dev/briiqn/reunion/core/network/packet/**")
        include("dev/briiqn/reunion/core/network/packet/annotation/**")
        include("dev/briiqn/reunion/core/util/math/**")
    }
    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.endsWith(".jar") }
            .map { zipTree(it) }
    }) {
        include("org/yaml/**")
        include("org/apache/logging/log4j/**")
        include("com/alibaba/fastjson2/**")
    }

    manifest {
        attributes(
            "Specification-Title"   to "Reunion Plugin API",
            "Specification-Version" to project.version,
            "Implementation-Title"  to "dev.briiqn.reunion.api",
            "Built-By"              to System.getProperty("user.name"),
            "Build-Jdk"             to System.getProperty("java.version"),
        )
    }

    dependsOn(tasks.compileJava)
}

tasks.register("buildAPI") {
    group = "build"
    dependsOn(apiJar)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = "reunion-api"
            version = project.version.toString()

            artifact(apiJar)
        }
    }
}