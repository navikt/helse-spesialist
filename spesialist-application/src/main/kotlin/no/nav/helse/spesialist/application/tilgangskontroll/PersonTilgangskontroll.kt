package no.nav.helse.spesialist.application.tilgangskontroll

import no.nav.helse.db.EgenAnsattDao
import no.nav.helse.db.PersonDao
import no.nav.helse.db.api.EgenAnsattApiDao
import no.nav.helse.db.api.PersonApiDao
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import no.nav.helse.spesialist.application.logg.sikkerlogg
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe

object PersonTilgangskontroll {
    fun harTilgangTilPerson(
        tilgangsgrupper: Set<Tilgangsgruppe>,
        fødselsnummer: String,
        egenAnsattApiDao: EgenAnsattApiDao,
        personApiDao: PersonApiDao,
    ): Boolean =
        harTilgangTilEgenAnsattStatus(
            erEgenAnsatt = egenAnsattApiDao.erEgenAnsatt(fødselsnummer),
            tilgangsgrupper = tilgangsgrupper,
        ) &&
            harTilgangTilAdressebeskyttelse(
                adressebeskyttelse = personApiDao.hentAdressebeskyttelse(fødselsnummer),
                tilgangsgrupper = tilgangsgrupper,
            )

    fun harTilgangTilPerson(
        tilgangsgrupper: Set<Tilgangsgruppe>,
        fødselsnummer: String,
        egenAnsattDao: EgenAnsattDao,
        personDao: PersonDao,
    ): Boolean =
        harTilgangTilEgenAnsattStatus(
            erEgenAnsatt = egenAnsattDao.erEgenAnsatt(fødselsnummer),
            tilgangsgrupper = tilgangsgrupper,
        ) &&
            harTilgangTilAdressebeskyttelse(
                adressebeskyttelse =
                    personDao.finnAdressebeskyttelse(fødselsnummer).let {
                        when (it) {
                            no.nav.helse.modell.person.Adressebeskyttelse.Ugradert -> Adressebeskyttelse.Ugradert
                            no.nav.helse.modell.person.Adressebeskyttelse.Fortrolig -> Adressebeskyttelse.Fortrolig
                            no.nav.helse.modell.person.Adressebeskyttelse.StrengtFortrolig -> Adressebeskyttelse.StrengtFortrolig
                            no.nav.helse.modell.person.Adressebeskyttelse.StrengtFortroligUtland -> Adressebeskyttelse.StrengtFortroligUtland
                            no.nav.helse.modell.person.Adressebeskyttelse.Ukjent -> Adressebeskyttelse.Ukjent
                            null -> null
                        }
                    },
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
        }.also { if (!it) sikkerlogg.warn("Mangler tilgang til egenansatte") }

    private fun harTilgangTilAdressebeskyttelse(
        adressebeskyttelse: Adressebeskyttelse?,
        tilgangsgrupper: Set<Tilgangsgruppe>,
    ): Boolean =
        when (adressebeskyttelse) {
            Adressebeskyttelse.Ugradert -> true
            Adressebeskyttelse.Fortrolig -> Tilgangsgruppe.KODE_7 in tilgangsgrupper
            else -> false
        }.also { if (!it) sikkerlogg.warn("Mangler tilgang til adressebeskyttelse") }
}
