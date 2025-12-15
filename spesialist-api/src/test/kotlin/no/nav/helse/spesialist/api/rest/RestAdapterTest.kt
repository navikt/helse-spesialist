package no.nav.helse.spesialist.api.rest

import com.auth0.jwt.interfaces.Claim
import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.routing.RoutingCall
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import no.nav.helse.db.SessionContext
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.UUID
import kotlin.test.Test

class RestAdapterTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val sessionFactory = integrationTestFixture.sessionFactory

    @Test
    fun `Uhåndtert feil returnerer HttpProblem med Internal Server Error uten code`() {
        val responseTextSlot = slot<TextContent>()
        val call =
            mockk<RoutingCall>(relaxed = true) {
                coEvery { respond(capture(responseTextSlot), any()) } returns Unit
                every { principal<JWTPrincipal>() } returns
                    mockk(relaxed = true) {
                        every { payload } returns
                            mockk(relaxed = true) {
                                val claim =
                                    mockk<Claim>(relaxed = true) {
                                        every { asString() } returns UUID.randomUUID().toString()
                                    }
                                every { getClaim("oid") } returns claim
                                every { getClaim("name") } returns claim
                                every { getClaim("preferred_username") } returns claim
                                every { getClaim("NAVident") } returns
                                    mockk<Claim>(relaxed = true) {
                                        every { asString() } returns lagSaksbehandler().ident.value
                                    }
                                every { getClaim("groups") } returns null
                            }
                    }
            }

        val adapter =
            RestAdapter(
                sessionFactory = sessionFactory,
                tilgangsgruppeUuider = IntegrationTestFixture.tilgangsgruppeUuider,
                meldingPubliserer = integrationTestFixture.meldingPubliserer,
                versjonAvKode = "0.0.0",
            )

        runBlocking {
            adapter.behandle(Unit, call, UnitUnitErrorGetBehandler())
        }

        assertEquals(
            """{"type":"about:blank","status":500,"title":"Internal Server Error"}""",
            responseTextSlot.captured.text,
        )
    }

    private class UnitUnitErrorGetBehandler : GetBehandler<Unit, Unit, Error> {
        override fun behandle(
            resource: Unit,
            saksbehandler: Saksbehandler,
            tilgangsgrupper: Set<Tilgangsgruppe>,
            transaksjon: SessionContext,
        ): RestResponse<Unit, Error> = error("Intern feil oppstod")

        override fun openApi(config: RouteConfig) {}
    }

    private class Error : ApiErrorCode {
        override val statusCode: HttpStatusCode = error("Blir aldri kalt i test")
        override val title: String = error("Blir aldri kalt i test")
    }
}
