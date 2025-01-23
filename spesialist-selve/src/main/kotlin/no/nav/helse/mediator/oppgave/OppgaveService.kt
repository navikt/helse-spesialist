package no.nav.helse.mediator.oppgave

import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.MeldingPubliserer
import no.nav.helse.db.EgenskapForDatabase
import no.nav.helse.db.OppgaveDao
import no.nav.helse.db.OppgavesorteringForDatabase
import no.nav.helse.db.OpptegnelseRepository
import no.nav.helse.db.Repositories
import no.nav.helse.db.ReservasjonDao
import no.nav.helse.db.SaksbehandlerDao
import no.nav.helse.db.SessionContext
import no.nav.helse.db.SorteringsnøkkelForDatabase
import no.nav.helse.db.TildelingDao
import no.nav.helse.db.TotrinnsvurderingDao
import no.nav.helse.db.TotrinnsvurderingFraDatabase
import no.nav.helse.mediator.SaksbehandlerMediator.Companion.tilApiversjon
import no.nav.helse.mediator.TilgangskontrollørForApi
import no.nav.helse.mediator.oppgave.OppgaveMapper.tilApiversjon
import no.nav.helse.mediator.oppgave.OppgaveMapper.tilBehandledeOppgaver
import no.nav.helse.mediator.oppgave.OppgaveMapper.tilDatabaseversjon
import no.nav.helse.mediator.oppgave.OppgaveMapper.tilEgenskaperForVisning
import no.nav.helse.mediator.oppgave.OppgaveMapper.tilOppgaverTilBehandling
import no.nav.helse.modell.Modellfeil
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.oppgave.Oppgave.Companion.nyOppgave
import no.nav.helse.modell.oppgave.Oppgave.Companion.toDto
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.saksbehandler.Tilgangskontroll
import no.nav.helse.modell.saksbehandler.handlinger.EndrePåVent
import no.nav.helse.modell.saksbehandler.handlinger.LeggPåVent
import no.nav.helse.modell.saksbehandler.handlinger.Oppgavehandling
import no.nav.helse.modell.saksbehandler.handlinger.Overstyring
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingDto
import no.nav.helse.spesialist.api.abonnement.GodkjenningsbehovPayload
import no.nav.helse.spesialist.api.abonnement.OpptegnelseType
import no.nav.helse.spesialist.api.bootstrap.Tilgangsgrupper
import no.nav.helse.spesialist.api.graphql.schema.AntallOppgaver
import no.nav.helse.spesialist.api.graphql.schema.BehandledeOppgaver
import no.nav.helse.spesialist.api.graphql.schema.Filtrering
import no.nav.helse.spesialist.api.graphql.schema.Oppgaveegenskap
import no.nav.helse.spesialist.api.graphql.schema.OppgaverTilBehandling
import no.nav.helse.spesialist.api.graphql.schema.Oppgavesortering
import no.nav.helse.spesialist.api.graphql.schema.Sorteringsnokkel
import no.nav.helse.spesialist.api.oppgave.Oppgavehåndterer
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import org.slf4j.LoggerFactory
import java.sql.SQLException
import java.util.UUID

interface Oppgavefinner {
    fun oppgave(
        utbetalingId: UUID,
        oppgaveBlock: Oppgave.() -> Unit,
    )
}

