package no.nav.helse.spesialist.api

import com.expediagroup.graphql.server.ktor.GraphQL
import com.expediagroup.graphql.server.ktor.KtorGraphQLRequestParser
import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.application.plugin
import io.ktor.server.routing.application
import io.ktor.server.routing.route
import io.ktor.server.testing.ApplicationTestBuilder
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.spesialist.api.avviksvurdering.Avviksvurdering
import no.nav.helse.spesialist.api.avviksvurdering.Beregningsgrunnlag
import no.nav.helse.spesialist.api.avviksvurdering.InnrapportertInntekt
import no.nav.helse.spesialist.api.avviksvurdering.Inntekt
import no.nav.helse.spesialist.api.avviksvurdering.OmregnetÅrsinntekt
import no.nav.helse.spesialist.api.avviksvurdering.Sammenligningsgrunnlag
import no.nav.helse.spesialist.api.behandlingsstatistikk.IBehandlingsstatistikkService
import no.nav.helse.spesialist.api.endepunkter.ApiTesting
import no.nav.helse.spesialist.api.graphql.ContextFactory
import no.nav.helse.spesialist.api.graphql.GraphQLTestdata.graphQLSpleisVilkarsgrunnlag
import no.nav.helse.spesialist.api.graphql.GraphQLTestdata.opprettBeregnetPeriode
import no.nav.helse.spesialist.api.graphql.GraphQLTestdata.opprettSnapshotArbeidsgiver
import no.nav.helse.spesialist.api.graphql.GraphQLTestdata.opprettSnapshotGenerasjon
import no.nav.helse.spesialist.api.graphql.SpesialistSchema
import no.nav.helse.spesialist.api.graphql.mutation.AnnulleringMutationHandler
import no.nav.helse.spesialist.api.graphql.mutation.MinimumSykdomsgradMutationHandler
import no.nav.helse.spesialist.api.graphql.mutation.NotatMutationHandler
import no.nav.helse.spesialist.api.graphql.mutation.OpphevStansMutationHandler
import no.nav.helse.spesialist.api.graphql.mutation.OpptegnelseMutationHandler
import no.nav.helse.spesialist.api.graphql.mutation.OverstyringMutationHandler
import no.nav.helse.spesialist.api.graphql.mutation.PaVentMutationHandler
import no.nav.helse.spesialist.api.graphql.mutation.PersonMutationHandler
import no.nav.helse.spesialist.api.graphql.mutation.SkjonnsfastsettelseMutationHandler
import no.nav.helse.spesialist.api.graphql.mutation.TildelingMutationHandler
import no.nav.helse.spesialist.api.graphql.mutation.TotrinnsvurderingMutationHandler
import no.nav.helse.spesialist.api.graphql.mutation.VarselMutationHandler
import no.nav.helse.spesialist.api.graphql.mutation.VedtakMutationHandler
import no.nav.helse.spesialist.api.graphql.query.BehandlingsstatistikkQueryHandler
import no.nav.helse.spesialist.api.graphql.query.DokumentQueryHandler
import no.nav.helse.spesialist.api.graphql.query.OppgaverQueryHandler
import no.nav.helse.spesialist.api.graphql.query.OpptegnelseQueryHandler
import no.nav.helse.spesialist.api.graphql.query.PersonQueryHandler
import no.nav.helse.spesialist.api.graphql.queryHandler
import no.nav.helse.spesialist.api.person.PersonService
import no.nav.helse.spesialist.api.snapshot.SnapshotClient
import no.nav.helse.spesialist.api.snapshot.SnapshotService
import no.nav.helse.spesialist.application.Reservasjonshenter
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLArbeidsgiver
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLPerson
import org.intellij.lang.annotations.Language
import java.time.YearMonth
import java.util.UUID

internal abstract class AbstractGraphQLApiTest : DatabaseIntegrationTest() {
    protected val kode7Saksbehandlergruppe: UUID = UUID.randomUUID()
    protected val skjermedePersonerGruppeId: UUID = UUID.randomUUID()
    private val beslutterGruppeId: UUID = UUID.randomUUID()
    private val avviksvurderingId: UUID = UUID.randomUUID()

    private val reservasjonshenter = mockk<Reservasjonshenter>(relaxed = true)
    private val behandlingsstatistikkMediator = mockk<IBehandlingsstatistikkService>(relaxed = true)
    protected val saksbehandlerhåndterer = mockk<Saksbehandlerhåndterer>(relaxed = true)
    private val godkjenninghåndterer = mockk<Godkjenninghåndterer>(relaxed = true)
    private val personhåndterer = mockk<Personhåndterer>(relaxed = true)
    protected val dokumenthåndterer = mockk<Dokumenthåndterer>(relaxed = true)
    private val avviksvurderinghenter = mockk<Avviksvurderinghenter>(relaxed = true)
    private val stansAutomatiskBehandlinghåndterer = mockk<StansAutomatiskBehandlinghåndterer>(relaxed = true)

