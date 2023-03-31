package no.nav.helse.mediator.builders

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.modell.varsel.Varsel

class VarselBuilder(
    private val generasjonId: UUID
) {
    private lateinit var varselkode: String
    private lateinit var varselId: UUID
    private lateinit var vedtaksperiodeId: UUID
    private lateinit var opprettet: LocalDateTime
    private lateinit var status: Varsel.Status

    internal fun build() = Varsel(varselId, varselkode, opprettet, vedtaksperiodeId, status)

    internal fun generasjonId() = generasjonId

    internal fun varselkode(varselkode: String) {
        this.varselkode = varselkode
    }

    internal fun varselId(varselId: UUID) {
        this.varselId = varselId
    }

    internal fun vedtaksperiodeId(vedtaksperiodeId: UUID) {
        this.vedtaksperiodeId = vedtaksperiodeId
    }

    internal fun opprettet(opprettet: LocalDateTime) {
        this.opprettet = opprettet
    }

    internal fun status(status: Varsel.Status) {
        this.status = status
    }
}