package no.nav.helse.spesialist.api.rest.tilkomneinntekter

import io.ktor.http.HttpStatusCode
import no.nav.helse.modell.melding.InntektsendringerEvent
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.NAVIdent
import no.nav.helse.spesialist.domain.Periode.Companion.tilOgMed
import no.nav.helse.spesialist.domain.Totrinnsvurdering
import no.nav.helse.spesialist.domain.TotrinnsvurderingId
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.domain.testfixtures.lagBehandling
import no.nav.helse.spesialist.domain.testfixtures.lagVedtaksperiode
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntekt
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntektFjernetEvent
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PatchTilkommenInntektBehandlerIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val sessionContext = integrationTestFixture.sessionContext

    @Test
    fun `kan fjerne tilkommen inntekt når alle vedtaksperiodene er forkastet`() {
        // Given:
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val vedtaksperiode =
            lagVedtaksperiode(identitetsnummer = person.id, forkastet = true)
                .also(sessionContext.vedtaksperiodeRepository::lagre)
        lagBehandling(vedtaksperiodeId = vedtaksperiode.id, fom = 1.jan(2018), tom = 31.jan(2018))
            .also(sessionContext.behandlingRepository::lagre)
        val totrinnsvurdering =
            Totrinnsvurdering
                .ny(fødselsnummer = person.id.value)
                .also { it.forkast() }
                .also(sessionContext.totrinnsvurderingRepository::lagre)
        val tilkommenInntekt = lagTilkommenInntekt(person.id, totrinnsvurdering.id())

        // When:
        val response = fjern(tilkommenInntekt)

        // Then:
        assertEquals(HttpStatusCode.NoContent.value, response.status)

        val lagret = sessionContext.tilkommenInntektRepository.finnOrNull(tilkommenInntekt.id)
        assertNotNull(lagret)
        assertTrue(lagret.fjernet)
        val fjernetEvent = assertIs<TilkommenInntektFjernetEvent>(lagret.events.last())
        assertEquals(totrinnsvurdering.id(), fjernetEvent.metadata.totrinnsvurderingId)

        assertEquals(null, sessionContext.totrinnsvurderingRepository.finnAktivForPersonOrNull(person.id.value))

        integrationTestFixture.assertPubliserteUtgåendeHendelser({ publisert ->
            val event = assertIs<InntektsendringerEvent>(publisert.hendelse)
            assertTrue(
                event.inntektskilder
                    .single()
                    .nullstill
                    .isNotEmpty(),
            )
        })
    }

    @Test
    fun `fjerning innenfor aktivt sykefraværstilfelle knyttes til aktiv totrinnsvurdering`() {
        // Given:
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val vedtaksperiode =
            lagVedtaksperiode(identitetsnummer = person.id)
                .also(sessionContext.vedtaksperiodeRepository::lagre)
        lagBehandling(vedtaksperiodeId = vedtaksperiode.id, fom = 1.jan(2018), tom = 31.jan(2018))
            .also(sessionContext.behandlingRepository::lagre)
        val gammelTotrinnsvurdering =
            Totrinnsvurdering
                .ny(fødselsnummer = person.id.value)
                .also { it.ferdigstill() }
                .also(sessionContext.totrinnsvurderingRepository::lagre)
        val tilkommenInntekt = lagTilkommenInntekt(person.id, gammelTotrinnsvurdering.id())

        // When:
        val response = fjern(tilkommenInntekt)

        // Then:
        assertEquals(HttpStatusCode.NoContent.value, response.status)

        val aktivTotrinnsvurdering = sessionContext.totrinnsvurderingRepository.finnAktivForPersonOrNull(person.id.value)
        assertNotNull(aktivTotrinnsvurdering)
        assertNotEquals(gammelTotrinnsvurdering.id(), aktivTotrinnsvurdering.id())

        val lagret = sessionContext.tilkommenInntektRepository.finnOrNull(tilkommenInntekt.id)
        assertNotNull(lagret)
        assertTrue(lagret.fjernet)
        assertEquals(aktivTotrinnsvurdering.id(), lagret.totrinnsvurderingId)
    }

    private fun fjern(tilkommenInntekt: TilkommenInntekt) =
        integrationTestFixture.patch(
            url = "/api/tilkomne-inntekter/${tilkommenInntekt.id.value}",
            body =
                """
                {
                  "endringer": { "fjernet": { "fra": false, "til": true } },
                  "notatTilBeslutter": "Periodene er forkastet"
                }
                """.trimIndent(),
        )

    private fun lagTilkommenInntekt(
        identitetsnummer: Identitetsnummer,
        totrinnsvurderingId: TotrinnsvurderingId,
    ): TilkommenInntekt =
        TilkommenInntekt
            .ny(
                identitetsnummer = identitetsnummer,
                saksbehandlerIdent = NAVIdent("A123456"),
                notatTilBeslutter = "notat",
                totrinnsvurderingId = totrinnsvurderingId,
                organisasjonsnummer = "111111111",
                periode = 10.jan(2018) tilOgMed 20.jan(2018),
                periodebeløp = BigDecimal("10000"),
                ekskluderteUkedager = emptySet(),
            ).also(sessionContext.tilkommenInntektRepository::lagre)
}