    protected val snapshotClient = mockk<SnapshotClient>(relaxed = true)
    private val personinfoDao = repositories.personinfoDao
    private val snapshotService = SnapshotService(personinfoDao, snapshotClient)

    private val apiTesting = ApiTesting(
        jwtStub,
        applicationBuilder = {
            graphQL()
        },
        routeBuilder = {
            route("graphql") {
                queryHandler(application.plugin(GraphQL).server)
            }
        }
    )

    private fun ApplicationTestBuilder.graphQL() {
        val spesialistSchema =
            SpesialistSchema(
                queryHandlers = SpesialistSchema.QueryHandlers(
                    person = PersonQueryHandler(
                        personoppslagService =
                            PersonService(
                                personApiDao = personApiDao,
                                egenAnsattApiDao = egenAnsattApiDao,
                                tildelingApiDao = tildelingApiDao,
                                arbeidsgiverApiDao = arbeidsgiverApiDao,
                                overstyringApiDao = overstyringApiDao,
                                risikovurderingApiDao = risikovurderingApiDao,
                                varselRepository = apiVarselRepository,
                                oppgaveApiDao = oppgaveApiDao,
                                periodehistorikkApiDao = periodehistorikkApiDao,
                                notatDao = notatDao,
                                totrinnsvurderingApiDao = totrinnsvurderingApiDao,
                                påVentApiDao = påVentApiDao,
                                vergemålApiDao = vergemålApiDao,
                                snapshotService = snapshotService,
                                reservasjonshenter = reservasjonshenter,
                                apiOppgaveService = apiOppgaveService,
                                saksbehandlerhåndterer = saksbehandlerhåndterer,
                                avviksvurderinghenter = avviksvurderinghenter,
                                personhåndterer = personhåndterer,
                                stansAutomatiskBehandlinghåndterer = stansAutomatiskBehandlinghåndterer,
                                env = environment,
                            ),
                    ),
                    oppgaver = OppgaverQueryHandler(
                        apiOppgaveService = apiOppgaveService,
                    ),
                    behandlingsstatistikk = BehandlingsstatistikkQueryHandler(
                        behandlingsstatistikkMediator = behandlingsstatistikkMediator,
                    ),
                    opptegnelse = OpptegnelseQueryHandler(
                        saksbehandlerhåndterer = saksbehandlerhåndterer,
                    ),
                    dokument = DokumentQueryHandler(
                        personApiDao = personApiDao,
                        egenAnsattApiDao = egenAnsattApiDao,
                        dokumenthåndterer = dokumenthåndterer,
                    ),
                ),
                mutationHandlers = SpesialistSchema.MutationHandlers(
                    notat = NotatMutationHandler(sessionFactory = sessionFactory),
                    varsel = VarselMutationHandler(varselRepository = apiVarselRepository),
                    tildeling = TildelingMutationHandler(saksbehandlerhåndterer = saksbehandlerhåndterer),
                    opptegnelse = OpptegnelseMutationHandler(saksbehandlerhåndterer = saksbehandlerhåndterer),
                    overstyring = OverstyringMutationHandler(saksbehandlerhåndterer = saksbehandlerhåndterer),
                    skjonnsfastsettelse = SkjonnsfastsettelseMutationHandler(saksbehandlerhåndterer = saksbehandlerhåndterer),
                    minimumSykdomsgrad = MinimumSykdomsgradMutationHandler(saksbehandlerhåndterer = saksbehandlerhåndterer),
                    totrinnsvurdering = TotrinnsvurderingMutationHandler(
                        saksbehandlerhåndterer = saksbehandlerhåndterer,
                    ),
                    vedtak = VedtakMutationHandler(
                        saksbehandlerhåndterer = saksbehandlerhåndterer,
                        godkjenninghåndterer = godkjenninghåndterer,
                    ),
                    person = PersonMutationHandler(personhåndterer = personhåndterer),
                    annullering = AnnulleringMutationHandler(
                        saksbehandlerhåndterer = saksbehandlerhåndterer,
                    ),
                    paVent = PaVentMutationHandler(saksbehandlerhåndterer = saksbehandlerhåndterer),
                    opphevStans = OpphevStansMutationHandler(saksbehandlerhåndterer = saksbehandlerhåndterer)
                ),
            )

        install(GraphQL) {
            server {
                requestParser = KtorGraphQLRequestParser(objectMapper)
                contextFactory =
                    ContextFactory(
                        kode7Saksbehandlergruppe = kode7Saksbehandlergruppe,
                        skjermedePersonerSaksbehandlergruppe = skjermedePersonerGruppeId,
                        beslutterSaksbehandlergruppe = beslutterGruppeId,
                    )
            }
            schema(spesialistSchema::setup)
        }
    }

    fun mockSnapshot(
        fødselsnummer: String = FØDSELSNUMMER,
        arbeidsgivere: List<GraphQLArbeidsgiver> = listOf(defaultArbeidsgivere()),
    ) {
        val respons = snapshot(fødselsnummer, arbeidsgivere)
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns respons
    }

