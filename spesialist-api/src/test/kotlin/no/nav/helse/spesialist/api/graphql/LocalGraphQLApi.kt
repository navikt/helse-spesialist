package no.nav.helse.spesialist.api.graphql

import com.auth0.jwt.JWT
import com.auth0.jwt.impl.JWTParser
import com.auth0.jwt.interfaces.DecodedJWT
import io.ktor.http.ContentType
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.application.install
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.doublereceive.DoubleReceive
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.util.decodeBase64String
import io.mockk.Call
import io.mockk.MockKAnswerScope
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.helse.mediator.IBehandlingsstatistikkMediator
import no.nav.helse.spesialist.api.Avviksvurderinghenter
import no.nav.helse.spesialist.api.Dokumenthåndterer
import no.nav.helse.spesialist.api.Godkjenninghåndterer
import no.nav.helse.spesialist.api.JwtStub
import no.nav.helse.spesialist.api.Personhåndterer
import no.nav.helse.spesialist.api.Saksbehandlerhåndterer
import no.nav.helse.spesialist.api.StansAutomatiskBehandlinghåndterer
import no.nav.helse.spesialist.api.TestApplication
import no.nav.helse.spesialist.api.TestdataGenerator
import no.nav.helse.spesialist.api.Totrinnsvurderinghåndterer
import no.nav.helse.spesialist.api.arbeidsgiver.ArbeidsgiverApiDao
import no.nav.helse.spesialist.api.behandlingsstatistikk.BehandlingsstatistikkResponse
import no.nav.helse.spesialist.api.behandlingsstatistikk.Statistikk
import no.nav.helse.spesialist.api.egenAnsatt.EgenAnsattApiDao
import no.nav.helse.spesialist.api.graphql.schema.Adressebeskyttelse
import no.nav.helse.spesialist.api.graphql.schema.AntallOppgaver
import no.nav.helse.spesialist.api.graphql.schema.Avslag
import no.nav.helse.spesialist.api.graphql.schema.BehandledeOppgaver
import no.nav.helse.spesialist.api.graphql.schema.BehandletOppgave
import no.nav.helse.spesialist.api.graphql.schema.Egenskap
import no.nav.helse.spesialist.api.graphql.schema.Filtrering
import no.nav.helse.spesialist.api.graphql.schema.Kategori
import no.nav.helse.spesialist.api.graphql.schema.Kjonn
import no.nav.helse.spesialist.api.graphql.schema.OppgaveTilBehandling
import no.nav.helse.spesialist.api.graphql.schema.Oppgaveegenskap
import no.nav.helse.spesialist.api.graphql.schema.OppgaverTilBehandling
import no.nav.helse.spesialist.api.graphql.schema.Oppgavesortering
import no.nav.helse.spesialist.api.graphql.schema.Opptegnelse
import no.nav.helse.spesialist.api.graphql.schema.PaVent
import no.nav.helse.spesialist.api.graphql.schema.Personinfo
import no.nav.helse.spesialist.api.graphql.schema.Reservasjon
import no.nav.helse.spesialist.api.graphql.schema.Sorteringsnokkel
import no.nav.helse.spesialist.api.graphql.schema.Tildeling
import no.nav.helse.spesialist.api.notat.NotatDao
import no.nav.helse.spesialist.api.notat.NotatMediator
import no.nav.helse.spesialist.api.objectMapper
import no.nav.helse.spesialist.api.oppgave.OppgaveApiDao
import no.nav.helse.spesialist.api.oppgave.Oppgavehåndterer
import no.nav.helse.spesialist.api.overstyring.OverstyringApiDao
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.spesialist.api.person.PersonApiDao
import no.nav.helse.spesialist.api.påvent.PåVentApiDao
import no.nav.helse.spesialist.api.reservasjon.ReservasjonClient
import no.nav.helse.spesialist.api.risikovurdering.RisikovurderingApiDao
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.AvmeldOppgave
import no.nav.helse.spesialist.api.saksbehandler.handlinger.HandlingFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.TildelOppgave
import no.nav.helse.spesialist.api.snapshot.SnapshotApiDao
import no.nav.helse.spesialist.api.snapshot.SnapshotMediator
import no.nav.helse.spesialist.api.tildeling.TildelingDao
import no.nav.helse.spesialist.api.totrinnsvurdering.TotrinnsvurderingApiDao
import no.nav.helse.spesialist.api.varsel.ApiVarselRepository
import no.nav.helse.spesialist.api.vedtak.GodkjenningDto
import no.nav.helse.spesialist.api.vedtaksperiode.EnhetDto
import no.nav.helse.spesialist.api.websockets.webSocketsApi
import no.nav.helse.spleis.graphql.enums.GraphQLInntektstype
import no.nav.helse.spleis.graphql.enums.GraphQLPeriodetilstand
import no.nav.helse.spleis.graphql.enums.GraphQLPeriodetype
import no.nav.helse.spleis.graphql.enums.GraphQLUtbetalingstatus
import no.nav.helse.spleis.graphql.enums.Utbetalingtype
import no.nav.helse.spleis.graphql.hentsnapshot.Alder
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLArbeidsgiver
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLBeregnetPeriode
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLGenerasjon
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLOppdrag
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLPeriodevilkar
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLPerson
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLSimulering
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLSimuleringsdetaljer
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLSimuleringsperiode
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLSimuleringsutbetaling
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLUtbetaling
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLVurdering
import no.nav.helse.spleis.graphql.hentsnapshot.Sykepengedager
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

