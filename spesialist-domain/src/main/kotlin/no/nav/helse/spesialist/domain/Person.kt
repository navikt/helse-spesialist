package no.nav.helse.spesialist.domain

import no.nav.helse.spesialist.domain.ddd.AggregateRoot
import no.nav.helse.spesialist.domain.ddd.ValueObject
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
import java.time.Instant
import java.time.LocalDate

data class Personinfo(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val fødselsdato: LocalDate?,
    val kjønn: Kjønn?,
    val adressebeskyttelse: Adressebeskyttelse,
) : ValueObject {
    enum class Kjønn {
        Kvinne,
        Mann,
        Ukjent,
    }

    enum class Adressebeskyttelse {
        Ugradert,
        Fortrolig,
        StrengtFortrolig,
        StrengtFortroligUtland,
        Ukjent,
    }
}

class Person private constructor(
    id: Identitetsnummer,
    val aktørId: String,
    info: Personinfo?,
    infoOppdatert: LocalDate?,
    val enhetRef: Int?,
    val enhetRefOppdatert: LocalDate?,
    val infotrygdutbetalingerRef: Int?,
    val infotrygdutbetalingerOppdatert: LocalDate?,
    egenAnsattStatus: EgenAnsattStatus?,
) : AggregateRoot<Identitetsnummer>(id) {
    var info: Personinfo? = info
        private set
    var infoOppdatert: LocalDate? = infoOppdatert
        private set
    var egenAnsattStatus: EgenAnsattStatus? = egenAnsattStatus
        private set

    fun oppdaterInfo(personinfo: Personinfo) {
        this.info = personinfo
        this.infoOppdatert = LocalDate.now()
    }

    fun kanSeesAvSaksbehandlerMedGrupper(tilgangsgrupper: Set<Tilgangsgruppe>): Boolean =
        girTilgangTilEgenAnsattStatus(tilgangsgrupper) &&
            girTilgangTilAdressebeskyttelse(tilgangsgrupper)

    private fun girTilgangTilEgenAnsattStatus(tilgangsgrupper: Set<Tilgangsgruppe>): Boolean =
        when (egenAnsattStatus?.erEgenAnsatt) {
            true -> Tilgangsgruppe.EGEN_ANSATT in tilgangsgrupper
            false -> true
            null -> false
        }

    private fun girTilgangTilAdressebeskyttelse(tilgangsgrupper: Set<Tilgangsgruppe>): Boolean =
        when (info?.adressebeskyttelse) {
            Personinfo.Adressebeskyttelse.Ugradert -> true
            Personinfo.Adressebeskyttelse.Fortrolig -> Tilgangsgruppe.KODE_7 in tilgangsgrupper
            else -> false
        }

    fun oppdaterEgenAnsattStatus(
        erEgenAnsatt: Boolean,
        oppdatertTidspunkt: Instant,
    ) {
        this.egenAnsattStatus =
            EgenAnsattStatus(
                erEgenAnsatt = erEgenAnsatt,
                oppdatertTidspunkt = oppdatertTidspunkt,
            )
    }

    object Factory {
        fun ny(
            id: Identitetsnummer,
            aktørId: String,
            info: Personinfo?,
            egenAnsattStatus: EgenAnsattStatus?,
        ) = Person(
            id = id,
            aktørId = aktørId,
            info = info,
            infoOppdatert = info?.let { LocalDate.now() },
            enhetRef = null,
            enhetRefOppdatert = null,
            infotrygdutbetalingerRef = null,
            infotrygdutbetalingerOppdatert = null,
            egenAnsattStatus = egenAnsattStatus,
        )

        fun fraLagring(
            id: Identitetsnummer,
            aktørId: String,
            info: Personinfo?,
            infoOppdatert: LocalDate?,
            enhetRef: Int?,
            enhetRefOppdatert: LocalDate?,
            infotrygdutbetalingerRef: Int?,
            infotrygdutbetalingerOppdatert: LocalDate?,
            egenAnsattStatus: EgenAnsattStatus?,
        ) = Person(
            id = id,
            aktørId = aktørId,
            info = info,
            infoOppdatert = infoOppdatert,
            enhetRef = enhetRef,
            enhetRefOppdatert = enhetRefOppdatert,
            infotrygdutbetalingerRef = infotrygdutbetalingerRef,
            infotrygdutbetalingerOppdatert = infotrygdutbetalingerOppdatert,
            egenAnsattStatus = egenAnsattStatus,
        )
    }
}