    private fun snapshot(
        fødselsnummer: String = FØDSELSNUMMER,
        arbeidsgivere: List<GraphQLArbeidsgiver>,
    ): GraphQLPerson {
        val vilkårsgrunnlag = graphQLSpleisVilkarsgrunnlag(ORGANISASJONSNUMMER)

        return GraphQLPerson(
            aktorId = AKTØRID,
            arbeidsgivere = arbeidsgivere,
            dodsdato = null,
            fodselsnummer = fødselsnummer,
            versjon = 1,
            vilkarsgrunnlag = listOf(vilkårsgrunnlag),
        )
    }

    private fun defaultArbeidsgivere(): GraphQLArbeidsgiver {
        val periodeMedOppgave = Periode(UUID.randomUUID(), 1.januar, 25.januar)
        val graphQLperiodeMedOppgave = opprettBeregnetPeriode(4.januar(2023), 5.januar(2023), periodeMedOppgave.id)
        val snapshotGenerasjon = opprettSnapshotGenerasjon(listOf(graphQLperiodeMedOppgave))
        val arbeidsgiver = opprettSnapshotArbeidsgiver(ORGANISASJONSNUMMER, listOf(snapshotGenerasjon))
        return arbeidsgiver
    }

    fun mockAvviksvurdering(
        fødselsnummer: String = FØDSELSNUMMER,
        avviksprosent: Double = 0.0,
    ) {
        every { avviksvurderinghenter.hentAvviksvurdering(any()) } returns
            Avviksvurdering(
                unikId = avviksvurderingId,
                vilkårsgrunnlagId = UUID.randomUUID(),
                fødselsnummer = fødselsnummer,
                skjæringstidspunkt = 1.januar,
                opprettet = 1.januar.atStartOfDay(),
                avviksprosent = avviksprosent,
                sammenligningsgrunnlag =
                    Sammenligningsgrunnlag(
                        totalbeløp = 10000.0,
                        innrapporterteInntekter =
                            listOf(
                                InnrapportertInntekt(
                                    arbeidsgiverreferanse = ORGANISASJONSNUMMER,
                                    inntekter =
                                        listOf(
                                            Inntekt(
                                                årMåned = YearMonth.from(1.januar),
                                                beløp = 2000.0,
                                            ),
                                            Inntekt(
                                                årMåned = YearMonth.from(1.februar),
                                                beløp = 2000.0,
                                            ),
                                        ),
                                ),
                                InnrapportertInntekt(
                                    arbeidsgiverreferanse = "987656789",
                                    inntekter =
                                        listOf(
                                            Inntekt(
                                                årMåned = YearMonth.from(1.januar),
                                                beløp = 1500.0,
                                            ),
                                            Inntekt(
                                                årMåned = YearMonth.from(1.februar),
                                                beløp = 1500.0,
                                            ),
                                            Inntekt(
                                                årMåned = YearMonth.from(1.mars),
                                                beløp = 1500.0,
                                            ),
                                            Inntekt(
                                                årMåned = YearMonth.from(1.april),
                                                beløp = 1500.0,
                                            ),
                                        ),
                                ),
                            ),
                    ),
                beregningsgrunnlag =
                    Beregningsgrunnlag(
                        totalbeløp = 10000.0,
                        omregnedeÅrsinntekter =
                            listOf(
                                OmregnetÅrsinntekt(arbeidsgiverreferanse = ORGANISASJONSNUMMER, beløp = 10000.0),
                            ),
                    ),
            )
    }

    companion object {
        internal val jwtStub = JwtStub()
        private val requiredGroup: UUID = UUID.randomUUID()
        private const val clientId = "client_id"
        private const val issuer = "https://jwt-provider-domain"

        fun HttpRequestBuilder.authentication(
            navn: String,
            epost: String,
            ident: String,
            oid: String,
            group: String? = null,
        ) {
            header(
                "Authorization",
                "Bearer ${
                    jwtStub.getToken(
                        groups = listOfNotNull(requiredGroup.toString(), group),
                        oid = oid,
                        epostadresse = epost,
                        clientId = clientId,
                        issuer = issuer,
                        navn = navn,
                        navIdent = ident,
                    )
                }",
            )
        }
    }

    protected fun runQuery(
        @Language("GraphQL") query: String,
        group: UUID? = null,
    ): JsonNode =
        apiTesting.spesialistApi { client ->
            client.post("/graphql") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                authentication(
                    navn = SAKSBEHANDLER.navn,
                    epost = SAKSBEHANDLER.epost,
                    ident = SAKSBEHANDLER.ident,
                    oid = SAKSBEHANDLER.oid.toString(),
                    group = group?.toString(),
                )
                setBody(mapOf("query" to query))
            }.body<String>()
        }.let(objectMapper::readTree)
}
