package no.nav.helse.spesialist.client.entraid

import com.ethlo.time.Duration
import no.nav.helse.spesialist.client.entraid.testfixtures.ClientEntraIDModuleIntegrationTestFixture
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

class EntraIDAccessTokenGeneratorTest {
    private val integrationTestFixture = ClientEntraIDModuleIntegrationTestFixture()
    private val accessTokenGenerator = integrationTestFixture.module.accessTokenGenerator

    @Test
    fun `klarer å hente access token`() {
        // When:
        val token = accessTokenGenerator.hentAccessToken("etscope")

        // Then:
        assertNotNull(token)
        assertFalse(token.isBlank(), "Token var blankt: \"$token\"")
    }

    @Test
    fun `får samme token to ganger hvis tokenet går ut om 5 minutter`() {
        // Given:
        settTokenExpiry(Duration.ofMinutes(5))
        val token1 = accessTokenGenerator.hentAccessToken("etscope")

        // When:
        val token2 = accessTokenGenerator.hentAccessToken("etscope")

        // Then:
        assertEquals(token1, token2)
    }

    @Test
    fun `får nytt token like etterpå hvis mottatt token umiddelbart gikk ut`() {
        // Given:
        settTokenExpiry(Duration.ZERO)
        val token1 = accessTokenGenerator.hentAccessToken("etscope")

        // When:
        val token2 = accessTokenGenerator.hentAccessToken("etscope")

        // Then:
        assertNotEquals(token1, token2)
    }

    @Test
    fun `får nytt token like etterpå hvis mottatt token går ut om mindre enn 2 minutter`() {
        // Given:
        settTokenExpiry(Duration.ofMinutes(2).plusSeconds(-1))
        val token1 = accessTokenGenerator.hentAccessToken("etscope")

        // When:
        val token2 = accessTokenGenerator.hentAccessToken("etscope")

        // Then:
        assertNotEquals(token1, token2)
    }

    @Test
    fun `får forskjellig token for forskjellige scope`() {
        // Given:
        val token1 = accessTokenGenerator.hentAccessToken("etscope")

        // When:
        val token2 = accessTokenGenerator.hentAccessToken("etannetscope")

        // Then:
        assertNotEquals(token1, token2)
    }

    @Test
    fun `kaster exception ved HTTP 500 fra token-endepunktet`() {
        // Given:
        integrationTestFixture.mockOAuth2Server.enqueueCallback(
            object : DefaultOAuth2TokenCallback() {
                override fun issuerId() = error("Simulert feil i OAuth2-serveren")
            }
        )

        // Then:
        assertThrows<IllegalStateException> {
            // When:
            accessTokenGenerator.hentAccessToken("etscope")
        }.also {
            assertEquals("Fikk HTTP 500 fra Entra ID", it.message)
        }
    }

    private fun settTokenExpiry(expiry: Duration) {
        integrationTestFixture.mockOAuth2Server.enqueueCallback(
            DefaultOAuth2TokenCallback(
                expiry = expiry.seconds
            )
        )
    }
}
