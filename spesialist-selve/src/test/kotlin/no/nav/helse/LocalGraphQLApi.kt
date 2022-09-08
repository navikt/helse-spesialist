package no.nav.helse

import ToggleHelpers.enable
import io.ktor.http.ContentType
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlinx.coroutines.runBlocking
import no.nav.helse.mediator.Toggle
import no.nav.helse.mediator.api.ReservasjonClient
import no.nav.helse.mediator.api.graphQLApi
import no.nav.helse.mediator.api.graphql.SnapshotMediator
import no.nav.helse.mediator.graphql.enums.GraphQLInntektstype
import no.nav.helse.mediator.graphql.enums.GraphQLPeriodetilstand
import no.nav.helse.mediator.graphql.enums.GraphQLPeriodetype
import no.nav.helse.mediator.graphql.enums.GraphQLUtbetalingstatus
import no.nav.helse.mediator.graphql.enums.Utbetalingtype
import no.nav.helse.mediator.graphql.hentsnapshot.Alder
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLArbeidsgiver
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLBeregnetPeriode
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLGenerasjon
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLOppdrag
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLPeriodevilkar
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLPerson
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLSimulering
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLSimuleringsdetaljer
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLSimuleringsperiode
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLSimuleringsutbetaling
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLUtbetaling
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLVurdering
import no.nav.helse.mediator.graphql.hentsnapshot.Soknadsfrist
import no.nav.helse.mediator.graphql.hentsnapshot.Sykepengedager
import no.nav.helse.modell.Adressebeskyttelse
import no.nav.helse.modell.Kjønn
import no.nav.helse.modell.PersoninfoDto
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.spesialist.api.arbeidsgiver.ArbeidsgiverApiDao
import no.nav.helse.spesialist.api.behandlingsstatistikk.Statistikk
import no.nav.helse.spesialist.api.behandlingsstatistikk.BehandlingsstatistikkResponse
import no.nav.helse.spesialist.api.behandlingsstatistikk.BehandlingsstatistikkMediator
import no.nav.helse.spesialist.api.notat.NotatDao
import no.nav.helse.spesialist.api.oppgave.OppgaveApiDao
import no.nav.helse.spesialist.api.oppgave.OppgaveDao
import no.nav.helse.spesialist.api.overstyring.OverstyringApiDao
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.spesialist.api.person.PersonApiDao
import no.nav.helse.spesialist.api.risikovurdering.RisikovurderingApiDao
import no.nav.helse.spesialist.api.tildeling.TildelingDao
import no.nav.helse.spesialist.api.vedtaksperiode.EnhetDto
import no.nav.helse.spesialist.api.vedtaksperiode.VarselDao

fun main() = runBlocking {
    Toggle.GraphQLApi.enable()
    Toggle.GraphQLPlayground.enable()
    TestApplication(4321).start { dataSource ->
        val snapshotDao = mockk<SnapshotDao>(relaxed = true)
        val personApiDao = mockk<PersonApiDao>(relaxed = true)
        val egenAnsattDao = mockk<EgenAnsattDao>(relaxed = true)
        val tildelingDao = TildelingDao(dataSource)
        val arbeidsgiverApiDao = ArbeidsgiverApiDao(dataSource)
        val overstyringApiDao = OverstyringApiDao(dataSource)
        val risikovurderingApiDao = RisikovurderingApiDao(dataSource)
        val varselDao = VarselDao(dataSource)
        val utbetalingDao = mockk<UtbetalingDao>(relaxed = true)
        val oppgaveDao = mockk<OppgaveDao>(relaxed = true)
        val oppgaveApiDao = mockk<OppgaveApiDao>(relaxed = true)
        val periodehistorikkDao = mockk<PeriodehistorikkDao>(relaxed = true)
        val notatDao = mockk<NotatDao>(relaxed = true)
        val reservasjonClient = mockk<ReservasjonClient>(relaxed = true)
        val behandlingsstatistikkMediator = mockk<BehandlingsstatistikkMediator>(relaxed = true)

        every { snapshotDao.hentSnapshotMedMetadata(any()) } returns (enPersoninfo to enPerson)
        every { personApiDao.personHarAdressebeskyttelse(any(), any()) } returns false
        every {
            personApiDao.personHarAdressebeskyttelse(
                any(),
                no.nav.helse.spesialist.api.person.Adressebeskyttelse.Ugradert
            )
        } returns true
        every { personApiDao.finnEnhet(any()) } returns EnhetDto("1234", "Bømlo")
        every { personApiDao.finnFødselsnummer(isNull(inverse = true)) } returns enPerson.fodselsnummer
        every { utbetalingDao.findUtbetalinger(any()) } returns emptyList()
        every { oppgaveDao.finnOppgaveId(any<UUID>()) } returns 123456789L
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

        graphQLApi(
            personApiDao = personApiDao,
            egenAnsattDao = egenAnsattDao,
            tildelingDao = tildelingDao,
            arbeidsgiverApiDao = arbeidsgiverApiDao,
            overstyringApiDao = overstyringApiDao,
            risikovurderingApiDao = risikovurderingApiDao,
            varselDao = varselDao,
            utbetalingDao = utbetalingDao,
            oppgaveDao = oppgaveDao,
            oppgaveApiDao = oppgaveApiDao,
            periodehistorikkDao = periodehistorikkDao,
            notatDao = notatDao,
            skjermedePersonerGruppeId = UUID.randomUUID(),
            kode7Saksbehandlergruppe = UUID.randomUUID(),
            beslutterGruppeId = UUID.randomUUID(),
            riskGruppeId = UUID.randomUUID(),
            reservasjonClient = reservasjonClient,
            snapshotMediator = SnapshotMediator(snapshotDao, mockk(relaxed = true)),
            oppgaveMediator = mockk(relaxed = true),
            oppgaveService = mockk(relaxed = true),
            behandlingsstatistikkMediator = behandlingsstatistikkMediator,
        )
    }
}

private val enPersoninfo = PersoninfoDto(
    fornavn = "Luke",
    mellomnavn = null,
    etternavn = "Skywalker",
    fødselsdato = LocalDate.EPOCH,
    kjønn = Kjønn.Mann,
    adressebeskyttelse = Adressebeskyttelse.Ugradert
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
    aktivitetslogg = emptyList(),
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
        soknadsfrist = Soknadsfrist(
            oppfylt = true,
            sendtNav = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
            soknadFom = "2020-01-01",
            soknadTom = "2020-01-31"
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
    refusjon = null,
    vilkarsgrunnlaghistorikkId = UUID.randomUUID().toString(),
    periodetilstand = GraphQLPeriodetilstand.TILGODKJENNING
)

private fun enGenerasjon() = GraphQLGenerasjon(
    id = UUID.randomUUID().toString(),
    perioder = listOf(enPeriode())
)

private fun enArbeidsgiver(organisasjonsnummer: String = "987654321") = GraphQLArbeidsgiver(
    organisasjonsnummer = organisasjonsnummer,
    ghostPerioder = emptyList(),
    generasjoner = listOf(enGenerasjon())
)

private val enPerson = GraphQLPerson(
    aktorId = "jedi-master",
    arbeidsgivere = listOf(enArbeidsgiver()),
    dodsdato = null,
    fodselsnummer = "01017012345",
    versjon = 1,
    vilkarsgrunnlaghistorikk = emptyList()
)
