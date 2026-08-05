package no.nav.helse.spesialist.api.rest.totrinnsvurdering

import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.application.testing.assertJsonEquals
import no.nav.helse.spesialist.domain.testfixtures.lagOppgave
import no.nav.helse.spesialist.domain.testfixtures.lagSpleisBehandlingId
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals

class PostSendTilGodkjenningIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val sessionContext = integrationTestFixture.sessionFactory.sessionContext

    @Test
    fun `gir Internal Server Error hvis behandlingen ikke finnes`() {
        // Given:
        val oppgave =
            lagOppgave(
                behandlingId = lagSpleisBehandlingId(),
                godkjenningsbehovId = UUID.randomUUID(),
            ).also(sessionContext.oppgaveRepository::lagre)

        // When:
        val response =
            integrationTestFixture.post(
                url = "/api/oppgaver/${oppgave.id.value}/totrinnsvurdering/send-til-godkjenning",
                body = """{"begrunnelse":"Mangler behandling"}""",
            )

        // Then:
        assertEquals(HttpStatusCode.InternalServerError.value, response.status)
        assertJsonEquals(
            """
            {
              "type": "about:blank",
              "status": 500,
              "title": "Internal Server Error" 
            }
            """.trimIndent(),
            response.bodyAsJsonNode!!,
        )
    }
}
