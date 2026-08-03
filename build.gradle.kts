plugins {
    alias(libs.plugins.sas.root)
    alias(libs.plugins.sas.module) apply false
    alias(libs.plugins.sas.kotlin) apply false
    alias(libs.plugins.sas.deployable) apply false
}

allprojects {
    group = "no.nav.helse"
}
