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
import io.ktor.util.decodeBase64String
import io.mockk.Call
import io.mockk.MockKAnswerScope
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.helse.spesialist.api.Godkjenninghåndterer
import no.nav.helse.spesialist.api.JwtStub
import no.nav.helse.spesialist.api.Saksbehandlerhåndterer
import no.nav.helse.spesialist.api.TestApplication
import no.nav.helse.spesialist.api.Totrinnsvurderinghåndterer
import no.nav.helse.spesialist.api.arbeidsgiver.ArbeidsgiverApiDao
import no.nav.helse.spesialist.api.behandlingsstatistikk.BehandlingsstatistikkMediator
import no.nav.helse.spesialist.api.behandlingsstatistikk.BehandlingsstatistikkResponse
import no.nav.helse.spesialist.api.behandlingsstatistikk.Statistikk
import no.nav.helse.spesialist.api.egenAnsatt.EgenAnsattApiDao
import no.nav.helse.spesialist.api.graphql.schema.Adressebeskyttelse
import no.nav.helse.spesialist.api.graphql.schema.Boenhet
import no.nav.helse.spesialist.api.graphql.schema.Kjonn
import no.nav.helse.spesialist.api.graphql.schema.Mottaker
import no.nav.helse.spesialist.api.graphql.schema.OppgaveForOversiktsvisning
import no.nav.helse.spesialist.api.graphql.schema.Personinfo
import no.nav.helse.spesialist.api.graphql.schema.Personnavn
import no.nav.helse.spesialist.api.graphql.schema.Reservasjon
import no.nav.helse.spesialist.api.graphql.schema.Tildeling
import no.nav.helse.spesialist.api.graphql.schema.Totrinnsvurdering
import no.nav.helse.spesialist.api.notat.NotatDao
import no.nav.helse.spesialist.api.notat.NotatMediator
import no.nav.helse.spesialist.api.objectMapper
import no.nav.helse.spesialist.api.oppgave.OppgaveApiDao
import no.nav.helse.spesialist.api.overstyring.OverstyringApiDao
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.spesialist.api.person.PersonApiDao
import no.nav.helse.spesialist.api.reservasjon.ReservasjonClient
import no.nav.helse.spesialist.api.risikovurdering.RisikovurderingApiDao
import no.nav.helse.spesialist.api.snapshot.SnapshotApiDao
import no.nav.helse.spesialist.api.snapshot.SnapshotMediator
import no.nav.helse.spesialist.api.tildeling.Oppgavehåndterer
import no.nav.helse.spesialist.api.tildeling.TildelingDao
import no.nav.helse.spesialist.api.tildeling.TildelingService
import no.nav.helse.spesialist.api.totrinnsvurdering.TotrinnsvurderingApiDao
import no.nav.helse.spesialist.api.utbetaling.UtbetalingApiDao
import no.nav.helse.spesialist.api.varsel.ApiVarselRepository
import no.nav.helse.spesialist.api.vedtaksperiode.EnhetDto
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

