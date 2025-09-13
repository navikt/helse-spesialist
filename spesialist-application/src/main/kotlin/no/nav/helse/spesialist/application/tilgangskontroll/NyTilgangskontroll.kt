package no.nav.helse.spesialist.application.tilgangskontroll

import no.nav.helse.db.api.EgenAnsattApiDao
import no.nav.helse.db.api.PersonApiDao
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import no.nav.helse.spesialist.domain.tilgangskontroll.SaksbehandlerTilganger

class NyTilgangskontroll(
    private val egenAnsattApiDao: EgenAnsattApiDao,
    private val personApiDao: PersonApiDao,
) {
    fun harTilgangTilPerson(
        saksbehandlerTilganger: SaksbehandlerTilganger,
        fødselsnummer: String,
    ): Boolean =
        !manglerTilgang(
            egenAnsattApiDao = egenAnsattApiDao,
            personApiDao = personApiDao,
            fødselsnummer = fødselsnummer,
            tilganger = saksbehandlerTilganger,
        )

    private fun manglerTilgang(
        egenAnsattApiDao: EgenAnsattApiDao,
        personApiDao: PersonApiDao,
        fødselsnummer: String,
        tilganger: SaksbehandlerTilganger,
    ): Boolean {
        val kanSeSkjermede = tilganger.harTilgangTilSkjermedePersoner()
        val erSkjermet = egenAnsattApiDao.erEgenAnsatt(fødselsnummer) ?: return true
        if (erSkjermet && !kanSeSkjermede) return true

        val kanSeKode7 = tilganger.harTilgangTilKode7()
        val erFortrolig = personApiDao.personHarAdressebeskyttelse(fødselsnummer, Adressebeskyttelse.Fortrolig)
        val erUgradert = personApiDao.personHarAdressebeskyttelse(fødselsnummer, Adressebeskyttelse.Ugradert)
        return (!kanSeKode7 && erFortrolig) || (!erFortrolig && !erUgradert)
    }
}
