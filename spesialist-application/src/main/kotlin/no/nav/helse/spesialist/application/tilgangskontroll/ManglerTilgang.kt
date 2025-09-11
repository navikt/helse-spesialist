package no.nav.helse.spesialist.application.tilgangskontroll

import no.nav.helse.db.api.EgenAnsattApiDao
import no.nav.helse.db.api.PersonApiDao
import no.nav.helse.spesialist.api.person.Adressebeskyttelse

fun manglerTilgang(
    egenAnsattApiDao: EgenAnsattApiDao,
    personApiDao: PersonApiDao,
    fnr: String,
    tilganger: SaksbehandlerTilganger,
): Boolean {
    val kanSeSkjermede = tilganger.harTilgangTilSkjermedePersoner()
    val erSkjermet = egenAnsattApiDao.erEgenAnsatt(fnr) ?: return true
    if (erSkjermet && !kanSeSkjermede) return true

    val kanSeKode7 = tilganger.harTilgangTilKode7()
    val erFortrolig = personApiDao.personHarAdressebeskyttelse(fnr, Adressebeskyttelse.Fortrolig)
    val erUgradert = personApiDao.personHarAdressebeskyttelse(fnr, Adressebeskyttelse.Ugradert)
    return (!kanSeKode7 && erFortrolig) || (!erFortrolig && !erUgradert)
}
