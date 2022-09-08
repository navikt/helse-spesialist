package no.nav.helse.mediator.api

import com.expediagroup.graphql.server.execution.GraphQLRequestHandler
import com.expediagroup.graphql.server.execution.GraphQLServer
import graphql.GraphQL
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.request.ApplicationRequest
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlinx.coroutines.runBlocking
import no.nav.helse.mediator.api.graphql.ContextFactory
import no.nav.helse.mediator.api.graphql.RequestParser
import no.nav.helse.mediator.api.graphql.SchemaBuilder
import no.nav.helse.mediator.api.graphql.SnapshotMediator
import no.nav.helse.mediator.graphql.enums.GraphQLInntektstype
import no.nav.helse.mediator.graphql.enums.GraphQLPeriodetilstand
import no.nav.helse.mediator.graphql.enums.GraphQLPeriodetype
import no.nav.helse.mediator.graphql.enums.GraphQLUtbetalingstatus
import no.nav.helse.mediator.graphql.enums.GraphQLVilkarsgrunnlagtype
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
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLSpleisVilkarsgrunnlag
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLSykepengegrunnlagsgrense
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLUtbetaling
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLVilkarsgrunnlaghistorikk
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLVurdering
import no.nav.helse.mediator.graphql.hentsnapshot.Soknadsfrist
import no.nav.helse.mediator.graphql.hentsnapshot.Sykepengedager
import no.nav.helse.modell.Adressebeskyttelse
import no.nav.helse.modell.Kjønn
import no.nav.helse.modell.PersoninfoDto
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.modell.oppgave.OppgaveDao
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.objectMapper
import no.nav.helse.spesialist.api.arbeidsgiver.ArbeidsgiverApiDao
import no.nav.helse.spesialist.api.notat.NotatDao
import no.nav.helse.spesialist.api.oppgave.OppgaveApiDao
import no.nav.helse.spesialist.api.overstyring.OverstyringApiDao
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.spesialist.api.person.Adressebeskyttelse.Ugradert
import no.nav.helse.spesialist.api.person.PersonApiDao
import no.nav.helse.spesialist.api.risikovurdering.RisikovurderingApiDao
import no.nav.helse.spesialist.api.tildeling.TildelingDao
import no.nav.helse.spesialist.api.vedtaksperiode.VarselDao
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle

@TestInstance(Lifecycle.PER_CLASS)
class GraphQLApiTest : AbstractApiTest() {
    private val personApiDao = mockk<PersonApiDao>(relaxed = true)
    private val egenAnsattDao = mockk<EgenAnsattDao>(relaxed = true)
    private val tildelingDao = mockk<TildelingDao>()
    private val arbeidsgiverApiDao = mockk<ArbeidsgiverApiDao>()
    private val overstyringApiDao = mockk<OverstyringApiDao>()
    private val risikovurderingApiDao = mockk<RisikovurderingApiDao>()
    private val varselDao = mockk<VarselDao>()
    private val utbetalingDao = mockk<UtbetalingDao>(relaxed = true)
    private val oppgaveDao = mockk<OppgaveDao>(relaxed = true)
    private val oppgaveApiDao = mockk<OppgaveApiDao>(relaxed = true)
    private val periodehistorikkDao = mockk<PeriodehistorikkDao>(relaxed = true)
    private val notatDao = mockk<NotatDao>(relaxed = true)

    private val snapshotMediator = mockk<SnapshotMediator>(relaxed = true)

    private val kode7Saksbehandlergruppe = UUID.randomUUID()
    private val skjermedePersonerGruppeId = UUID.randomUUID()
    private val beslutterGruppeId = UUID.randomUUID()
    private val riskSaksbehandlergruppe = UUID.randomUUID()

    private lateinit var server: GraphQLServer<ApplicationRequest>

    @BeforeAll
    fun setup() {
        val schema = SchemaBuilder(
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
            snapshotMediator = snapshotMediator,
            reservasjonClient = mockk(relaxed = true),
            oppgaveMediator = mockk(relaxed = true),
            oppgaveService = mockk(relaxed = true),
            behandlingsstatistikkMediator = mockk(relaxed = true),
        ).build()

        server = GraphQLServer(
            requestParser = RequestParser(),
            contextFactory = ContextFactory(kode7Saksbehandlergruppe, skjermedePersonerGruppeId, beslutterGruppeId, riskSaksbehandlergruppe),
            requestHandler = GraphQLRequestHandler(
                GraphQL.newGraphQL(schema).build()
            )
        )

        setupServer {
            routes(server)
        }
    }

