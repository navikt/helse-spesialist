package no.nav.helse.api

import no.nav.helse.Oppgavestatus
import no.nav.helse.mediator.kafka.meldinger.Hendelse
import no.nav.helse.modell.Oppgave
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.command.OppgaveDao
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import java.time.LocalDateTime
import java.util.*

internal class OppgaveMediator(
    private val oppgaveDao: OppgaveDao,
    private val vedtakDao: VedtakDao
) {

    private val oppgaver = mutableListOf<Oppgave>()
    private val meldinger = mutableListOf<String>()

    fun hentOppgaver() = oppgaveDao.finnOppgaver()

    fun hentOppgaveId(fødselsnummer: String) = oppgaveDao.finnOppgaveId(fødselsnummer)

    internal fun oppgave(oppgave: Oppgave) {
        oppgaver.add(oppgave)
    }

    fun lagreOppgaver(hendelse: Hendelse, messageContext: RapidsConnection.MessageContext, contextId: UUID) {
        oppgaver
            .onEach { it.lagre(this, hendelse.id, contextId) }
            .clear()
        meldinger.onEach { messageContext.send(it) }
            .clear()
    }

    internal fun opprett(
        hendelseId: UUID,
        contextId: UUID,
        vedtaksperiodeId: UUID,
        navn: String,
        status: Oppgavestatus
    ) {
        val vedtakRef = requireNotNull(vedtakDao.findVedtak(vedtaksperiodeId)?.id)
        val oppgaveId = oppgaveDao.insertOppgave(
            hendelseId,
            contextId,
            navn,
            status,
            null,
            null,
            vedtakRef
        )
        køMelding("oppgave_opprettet", hendelseId, contextId, vedtaksperiodeId, oppgaveId, status)
    }

    internal fun oppdater(
        hendelseId: UUID,
        contextId: UUID,
        vedtaksperiodeId: UUID,
        oppgaveId: Long,
        status: Oppgavestatus,
        ferdigstiltAvIdent: String?,
        ferdigstiltAvOid: UUID?
    ) {
        oppgaveDao.updateOppgave(oppgaveId, status, ferdigstiltAvIdent, ferdigstiltAvOid)
        køMelding(
            "oppgave_oppdatert",
            hendelseId,
            contextId,
            vedtaksperiodeId,
            oppgaveId,
            status,
            ferdigstiltAvIdent,
            ferdigstiltAvOid
        )
    }

    private fun køMelding(
        eventNavn: String,
        hendelseId: UUID,
        contextId: UUID,
        vedtaksperiodeId: UUID,
        oppgaveId: Long,
        status: Oppgavestatus,
        ferdigstiltAvIdent: String? = null,
        ferdigstiltAvOid: UUID? = null
    ) {
        meldinger.add(JsonMessage.newMessage(
            mutableMapOf(
                "@event_name" to eventNavn,
                "@id" to UUID.randomUUID(),
                "@opprettet" to LocalDateTime.now(),
                "hendelseId" to hendelseId,
                "contextId" to contextId,
                "vedtaksperiodeId" to vedtaksperiodeId,
                "oppgaveId" to oppgaveId,
                "status" to status.name
            ).apply {
                ferdigstiltAvIdent?.also { put("ferdigstiltAvIdent", it) }
                ferdigstiltAvOid?.also { put("ferdigstiltAvOid", it) }
            }
        ).toJson())
    }
}
