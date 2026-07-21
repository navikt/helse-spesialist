plugins {
    id("sas-deployable")
}

sasDeployable {
    mainClass = "no.nav.helse.spesialist.db.migrations.AppKt"
    imageName = "${rootProject.name}-db-migrations"
}

dependencies {
    implementation(libs.postgresSocketFactory)
    implementation(libs.bundles.flyway.postgres)
    implementation(libs.postgresJdbcDriver)
    implementation(libs.hikari)
    implementation(libs.bundles.logback)
}
