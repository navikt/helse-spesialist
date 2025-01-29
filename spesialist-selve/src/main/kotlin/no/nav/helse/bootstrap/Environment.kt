package no.nav.helse.bootstrap

interface Environment : Map<String, String> {
    val erLokal: Boolean
    val erDev: Boolean
    val erProd: Boolean
}
