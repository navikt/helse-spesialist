package no.nav.helse.spesialist.api.graphql.mapping

import no.nav.helse.db.api.NotatApiDao
import no.nav.helse.db.api.VarselDbDto
import no.nav.helse.spesialist.api.graphql.schema.NotatType
import no.nav.helse.spesialist.api.graphql.schema.VarselDTO
import no.nav.helse.spesialist.api.graphql.schema.VarselDTO.VarselvurderingDTO
import no.nav.helse.spesialist.api.graphql.schema.Varselstatus

fun VarselDbDto.toVarselDto(): VarselDTO {
    val varseldefinisjon = this.varseldefinisjon
    checkNotNull(varseldefinisjon)
    return VarselDTO(
        generasjonId = generasjonId,
        definisjonId = varseldefinisjon.definisjonId,
        opprettet = opprettet,
        kode = kode,
        tittel = varseldefinisjon.tittel,
        forklaring = varseldefinisjon.forklaring,
        handling = varseldefinisjon.handling,
        vurdering =
            varselvurdering?.let { varselvurdering ->
                VarselvurderingDTO(
                    ident = varselvurdering.ident,
                    tidsstempel = varselvurdering.tidsstempel,
                    status =
                        Varselstatus.valueOf(
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
        NotatApiDao.NotatType.Retur -> NotatType.Retur
        NotatApiDao.NotatType.Generelt -> NotatType.Generelt
        NotatApiDao.NotatType.PaaVent -> NotatType.PaaVent
        NotatApiDao.NotatType.OpphevStans -> NotatType.OpphevStans
    }

internal fun NotatType.tilDatabasetype() =
    when (this) {
        NotatType.Retur -> NotatApiDao.NotatType.Retur
        NotatType.Generelt -> NotatApiDao.NotatType.Generelt
        NotatType.PaaVent -> NotatApiDao.NotatType.PaaVent
        NotatType.OpphevStans -> NotatApiDao.NotatType.OpphevStans
    }
