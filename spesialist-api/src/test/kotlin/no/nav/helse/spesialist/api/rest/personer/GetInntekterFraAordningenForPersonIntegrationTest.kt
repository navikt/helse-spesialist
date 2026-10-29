package no.nav.helse.spesialist.api.rest.personer

import com.github.navikt.tbd_libs.populasjonstilgang.api.TilgangSomMangler
import com.github.navikt.tbd_libs.populasjonstilgang.api.TilgangskontrollResultat
import no.nav.helse.mediator.meldinger.løsninger.Inntekter
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.application.testing.assertJsonEquals
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals

class GetInntekterFraAordningenForPersonIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val sessionContext = integrationTestFixture.sessionContext
    private val personPseudoIdProvider = integrationTestFixture.personPseudoIdProvider

    @Test
    fun `gir inntektene per arbeidsgiver og skjæringstidspunkt med sum per måned`() {
        // Given:
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val personPseudoId = personPseudoIdProvider.nyPersonPseudoId(person.id)
        sessionContext.personDao.lagreInntekter(
            fødselsnummer = person.id.value,
            skjæringstidspunkt = LocalDate.parse("2022-01-01"),
            inntekter =
                listOf(
                    Inntekter(
                        årMåned = YearMonth.parse("2021-12"),
                        inntektsliste =
                            listOf(
                                Inntekter.Inntekt(beløp = 20000.0, orgnummer = "111111111"),
                                Inntekter.Inntekt(beløp = 2000.0, orgnummer = "111111111"),
                                Inntekter.Inntekt(beløp = 15000.0, orgnummer = "222222222"),
                            ),
                    ),
                    Inntekter(
                        årMåned = YearMonth.parse("2021-11"),
                        inntektsliste = listOf(Inntekter.Inntekt(beløp = 21000.0, orgnummer = "111111111")),
                    ),
                ),
        )
        sessionContext.personDao.lagreInntekter(
            fødselsnummer = person.id.value,
            skjæringstidspunkt = LocalDate.parse("2023-01-01"),
            inntekter =
                listOf(
                    Inntekter(
                        årMåned = YearMonth.parse("2022-12"),
                        inntektsliste = listOf(Inntekter.Inntekt(beløp = 23000.0, orgnummer = "111111111")),
                    ),
                ),
        )

        // When:
        val response = integrationTestFixture.get("/api/personer/${personPseudoId.value}/inntekter-fra-aordningen")

        // Then:
        assertEquals(200, response.status)
        assertJsonEquals(
            """
            [
              {
                "organisasjonsnummer": "111111111",
                "skjaeringstidspunkt": "2022-01-01",
                "inntekter": [
                  { "maned": "2021-12", "sum": 22000.0 },
                  { "maned": "2021-11", "sum": 21000.0 }
                ]
              },
              {
                "organisasjonsnummer": "222222222",
                "skjaeringstidspunkt": "2022-01-01",
                "inntekter": [
                  { "maned": "2021-12", "sum": 15000.0 }
                ]
              },
              {
                "organisasjonsnummer": "111111111",
                "skjaeringstidspunkt": "2023-01-01",
                "inntekter": [
                  { "maned": "2022-12", "sum": 23000.0 }
                ]
              }
            ]
            """.trimIndent(),
            response.bodyAsText,
        )
    }

    @Test
    fun `gir tom liste når personen ikke har inntekter fra a-ordningen`() {
        // Given:
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val personPseudoId = personPseudoIdProvider.nyPersonPseudoId(person.id)

        // When:
        val response = integrationTestFixture.get("/api/personer/${personPseudoId.value}/inntekter-fra-aordningen")

        // Then:
        assertEquals(200, response.status)
        assertJsonEquals("[]", response.bodyAsText)
    }

    @Test
    fun `gir 404 når pseudoId ikke finnes`() {
        val response = integrationTestFixture.get("/api/personer/${PersonPseudoId.ny().value}/inntekter-fra-aordningen")

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
        val response = integrationTestFixture.get("/api/personer/${personPseudoId.value}/inntekter-fra-aordningen")

        // Then:
        assertEquals(403, response.status)
    }

    @Test
    fun `krever lesetilgang`() {
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val personPseudoId = personPseudoIdProvider.nyPersonPseudoId(person.id)

        val response =
            integrationTestFixture.get(
                url = "/api/personer/${personPseudoId.value}/inntekter-fra-aordningen",
                tilganger = emptySet<Tilgang>(),
            )

        assertEquals(403, response.status)
    }
}
