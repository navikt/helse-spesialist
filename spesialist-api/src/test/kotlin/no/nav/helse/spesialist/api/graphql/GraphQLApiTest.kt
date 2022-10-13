package no.nav.helse.spesialist.api.graphql

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
import java.util.UUID
import kotlinx.coroutines.runBlocking
import no.nav.helse.spesialist.api.AbstractApiTest
import no.nav.helse.spesialist.api.arbeidsgiver.ArbeidsgiverApiDao
import no.nav.helse.spesialist.api.egenAnsatt.EgenAnsattApiDao
import no.nav.helse.spesialist.api.graphql.enums.GraphQLVilkarsgrunnlagtype
import no.nav.helse.spesialist.api.graphql.hentsnapshot.GraphQLPerson
import no.nav.helse.spesialist.api.graphql.hentsnapshot.GraphQLSpleisVilkarsgrunnlag
import no.nav.helse.spesialist.api.graphql.hentsnapshot.GraphQLSykepengegrunnlagsgrense
import no.nav.helse.spesialist.api.graphql.hentsnapshot.GraphQLVilkarsgrunnlaghistorikk
import no.nav.helse.spesialist.api.graphql.schema.Adressebeskyttelse
import no.nav.helse.spesialist.api.graphql.schema.Kjonn
import no.nav.helse.spesialist.api.graphql.schema.Personinfo
import no.nav.helse.spesialist.api.notat.NotatDao
import no.nav.helse.spesialist.api.objectMapper
import no.nav.helse.spesialist.api.oppgave.OppgaveApiDao
import no.nav.helse.spesialist.api.overstyring.OverstyringApiDao
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.spesialist.api.person.Adressebeskyttelse.Ugradert
import no.nav.helse.spesialist.api.person.PersonApiDao
import no.nav.helse.spesialist.api.risikovurdering.RisikovurderingApiDao
import no.nav.helse.spesialist.api.snapshot.SnapshotApiDao
import no.nav.helse.spesialist.api.snapshot.SnapshotMediator
import no.nav.helse.spesialist.api.tildeling.TildelingDao
import no.nav.helse.spesialist.api.utbetaling.UtbetalingApiDao
import no.nav.helse.spesialist.api.vedtaksperiode.VarselDao
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle

@TestInstance(Lifecycle.PER_CLASS)
class GraphQLApiTest : AbstractApiTest() {
    private val personApiDao = mockk<PersonApiDao>(relaxed = true)
    private val egenAnsattApiDao = mockk<EgenAnsattApiDao>(relaxed = true)
    private val tildelingDao = mockk<TildelingDao>()
    private val arbeidsgiverApiDao = mockk<ArbeidsgiverApiDao>()
    private val overstyringApiDao = mockk<OverstyringApiDao>()
    private val risikovurderingApiDao = mockk<RisikovurderingApiDao>()
    private val varselDao = mockk<VarselDao>()
    private val utbetalingApiDao = mockk<UtbetalingApiDao>(relaxed = true)
    private val snapshotApiDao = mockk<SnapshotApiDao>(relaxed = true)
    private val snapshotMediator = SnapshotMediator(snapshotApiDao, mockk(relaxed = true))
    private val oppgaveApiDao = mockk<OppgaveApiDao>(relaxed = true)
    private val periodehistorikkDao = mockk<PeriodehistorikkDao>(relaxed = true)
    private val notatDao = mockk<NotatDao>(relaxed = true)

    private val kode7Saksbehandlergruppe = UUID.randomUUID()
    private val skjermedePersonerGruppeId = UUID.randomUUID()
    private val beslutterGruppeId = UUID.randomUUID()
    private val riskSaksbehandlergruppe = UUID.randomUUID()

    private lateinit var server: GraphQLServer<ApplicationRequest>

