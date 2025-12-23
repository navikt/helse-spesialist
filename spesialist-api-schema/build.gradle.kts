plugins {
    kotlin("plugin.serialization") version "2.3.0"
}

dependencies {
    implementation(libs.graphqlKotlin.server.ktor)
    implementation(libs.ktor.server.resources)
    implementation(libs.kotlinx.serialization.json)
}