fun main() =
    runBlocking {
        val jwtStub = JwtStub()
        val clientId = "client_id"
        val issuer = "https://jwt-provider-domain"
        val epostadresse = "dev@nav.no"

        fun getJwt(
            jwtStub: JwtStub,
            epostadresse: String,
            clientId: String,
            issuer: String,
        ) = jwtStub.getToken(
            groups = emptyList(),
            oid = UUID.fromString("4577332e-801a-4c13-8a71-39f12b8abfa3").toString(),
            navn = "Utvikler, Lokal",
            epostadresse = epostadresse,
            clientId = clientId,
            issuer = issuer,
        )

        TestApplication(4321).start { dataSource ->
            val snapshotApiDao = mockk<SnapshotApiDao>(relaxed = true)
            val personApiDao = mockk<PersonApiDao>(relaxed = true)
            val egenAnsattApiDao = mockk<EgenAnsattApiDao>(relaxed = true)
            val tildelingDao = TildelingDao(dataSource)
            val arbeidsgiverApiDao = ArbeidsgiverApiDao(dataSource)
            val overstyringApiDao = OverstyringApiDao(dataSource)
            val risikovurderingApiDao = RisikovurderingApiDao(dataSource)
            val apiVarselRepository = mockk<ApiVarselRepository>(relaxed = true)
            val oppgaveApiDao = mockk<OppgaveApiDao>(relaxed = true)
            val periodehistorikkDao = mockk<PeriodehistorikkDao>(relaxed = true)
            val notatDao = mockk<NotatDao>(relaxed = true)
            val totrinnsvurderingApiDao = mockk<TotrinnsvurderingApiDao>(relaxed = true)
            val påVentApiDao = mockk<PåVentApiDao>(relaxed = true)
            val reservasjonClient = mockk<ReservasjonClient>(relaxed = true)
            val avviksvurderinghenter = mockk<Avviksvurderinghenter>(relaxed = true)
            val behandlingsstatistikkMediator = mockk<IBehandlingsstatistikkMediator>(relaxed = true)
            val notatMediator = mockk<NotatMediator>(relaxed = true)
            val totrinnsvurderinghåndterer = mockk<Totrinnsvurderinghåndterer>(relaxed = true)
            val godkjenninghåndterer = mockk<Godkjenninghåndterer>(relaxed = true)
            val personhåndterer = mockk<Personhåndterer>(relaxed = true)
            val dokumenthåndterer = mockk<Dokumenthåndterer>(relaxed = true)
            val stansAutomatiskBehandlinghåndterer = mockk<StansAutomatiskBehandlinghåndterer>(relaxed = true)

            every { snapshotApiDao.utdatert(any()) } returns false
            every { snapshotApiDao.hentSnapshotMedMetadata(any()) } answers withDelay(800) { (enPersoninfo() to enPerson()) }
            every { personApiDao.personHarAdressebeskyttelse(any(), any()) } returns false
            every {
                personApiDao.personHarAdressebeskyttelse(
                    any(),
                    no.nav.helse.spesialist.api.person.Adressebeskyttelse.Ugradert,
                )
            } returns true
            every { personApiDao.finnesPersonMedFødselsnummer(any()) } returns true
            every { personApiDao.finnEnhet(any()) } returns EnhetDto("1234", "Bømlo")
            every { personApiDao.finnFødselsnummer(isNull(inverse = true)) } returns enPerson().fodselsnummer
            every { personApiDao.spesialistHarPersonKlarForVisningISpeil(any()) } returns true
            every { personApiDao.finnInfotrygdutbetalinger(any()) } returns "[]"
            every { totrinnsvurderingApiDao.hentAktiv(any()) } returns
                TotrinnsvurderingApiDao.TotrinnsvurderingDto(
                    opprettet = LocalDateTime.now(),
                    oppdatert = LocalDateTime.now(),
                    saksbehandler = UUID.randomUUID(),
                    beslutter = null,
                    erRetur = false,
                    utbetalingIdRef = 42,
                    vedtaksperiodeId = UUID.randomUUID(),
                )
            every { påVentApiDao.hentAktivPåVent(any()) } returns
                PaVent(
                    frist = LocalDate.now(),
                    begrunnelse = null,
                    oid = UUID.randomUUID(),
                )
            coEvery { reservasjonClient.hentReservasjonsstatus(any()) } answers
                withDelay(800) {
                    Reservasjon(kanVarsles = true, reservert = false)
                }
            every { behandlingsstatistikkMediator.getBehandlingsstatistikk() } returns
                BehandlingsstatistikkResponse(
                    enArbeidsgiver = Statistikk(485, 104, 789),
                    flereArbeidsgivere = Statistikk(254, 58, 301),
                    forstegangsbehandling = Statistikk(201, 75, 405),
                    forlengelser = Statistikk(538, 87, 685),
                    forlengelseIt = Statistikk(2, 10, 0),
                    utbetalingTilArbeidsgiver = Statistikk(123, 12, 1),
                    utbetalingTilSykmeldt = Statistikk(0, 21, 63),
                    faresignaler = Statistikk(0, 12, 2),
                    fortroligAdresse = Statistikk(0, 1, 0),
                    stikkprover = Statistikk(0, 10, 6),
                    revurdering = Statistikk(0, 105, 204),
                    delvisRefusjon = Statistikk(0, 64, 64),
                    beslutter = Statistikk(0, 150, 204),
                    egenAnsatt = Statistikk(0, 3, 10),
                    antallAnnulleringer = 0,
                )

            install(ContentNegotiation) {
                register(
                    ContentType.Application.Json,
                    JacksonConverter(objectMapper),
                )
            }

            install(CallLogging)
            install(DoubleReceive)
            install(WebSockets)
            authentication {
                provider("oidc") {
                    authenticate { authenticationContext ->
                        val jwt = getJwt(jwtStub, epostadresse, clientId, issuer)
                        val decodedJwt = JWT().decodeJwt(jwt)
                        authenticationContext.principal(decodedJwt.toJwtPrincipal())
                    }
                }
            }

            val randomOppgaver = MutableList(1000) { TestdataGenerator.oppgave() }
            val randomBehandledeOppgaver = MutableList(32) { TestdataGenerator.behandletOppgave() }

            graphQLApi(
                env = emptyMap(),
                personApiDao = personApiDao,
                egenAnsattApiDao = egenAnsattApiDao,
                tildelingDao = tildelingDao,
                arbeidsgiverApiDao = arbeidsgiverApiDao,
                overstyringApiDao = overstyringApiDao,
                risikovurderingApiDao = risikovurderingApiDao,
                varselRepository = apiVarselRepository,
                oppgaveApiDao = oppgaveApiDao,
                periodehistorikkDao = periodehistorikkDao,
                notatDao = notatDao,
                totrinnsvurderingApiDao = totrinnsvurderingApiDao,
                påVentApiDao = påVentApiDao,
                reservasjonClient = reservasjonClient,
                avviksvurderinghenter = avviksvurderinghenter,
                skjermedePersonerGruppeId = UUID.randomUUID(),
                kode7Saksbehandlergruppe = UUID.randomUUID(),
                beslutterGruppeId = UUID.randomUUID(),
                snapshotMediator = SnapshotMediator(snapshotApiDao, mockk(relaxed = true)),
                behandlingsstatistikkMediator = behandlingsstatistikkMediator,
                notatMediator = notatMediator,
                saksbehandlerhåndtererProvider = { SneakySaksbehandlerhåndterer(randomOppgaver) },
                oppgavehåndtererProvider = {
                    SneakyOppgaveHåndterer(
                        randomOppgaver = randomOppgaver,
                        randomBehandledeOppgaver = randomBehandledeOppgaver,
                    )
                },
                totrinnsvurderinghåndterer = totrinnsvurderinghåndterer,
                godkjenninghåndtererProvider = { godkjenninghåndterer },
                personhåndtererProvider = { personhåndterer },
                dokumenthåndtererProvider = { dokumenthåndterer },
                stansAutomatiskBehandlinghåndterer = stansAutomatiskBehandlinghåndterer,
            )
            routing {
                webSocketsApi()
            }
        }
    }

