package no.nav.helse.mediator

import no.nav.helse.mediator.meldinger.Hendelse
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.oppgave.OppgaveDao
import no.nav.helse.modell.oppgave.Oppgavestatus
import no.nav.helse.modell.tildeling.ReservasjonDao
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.tildeling.TildelingDao
import org.postgresql.util.PSQLException
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

internal class OppgaveMediator(
    private val oppgaveDao: OppgaveDao,
    private val vedtakDao: VedtakDao,
    private val tildelingDao: TildelingDao,
    private val reservasjonDao: ReservasjonDao
) {
    private val oppgaver = mutableSetOf<Oppgave>()
    private val oppgaverForPublisering = mutableMapOf<Long, String>()
    private val log = LoggerFactory.getLogger(this::class.java)

    internal fun hentOppgaver(inkluderRiskQaOppgaver: Boolean) = oppgaveDao.finnOppgaver(inkluderRiskQaOppgaver)

    internal fun hentOppgaveId(fødselsnummer: String) = oppgaveDao.finnOppgaveId(fødselsnummer)

    internal fun opprett(oppgave: Oppgave) {
        nyOppgave(oppgave)
    }

    internal fun tildel(oppgaveId: Long, saksbehandleroid: UUID, gyldigTil: LocalDateTime? = null) {
        tildelingDao.opprettTildeling(oppgaveId, saksbehandleroid, gyldigTil)
    }

    private fun nyOppgave(oppgave: Oppgave) {
        oppgaver.add(oppgave)
    }

    internal fun ferdigstill(oppgave: Oppgave, saksbehandlerIdent: String, oid: UUID) {
        oppgave.ferdigstill(saksbehandlerIdent, oid)
        nyOppgave(oppgave)
    }

    internal fun ferdigstill(oppgave: Oppgave) {
        oppgave.ferdigstill()
        nyOppgave(oppgave)
    }

    private fun avbryt(oppgave: Oppgave) {
        oppgave.avbryt()
        nyOppgave(oppgave)
    }

    internal fun invalider(oppgave: Oppgave) {
        oppgave.avbryt()
        nyOppgave(oppgave)
    }

    private fun avventerSystem(oppgave: Oppgave, saksbehandlerIdent: String, oid: UUID) {
        oppgave.avventerSystem(saksbehandlerIdent, oid)
        nyOppgave(oppgave)
    }

    internal fun lagreOgTildelOppgaver(
        hendelse: Hendelse,
        messageContext: MessageContext,
        contextId: UUID
    ) {
        lagreOppgaver(hendelse.id, contextId, messageContext) { tildelOppgaver(hendelse.fødselsnummer()) }
    }

    internal fun lagreOppgaver(rapidsConnection: RapidsConnection, hendelseId: UUID, contextId: UUID) {
        lagreOppgaver(hendelseId, contextId, rapidsConnection)
    }

    internal fun avbrytOppgaver(vedtaksperiodeId: UUID) {
        oppgaveDao.finnAktive(vedtaksperiodeId).forEach(::avbryt)
    }

    internal fun avventerSystem(oppgaveId: Long, saksbehandlerIdent: String, oid: UUID) {
        val oppgave = oppgaveDao.finn(oppgaveId) ?: return
        avventerSystem(oppgave, saksbehandlerIdent, oid)
    }

    internal fun opprett(
        contextId: UUID,
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
        navn: String
    ): Long? {
        if (oppgaveDao.harAktivOppgave(vedtaksperiodeId)) return null
        val vedtakRef = requireNotNull(vedtakDao.findVedtak(vedtaksperiodeId)?.id)
        return oppgaveDao.opprettOppgave(
            contextId,
            navn,
            vedtakRef,
            utbetalingId
        ).also { oppgaveId ->
            oppgaverForPublisering.put(oppgaveId, "oppgave_opprettet")
        }
    }

    internal fun oppdater(
        oppgaveId: Long,
        status: Oppgavestatus,
        ferdigstiltAvIdent: String?,
        ferdigstiltAvOid: UUID?
    ) {
        oppgaveDao.updateOppgave(oppgaveId, status, ferdigstiltAvIdent, ferdigstiltAvOid)
        oppgaverForPublisering.put(oppgaveId, "oppgave_oppdatert")
    }

    internal fun reserverOppgave(saksbehandleroid: UUID, fødselsnummer: String) {
        try {
            reservasjonDao.reserverPerson(saksbehandleroid, fødselsnummer)
        } catch (e: PSQLException) {
            log.warn("Kunne ikke reservere person")
        }
    }

    private fun tildelOppgaver(fødselsnummer: String) {
        reservasjonDao.hentReservasjonFor(fødselsnummer)?.let { (oid, gyldigTil) ->
            oppgaver.forEach { it.tildel(this, oid, gyldigTil) }
        }
    }

    private fun lagreOppgaver(hendelseId: UUID, contextId: UUID, messageContext: MessageContext, doAlso: () -> Unit = {}) {
        if (oppgaver.size > 1) log.info("Oppgaveliste har ${oppgaver.size} oppgaver, hendelsesId: $hendelseId og contextId: $contextId")

        oppgaver.forEach { oppgave -> oppgave.lagre(this, contextId) }
        doAlso()
        oppgaver.clear()
        oppgaverForPublisering.onEach { (oppgaveId, eventName) ->
            messageContext.publish(Oppgave.lagMelding(oppgaveId, eventName, oppgaveDao).toJson())
        }.clear()
    }
}
