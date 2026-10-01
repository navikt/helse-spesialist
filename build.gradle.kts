plugins {
    alias(libs.plugins.sykepenger.root)
    alias(libs.plugins.sykepenger.module) apply false
    alias(libs.plugins.sykepenger.kotlin) apply false
    alias(libs.plugins.sykepenger.deployable) apply false
}

allprojects {
    group = "no.nav.helse"
}