private class SneakySaksbehandlerhåndterer(
    private val randomOppgaver: MutableList<OppgaveTilBehandling>,
) : Saksbehandlerhåndterer {
    val mock = mockk<Saksbehandlerhåndterer>(relaxed = true)

    override fun <T : HandlingFraApi> håndter(
        handlingFraApi: T,
        saksbehandlerFraApi: SaksbehandlerFraApi,
    ) {
        when (handlingFraApi) {
            is TildelOppgave -> tildelOppgave(randomOppgaver, handlingFraApi, saksbehandlerFraApi)
            is AvmeldOppgave -> avmeldOppgave(randomOppgaver, handlingFraApi)
        }
    }

    override fun håndter(
        godkjenning: GodkjenningDto,
        behandlingId: UUID,
        saksbehandlerFraApi: SaksbehandlerFraApi,
    ) {
        return mock.håndter(godkjenning, behandlingId, saksbehandlerFraApi)
    }

    override fun opprettAbonnement(
        saksbehandlerFraApi: SaksbehandlerFraApi,
        personidentifikator: String,
    ) {
        return opprettAbonnement(saksbehandlerFraApi, personidentifikator)
    }

    override fun hentAbonnerteOpptegnelser(
        saksbehandlerFraApi: SaksbehandlerFraApi,
        sisteSekvensId: Int,
    ): List<Opptegnelse> {
        return mock.hentAbonnerteOpptegnelser(saksbehandlerFraApi, sisteSekvensId)
    }

    override fun hentAvslag(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
    ): Set<Avslag> {
        return mock.hentAvslag(vedtaksperiodeId, utbetalingId)
    }

    override fun håndterAvslag(
        oppgaveId: Long,
        saksbehandlerFraApi: SaksbehandlerFraApi,
        avslag: no.nav.helse.spesialist.api.graphql.mutation.Avslag,
    ) {
        return mock.håndterAvslag(oppgaveId, saksbehandlerFraApi, avslag)
    }

    override fun hentAbonnerteOpptegnelser(saksbehandlerFraApi: SaksbehandlerFraApi): List<Opptegnelse> {
        return mock.hentAbonnerteOpptegnelser(saksbehandlerFraApi)
    }

    override fun håndterTotrinnsvurdering(oppgavereferanse: Long) {
        return mock.håndterTotrinnsvurdering(oppgavereferanse)
    }
}

