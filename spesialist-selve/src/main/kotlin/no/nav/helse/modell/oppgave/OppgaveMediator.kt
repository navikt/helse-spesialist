package no.nav.helse.modell.oppgave

import java.sql.SQLException
import java.util.UUID
import no.nav.helse.Tilgangskontroll
import no.nav.helse.modell.oppgave.Oppgave.Companion.loggOppgaverAvbrutt
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.spesialist.api.abonnement.GodkjenningsbehovPayload
import no.nav.helse.spesialist.api.abonnement.GodkjenningsbehovPayload.Companion.lagre
import no.nav.helse.spesialist.api.abonnement.OpptegnelseDao
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus
import no.nav.helse.spesialist.api.oppgave.Oppgavetype
import no.nav.helse.spesialist.api.reservasjon.ReservasjonDao
import no.nav.helse.spesialist.api.tildeling.TildelingDao
import org.slf4j.LoggerFactory

class OppgaveMediator(
    private val oppgaveDao: OppgaveDao,
    private val tildelingDao: TildelingDao,
    private val reservasjonDao: ReservasjonDao,
    private val opptegnelseDao: OpptegnelseDao,
    private val harTilgangTil: Tilgangskontroll = { _, _ -> false },
) {
    private var oppgaveForLagring: Oppgave? = null
    private val oppgaverForPublisering = mutableMapOf<Long, String>()
    private val log = LoggerFactory.getLogger(this::class.java)

    fun opprett(oppgave: Oppgave) {
        leggPåVentForSenereLagring(oppgave)
    }

    fun tildel(oppgaveId: Long, saksbehandleroid: UUID, påVent: Boolean = false): Boolean {
        return tildelingDao.opprettTildeling(oppgaveId, saksbehandleroid, påVent)
    }

    /*
        For nå må oppgaver mellomlagres i denne mediatoren, fordi ved lagring skal det sendes ut meldinger på Kafka,
        og de skal inneholde standardfeltene for rapids-and-rivers, som i utgangspunktet kun er tilgjengelige via
        MessageContext, som HendelseMediator har tilgang til.
    */
    private fun leggPåVentForSenereLagring(oppgave: Oppgave) {
        oppgaveForLagring = oppgave
    }

    fun ferdigstill(oppgave: Oppgave, saksbehandlerIdent: String, oid: UUID) {
        oppgave.ferdigstill(saksbehandlerIdent, oid)
        leggPåVentForSenereLagring(oppgave)
    }

    fun ferdigstill(oppgave: Oppgave) {
        oppgave.ferdigstill()
        leggPåVentForSenereLagring(oppgave)
    }

    private fun avbryt(oppgave: Oppgave) {
        oppgave.avbryt()
        leggPåVentForSenereLagring(oppgave)
    }

    fun invalider(oppgave: Oppgave) {
        oppgave.avbryt()
        leggPåVentForSenereLagring(oppgave)
    }

    fun lagreOgTildelOppgaver(
        hendelseId: UUID,
        fødselsnummer: String,
        contextId: UUID,
        messageContext: MessageContext,
    ) {
        lagreOppgaver(hendelseId, contextId, messageContext) { tildelOppgaver(fødselsnummer) }
    }

    fun avbrytOppgaver(vedtaksperiodeId: UUID) {
        oppgaveDao.finnAktive(vedtaksperiodeId)
            .also { it.loggOppgaverAvbrutt(vedtaksperiodeId) }
            .map(::avbryt)
    }

    fun opprett(
        contextId: UUID,
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
        navn: Oppgavetype,
        hendelseId: UUID
    ): Long? {
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
        } catch (e: SQLException) {
            log.warn("Kunne ikke reservere person")
        }
    }

    private fun tildelOppgaver(fødselsnummer: String) {
        reservasjonDao.hentReservasjonFor(fødselsnummer)?.let { (oid, settPåVent) ->
            oppgaveForLagring?.tildelHvisIkkeStikkprøve(this, oid, settPåVent, harTilgangTil)
        }
    }

    private fun lagreOppgaver(
        hendelseId: UUID,
        contextId: UUID,
        messageContext: MessageContext,
        doAlso: () -> Unit = {}
    ) {
        oppgaveForLagring?.lagre(this, contextId, hendelseId)
        doAlso()
        oppgaveForLagring = null
        oppgaverForPublisering.onEach { (oppgaveId, eventName) ->
            messageContext.publish(Oppgave.lagMelding(oppgaveId, eventName, oppgaveDao = oppgaveDao).second.toJson())
        }.clear()
    }

    fun harFerdigstiltOppgave(vedtaksperiodeId: UUID) = oppgaveDao.harFerdigstiltOppgave(vedtaksperiodeId)
}
