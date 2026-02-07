package no.nav.helse.spesialist.client.entraid

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlPathTemplate
import no.nav.helse.spesialist.application.Either
import no.nav.helse.spesialist.application.tilgangskontroll.Brukerrollehenter
import no.nav.helse.spesialist.client.entraid.testfixtures.ClientEntraIDModuleIntegrationTestFixture
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Brukerrolle
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MsGraphTilgangsgruppehenterTest {
    private val integrationTestFixture = ClientEntraIDModuleIntegrationTestFixture()
    private val tilgangsgruppehenter = integrationTestFixture.module.tilgangsgruppehenter

    @Test
    fun `klarer å hente brukerroller`() {
        // Given:
        integrationTestFixture.msGraphWireMockServer.stubFor(
            post(urlPathTemplate("/v1.0/users/{oid}/checkMemberGroups")).willReturn(
                okJson(
                    """
                    {
                      "value": [
                        "${integrationTestFixture.tilgangsgrupperTilBrukerroller.beslutter.first()}",
                        "${integrationTestFixture.tilgangsgrupperTilBrukerroller.egenAnsatt.last()}"
                      ]
                    }
                    """.trimIndent()
                )
            )
        )

        // When:
        val token = tilgangsgruppehenter.hentBrukerroller(lagSaksbehandler().id)

        // Then:
        assertIs<Either.Success<Set<Brukerrolle>, Brukerrollehenter.Feil>>(token)
        assertEquals(setOf(Brukerrolle.Beslutter, Brukerrolle.EgenAnsatt), token.result)
    }

    @Test
    fun `klarer å tolke tomt sett med grupper`() {
        // Given:
        integrationTestFixture.msGraphWireMockServer.stubFor(
            post(urlPathTemplate("/v1.0/users/{oid}/checkMemberGroups")).willReturn(
                okJson("""{ "value": [] }""")
            )
        )

        // When:
        val token = tilgangsgruppehenter.hentBrukerroller(lagSaksbehandler().id)

        // Then:
        assertIs<Either.Success<Set<Brukerrolle>, Brukerrollehenter.Feil>>(token)
        assertEquals(emptySet(), token.result)
    }

    @Test
    fun `gir typet feil tilbake dersom saksbehandler ikke finnes`() {
        // Given:
        integrationTestFixture.msGraphWireMockServer.stubFor(
            post(urlPathTemplate("/v1.0/users/{oid}/checkMemberGroups")).willReturn(
                WireMock.notFound().withBody("""{ "error": {"code": "Request_ResourceNotFound" } }""")
            )
        )

        // When:
        val token = tilgangsgruppehenter.hentBrukerroller(lagSaksbehandler().id)

        // Then:
        assertIs<Either.Failure<Set<Brukerrolle>, Brukerrollehenter.Feil>>(token)
        assertIs<Brukerrollehenter.Feil.SaksbehandlerFinnesIkke>(token.error)
    }

    @Test
    fun `kaster exception ved HTTP 500 fra MS Graph`() {
        // Given:
        integrationTestFixture.msGraphWireMockServer.stubFor(
            post(urlPathTemplate("/v1.0/users/{oid}/checkMemberGroups")).willReturn(
                WireMock.serverError().withBody("Feilmelding som ikke engang er JSON")
            )
        )

        // Then:
        assertThrows<IllegalStateException> {
            // When:
            tilgangsgruppehenter.hentBrukerroller(lagSaksbehandler().id)
        }.also {
            assertEquals("Fikk HTTP-kode 500 fra MS Graph. Se sikkerlogg for detaljer.", it.message)
        }
    }
}
