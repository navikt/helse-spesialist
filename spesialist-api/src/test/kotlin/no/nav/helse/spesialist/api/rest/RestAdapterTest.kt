package no.nav.helse.spesialist.api.rest

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.server.auth.AuthenticationContext
import io.ktor.server.routing.RoutingCall
import io.ktor.util.AttributeKey
import io.ktor.util.Attributes
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.api.auth.SaksbehandlerPrincipal
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Brukerrolle
import org.junit.jupiter.api.Assertions.assertEquals
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
                every { attributes } returns Attributes().apply {
                    put(AttributeKey<AuthenticationContext>("AuthContext"), mockk<AuthenticationContext>() {
                        every { principal<SaksbehandlerPrincipal>() } returns SaksbehandlerPrincipal(
                            saksbehandler = lagSaksbehandler(),
                            brukerroller = Brukerrolle.entries.toSet(),
                        )
                    })
                }
            }

        val adapter =
            RestAdapter(
                sessionFactory = sessionFactory,
                meldingPubliserer = integrationTestFixture.meldingPubliserer,
                versjonAvKode = "0.0.0"
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
            kallKontekst: KallKontekst,
        ): RestResponse<Unit, Error> = error("Intern feil oppstod")

        override val autoriserteBrukerroller: Set<Brukerrolle> = Brukerrolle.entries.toSet()

        override fun openApi(config: RouteConfig) {}
    }

    private class Error : ApiErrorCode {
        override val statusCode: HttpStatusCode = error("Blir aldri kalt i test")
        override val title: String = error("Blir aldri kalt i test")
    }
}
