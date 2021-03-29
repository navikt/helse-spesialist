package no.nav.helse.modell

import no.nav.helse.mediator.OppgaveMediator
import no.nav.helse.rapids_rivers.JsonMessage
import java.time.LocalDateTime
import java.util.*

internal class Oppgave private constructor(
    private val type: String,
    private var status: Oppgavestatus,
    private val vedtaksperiodeId: UUID
) {
    private var id: Long? = null
    private var ferdigstiltAvIdent: String? = null
    private var ferdigstiltAvOid: UUID? = null

    constructor(id: Long, type: String, status: Oppgavestatus, vedtaksperiodeId: UUID, ferdigstiltAvIdent: String? = null, ferdigstiltAvOid: UUID? = null) : this(
        type,
        status,
        vedtaksperiodeId,
    ) {
        this.id = id
        this.ferdigstiltAvIdent = ferdigstiltAvIdent
        this.ferdigstiltAvOid = ferdigstiltAvOid
    }

    internal companion object {
        fun søknad(vedtaksperiodeId: UUID) = oppgave("SØKNAD", vedtaksperiodeId)
        fun stikkprøve(vedtaksperiodeId: UUID) = oppgave("STIKKPRØVE", vedtaksperiodeId)
        fun riskQA(vedtaksperiodeId: UUID) = oppgave("RISK_QA", vedtaksperiodeId)
        private fun oppgave(type: String, vedtaksperiodeId: UUID) =
            Oppgave(type, Oppgavestatus.AvventerSaksbehandler, vedtaksperiodeId)

        internal fun lagMelding(
            oppgaveId: Long,
            eventName: String,
            oppgaveDao: OppgaveDao
        ): JsonMessage {
            val hendelseId = oppgaveDao.finnHendelseId(oppgaveId)
            val contextId = oppgaveDao.finnContextId(oppgaveId)
            val oppgave = requireNotNull(oppgaveDao.finn(oppgaveId))
            val fødselsnummer = oppgaveDao.finnFødselsnummer(oppgaveId)

            return lagMelding(
                eventName,
                hendelseId,
                contextId,
                oppgaveId,
                oppgave.status,
                fødselsnummer,
                oppgave.ferdigstiltAvIdent,
                oppgave.ferdigstiltAvOid
            )
        }

        private fun lagMelding(
            eventName: String,
            hendelseId: UUID,
            contextId: UUID,
            oppgaveId: Long,
            status: Oppgavestatus,
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
                    "fødselsnummer" to fødselsnummer
                ).apply {
                    ferdigstiltAvIdent?.also { put("ferdigstiltAvIdent", it) }
                    ferdigstiltAvOid?.also { put("ferdigstiltAvOid", it) }
                }
            )
        }
    }

    internal fun ferdigstill(ident: String, oid: UUID) {
        status = Oppgavestatus.Ferdigstilt
        ferdigstiltAvIdent = ident
        ferdigstiltAvOid = oid
    }

    internal fun avventerSystem(ident: String, oid: UUID) {
        status = Oppgavestatus.AvventerSystem
        ferdigstiltAvIdent = ident
        ferdigstiltAvOid = oid
    }

    internal fun lagre(oppgaveMediator: OppgaveMediator, contextId: UUID) {
        id?.also {
            oppgaveMediator.oppdater(it, status, ferdigstiltAvIdent, ferdigstiltAvOid)
        } ?: oppgaveMediator.opprett(contextId, vedtaksperiodeId, type).also { id = it } ?: return
    }

    internal fun avbryt() {
        status = Oppgavestatus.Invalidert
    }

    internal fun tildel(oppgaveMediator: OppgaveMediator, saksbehandleroid: UUID, gyldigTil: LocalDateTime) {
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
