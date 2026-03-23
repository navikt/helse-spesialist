package no.nav.helse.modell.person

interface LegacyPersonRepository {
    fun brukPersonHvisFinnes(
        fødselsnummer: String,
        personScope: LegacyPerson.() -> Unit,
    )

    fun finnFødselsnumre(aktørId: String): List<String>
}
