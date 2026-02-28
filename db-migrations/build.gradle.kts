plugins {
    id("application")
}

application {
    mainClass.set("no.nav.helse.spesialist.db.migrations.AppKt")
    applicationName = "app"
}

dependencies {
    implementation(libs.postgresSocketFactory)
    implementation(libs.bundles.flyway.postgres)
    implementation(libs.postgresJdbcDriver)
    implementation(libs.hikari)
    implementation(libs.bundles.logback)
}
