package no.nav.helse.spesialist.api.rest.vedtaksperioder

import com.github.navikt.tbd_libs.populasjonstilgang.api.TilgangSomMangler
import com.github.navikt.tbd_libs.populasjonstilgang.api.TilgangskontrollResultat
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.api.objectMapper
import no.nav.helse.spesialist.application.testing.assertJsonEquals
import no.nav.helse.spesialist.domain.testfixtures.lagVedtaksperiode
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class GetRisikovurderingForVedtaksperiodeIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val sessionContext = integrationTestFixture.sessionContext

    @Test
    fun `gir siste risikovurdering for vedtaksperioden`() {
        // Given:
        val vedtaksperiode = lagVedtaksperiodeForNyPerson()
        lagreRisikovurdering(vedtaksperiode.id.value, """{ "funn": [], "kontrollertOk": [] }""")
        lagreRisikovurdering(
            vedtaksperiode.id.value,
            """
            {
              "funn": [{ "beskrivelse": "Et funn", "kategori": ["8-4"] }],
              "kontrollertOk": [{ "beskrivelse": "Noe som er ok", "kategori": ["Arbeid"] }]
            }
            """.trimIndent(),
        )

        // When:
        val response = integrationTestFixture.get("/api/vedtaksperioder/${vedtaksperiode.id.value}/risikovurdering")

        // Then:
        assertEquals(200, response.status)
        assertJsonEquals(
            """
            {
              "risikovurdering": {
                "funn": [{ "beskrivelse": "Et funn", "kategori": ["8-4"] }],
                "kontrollertOk": [{ "beskrivelse": "Noe som er ok", "kategori": ["Arbeid"] }]
              }
            }
            """.trimIndent(),
            response.bodyAsText,
        )
    }

    @Test
    fun `gir null når vedtaksperioden ikke har risikovurdering`() {
        val vedtaksperiode = lagVedtaksperiodeForNyPerson()

        val response = integrationTestFixture.get("/api/vedtaksperioder/${vedtaksperiode.id.value}/risikovurdering")

        assertEquals(200, response.status)
        assertJsonEquals("""{ "risikovurdering": null }""", response.bodyAsText)
    }

    @Test
    fun `gir 404 når vedtaksperioden ikke finnes`() {
        val response = integrationTestFixture.get("/api/vedtaksperioder/${UUID.randomUUID()}/risikovurdering")

        assertEquals(404, response.status)
    }

    @ParameterizedTest
    @EnumSource(TilgangSomMangler::class)
    fun `gir 403 når saksbehandler mangler tilgang til personen`(tilgangSomMangler: TilgangSomMangler) {
        integrationTestFixture.populasjonstilgangskontrollProvider.resultat =
            TilgangskontrollResultat.ManglerTilgang(tilgangSomMangler)
        val vedtaksperiode = lagVedtaksperiodeForNyPerson()

        val response = integrationTestFixture.get("/api/vedtaksperioder/${vedtaksperiode.id.value}/risikovurdering")

        assertEquals(403, response.status)
    }

    @Test
    fun `krever lesetilgang`() {
        val vedtaksperiode = lagVedtaksperiodeForNyPerson()

        val response =
            integrationTestFixture.get(
                url = "/api/vedtaksperioder/${vedtaksperiode.id.value}/risikovurdering",
                tilganger = emptySet<Tilgang>(),
            )

        assertEquals(403, response.status)
    }

    private fun lagVedtaksperiodeForNyPerson() =
        lagPerson()
            .also(sessionContext.personRepository::lagre)
            .let { lagVedtaksperiode(identitetsnummer = it.id) }
            .also(sessionContext.vedtaksperiodeRepository::lagre)

    private fun lagreRisikovurdering(
        vedtaksperiodeId: UUID,
        data: String,
    ) = sessionContext.risikovurderingDao.lagre(
        vedtaksperiodeId = vedtaksperiodeId,
        kanGodkjennesAutomatisk = false,
        data = objectMapper.readTree(data),
        opprettet = LocalDateTime.now(),
    )
}
