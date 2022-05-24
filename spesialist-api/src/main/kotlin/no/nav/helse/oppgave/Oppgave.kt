package no.nav.helse.oppgave

import java.util.Objects
import java.util.UUID
import no.nav.helse.rapids_rivers.JsonMessage
import org.slf4j.LoggerFactory

class Oppgave private constructor(
    private val type: Oppgavetype,
    private var status: Oppgavestatus,
    private val vedtaksperiodeId: UUID,
    private val utbetalingId: UUID?
) {
    private var id: Long? = null
    private var ferdigstiltAvIdent: String? = null
    private var ferdigstiltAvOid: UUID? = null

    constructor(id: Long, type: Oppgavetype, status: Oppgavestatus, vedtaksperiodeId: UUID, ferdigstiltAvIdent: String? = null, ferdigstiltAvOid: UUID? = null, utbetalingId: UUID?) : this(
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
        private val log = LoggerFactory.getLogger(this::class.java)

        fun søknad(vedtaksperiodeId: UUID, utbetalingId: UUID) = oppgave(Oppgavetype.SØKNAD, vedtaksperiodeId, utbetalingId)
        fun stikkprøve(vedtaksperiodeId: UUID, utbetalingId: UUID) = oppgave(Oppgavetype.STIKKPRØVE, vedtaksperiodeId, utbetalingId)
        fun revurdering(vedtaksperiodeId: UUID, utbetalingId: UUID) = oppgave(Oppgavetype.REVURDERING, vedtaksperiodeId, utbetalingId)
        fun riskQA(vedtaksperiodeId: UUID, utbetalingId: UUID) = oppgave(Oppgavetype.RISK_QA, vedtaksperiodeId, utbetalingId)
        fun fortroligAdressebeskyttelse(vedtaksperiodeId: UUID, utbetalingId: UUID) = oppgave(Oppgavetype.FORTROLIG_ADRESSE, vedtaksperiodeId, utbetalingId)
        fun utbetalingTilSykmeldt(vedtaksperiodeId: UUID, utbetalingId: UUID) = oppgave(Oppgavetype.UTBETALING_TIL_SYKMELDT, vedtaksperiodeId, utbetalingId)
        fun delvisRefusjon(vedtaksperiodeId: UUID, utbetalingId: UUID) = oppgave(Oppgavetype.DELVIS_REFUSJON, vedtaksperiodeId, utbetalingId)

        private fun oppgave(type: Oppgavetype, vedtaksperiodeId: UUID, utbetalingId: UUID) =
            Oppgave(type, Oppgavestatus.AvventerSaksbehandler, vedtaksperiodeId, utbetalingId)

        fun List<Oppgave>.loggOppgaverAvbrutt(vedtaksperiodeId: UUID) {
            if (isNotEmpty()) {
                val oppgaveIds = map(Oppgave::id).joinToString()
                log.info("Har avbrutt oppgave(r) $oppgaveIds for vedtaksperiode $vedtaksperiodeId")
            }
        }

        fun lagMelding(
            oppgaveId: Long,
            eventName: String,
            oppgaveDao: OppgaveDao
        ): Pair<String, JsonMessage> {
            val hendelseId = oppgaveDao.finnHendelseId(oppgaveId)
            val contextId = oppgaveDao.finnContextId(oppgaveId)
            val oppgave = requireNotNull(oppgaveDao.finn(oppgaveId))
            val fødselsnummer = oppgaveDao.finnFødselsnummer(oppgaveId)

            return fødselsnummer to lagMelding(
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
            type: Oppgavetype,
            fødselsnummer: String,
            ferdigstiltAvIdent: String? = null,
            ferdigstiltAvOid: UUID? = null,
        ): JsonMessage {
            return JsonMessage.newMessage(eventName, mutableMapOf(
                "@forårsaket_av" to mapOf(
                    "id" to hendelseId
                ),
                "hendelseId" to hendelseId,
                "contextId" to contextId,
                "oppgaveId" to oppgaveId,
                "status" to status.name,
                "type" to type.name,
                "fødselsnummer" to fødselsnummer
            ).apply {
                ferdigstiltAvIdent?.also { put("ferdigstiltAvIdent", it) }
                ferdigstiltAvOid?.also { put("ferdigstiltAvOid", it) }
            })
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

    fun lagre(oppgaveMediator: OppgaveMediator, contextId: UUID, hendelseId: UUID) {
        id?.also {
            oppgaveMediator.oppdater(it, status, ferdigstiltAvIdent, ferdigstiltAvOid)
        } ?: oppgaveMediator.opprett(contextId, vedtaksperiodeId, utbetalingId!!, type, hendelseId).also { id = it } ?: return
    }

    fun avbryt() {
        status = Oppgavestatus.Invalidert
    }

    fun tildel(oppgaveMediator: OppgaveMediator, saksbehandleroid: UUID) {
        oppgaveMediator.tildel(requireNotNull(id), saksbehandleroid)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Oppgave) return false
        if (this.id != other.id) return false
        return this.type == other.type && this.vedtaksperiodeId == other.vedtaksperiodeId
    }

    override fun hashCode(): Int {
        return Objects.hash(id, type, vedtaksperiodeId)
    }

    override fun toString(): String {
        return "Oppgave(type=$type, status=$status, vedtaksperiodeId=$vedtaksperiodeId, utbetalingId=$utbetalingId, id=$id)"
    }
}

enum class Oppgavetype {
    SØKNAD, STIKKPRØVE, RISK_QA, REVURDERING, FORTROLIG_ADRESSE, UTBETALING_TIL_SYKMELDT, DELVIS_REFUSJON
}
