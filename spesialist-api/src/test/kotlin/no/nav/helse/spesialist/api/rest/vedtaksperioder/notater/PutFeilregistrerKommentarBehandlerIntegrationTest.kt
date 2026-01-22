package no.nav.helse.spesialist.api.rest.vedtaksperioder.notater

import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.domain.Dialog
import no.nav.helse.spesialist.domain.testfixtures.lagNotat
import no.nav.helse.spesialist.domain.testfixtures.lagVedtaksperiode
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PutFeilregistrerKommentarBehandlerIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val sessionContext = integrationTestFixture.sessionFactory.sessionContext

    @Test
    fun `feilregistert kommentar fungerer som forventet`() {
        // Given:
        val saksbehandler = lagSaksbehandler()

        val identitetsnummer = lagPerson()
            .also(sessionContext.personRepository::lagre)
            .id
        val vedtaksperiodeId = lagVedtaksperiode(identitetsnummer = identitetsnummer)
            .also(sessionContext.vedtaksperiodeRepository::lagre)
            .id
        val dialog = Dialog.Factory.ny()
            .also(sessionContext.dialogRepository::lagre)
        val notatId = lagNotat(
            dialogRef = dialog.id(),
            saksbehandlerOid = saksbehandler.id,
            vedtaksperiodeId = vedtaksperiodeId.value
        ).also(sessionContext.notatRepository::lagre).id()
        val kommentar = dialog.leggTilKommentar(
            tekst = "Dette er en kommentar",
            saksbehandlerident = saksbehandler.ident,
        )

        sessionContext.dialogRepository.lagre(dialog)

        // When:
        val response = integrationTestFixture.put(
            url = "/api/vedtaksperioder/${vedtaksperiodeId.value}/notater/${notatId.value}/kommentarer/${kommentar.id().value}/feilregistrer",
            body = "{}",
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