fun main() = runBlocking {
    val jwtStub = JwtStub()
    val clientId = "client_id"
    val issuer = "https://jwt-provider-domain"
    val epostadresse = "sara.saksbehandler@nav.no"
    fun getJwt(
        jwtStub: JwtStub,
        epostadresse: String,
        clientId: String,
        issuer: String,
    ) = jwtStub.getToken(emptyList(), UUID.randomUUID().toString(), epostadresse, clientId, issuer)

    TestApplication(4321).start { dataSource ->
        val snapshotApiDao = mockk<SnapshotApiDao>(relaxed = true)
        val personApiDao = mockk<PersonApiDao>(relaxed = true)
        val egenAnsattApiDao = mockk<EgenAnsattApiDao>(relaxed = true)
        val tildelingDao = TildelingDao(dataSource)
        val arbeidsgiverApiDao = ArbeidsgiverApiDao(dataSource)
        val overstyringApiDao = OverstyringApiDao(dataSource)
        val risikovurderingApiDao = RisikovurderingApiDao(dataSource)
        val apiVarselRepository = ApiVarselRepository(dataSource)
        val utbetalingApiDao = mockk<UtbetalingApiDao>(relaxed = true)
        val oppgaveApiDao = mockk<OppgaveApiDao>(relaxed = true)
        val periodehistorikkDao = mockk<PeriodehistorikkDao>(relaxed = true)
        val notatDao = mockk<NotatDao>(relaxed = true)
        val totrinnsvurderingApiDao = mockk<TotrinnsvurderingApiDao>(relaxed = true)
        val reservasjonClient = mockk<ReservasjonClient>(relaxed = true)
        val behandlingsstatistikkMediator = mockk<BehandlingsstatistikkMediator>(relaxed = true)
        val tildelingService = mockk<TildelingService>(relaxed = true)
        val notatMediator = mockk<NotatMediator>(relaxed = true)
        val saksbehandlerhåndterer = mockk<Saksbehandlerhåndterer>(relaxed = true)
        val oppgavehåndterer = mockk<Oppgavehåndterer>(relaxed = true)
        val totrinnsvurderinghåndterer = mockk<Totrinnsvurderinghåndterer>(relaxed = true)
        val godkjenninghåndterer = mockk<Godkjenninghåndterer>(relaxed = true)

        every { snapshotApiDao.utdatert(any()) } returns false
        every { snapshotApiDao.hentSnapshotMedMetadata(any()) } answers withDelay(800) { (enPersoninfo() to enPerson()) }
        every { personApiDao.personHarAdressebeskyttelse(any(), any()) } returns false
        every {
            personApiDao.personHarAdressebeskyttelse(
                any(),
                no.nav.helse.spesialist.api.person.Adressebeskyttelse.Ugradert
            )
        } returns true
        every { personApiDao.finnesPersonMedFødselsnummer(any()) } returns true
        every { personApiDao.finnEnhet(any()) } returns EnhetDto("1234", "Bømlo")
        every { personApiDao.finnFødselsnummer(isNull(inverse = true)) } returns enPerson().fodselsnummer
        every { personApiDao.spesialistHarPersonKlarForVisningISpeil(any()) } returns true
        coEvery { reservasjonClient.hentReservasjonsstatus(any()) } answers withDelay(800) {
            Reservasjon(kanVarsles = true, reservert = false)
        }
        every { utbetalingApiDao.findUtbetalinger(any()) } returns emptyList()
        every { oppgaveApiDao.finnOppgaver(any()) } returns listOf(enOppgave())
        every { behandlingsstatistikkMediator.getBehandlingsstatistikk() } returns BehandlingsstatistikkResponse(
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
            antallAnnulleringer = 0,
        )

        install(ContentNegotiation) {
            register(
                ContentType.Application.Json,
                JacksonConverter(objectMapper)
            )
        }

        install(CallLogging)
        install(DoubleReceive)

        authentication {
            provider("oidc") {
                authenticate { authenticationContext ->
                    val jwt = getJwt(jwtStub, epostadresse, clientId, issuer)
                    val decodedJwt = JWT().decodeJwt(jwt)
                    authenticationContext.principal(decodedJwt.toJwtPrincipal())
                }
            }
        }

        graphQLApi(
            personApiDao = personApiDao,
            egenAnsattApiDao = egenAnsattApiDao,
            tildelingDao = tildelingDao,
            arbeidsgiverApiDao = arbeidsgiverApiDao,
            overstyringApiDao = overstyringApiDao,
            risikovurderingApiDao = risikovurderingApiDao,
            varselRepository = apiVarselRepository,
            utbetalingApiDao = utbetalingApiDao,
            oppgaveApiDao = oppgaveApiDao,
            periodehistorikkDao = periodehistorikkDao,
            notatDao = notatDao,
            totrinnsvurderingApiDao = totrinnsvurderingApiDao,
            reservasjonClient = reservasjonClient,
            skjermedePersonerGruppeId = UUID.randomUUID(),
            kode7Saksbehandlergruppe = UUID.randomUUID(),
            beslutterGruppeId = UUID.randomUUID(),
            riskGruppeId = UUID.randomUUID(),
            stikkprøveGruppeId = UUID.randomUUID(),
            saksbehandlereMedTilgangTilSpesialsaker = listOf("EN_IDENT"),
            snapshotMediator = SnapshotMediator(snapshotApiDao, mockk(relaxed = true)),
            behandlingsstatistikkMediator = behandlingsstatistikkMediator,
            tildelingService = tildelingService,
            notatMediator = notatMediator,
            saksbehandlerhåndterer = saksbehandlerhåndterer,
            oppgavehåndterer = oppgavehåndterer,
            totrinnsvurderinghåndterer = totrinnsvurderinghåndterer,
            godkjenninghåndterer = godkjenninghåndterer
        )
    }
}

