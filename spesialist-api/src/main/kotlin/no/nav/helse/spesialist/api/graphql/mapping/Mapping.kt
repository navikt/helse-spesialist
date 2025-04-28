package no.nav.helse.spesialist.api.graphql.mapping

import no.nav.helse.db.api.NotatApiDao
import no.nav.helse.db.api.VarselDbDto
import no.nav.helse.spesialist.api.graphql.schema.ApiNotatType
import no.nav.helse.spesialist.api.graphql.schema.ApiPeriodehistorikkType
import no.nav.helse.spesialist.api.graphql.schema.ApiReservasjon
import no.nav.helse.spesialist.api.graphql.schema.ApiVarselDTO
import no.nav.helse.spesialist.api.graphql.schema.ApiVarselDTO.ApiVarselvurderingDTO
import no.nav.helse.spesialist.api.graphql.schema.ApiVarselstatus
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkType
import no.nav.helse.spesialist.api.varsel.Varselstatus
import no.nav.helse.spesialist.application.Reservasjonshenter

fun VarselDbDto.toVarselDto(): ApiVarselDTO {
    val varseldefinisjon = this.varseldefinisjon
    checkNotNull(varseldefinisjon)
    return ApiVarselDTO(
        generasjonId = generasjonId,
        definisjonId = varseldefinisjon.definisjonId,
        opprettet = opprettet,
        kode = kode,
        tittel = varseldefinisjon.tittel,
        forklaring = varseldefinisjon.forklaring,
        handling = varseldefinisjon.handling,
        vurdering =
            varselvurdering?.let { varselvurdering ->
                ApiVarselvurderingDTO(
                    ident = varselvurdering.ident,
                    tidsstempel = varselvurdering.tidsstempel,
                    status =
                        ApiVarselstatus.valueOf(
                            status.let { status ->
                                when (status) {
                                    VarselDbDto.Varselstatus.INAKTIV -> Varselstatus.INAKTIV
                                    VarselDbDto.Varselstatus.AKTIV -> Varselstatus.AKTIV
                                    VarselDbDto.Varselstatus.VURDERT -> Varselstatus.VURDERT
                                    VarselDbDto.Varselstatus.GODKJENT -> Varselstatus.GODKJENT
                                    VarselDbDto.Varselstatus.AVVIST -> Varselstatus.AVVIST
                                    VarselDbDto.Varselstatus.AVVIKLET -> error("Varsler med status avviklet skal ikke sendes til speil")
                                }
                            }.name,
                        ),
                )
            },
    )
}

internal fun NotatApiDao.NotatType.tilSkjematype() =
    when (this) {
        NotatApiDao.NotatType.Retur -> ApiNotatType.Retur
        NotatApiDao.NotatType.Generelt -> ApiNotatType.Generelt
        NotatApiDao.NotatType.PaaVent -> ApiNotatType.PaaVent
        NotatApiDao.NotatType.OpphevStans -> ApiNotatType.OpphevStans
    }

internal fun ApiNotatType.tilDatabasetype() =
    when (this) {
        ApiNotatType.Retur -> NotatApiDao.NotatType.Retur
        ApiNotatType.Generelt -> NotatApiDao.NotatType.Generelt
        ApiNotatType.PaaVent -> NotatApiDao.NotatType.PaaVent
        ApiNotatType.OpphevStans -> NotatApiDao.NotatType.OpphevStans
    }

fun PeriodehistorikkType.tilApiPeriodehistorikkType() =
    when (this) {
        PeriodehistorikkType.TOTRINNSVURDERING_TIL_GODKJENNING -> ApiPeriodehistorikkType.TOTRINNSVURDERING_TIL_GODKJENNING
        PeriodehistorikkType.TOTRINNSVURDERING_RETUR -> ApiPeriodehistorikkType.TOTRINNSVURDERING_RETUR
        PeriodehistorikkType.TOTRINNSVURDERING_ATTESTERT -> ApiPeriodehistorikkType.TOTRINNSVURDERING_ATTESTERT
        PeriodehistorikkType.VEDTAKSPERIODE_REBEREGNET -> ApiPeriodehistorikkType.VEDTAKSPERIODE_REBEREGNET
        PeriodehistorikkType.LEGG_PA_VENT -> ApiPeriodehistorikkType.LEGG_PA_VENT
        PeriodehistorikkType.ENDRE_PA_VENT -> ApiPeriodehistorikkType.ENDRE_PA_VENT
        PeriodehistorikkType.FJERN_FRA_PA_VENT -> ApiPeriodehistorikkType.FJERN_FRA_PA_VENT
        PeriodehistorikkType.STANS_AUTOMATISK_BEHANDLING -> ApiPeriodehistorikkType.STANS_AUTOMATISK_BEHANDLING
        PeriodehistorikkType.STANS_AUTOMATISK_BEHANDLING_SAKSBEHANDLER -> ApiPeriodehistorikkType.STANS_AUTOMATISK_BEHANDLING_SAKSBEHANDLER
        PeriodehistorikkType.OPPHEV_STANS_AUTOMATISK_BEHANDLING_SAKSBEHANDLER -> ApiPeriodehistorikkType.OPPHEV_STANS_AUTOMATISK_BEHANDLING_SAKSBEHANDLER
    }

fun Reservasjonshenter.ReservasjonDto.toApiReservasjon(): ApiReservasjon =
    ApiReservasjon(
        kanVarsles = kanVarsles,
        reservert = reservert,
    )
