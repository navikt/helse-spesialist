package no.nav.helse.spesialist.api.rest.dialoger.kommentarer

import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.domain.Dialog
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PatchKommentarBehandlerIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val sessionContext = integrationTestFixture.sessionFactory.sessionContext

    @Test
    fun `feilregistert kommentar fungerer som forventet`() {
        // Given:
        val saksbehandler = lagSaksbehandler()

        val dialog = Dialog.Factory.ny()
            .also(sessionContext.dialogRepository::lagre)
        val kommentar = dialog.leggTilKommentar(
            tekst = "Dette er en kommentar",
            saksbehandlerident = saksbehandler.ident,
        )

        sessionContext.dialogRepository.lagre(dialog)

        // When:
        val response = integrationTestFixture.patch(
            url = "/api/dialoger/${dialog.id().value}/kommentarer/${kommentar.id().value}",
            body = """{ "feilregistrert":  true }""",
            saksbehandler = saksbehandler,
        )

        // Then:
        assertEquals(HttpStatusCode.OK.value, response.status)
        assertEquals("{}", response.bodyAsJsonNode.toString())

        // Bekreft persistert resultat
        val lagretDialog = sessionContext.dialogRepository.finn(dialog.id())
        assertNotNull(lagretDialog, "Lagret dialog med ID ${dialog.id()} ble ikke gjenfunnet i databasen")

        assertEquals(dialog.id(), lagretDialog.id())

        val lagretKommentar = lagretDialog.finnKommentar(kommentar.id())
        assertNotNull(lagretKommentar, "Lagret kommentar med ID ${kommentar.id()} ble ikke gjenfunnet i databasen")
        assertEquals(kommentar.id(), lagretKommentar.id())

        val feilregistrertTidspunkt = lagretKommentar.feilregistrertTidspunkt
        assertNotNull(feilregistrertTidspunkt)
        with(feilregistrertTidspunkt) {
            val now = LocalDateTime.now()
            assertTrue(
                isBefore(now),
                "Forventet at den lagrede verdien av opprettetTidspunkt var før nå ($now), men den var $this"
            )
            assertTrue(
                isAfter(now.minusSeconds(5)),
                "Forventet at den lagrede verdien av opprettetTidspunkt var mindre enn fem sekunder før nå ($now), men den var $this"
            )
        }
    }
}
