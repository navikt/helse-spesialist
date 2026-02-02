package no.nav.helse.spesialist.domain

import no.nav.helse.spesialist.domain.ddd.AggregateRoot
import no.nav.helse.spesialist.domain.ddd.ValueObject
import no.nav.helse.spesialist.domain.tilgangskontroll.Brukerrolle
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
    enhetRef: Int?,
    enhetRefOppdatert: LocalDate?,
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

    var enhetRef: Int? = enhetRef
        private set
    var enhetRefOppdatert: LocalDate? = enhetRefOppdatert
        private set

    fun oppdaterInfo(personinfo: Personinfo) {
        this.info = personinfo
        this.infoOppdatert = LocalDate.now()
    }

    fun oppdaterEnhet(enhet: Int) {
        this.enhetRef = enhet
        this.enhetRefOppdatert = LocalDate.now()
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

    fun kanSeesAvSaksbehandlerMedGrupper(brukerroller: Set<Brukerrolle>): Boolean =
        girTilgangTilEgenAnsattStatus(brukerroller) &&
            girTilgangTilAdressebeskyttelse(brukerroller)

    private fun girTilgangTilEgenAnsattStatus(brukerroller: Set<Brukerrolle>): Boolean =
        when (egenAnsattStatus?.erEgenAnsatt) {
            true -> Brukerrolle.EGEN_ANSATT in brukerroller
            false -> true
            null -> false
        }

    private fun girTilgangTilAdressebeskyttelse(brukerroller: Set<Brukerrolle>): Boolean =
        when (info?.adressebeskyttelse) {
            Personinfo.Adressebeskyttelse.Ugradert -> true
            Personinfo.Adressebeskyttelse.Fortrolig -> Brukerrolle.KODE_7 in brukerroller
            else -> false
        }

    fun harDataNødvendigForVisning() = info != null && egenAnsattStatus != null && enhetRef != null

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
