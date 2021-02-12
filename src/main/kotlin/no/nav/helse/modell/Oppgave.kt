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

    constructor(id: Long, type: String, status: Oppgavestatus, vedtaksperiodeId: UUID) : this(
        type,
        status,
        vedtaksperiodeId
    ) {
        this.id = id
    }

    internal companion object {
        fun søknad(vedtaksperiodeId: UUID) = oppgave("SØKNAD", vedtaksperiodeId)
        fun stikkprøve(vedtaksperiodeId: UUID) = oppgave("STIKKPRØVE", vedtaksperiodeId)
        fun riskQA(vedtaksperiodeId: UUID) = oppgave("RISK_QA", vedtaksperiodeId)
        private fun oppgave(type: String, vedtaksperiodeId: UUID) =
            Oppgave(type, Oppgavestatus.AvventerSaksbehandler, vedtaksperiodeId)

        internal fun lagMelding(
            oppgaveId: Long,
            oppgaveDao: OppgaveDao
        ): JsonMessage {
            val hendelseId = oppgaveDao.finnHendelseId(oppgaveId)
            val contextId = oppgaveDao.finnContextId(oppgaveId)
            val oppgave = requireNotNull(oppgaveDao.finn(oppgaveId))
            val fødselsnummer = oppgaveDao.finnFødselsnummer(oppgaveId)
            val makstid = oppgaveDao.finnMakstid(oppgaveId) ?: oppgaveDao.opprettMakstid(oppgaveId)

            return lagMelding(
                "oppgave_oppdatert",
                hendelseId,
                contextId,
                oppgaveId,
                oppgave.status,
                fødselsnummer,
                makstid
            )
        }

        internal fun lagMelding(
            eventNavn: String,
            hendelseId: UUID,
            contextId: UUID,
            oppgaveId: Long,
            status: Oppgavestatus,
            fødselsnummer: String,
            makstid: LocalDateTime,
            ferdigstiltAvIdent: String? = null,
            ferdigstiltAvOid: UUID? = null,
        ): JsonMessage {
            return JsonMessage.newMessage(
                mutableMapOf(
                    "@event_name" to eventNavn,
                    "@id" to UUID.randomUUID(),
                    "@opprettet" to LocalDateTime.now(),
                    "hendelseId" to hendelseId,
                    "contextId" to contextId,
                    "oppgaveId" to oppgaveId,
                    "status" to status.name,
                    "fødselsnummer" to fødselsnummer,
                    "makstid" to makstid
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

    internal fun makstidOppnådd() {
        status = Oppgavestatus.MakstidOppnådd
    }

    internal fun lagre(oppgaveMediator: OppgaveMediator, hendelseId: UUID, contextId: UUID) {
        id?.also {
            oppgaveMediator.oppdater(hendelseId, contextId, it, status, ferdigstiltAvIdent, ferdigstiltAvOid)
        } ?: oppgaveMediator.opprett(hendelseId, contextId, vedtaksperiodeId, type).also { id = it } ?: return
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
