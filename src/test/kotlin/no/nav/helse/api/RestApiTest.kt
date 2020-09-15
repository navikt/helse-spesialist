package no.nav.helse.api

import AbstractEndToEndTest
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.*
import no.nav.helse.mediator.kafka.HendelseMediator
import no.nav.helse.mediator.kafka.meldinger.GodkjenningMessage
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.command.OppgaveDao
import no.nav.helse.modell.person.HentEnhetLøsning
import no.nav.helse.modell.person.HentInfotrygdutbetalingerLøsning
import no.nav.helse.modell.person.HentPersoninfoLøsning
import no.nav.helse.modell.person.Kjønn
import no.nav.helse.modell.vedtak.SaksbehandleroppgaveDto
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.vedtaksperiode.PersonForSpeilDto
import no.nav.helse.vedtaksperiode.VedtaksperiodeMediator
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.ServerSocket
import java.time.LocalDate
import java.util.*
import java.util.concurrent.TimeUnit.SECONDS

internal class RestApiTest : AbstractEndToEndTest() {

    private lateinit var app: ApplicationEngine
    private lateinit var spleisbehovMediator: HendelseMediator
    private val httpPort = ServerSocket(0).use { it.localPort }
    private val jwtStub = JwtStub()
    private val requiredGroup = "required_group"
    private val saksbehandlerIdent = "1234"
    private val epostadresse = "epostadresse"
    private val clientId = "client_id"
    private val speilClientId = "speil_id"
    private val oid: UUID = UUID.randomUUID()
    private val spesialistOID: UUID = UUID.randomUUID()
    private val issuer = "https://jwt-provider-domain"
    private val client = HttpClient {
        defaultRequest {
            host = "localhost"
            port = httpPort
            header(
                "Authorization",
                "Bearer ${jwtStub.getToken(
                    arrayOf(requiredGroup),
                    oid.toString(),
                    epostadresse,
                    clientId,
                    issuer
                )}"
            )
        }
        install(JsonFeature) {
            serializer = JacksonSerializer(jackson = objectMapper)
        }
    }
    private var vedtaksperiodeId = UUID.randomUUID()
    private val spleisMockClient = SpleisMockClient()

    @BeforeAll
    internal fun `start embedded environment`() {
        val speilSnapshotRestClient = SpeilSnapshotRestClient(
            spleisMockClient.client,
            accessTokenClient(),
            "spleisClientId"
        )

        spleisbehovMediator = HendelseMediator(
            rapidsConnection = testRapid,
            dataSource = dataSource,
            speilSnapshotRestClient = speilSnapshotRestClient,
            spesialistOID = spesialistOID,
            miljøstyrtFeatureToggle = mockk(relaxed = true)
        )
        val oppgaveMediator = OppgaveMediator(OppgaveDao(dataSource), VedtakDao(dataSource))
        val vedtaksperiodeMediator = VedtaksperiodeMediator(
            dataSource = dataSource,
            oppgaveDao = oppgaveDao
        )

        val oidcDiscovery = OidcDiscovery(token_endpoint = "token_endpoint", jwks_uri = "en_uri", issuer = issuer)
        val azureConfig =
            AzureAdAppConfig(clientId = clientId, speilClientId = speilClientId, requiredGroup = requiredGroup)
        val jwkProvider = jwtStub.getJwkProviderMock()

        app = embeddedServer(Netty, port = httpPort) {
            install(ContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(objectMapper)) }
            azureAdAppAuthentication(oidcDiscovery, azureConfig, jwkProvider)
            routing {
                oppgaveApi(oppgaveMediator)
                vedtaksperiodeApi(vedtaksperiodeMediator, spleisbehovMediator)
            }
        }

        app.start(wait = false)
    }

    @BeforeEach
    internal fun updateVedtaksperiodeId() {
        spleisMockClient.setVedtaksperiodeId(vedtaksperiodeId)
    }

    @AfterAll
    internal fun `stop embedded environment`() {
        app.stop(1L, 1L, SECONDS)
    }


    @Test
    fun `hent oppgaver`() {
        val spleisbehovId = UUID.randomUUID()
        val boenhetId = "235"
        val godkjenningMessage = GodkjenningMessage(
            id = spleisbehovId,
            fødselsnummer = "12345",
            aktørId = "12345",
            organisasjonsnummer = "89123",
            vedtaksperiodeId = vedtaksperiodeId,
            periodeFom = LocalDate.of(2018, 1, 1),
            periodeTom = LocalDate.of(2018, 1, 31),
            warnings = emptyList()
        )
        spleisbehovMediator.håndter(godkjenningMessage, "{}")
        spleisbehovMediator.håndter(
            spleisbehovId,
            HentEnhetLøsning(boenhetId),
            hentPersoninfoLøsning(),
            HentInfotrygdutbetalingerLøsning(infotrygdutbetalingerLøsning())
        )
        val response = runBlocking { client.get<HttpStatement>("/api/oppgaver").execute() }
        assertEquals(HttpStatusCode.OK, response.status)
        val oppgaver = runBlocking { response.receive<List<SaksbehandleroppgaveDto>>() }
        assertTrue(oppgaver.any { it.vedtaksperiodeId == vedtaksperiodeId })
        assertTrue(oppgaver.any { it.boenhet.id == "0$boenhetId" })
    }

    @Test
    fun `PersonDTO inneholder infotrygdutbetalinger`() {
        val spleisbehovId = UUID.randomUUID()
        val godkjenningMessage = GodkjenningMessage(
            id = spleisbehovId,
            fødselsnummer = "12345",
            aktørId = "12345",
            organisasjonsnummer = "89123",
            vedtaksperiodeId = vedtaksperiodeId,
            periodeFom = LocalDate.of(2018, 1, 1),
            periodeTom = LocalDate.of(2018, 1, 31),
            warnings = emptyList()
        )
        spleisbehovMediator.håndter(godkjenningMessage, "{}")
        spleisbehovMediator.håndter(
            spleisbehovId,
            HentEnhetLøsning("1234"),
            hentPersoninfoLøsning(),
            HentInfotrygdutbetalingerLøsning(
                infotrygdutbetalingerLøsning(
                    LocalDate.of(2018, 1, 1),
                    LocalDate.of(2018, 1, 31),
                    grad = 100,
                    dagsats = 1200.0,
                    typetekst = "ArbRef",
                    orgnr = "89123"
                )
            )
        )
        val response = runBlocking { client.get<HttpStatement>("/api/person/$vedtaksperiodeId").execute() }
        val infotrygdutbetalinger =
            runBlocking { requireNotNull(response.receive<PersonForSpeilDto>().infotrygdutbetalinger) }
        assertNotNull(infotrygdutbetalinger)
        assertEquals(LocalDate.of(2018, 1, 1), infotrygdutbetalinger[0]["fom"].asLocalDate())
        assertEquals(LocalDate.of(2018, 1, 31), infotrygdutbetalinger[0]["tom"].asLocalDate())
        assertEquals(100, infotrygdutbetalinger[0]["grad"].asInt())
        assertEquals(1200.0, infotrygdutbetalinger[0]["dagsats"].asDouble())
        assertEquals("ArbRef", infotrygdutbetalinger[0]["typetekst"].asText())
        assertEquals("89123", infotrygdutbetalinger[0]["organisasjonsnummer"].asText())
    }

    @Test
    fun `PersonDTO inneholder enhetsinfo`() {
        val spleisbehovId = UUID.randomUUID()
        val godkjenningMessage = GodkjenningMessage(
            id = spleisbehovId,
            fødselsnummer = "12345",
            aktørId = "12345",
            organisasjonsnummer = "89123",
            vedtaksperiodeId = vedtaksperiodeId,
            periodeFom = LocalDate.of(2018, 1, 1),
            periodeTom = LocalDate.of(2018, 1, 31),
            warnings = emptyList()
        )
        spleisbehovMediator.håndter(godkjenningMessage, "{}")
        spleisbehovMediator.håndter(
            spleisbehovId,
            HentEnhetLøsning("301"),
            hentPersoninfoLøsning(),
            HentInfotrygdutbetalingerLøsning(infotrygdutbetalingerLøsning())
        )
        val response = runBlocking { client.get<HttpStatement>("/api/person/$vedtaksperiodeId").execute() }
        val enhet = runBlocking { requireNotNull(response.receive<PersonForSpeilDto>().enhet) }
        assertNotNull(enhet)
        assertEquals("Oslo", enhet.navn)
    }

    @Test
    fun `PersonDTO inneholder fødselsdato og kjønn`() {
        val spleisbehovId = UUID.randomUUID()
        val godkjenningMessage = GodkjenningMessage(
            id = spleisbehovId,
            fødselsnummer = "12345",
            aktørId = "12345",
            organisasjonsnummer = "89123",
            vedtaksperiodeId = vedtaksperiodeId,
            periodeFom = LocalDate.of(2018, 1, 1),
            periodeTom = LocalDate.of(2018, 1, 31),
            warnings = emptyList()
        )
        spleisbehovMediator.håndter(godkjenningMessage, "{}")
        spleisbehovMediator.håndter(
            spleisbehovId,
            HentEnhetLøsning("301"),
            hentPersoninfoLøsning(fornavn = "Sigrun", kjønn = Kjønn.Kvinne, fødselsdato = LocalDate.of(1950, 10, 29)),
            HentInfotrygdutbetalingerLøsning(infotrygdutbetalingerLøsning())
        )

        val response = runBlocking { client.get<HttpStatement>("/api/person/$vedtaksperiodeId").execute() }
        val personinfo = runBlocking { requireNotNull(response.receive<PersonForSpeilDto>().personinfo) }
        assertNotNull(personinfo)
        assertEquals("Sigrun", personinfo.fornavn)
        assertEquals(Kjønn.Kvinne, personinfo.kjønn)
        assertEquals(LocalDate.of(1950, 10, 29), personinfo.fødselsdato)

        // For å simulere de innslagene i person_info som stammer fra før vi begynte å lagre fødselsdato og kjønn
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    "UPDATE person_info SET fodselsdato=?, kjonn=? WHERE id=(SELECT info_ref FROM person WHERE fodselsnummer=?);",
                    null,
                    null,
                    12345
                ).asUpdate
            )
            session.run(
                queryOf(
                    "UPDATE person SET personinfo_oppdatert=now() WHERE fodselsnummer=?;",
                    12345
                ).asUpdate
            )
        }
        val response2 = runBlocking { client.get<HttpStatement>("/api/person/$vedtaksperiodeId").execute() }
        val personinfo2 = runBlocking { requireNotNull(response2.receive<PersonForSpeilDto>().personinfo) }
        assertNotNull(personinfo2)
        assertEquals("Sigrun", personinfo2.fornavn)
        assertEquals(null, personinfo2.kjønn)
        assertEquals(null, personinfo2.fødselsdato)
    }

    @Test
    fun `hent vedtaksperiode med vedtaksperiodeId`() {
        val spleisbehovId = UUID.randomUUID()
        val godkjenningMessage = GodkjenningMessage(
            id = spleisbehovId,
            fødselsnummer = "12345",
            aktørId = "12345",
            organisasjonsnummer = "89123",
            vedtaksperiodeId = vedtaksperiodeId,
            periodeFom = LocalDate.of(2018, 1, 1),
            periodeTom = LocalDate.of(2018, 1, 31),
            warnings = emptyList()
        )
        spleisbehovMediator.håndter(godkjenningMessage, "{}")
        spleisbehovMediator.håndter(
            spleisbehovId,
            HentEnhetLøsning("1234"),
            hentPersoninfoLøsning(),
            HentInfotrygdutbetalingerLøsning(infotrygdutbetalingerLøsning())
        )
        val response = runBlocking { client.get<HttpStatement>("/api/person/$vedtaksperiodeId").execute() }
        assertEquals(HttpStatusCode.OK, response.status)
        val personForSpeilDto = runBlocking { response.receive<PersonForSpeilDto>() }
        assertEquals(
            vedtaksperiodeId.toString(),
            personForSpeilDto.arbeidsgivere.first().vedtaksperioder.first()["id"].asText()
        )
    }

    @Test
    fun `hent vedtaksperiode med aktørId`() {
        val spleisbehovId = UUID.randomUUID()
        val aktørId = "98765"
        val godkjenningMessage = GodkjenningMessage(
            id = spleisbehovId,
            fødselsnummer = "12345",
            aktørId = aktørId,
            organisasjonsnummer = "89123",
            vedtaksperiodeId = vedtaksperiodeId,
            periodeFom = LocalDate.of(2018, 1, 1),
            periodeTom = LocalDate.of(2018, 1, 31),
            warnings = emptyList()
        )
        spleisbehovMediator.håndter(godkjenningMessage, "{}")
        spleisbehovMediator.håndter(
            spleisbehovId,
            HentEnhetLøsning("1234"),
            hentPersoninfoLøsning(),
            HentInfotrygdutbetalingerLøsning(infotrygdutbetalingerLøsning())
        )
        val response = runBlocking { client.get<HttpStatement>("/api/person/aktorId/$aktørId").execute() }
        assertEquals(HttpStatusCode.OK, response.status)
        val personForSpeilDto = runBlocking { response.receive<PersonForSpeilDto>() }
        assertEquals(
            vedtaksperiodeId.toString(),
            personForSpeilDto.arbeidsgivere.first().vedtaksperioder.first()["id"].asText()
        )
    }

    @Test
    fun `hent vedtaksperiode med fødselsnummer`() {
        val spleisbehovId = UUID.randomUUID()
        val fødselsnummer = "42167376532"
        val godkjenningMessage = GodkjenningMessage(
            id = spleisbehovId,
            fødselsnummer = fødselsnummer,
            aktørId = "12345",
            organisasjonsnummer = "89123",
            vedtaksperiodeId = vedtaksperiodeId,
            periodeFom = LocalDate.of(2018, 1, 1),
            periodeTom = LocalDate.of(2018, 1, 31),
            warnings = emptyList()
        )
        spleisbehovMediator.håndter(godkjenningMessage, "{}")
        spleisbehovMediator.håndter(
            spleisbehovId,
            HentEnhetLøsning("1234"),
            hentPersoninfoLøsning(),
            HentInfotrygdutbetalingerLøsning(infotrygdutbetalingerLøsning())
        )
        val response = runBlocking { client.get<HttpStatement>("/api/person/fnr/$fødselsnummer").execute() }
        assertEquals(HttpStatusCode.OK, response.status)
        val personForSpeilDto = runBlocking { response.receive<PersonForSpeilDto>() }
        assertEquals(
            vedtaksperiodeId.toString(),
            personForSpeilDto.arbeidsgivere.first().vedtaksperioder.first()["id"].asText()
        )
    }

    @Test
    fun `404 ved gyldig søketekst-input som ikke eksisterer`() {
        val søketekst = 123456789101
        val responseByAktørId = runBlocking { client.get<HttpStatement>("/api/person/aktorId/$søketekst").execute() }
        val responseByFnr = runBlocking { client.get<HttpStatement>("/api/person/fnr/$søketekst").execute() }
        assertEquals(HttpStatusCode.NotFound, responseByAktørId.status)
        assertEquals(HttpStatusCode.NotFound, responseByFnr.status)
    }

    @Test
    fun `400 ved ikke-numerisk søketekst-input`() {
        val søketekst = "12345678a9101"
        val responseByAktørId = runBlocking { client.get<HttpStatement>("/api/person/aktorId/$søketekst").execute() }
        val responseByFnr = runBlocking { client.get<HttpStatement>("/api/person/fnr/$søketekst").execute() }
        assertEquals(HttpStatusCode.BadRequest, responseByAktørId.status)
        assertEquals(HttpStatusCode.BadRequest, responseByFnr.status)
    }
}

private fun hentPersoninfoLøsning(
    fornavn: String = "Test",
    mellomnavn: String? = null,
    etternavn: String = "Testsen",
    fødselsdato: LocalDate = LocalDate.now(),
    kjønn: Kjønn = Kjønn.Mann
) = HentPersoninfoLøsning(fornavn, mellomnavn, etternavn, fødselsdato, kjønn)

private fun infotrygdutbetalingerLøsning(
    fom: LocalDate = LocalDate.of(2020, 1, 1),
    tom: LocalDate = LocalDate.of(2020, 1, 1),
    grad: Int = 100,
    dagsats: Double = 1200.0,
    typetekst: String = "ArbRef",
    orgnr: String = "89123"
) = objectMapper.readTree(
    """
            [
                {
                    "fom": "$fom",
                    "tom": "$tom",
                    "grad": "$grad",
                    "dagsats": $dagsats,
                    "typetekst": "$typetekst",
                    "organisasjonsnummer": "$orgnr"
                }
            ]
        """.trimIndent()
)
