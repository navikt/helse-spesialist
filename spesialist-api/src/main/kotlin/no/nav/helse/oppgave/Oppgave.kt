package no.nav.helse.oppgave

import no.nav.helse.rapids_rivers.JsonMessage
import java.time.LocalDateTime
import java.util.*

class Oppgave private constructor(
    private val type: String,
    private var status: Oppgavestatus,
    private val vedtaksperiodeId: UUID,
    private val utbetalingId: UUID?
) {
    private var id: Long? = null
    private var ferdigstiltAvIdent: String? = null
    private var ferdigstiltAvOid: UUID? = null

    constructor(id: Long, type: String, status: Oppgavestatus, vedtaksperiodeId: UUID, ferdigstiltAvIdent: String? = null, ferdigstiltAvOid: UUID? = null, utbetalingId: UUID?) : this(
        type,
        status,
        vedtaksperiodeId,
        utbetalingId
    ) {
        this.id = id
        this.ferdigstiltAvIdent = ferdigstiltAvIdent
        this.ferdigstiltAvOid = ferdigstiltAvOid
    }

    companion object {
        fun søknad(vedtaksperiodeId: UUID, utbetalingId: UUID) = oppgave("SØKNAD", vedtaksperiodeId, utbetalingId)
        fun stikkprøve(vedtaksperiodeId: UUID, utbetalingId: UUID) = oppgave("STIKKPRØVE", vedtaksperiodeId, utbetalingId)
        fun revurdering(vedtaksperiodeId: UUID, utbetalingId: UUID) = oppgave("REVURDERING", vedtaksperiodeId, utbetalingId)
        fun riskQA(vedtaksperiodeId: UUID, utbetalingId: UUID) = oppgave("RISK_QA", vedtaksperiodeId, utbetalingId)
        private fun oppgave(type: String, vedtaksperiodeId: UUID, utbetalingId: UUID) =
            Oppgave(type, Oppgavestatus.AvventerSaksbehandler, vedtaksperiodeId, utbetalingId)

        fun lagMelding(
            oppgaveId: Long,
            eventName: String,
            oppgaveDao: OppgaveDao
        ): JsonMessage {
            val hendelseId = oppgaveDao.finnHendelseId(oppgaveId)
            val contextId = oppgaveDao.finnContextId(oppgaveId)
            val oppgave = requireNotNull(oppgaveDao.finn(oppgaveId))
            val fødselsnummer = oppgaveDao.finnFødselsnummer(oppgaveId)

            return lagMelding(
                eventName = eventName,
                hendelseId = hendelseId,
                contextId = contextId,
                oppgaveId = oppgaveId,
                status = oppgave.status,
                type = oppgave.type,
                fødselsnummer = fødselsnummer,
                ferdigstiltAvIdent = oppgave.ferdigstiltAvIdent,
                ferdigstiltAvOid = oppgave.ferdigstiltAvOid
            )
        }

        private fun lagMelding(
            eventName: String,
            hendelseId: UUID,
            contextId: UUID,
            oppgaveId: Long,
            status: Oppgavestatus,
            type: String,
            fødselsnummer: String,
            ferdigstiltAvIdent: String? = null,
            ferdigstiltAvOid: UUID? = null,
        ): JsonMessage {
            return JsonMessage.newMessage(
                mutableMapOf(
                    "@event_name" to eventName,
                    "@id" to UUID.randomUUID(),
                    "@opprettet" to LocalDateTime.now(),
                    "hendelseId" to hendelseId,
                    "contextId" to contextId,
                    "oppgaveId" to oppgaveId,
                    "status" to status.name,
                    "type" to type,
                    "fødselsnummer" to fødselsnummer
                ).apply {
                    ferdigstiltAvIdent?.also { put("ferdigstiltAvIdent", it) }
                    ferdigstiltAvOid?.also { put("ferdigstiltAvOid", it) }
                }
            )
        }
    }

    fun ferdigstill(ident: String, oid: UUID) {
        status = Oppgavestatus.Ferdigstilt
        ferdigstiltAvIdent = ident
        ferdigstiltAvOid = oid
    }

    fun ferdigstill() {
        status = Oppgavestatus.Ferdigstilt
    }

    fun avventerSystem(ident: String, oid: UUID) {
        status = Oppgavestatus.AvventerSystem
        ferdigstiltAvIdent = ident
        ferdigstiltAvOid = oid
    }

    fun lagre(oppgaveMediator: OppgaveMediator, contextId: UUID) {
        id?.also {
            oppgaveMediator.oppdater(it, status, ferdigstiltAvIdent, ferdigstiltAvOid)
        } ?: oppgaveMediator.opprett(contextId, vedtaksperiodeId, utbetalingId!!, type).also { id = it } ?: return
    }

    fun avbryt() {
        status = Oppgavestatus.Invalidert
    }

    fun tildel(oppgaveMediator: OppgaveMediator, saksbehandleroid: UUID, gyldigTil: LocalDateTime) {
        oppgaveMediator.tildel(requireNotNull(id), saksbehandleroid, gyldigTil)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Oppgave) return false
        if (this.id != other.id) return false
        return this.type == other.type && this.vedtaksperiodeId == other.vedtaksperiodeId
    }

    override fun hashCode(): Int {
        return Objects.hash(id, type, vedtaksperiodeId)
    }
}