    @BeforeAll
    fun setup() {
        val schema = SchemaBuilder(
            personApiDao = personApiDao,
            egenAnsattApiDao = egenAnsattApiDao,
            tildelingDao = tildelingDao,
            arbeidsgiverApiDao = arbeidsgiverApiDao,
            overstyringApiDao = overstyringApiDao,
            risikovurderingApiDao = risikovurderingApiDao,
            varselDao = varselDao,
            utbetalingApiDao = utbetalingApiDao,
            oppgaveApiDao = oppgaveApiDao,
            periodehistorikkDao = periodehistorikkDao,
            notatDao = notatDao,
            snapshotMediator = snapshotMediator,
            reservasjonClient = mockk(relaxed = true),
            oppgaveService = mockk(relaxed = true),
            behandlingsstatistikkMediator = mockk(relaxed = true),
        ).build()

        server = GraphQLServer(
            requestParser = RequestParser(),
            contextFactory = ContextFactory(
                kode7Saksbehandlergruppe,
                skjermedePersonerGruppeId,
                beslutterGruppeId,
                riskSaksbehandlergruppe
            ),
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
        every { snapshotApiDao.hentSnapshotMedMetadata(fødselsnummer) } returns getSnapshotMedMetadata(fødselsnummer)

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
        every { snapshotApiDao.hentSnapshotMedMetadata(fødselsnummer) } returns getSnapshotMedMetadata(fødselsnummer)

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

        assertEquals(
            100_000,
            objectMapper.readTree(body)["data"]["person"]["vilkarsgrunnlaghistorikk"].first()["grunnlag"].first()["sykepengegrunnlagsgrense"]["grunnbelop"].asInt()
        )
        assertEquals(
            600_000,
            objectMapper.readTree(body)["data"]["person"]["vilkarsgrunnlaghistorikk"].first()["grunnlag"].first()["sykepengegrunnlagsgrense"]["grense"].asInt()
        )
        assertEquals(
            "2020-01-01",
            objectMapper.readTree(body)["data"]["person"]["vilkarsgrunnlaghistorikk"].first()["grunnlag"].first()["sykepengegrunnlagsgrense"]["virkningstidspunkt"].asText()
        )
    }

    @Test
    fun `Infinity avviksprosent gir error`() = ugyldigAvvikprosent(Double.POSITIVE_INFINITY)

    @Test
    fun `Nan avviksprosent gir error`() = ugyldigAvvikprosent(Double.NaN)

    private fun ugyldigAvvikprosent(avviksprosent: Double) {
        val fødselsnummer = "12345678910"
        every { personApiDao.finnesPersonMedFødselsnummer(fødselsnummer) } returns true
        every { personApiDao.personHarAdressebeskyttelse(fødselsnummer, Ugradert) } returns true
        every { snapshotApiDao.hentSnapshotMedMetadata(fødselsnummer) } returns getSnapshotMedMetadata(fødselsnummer, avviksprosent)

        val queryString = """
            {
                person(fnr:"$fødselsnummer") {
                    vilkarsgrunnlaghistorikk {
                        id,
                        grunnlag {
                            skjaeringstidspunkt
                             ... on VilkarsgrunnlagSpleis {
                                avviksprosent
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

        val errors = objectMapper.readTree(body)["errors"]
        val forventet = objectMapper.readTree(
            """
            {
                "message": "Can't serialize value (/person/vilkarsgrunnlaghistorikk[0]/grunnlag[0]/avviksprosent) : Expected type 'Float' but was 'Double'.",
                "path": ["person", "vilkarsgrunnlaghistorikk", 0, "grunnlag", 0, "avviksprosent"]
            }
        """
        )
        assertEquals(forventet, errors.first())
        val grunnlag = objectMapper.readTree(body)["data"]["person"]["vilkarsgrunnlaghistorikk"].first()["grunnlag"]
        assertNotNull(grunnlag)
        assertNull(grunnlag["avviksprosent"])
    }

}

private fun getSnapshotMedMetadata(fødselsnummer: String, avviksprosent: Double = 0.0): Pair<Personinfo, GraphQLPerson> {
    val personinfo = Personinfo(
        fornavn = "et-fornavn",
        mellomnavn = "et-mellomnavn",
        etternavn = "et-etternavn",
        fodselsdato = "1970-01-01",
        kjonn = Kjonn.Kvinne,
        adressebeskyttelse = Adressebeskyttelse.Ugradert,
        reservasjon = null,
    )
    val snapshot = GraphQLPerson(
        aktorId = "en-aktørid",
        arbeidsgivere = emptyList(),
        dodsdato = null,
        fodselsnummer = fødselsnummer,
        versjon = 1,
        vilkarsgrunnlaghistorikk = listOf(
            GraphQLVilkarsgrunnlaghistorikk(
                id = "en-id",
                grunnlag = listOf(
                    GraphQLSpleisVilkarsgrunnlag(
                        vilkarsgrunnlagtype = GraphQLVilkarsgrunnlagtype.SPLEIS,
                        inntekter = emptyList(),
                        omregnetArsinntekt = 1_000_000.0,
                        sammenligningsgrunnlag = 1_000_000.0,
                        skjaeringstidspunkt = "2020-01-01",
                        sykepengegrunnlag = 1_000_000.0,
                        antallOpptjeningsdagerErMinst = 123,
                        avviksprosent = avviksprosent,
                        grunnbelop = 100_000,
                        sykepengegrunnlagsgrense = GraphQLSykepengegrunnlagsgrense(
                            grunnbelop = 100_000,
                            grense = 600_000,
                            virkningstidspunkt = "2020-01-01",
                        ),
                        oppfyllerKravOmMedlemskap = true,
                        oppfyllerKravOmMinstelonn = true,
                        oppfyllerKravOmOpptjening = true,
                        opptjeningFra = "2000-01-01",
                    )
                )
            )
        )
    )

    return personinfo to snapshot
}