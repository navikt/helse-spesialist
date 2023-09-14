package no.nav.helse.mediator.oppgave

import java.sql.SQLException
import java.util.UUID
import no.nav.helse.Tilgangskontroll
import no.nav.helse.db.SaksbehandlerRepository
import no.nav.helse.db.TildelingDao
import no.nav.helse.db.TotrinnsvurderingFraDatabase
import no.nav.helse.db.TotrinnsvurderingRepository
import no.nav.helse.modell.HendelseDao
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spesialist.api.abonnement.GodkjenningsbehovPayload
import no.nav.helse.spesialist.api.abonnement.GodkjenningsbehovPayload.Companion.lagre
import no.nav.helse.spesialist.api.abonnement.OpptegnelseDao
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus
import no.nav.helse.spesialist.api.oppgave.Oppgavetype
import no.nav.helse.spesialist.api.reservasjon.ReservasjonDao
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import no.nav.helse.spesialist.api.tildeling.Oppgavehåndterer
import no.nav.helse.spesialist.api.tildeling.TildelingApiDto
import org.slf4j.LoggerFactory

interface Oppgavefinner {
    fun oppgave(utbetalingId: UUID, oppgaveBlock: Oppgave?.() -> Unit)
}

internal class OppgaveMediator(
    private val hendelseDao: HendelseDao,
    private val oppgaveDao: OppgaveDao,
    private val tildelingDao: TildelingDao,
    private val reservasjonDao: ReservasjonDao,
    private val opptegnelseDao: OpptegnelseDao,
    private val totrinnsvurderingRepository: TotrinnsvurderingRepository,
    private val saksbehandlerRepository: SaksbehandlerRepository,
    private val rapidsConnection: RapidsConnection,
    private val harTilgangTil: Tilgangskontroll = { _, _ -> false }
): Oppgavehåndterer, Oppgavefinner {
    private var oppgaveForLagring: Oppgave? = null
    private var oppgaveForOppdatering: Oppgave? = null
    private val oppgaverForPublisering = mutableMapOf<Long, String>()
    private val logg = LoggerFactory.getLogger(this::class.java)
    private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

    internal fun nyOppgave(opprettOppgaveBlock: (reservertId: Long) -> Oppgave) {
        val nesteId = oppgaveDao.reserverNesteId()
        val oppgave = opprettOppgaveBlock(nesteId)
        oppgave.register(Oppgavemelder(hendelseDao, oppgaveDao, rapidsConnection))
        leggPåVentForSenereLagring(oppgave)
    }

    fun <T> oppgave(id: Long, oppgaveBlock: Oppgave.() -> T): T {
        val oppgave = Oppgavehenter(oppgaveDao, totrinnsvurderingRepository, saksbehandlerRepository).oppgave(id)
        oppgave.register(Oppgavemelder(hendelseDao, oppgaveDao, rapidsConnection))
        val returverdi = oppgaveBlock(oppgave)
        Oppgavelagrer(tildelingDao).apply {
            oppgave.accept(this)
            oppdater(this@OppgaveMediator)
        }
        leggPåVentForSenereOppdatering(oppgave)
        return returverdi
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

    override fun sendTilBeslutter(oppgaveId: Long, behandlendeSaksbehandler: SaksbehandlerFraApi) {
        val saksbehandler = behandlendeSaksbehandler.tilSaksbehandler()
        oppgave(oppgaveId) {
            sendTilBeslutter(saksbehandler)
        }
    }

    override fun sendIRetur(oppgaveId: Long, besluttendeSaksbehandler: SaksbehandlerFraApi) {
        val saksbehandler = besluttendeSaksbehandler.tilSaksbehandler()
        oppgave(oppgaveId) {
            sendIRetur(saksbehandler)
        }
    }

    override fun leggPåVent(oppgaveId: Long): TildelingApiDto {
        return oppgave(oppgaveId) {
            val tildeltTil = this.leggPåVent()
            tildeltTil.toDto().let {
                TildelingApiDto(it.navn, it.epost, it.oid, true)
            }
        }
    }

    override fun fjernPåVent(oppgaveId: Long): TildelingApiDto {
        return oppgave(oppgaveId) {
            val tildeltTil = this.fjernPåVent()
            tildeltTil.toDto().let {
                TildelingApiDto(it.navn, it.epost, it.oid, true)
            }
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
        fødselsnummer: String,
        contextId: UUID,
        messageContext: MessageContext,
    ) {
        tildelVedReservasjon(fødselsnummer)
        lagreOppgaver(contextId, messageContext)
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
        // TODO: skal ikke være SaksbehandlerFraApi, men SaksbehandlerFraDatabase. Må fikses når ReservasjonDao kan flyttes til selve.db
        val (saksbehandlerFraDatabase, settPåVent) = reservasjonDao.hentReservasjonFor(fødselsnummer) ?: return
        val saksbehandler = Saksbehandler(saksbehandlerFraDatabase.epost, saksbehandlerFraDatabase.oid, saksbehandlerFraDatabase.navn, saksbehandlerFraDatabase.ident)
        oppgaveForLagring?.forsøkTildeling(saksbehandler, settPåVent, harTilgangTil)
    }

    private fun lagreOppgaver(
        contextId: UUID,
        messageContext: MessageContext
    ) {
        oppgaveForLagring?.let {
            Oppgavelagrer(tildelingDao).apply {
                it.accept(this)
                lagre(this@OppgaveMediator, contextId)
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

    private fun SaksbehandlerFraApi.tilSaksbehandler() = Saksbehandler(
        epostadresse = epost,
        oid = oid,
        navn = navn,
        ident = ident
    )
}
