package no.nav.helse.bootstrap

interface Environment : Map<String, String> {
    val erLokal: Boolean

    fun isTrue(key: String): Boolean = get(key).toBoolean()
}
