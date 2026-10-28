package no.nav.helse.spesialist.api.rest.personer

import com.github.navikt.tbd_libs.populasjonstilgang.api.TilgangSomMangler
import com.github.navikt.tbd_libs.populasjonstilgang.api.TilgangskontrollResultat
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.application.testing.assertJsonEquals
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.Totrinnsvurdering
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class GetTotrinnsvurderingForPersonIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val sessionContext = integrationTestFixture.sessionContext
    private val personPseudoIdProvider = integrationTestFixture.personPseudoIdProvider

    private val saksbehandler = SaksbehandlerOid(UUID.randomUUID())
    private val beslutter = SaksbehandlerOid(UUID.randomUUID())

    @Test
    fun `gir ny totrinnsvurdering som avventer saksbehandler`() {
        // Given:
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val personPseudoId = personPseudoIdProvider.nyPersonPseudoId(person.id)
        lagTotrinnsvurdering(person.id)

        // When:
        val response = integrationTestFixture.get("/api/personer/${personPseudoId.value}/totrinnsvurdering")

        // Then:
        assertEquals(200, response.status)
        assertJsonEquals(
            """
            {
              "totrinnsvurdering": {
                "erRetur": false,
                "saksbehandler": null,
                "beslutter": null,
                "erBeslutteroppgave": false
              }
            }
            """.trimIndent(),
            response.bodyAsText,
        )
    }

    @Test
    fun `gir totrinnsvurdering som avventer beslutter`() {
        // Given:
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val personPseudoId = personPseudoIdProvider.nyPersonPseudoId(person.id)
        lagTotrinnsvurdering(person.id) { it.sendTilBeslutter(1L, saksbehandler) }

        // When:
        val response = integrationTestFixture.get("/api/personer/${personPseudoId.value}/totrinnsvurdering")

        // Then:
        assertEquals(200, response.status)
        assertJsonEquals(
            """
            {
              "totrinnsvurdering": {
                "erRetur": false,
                "saksbehandler": "${saksbehandler.value}",
                "beslutter": null,
                "erBeslutteroppgave": true
              }
            }
            """.trimIndent(),
            response.bodyAsText,
        )
    }

    @Test
    fun `gir totrinnsvurdering som er sendt i retur`() {
        // Given:
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val personPseudoId = personPseudoIdProvider.nyPersonPseudoId(person.id)
        lagTotrinnsvurdering(person.id) {
            it.sendTilBeslutter(1L, saksbehandler)
            it.sendIRetur(1L, beslutter)
        }

        // When:
        val response = integrationTestFixture.get("/api/personer/${personPseudoId.value}/totrinnsvurdering")

        // Then:
        assertEquals(200, response.status)
        assertJsonEquals(
            """
            {
              "totrinnsvurdering": {
                "erRetur": true,
                "saksbehandler": "${saksbehandler.value}",
                "beslutter": "${beslutter.value}",
                "erBeslutteroppgave": false
              }
            }
            """.trimIndent(),
            response.bodyAsText,
        )
    }

    @Test
    fun `gir null når personen ikke har aktiv totrinnsvurdering`() {
        // Given:
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val personPseudoId = personPseudoIdProvider.nyPersonPseudoId(person.id)
        lagTotrinnsvurdering(person.id) { it.ferdigstill() }

        // When:
        val response = integrationTestFixture.get("/api/personer/${personPseudoId.value}/totrinnsvurdering")

        // Then:
        assertEquals(200, response.status)
        assertJsonEquals("""{ "totrinnsvurdering": null }""", response.bodyAsText)
    }

    @Test
    fun `gir 404 når pseudoId ikke finnes`() {
        val response = integrationTestFixture.get("/api/personer/${PersonPseudoId.ny().value}/totrinnsvurdering")

        assertEquals(404, response.status)
    }

    @ParameterizedTest
    @EnumSource(TilgangSomMangler::class)
    fun `gir 403 når saksbehandler mangler tilgang til personen`(tilgangSomMangler: TilgangSomMangler) {
        // Given:
        integrationTestFixture.populasjonstilgangskontrollProvider.resultat =
            TilgangskontrollResultat.ManglerTilgang(tilgangSomMangler)
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val personPseudoId = personPseudoIdProvider.nyPersonPseudoId(person.id)

        // When:
        val response = integrationTestFixture.get("/api/personer/${personPseudoId.value}/totrinnsvurdering")

        // Then:
        assertEquals(403, response.status)
    }

    @Test
    fun `krever lesetilgang`() {
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val personPseudoId = personPseudoIdProvider.nyPersonPseudoId(person.id)

        val response =
            integrationTestFixture.get(
                url = "/api/personer/${personPseudoId.value}/totrinnsvurdering",
                tilganger = emptySet<Tilgang>(),
            )

        assertEquals(403, response.status)
    }

    private fun lagTotrinnsvurdering(
        identitetsnummer: Identitetsnummer,
        endring: (Totrinnsvurdering) -> Unit = {},
    ): Totrinnsvurdering =
        Totrinnsvurdering
            .ny(fødselsnummer = identitetsnummer.value)
            .also(endring)
            .also(sessionContext.totrinnsvurderingRepository::lagre)
}
