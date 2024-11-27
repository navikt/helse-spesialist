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
            version("rapids-and-rivers", "2024022311041708682651.01821651ed22")
            version("logback", "1.5.7")
            version("logstash", "8.0")
            version("jackson", "2.18.1")
            version("tbd-libs", "2024.11.08-08.30-f5ffe5d3")
            version("ktor", "2.3.12")
            version("micrometer", "1.12.3")

            library("rapids-and-rivers", "com.github.navikt", "rapids-and-rivers").versionRef("rapids-and-rivers")

            library("logback", "ch.qos.logback", "logback-classic").versionRef("logback")
            library("logstash", "net.logstash.logback", "logstash-logback-encoder").versionRef("logstash")

            library("jackson-helpers", "com.github.navikt.tbd-libs", "jackson").versionRef("tbd-libs")
            library("jackson-kotlin", "com.fasterxml.jackson.module", "jackson-module-kotlin").versionRef("jackson")
            library("jackson-datatype", "com.fasterxml.jackson.datatype", "jackson-datatype-jsr310").versionRef("jackson")

            library("ktor-micrometer", "io.ktor", "ktor-server-metrics-micrometer").versionRef("ktor")
            library("micrometer-prometheus", "io.micrometer", "micrometer-registry-prometheus").versionRef("micrometer")

            bundle("logging", listOf("logback", "logstash"))
        }
    }
}
