package no.nav.helse.spesialist.api.rest.vedtaksperioder.notater

import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.application.testing.assertJsonEquals
import no.nav.helse.spesialist.domain.Dialog
import no.nav.helse.spesialist.domain.KommentarId
import no.nav.helse.spesialist.domain.testfixtures.lagNotat
import no.nav.helse.spesialist.domain.testfixtures.lagVedtaksperiode
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PostKommentarBehandlerIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val sessionContext = integrationTestFixture.sessionFactory.sessionContext

    @Test
    fun `legg til kommentar fungerer som forventet`() {
        // Given:
        val saksbehandler = lagSaksbehandler()

        val identitetsnummer = lagPerson()
            .also(sessionContext.personRepository::lagre)
            .id
        val vedtaksperiodeId = lagVedtaksperiode(identitetsnummer = identitetsnummer)
            .also(sessionContext.vedtaksperiodeRepository::lagre)
            .id
        val dialogId = Dialog.Factory.ny()
            .also(sessionContext.dialogRepository::lagre)
            .id()
        val notatId = lagNotat(
            dialogRef = dialogId,
            saksbehandlerOid = saksbehandler.id,
            vedtaksperiodeId = vedtaksperiodeId.value
        ).also(sessionContext.notatRepository::lagre).id()

        // When:
        val response = integrationTestFixture.post(
            url = "/api/vedtaksperioder/${vedtaksperiodeId.value}/notater/${notatId.value}/kommentarer",
            body = """{"tekst" :  "Dette er en kommentar"}""",
            saksbehandler = saksbehandler,
        )

        val body = response.bodyAsJsonNode

        // Then:
        assertEquals(HttpStatusCode.Created.value, response.status)
        assertNotNull(body)

        val kommentarId = body["id"]?.asInt()?.let(::KommentarId)
        assertNotNull(kommentarId, "Fikk ikke noen ID på opprettet kommentar i svaret: $body")

        // Bekreft persistert resultat
        val lagretDialog = sessionContext.dialogRepository.finnForKommentar(kommentarId)
        assertNotNull(lagretDialog, "Lagret dialog for kommentar med ID $kommentarId ble ikke gjenfunnet i databasen")

        val kommentar = lagretDialog.finnKommentar(kommentarId)
        assertNotNull(kommentar?.id())
        assertEquals("Dette er en kommentar", kommentar.tekst)
        assertEquals(saksbehandler.ident, kommentar.saksbehandlerident)
        val opprettetTidspunkt = kommentar.opprettetTidspunkt
        with(opprettetTidspunkt) {
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
        assertNull(kommentar.feilregistrertTidspunkt, "feilregistrertTidspunkt ble lagret selv om kommentaren er ny")

        // Bekreft at svaret ikke inneholdt noe mer
        assertJsonEquals("""{ "id" : ${kommentarId.value} }""", body)

    }
}
