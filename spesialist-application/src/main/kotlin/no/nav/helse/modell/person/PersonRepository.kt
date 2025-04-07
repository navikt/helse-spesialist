package no.nav.helse.modell.person

interface PersonRepository {
    fun brukPersonHvisFinnes(
        fødselsnummer: String,
        personScope: Person.(() -> Unit) -> Unit,
    )

    fun lagrePerson(
        dtoFør: PersonDto,
        dtoEtter: PersonDto,
    )
}
