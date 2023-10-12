package no.nav.helse.mediator.oppgave

import java.sql.SQLException
import java.util.UUID
import no.nav.helse.Tilgangsgrupper
import no.nav.helse.db.EgenskapForDatabase
import no.nav.helse.db.OppgavesorteringForDatabase
import no.nav.helse.db.ReservasjonDao
import no.nav.helse.db.SaksbehandlerRepository
import no.nav.helse.db.SorteringsnøkkelForDatabase
import no.nav.helse.db.TildelingDao
import no.nav.helse.db.TotrinnsvurderingFraDatabase
import no.nav.helse.db.TotrinnsvurderingRepository
import no.nav.helse.mediator.SaksbehandlerMediator.Companion.tilApiversjon
import no.nav.helse.mediator.TilgangskontrollørForApi
import no.nav.helse.mediator.oppgave.OppgaveMapper.tilOppgaverTilBehandling
import no.nav.helse.mediator.saksbehandler.SaksbehandlerMapper.tilModellversjon
import no.nav.helse.modell.HendelseDao
import no.nav.helse.modell.ManglerTilgang
import no.nav.helse.modell.Modellfeil
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.saksbehandler.Tilgangskontroll
import no.nav.helse.modell.saksbehandler.handlinger.Oppgavehandling
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spesialist.api.abonnement.GodkjenningsbehovPayload
import no.nav.helse.spesialist.api.abonnement.GodkjenningsbehovPayload.Companion.lagre
import no.nav.helse.spesialist.api.abonnement.OpptegnelseDao
import no.nav.helse.spesialist.api.graphql.schema.OppgaveTilBehandling
import no.nav.helse.spesialist.api.graphql.schema.Oppgavesortering
import no.nav.helse.spesialist.api.graphql.schema.Sorteringsnokkel
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import no.nav.helse.spesialist.api.tildeling.Oppgavehåndterer
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
    private val tilgangskontroll: Tilgangskontroll,
    private val tilgangsgrupper: Tilgangsgrupper
): Oppgavehåndterer, Oppgavefinner {
    private val logg = LoggerFactory.getLogger(this::class.java)

    internal fun nyOppgave(fødselsnummer: String, contextId: UUID, opprettOppgaveBlock: (reservertId: Long) -> Oppgave) {
        val nesteId = oppgaveDao.reserverNesteId()
        val oppgave = opprettOppgaveBlock(nesteId)
        val oppgavemelder = Oppgavemelder(hendelseDao, oppgaveDao, rapidsConnection)
        oppgavemelder.oppgaveOpprettet(oppgave)
        oppgave.register(oppgavemelder)
        tildelVedReservasjon(fødselsnummer, oppgave)
        Oppgavelagrer(tildelingDao).apply {
            oppgave.accept(this)
            this.lagre(this@OppgaveMediator, contextId)
        }
    }

    fun <T> oppgave(id: Long, oppgaveBlock: Oppgave.() -> T): T {
        val oppgave = Oppgavehenter(oppgaveDao, totrinnsvurderingRepository, saksbehandlerRepository, tilgangskontroll).oppgave(id)
        oppgave.register(Oppgavemelder(hendelseDao, oppgaveDao, rapidsConnection))
        val returverdi = oppgaveBlock(oppgave)
        Oppgavelagrer(tildelingDao).apply {
            oppgave.accept(this)
            oppdater(this@OppgaveMediator)
        }
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

    internal fun håndter(handling: Oppgavehandling, saksbehandler: Saksbehandler) {
        oppgave(handling.oppgaveId()) {
            handling.oppgave(this)
            handling.utførAv(saksbehandler)
        }
    }

    override fun sendTilBeslutter(oppgaveId: Long, behandlendeSaksbehandler: SaksbehandlerFraApi) {
        val saksbehandler = behandlendeSaksbehandler.tilSaksbehandler()
        oppgave(oppgaveId) {
            try {
                sendTilBeslutter(saksbehandler)
            } catch (e: Modellfeil) {
                throw e.tilApiversjon()
            }
        }
    }

    override fun sendIRetur(oppgaveId: Long, besluttendeSaksbehandler: SaksbehandlerFraApi) {
        val saksbehandler = besluttendeSaksbehandler.tilSaksbehandler()
        oppgave(oppgaveId) {
            try {
                sendIRetur(saksbehandler)
            } catch (e: Modellfeil) {
                throw e.tilApiversjon()
            }
        }
    }

    override fun leggPåVent(oppgaveId: Long, saksbehandler: SaksbehandlerFraApi) {
        return oppgave(oppgaveId) {
            try {
                this.leggPåVent(saksbehandler.tilModellversjon(tilgangsgrupper))
            } catch (e: Modellfeil) {
                throw e.tilApiversjon()
            }
        }
    }

    override fun fjernPåVent(oppgaveId: Long, saksbehandler: SaksbehandlerFraApi) {
        oppgave(oppgaveId) {
            try {
                this.fjernPåVent(saksbehandler.tilModellversjon(tilgangsgrupper))
            } catch (e: Modellfeil) {
                throw e.tilApiversjon()
            }
        }
    }

    override fun venterPåSaksbehandler(oppgaveId: Long): Boolean = oppgaveDao.venterPåSaksbehandler(oppgaveId)
    override fun erRiskoppgave(oppgaveId: Long): Boolean = oppgaveDao.erRiskoppgave(oppgaveId)

    override fun oppgaver(
        saksbehandlerFraApi: SaksbehandlerFraApi,
        startIndex: Int,
        pageSize: Int,
        sortering: List<Oppgavesortering>
    ): List<OppgaveTilBehandling> {
        val saksbehandler = saksbehandlerFraApi.tilSaksbehandler()
        val egenskaperSaksbehandlerIkkeHarTilgangTil = Egenskap
            .alleTilgangsstyrteEgenskaper
            .filterNot { saksbehandler.harTilgangTil(listOf(it)) }
            .map(Egenskap::toString)

        val oppgaver = oppgaveDao
            .finnOppgaverForVisning(
                ekskluderEgenskaper = egenskaperSaksbehandlerIkkeHarTilgangTil,
                saksbehandlerOid = saksbehandler.oid(),
                startIndex = startIndex,
                pageSize = pageSize,
                sortering = sortering.tilOppgavesorteringForDatabase()
            )
        return oppgaver.tilOppgaverTilBehandling()
    }

    fun avbrytOppgaveFor(vedtaksperiodeId: UUID) {
        oppgaveDao.finnNyesteOppgaveId(vedtaksperiodeId)?.also {
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
        egenskap: String,
        egenskaper: List<EgenskapForDatabase>,
        hendelseId: UUID,
        kanAvvises: Boolean,
    ) {
        oppgaveDao.opprettOppgave(id, contextId, egenskap, egenskaper, vedtaksperiodeId, utbetalingId, kanAvvises)
        GodkjenningsbehovPayload(hendelseId).lagre(opptegnelseDao, oppgaveDao.finnFødselsnummer(id))
    }

    fun oppdater(
        oppgaveId: Long,
        status: String,
        ferdigstiltAvIdent: String?,
        ferdigstiltAvOid: UUID?,
        egenskaper: List<EgenskapForDatabase>,
    ) {
        oppgaveDao.updateOppgave(oppgaveId, status, ferdigstiltAvIdent, ferdigstiltAvOid, egenskaper)
    }

    fun reserverOppgave(saksbehandleroid: UUID, fødselsnummer: String) {
        try {
            reservasjonDao.reserverPerson(saksbehandleroid, fødselsnummer, false)
        } catch (e: SQLException) {
            logg.warn("Kunne ikke reservere person")
        }
    }

    private fun tildelVedReservasjon(fødselsnummer: String, oppgave: Oppgave) {
        val (saksbehandlerFraDatabase, settPåVent) = reservasjonDao.hentReservasjonFor(fødselsnummer) ?: return
        val saksbehandler = Saksbehandler(
            epostadresse = saksbehandlerFraDatabase.epostadresse,
            oid = saksbehandlerFraDatabase.oid,
            navn = saksbehandlerFraDatabase.navn,
            ident = saksbehandlerFraDatabase.ident,
            tilgangskontroll = tilgangskontroll
        )
        try {
            oppgave.forsøkTildelingVedReservasjon(saksbehandler, settPåVent)
        } catch (_: ManglerTilgang) {}
    }

    fun harFerdigstiltOppgave(vedtaksperiodeId: UUID) = oppgaveDao.harFerdigstiltOppgave(vedtaksperiodeId)

    private fun SaksbehandlerFraApi.tilSaksbehandler() = Saksbehandler(
        epostadresse = epost,
        oid = oid,
        navn = navn,
        ident = ident,
        tilgangskontroll = TilgangskontrollørForApi(grupper, tilgangsgrupper),
    )


    private fun List<Oppgavesortering>.tilOppgavesorteringForDatabase() = map {
        when (it.nokkel) {
            Sorteringsnokkel.TILDELT_TIL -> OppgavesorteringForDatabase(SorteringsnøkkelForDatabase.TILDELT_TIL, it.stigende)
            Sorteringsnokkel.OPPRETTET -> OppgavesorteringForDatabase(SorteringsnøkkelForDatabase.OPPRETTET, it.stigende)
            Sorteringsnokkel.SOKNAD_MOTTATT -> OppgavesorteringForDatabase(SorteringsnøkkelForDatabase.SØKNAD_MOTTATT, it.stigende)
        }
    }
}
