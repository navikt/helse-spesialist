plugins {
    kotlin("plugin.serialization") version "2.2.20"
}

dependencies {
    implementation(libs.graphqlKotlin.server.ktor)
    implementation(libs.ktor.server.resources)
    implementation(libs.kotlinx.serialization.json)
    implementation("io.github.smiley4:schema-kenerator-core:2.4.0")
}
