package no.nav.helse.spesialist.api.rest.personer.tildeling

import com.github.navikt.tbd_libs.populasjonstilgang.api.TilgangSomMangler
import com.github.navikt.tbd_libs.populasjonstilgang.api.TilgangskontrollResultat
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.application.testing.assertJsonEquals
import no.nav.helse.spesialist.domain.testfixtures.lagBehandling
import no.nav.helse.spesialist.domain.testfixtures.lagOppgave
import no.nav.helse.spesialist.domain.testfixtures.lagVedtaksperiode
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class GetTildelingForPersonIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val sessionContext = integrationTestFixture.sessionContext
    private val personPseudoIdProvider = integrationTestFixture.personPseudoIdProvider

    @Test
    fun `gir tildelingen på den aktive oppgaven`() {
        // Given:
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val personPseudoId = personPseudoIdProvider.nyPersonPseudoId(person.id)
        val saksbehandler = lagSaksbehandler().also(sessionContext.saksbehandlerRepository::lagre)
        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id).also(sessionContext.vedtaksperiodeRepository::lagre)
        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id).also(sessionContext.behandlingRepository::lagre)
        lagOppgave(behandling.spleisBehandlingId!!, UUID.randomUUID(), vedtaksperiode.id)
            .also { it.tildelTil(saksbehandler, brukerroller = setOf()) }
            .also(sessionContext.oppgaveRepository::lagre)

        // When:
        val response = integrationTestFixture.get("/api/personer/${personPseudoId.value}/tildeling")

        // Then:
        assertEquals(200, response.status)
        assertJsonEquals(
            """
            {
              "tildeling": {
                "navn": "${saksbehandler.navn}",
                "epost": "${saksbehandler.epost}",
                "oid": "${saksbehandler.id.value}"
              }
            }
            """.trimIndent(),
            response.bodyAsText,
        )
    }

    @Test
    fun `gir null når den aktive oppgaven ikke er tildelt`() {
        // Given:
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val personPseudoId = personPseudoIdProvider.nyPersonPseudoId(person.id)
        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id).also(sessionContext.vedtaksperiodeRepository::lagre)
        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id).also(sessionContext.behandlingRepository::lagre)
        lagOppgave(behandling.spleisBehandlingId!!, UUID.randomUUID(), vedtaksperiode.id)
            .also(sessionContext.oppgaveRepository::lagre)

        // When:
        val response = integrationTestFixture.get("/api/personer/${personPseudoId.value}/tildeling")

        // Then:
        assertEquals(200, response.status)
        assertJsonEquals("""{ "tildeling": null }""", response.bodyAsText)
    }

    @Test
    fun `gir null når personen ikke har aktiv oppgave`() {
        // Given:
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val personPseudoId = personPseudoIdProvider.nyPersonPseudoId(person.id)

        // When:
        val response = integrationTestFixture.get("/api/personer/${personPseudoId.value}/tildeling")

        // Then:
        assertEquals(200, response.status)
        assertJsonEquals("""{ "tildeling": null }""", response.bodyAsText)
    }

    @Test
    fun `gir 404 når pseudoId ikke finnes`() {
        val response = integrationTestFixture.get("/api/personer/${PersonPseudoId.ny().value}/tildeling")

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
        val response = integrationTestFixture.get("/api/personer/${personPseudoId.value}/tildeling")

        // Then:
        assertEquals(403, response.status)
    }

    @Test
    fun `krever lesetilgang`() {
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val personPseudoId = personPseudoIdProvider.nyPersonPseudoId(person.id)

        val response =
            integrationTestFixture.get(
                url = "/api/personer/${personPseudoId.value}/tildeling",
                tilganger = emptySet<Tilgang>(),
            )

        assertEquals(403, response.status)
    }
}
