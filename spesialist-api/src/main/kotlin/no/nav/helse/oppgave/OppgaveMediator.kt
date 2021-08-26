package no.nav.helse.oppgave

import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.reservasjon.ReservasjonDao
import no.nav.helse.tildeling.TildelingDao
import org.postgresql.util.PSQLException
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

class OppgaveMediator(
    private val oppgaveDao: OppgaveDao,
    private val tildelingDao: TildelingDao,
    private val reservasjonDao: ReservasjonDao
) {
    private val oppgaver = mutableSetOf<Oppgave>()
    private val oppgaverForPublisering = mutableMapOf<Long, String>()
    private val log = LoggerFactory.getLogger(this::class.java)

    fun hentOppgaver(inkluderRiskQaOppgaver: Boolean) = oppgaveDao.finnOppgaver(inkluderRiskQaOppgaver)

    fun hentOppgaveId(fødselsnummer: String) = oppgaveDao.finnOppgaveId(fødselsnummer)

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
        oppgaveDao.finnAktive(vedtaksperiodeId).onEach(::avbryt).also { oppgaver ->
            log.info("Har avbrutt oppgave(r) ${oppgaver.joinToString()} for vedtaksperiode $vedtaksperiodeId")
        }
    }

    fun avventerSystem(oppgaveId: Long, saksbehandlerIdent: String, oid: UUID) {
        val oppgave = oppgaveDao.finn(oppgaveId) ?: return
        avventerSystem(oppgave, saksbehandlerIdent, oid)
    }

    fun opprett(contextId: UUID, vedtaksperiodeId: UUID, utbetalingId: UUID, navn: String): Long? {
        if (oppgaveDao.harGyldigOppgave(utbetalingId)) return null
        return oppgaveDao.opprettOppgave(contextId, navn, vedtaksperiodeId, utbetalingId)
            .also { oppgaveId -> oppgaverForPublisering[oppgaveId] = "oppgave_opprettet" }
    }

    fun oppdater(
        oppgaveId: Long,
        status: Oppgavestatus,
        ferdigstiltAvIdent: String?,
        ferdigstiltAvOid: UUID?
    ) {
        oppgaveDao.updateOppgave(oppgaveId, status, ferdigstiltAvIdent, ferdigstiltAvOid)
        oppgaverForPublisering.put(oppgaveId, "oppgave_oppdatert")
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
        if (oppgaver.size > 1) log.info("Oppgaveliste har ${oppgaver.size} oppgaver, hendelsesId: $hendelseId og contextId: $contextId")

        oppgaver.forEach { oppgave -> oppgave.lagre(this, contextId) }
        doAlso()
        oppgaver.clear()
        oppgaverForPublisering.onEach { (oppgaveId, eventName) ->
            messageContext.publish(Oppgave.lagMelding(oppgaveId, eventName, oppgaveDao).toJson())
        }.clear()
    }
}
