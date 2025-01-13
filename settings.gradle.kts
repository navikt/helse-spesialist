rootProject.name = "spesialist"
include(
    "spesialist-felles",
    "spesialist-api",
    "spesialist-selve",
    "spesialist-opprydding-dev",
    "spesialist-modell",
    "spesialist-bootstrap",
)

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            version("rapids-and-rivers", "2025010715371736260653.d465d681c420")
            version("logback", "1.5.7")
            version("logstash", "8.0")
            version("jackson", "2.18.1")
            version("tbd-libs", "2024.11.25-10.59-6f263a10")
            version("ktor", "3.0.1")
            version("micrometer", "1.13.3")
            version("testcontainers", "1.20.4")

            library("rapids-and-rivers", "com.github.navikt", "rapids-and-rivers").versionRef("rapids-and-rivers")
            library("rapids-and-rivers-test", "com.github.navikt.tbd-libs", "rapids-and-rivers-test").versionRef("tbd-libs")

            library("logback", "ch.qos.logback", "logback-classic").versionRef("logback")
            library("logstash", "net.logstash.logback", "logstash-logback-encoder").versionRef("logstash")

            library("jackson-helpers", "com.github.navikt.tbd-libs", "jackson").versionRef("tbd-libs")
            library("jackson-kotlin", "com.fasterxml.jackson.module", "jackson-module-kotlin").versionRef("jackson")
            library("jackson-datatype", "com.fasterxml.jackson.datatype", "jackson-datatype-jsr310").versionRef("jackson")

            library("ktor-micrometer", "io.ktor", "ktor-server-metrics-micrometer").versionRef("ktor")
            library("micrometer-prometheus", "io.micrometer", "micrometer-registry-prometheus").versionRef("micrometer")

            library("ktor-serialization-jackson", "io.ktor", "ktor-serialization-jackson").versionRef("ktor")

            library("ktor-server-double-receive", "io.ktor", "ktor-server-double-receive").versionRef("ktor")
            library("ktor-server-cio", "io.ktor", "ktor-server-cio").versionRef("ktor")
            library("ktor-server-content-negotiation", "io.ktor", "ktor-server-content-negotiation").versionRef("ktor")
            library("ktor-server-status-pages", "io.ktor", "ktor-server-status-pages").versionRef("ktor")
            library("ktor-server-core", "io.ktor", "ktor-server-core").versionRef("ktor")
            library("ktor-server-call-logging", "io.ktor", "ktor-server-call-logging").versionRef("ktor")
            library("ktor-server-call-id", "io.ktor", "ktor-server-call-id").versionRef("ktor")
            library("ktor-server-auth", "io.ktor", "ktor-server-auth").versionRef("ktor")
            library("ktor-server-auth-jwt", "io.ktor", "ktor-server-auth-jwt").versionRef("ktor")
            library("ktor-server-websockets", "io.ktor", "ktor-server-websockets").versionRef("ktor")

            library("ktor-server-test-host", "io.ktor", "ktor-server-test-host").versionRef("ktor")

            library("ktor-client-core", "io.ktor", "ktor-client-core").versionRef("ktor")
            library("ktor-client-apache", "io.ktor", "ktor-client-apache").versionRef("ktor")
            library("ktor-client-content-negotiation", "io.ktor", "ktor-client-content-negotiation").versionRef("ktor")

            library("testcontainers-kafka", "org.testcontainers", "kafka").versionRef("testcontainers")
            library("testcontainers-postgresql", "org.testcontainers", "postgresql").versionRef("testcontainers")

            bundle("logging", listOf("logback", "logstash"))
            bundle(
                "ktor-server",
                listOf(
                    "ktor-server-core",
                    "ktor-server-cio",
                    "ktor-server-double-receive",
                    "ktor-server-content-negotiation",
                    "ktor-serialization-jackson",
                    "ktor-server-status-pages",
                    "ktor-server-call-logging",
                    "ktor-server-call-id",
                    "ktor-server-auth",
                    "ktor-server-auth-jwt",
                    "ktor-server-websockets",
                ),
            )
            bundle(
                "ktor-server-test",
                listOf("ktor-server-test-host"),
            )

            bundle(
                "ktor-client",
                listOf("ktor-client-core", "ktor-client-apache", "ktor-client-content-negotiation", "ktor-serialization-jackson"),
            )
        }
    }
}