private class SneakyOppgaveHåndterer(
    private val randomOppgaver: List<OppgaveTilBehandling>,
    private val randomBehandledeOppgaver: List<BehandletOppgave>,
) : Oppgavehåndterer {
    val mock = mockk<Oppgavehåndterer>(relaxed = true)

    override fun sendTilBeslutter(
        oppgaveId: Long,
        behandlendeSaksbehandler: SaksbehandlerFraApi,
    ) {
        return mock.sendTilBeslutter(oppgaveId, behandlendeSaksbehandler)
    }

    override fun sendIRetur(
        oppgaveId: Long,
        besluttendeSaksbehandler: SaksbehandlerFraApi,
    ) {
        return mock.sendIRetur(oppgaveId, besluttendeSaksbehandler)
    }

    override fun endretEgenAnsattStatus(
        erEgenAnsatt: Boolean,
        fødselsnummer: String,
    ) {}

    override fun venterPåSaksbehandler(oppgaveId: Long): Boolean {
        return mock.venterPåSaksbehandler(oppgaveId)
    }

    override fun oppgaver(
        saksbehandlerFraApi: SaksbehandlerFraApi,
        offset: Int,
        limit: Int,
        sortering: List<Oppgavesortering>,
        filtrering: Filtrering,
    ): OppgaverTilBehandling {
        val oppgaver = randomOppgaver.filtered(filtrering, saksbehandlerFraApi).sorted(sortering)
        return OppgaverTilBehandling(
            oppgaver = oppgaver.drop(offset).take(limit),
            totaltAntallOppgaver = oppgaver.size,
        )
    }

    override fun antallOppgaver(saksbehandlerFraApi: SaksbehandlerFraApi): AntallOppgaver {
        return AntallOppgaver(
            antallMineSaker = randomOppgaver.filter { it.erTildelt(saksbehandlerFraApi) }.size,
            antallMineSakerPaVent = randomOppgaver.filter { it.erTildeltOgPåVent(saksbehandlerFraApi) }.size,
        )
    }

    override fun behandledeOppgaver(
        saksbehandlerFraApi: SaksbehandlerFraApi,
        offset: Int,
        limit: Int,
    ): BehandledeOppgaver {
        return BehandledeOppgaver(
            oppgaver = randomBehandledeOppgaver.drop(offset).take(limit),
            totaltAntallOppgaver = randomBehandledeOppgaver.size,
        )
    }

    override fun hentEgenskaper(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
    ): List<Oppgaveegenskap> {
        return mock.hentEgenskaper(vedtaksperiodeId, utbetalingId)
    }
}

