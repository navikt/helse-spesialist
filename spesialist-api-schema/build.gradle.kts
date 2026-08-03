plugins {
    id("no.nav.helse.sas.sas-kotlin")
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(libs.graphqlKotlin.server.ktor)
    implementation(libs.ktor.server.resources)
    implementation(libs.kotlinx.serialization.json)
}
