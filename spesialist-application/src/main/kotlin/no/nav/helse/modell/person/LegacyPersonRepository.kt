package no.nav.helse.modell.person

interface LegacyPersonRepository {
    fun <T> brukPersonHvisFinnes(
        fødselsnummer: String,
        personScope: LegacyPerson.() -> T,
    ): T?

    fun <T> brukPerson(
        fødselsnummer: String,
        personScope: LegacyPerson.() -> T,
    ): T =
        brukPersonHvisFinnes(fødselsnummer, personScope)
            ?: throw IllegalArgumentException("Person med fødselsnummer $fødselsnummer finnes ikke")

    fun finnFødselsnumre(aktørId: String): List<String>
}
