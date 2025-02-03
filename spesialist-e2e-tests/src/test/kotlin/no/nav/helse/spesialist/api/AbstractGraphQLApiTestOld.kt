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
import no.nav.helse.mediator.SaksbehandlerMediator
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
import no.nav.helse.spesialist.api.snapshot.SnapshotService
import no.nav.helse.spesialist.application.Reservasjonshenter
import no.nav.helse.spesialist.application.Snapshothenter
import no.nav.helse.spesialist.client.spleis.SpleisClient
import no.nav.helse.spesialist.db.TransactionalSessionFactory
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLArbeidsgiver
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLPerson
import no.nav.helse.util.Periode
import org.intellij.lang.annotations.Language
import java.util.UUID

// Planen er å erstatte bruk av denne med no/nav/helse/spesialist/api/AbstractGraphQLApiTest.kt
internal abstract class AbstractGraphQLApiTestOld : DatabaseIntegrationTestOld() {
    private val kode7Saksbehandlergruppe: UUID = UUID.randomUUID()
    private val skjermedePersonerGruppeId: UUID = UUID.randomUUID()
    private val beslutterGruppeId: UUID = UUID.randomUUID()

    private val reservasjonClient = Reservasjonshenter { null }
    private val behandlingsstatistikkMediator = mockk<IBehandlingsstatistikkService>(relaxed = true)
    private val saksbehandlerMediator = mockk<SaksbehandlerMediator>(relaxed = true)
    private val godkjenninghåndterer = mockk<Godkjenninghåndterer>(relaxed = true)
    private val personhåndterer = mockk<Personhåndterer>(relaxed = true)
    private val dokumenthåndterer = mockk<Dokumenthåndterer>(relaxed = true)
    private val avviksvurderinghenter = mockk<Avviksvurderinghenter>(relaxed = true)
    private val stansAutomatiskBehandlinghåndterer = mockk<StansAutomatiskBehandlinghåndterer>(relaxed = true)

    private val snapshothenter =
        object : Snapshothenter {
            override fun hentPerson(fødselsnummer: String) = null
        }
    private val spleisClient = mockk<SpleisClient>()
    private val snapshotService = SnapshotService(daos.personinfoDao, snapshothenter)

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
                                personApiDao = daos.personApiDao,
                                egenAnsattApiDao = egenAnsattApiDao,
                                tildelingApiDao = daos.tildelingApiDao,
                                arbeidsgiverApiDao = daos.arbeidsgiverApiDao,
                                overstyringApiDao = daos.overstyringApiDao,
                                risikovurderingApiDao = daos.risikovurderingApiDao,
                                varselRepository = daos.varselApiRepository,
                                oppgaveApiDao = daos.oppgaveApiDao,
                                periodehistorikkApiDao = periodehistorikkApiDao,
                                notatDao = daos.notatApiDao,
                                totrinnsvurderingApiDao = daos.totrinnsvurderingApiDao,
                                påVentApiDao = daos.påVentApiDao,
                                vergemålApiDao = daos.vergemålApiDao,
                                snapshotService = snapshotService,
                                reservasjonshenter = reservasjonClient,
                                apiOppgaveService = apiOppgaveService,
                                saksbehandlerMediator = saksbehandlerMediator,
                                personhåndterer = personhåndterer,
                                stansAutomatiskBehandlinghåndterer = stansAutomatiskBehandlinghåndterer,
                                sessionFactory = TransactionalSessionFactory(dataSource),
                                vedtakBegrunnelseDao = daos.vedtakBegrunnelseDao,
                            ),
                    ),
                    oppgaver = OppgaverQueryHandler(
                        apiOppgaveService = apiOppgaveService,
                    ),
                    behandlingsstatistikk = BehandlingsstatistikkQueryHandler(
                        behandlingsstatistikkMediator = behandlingsstatistikkMediator,
                    ),
                    opptegnelse = OpptegnelseQueryHandler(saksbehandlerMediator),
                    dokument = DokumentQueryHandler(
                        personApiDao = daos.personApiDao,
                        egenAnsattApiDao = egenAnsattApiDao,
                        dokumenthåndterer = dokumenthåndterer,
                    ),
                ),
                mutationHandlers = SpesialistSchema.MutationHandlers(
                    notat = NotatMutationHandler(sessionFactory),
                    varsel = VarselMutationHandler(varselRepository = daos.varselApiRepository),
                    tildeling = TildelingMutationHandler(saksbehandlerMediator),
                    opptegnelse = OpptegnelseMutationHandler(saksbehandlerMediator),
                    overstyring = OverstyringMutationHandler(saksbehandlerMediator),
                    skjonnsfastsettelse = SkjonnsfastsettelseMutationHandler(saksbehandlerMediator),
                    minimumSykdomsgrad = MinimumSykdomsgradMutationHandler(saksbehandlerMediator),
                    totrinnsvurdering = TotrinnsvurderingMutationHandler(
                        saksbehandlerMediator = saksbehandlerMediator,
                    ),
                    vedtak = VedtakMutationHandler(
                        saksbehandlerMediator = saksbehandlerMediator,
                        godkjenninghåndterer = godkjenninghåndterer,
                    ),
                    person = PersonMutationHandler(personhåndterer = personhåndterer),
                    annullering = AnnulleringMutationHandler(
                        saksbehandlerMediator = saksbehandlerMediator,
                    ),
                    paVent = PaVentMutationHandler(saksbehandlerMediator = saksbehandlerMediator),
                    opphevStans = OpphevStansMutationHandler(saksbehandlerMediator = saksbehandlerMediator)
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
        every { spleisClient.hentPerson(fødselsnummer) } returns snapshot(fødselsnummer, arbeidsgivere)
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
