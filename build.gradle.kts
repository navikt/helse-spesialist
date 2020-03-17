plugins {
    kotlin("jvm") version "1.3.70"
}

group = "no.nav.helse"

val githubUser: String by project
val githubPassword: String by project

repositories {
    jcenter()

    maven {
        url = uri("https://maven.pkg.github.com/navikt/rapids-and-rivers")
        credentials {
            username = githubUser
            password = githubPassword
        }
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("io.ktor:ktor-server-cio:1.3.1")
    implementation("net.logstash.logback:logstash-logback-encoder:6.3")
    implementation("com.zaxxer:HikariCP:3.4.2")
    implementation("no.nav:vault-jdbc:1.3.7")
    implementation("org.flywaydb:flyway-core:6.2.4")
    implementation("com.github.seratch:kotliquery:1.3.1")

    implementation("com.github.navikt:rapids-and-rivers:1.a468ae5")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }


    named<Jar>("jar") {
        archiveBaseName.set("app")

        manifest {
            attributes["Main-Class"] = "no.nav.helse.AppKt"
            attributes["Class-Path"] = configurations.runtimeClasspath.get().joinToString(separator = " ") {
                it.name
            }
        }

        doLast {
            configurations.runtimeClasspath.get().forEach {
                val file = File("$buildDir/libs/${it.name}")
                if (!file.exists())
                    it.copyTo(file)
            }
        }
    }
}