class OppgaveService(
    private val oppgaveDao: OppgaveDao,
    private val tildelingDao: TildelingDao,
    private val reservasjonDao: ReservasjonDao,
    private val opptegnelseRepository: OpptegnelseRepository,
    private val totrinnsvurderingDao: TotrinnsvurderingDao,
    private val saksbehandlerDao: SaksbehandlerDao,
    private val meldingPubliserer: MeldingPubliserer,
    private val tilgangskontroll: Tilgangskontroll,
    private val tilgangsgrupper: Tilgangsgrupper,
    private val repositories: Repositories,
) : Oppgavehåndterer, Oppgavefinner {
    private val logg = LoggerFactory.getLogger(this::class.java)
    private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

    internal fun nyOppgaveService(sessionContext: SessionContext): OppgaveService =
        OppgaveService(
            oppgaveDao = sessionContext.oppgaveDao,
            tildelingDao = sessionContext.tildelingDao,
            reservasjonDao = sessionContext.reservasjonDao,
            opptegnelseRepository = sessionContext.opptegnelseRepository,
            totrinnsvurderingDao = sessionContext.totrinnsvurderingDao,
            saksbehandlerDao = sessionContext.saksbehandlerDao,
            meldingPubliserer = meldingPubliserer,
            tilgangskontroll = tilgangskontroll,
            tilgangsgrupper = tilgangsgrupper,
            repositories = repositories,
        )

    internal fun nyOppgave(
        fødselsnummer: String,
        vedtaksperiodeId: UUID,
        behandlingId: UUID,
        utbetalingId: UUID,
        hendelseId: UUID,
        kanAvvises: Boolean,
        egenskaper: Set<Egenskap>,
    ) {
        logg.info("Oppretter saksbehandleroppgave")
        sikkerlogg.info("Oppretter saksbehandleroppgave for {}", kv("fødselsnummer", fødselsnummer))
        val nesteId = oppgaveDao.reserverNesteId()
        val oppgavemelder = Oppgavemelder(fødselsnummer, meldingPubliserer)
        val oppgave =
            nyOppgave(
                id = nesteId,
                vedtaksperiodeId = vedtaksperiodeId,
                behandlingId = behandlingId,
                utbetalingId = utbetalingId,
                hendelseId = hendelseId,
                kanAvvises = kanAvvises,
                egenskaper = egenskaper,
            )
        oppgave.register(oppgavemelder)
        oppgavemelder.oppgaveOpprettet(oppgave)
        tildelVedReservasjon(fødselsnummer, oppgave)
        Oppgavelagrer(tildelingDao).lagre(this, oppgave.toDto())
    }

    fun <T> oppgave(
        id: Long,
        oppgaveBlock: Oppgave.() -> T,
    ): T {
        val oppgave =
            Oppgavehenter(
                oppgaveDao,
                totrinnsvurderingDao,
                saksbehandlerDao,
                tilgangskontroll,
            ).oppgave(id)
        val fødselsnummer = oppgaveDao.finnFødselsnummer(id)
        oppgave.register(Oppgavemelder(fødselsnummer, meldingPubliserer))
        val returverdi = oppgaveBlock(oppgave)
        Oppgavelagrer(tildelingDao).oppdater(this@OppgaveService, oppgave.toDto())
        return returverdi
    }

    override fun oppgave(
        utbetalingId: UUID,
        oppgaveBlock: Oppgave.() -> Unit,
    ) {
        val oppgaveId = oppgaveDao.finnOppgaveId(utbetalingId)
        oppgaveId?.let {
            oppgave(it, oppgaveBlock)
        }
    }

    fun lagreTotrinnsvurdering(totrinnsvurderingDto: TotrinnsvurderingDto) {
        val totrinnsvurderingFraDatabase =
            TotrinnsvurderingFraDatabase(
                vedtaksperiodeId = totrinnsvurderingDto.vedtaksperiodeId,
                erRetur = totrinnsvurderingDto.erRetur,
                saksbehandler = totrinnsvurderingDto.saksbehandler?.oid,
                beslutter = totrinnsvurderingDto.beslutter?.oid,
                utbetalingId = totrinnsvurderingDto.utbetalingId,
                opprettet = totrinnsvurderingDto.opprettet,
                oppdatert = totrinnsvurderingDto.oppdatert,
            )
        totrinnsvurderingDao.oppdater(totrinnsvurderingFraDatabase)
    }

    internal fun håndter(
        handling: Oppgavehandling,
        saksbehandler: Saksbehandler,
    ) {
        oppgave(handling.oppgaveId()) {
            handling.oppgave(this)
            handling.utførAv(saksbehandler)
        }
    }

    internal fun håndter(handling: Overstyring) {
        oppgaveDao.finnOppgaveId(handling.gjelderFødselsnummer())?.let {
            oppgave(it) {
                this.avbryt()
            }
        }
    }

    internal fun leggPåVent(
        handling: LeggPåVent,
        saksbehandler: Saksbehandler,
    ) {
        oppgave(handling.oppgaveId) {
            this.leggPåVent(handling.skalTildeles, saksbehandler)
        }
    }

    internal fun endrePåVent(
        handling: EndrePåVent,
        saksbehandler: Saksbehandler,
    ) {
        oppgave(handling.oppgaveId) {
            this.endrePåVent(handling.skalTildeles, saksbehandler)
        }
    }

    internal fun fjernFraPåVent(oppgaveId: Long) {
        oppgave(oppgaveId) {
            this.fjernFraPåVent()
        }
    }

    override fun sendTilBeslutter(
        oppgaveId: Long,
        behandlendeSaksbehandler: SaksbehandlerFraApi,
    ) {
        val saksbehandler = behandlendeSaksbehandler.tilSaksbehandler()
        oppgave(oppgaveId) {
            try {
                sendTilBeslutter(saksbehandler)
            } catch (e: Modellfeil) {
                throw e.tilApiversjon()
            }
        }
    }

    override fun sendIRetur(
        oppgaveId: Long,
        besluttendeSaksbehandler: SaksbehandlerFraApi,
    ) {
        val saksbehandler = besluttendeSaksbehandler.tilSaksbehandler()
        oppgave(oppgaveId) {
            try {
                sendIRetur(saksbehandler)
            } catch (e: Modellfeil) {
                throw e.tilApiversjon()
            }
        }
    }

    override fun endretEgenAnsattStatus(
        erEgenAnsatt: Boolean,
        fødselsnummer: String,
    ) {
        val oppgaveId =
            oppgaveDao.finnOppgaveId(fødselsnummer) ?: run {
                sikkerlogg.info("Ingen aktiv oppgave for {}", kv("fødselsnummer", fødselsnummer))
                return
            }
        oppgave(oppgaveId) {
            if (erEgenAnsatt) {
                logg.info("Legger til egenskap EGEN_ANSATT på {}", kv("oppgaveId", oppgaveId))
                sikkerlogg.info("Legger til egenskap EGEN_ANSATT for {}", kv("fødselsnummer", fødselsnummer))
                leggTilEgenAnsatt()
            } else {
                logg.info("Fjerner egenskap EGEN_ANSATT på {}", kv("oppgaveId", oppgaveId))
                sikkerlogg.info("Fjerner egenskap EGEN_ANSATT for {}", kv("fødselsnummer", fødselsnummer))
                fjernEgenAnsatt()
            }
        }
    }

    override fun venterPåSaksbehandler(oppgaveId: Long): Boolean = oppgaveDao.venterPåSaksbehandler(oppgaveId)

    override fun spleisBehandlingId(oppgaveId: Long): UUID = oppgaveDao.finnSpleisBehandlingId(oppgaveId)

    override fun fødselsnummer(oppgaveId: Long): String = oppgaveDao.finnFødselsnummer(oppgaveId)

    override fun oppgaver(
        saksbehandlerFraApi: SaksbehandlerFraApi,
        offset: Int,
        limit: Int,
        sortering: List<Oppgavesortering>,
        filtrering: Filtrering,
    ): OppgaverTilBehandling {
        val saksbehandler = saksbehandlerFraApi.tilSaksbehandler()
        val egenskaperSaksbehandlerIkkeHarTilgangTil =
            Egenskap
                .alleTilgangsstyrteEgenskaper
                .filterNot { saksbehandler.harTilgangTil(listOf(it)) }
                .map(Egenskap::toString)

        val alleUkategoriserteEgenskaper =
            Egenskap
                .alleUkategoriserteEgenskaper
                .map(Egenskap::toString)

        val ekskluderteEgenskaper =
            filtrering.ekskluderteEgenskaper?.tilDatabaseversjon()?.map(
                EgenskapForDatabase::toString,
            ) ?: emptyList()

        val egenskaperSomSkalEkskluderes =
            egenskaperSaksbehandlerIkkeHarTilgangTil + ekskluderteEgenskaper + if (filtrering.ingenUkategoriserteEgenskaper) alleUkategoriserteEgenskaper else emptyList()

        val grupperteFiltrerteEgenskaper =
            filtrering.egenskaper
                .groupBy { it.kategori }
                .map { it.key.tilDatabaseversjon() to it.value.tilDatabaseversjon() }
                .toMap()

        val oppgaver =
            oppgaveDao
                .finnOppgaverForVisning(
                    ekskluderEgenskaper = egenskaperSomSkalEkskluderes,
                    saksbehandlerOid = saksbehandler.oid(),
                    offset = offset,
                    limit = limit,
                    sortering = sortering.tilOppgavesorteringForDatabase(),
                    egneSakerPåVent = filtrering.egneSakerPaVent,
                    egneSaker = filtrering.egneSaker,
                    tildelt = filtrering.tildelt,
                    grupperteFiltrerteEgenskaper = grupperteFiltrerteEgenskaper,
                )
        return OppgaverTilBehandling(
            oppgaver = oppgaver.tilOppgaverTilBehandling(),
            totaltAntallOppgaver = if (oppgaver.isEmpty()) 0 else oppgaver.first().filtrertAntall,
        )
    }

    override fun antallOppgaver(saksbehandlerFraApi: SaksbehandlerFraApi): AntallOppgaver {
        val saksbehandler = saksbehandlerFraApi.tilSaksbehandler()
        val antallOppgaver = oppgaveDao.finnAntallOppgaver(saksbehandlerOid = saksbehandler.oid())
        return antallOppgaver.tilApiversjon()
    }

    override fun behandledeOppgaver(
        saksbehandlerFraApi: SaksbehandlerFraApi,
        offset: Int,
        limit: Int,
    ): BehandledeOppgaver {
        val saksbehandler = saksbehandlerFraApi.tilSaksbehandler()
        val behandledeOppgaver =
            oppgaveDao.finnBehandledeOppgaver(
                behandletAvOid = saksbehandler.oid(),
                offset = offset,
                limit = limit,
            )
        return BehandledeOppgaver(
            oppgaver = behandledeOppgaver.tilBehandledeOppgaver(),
            totaltAntallOppgaver = if (behandledeOppgaver.isEmpty()) 0 else behandledeOppgaver.first().filtrertAntall,
        )
    }

    override fun hentEgenskaper(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
    ): List<Oppgaveegenskap> {
        val egenskaper =
            oppgaveDao.finnEgenskaper(
                vedtaksperiodeId = vedtaksperiodeId,
                utbetalingId = utbetalingId,
            )

        return egenskaper?.tilEgenskaperForVisning() ?: emptyList()
    }

    fun avbrytOppgaveFor(vedtaksperiodeId: UUID) {
        oppgaveDao.finnIdForAktivOppgave(vedtaksperiodeId)?.also {
            oppgave(it) {
                this.avbryt()
            }
        }
    }

    fun fjernTilbakedatert(vedtaksperiodeId: UUID) {
        oppgaveDao.finnIdForAktivOppgave(vedtaksperiodeId)?.also { oppgaveId ->
            oppgave(oppgaveId) {
                logg.info("Fjerner egenskap TILBAKEDATERT på {}", kv("oppgaveId", oppgaveId))
                fjernTilbakedatert()
            }
        }
    }

    fun fjernGosysEgenskap(vedtaksperiodeId: UUID) {
        oppgaveDao.finnIdForAktivOppgave(vedtaksperiodeId)?.also { oppgaveId ->
            oppgave(oppgaveId) {
                logg.info("Fjerner egenskap GOSYS på {}", kv("oppgaveId", oppgaveId))
                fjernGosys()
            }
        }
    }

    fun leggTilGosysEgenskap(vedtaksperiodeId: UUID) {
        oppgaveDao.finnIdForAktivOppgave(vedtaksperiodeId)?.also { oppgaveId ->
            oppgave(oppgaveId) {
                logg.info("Legger til egenskap GOSYS på {}", kv("oppgaveId", oppgaveId))
                leggTilGosys()
            }
        }
    }

    fun opprett(
        id: Long,
        vedtaksperiodeId: UUID,
        behandlingId: UUID,
        utbetalingId: UUID,
        egenskaper: List<EgenskapForDatabase>,
        godkjenningsbehovId: UUID,
        kanAvvises: Boolean,
    ) {
        oppgaveDao.opprettOppgave(id, godkjenningsbehovId, egenskaper, vedtaksperiodeId, behandlingId, utbetalingId, kanAvvises)
        opptegnelseRepository.opprettOpptegnelse(
            oppgaveDao.finnFødselsnummer(id),
            GodkjenningsbehovPayload(godkjenningsbehovId),
            OpptegnelseType.NY_SAKSBEHANDLEROPPGAVE,
        )
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

    fun reserverOppgave(
        saksbehandleroid: UUID,
        fødselsnummer: String,
    ) {
        try {
            reservasjonDao.reserverPerson(saksbehandleroid, fødselsnummer)
        } catch (e: SQLException) {
            logg.warn("Kunne ikke reservere person")
        }
    }

    private fun tildelVedReservasjon(
        fødselsnummer: String,
        oppgave: Oppgave,
    ) {
        val (saksbehandlerFraDatabase) =
            reservasjonDao.hentReservasjonFor(fødselsnummer) ?: run {
                logg.info("Finner ingen reservasjon for $oppgave, blir ikke tildelt.")
                return
            }
        val saksbehandler =
            Saksbehandler(
                epostadresse = saksbehandlerFraDatabase.epostadresse,
                oid = saksbehandlerFraDatabase.oid,
                navn = saksbehandlerFraDatabase.navn,
                ident = saksbehandlerFraDatabase.ident,
                tilgangskontroll = tilgangskontroll,
            )
        oppgave.forsøkTildelingVedReservasjon(saksbehandler)
    }

    fun harFerdigstiltOppgave(vedtaksperiodeId: UUID) = oppgaveDao.harFerdigstiltOppgave(vedtaksperiodeId)

    private fun SaksbehandlerFraApi.tilSaksbehandler() =
        Saksbehandler(
            epostadresse = epost,
            oid = oid,
            navn = navn,
            ident = ident,
            tilgangskontroll = TilgangskontrollørForApi(grupper, tilgangsgrupper),
        )

    private fun List<Oppgavesortering>.tilOppgavesorteringForDatabase() =
        map {
            when (it.nokkel) {
                Sorteringsnokkel.TILDELT_TIL ->
                    OppgavesorteringForDatabase(
                        SorteringsnøkkelForDatabase.TILDELT_TIL,
                        it.stigende,
                    )

                Sorteringsnokkel.OPPRETTET ->
                    OppgavesorteringForDatabase(
                        SorteringsnøkkelForDatabase.OPPRETTET,
                        it.stigende,
                    )

                Sorteringsnokkel.SOKNAD_MOTTATT ->
                    OppgavesorteringForDatabase(
                        SorteringsnøkkelForDatabase.SØKNAD_MOTTATT,
                        it.stigende,
                    )

                Sorteringsnokkel.TIDSFRIST ->
                    OppgavesorteringForDatabase(
                        SorteringsnøkkelForDatabase.TIDSFRIST,
                        it.stigende,
                    )
            }
        }
}
