package no.nav.helse.oppgave

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.SaksbehandlerTilganger
import no.nav.helse.abonnement.GodkjenningsbehovPayload
import no.nav.helse.abonnement.GodkjenningsbehovPayload.Companion.lagre
import no.nav.helse.abonnement.OpptegnelseDao
import no.nav.helse.oppgave.Oppgave.Companion.loggOppgaverAvbrutt
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.reservasjon.ReservasjonDao
import no.nav.helse.tildeling.TildelingDao
import org.postgresql.util.PSQLException
import org.slf4j.LoggerFactory

class OppgaveMediator(
    private val oppgaveDao: OppgaveDao,
    private val tildelingDao: TildelingDao,
    private val reservasjonDao: ReservasjonDao,
    private val opptegnelseDao: OpptegnelseDao
) {
    private val oppgaver = mutableSetOf<Oppgave>()
    private val oppgaverForPublisering = mutableMapOf<Long, String>()
    private val log = LoggerFactory.getLogger(this::class.java)

    fun hentOppgaver(saksbehandlerTilganger: SaksbehandlerTilganger) = oppgaveDao.finnOppgaver(saksbehandlerTilganger)

    fun opprett(oppgave: Oppgave) {
        nyOppgave(oppgave)
    }

    fun tildel(oppgaveId: Long, saksbehandleroid: UUID, gyldigTil: LocalDateTime? = null): Boolean {
        return tildelingDao.opprettTildeling(oppgaveId, saksbehandleroid, gyldigTil)
    }

    private fun nyOppgave(oppgave: Oppgave) {
        oppgaver.add(oppgave)
    }

    fun ferdigstill(oppgave: Oppgave, saksbehandlerIdent: String, oid: UUID) {
        oppgave.ferdigstill(saksbehandlerIdent, oid)
        nyOppgave(oppgave)
    }

    fun ferdigstill(oppgave: Oppgave) {
        oppgave.ferdigstill()
        nyOppgave(oppgave)
    }

    private fun avbryt(oppgave: Oppgave) {
        oppgave.avbryt()
        nyOppgave(oppgave)
    }

    fun invalider(oppgave: Oppgave) {
        oppgave.avbryt()
        nyOppgave(oppgave)
    }

    private fun avventerSystem(oppgave: Oppgave, saksbehandlerIdent: String, oid: UUID) {
        oppgave.avventerSystem(saksbehandlerIdent, oid)
        nyOppgave(oppgave)
    }

    fun lagreOgTildelOppgaver(
        hendelseId: UUID,
        fødselsnummer: String,
        contextId: UUID,
        messageContext: MessageContext
    ) {
        lagreOppgaver(hendelseId, contextId, messageContext) { tildelOppgaver(fødselsnummer) }
    }

    fun lagreOppgaver(rapidsConnection: RapidsConnection, hendelseId: UUID, contextId: UUID) {
        lagreOppgaver(hendelseId, contextId, rapidsConnection)
    }

    fun avbrytOppgaver(vedtaksperiodeId: UUID) {
        oppgaveDao.finnAktive(vedtaksperiodeId)
            .also { it.loggOppgaverAvbrutt(vedtaksperiodeId) }
            .map(::avbryt)
    }

    fun avventerSystem(oppgaveId: Long, saksbehandlerIdent: String, oid: UUID) {
        val oppgave = oppgaveDao.finn(oppgaveId) ?: return
        avventerSystem(oppgave, saksbehandlerIdent, oid)
    }

    fun opprett(contextId: UUID, vedtaksperiodeId: UUID, utbetalingId: UUID, navn: Oppgavetype, hendelseId: UUID): Long? {
        if (oppgaveDao.harGyldigOppgave(utbetalingId)) return null
        return oppgaveDao.opprettOppgave(contextId, navn, vedtaksperiodeId, utbetalingId).also { oppgaveId ->
            oppgaverForPublisering[oppgaveId] = "oppgave_opprettet"
            GodkjenningsbehovPayload(hendelseId).lagre(opptegnelseDao, oppgaveDao.finnFødselsnummer(oppgaveId))
        }
    }

    fun oppdater(
        oppgaveId: Long,
        status: Oppgavestatus,
        ferdigstiltAvIdent: String?,
        ferdigstiltAvOid: UUID?
    ) {
        oppgaveDao.updateOppgave(oppgaveId, status, ferdigstiltAvIdent, ferdigstiltAvOid)
        oppgaverForPublisering[oppgaveId] = "oppgave_oppdatert"
    }

    fun reserverOppgave(saksbehandleroid: UUID, fødselsnummer: String) {
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

    private fun lagreOppgaver(
        hendelseId: UUID,
        contextId: UUID,
        messageContext: MessageContext,
        doAlso: () -> Unit = {}
    ) {
        if (oppgaver.size > 1) log.info("""
            Oppgaveliste har ${oppgaver.size} oppgaver (hendelsesId: $hendelseId og contextId: $contextId):
            ${oppgaver.joinToString()}
        """.trimIndent())

        oppgaver.forEach { oppgave -> oppgave.lagre(this, contextId, hendelseId) }
        doAlso()
        oppgaver.clear()
        oppgaverForPublisering.onEach { (oppgaveId, eventName) ->
            messageContext.publish(Oppgave.lagMelding(oppgaveId, eventName, oppgaveDao).second.toJson())
        }.clear()
    }

    fun erAktivOppgave(oppgaveId: Long) = oppgaveDao.venterPåSaksbehandler(oppgaveId)

    fun erBeslutteroppgave(oppgaveId: Long) = oppgaveDao.erBeslutteroppgave(oppgaveId)

    fun setBeslutterOppgave(
        oppgaveId: Long,
        erBeslutterOppgave: Boolean,
        erReturOppgave: Boolean,
        tidligereSaksbehandlerOid: UUID
    ) = oppgaveDao.setBeslutterOppgave(oppgaveId, erBeslutterOppgave, erReturOppgave, tidligereSaksbehandlerOid)

    fun finnTidligereSaksbehandler(oppgaveId: Long) = oppgaveDao.finnTidligereSaksbehandler(oppgaveId)
}
