package no.nav.helse.spesialist.api.rest.personer

import com.github.navikt.tbd_libs.populasjonstilgang.api.TilgangSomMangler
import com.github.navikt.tbd_libs.populasjonstilgang.api.TilgangskontrollResultat
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.api.overstyring.Dagtype
import no.nav.helse.spesialist.api.overstyring.OverstyringArbeidsforholdDto
import no.nav.helse.spesialist.api.overstyring.OverstyringDagDto
import no.nav.helse.spesialist.api.overstyring.OverstyringTidslinjeDto
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.application.testing.assertJsonEquals
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class GetOverstyringerForPersonIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val sessionContext = integrationTestFixture.sessionContext
    private val personPseudoIdProvider = integrationTestFixture.personPseudoIdProvider

    @Test
    fun `gir overstyringene til personen med organisasjonsnummer`() {
        // Given:
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val annenPerson = lagPerson().also(sessionContext.personRepository::lagre)
        val personPseudoId = personPseudoIdProvider.nyPersonPseudoId(person.id)
        val dagoverstyringHendelseId = UUID.randomUUID()
        val arbeidsforholdHendelseId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        sessionContext.overstyringDao.overstyringer.addAll(
            listOf(
                OverstyringTidslinjeDto(
                    hendelseId = dagoverstyringHendelseId,
                    fødselsnummer = person.id.value,
                    organisasjonsnummer = "111111111",
                    begrunnelse = "En begrunnelse",
                    timestamp = LocalDateTime.parse("2024-01-10T12:00:00"),
                    saksbehandlerNavn = "Saks Behandler",
                    saksbehandlerIdent = "S123456",
                    overstyrteDager =
                        listOf(
                            OverstyringDagDto(
                                dato = LocalDate.parse("2024-01-01"),
                                type = Dagtype.Feriedag,
                                fraType = Dagtype.Sykedag,
                                grad = null,
                                fraGrad = 100,
                            ),
                        ),
                    ferdigstilt = false,
                    vedtaksperiodeId = vedtaksperiodeId,
                ),
                OverstyringArbeidsforholdDto(
                    hendelseId = arbeidsforholdHendelseId,
                    fødselsnummer = person.id.value,
                    organisasjonsnummer = "222222222",
                    begrunnelse = "Begrunnelse",
                    forklaring = "Forklaring",
                    timestamp = LocalDateTime.parse("2024-01-11T12:00:00"),
                    saksbehandlerNavn = "Saks Behandler",
                    saksbehandlerIdent = "S123456",
                    deaktivert = true,
                    skjæringstidspunkt = LocalDate.parse("2024-01-01"),
                    ferdigstilt = true,
                    vedtaksperiodeId = vedtaksperiodeId,
                ),
                OverstyringArbeidsforholdDto(
                    hendelseId = UUID.randomUUID(),
                    fødselsnummer = annenPerson.id.value,
                    organisasjonsnummer = "111111111",
                    begrunnelse = "Begrunnelse",
                    forklaring = "Forklaring",
                    timestamp = LocalDateTime.parse("2024-01-11T12:00:00"),
                    saksbehandlerNavn = "Saks Behandler",
                    saksbehandlerIdent = "S123456",
                    deaktivert = true,
                    skjæringstidspunkt = LocalDate.parse("2024-01-01"),
                    ferdigstilt = true,
                    vedtaksperiodeId = UUID.randomUUID(),
                ),
            ),
        )

        // When:
        val response = integrationTestFixture.get("/api/personer/${personPseudoId.value}/overstyringer")

        // Then:
        assertEquals(200, response.status)
        assertJsonEquals(
            """
            {
              "overstyringer": [
              {
                "type": "ApiDagoverstyring",
                "hendelseId": "$dagoverstyringHendelseId",
                "timestamp": "2024-01-10T12:00:00",
                "saksbehandler": { "navn": "Saks Behandler", "ident": "S123456" },
                "ferdigstilt": false,
                "dager": [
                  { "dato": "2024-01-01", "type": "Feriedag", "fraType": "Sykedag", "grad": null, "fraGrad": 100 }
                ],
                "begrunnelse": "En begrunnelse",
                "vedtaksperiodeId": "$vedtaksperiodeId",
                "organisasjonsnummer": "111111111"
              },
              {
                "type": "ApiArbeidsforholdoverstyring",
                "hendelseId": "$arbeidsforholdHendelseId",
                "timestamp": "2024-01-11T12:00:00",
                "saksbehandler": { "navn": "Saks Behandler", "ident": "S123456" },
                "ferdigstilt": true,
                "deaktivert": true,
                "skjaeringstidspunkt": "2024-01-01",
                "forklaring": "Forklaring",
                "begrunnelse": "Begrunnelse",
                "vedtaksperiodeId": "$vedtaksperiodeId",
                "organisasjonsnummer": "222222222"
              }
              ]
            }
            """.trimIndent(),
            response.bodyAsText,
        )
    }

    @Test
    fun `gir tom liste når personen ikke har overstyringer`() {
        // Given:
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val personPseudoId = personPseudoIdProvider.nyPersonPseudoId(person.id)

        // When:
        val response = integrationTestFixture.get("/api/personer/${personPseudoId.value}/overstyringer")

        // Then:
        assertEquals(200, response.status)
        assertJsonEquals("""{ "overstyringer": [] }""", response.bodyAsText)
    }

    @Test
    fun `gir 404 når pseudoId ikke finnes`() {
        val response = integrationTestFixture.get("/api/personer/${PersonPseudoId.ny().value}/overstyringer")

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
        val response = integrationTestFixture.get("/api/personer/${personPseudoId.value}/overstyringer")

        // Then:
        assertEquals(403, response.status)
    }

    @Test
    fun `krever lesetilgang`() {
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val personPseudoId = personPseudoIdProvider.nyPersonPseudoId(person.id)

        val response =
            integrationTestFixture.get(
                url = "/api/personer/${personPseudoId.value}/overstyringer",
                tilganger = emptySet<Tilgang>(),
            )

        assertEquals(403, response.status)
    }
}
