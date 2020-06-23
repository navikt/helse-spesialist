package no.nav.helse.modell.command

class Løsninger {
    private val løsninger = mutableListOf<Any>()

    internal fun add(løsning: Any) {
        løsninger.add(løsning)
    }

    internal inline fun <reified T> løsning(): T = løsninger.filterIsInstance<T>().first()
}
