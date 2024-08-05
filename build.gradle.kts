plugins {
    kotlin("jvm") version "2.0.0"
    kotlin("plugin.serialization") version "2.0.0"
    id("io.exoquery.terpal-plugin") version "2.0.0-0.2.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    // required for xtdb snapshot
    maven(url = "https://s01.oss.sonatype.org/content/repositories/snapshots")
    maven(url = "https://repo.clojars.org")
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")

    implementation("org.postgresql:postgresql:42.7.3")
    implementation("com.zaxxer:HikariCP:5.1.0")

    implementation("io.exoquery:terpal-sql-jdbc:2.0.0-0.3.0")

    testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
    testImplementation("io.kotest:kotest-assertions-core:5.9.1")
    testImplementation("io.kotest.extensions:kotest-extensions-testcontainers:2.0.2")
    testImplementation("org.testcontainers:postgresql:1.20.1")

    testImplementation("com.xtdb:xtdb-core:2.0.0-SNAPSHOT")
    testImplementation("com.xtdb:xtdb-api:2.0.0-SNAPSHOT")
    testImplementation("com.xtdb:xtdb-http-client-jvm:2.0.0-SNAPSHOT")
}

tasks {
    test {
        jvmArgs(
            // required to run XTDB in-process with Apache Arrow - https://docs.xtdb.com/drivers/java/getting-started.html#_in_process
            "--add-opens=java.base/java.nio=ALL-UNNAMED",
            "-Dio.netty.tryReflectionSetAccessible=true",
        )
        useJUnitPlatform()
    }
}

kotlin {
    jvmToolchain(21)
}