private fun DecodedJWT.toJwtPrincipal() =
    JWTPrincipal(JWTParser().parsePayload(payload.decodeBase64String()))

private fun enOppgave() = OppgaveForOversiktsvisning(
    id = "oppgave_id",
    type = no.nav.helse.spesialist.api.graphql.schema.Oppgavetype.SOKNAD,
    opprettet = "20.47",
    opprinneligSoknadsdato = "01.01.2018",
    vedtaksperiodeId = "vedtaksperiode_id",
    personinfo = Personinfo(
        fornavn = "fornavn",
        mellomnavn = "mellomnavn",
        etternavn = "etternavn",
        fodselsdato = "fodselsdato",
        kjonn = Kjonn.Ukjent,
        adressebeskyttelse = Adressebeskyttelse.Ugradert,
    ),
    aktorId = "aktor_id",
    fodselsnummer = "fodselsnummer",
    flereArbeidsgivere = false,
    boenhet = Boenhet(id = "enhet_id", navn = "enhet_navn"),
    tildeling =
    Tildeling(
        navn = "saksbehandler_navn",
        epost = "epost",
        oid = "oid",
        paaVent = true,
    ),
    periodetype = no.nav.helse.spesialist.api.graphql.schema.Periodetype.FORSTEGANGSBEHANDLING,
    sistSendt = "20.43",
    totrinnsvurdering =
    Totrinnsvurdering(
        erRetur = false,
        saksbehandler = "saksbehandler",
        beslutter = "beslutter",
        erBeslutteroppgave = true
    ),
    mottaker = Mottaker.ARBEIDSGIVER,
    navn = Personnavn(
        fornavn = "fornavn",
        mellomnavn = "mellomnavn",
        etternavn = "etternavn",
    ),
    haster = false,
    harVergemal = false,
    tilhorerEnhetUtland = false,
    spesialsak = false,
)

private fun enPersoninfo() = Personinfo(
    fornavn = "Luke",
    mellomnavn = null,
    etternavn = "Skywalker",
    fodselsdato = "2000-01-01",
    kjonn = Kjonn.Kvinne,
    adressebeskyttelse = Adressebeskyttelse.Ugradert,
    reservasjon = null, // Denne hentes runtime ved hjelp av et kall til KRR
)

