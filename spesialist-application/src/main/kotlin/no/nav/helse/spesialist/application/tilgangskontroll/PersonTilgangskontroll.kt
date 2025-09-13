package no.nav.helse.spesialist.application.tilgangskontroll

import no.nav.helse.db.api.EgenAnsattApiDao
import no.nav.helse.db.api.PersonApiDao
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe

class PersonTilgangskontroll(
    private val egenAnsattApiDao: EgenAnsattApiDao,
    private val personApiDao: PersonApiDao,
) {
    fun harTilgangTilPerson(
        tilgangsgrupper: Set<Tilgangsgruppe>,
        fødselsnummer: String,
    ): Boolean =
        harTilgangTilEgenAnsattStatus(
            erEgenAnsatt = egenAnsattApiDao.erEgenAnsatt(fødselsnummer),
            tilgangsgrupper = tilgangsgrupper,
        ) &&
            harTilgangTilAdressebeskyttelse(
                adressebeskyttelse = personApiDao.hentAdressebeskyttelse(fødselsnummer),
                tilgangsgrupper = tilgangsgrupper,
            )

    private fun harTilgangTilEgenAnsattStatus(
        erEgenAnsatt: Boolean?,
        tilgangsgrupper: Set<Tilgangsgruppe>,
    ): Boolean =
        when (erEgenAnsatt) {
            true -> Tilgangsgruppe.EGEN_ANSATT in tilgangsgrupper
            false -> true
            null -> false
        }

    private fun harTilgangTilAdressebeskyttelse(
        adressebeskyttelse: Adressebeskyttelse?,
        tilgangsgrupper: Set<Tilgangsgruppe>,
    ): Boolean =
        when (adressebeskyttelse) {
            Adressebeskyttelse.Ugradert -> true
            Adressebeskyttelse.Fortrolig -> Tilgangsgruppe.KODE_7 in tilgangsgrupper
            else -> false
        }
}
