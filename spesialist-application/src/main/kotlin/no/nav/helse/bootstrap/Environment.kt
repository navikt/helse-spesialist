package no.nav.helse.bootstrap

interface Environment : Map<String, String> {
    val erDev: Boolean
    val erProd: Boolean
}
