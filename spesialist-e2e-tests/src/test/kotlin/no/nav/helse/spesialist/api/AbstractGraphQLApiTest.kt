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
import no.nav.helse.db.StansAutomatiskBehandlingSaksbehandlerDao
import no.nav.helse.db.VedtakBegrunnelseDao
import no.nav.helse.mediator.SaksbehandlerMediator
import no.nav.helse.spesialist.api.behandlingsstatistikk.IBehandlingsstatistikkService
import no.nav.helse.spesialist.api.endepunkter.ApiTesting
import no.nav.helse.spesialist.api.graphql.ContextFactory
import no.nav.helse.spesialist.api.graphql.GraphQLTestdata.graphQLSpleisVilkarsgrunnlag
import no.nav.helse.spesialist.api.graphql.GraphQLTestdata.opprettBeregnetPeriode
import no.nav.helse.spesialist.api.graphql.GraphQLTestdata.opprettSnapshotArbeidsgiver
import no.nav.helse.spesialist.api.graphql.GraphQLTestdata.opprettSnapshotGenerasjon
import no.nav.helse.spesialist.api.graphql.SpesialistSchema
import no.nav.helse.spesialist.api.graphql.mutation.NotatMutationHandler
import no.nav.helse.spesialist.api.graphql.mutation.OpptegnelseMutationHandler
import no.nav.helse.spesialist.api.graphql.mutation.OverstyringMutationHandler
import no.nav.helse.spesialist.api.graphql.mutation.PaVentMutationHandler
import no.nav.helse.spesialist.api.graphql.mutation.PersonMutationHandler
import no.nav.helse.spesialist.api.graphql.mutation.SkjonnsfastsettelseMutationHandler
import no.nav.helse.spesialist.api.graphql.mutation.StansAutomatiskBehandlingMutationHandler
import no.nav.helse.spesialist.api.graphql.mutation.TildelingMutationHandler
import no.nav.helse.spesialist.api.graphql.mutation.TotrinnsvurderingMutationHandler
import no.nav.helse.spesialist.api.graphql.query.BehandlingsstatistikkQueryHandler
import no.nav.helse.spesialist.api.graphql.query.OppgaverQueryHandler
import no.nav.helse.spesialist.api.graphql.query.OpptegnelseQueryHandler
import no.nav.helse.spesialist.api.graphql.query.PersonQueryHandler
import no.nav.helse.spesialist.api.graphql.queryHandler
import no.nav.helse.spesialist.api.person.PersonService
import no.nav.helse.spesialist.api.snapshot.SnapshotService
import no.nav.helse.spesialist.application.Reservasjonshenter
import no.nav.helse.spesialist.application.tilgangskontroll.randomTilgangsgruppeUuider
import no.nav.helse.spesialist.client.spleis.SpleisClient
import no.nav.helse.spesialist.client.spleis.SpleisClientSnapshothenter
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLArbeidsgiver
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLPerson
import org.intellij.lang.annotations.Language
import java.util.UUID

abstract class AbstractGraphQLApiTest : DatabaseIntegrationTest() {
    protected val tilgangsgruppeUuider = randomTilgangsgruppeUuider()

    private val reservasjonshenter = mockk<Reservasjonshenter>(relaxed = true)
    private val behandlingsstatistikkMediator = mockk<IBehandlingsstatistikkService>(relaxed = true)
    protected val saksbehandlerMediator = mockk<SaksbehandlerMediator>(relaxed = true)
    protected val vedtakBegrunnelseDao = mockk<VedtakBegrunnelseDao>(relaxed = true)
    protected val stansAutomatiskBehandlingSaksbehandlerDao =
        mockk<StansAutomatiskBehandlingSaksbehandlerDao>(relaxed = true)
    private val personhåndterer = mockk<Personhåndterer>(relaxed = true)
    private val stansAutomatiskBehandlinghåndterer = mockk<StansAutomatiskBehandlinghåndterer>(relaxed = true)