private fun tildelOppgave(
    randomOppgaver: MutableList<OppgaveTilBehandling>,
    handlingFraApi: TildelOppgave,
    saksbehandlerFraApi: SaksbehandlerFraApi,
) {
    val oppgave = randomOppgaver.find { it.id.toLong() == handlingFraApi.oppgaveId } ?: return
    randomOppgaver.remove(oppgave)
    randomOppgaver.add(
        oppgave.copy(
            tildeling =
                Tildeling(
                    navn = saksbehandlerFraApi.navn,
                    epost = saksbehandlerFraApi.epost,
                    oid = saksbehandlerFraApi.oid,
                ),
        ),
    )
}

private fun avmeldOppgave(
    randomOppgaver: MutableList<OppgaveTilBehandling>,
    handlingFraApi: AvmeldOppgave,
) {
    val oppgave = randomOppgaver.find { it.id.toLong() == handlingFraApi.oppgaveId } ?: return
    randomOppgaver.remove(oppgave)
    randomOppgaver.add(oppgave.copy(tildeling = null))
}

private fun List<OppgaveTilBehandling>.filtered(
    filtrering: Filtrering,
    saksbehandlerFraApi: SaksbehandlerFraApi,
): List<OppgaveTilBehandling> =
    this
        .asSequence()
        .filter { oppgave -> if (filtrering.egneSaker) oppgave.erTildelt(saksbehandlerFraApi) else true }
        .filter { oppgave -> if (filtrering.egneSakerPaVent) oppgave.erTildeltOgPåVent(saksbehandlerFraApi) else true }
        .filter {
                oppgave ->
            if (filtrering.ingenUkategoriserteEgenskaper) !oppgave.egenskaper.any { it.kategori == Kategori.Ukategorisert } else true
        }
        .filter { oppgave -> filtrering.egenskaper.isEmpty() || oppgave.egenskaper.containsAll(filtrering.egenskaper) }
        .filter {
                oppgave ->
            filtrering.tildelt == null || if (filtrering.tildelt == true) oppgave.tildeling != null else oppgave.tildeling == null
        }
        .toList()

