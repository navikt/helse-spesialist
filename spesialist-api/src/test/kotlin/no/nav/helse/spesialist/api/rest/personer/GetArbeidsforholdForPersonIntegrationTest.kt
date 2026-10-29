package no.nav.helse.spesialist.api.rest.personer

import com.github.navikt.tbd_libs.populasjonstilgang.api.TilgangSomMangler
import com.github.navikt.tbd_libs.populasjonstilgang.api.TilgangskontrollResultat
import no.nav.helse.modell.KomplettArbeidsforholdDto
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.application.testing.assertJsonEquals
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class GetArbeidsforholdForPersonIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val sessionContext = integrationTestFixture.sessionContext
    private val personPseudoIdProvider = integrationTestFixture.personPseudoIdProvider

    @Test
    fun `gir arbeidsforholdene til personen med organisasjonsnummer`() {
        // Given:
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val annenPerson = lagPerson().also(sessionContext.personRepository::lagre)
        val personPseudoId = personPseudoIdProvider.nyPersonPseudoId(person.id)
        lagreArbeidsforhold(person.id.value, "111111111", "Utvikler", 100, LocalDate.parse("2020-01-01"), null)
        lagreArbeidsforhold(person.id.value, "222222222", "Tester", 40, LocalDate.parse("2018-01-01"), LocalDate.parse("2019-12-31"))
        lagreArbeidsforhold(annenPerson.id.value, "111111111", "Sjef", 100, LocalDate.parse("2020-01-01"), null)

        // When:
        val response = integrationTestFixture.get("/api/personer/${personPseudoId.value}/arbeidsforhold")

        // Then:
        assertEquals(200, response.status)
        assertJsonEquals(
            """
            [
              {
                "organisasjonsnummer": "111111111",
                "stillingstittel": "Utvikler",
                "stillingsprosent": 100,
                "startdato": "2020-01-01",
                "sluttdato": null
              },
              {
                "organisasjonsnummer": "222222222",
                "stillingstittel": "Tester",
                "stillingsprosent": 40,
                "startdato": "2018-01-01",
                "sluttdato": "2019-12-31"
              }
            ]
            """.trimIndent(),
            response.bodyAsText,
        )
    }

    @Test
    fun `gir tom liste når personen ikke har arbeidsforhold`() {
        // Given:
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val personPseudoId = personPseudoIdProvider.nyPersonPseudoId(person.id)

        // When:
        val response = integrationTestFixture.get("/api/personer/${personPseudoId.value}/arbeidsforhold")

        // Then:
        assertEquals(200, response.status)
        assertJsonEquals("[]", response.bodyAsText)
    }

    @Test
    fun `gir 404 når pseudoId ikke finnes`() {
        val response = integrationTestFixture.get("/api/personer/${PersonPseudoId.ny().value}/arbeidsforhold")

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
        val response = integrationTestFixture.get("/api/personer/${personPseudoId.value}/arbeidsforhold")

        // Then:
        assertEquals(403, response.status)
    }

    @Test
    fun `krever lesetilgang`() {
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val personPseudoId = personPseudoIdProvider.nyPersonPseudoId(person.id)

        val response =
            integrationTestFixture.get(
                url = "/api/personer/${personPseudoId.value}/arbeidsforhold",
                tilganger = emptySet<Tilgang>(),
            )

        assertEquals(403, response.status)
    }

    private fun lagreArbeidsforhold(
        fødselsnummer: String,
        organisasjonsnummer: String,
        stillingstittel: String,
        stillingsprosent: Int,
        startdato: LocalDate,
        sluttdato: LocalDate?,
    ) {
        sessionContext.arbeidsforholdDao.upsertArbeidsforhold(
            fødselsnummer,
            organisasjonsnummer,
            listOf(
                KomplettArbeidsforholdDto(
                    fødselsnummer = fødselsnummer,
                    organisasjonsnummer = organisasjonsnummer,
                    startdato = startdato,
                    sluttdato = sluttdato,
                    stillingstittel = stillingstittel,
                    stillingsprosent = stillingsprosent,
                ),
            ),
        )
    }
}
