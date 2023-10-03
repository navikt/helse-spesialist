package no.nav.helse.mediator.oppgave

import java.sql.SQLException
import java.util.UUID
import no.nav.helse.Tilgangsgrupper
import no.nav.helse.db.OppgaveFraDatabaseForVisning
import no.nav.helse.db.ReservasjonDao
import no.nav.helse.db.SaksbehandlerRepository
import no.nav.helse.db.TildelingDao
import no.nav.helse.db.TotrinnsvurderingFraDatabase
import no.nav.helse.db.TotrinnsvurderingRepository
import no.nav.helse.mediator.TilgangskontrollørForApi
import no.nav.helse.modell.HendelseDao
import no.nav.helse.modell.Modellfeil
import no.nav.helse.modell.OppgaveAlleredeSendtBeslutter
import no.nav.helse.modell.OppgaveAlleredeSendtIRetur
import no.nav.helse.modell.OppgaveKreverVurderingAvToSaksbehandlere
import no.nav.helse.modell.OppgaveTildeltNoenAndre
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.saksbehandler.Tilgangskontroll
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spesialist.api.abonnement.GodkjenningsbehovPayload
import no.nav.helse.spesialist.api.abonnement.GodkjenningsbehovPayload.Companion.lagre
import no.nav.helse.spesialist.api.abonnement.OpptegnelseDao
import no.nav.helse.spesialist.api.feilhåndtering.OppgaveIkkeTildelt
import no.nav.helse.spesialist.api.graphql.schema.OppgaveTilBehandling
import no.nav.helse.spesialist.api.graphql.schema.Oppgaveegenskap
import no.nav.helse.spesialist.api.graphql.schema.Personnavn
import no.nav.helse.spesialist.api.graphql.schema.Tildeling
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import no.nav.helse.spesialist.api.tildeling.Oppgavehåndterer
import no.nav.helse.spesialist.api.tildeling.TildelingApiDto
import org.slf4j.LoggerFactory
import no.nav.helse.spesialist.api.graphql.schema.Egenskap as EgenskapForApi
import no.nav.helse.spesialist.api.graphql.schema.Kategori as KategoriForApi

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

    override fun sendTilBeslutter(oppgaveId: Long, behandlendeSaksbehandler: SaksbehandlerFraApi) {
        val saksbehandler = behandlendeSaksbehandler.tilSaksbehandler()
        oppgave(oppgaveId) {
            try {
                sendTilBeslutter(saksbehandler)
            } catch (e: Modellfeil) {
                throw e.tilApiFeil()
            }
        }
    }

    override fun sendIRetur(oppgaveId: Long, besluttendeSaksbehandler: SaksbehandlerFraApi) {
        val saksbehandler = besluttendeSaksbehandler.tilSaksbehandler()
        oppgave(oppgaveId) {
            try {
                sendIRetur(saksbehandler)
            } catch (e: Modellfeil) {
                throw e.tilApiFeil()
            }
        }
    }

    override fun leggPåVent(oppgaveId: Long): TildelingApiDto {
        return oppgave(oppgaveId) {
            val tildeling = try {
                this.leggPåVent()
            } catch (e: Modellfeil) {
                throw e.tilApiFeil()
            }
            tildeling.let {
                TildelingApiDto(it.navn, it.epost, it.oid, it.påVent)
            }
        }
    }

    override fun fjernPåVent(oppgaveId: Long): TildelingApiDto {
        return oppgave(oppgaveId) {
            val tildeling = try {
                this.fjernPåVent()
            } catch (e: Modellfeil) {
                throw e.tilApiFeil()
            }
            tildeling.let {
                TildelingApiDto(it.navn, it.epost, it.oid, it.påVent)
            }
        }
    }

    override fun venterPåSaksbehandler(oppgaveId: Long): Boolean = oppgaveDao.venterPåSaksbehandler(oppgaveId)
    override fun erRiskoppgave(oppgaveId: Long): Boolean = oppgaveDao.erRiskoppgave(oppgaveId)

    override fun oppgaver(saksbehandlerFraApi: SaksbehandlerFraApi): List<OppgaveTilBehandling> {
        val saksbehandler = saksbehandlerFraApi.tilSaksbehandler()
        val egenskaperSaksbehandlerIkkeHarTilgangTil = Egenskap
            .alleTilgangsstyrteEgenskaper
            .filterNot { saksbehandler.harTilgangTil(listOf(it)) }
            .map(Egenskap::toString)

        val oppgaver = oppgaveDao
            .finnOppgaverForVisning(ekskluderEgenskaper = egenskaperSaksbehandlerIkkeHarTilgangTil, saksbehandlerOid = saksbehandler.oid())
            .groupBy { it.egenskaper.tilEgenskaper() }
        return oppgaver.flatMap { it.value }.tilOppgaveTilBehandling()
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
        egenskaper: List<String>,
        hendelseId: UUID
    ) {
        oppgaveDao.opprettOppgave(id, contextId, egenskap, egenskaper, vedtaksperiodeId, utbetalingId)
        GodkjenningsbehovPayload(hendelseId).lagre(opptegnelseDao, oppgaveDao.finnFødselsnummer(id))
    }

    fun oppdater(
        oppgaveId: Long,
        status: String,
        ferdigstiltAvIdent: String?,
        ferdigstiltAvOid: UUID?,
        egenskaper: List<String>,
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
        oppgave.forsøkTildelingVedReservasjon(saksbehandler, settPåVent)
    }

    fun harFerdigstiltOppgave(vedtaksperiodeId: UUID) = oppgaveDao.harFerdigstiltOppgave(vedtaksperiodeId)

    private fun SaksbehandlerFraApi.tilSaksbehandler() = Saksbehandler(
        epostadresse = epost,
        oid = oid,
        navn = navn,
        ident = ident,
        tilgangskontroll = TilgangskontrollørForApi(grupper, tilgangsgrupper),
    )

    private fun List<String>.tilEgenskaper(): List<Egenskap> = this.map { enumValueOf<Egenskap>(it) }

    private fun List<OppgaveFraDatabaseForVisning>.tilOppgaveTilBehandling() = map {
        OppgaveTilBehandling(
            id = it.id.toString(),
            opprettet = it.opprettet.toString(),
            opprinneligSoknadsdato = it.opprinneligSøknadsdato.toString(),
            vedtaksperiodeId = it.vedtaksperiodeId.toString(),
            navn = Personnavn(
                fornavn = it.navn.fornavn,
                etternavn = it.navn.etternavn,
                mellomnavn = it.navn.mellomnavn,
            ),
            aktorId = it.aktørId,
            tildeling = it.tildelt?.let { tildelt ->
                Tildeling(
                    tildelt.navn,
                    tildelt.epostadresse,
                    tildelt.oid.toString(),
                    it.påVent
                )
            },
            egenskaper = it.egenskaper.tilEgenskaper().map { egenskap ->
                Oppgaveegenskap(egenskap.mapToApiEgenskap(), egenskap.kategori.mapToApiKategori())
            }
        )
    }

    private fun Modellfeil.tilApiFeil(): no.nav.helse.spesialist.api.feilhåndtering.Modellfeil {
        return when (this) {
            is no.nav.helse.modell.OppgaveIkkeTildelt -> OppgaveIkkeTildelt(oppgaveId)
            is OppgaveTildeltNoenAndre -> TODO()
            is OppgaveAlleredeSendtBeslutter -> no.nav.helse.spesialist.api.feilhåndtering.OppgaveAlleredeSendtBeslutter(oppgaveId)
            is OppgaveAlleredeSendtIRetur -> no.nav.helse.spesialist.api.feilhåndtering.OppgaveAlleredeSendtIRetur(oppgaveId)
            is OppgaveKreverVurderingAvToSaksbehandlere -> no.nav.helse.spesialist.api.feilhåndtering.OppgaveKreverVurderingAvToSaksbehandlere(oppgaveId)
        }
    }

    private fun Egenskap.mapToApiEgenskap(): EgenskapForApi = when (this) {
        Egenskap.RISK_QA -> EgenskapForApi.RISK_QA
        Egenskap.FORTROLIG_ADRESSE -> EgenskapForApi.FORTROLIG_ADRESSE
        Egenskap.EGEN_ANSATT -> EgenskapForApi.EGEN_ANSATT
        Egenskap.BESLUTTER -> EgenskapForApi.BESLUTTER
        Egenskap.SPESIALSAK -> EgenskapForApi.SPESIALSAK
        Egenskap.REVURDERING -> EgenskapForApi.REVURDERING
        Egenskap.SØKNAD -> EgenskapForApi.SOKNAD
        Egenskap.STIKKPRØVE -> EgenskapForApi.STIKKPROVE
        Egenskap.UTBETALING_TIL_SYKMELDT -> EgenskapForApi.UTBETALING_TIL_SYKMELDT
        Egenskap.DELVIS_REFUSJON -> EgenskapForApi.DELVIS_REFUSJON
        Egenskap.UTBETALING_TIL_ARBEIDSGIVER -> EgenskapForApi.UTBETALING_TIL_ARBEIDSGIVER
        Egenskap.INGEN_UTBETALING -> EgenskapForApi.INGEN_UTBETALING
        Egenskap.HASTER -> EgenskapForApi.HASTER
        Egenskap.RETUR -> EgenskapForApi.RETUR
        Egenskap.FULLMAKT -> EgenskapForApi.FULLMAKT
        Egenskap.VERGEMÅL -> EgenskapForApi.VERGEMAL
        Egenskap.EN_ARBEIDSGIVER -> EgenskapForApi.EN_ARBEIDSGIVER
        Egenskap.FLERE_ARBEIDSGIVERE -> EgenskapForApi.FLERE_ARBEIDSGIVERE
        Egenskap.UTLAND -> EgenskapForApi.UTLAND
        Egenskap.FORLENGELSE -> EgenskapForApi.FORLENGELSE
        Egenskap.FORSTEGANGSBEHANDLING -> EgenskapForApi.FORSTEGANGSBEHANDLING
        Egenskap.INFOTRYGDFORLENGELSE -> EgenskapForApi.INFOTRYGDFORLENGELSE
        Egenskap.OVERGANG_FRA_IT -> EgenskapForApi.OVERGANG_FRA_IT
    }

    private fun Egenskap.Kategori.mapToApiKategori(): KategoriForApi {
        return when (this) {
            Egenskap.Kategori.Mottaker -> KategoriForApi.Mottaker
            Egenskap.Kategori.Inntektskilde -> KategoriForApi.Inntektskilde
            Egenskap.Kategori.Oppgavetype -> KategoriForApi.Oppgavetype
            Egenskap.Kategori.Ukategorisert -> KategoriForApi.Ukategorisert
            Egenskap.Kategori.Periodetype -> KategoriForApi.Periodetype
        }
    }
}