private fun enPeriode() = GraphQLBeregnetPeriode(
    erForkastet = false,
    fom = "2020-01-01",
    tom = "2020-01-31",
    inntektstype = GraphQLInntektstype.ENARBEIDSGIVER,
    opprettet = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
    periodetype = GraphQLPeriodetype.FORSTEGANGSBEHANDLING,
    tidslinje = emptyList(),
    vedtaksperiodeId = UUID.randomUUID().toString(),
    id = UUID.randomUUID().toString(),
    beregningId = UUID.randomUUID().toString(),
    forbrukteSykedager = 10,
    gjenstaendeSykedager = 270,
    hendelser = emptyList(),
    maksdato = "2021-01-01",
    periodevilkar = GraphQLPeriodevilkar(
        Alder(
            alderSisteSykedag = 40,
            oppfylt = true
        ),
        sykepengedager = Sykepengedager(
            forbrukteSykedager = 10,
            gjenstaendeSykedager = 270,
            maksdato = "2021-01-01",
            oppfylt = true,
            skjaeringstidspunkt = "2020-01-01"
        )
    ),
    skjaeringstidspunkt = "2020-01-01",
    utbetaling = GraphQLUtbetaling(
        id = "EN-UTBETALING",
        arbeidsgiverFagsystemId = "EN-ARBEIDSGIVERFAGSYSTEMID",
        arbeidsgiverNettoBelop = 30000,
        personFagsystemId = "EN-PERSONFAGSYSTEMID",
        personNettoBelop = 0,
        statusEnum = GraphQLUtbetalingstatus.GODKJENT,
        typeEnum = Utbetalingtype.UTBETALING,
        vurdering = GraphQLVurdering(
            automatisk = false,
            godkjent = true,
            ident = "AB123456",
            tidsstempel = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
        ),
        personoppdrag = GraphQLOppdrag(
            fagsystemId = "EN-PERSONFAGSYSTEMID",
            tidsstempel = "2021-01-01",
            utbetalingslinjer = emptyList(),
            simulering = null
        ),
        arbeidsgiveroppdrag = GraphQLOppdrag(
            fagsystemId = "EN-ARBEIDSGIVERFAGSYSTEMID",
            tidsstempel = "2021-01-01",
            utbetalingslinjer = emptyList(),
            simulering = GraphQLSimulering(
                totalbelop = 30000,
                perioder = listOf(
                    GraphQLSimuleringsperiode(
                        fom = "2020-01-01",
                        tom = "2020-01-31",
                        utbetalinger = listOf(
                            GraphQLSimuleringsutbetaling(
                                utbetalesTilNavn = "EN-PERSON",
                                utbetalesTilId = "EN-PERSONID",
                                feilkonto = false,
                                forfall = "2022-01-01",
                                detaljer = listOf(
                                    GraphQLSimuleringsdetaljer(
                                        belop = 30000,
                                        antallSats = 1,
                                        faktiskFom = "2020-01-01",
                                        faktiskTom = "2020-01-31",
                                        klassekode = "EN-KLASSEKODE",
                                        klassekodeBeskrivelse = "EN-KLASSEKODEBESKRIVELSE",
                                        konto = "EN-KONTO",
                                        refunderesOrgNr = "ET-ORGNR",
                                        sats = 30000.0,
                                        tilbakeforing = false,
                                        typeSats = "EN-TYPESATS",
                                        uforegrad = 100,
                                        utbetalingstype = "EN-UTBETALINGSTYPE"
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
    ),
    vilkarsgrunnlagId = null,
    periodetilstand = GraphQLPeriodetilstand.TILGODKJENNING,
)

private fun enGenerasjon() = GraphQLGenerasjon(
    id = UUID.randomUUID().toString(),
    perioder = listOf(enPeriode()),
)

private fun enArbeidsgiver(organisasjonsnummer: String = "987654321") = GraphQLArbeidsgiver(
    organisasjonsnummer = organisasjonsnummer,
    ghostPerioder = emptyList(),
    generasjoner = listOf(enGenerasjon()),
)

private fun enPerson() = GraphQLPerson(
    aktorId = "jedi-master",
    arbeidsgivere = listOf(enArbeidsgiver()),
    dodsdato = null,
    fodselsnummer = "01017012345",
    versjon = 1,
    vilkarsgrunnlag = emptyList(),
)

fun <T, B> withDelay(millis: Long, block: () -> T): MockKAnswerScope<T, B>.(Call) -> T = {
    runBlocking { delay(millis) }
    block()
}
