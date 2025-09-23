package no.nav.helse.modell.person

interface PersonRepository {
    fun brukPersonHvisFinnes(
        fødselsnummer: String,
        personScope: Person.() -> Unit,
    )

    fun finnFødselsnumre(aktørId: String): List<String>
}