private fun List<OppgaveTilBehandling>.sorted(sortering: List<Oppgavesortering>): List<OppgaveTilBehandling> =
    when (if (sortering.isEmpty()) null else sortering.first().nokkel) {
        Sorteringsnokkel.TILDELT_TIL ->
            if (sortering.first().stigende) {
                this.sortedBy {
                    it.tildeling?.navn
                }
            } else {
                this.sortedByDescending { it.tildeling?.navn }
            }
        Sorteringsnokkel.OPPRETTET -> if (sortering.first().stigende) this.sortedBy { it.opprettet } else this.sortedByDescending { it.opprettet }
        Sorteringsnokkel.SOKNAD_MOTTATT ->
            if (sortering.first().stigende) {
                this.sortedBy {
                    it.opprinneligSoknadsdato
                }
            } else {
                this.sortedByDescending { it.opprinneligSoknadsdato }
            }
        Sorteringsnokkel.TIDSFRIST -> if (sortering.first().stigende) this.sortedBy { it.tidsfrist } else this.sortedByDescending { it.tidsfrist }
        null -> this
    }

private fun OppgaveTilBehandling.erTildelt(saksbehandlerFraApi: SaksbehandlerFraApi): Boolean =
    !((this.tildeling == null || this.egenskaper.any { it.egenskap == Egenskap.PA_VENT }) || this.tildeling?.oid != saksbehandlerFraApi.oid)

private fun OppgaveTilBehandling.erTildeltOgPåVent(saksbehandlerFraApi: SaksbehandlerFraApi): Boolean =
    !((this.tildeling == null || this.egenskaper.none { it.egenskap == Egenskap.PA_VENT }) || this.tildeling?.oid != saksbehandlerFraApi.oid)

private fun DecodedJWT.toJwtPrincipal() = JWTPrincipal(JWTParser().parsePayload(payload.decodeBase64String()))

private fun enPersoninfo() =
    Personinfo(
        fornavn = "Luke",
        mellomnavn = null,
        etternavn = "Skywalker",
        fodselsdato = LocalDate.parse("2000-01-01"),
        kjonn = Kjonn.Kvinne,
        adressebeskyttelse = Adressebeskyttelse.Ugradert,
        reservasjon = null, // Denne hentes runtime ved hjelp av et kall til KRR
        unntattFraAutomatisering = null,
    )

