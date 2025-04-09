package no.nav.helse.spesialist.e2etests.tests

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer
import no.nav.helse.spesialist.e2etests.AbstractE2EIntegrationTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.test.assertEquals

class TilkommenInntektE2ETest : AbstractE2EIntegrationTest() {
    @Test
    fun `saksbehandler legger til tilkommen inntekt`() {
        // Given:
        val organisasjonsnummer = lagOrganisasjonsnummer()
        val vedtaksperiode = førsteVedtaksperiode()
        val fom = vedtaksperiode.fom.plusDays(1)
        val tom = vedtaksperiode.tom
        val dager = fom
            .datesUntil(tom.plusDays(1))
            .toList()
        val periodebeløp = BigDecimal("2000")
        val notatTilBeslutter = "notat"
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        søknadOgGodkjenningbehovKommerInn()

        // When:
        medPersonISpeil {
            saksbehandlerLeggerTilTilkommenInntekt(
                organisasjonsnummer = organisasjonsnummer,
                fom = fom,
                tom = tom,
                periodebeløp = periodebeløp,
                dager = dager,
                notatTilBeslutter = notatTilBeslutter
            )
        }

        // Then:
        medPersonISpeil {
            assertTilkomneInntektskilder(
                tilkomneInntektskilder = person["tilkomneInntektskilder"],
                expectedOrganisasjonsnummer = organisasjonsnummer,
                expectedFom = fom,
                expectedTom = tom,
                expectedPeriodebeløp = periodebeløp,
                expectedDager = dager,
                expectedFjernet = false,
                expectedSaksbehandlerIdent = saksbehandlerIdent(),
                expectedNotatTilBeslutter = notatTilBeslutter
            )
        }
    }

    private fun assertTilkomneInntektskilder(
        tilkomneInntektskilder: JsonNode,
        expectedOrganisasjonsnummer: String,
        expectedFom: LocalDate,
        expectedTom: LocalDate,
        expectedPeriodebeløp: BigDecimal,
        expectedDager: List<LocalDate>,
        expectedFjernet: Boolean,
        expectedSaksbehandlerIdent: String,
        expectedNotatTilBeslutter: String
    ) {
        assertEquals(1, tilkomneInntektskilder.size())
        tilkomneInntektskilder[0].let { tilkommenInntektskilde ->
            assertEquals(expectedOrganisasjonsnummer, tilkommenInntektskilde["organisasjonsnummer"].asText())
            // TODO: Assert faktisk organisasjonsnavn når det hentes ordentlig
            assertEquals(expectedOrganisasjonsnummer, tilkommenInntektskilde["organisasjonsnavn"].asText())
            tilkommenInntektskilde["inntekter"].let { inntekter ->
                assertEquals(1, inntekter.size())
                inntekter[0].let { inntekt ->
                    assertInntekt(
                        inntekt = inntekt,
                        expectedFom = expectedFom,
                        expectedTom = expectedTom,
                        expectedPeriodebeløp = expectedPeriodebeløp,
                        expectedDager = expectedDager,
                        expectedFjernet = expectedFjernet
                    )
                    inntekt["events"].let { events ->
                        assertEquals(1, events.size())
                        assertTilkommenInntektOpprettetEvent(
                            event = events[0],
                            expectedSaksbehandlerIdent = expectedSaksbehandlerIdent,
                            expectedNotatTilBeslutter = expectedNotatTilBeslutter,
                            expectedOrganisasjonsnummer = expectedOrganisasjonsnummer,
                            expectedFom = expectedFom,
                            expectedTom = expectedTom,
                            expectedPeriodebeløp = expectedPeriodebeløp,
                            expectedDager = expectedDager
                        )
                    }
                }
            }
        }
    }

    private fun assertInntekt(
        inntekt: JsonNode,
        expectedFom: LocalDate,
        expectedTom: LocalDate,
        expectedPeriodebeløp: BigDecimal,
        expectedDager: List<LocalDate>,
        expectedFjernet: Boolean
    ) {
        assertEquals(expectedFom.toString(), inntekt["fom"].asText())
        assertEquals(expectedTom.toString(), inntekt["tom"].asText())
        assertEquals(expectedPeriodebeløp.toString(), inntekt["periodebelop"].asText())
        assertEquals(expectedDager.size, inntekt["dager"].size())
        assertEquals(expectedDager.map(LocalDate::toString), inntekt["dager"].map(JsonNode::asText))
        assertEquals(expectedFjernet, inntekt["fjernet"].asBoolean())
    }

    private fun assertTilkommenInntektOpprettetEvent(
        event: JsonNode,
        expectedSaksbehandlerIdent: String,
        expectedNotatTilBeslutter: String,
        expectedOrganisasjonsnummer: String,
        expectedFom: LocalDate,
        expectedTom: LocalDate,
        expectedPeriodebeløp: BigDecimal,
        expectedDager: List<LocalDate>
    ) {
        val expectedSekvensnummer = 1
        assertTilkommenInntektEventMetadata(
            metadata = event["metadata"],
            expectedSekvensnummer = expectedSekvensnummer,
            expectedSaksbehandlerIdent = expectedSaksbehandlerIdent,
            expectedNotatTilBeslutter = expectedNotatTilBeslutter
        )
        assertEquals(expectedOrganisasjonsnummer, event["organisasjonsnummer"].asText())
        assertEquals(expectedFom.toString(), event["fom"].asText())
        assertEquals(expectedTom.toString(), event["tom"].asText())
        assertEquals(expectedPeriodebeløp.toString(), event["periodebelop"].asText())
        assertEquals(expectedDager.size, event["dager"].size())
        assertEquals(expectedDager.map(LocalDate::toString), event["dager"].map(JsonNode::asText))
    }

    private fun assertTilkommenInntektEventMetadata(
        metadata: JsonNode,
        expectedSekvensnummer: Int,
        expectedSaksbehandlerIdent: String,
        expectedNotatTilBeslutter: String
    ) {
        assertEquals(expectedSekvensnummer, metadata["sekvensnummer"].asInt())
        val eventTidspunkt = metadata["tidspunkt"].asText().let(LocalDateTime::parse).somInstantIOslo()
        assertInnenforFemSekunderSiden(eventTidspunkt = eventTidspunkt, navnPåTidspunkt = "tidspunkt på event")
        assertEquals(expectedSaksbehandlerIdent, metadata["utfortAvSaksbehandlerIdent"].asText())
        assertEquals(expectedNotatTilBeslutter, metadata["notatTilBeslutter"].asText())
    }

    private fun LocalDateTime.somInstantIOslo(): Instant =
        atZone(ZoneId.of("Europe/Oslo")).toInstant()

    private fun assertInnenforFemSekunderSiden(eventTidspunkt: Instant, navnPåTidspunkt: String) {
        val now = Instant.now()
        assertTrue(eventTidspunkt.isBefore(now)) {
            "Forventet at $navnPåTidspunkt ($eventTidspunkt) var før nå ($now)"
        }
        assertTrue(eventTidspunkt.isAfter(now.minusSeconds(5))) {
            "Forventet at $navnPåTidspunkt ($eventTidspunkt) var senere enn fem sekunder før nå ($now)"
        }
    }
}
