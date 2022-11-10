package no.nav.helse.modell.varsel

import java.util.UUID

internal interface VarselRepository {
    fun erAktivFor(vedtaksperiodeId: UUID, varselkode: String): Boolean
    fun nyttVarsel(vedtaksperiodeId: UUID, varselkode: String)
    fun deaktiverFor(vedtaksperiodeId: UUID, varselkode: String)
}