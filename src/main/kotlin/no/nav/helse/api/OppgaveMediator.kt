package no.nav.helse.api

import no.nav.helse.Oppgavestatus
import no.nav.helse.Oppgavestatus.AvventerSaksbehandler
import no.nav.helse.mediator.kafka.meldinger.Hendelse
import no.nav.helse.modell.Oppgave
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.command.OppgaveDao
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.tildeling.TildelingDao
import java.time.LocalDateTime
import java.util.*

internal class OppgaveMediator(
    private val oppgaveDao: OppgaveDao,
    private val vedtakDao: VedtakDao,
    private val tildelingDao: TildelingDao
) {

    private val oppgaver = mutableMapOf<Oppgave, Pair<UUID, LocalDateTime>?>()
    private val meldinger = mutableListOf<String>()

    internal fun hentOppgaver() = oppgaveDao.finnOppgaver()

    internal fun hentOppgaveId(fødselsnummer: String) = oppgaveDao.finnOppgaveId(fødselsnummer)

    internal fun oppgave(oppgave: Oppgave, reservasjon: Pair<UUID, LocalDateTime>? = null) {
        oppgaver[oppgave] = reservasjon
    }

    internal fun lagreOppgaver(hendelse: Hendelse, messageContext: RapidsConnection.MessageContext, contextId: UUID) {
        oppgaver
            .onEach { (oppgave, reservasjon) ->
                oppgave.lagre(this, hendelse.id, contextId)
                reservasjon?.let {
                    oppgave.tildel(this, reservasjon)
                }
            }.clear()
        meldinger.onEach { messageContext.send(it) }.clear()
    }

    internal fun tildel(oppgaveId: Long, reservasjon: Pair<UUID, LocalDateTime>) {
        tildelingDao.opprettTildeling(oppgaveId, reservasjon.first, reservasjon.second)
    }

    internal fun opprett(
        hendelseId: UUID,
        contextId: UUID,
        vedtaksperiodeId: UUID,
        navn: String
    ): Long {
        val vedtakRef = requireNotNull(vedtakDao.findVedtak(vedtaksperiodeId)?.id)
        return oppgaveDao.opprettOppgave(
            hendelseId,
            contextId,
            navn,
            vedtakRef
        ).also { oppgaveId ->
            køMelding("oppgave_opprettet", hendelseId, contextId, vedtaksperiodeId, oppgaveId, AvventerSaksbehandler)
        }
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