    protected val spleisClient = mockk<SpleisClient>(relaxed = true)
    private val snapshothenter = SpleisClientSnapshothenter(spleisClient)
    private val personinfoDao = daos.personinfoDao
    private val snapshotService = SnapshotService(personinfoDao, snapshothenter)

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
                                vergemålApiDao = vergemålApiDao,
                                tildelingApiDao = tildelingApiDao,
                                arbeidsgiverApiDao = arbeidsgiverApiDao,
                                overstyringApiDao = overstyringApiDao,
                                risikovurderingApiDao = risikovurderingApiDao,
                                varselRepository = apiVarselRepository,
                                oppgaveApiDao = oppgaveApiDao,
                                periodehistorikkApiDao = periodehistorikkApiDao,
                                notatDao = notatDao,
                                påVentApiDao = påVentApiDao,
                                apiOppgaveService = apiOppgaveService,
                                saksbehandlerMediator = saksbehandlerMediator,
                                stansAutomatiskBehandlinghåndterer = stansAutomatiskBehandlinghåndterer,
                                personhåndterer = personhåndterer,
                                snapshotService = snapshotService,
                                reservasjonshenter = reservasjonshenter,
                                sessionFactory = sessionFactory,
                                vedtakBegrunnelseDao = vedtakBegrunnelseDao,
                                stansAutomatiskBehandlingSaksbehandlerDao = stansAutomatiskBehandlingSaksbehandlerDao,
                                annulleringRepository = daos.annulleringRepository,
                                saksbehandlerRepository = daos.saksbehandlerRepository,
                            ),
                    ),
                    oppgaver = OppgaverQueryHandler(
                        apiOppgaveService = apiOppgaveService,
                    ),
                    behandlingsstatistikk = BehandlingsstatistikkQueryHandler(
                        behandlingsstatistikkMediator = behandlingsstatistikkMediator,
                    ),
                    opptegnelse = OpptegnelseQueryHandler(
                        saksbehandlerMediator = saksbehandlerMediator,
                    ),
                ),
                mutationHandlers = SpesialistSchema.MutationHandlers(
                    notat = NotatMutationHandler(sessionFactory = sessionFactory),
                    tildeling = TildelingMutationHandler(saksbehandlerMediator = saksbehandlerMediator),
                    opptegnelse = OpptegnelseMutationHandler(saksbehandlerMediator = saksbehandlerMediator),
                    overstyring = OverstyringMutationHandler(
                        saksbehandlerMediator = saksbehandlerMediator
                    ),
                    skjonnsfastsettelse = SkjonnsfastsettelseMutationHandler(saksbehandlerMediator = saksbehandlerMediator),
                    totrinnsvurdering = TotrinnsvurderingMutationHandler(
                        saksbehandlerMediator = saksbehandlerMediator,
                    ),
                    person = PersonMutationHandler(personhåndterer = personhåndterer),
                    paVent = PaVentMutationHandler(saksbehandlerMediator = saksbehandlerMediator),
                    stansAutomatiskBehandling = StansAutomatiskBehandlingMutationHandler(sessionFactory)
                ),
            )

        install(GraphQL) {
            server {
                requestParser = KtorGraphQLRequestParser(objectMapper)
                contextFactory = ContextFactory(tilgangsgruppeUuider = tilgangsgruppeUuider)
            }
            schema(spesialistSchema::setup)
        }
    }

    fun mockSnapshot(
        fødselsnummer: String = FØDSELSNUMMER,
        arbeidsgivere: List<GraphQLArbeidsgiver> = listOf(defaultArbeidsgivere()),
        vilkårsgrunnlagId: UUID = UUID.randomUUID(),
    ) {
        val respons = snapshot(fødselsnummer, arbeidsgivere, vilkårsgrunnlagId)
        every { spleisClient.hentPerson(FØDSELSNUMMER) } returns respons
    }

    private fun snapshot(
        fødselsnummer: String = FØDSELSNUMMER,
        arbeidsgivere: List<GraphQLArbeidsgiver>,
        vilkårsgrunnlagId: UUID,
    ): GraphQLPerson {
        val vilkårsgrunnlag = graphQLSpleisVilkarsgrunnlag(ORGANISASJONSNUMMER, vilkårsgrunnlagId)

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
        val periodeMedOppgave = Periode(UUID.randomUUID(), 1 jan 2018, 25 jan 2018)
        val graphQLperiodeMedOppgave = opprettBeregnetPeriode(4 jan 2023, 5 jan 2023, periodeMedOppgave.id)
        val snapshotGenerasjon = opprettSnapshotGenerasjon(listOf(graphQLperiodeMedOppgave))
        val arbeidsgiver = opprettSnapshotArbeidsgiver(ORGANISASJONSNUMMER, listOf(snapshotGenerasjon))
        return arbeidsgiver
    }

    companion object {
        val jwtStub = JwtStub()
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
        tilgangsgruppe: Tilgangsgruppe? = null,
    ): JsonNode =
        apiTesting.spesialistApi {
            client.post("/graphql") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                authentication(
                    navn = SAKSBEHANDLER.navn,
                    epost = SAKSBEHANDLER.epost,
                    ident = SAKSBEHANDLER.ident,
                    oid = SAKSBEHANDLER.oid.toString(),
                    group = tilgangsgruppe?.let(tilgangsgruppeUuider::uuidFor)?.toString(),
                )
                setBody(mapOf("query" to query))
            }.body<String>()
        }.let(objectMapper::readTree)
}
