package no.nav.helse.spesialist.api.varsel

import no.nav.helse.spesialist.api.graphql.schema.VarselDTO
import no.nav.helse.spesialist.api.graphql.schema.VarselDTO.VarselvurderingDTO
import no.nav.helse.spesialist.api.varsel.Varsel.Varselstatus.AKTIV
import no.nav.helse.spesialist.api.varsel.Varsel.Varselstatus.AVVIST
import no.nav.helse.spesialist.api.varsel.Varsel.Varselstatus.GODKJENT
import no.nav.helse.spesialist.api.varsel.Varsel.Varselstatus.VURDERT
import java.time.LocalDateTime
import java.util.UUID

data class Varsel(
    private val varselId: UUID,
    private val generasjonId: UUID,
    private val definisjonId: UUID,
    private val opprettet: LocalDateTime,
    private val kode: String,
    private var status: Varselstatus,
    private val tittel: String,
    private val forklaring: String?,
    private val handling: String?,
    private val vurdering: Varselvurdering?,
) {
    companion object {
        fun Set<Varsel>.toDto(): Set<VarselDTO> {
            return map { it.toDto() }.toSet()
        }
    }

    fun toDto() =
        VarselDTO(
            generasjonId,
            definisjonId,
            opprettet,
            kode,
            tittel,
            forklaring,
            handling,
            vurdering?.toDto(status),
        )

    fun erAktiv(): Boolean {
        return status == AKTIV
    }

    fun vurder(
        godkjent: Boolean,
        fødselsnummer: String,
        behandlingId: UUID,
        vedtaksperiodeId: UUID,
        ident: String,
        vurderer: (
            fødselsnummer: String,
            behandlingId: UUID,
            vedtaksperiodeId: UUID,
            varselId: UUID,
            varselTittel: String,
            varselkode: String,
            forrigeStatus: Varselstatus,
            gjeldendeStatus: Varselstatus,
            saksbehandlerIdent: String,
        ) -> Unit,
    ) {
        if (status !in (listOf(AKTIV, VURDERT))) return

        val forrigeStatus = status
        status = if (godkjent) GODKJENT else AVVIST
        vurderer(fødselsnummer, behandlingId, vedtaksperiodeId, varselId, tittel, kode, forrigeStatus, status, ident)
    }

    data class Varselvurdering(
        private val ident: String,
        private val tidsstempel: LocalDateTime,
    ) {
        internal fun toDto(status: Varselstatus) =
            VarselvurderingDTO(
                ident = ident,
                tidsstempel = tidsstempel,
                status = no.nav.helse.spesialist.api.graphql.schema.Varselstatus.valueOf(status.name),
            )
    }

    enum class Varselstatus {
        INAKTIV,
        AKTIV,

        // Varsler er 'VURDERT' når saksbehandler har trykket på avkrysningsboksen i Speil
        VURDERT,

        // Varsler er 'GODKJENT' når behandlingen de tilhører er godkjent/ferdigbehandlet
        GODKJENT,
        AVVIST,
    }
}
