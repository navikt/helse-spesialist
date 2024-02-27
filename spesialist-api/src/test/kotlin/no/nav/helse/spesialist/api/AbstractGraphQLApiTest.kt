package no.nav.helse.spesialist.api

import com.expediagroup.graphql.server.execution.GraphQLRequestHandler
import com.expediagroup.graphql.server.execution.GraphQLServer
import com.fasterxml.jackson.databind.JsonNode
import graphql.GraphQL
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.routing.route
import io.mockk.every
import io.mockk.mockk
import java.time.YearMonth
import java.util.UUID
import no.nav.helse.mediator.BehandlingsstatistikkMediator
import no.nav.helse.spesialist.api.avviksvurdering.Avviksvurdering
import no.nav.helse.spesialist.api.avviksvurdering.Beregningsgrunnlag
import no.nav.helse.spesialist.api.avviksvurdering.InnrapportertInntekt
import no.nav.helse.spesialist.api.avviksvurdering.Inntekt
import no.nav.helse.spesialist.api.avviksvurdering.OmregnetÅrsinntekt
import no.nav.helse.spesialist.api.avviksvurdering.Sammenligningsgrunnlag
import no.nav.helse.spesialist.api.endepunkter.ApiTesting
import no.nav.helse.spesialist.api.graphql.ContextFactory
import no.nav.helse.spesialist.api.graphql.RequestParser
import no.nav.helse.spesialist.api.graphql.SchemaBuilder
import no.nav.helse.spesialist.api.graphql.queryHandler
import no.nav.helse.spesialist.api.reservasjon.ReservasjonClient
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLArbeidsgiver
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal abstract class AbstractGraphQLApiTest : DatabaseIntegrationTest() {

    protected val kode7Saksbehandlergruppe: UUID = UUID.randomUUID()
    protected val skjermedePersonerGruppeId: UUID = UUID.randomUUID()
    private val beslutterGruppeId: UUID = UUID.randomUUID()
    private val avviksvurderingId: UUID = UUID.randomUUID()


    private val reservasjonClient = mockk<ReservasjonClient>(relaxed = true)
    private val behandlingsstatistikkMediator = mockk<BehandlingsstatistikkMediator>(relaxed = true)
    protected open val saksbehandlerhåndterer = mockk<Saksbehandlerhåndterer>(relaxed = true)
    protected open val godkjenninghåndterer = mockk<Godkjenninghåndterer>(relaxed = true)
    protected open val personhåndterer = mockk<Personhåndterer>(relaxed = true)
    protected open val dokumenthåndterer = mockk<Dokumenthåndterer>(relaxed = true)
    private val avviksvurderinghenter = mockk<Avviksvurderinghenter>(relaxed = true)

    private lateinit var graphQLServer: GraphQLServer<ApplicationRequest>

    private val apiTesting = ApiTesting(jwtStub) {
        route("graphql") {
            queryHandler(graphQLServer)
        }
    }

    private fun setupGraphQLServer() {
        val schema = SchemaBuilder(
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
            snapshotMediator = snapshotMediator,
            notatDao = notatDao,
            totrinnsvurderingApiDao = totrinnsvurderingApiDao,
            påVentApiDao = påVentApiDao,
            reservasjonClient = reservasjonClient,
            avviksvurderinghenter = avviksvurderinghenter,
            behandlingsstatistikkMediator = behandlingsstatistikkMediator,
            notatMediator = notatMediator,
            saksbehandlerhåndterer = saksbehandlerhåndterer,
            oppgavehåndterer = oppgavehåndterer,
            totrinnsvurderinghåndterer = totrinnsvurderinghåndterer,
            godkjenninghåndterer = godkjenninghåndterer,
            personhåndterer = personhåndterer,
            dokumenthåndterer = dokumenthåndterer,
        ).build()

        graphQLServer = GraphQLServer(
            requestParser = RequestParser(),
            contextFactory = ContextFactory(
                kode7Saksbehandlergruppe,
                skjermedePersonerGruppeId,
                beslutterGruppeId,
            ),
            requestHandler = GraphQLRequestHandler(
                GraphQL.newGraphQL(schema).build()
            )
        )
    }

    @BeforeAll
    fun setup() {
        setupGraphQLServer()
    }

    override fun mockSnapshot(fødselsnummer: String, avviksprosent: Double, arbeidsgivere: List<GraphQLArbeidsgiver>) {
        super.mockSnapshot(fødselsnummer, avviksprosent, arbeidsgivere)
        every { avviksvurderinghenter.hentAvviksvurdering(any()) } returns Avviksvurdering(
            unikId = avviksvurderingId,
            vilkårsgrunnlagId = UUID.randomUUID(),
            fødselsnummer = fødselsnummer,
            skjæringstidspunkt = 1.januar,
            opprettet = 1.januar.atStartOfDay(),
            avviksprosent = avviksprosent,
            sammenligningsgrunnlag = Sammenligningsgrunnlag(
                totalbeløp = 10000.0,
                innrapporterteInntekter = listOf(
                    InnrapportertInntekt(
                        arbeidsgiverreferanse = ORGANISASJONSNUMMER,
                        inntekter = listOf(
                            Inntekt(
                                årMåned = YearMonth.from(1.januar),
                                beløp = 2000.0
                            ),
                            Inntekt(
                                årMåned = YearMonth.from(1.februar),
                                beløp = 2000.0
                            )
                        )
                    ),
                    InnrapportertInntekt(
                        arbeidsgiverreferanse = "987656789",
                        inntekter = listOf(
                            Inntekt(
                                årMåned = YearMonth.from(1.januar),
                                beløp = 1500.0
                            ),
                            Inntekt(
                                årMåned = YearMonth.from(1.februar),
                                beløp = 1500.0
                            ),
                            Inntekt(
                                årMåned = YearMonth.from(1.mars),
                                beløp = 1500.0
                            ),
                            Inntekt(
                                årMåned = YearMonth.from(1.april),
                                beløp = 1500.0
                            )
                        )
                    )
                )
            ),
            beregningsgrunnlag = Beregningsgrunnlag(
                totalbeløp = 10000.0,
                omregnedeÅrsinntekter = listOf(
                    OmregnetÅrsinntekt(arbeidsgiverreferanse = ORGANISASJONSNUMMER, beløp = 10000.0)
                )
            )
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
                        navIdent = ident
                    )
                }"
            )
        }
    }

    protected fun runQuery(@Language("GraphQL") query: String, group: UUID? = null): JsonNode =
        apiTesting.spesialistApi { client ->
            client.post("/graphql") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                authentication(
                    navn = SAKSBEHANDLER.navn,
                    epost = SAKSBEHANDLER.epost,
                    ident = SAKSBEHANDLER.ident,
                    oid = SAKSBEHANDLER.oid.toString(),
                    group = group?.toString()
                )
                setBody(mapOf("query" to query))
            }.body<String>()
        }.let(objectMapper::readTree)
}
