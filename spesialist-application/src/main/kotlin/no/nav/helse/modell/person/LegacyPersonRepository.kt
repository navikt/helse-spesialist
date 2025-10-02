package no.nav.helse.modell.person

interface LegacyPersonRepository {
    fun brukPersonHvisFinnes(
        fødselsnummer: String,
        personScope: Person.() -> Unit,
    )

    fun finnFødselsnumre(aktørId: String): List<String>
}
