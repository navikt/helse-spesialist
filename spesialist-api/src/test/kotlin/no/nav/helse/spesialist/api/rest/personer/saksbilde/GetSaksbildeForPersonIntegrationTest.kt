package no.nav.helse.spesialist.api.rest.personer.saksbilde

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.github.navikt.tbd_libs.populasjonstilgang.api.TilgangSomMangler
import com.github.navikt.tbd_libs.populasjonstilgang.api.TilgangskontrollResultat
import io.mockk.Called
import io.mockk.every
import io.mockk.verify
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.application.testing.assertJsonEquals
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.parallel.ResourceLock
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.slf4j.LoggerFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class GetSaksbildeForPersonIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val personPseudoIdProvider = integrationTestFixture.personPseudoIdProvider
    private val personRepository = integrationTestFixture.sessionContext.personRepository

    @Test
    fun `gir feilmelding om personPseudoId ikke finnes`() {
        // When:
        val response = integrationTestFixture.get(url = "/api/personer/${PersonPseudoId.ny().value}/saksbilde")

        // Then:
        assertFeilrespons(response, 404, "PersonPseudoId har utløpt (eller aldri eksistert)", "PERSON_PSEUDO_ID_IKKE_FUNNET")
        verify { integrationTestFixture.snapshothenterMock wasNot Called }
    }

    @Test
    fun `gir 400 om personPseudoId ikke er en UUID`() {
        // When:
        val response = integrationTestFixture.get(url = "/api/personer/ikke-en-uuid/saksbilde")

        // Then:
        assertUgyldigForespørsel(response)
        verify { integrationTestFixture.snapshothenterMock wasNot Called }
    }

    @Test
    fun `gir 400 også for andre id-er i path som ikke kan parses`() {
        // When:
        val response = integrationTestFixture.get(url = "/api/vedtaksperioder/ikke-en-uuid/notater")

        // Then:
        assertUgyldigForespørsel(response)
    }

    @Test
    fun `gir feilmelding om personen ikke finnes`() {
        // Given:
        val personPseudoId = personPseudoIdProvider.nyPersonPseudoId(lagPerson().id)

        // When:
        val response = integrationTestFixture.get(url = "/api/personer/${personPseudoId.value}/saksbilde")

        // Then:
        assertFeilrespons(response, 404, "Fant ikke data for person", "PERSON_IKKE_FUNNET")
        verify { integrationTestFixture.snapshothenterMock wasNot Called }
    }

    @ParameterizedTest
    @EnumSource(TilgangSomMangler::class)
    @ResourceLock("auditlogg-lytter")
    fun `gir feilmelding og auditlogger når saksbehandler mangler tilgang til personen`(tilgangSomMangler: TilgangSomMangler) {
        // Given:
        val logglytter = Logglytter()
        val saksbehandler = lagSaksbehandler()
        integrationTestFixture.populasjonstilgangskontrollProvider.resultat =
            TilgangskontrollResultat.ManglerTilgang(tilgangSomMangler)
        val person = lagPerson().also(personRepository::lagre)
        val personPseudoId = personPseudoIdProvider.nyPersonPseudoId(person.id)

        // When:
        val response =
            integrationTestFixture.get(url = "/api/personer/${personPseudoId.value}/saksbilde", saksbehandler = saksbehandler)

        // Then:
        assertFeilrespons(response, 403, "Mangler tilgang til person", "MANGLER_TILGANG_TIL_PERSON")
        verify { integrationTestFixture.snapshothenterMock wasNot Called }
        logglytter.assertBleLogget("suid=${saksbehandler.ident.value} duid=${person.id.value} flexString1=Deny", Level.WARN)
    }

    @Test
    fun `krever lesetilgang`() {
        // Given:
        val person = lagPerson().also(personRepository::lagre)
        val personPseudoId = personPseudoIdProvider.nyPersonPseudoId(person.id)

        // When:
        val response =
            integrationTestFixture.get(
                url = "/api/personer/${personPseudoId.value}/saksbilde",
                tilganger = emptySet<Tilgang>(),
            )

        // Then:
        assertEquals(403, response.status)
        verify { integrationTestFixture.snapshothenterMock wasNot Called }
    }

    @Test
    fun `gir feilmelding om snapshot ikke kan hentes`() {
        // Given:
        val person = lagPerson().also(personRepository::lagre)
        val personPseudoId = personPseudoIdProvider.nyPersonPseudoId(person.id)
        every { integrationTestFixture.snapshothenterMock.hentPerson(person.id.value) } throws RuntimeException("Spleis er nede")

        // When:
        val response = integrationTestFixture.get(url = "/api/personer/${personPseudoId.value}/saksbilde")

        // Then:
        assertFeilrespons(response, 500, "Feil ved henting av snapshot for person", "SNAPSHOT_KUNNE_IKKE_HENTES")
    }

    private fun assertFeilrespons(
        response: IntegrationTestFixture.Response,
        status: Int,
        title: String,
        code: String,
    ) {
        assertEquals(status, response.status)
        val body = response.bodyAsJsonNode
        assertNotNull(body)
        assertJsonEquals(
            """
            {
              "type" : "about:blank",
              "status" : $status,
              "title" : "$title",
              "code" : "$code"
            }
            """.trimIndent(),
            body,
        )
    }

    private fun assertUgyldigForespørsel(response: IntegrationTestFixture.Response) {
        assertEquals(400, response.status)
        val body = response.bodyAsJsonNode
        assertNotNull(body)
        assertJsonEquals(
            """
            {
              "type" : "about:blank",
              "status" : 400,
              "title" : "Bad Request"
            }
            """.trimIndent(),
            body,
        )
    }

    private class Logglytter {
        private val appender =
            ListAppender<ILoggingEvent>().apply {
                (LoggerFactory.getLogger("auditLogger") as Logger).addAppender(this)
                start()
            }

        fun assertBleLogget(
            melding: String,
            level: Level,
        ) = assertTrue(appender.list.count { it.message.contains(melding) && it.level == level } == 1) {
            "Forventet ett $level-innslag i auditloggen som inneholder «$melding», men fikk: ${appender.list}"
        }
    }
}
