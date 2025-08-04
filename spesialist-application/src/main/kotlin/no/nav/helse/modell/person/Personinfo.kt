package no.nav.helse.modell.person

import no.nav.helse.db.PersonDao
import no.nav.helse.spesialist.typer.Kjønn
import java.time.LocalDate

class HentPersoninfoløsninger(
    private val løsninger: List<HentPersoninfoløsning>,
) {
    internal fun relevantLøsning(ident: String) = løsninger.find { it.ident == ident }
}

class HentPersoninfoløsning(
    val ident: String,
    private val fornavn: String,
    private val mellomnavn: String?,
    private val etternavn: String,
    private val fødselsdato: LocalDate,
    private val kjønn: Kjønn,
    private val adressebeskyttelse: Adressebeskyttelse,
) {
    internal fun navn() = listOfNotNull(fornavn, mellomnavn, etternavn).joinToString(" ")

    fun oppdater(
        personDao: PersonDao,
        fødselsnummer: String,
    ) = personDao.upsertPersoninfo(
        fødselsnummer = fødselsnummer,
        fornavn = fornavn,
        mellomnavn = mellomnavn,
        etternavn = etternavn,
        fødselsdato = fødselsdato,
        kjønn = kjønn,
        adressebeskyttelse = adressebeskyttelse,
    )
}
