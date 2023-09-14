package no.nav.helse.spesialist.api

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import java.util.UUID
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.spesialist.api.db.AbstractDatabaseTest
import no.nav.helse.spesialist.api.endepunkter.ApiTesting
import no.nav.helse.spesialist.api.endepunkter.overstyringApi
import no.nav.helse.spesialist.api.graphql.schema.Opptegnelse
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.HandlingFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.OverstyrArbeidsforholdHandlingFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.OverstyrInntektOgRefusjonHandlingFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.OverstyrTidslinjeHandlingFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.SkjønnsfastsettSykepengegrunnlagHandlingFraApi
import no.nav.helse.spesialist.api.vedtak.GodkjenningDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach

internal abstract class AbstractE2ETest : AbstractDatabaseTest() {

    private val testRapid = TestRapid()
    private val saksbehandlerhåndterer = object : Saksbehandlerhåndterer {
        override fun <T : HandlingFraApi> håndter(handlingFraApi: T, saksbehandlerFraApi: SaksbehandlerFraApi) {}
        override fun håndter(godkjenning: GodkjenningDto, behandlingId: UUID, saksbehandlerFraApi: SaksbehandlerFraApi) {}
        override fun opprettAbonnement(saksbehandlerFraApi: SaksbehandlerFraApi, personidentifikator: String) {}
        override fun hentAbonnerteOpptegnelser(saksbehandlerFraApi: SaksbehandlerFraApi, sisteSekvensId: Int): List<Opptegnelse> {
            return emptyList()
        }

        override fun hentAbonnerteOpptegnelser(saksbehandlerFraApi: SaksbehandlerFraApi): List<Opptegnelse> {
            return emptyList()
        }

        override fun håndterTotrinnsvurdering(oppgavereferanse: Long) {}
    }

    private val jwtStub = JwtStub()
    private val clientId = "client_id"
    private val issuer = "https://jwt-provider-domain"

    private val SAKSBEHANDLER_OID: UUID = UUID.randomUUID()
    private val SAKSBEHANDLER_EPOST = "sara.saksbehandler@nav.no"
    private val SAKSBEHANDLER_NAVN = "Sara Saksbehandler"
    private val SAKSBEHANDLER_IDENT = "X999999"

    protected val AKTØR_ID = "1234567891011"
    protected val FØDSELSNUMMER = "12345678910"
    protected val ORGANISASJONSNUMMER = "987654321"
    protected val ORGANISASJONSNUMMER_GHOST = "123456789"

    private val saksbehandler = Saksbehandler(
        oid = SAKSBEHANDLER_OID,
        epost = SAKSBEHANDLER_EPOST,
        navn = SAKSBEHANDLER_NAVN,
        ident = SAKSBEHANDLER_IDENT
    )

    private lateinit var sisteRespons: HttpResponse

    @BeforeEach
    fun beforeEach() {
        testRapid.reset()
    }

    protected fun overstyrTidslinje(payload: OverstyrTidslinjeHandlingFraApi) =
        sendOverstyring("/api/overstyr/dager", saksbehandler, objectMapper.writeValueAsString(payload))

    protected fun overstyrArbeidsforhold(payload: OverstyrArbeidsforholdHandlingFraApi) =
        sendOverstyring("/api/overstyr/arbeidsforhold", saksbehandler, objectMapper.writeValueAsString(payload))

    protected fun overstyrInntektOgRefusjon(payload: OverstyrInntektOgRefusjonHandlingFraApi) =
        sendOverstyring("/api/overstyr/inntektogrefusjon", saksbehandler, objectMapper.writeValueAsString(payload))

    protected fun skjønnsfastsettingSykepengegrunnlag(payload: SkjønnsfastsettSykepengegrunnlagHandlingFraApi) =
        sendOverstyring(
            "/api/skjonnsfastsett/sykepengegrunnlag",
            saksbehandler,
            objectMapper.writeValueAsString(payload)
        )

    protected fun assertSisteResponskode(forventetKode: HttpStatusCode) {
        assertEquals(forventetKode, sisteRespons.status)
    }

    private fun sendOverstyring(
        path: String,
        saksbehandler: Saksbehandler,
        body: String,
    ) {
        ApiTesting(jwtStub) {
            overstyringApi(saksbehandlerhåndterer)
        }.spesialistApi { client ->
            sisteRespons =
                client.post(path) {
                    header(HttpHeaders.ContentType, "application/json")
                    authentication(saksbehandler)
                    setBody(body)
                }
        }
    }

    private fun HttpRequestBuilder.authentication(saksbehandler: Saksbehandler) {
        header(
            "Authorization",
            "Bearer ${saksbehandler.token(jwtStub, clientId, issuer)}"
        )
    }

    protected class Saksbehandler(
        private val oid: UUID,
        private val epost: String,
        private val navn: String,
        private val ident: String,
        private val tilgangsgrupper: List<String> = emptyList(),
    ) {
        internal fun token(stub: JwtStub, clientId: String, issuer: String): String =
            stub.getToken(tilgangsgrupper, oid.toString(), epost, clientId, issuer, navn, ident)
    }
}
