plugins {
    id("com.gradleup.shadow") version "8.3.7"
}

dependencies {
    implementation(libs.postgresSocketFactory)
    implementation(libs.bundles.flyway.postgres)
    implementation(libs.postgresJdbcDriver)
    implementation(libs.hikari)
}

tasks.shadowJar {
    manifest {
        attributes["Main-Class"] = "no.nav.helse.spesialist.db.migrations.AppKt"
    }
    mergeServiceFiles()
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}
