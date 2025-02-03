package no.nav.helse.spesialist.api.graphql.mapping

import no.nav.helse.db.api.NotatApiDao
import no.nav.helse.db.api.VarselDbDto
import no.nav.helse.spesialist.api.graphql.schema.ApiNotatType
import no.nav.helse.spesialist.api.graphql.schema.ApiVarselDTO
import no.nav.helse.spesialist.api.graphql.schema.ApiVarselDTO.ApiVarselvurderingDTO
import no.nav.helse.spesialist.api.graphql.schema.ApiVarselstatus

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
                                    VarselDbDto.Varselstatus.INAKTIV -> no.nav.helse.spesialist.api.varsel.Varselstatus.INAKTIV
                                    VarselDbDto.Varselstatus.AKTIV -> no.nav.helse.spesialist.api.varsel.Varselstatus.AKTIV
                                    VarselDbDto.Varselstatus.VURDERT -> no.nav.helse.spesialist.api.varsel.Varselstatus.VURDERT
                                    VarselDbDto.Varselstatus.GODKJENT -> no.nav.helse.spesialist.api.varsel.Varselstatus.GODKJENT
                                    VarselDbDto.Varselstatus.AVVIST -> no.nav.helse.spesialist.api.varsel.Varselstatus.AVVIST
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
