package no.nav.helse.modell.person

import no.nav.helse.spesialist.domain.Personinfo
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

    fun personinfo() =
        Personinfo(
            fornavn = fornavn,
            mellomnavn = mellomnavn,
            etternavn = etternavn,
            fødselsdato = fødselsdato,
            kjønn =
                when (kjønn) {
                    Kjønn.Mann -> Personinfo.Kjønn.Mann
                    Kjønn.Kvinne -> Personinfo.Kjønn.Kvinne
                    Kjønn.Ukjent -> Personinfo.Kjønn.Ukjent
                },
            adressebeskyttelse =
                when (adressebeskyttelse) {
                    Adressebeskyttelse.Ugradert -> Personinfo.Adressebeskyttelse.Ugradert
                    Adressebeskyttelse.Fortrolig -> Personinfo.Adressebeskyttelse.Fortrolig
                    Adressebeskyttelse.StrengtFortrolig -> Personinfo.Adressebeskyttelse.StrengtFortrolig
                    Adressebeskyttelse.StrengtFortroligUtland -> Personinfo.Adressebeskyttelse.StrengtFortroligUtland
                    Adressebeskyttelse.Ukjent -> Personinfo.Adressebeskyttelse.Ukjent
                },
        )
}
