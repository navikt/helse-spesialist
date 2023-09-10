package no.nav.helse.mediator.oppgave

import java.sql.SQLException
import java.util.UUID
import no.nav.helse.Tilgangskontroll
import no.nav.helse.db.SaksbehandlerRepository
import no.nav.helse.db.TotrinnsvurderingFraDatabase
import no.nav.helse.db.TotrinnsvurderingRepository
import no.nav.helse.mediator.api.Oppgavehåndterer
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.spesialist.api.abonnement.GodkjenningsbehovPayload
import no.nav.helse.spesialist.api.abonnement.GodkjenningsbehovPayload.Companion.lagre
import no.nav.helse.spesialist.api.abonnement.OpptegnelseDao
import no.nav.helse.spesialist.api.modell.Saksbehandler
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
    private val totrinnsvurderingRepository: TotrinnsvurderingRepository,
    private val saksbehandlerRepository: SaksbehandlerRepository,
    private val harTilgangTil: Tilgangskontroll = { _, _ -> false },
): Oppgavehåndterer {
    private var oppgaveForLagring: Oppgave? = null
    private var oppgaveForOppdatering: Oppgave? = null
    private val oppgaverForPublisering = mutableMapOf<Long, String>()
    private val logg = LoggerFactory.getLogger(this::class.java)
    private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

    internal fun nyOppgave(opprettOppgaveBlock: (reservertId: Long) -> Oppgave) {
        val nesteId = oppgaveDao.reserverNesteId()
        val oppgave = opprettOppgaveBlock(nesteId)
        leggPåVentForSenereLagring(oppgave)
    }

    internal fun tildel(oppgaveId: Long, saksbehandleroid: UUID, påVent: Boolean = false): Boolean {
        return tildelingDao.opprettTildeling(oppgaveId, saksbehandleroid, påVent) != null
    }

    internal fun avmeld(oppgaveId: Long) {
        tildelingDao.slettTildeling(oppgaveId)
    }

    fun oppgave(id: Long, oppgaveBlock: Oppgave.() -> Unit) {
        val oppgave = Oppgavehenter(oppgaveDao, totrinnsvurderingRepository, saksbehandlerRepository).oppgave(id)
        oppgaveBlock(oppgave)
        Oppgavelagrer().apply {
            oppgave.accept(this)
            oppdater(this@OppgaveMediator)
        }
        leggPåVentForSenereOppdatering(oppgave)
    }

    override fun oppgave(utbetalingId: UUID, oppgaveBlock: Oppgave?.() -> Unit) {
        val oppgaveId = oppgaveDao.finnOppgaveId(utbetalingId)
        oppgaveId?.let {
            oppgave(it, oppgaveBlock)
        }
    }

    internal fun lagreTotrinnsvurdering(totrinnsvurderingFraDatabase: TotrinnsvurderingFraDatabase) {
        totrinnsvurderingRepository.oppdater(totrinnsvurderingFraDatabase)
    }

    override fun sendTilBeslutter(oppgaveId: Long, behandlendeSaksbehandler: Saksbehandler) {
        oppgave(oppgaveId) {
            sendTilBeslutter(behandlendeSaksbehandler)
        }
    }

    override fun sendIRetur(oppgaveId: Long, besluttendeSaksbehandler: Saksbehandler) {
        oppgave(oppgaveId) {
            sendIRetur(besluttendeSaksbehandler)
        }
    }

    /*
        For nå må oppgaver mellomlagres i denne mediatoren, fordi ved lagring skal det sendes ut meldinger på Kafka,
        og de skal inneholde standardfeltene for rapids-and-rivers, som i utgangspunktet kun er tilgjengelige via
        MessageContext, som HendelseMediator har tilgang til.
    */
    private fun leggPåVentForSenereLagring(oppgave: Oppgave) {
        oppgaveForLagring = oppgave
    }
    private fun leggPåVentForSenereOppdatering(oppgave: Oppgave) {
        oppgaveForOppdatering = oppgave
    }

    fun lagreOgTildelOppgaver(
        hendelseId: UUID,
        fødselsnummer: String,
        contextId: UUID,
        messageContext: MessageContext,
    ) {
        tildelVedReservasjon(fødselsnummer)
        lagreOppgaver(hendelseId, contextId, messageContext)
    }

    fun avbrytOppgaver(vedtaksperiodeId: UUID) {
        oppgaveDao.finnNyesteOppgaveId(vedtaksperiodeId)?.also { it ->
            oppgave(it) {
                this.avbryt()
            }
        }
    }

    internal fun opprett(
        id: Long,
        contextId: UUID,
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
        navn: Oppgavetype,
        hendelseId: UUID
    ) {
        if (oppgaveDao.harGyldigOppgave(utbetalingId)) return
        oppgaveDao.opprettOppgave(id, contextId, navn, vedtaksperiodeId, utbetalingId)
        oppgaverForPublisering[id] = "oppgave_opprettet"
        GodkjenningsbehovPayload(hendelseId).lagre(opptegnelseDao, oppgaveDao.finnFødselsnummer(id))
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
            reservasjonDao.reserverPerson(saksbehandleroid, fødselsnummer, false)
        } catch (e: SQLException) {
            logg.warn("Kunne ikke reservere person")
        }
    }

    private fun tildelVedReservasjon(fødselsnummer: String) {
        val (saksbehandler, settPåVent) = reservasjonDao.hentReservasjonFor(fødselsnummer) ?: return
        oppgaveForLagring?.forsøkTildeling(saksbehandler, settPåVent, harTilgangTil)
    }

    private fun lagreOppgaver(
        hendelseId: UUID,
        contextId: UUID,
        messageContext: MessageContext
    ) {
        oppgaveForLagring?.let {
            Oppgavelagrer().apply {
                it.accept(this)
                lagre(this@OppgaveMediator, hendelseId, contextId)
            }
            logg.info("Oppgave lagret: $it")
            sikkerlogg.info("Oppgave lagret: $it")
        }
        oppgaveForLagring = null
        oppgaveForOppdatering = null
        oppgaverForPublisering.onEach { (oppgaveId, eventName) ->
            messageContext.publish(Oppgave.lagMelding(oppgaveId, eventName, oppgaveDao = oppgaveDao).second.toJson())
        }.clear()
    }

    fun harFerdigstiltOppgave(vedtaksperiodeId: UUID) = oppgaveDao.harFerdigstiltOppgave(vedtaksperiodeId)
}
