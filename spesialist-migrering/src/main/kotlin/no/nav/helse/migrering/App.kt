package no.nav.helse.migrering

fun main() {
    val applicationBuilder = ApplicationBuilder(System.getenv())
    applicationBuilder.start()
}