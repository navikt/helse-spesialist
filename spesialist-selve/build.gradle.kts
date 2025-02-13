val micrometerRegistryPrometheusVersion = "1.13.6"
val logbackEncoderVersion = "8.0"

plugins {
    kotlin("plugin.serialization") version "2.1.10"
}

dependencies {
    api(project(":spesialist-modell"))

    implementation(libs.bundles.logging)
    implementation(libs.micrometer.prometheus)
}
