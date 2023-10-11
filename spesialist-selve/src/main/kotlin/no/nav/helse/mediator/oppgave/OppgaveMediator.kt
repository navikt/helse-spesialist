package no.nav.helse.mediator.oppgave

import java.sql.SQLException
import java.util.UUID
import no.nav.helse.Tilgangsgrupper
import no.nav.helse.db.EgenskapForDatabase
import no.nav.helse.db.OppgaveFraDatabaseForVisning
import no.nav.helse.db.OppgavesorteringForDatabase
import no.nav.helse.db.ReservasjonDao
import no.nav.helse.db.SaksbehandlerRepository
import no.nav.helse.db.SorteringsnøkkelForDatabase
import no.nav.helse.db.TildelingDao
import no.nav.helse.db.TotrinnsvurderingFraDatabase
import no.nav.helse.db.TotrinnsvurderingRepository
import no.nav.helse.mediator.TilgangskontrollørForApi
import no.nav.helse.mediator.saksbehandler.SaksbehandlerMapper.tilApiversjon
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
import no.nav.helse.spesialist.api.graphql.schema.AntallArbeidsforhold
import no.nav.helse.spesialist.api.graphql.schema.Mottaker
import no.nav.helse.spesialist.api.graphql.schema.OppgaveTilBehandling
import no.nav.helse.spesialist.api.graphql.schema.Oppgaveegenskap
import no.nav.helse.spesialist.api.graphql.schema.Oppgavesortering
import no.nav.helse.spesialist.api.graphql.schema.Oppgavetype
import no.nav.helse.spesialist.api.graphql.schema.Periodetype
import no.nav.helse.spesialist.api.graphql.schema.Personnavn
import no.nav.helse.spesialist.api.graphql.schema.Sorteringsnokkel
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

    override fun fjernPåVent(oppgaveId: Long) {
        oppgave(oppgaveId) {
            try {
                this.fjernPåVent()
            } catch (e: Modellfeil) {
                throw e.tilApiFeil()
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
        return oppgaver.tilOppgaveTilBehandling()
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

    private fun List<EgenskapForDatabase>.tilEgenskaper(): List<Egenskap> = this.map { it.mapTilEgenskap() }

    private fun List<OppgaveFraDatabaseForVisning>.tilOppgaveTilBehandling() = map {
        val egenskaper = it.egenskaper.tilEgenskaper()
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
            egenskaper = egenskaper.map { egenskap ->
                Oppgaveegenskap(egenskap.mapToApiEgenskap(), egenskap.kategori.mapToApiKategori())
            },
            periodetype = egenskaper.periodetype(),
            oppgavetype = egenskaper.oppgavetype(),
            mottaker = egenskaper.mottaker(),
            antallArbeidsforhold = egenskaper.antallArbeidsforhold(),
        )
    }

    private fun List<Oppgavesortering>.tilOppgavesorteringForDatabase() = map {
        when (it.nokkel) {
            Sorteringsnokkel.TILDELT_TIL -> OppgavesorteringForDatabase(SorteringsnøkkelForDatabase.TILDELT_TIL, it.stigende)
            Sorteringsnokkel.OPPRETTET -> OppgavesorteringForDatabase(SorteringsnøkkelForDatabase.OPPRETTET, it.stigende)
            Sorteringsnokkel.SOKNAD_MOTTATT -> OppgavesorteringForDatabase(SorteringsnøkkelForDatabase.SØKNAD_MOTTATT, it.stigende)
        }
    }

    private fun Modellfeil.tilApiFeil(): no.nav.helse.spesialist.api.feilhåndtering.Modellfeil {
        return when (this) {
            is no.nav.helse.modell.OppgaveIkkeTildelt -> OppgaveIkkeTildelt(oppgaveId)
            is OppgaveTildeltNoenAndre -> {
                val (oid, navn, epost) = this.saksbehandler.tilApiversjon()
                no.nav.helse.spesialist.api.feilhåndtering.OppgaveTildeltNoenAndre(TildelingApiDto(navn, epost, oid, påVent))
            }
            is OppgaveAlleredeSendtBeslutter -> no.nav.helse.spesialist.api.feilhåndtering.OppgaveAlleredeSendtBeslutter(oppgaveId)
            is OppgaveAlleredeSendtIRetur -> no.nav.helse.spesialist.api.feilhåndtering.OppgaveAlleredeSendtIRetur(oppgaveId)
            is OppgaveKreverVurderingAvToSaksbehandlere -> no.nav.helse.spesialist.api.feilhåndtering.OppgaveKreverVurderingAvToSaksbehandlere(oppgaveId)
        }
    }

    private fun List<Egenskap>.periodetype(): Periodetype {
        val egenskap = single { egenskap -> egenskap.kategori ==  Egenskap.Kategori.Periodetype }
        return when (egenskap) {
            Egenskap.FORSTEGANGSBEHANDLING -> Periodetype.FORSTEGANGSBEHANDLING
            Egenskap.FORLENGELSE -> Periodetype.FORLENGELSE
            Egenskap.INFOTRYGDFORLENGELSE -> Periodetype.INFOTRYGDFORLENGELSE
            Egenskap.OVERGANG_FRA_IT -> Periodetype.OVERGANG_FRA_IT
            else -> throw IllegalArgumentException("Kunne ikke mappe egenskap til periodetype")
        }
    }

    private fun List<Egenskap>.oppgavetype(): Oppgavetype {
        val egenskap = single { egenskap -> egenskap.kategori == Egenskap.Kategori.Oppgavetype }
        return when (egenskap) {
            Egenskap.SØKNAD -> Oppgavetype.SOKNAD
            Egenskap.REVURDERING -> Oppgavetype.REVURDERING
            else -> throw IllegalArgumentException("Kunne ikke mappe egenskap til periodetype")
        }
    }

    private fun List<Egenskap>.mottaker(): Mottaker {
        val egenskap = single { egenskap -> egenskap.kategori ==  Egenskap.Kategori.Mottaker }
        return when (egenskap) {
            Egenskap.UTBETALING_TIL_SYKMELDT -> Mottaker.SYKMELDT
            Egenskap.UTBETALING_TIL_ARBEIDSGIVER -> Mottaker.ARBEIDSGIVER
            Egenskap.DELVIS_REFUSJON -> Mottaker.BEGGE
            Egenskap.INGEN_UTBETALING -> Mottaker.INGEN
            else -> throw IllegalArgumentException("Kunne ikke mappe egenskap til periodetype")
        }
    }

    private fun List<Egenskap>.antallArbeidsforhold(): AntallArbeidsforhold {
        val egenskap = single { egenskap -> egenskap.kategori ==  Egenskap.Kategori.Inntektskilde }
        return when (egenskap) {
            Egenskap.EN_ARBEIDSGIVER -> AntallArbeidsforhold.ET_ARBEIDSFORHOLD
            Egenskap.FLERE_ARBEIDSGIVERE -> AntallArbeidsforhold.FLERE_ARBEIDSFORHOLD
            else -> throw IllegalArgumentException("Kunne ikke mappe egenskap til periodetype")
        }
    }

    private fun Egenskap.mapToApiEgenskap(): EgenskapForApi = when (this) {
        Egenskap.RISK_QA -> EgenskapForApi.RISK_QA
        Egenskap.FORTROLIG_ADRESSE -> EgenskapForApi.FORTROLIG_ADRESSE
        Egenskap.STRENGT_FORTROLIG_ADRESSE -> EgenskapForApi.STRENGT_FORTROLIG_ADRESSE
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

    private fun EgenskapForDatabase.mapTilEgenskap(): Egenskap = when (this) {
        EgenskapForDatabase.RISK_QA -> Egenskap.RISK_QA
        EgenskapForDatabase.FORTROLIG_ADRESSE -> Egenskap.FORTROLIG_ADRESSE
        EgenskapForDatabase.STRENGT_FORTROLIG_ADRESSE -> Egenskap.STRENGT_FORTROLIG_ADRESSE
        EgenskapForDatabase.EGEN_ANSATT -> Egenskap.EGEN_ANSATT
        EgenskapForDatabase.BESLUTTER -> Egenskap.BESLUTTER
        EgenskapForDatabase.SPESIALSAK -> Egenskap.SPESIALSAK
        EgenskapForDatabase.REVURDERING -> Egenskap.REVURDERING
        EgenskapForDatabase.SØKNAD -> Egenskap.SØKNAD
        EgenskapForDatabase.STIKKPRØVE -> Egenskap.STIKKPRØVE
        EgenskapForDatabase.UTBETALING_TIL_SYKMELDT -> Egenskap.UTBETALING_TIL_SYKMELDT
        EgenskapForDatabase.DELVIS_REFUSJON -> Egenskap.DELVIS_REFUSJON
        EgenskapForDatabase.UTBETALING_TIL_ARBEIDSGIVER -> Egenskap.UTBETALING_TIL_ARBEIDSGIVER
        EgenskapForDatabase.INGEN_UTBETALING -> Egenskap.INGEN_UTBETALING
        EgenskapForDatabase.HASTER -> Egenskap.HASTER
        EgenskapForDatabase.RETUR -> Egenskap.RETUR
        EgenskapForDatabase.FULLMAKT -> Egenskap.FULLMAKT
        EgenskapForDatabase.VERGEMÅL -> Egenskap.VERGEMÅL
        EgenskapForDatabase.EN_ARBEIDSGIVER -> Egenskap.EN_ARBEIDSGIVER
        EgenskapForDatabase.FLERE_ARBEIDSGIVERE -> Egenskap.FLERE_ARBEIDSGIVERE
        EgenskapForDatabase.UTLAND -> Egenskap.UTLAND
        EgenskapForDatabase.FORLENGELSE -> Egenskap.FORLENGELSE
        EgenskapForDatabase.FORSTEGANGSBEHANDLING -> Egenskap.FORSTEGANGSBEHANDLING
        EgenskapForDatabase.INFOTRYGDFORLENGELSE -> Egenskap.INFOTRYGDFORLENGELSE
        EgenskapForDatabase.OVERGANG_FRA_IT -> Egenskap.OVERGANG_FRA_IT
    }

    private fun Egenskap.Kategori.mapToApiKategori(): KategoriForApi = when (this) {
        Egenskap.Kategori.Mottaker -> KategoriForApi.Mottaker
        Egenskap.Kategori.Inntektskilde -> KategoriForApi.Inntektskilde
        Egenskap.Kategori.Oppgavetype -> KategoriForApi.Oppgavetype
        Egenskap.Kategori.Ukategorisert -> KategoriForApi.Ukategorisert
        Egenskap.Kategori.Periodetype -> KategoriForApi.Periodetype
    }
}
