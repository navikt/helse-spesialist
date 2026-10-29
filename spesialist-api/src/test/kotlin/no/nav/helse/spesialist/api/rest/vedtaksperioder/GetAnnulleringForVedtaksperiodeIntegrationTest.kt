package no.nav.helse.spesialist.api.rest.vedtaksperioder

import com.github.navikt.tbd_libs.populasjonstilgang.api.TilgangSomMangler
import com.github.navikt.tbd_libs.populasjonstilgang.api.TilgangskontrollResultat
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.api.objectMapper
import no.nav.helse.spesialist.application.testing.assertJsonEquals
import no.nav.helse.spesialist.domain.Annullering
import no.nav.helse.spesialist.domain.testfixtures.lagVedtaksperiode
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import tools.jackson.databind.node.ObjectNode
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class GetAnnulleringForVedtaksperiodeIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val sessionContext = integrationTestFixture.sessionContext

    @Test
    fun `gir annulleringen for vedtaksperioden`() {
        // Given:
        val vedtaksperiode = lagVedtaksperiodeForNyPerson()
        val saksbehandler = lagSaksbehandler().also(sessionContext.saksbehandlerRepository::lagre)
        val annullering =
            Annullering.Factory
                .ny(
                    arbeidsgiverFagsystemId = "ARBEIDSGIVER-FAGSYSTEM",
                    personFagsystemId = "PERSON-FAGSYSTEM",
                    saksbehandlerOid = saksbehandler.id,
                    vedtaksperiodeId = vedtaksperiode.id.value,
                    årsaker = listOf("Feil i beregningen"),
                    kommentar = "En kommentar",
                ).also(sessionContext.annulleringRepository::lagreAnnullering)

        // When:
        val response = integrationTestFixture.get("/api/vedtaksperioder/${vedtaksperiode.id.value}/annullering")

        // Then:
        assertEquals(200, response.status)
        val body = objectMapper.readTree(response.bodyAsText)["annullering"]
        assertEquals(annullering.tidspunkt, LocalDateTime.parse(body["tidspunkt"].asString()))
        (body as ObjectNode).remove("tidspunkt")
        assertJsonEquals(
            """
            {
              "saksbehandlerIdent": "${saksbehandler.ident.value}",
              "arbeidsgiverFagsystemId": "ARBEIDSGIVER-FAGSYSTEM",
              "personFagsystemId": "PERSON-FAGSYSTEM",
              "arsaker": ["Feil i beregningen"],
              "begrunnelse": "En kommentar",
              "vedtaksperiodeId": "${vedtaksperiode.id.value}"
            }
            """.trimIndent(),
            body.toString(),
        )
    }

    @Test
    fun `gir null når vedtaksperioden ikke er annullert`() {
        val vedtaksperiode = lagVedtaksperiodeForNyPerson()

        val response = integrationTestFixture.get("/api/vedtaksperioder/${vedtaksperiode.id.value}/annullering")

        assertEquals(200, response.status)
        assertJsonEquals("""{ "annullering": null }""", response.bodyAsText)
    }

    @Test
    fun `gir 404 når vedtaksperioden ikke finnes`() {
        val response = integrationTestFixture.get("/api/vedtaksperioder/${UUID.randomUUID()}/annullering")

        assertEquals(404, response.status)
    }

    @ParameterizedTest
    @EnumSource(TilgangSomMangler::class)
    fun `gir 403 når saksbehandler mangler tilgang til personen`(tilgangSomMangler: TilgangSomMangler) {
        integrationTestFixture.populasjonstilgangskontrollProvider.resultat =
            TilgangskontrollResultat.ManglerTilgang(tilgangSomMangler)
        val vedtaksperiode = lagVedtaksperiodeForNyPerson()

        val response = integrationTestFixture.get("/api/vedtaksperioder/${vedtaksperiode.id.value}/annullering")

        assertEquals(403, response.status)
    }

    @Test
    fun `krever lesetilgang`() {
        val vedtaksperiode = lagVedtaksperiodeForNyPerson()

        val response =
            integrationTestFixture.get(
                url = "/api/vedtaksperioder/${vedtaksperiode.id.value}/annullering",
                tilganger = emptySet<Tilgang>(),
            )

        assertEquals(403, response.status)
    }

    private fun lagVedtaksperiodeForNyPerson() =
        lagPerson()
            .also(sessionContext.personRepository::lagre)
            .let { lagVedtaksperiode(identitetsnummer = it.id) }
            .also(sessionContext.vedtaksperiodeRepository::lagre)
}
