rootProject.name = "spesialist"
include("spesialist-felles", "spesialist-api", "spesialist-selve", "spesialist-opprydding-dev", "spesialist-modell")

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            version("rapids-and-rivers", "2024022311041708682651.01821651ed22")
            version("logback", "1.5.7")
            version("logstash", "8.0")
            version("jackson", "2.18.1")
            version("tbd-libs", "2024.11.08-08.30-f5ffe5d3")
            version("prometheus", "1.3.2")

            library("rapids-and-rivers", "com.github.navikt", "rapids-and-rivers").versionRef("rapids-and-rivers")

            library("logback", "ch.qos.logback", "logback-classic").versionRef("logback")
            library("logstash", "net.logstash.logback", "logstash-logback-encoder").versionRef("logstash")

            library("jackson-helpers", "com.github.navikt.tbd-libs", "jackson").versionRef("tbd-libs")
            library("jackson-kotlin", "com.fasterxml.jackson.module", "jackson-module-kotlin").versionRef("jackson")
            library("jackson-datatype", "com.fasterxml.jackson.datatype", "jackson-datatype-jsr310").versionRef("jackson")

            library("prometheus-core", "io.prometheus", "prometheus-metrics-core").versionRef("prometheus")

            bundle("logging", listOf("logback", "logstash"))
        }
    }
}