private fun enPeriode() =
    GraphQLBeregnetPeriode(
        erForkastet = false,
        fom = LocalDate.parse("2020-01-01"),
        tom = LocalDate.parse("2020-01-31"),
        inntektstype = GraphQLInntektstype.ENARBEIDSGIVER,
        opprettet = LocalDateTime.now(),
        periodetype = GraphQLPeriodetype.FORSTEGANGSBEHANDLING,
        tidslinje = emptyList(),
        vedtaksperiodeId = UUID.randomUUID(),
        beregningId = UUID.randomUUID(),
        forbrukteSykedager = 10,
        gjenstaendeSykedager = 270,
        hendelser = emptyList(),
        maksdato = LocalDate.parse("2021-01-01"),
        periodevilkar =
            GraphQLPeriodevilkar(
                Alder(
                    alderSisteSykedag = 40,
                    oppfylt = true,
                ),
                sykepengedager =
                    Sykepengedager(
                        forbrukteSykedager = 10,
                        gjenstaendeSykedager = 270,
                        maksdato = LocalDate.parse("2021-01-01"),
                        oppfylt = true,
                        skjaeringstidspunkt = LocalDate.parse("2020-01-01"),
                    ),
            ),
        skjaeringstidspunkt = LocalDate.parse("2020-01-01"),
        utbetaling =
            GraphQLUtbetaling(
                id = UUID.randomUUID(),
                arbeidsgiverFagsystemId = "EN-ARBEIDSGIVERFAGSYSTEMID",
                arbeidsgiverNettoBelop = 30000,
                personFagsystemId = "EN-PERSONFAGSYSTEMID",
                personNettoBelop = 0,
                statusEnum = GraphQLUtbetalingstatus.GODKJENT,
                typeEnum = Utbetalingtype.UTBETALING,
                vurdering =
                    GraphQLVurdering(
                        automatisk = false,
                        godkjent = true,
                        ident = "AB123456",
                        tidsstempel = LocalDateTime.now(),
                    ),
                personoppdrag =
                    GraphQLOppdrag(
                        fagsystemId = "EN-PERSONFAGSYSTEMID",
                        tidsstempel = LocalDateTime.parse("2021-01-01"),
                        utbetalingslinjer = emptyList(),
                        simulering = null,
                    ),
                arbeidsgiveroppdrag =
                    GraphQLOppdrag(
                        fagsystemId = "EN-ARBEIDSGIVERFAGSYSTEMID",
                        tidsstempel = LocalDateTime.parse("2021-01-01"),
                        utbetalingslinjer = emptyList(),
                        simulering =
                            GraphQLSimulering(
                                totalbelop = 30000,
                                perioder =
                                    listOf(
                                        GraphQLSimuleringsperiode(
                                            fom = LocalDate.parse("2020-01-01"),
                                            tom = LocalDate.parse("2020-01-31"),
                                            utbetalinger =
                                                listOf(
                                                    GraphQLSimuleringsutbetaling(
                                                        utbetalesTilNavn = "EN-PERSON",
                                                        utbetalesTilId = "EN-PERSONID",
                                                        feilkonto = false,
                                                        forfall = LocalDate.parse("2022-01-01"),
                                                        detaljer =
                                                            listOf(
                                                                GraphQLSimuleringsdetaljer(
                                                                    belop = 30000,
                                                                    antallSats = 1,
                                                                    faktiskFom = LocalDate.parse("2020-01-01"),
                                                                    faktiskTom = LocalDate.parse("2020-01-31"),
                                                                    klassekode = "EN-KLASSEKODE",
                                                                    klassekodeBeskrivelse = "EN-KLASSEKODEBESKRIVELSE",
                                                                    konto = "EN-KONTO",
                                                                    refunderesOrgNr = "ET-ORGNR",
                                                                    sats = 30000.0,
                                                                    tilbakeforing = false,
                                                                    typeSats = "EN-TYPESATS",
                                                                    uforegrad = 100,
                                                                    utbetalingstype = "EN-UTBETALINGSTYPE",
                                                                ),
                                                            ),
                                                    ),
                                                ),
                                        ),
                                    ),
                            ),
                    ),
            ),
        vilkarsgrunnlagId = null,
        periodetilstand = GraphQLPeriodetilstand.TILGODKJENNING,
        behandlingId = UUID.randomUUID(),
    )

private fun enGenerasjon() =
    GraphQLGenerasjon(
        id = UUID.randomUUID(),
        perioder = listOf(enPeriode()),
    )

private fun enArbeidsgiver(organisasjonsnummer: String = "987654321") =
    GraphQLArbeidsgiver(
        organisasjonsnummer = organisasjonsnummer,
        ghostPerioder = emptyList(),
        generasjoner = listOf(enGenerasjon()),
    )

private fun enPerson() =
    GraphQLPerson(
        aktorId = "jedi-master",
        arbeidsgivere = listOf(enArbeidsgiver()),
        dodsdato = null,
        fodselsnummer = "01017012345",
        versjon = 1,
        vilkarsgrunnlag = emptyList(),
    )

fun <T, B> withDelay(
    millis: Long,
    block: () -> T,
): MockKAnswerScope<T, B>.(Call) -> T =
    {
        runBlocking { delay(millis) }
        block()
    }