    @Test
    fun `henter fødselsnummer`() {
        val fødselsnummer = "12345678910"
        every { personApiDao.finnesPersonMedFødselsnummer(fødselsnummer) } returns true
        every { personApiDao.personHarAdressebeskyttelse(fødselsnummer, Ugradert) } returns true
        every { snapshotMediator.hentSnapshot(fødselsnummer) } returns Pair(
            enPersoninfo,
            enPerson(fødselsnummer)
        )

        val queryString = """
            {
                person(fnr:"$fødselsnummer") {
                    fodselsnummer
                }
            }
        """.trimIndent()

        val body = runBlocking {
            val response = client.preparePost("/graphql") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                authentication(UUID.randomUUID())
                setBody(mapOf("query" to queryString))
            }.execute()
            response.body<String>()
        }

        assertEquals(fødselsnummer, objectMapper.readTree(body)["data"]["person"]["fodselsnummer"].asText())
    }

    @Test
    fun `henter sykepengegrunnlagsgrense`() {
        val fødselsnummer = "12345678910"
        every { personApiDao.finnesPersonMedFødselsnummer(fødselsnummer) } returns true
        every { personApiDao.personHarAdressebeskyttelse(fødselsnummer, Ugradert) } returns true
        every { snapshotMediator.hentSnapshot(fødselsnummer) } returns Pair(
            enPersoninfo,
            enPerson(fødselsnummer)
        )


        val queryString = """
            {
                person(fnr:"$fødselsnummer") {
                    vilkarsgrunnlaghistorikk {
                        id,
                        grunnlag {
                            skjaeringstidspunkt
                             ... on VilkarsgrunnlagSpleis {
                                sykepengegrunnlagsgrense {
                                    grunnbelop,
                                    grense,
                                    virkningstidspunkt
                                }
                             }
                        }
                    }
                }
            }
        """.trimIndent()


        val body = runBlocking {
            val response = client.preparePost("/graphql") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                authentication(UUID.randomUUID())
                setBody(mapOf("query" to queryString))
            }.execute()
            response.body<String>()
        }

        assertEquals(100_000, objectMapper.readTree(body)["data"]["person"]["vilkarsgrunnlaghistorikk"].first()["grunnlag"].first()["sykepengegrunnlagsgrense"]["grunnbelop"].asInt())
        assertEquals(600_000, objectMapper.readTree(body)["data"]["person"]["vilkarsgrunnlaghistorikk"].first()["grunnlag"].first()["sykepengegrunnlagsgrense"]["grense"].asInt())
        assertEquals("2022-05-01", objectMapper.readTree(body)["data"]["person"]["vilkarsgrunnlaghistorikk"].first()["grunnlag"].first()["sykepengegrunnlagsgrense"]["virkningstidspunkt"].asText())
    }

    private val enPersoninfo = PersoninfoDto(
        fornavn = "Luke",
        mellomnavn = null,
        etternavn = "Skywalker",
        fødselsdato = LocalDate.EPOCH,
        kjønn = Kjønn.Mann,
        adressebeskyttelse = Adressebeskyttelse.Ugradert,
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

    private fun enPerson(fødselsnummer: String) = GraphQLPerson(
        aktorId = "jedi-master",
        arbeidsgivere = listOf(enArbeidsgiver()),
        dodsdato = null,
        fodselsnummer = fødselsnummer,
        versjon = 1,
        vilkarsgrunnlaghistorikk = listOf(GraphQLVilkarsgrunnlaghistorikk(
            UUID.randomUUID().toString(),
            grunnlag = listOf(GraphQLSpleisVilkarsgrunnlag(
                GraphQLVilkarsgrunnlagtype.SPLEIS,
                inntekter = emptyList(),
                omregnetArsinntekt = 100.0,
                sammenligningsgrunnlag = 100.0,
                skjaeringstidspunkt = "2022-05-02",
                sykepengegrunnlag = 100.0,
                antallOpptjeningsdagerErMinst = 0,
                avviksprosent = 0.0,
                grunnbelop = 0,
                sykepengegrunnlagsgrense = GraphQLSykepengegrunnlagsgrense(100_000, 600_000, "2022-05-01"),
                oppfyllerKravOmMedlemskap = false,
                oppfyllerKravOmMinstelonn = false,
                oppfyllerKravOmOpptjening = false,
                opptjeningFra = "2022-05-23"
            ))
        ))
    )
}
