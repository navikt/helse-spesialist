package no.nav.helse.spesialist.api.rest.vedtaksperioder.notater

import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.application.testing.assertJsonEquals
import no.nav.helse.spesialist.domain.NotatId
import no.nav.helse.spesialist.domain.NotatType
import no.nav.helse.spesialist.domain.testfixtures.lagVedtaksperiode
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PostNotatBehandlerIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val sessionContext = integrationTestFixture.sessionFactory.sessionContext

    @Test
    fun `leggTilNotat fungerer som forventet`() {
        // Given:
        val saksbehandler = lagSaksbehandler()

        val identitetsnummer = lagPerson()
            .also(sessionContext.personRepository::lagre)
            .id
        val vedtaksperiodeId = lagVedtaksperiode(identitetsnummer = identitetsnummer)
            .also(sessionContext.vedtaksperiodeRepository::lagre)
            .id

        // When:
        val response = integrationTestFixture.post(
            url = "/api/vedtaksperioder/${vedtaksperiodeId.value}/notater",
            body = """{ "tekst": "Dette er et notat" }""",
            saksbehandler = saksbehandler,
        )
        val body = response.bodyAsJsonNode

        // Then:
        assertEquals(HttpStatusCode.Created.value, response.status)
        assertNotNull(body)

        val notatId = body["id"]?.asInt()?.let(::NotatId)
        assertNotNull(notatId, "Fikk ikke noen ID på opprettet notat i svaret: $body")

        // Bekreft persistert resultat
        val lagretNotat = sessionContext.notatRepository.finn(notatId)
        assertNotNull(lagretNotat, "Lagret notat med ID $notatId ble ikke gjenfunnet i databasen")

        assertEquals(notatId, lagretNotat.id())
        assertEquals(NotatType.Generelt, lagretNotat.type)
        assertEquals("Dette er et notat", lagretNotat.tekst)
        assertEquals(vedtaksperiodeId.value, lagretNotat.vedtaksperiodeId)
        assertEquals(saksbehandler.id.value, lagretNotat.saksbehandlerOid.value)
        val opprettetTidspunkt = lagretNotat.opprettetTidspunkt
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
        assertFalse(lagretNotat.feilregistrert)
        assertNull(lagretNotat.feilregistrertTidspunkt, "feilregistrertTidspunkt ble lagret selv om notatet er nytt")

        // Bekreft at svaret ikke inneholdt noe mer
        assertJsonEquals("""{ "id" : ${notatId.value} }""", body)
    }
}
