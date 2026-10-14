package no.nav.helse.spesialist.api.rest.tilkomneinntekter

import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.api.rest.ApiTilkommenInntektskilde
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.NAVIdent
import no.nav.helse.spesialist.domain.Periode.Companion.tilOgMed
import no.nav.helse.spesialist.domain.Totrinnsvurdering
import no.nav.helse.spesialist.domain.TotrinnsvurderingId
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntekt
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

class GetTilkomneInntektskilderForPersonBehandlerTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val sessionContext = integrationTestFixture.sessionContext

    @Test
    fun `markerer tilkomne inntekter som er del av aktiv totrinnsvurdering`() {
        // Given:
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val personPseudoId = integrationTestFixture.personPseudoIdProvider.nyPersonPseudoId(person.id)

        val aktiv = lagTotrinnsvurdering(person.id)
        val godkjent = lagTotrinnsvurdering(person.id) { it.ferdigstill() }
        val forkastet = lagTotrinnsvurdering(person.id) { it.forkast() }

        val iAktiv = lagTilkommenInntekt(person.id, aktiv.id(), organisasjonsnummer = "111111111")
        val iGodkjent = lagTilkommenInntekt(person.id, godkjent.id(), organisasjonsnummer = "222222222")
        val iForkastet = lagTilkommenInntekt(person.id, forkastet.id(), organisasjonsnummer = "333333333")

        // When:
        val response =
            integrationTestFixture.get(
                "/api/personer/${personPseudoId.value}/tilkomne-inntektskilder",
                saksbehandler = lagSaksbehandler(),
                tilganger = setOf(Tilgang.Les),
            )

        // Then:
        assertEquals(HttpStatusCode.OK.value, response.status)
        val erDelAvAktiv =
            response
                .body<List<ApiTilkommenInntektskilde>>()
                .flatMap { it.inntekter }
                .associate { it.tilkommenInntektId to it.erDelAvAktivTotrinnsvurdering }
        assertEquals(true, erDelAvAktiv[iAktiv.id.value])
        assertEquals(false, erDelAvAktiv[iGodkjent.id.value])
        assertEquals(false, erDelAvAktiv[iForkastet.id.value])
    }

    private fun lagTotrinnsvurdering(
        identitetsnummer: Identitetsnummer,
        endring: (Totrinnsvurdering) -> Unit = {},
    ): Totrinnsvurdering =
        Totrinnsvurdering
            .ny(fødselsnummer = identitetsnummer.value)
            .also(endring)
            .also(sessionContext.totrinnsvurderingRepository::lagre)

    private fun lagTilkommenInntekt(
        identitetsnummer: Identitetsnummer,
        totrinnsvurderingId: TotrinnsvurderingId,
        organisasjonsnummer: String,
    ): TilkommenInntekt =
        TilkommenInntekt
            .ny(
                identitetsnummer = identitetsnummer,
                saksbehandlerIdent = NAVIdent("A123456"),
                notatTilBeslutter = "notat",
                totrinnsvurderingId = totrinnsvurderingId,
                organisasjonsnummer = organisasjonsnummer,
                periode = 1.jan(2018) tilOgMed 10.jan(2018),
                periodebeløp = BigDecimal("10000"),
                ekskluderteUkedager = emptySet(),
            ).also(sessionContext.tilkommenInntektRepository::lagre)
}
