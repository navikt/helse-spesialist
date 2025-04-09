package no.nav.helse.spesialist.e2etests.tests

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.jackson.asUUID
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
        val dager = fom.datesUntil(tom.plusDays(1)).toList()
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
            val tilkomneInntektskilder = person["tilkomneInntektskilder"]
            assertEquals(1, tilkomneInntektskilder.size())
            assertTilkommenInntektskilde(
                tilkommenInntektskilde = tilkomneInntektskilder[0],
                expectedOrganisasjonsnummer = organisasjonsnummer,
                expectedFom = fom,
                expectedTom = tom,
                expectedPeriodebeløp = periodebeløp,
                expectedDager = dager,
                expectedFjernet = false
            )
            val events = tilkomneInntektskilder[0]["inntekter"][0]["events"]
            assertEquals(1, events.size())
            assertTilkommenInntektOpprettetEvent(
                event = events[0],
                expectedSaksbehandlerIdent = saksbehandlerIdent(),
                expectedNotatTilBeslutter = notatTilBeslutter,
                expectedOrganisasjonsnummer = organisasjonsnummer,
                expectedFom = fom,
                expectedTom = tom,
                expectedPeriodebeløp = periodebeløp,
                expectedDager = dager
            )
        }
    }

    @Test
    fun `saksbehandler endrer tilkommen inntekt`() {
        // Given:
        val vedtaksperiode = førsteVedtaksperiode()
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        søknadOgGodkjenningbehovKommerInn()
        val opprinneligOrganisasjonsnummer = lagOrganisasjonsnummer()
        val opprinneligFom = vedtaksperiode.fom.plusDays(1)
        val opprinneligTom = vedtaksperiode.tom
        val opprinneligPeriodebeløp = BigDecimal("2000")
        val opprinneligeDager = opprinneligFom.datesUntil(opprinneligTom.plusDays(1)).toList()
        medPersonISpeil {
            saksbehandlerLeggerTilTilkommenInntekt(
                organisasjonsnummer = opprinneligOrganisasjonsnummer,
                fom = opprinneligFom,
                tom = opprinneligTom,
                periodebeløp = opprinneligPeriodebeløp,
                dager = opprinneligeDager,
                notatTilBeslutter = "notat"
            )
        }

        // When:
        val endringOrganisasjonsnummer = lagOrganisasjonsnummer()
        val endringFom = opprinneligFom.plusDays(1)
        val endringTom = opprinneligTom.minusDays(1)
        val endringPeriodebeløp = BigDecimal("1337")
        val endringDager = endringFom.datesUntil(endringTom.plusDays(1))
            .filter { it.toEpochDay() % 2 == 0L }.toList()
        val endringNotatTilBeslutterFor = "endring i gang"
        medPersonISpeil {
            saksbehandlerEndrerTilkommenInntekt(
                uuid = person["tilkomneInntektskilder"][0]["inntekter"][0]["tilkommenInntektId"].asUUID(),
                organisasjonsnummer = endringOrganisasjonsnummer,
                fom = endringFom,
                tom = endringTom,
                periodebeløp = endringPeriodebeløp,
                dager = endringDager,
                notatTilBeslutter = endringNotatTilBeslutterFor
            )
        }

        // Then:
        medPersonISpeil {
            val tilkomneInntektskilder = person["tilkomneInntektskilder"]
            assertEquals(1, tilkomneInntektskilder.size())
            assertTilkommenInntektskilde(
                tilkommenInntektskilde = tilkomneInntektskilder[0],
                expectedOrganisasjonsnummer = endringOrganisasjonsnummer,
                expectedFom = endringFom,
                expectedTom = endringTom,
                expectedPeriodebeløp = endringPeriodebeløp,
                expectedDager = endringDager,
                expectedFjernet = false
            )
            val events = tilkomneInntektskilder[0]["inntekter"][0]["events"]
            assertEquals(2, events.size())
            assertTilkommenInntektEndretEvent(
                event = events[1],
                expectedSekvensnummer = 2,
                expectedSaksbehandlerIdent = saksbehandlerIdent(),
                expectedNotatTilBeslutter = endringNotatTilBeslutterFor,
                expectedOrganisasjonsnummerFra = opprinneligOrganisasjonsnummer,
                expectedOrganisasjonsnummerTil = endringOrganisasjonsnummer,
                expectedFomFra = opprinneligFom,
                expectedFomTil = endringFom,
                expectedTomFra = opprinneligTom,
                expectedTomTil = endringTom,
                expectedPeriodebeløpFra = opprinneligPeriodebeløp,
                expectedPeriodebeløpTil = endringPeriodebeløp,
                expectedDagerFra = opprinneligeDager,
                expectedDagerTil = endringDager
            )
        }
    }

    @Test
    fun `saksbehandler fjerner tilkommen inntekt`() {
        // Given:
        val vedtaksperiode = førsteVedtaksperiode()
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        søknadOgGodkjenningbehovKommerInn()
        val opprinneligOrganisasjonsnummer = lagOrganisasjonsnummer()
        val opprinneligFom = vedtaksperiode.fom.plusDays(1)
        val opprinneligTom = vedtaksperiode.tom
        val opprinneligPeriodebeløp = BigDecimal("2000")
        val opprinneligeDager = opprinneligFom.datesUntil(opprinneligTom.plusDays(1)).toList()
        medPersonISpeil {
            saksbehandlerLeggerTilTilkommenInntekt(
                organisasjonsnummer = opprinneligOrganisasjonsnummer,
                fom = opprinneligFom,
                tom = opprinneligTom,
                periodebeløp = opprinneligPeriodebeløp,
                dager = opprinneligeDager,
                notatTilBeslutter = "notat"
            )
        }

        // When:
        val fjerningNotatTilBeslutterFor = "fjerner inntekten"
        medPersonISpeil {
            saksbehandlerFjernerTilkommenInntekt(
                uuid = person["tilkomneInntektskilder"][0]["inntekter"][0]["tilkommenInntektId"].asUUID(),
                notatTilBeslutter = fjerningNotatTilBeslutterFor
            )
        }

        // Then:
        medPersonISpeil {
            val tilkomneInntektskilder = person["tilkomneInntektskilder"]
            assertEquals(1, tilkomneInntektskilder.size())
            assertTilkommenInntektskilde(
                tilkommenInntektskilde = tilkomneInntektskilder[0],
                expectedOrganisasjonsnummer = opprinneligOrganisasjonsnummer,
                expectedFom = opprinneligFom,
                expectedTom = opprinneligTom,
                expectedPeriodebeløp = opprinneligPeriodebeløp,
                expectedDager = opprinneligeDager,
                expectedFjernet = true
            )
            val events = tilkomneInntektskilder[0]["inntekter"][0]["events"]
            assertEquals(2, events.size())
            assertTilkommenInntektFjernetEvent(
                event = events[1],
                expectedSekvensnummer = 2,
                expectedSaksbehandlerIdent = saksbehandlerIdent(),
                expectedNotatTilBeslutter = fjerningNotatTilBeslutterFor
            )
        }
    }

    @Test
    fun `saksbehandler gjenoppretter tilkommen inntekt`() {
        // Given:
        val vedtaksperiode = førsteVedtaksperiode()
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        søknadOgGodkjenningbehovKommerInn()
        val opprinneligOrganisasjonsnummer = lagOrganisasjonsnummer()
        val opprinneligFom = vedtaksperiode.fom.plusDays(1)
        val opprinneligTom = vedtaksperiode.tom
        val opprinneligPeriodebeløp = BigDecimal("2000")
        val opprinneligeDager = opprinneligFom.datesUntil(opprinneligTom.plusDays(1)).toList()
        medPersonISpeil {
            saksbehandlerLeggerTilTilkommenInntekt(
                organisasjonsnummer = opprinneligOrganisasjonsnummer,
                fom = opprinneligFom,
                tom = opprinneligTom,
                periodebeløp = opprinneligPeriodebeløp,
                dager = opprinneligeDager,
                notatTilBeslutter = "notat"
            )
        }
        medPersonISpeil {
            saksbehandlerFjernerTilkommenInntekt(
                uuid = person["tilkomneInntektskilder"][0]["inntekter"][0]["tilkommenInntektId"].asUUID(),
                notatTilBeslutter = "fjerner inntekten"
            )
        }

        // When:
        val gjenopprettingOrganisasjonsnummer = lagOrganisasjonsnummer()
        val gjenopprettingFom = opprinneligFom.plusDays(1)
        val gjenopprettingTom = opprinneligTom.minusDays(1)
        val gjenopprettingPeriodebeløp = BigDecimal("1337")
        val gjenopprettingDager = gjenopprettingFom.datesUntil(gjenopprettingTom.plusDays(1))
            .filter { it.toEpochDay() % 2 == 0L }.toList()
        val gjenopprettingNotatTilBeslutter = "gjenoppretter etter feilaktig fjerning"
        medPersonISpeil {
            saksbehandlerGjenoppretterTilkommenInntekt(
                uuid = person["tilkomneInntektskilder"][0]["inntekter"][0]["tilkommenInntektId"].asUUID(),
                organisasjonsnummer = gjenopprettingOrganisasjonsnummer,
                fom = gjenopprettingFom,
                tom = gjenopprettingTom,
                periodebeløp = gjenopprettingPeriodebeløp,
                dager = gjenopprettingDager,
                notatTilBeslutter = gjenopprettingNotatTilBeslutter
            )
        }

        // Then:
        medPersonISpeil {
            val tilkomneInntektskilder = person["tilkomneInntektskilder"]
            assertEquals(1, tilkomneInntektskilder.size())
            assertTilkommenInntektskilde(
                tilkommenInntektskilde = tilkomneInntektskilder[0],
                expectedOrganisasjonsnummer = gjenopprettingOrganisasjonsnummer,
                expectedFom = gjenopprettingFom,
                expectedTom = gjenopprettingTom,
                expectedPeriodebeløp = gjenopprettingPeriodebeløp,
                expectedDager = gjenopprettingDager,
                expectedFjernet = false
            )
            val events = tilkomneInntektskilder[0]["inntekter"][0]["events"]
            assertEquals(3, events.size())
            assertTilkommenInntektGjenopprettetEvent(
                event = events[2],
                expectedSekvensnummer = 3,
                expectedSaksbehandlerIdent = saksbehandlerIdent(),
                expectedNotatTilBeslutter = gjenopprettingNotatTilBeslutter,
                expectedOrganisasjonsnummerFra = opprinneligOrganisasjonsnummer,
                expectedOrganisasjonsnummerTil = gjenopprettingOrganisasjonsnummer,
                expectedFomFra = opprinneligFom,
                expectedFomTil = gjenopprettingFom,
                expectedTomFra = opprinneligTom,
                expectedTomTil = gjenopprettingTom,
                expectedPeriodebeløpFra = opprinneligPeriodebeløp,
                expectedPeriodebeløpTil = gjenopprettingPeriodebeløp,
                expectedDagerFra = opprinneligeDager,
                expectedDagerTil = gjenopprettingDager
            )
        }
    }

    private fun assertTilkommenInntektskilde(
        tilkommenInntektskilde: JsonNode,
        expectedOrganisasjonsnummer: String,
        expectedFom: LocalDate,
        expectedTom: LocalDate,
        expectedPeriodebeløp: BigDecimal,
        expectedDager: List<LocalDate>,
        expectedFjernet: Boolean
    ) {
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
        assertEquals("TilkommenInntektOpprettetEvent", event["__typename"].asText())
        assertTilkommenInntektEventMetadata(
            metadata = event["metadata"],
            expectedSekvensnummer = 1,
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

    private fun assertTilkommenInntektEndretEvent(
        event: JsonNode,
        expectedSekvensnummer: Int,
        expectedSaksbehandlerIdent: String,
        expectedNotatTilBeslutter: String,
        expectedOrganisasjonsnummerFra: String,
        expectedOrganisasjonsnummerTil: String,
        expectedFomFra: LocalDate,
        expectedFomTil: LocalDate,
        expectedTomFra: LocalDate,
        expectedTomTil: LocalDate,
        expectedPeriodebeløpFra: BigDecimal,
        expectedPeriodebeløpTil: BigDecimal,
        expectedDagerFra: List<LocalDate>,
        expectedDagerTil: List<LocalDate>,
    ) {
        assertEquals("TilkommenInntektEndretEvent", event["__typename"].asText())
        assertTilkommenInntektEventMetadata(
            metadata = event["metadata"],
            expectedSekvensnummer = expectedSekvensnummer,
            expectedSaksbehandlerIdent = expectedSaksbehandlerIdent,
            expectedNotatTilBeslutter = expectedNotatTilBeslutter
        )
        val endringer = event["endringer"]
        assertEquals(expectedOrganisasjonsnummerFra, endringer["organisasjonsnummer"]["fra"].asText())
        assertEquals(expectedOrganisasjonsnummerTil, endringer["organisasjonsnummer"]["til"].asText())
        assertEquals(expectedFomFra.toString(), endringer["fom"]["fra"].asText())
        assertEquals(expectedFomTil.toString(), endringer["fom"]["til"].asText())
        assertEquals(expectedTomFra.toString(), endringer["tom"]["fra"].asText())
        assertEquals(expectedTomTil.toString(), endringer["tom"]["til"].asText())
        assertEquals(expectedPeriodebeløpFra.toString(), endringer["periodebelop"]["fra"].asText())
        assertEquals(expectedPeriodebeløpTil.toString(), endringer["periodebelop"]["til"].asText())
        assertEquals(expectedDagerFra.size, endringer["dager"]["fra"].size())
        assertEquals(expectedDagerTil.size, endringer["dager"]["til"].size())
        assertEquals(expectedDagerFra.map(LocalDate::toString), endringer["dager"]["fra"].map(JsonNode::asText))
        assertEquals(expectedDagerTil.map(LocalDate::toString), endringer["dager"]["til"].map(JsonNode::asText))
    }

    private fun assertTilkommenInntektFjernetEvent(
        event: JsonNode,
        expectedSekvensnummer: Int,
        expectedSaksbehandlerIdent: String,
        expectedNotatTilBeslutter: String,
    ) {
        assertEquals("TilkommenInntektFjernetEvent", event["__typename"].asText())
        assertTilkommenInntektEventMetadata(
            metadata = event["metadata"],
            expectedSekvensnummer = expectedSekvensnummer,
            expectedSaksbehandlerIdent = expectedSaksbehandlerIdent,
            expectedNotatTilBeslutter = expectedNotatTilBeslutter
        )
    }

    private fun assertTilkommenInntektGjenopprettetEvent(
        event: JsonNode,
        expectedSekvensnummer: Int,
        expectedSaksbehandlerIdent: String,
        expectedNotatTilBeslutter: String,
        expectedOrganisasjonsnummerFra: String,
        expectedOrganisasjonsnummerTil: String,
        expectedFomFra: LocalDate,
        expectedFomTil: LocalDate,
        expectedTomFra: LocalDate,
        expectedTomTil: LocalDate,
        expectedPeriodebeløpFra: BigDecimal,
        expectedPeriodebeløpTil: BigDecimal,
        expectedDagerFra: List<LocalDate>,
        expectedDagerTil: List<LocalDate>,
    ) {
        assertEquals("TilkommenInntektGjenopprettetEvent", event["__typename"].asText())
        assertTilkommenInntektEventMetadata(
            metadata = event["metadata"],
            expectedSekvensnummer = expectedSekvensnummer,
            expectedSaksbehandlerIdent = expectedSaksbehandlerIdent,
            expectedNotatTilBeslutter = expectedNotatTilBeslutter
        )
        val endringer = event["endringer"]
        assertEquals(expectedOrganisasjonsnummerFra, endringer["organisasjonsnummer"]["fra"].asText())
        assertEquals(expectedOrganisasjonsnummerTil, endringer["organisasjonsnummer"]["til"].asText())
        assertEquals(expectedFomFra.toString(), endringer["fom"]["fra"].asText())
        assertEquals(expectedFomTil.toString(), endringer["fom"]["til"].asText())
        assertEquals(expectedTomFra.toString(), endringer["tom"]["fra"].asText())
        assertEquals(expectedTomTil.toString(), endringer["tom"]["til"].asText())
        assertEquals(expectedPeriodebeløpFra.toString(), endringer["periodebelop"]["fra"].asText())
        assertEquals(expectedPeriodebeløpTil.toString(), endringer["periodebelop"]["til"].asText())
        assertEquals(expectedDagerFra.size, endringer["dager"]["fra"].size())
        assertEquals(expectedDagerTil.size, endringer["dager"]["til"].size())
        assertEquals(expectedDagerFra.map(LocalDate::toString), endringer["dager"]["fra"].map(JsonNode::asText))
        assertEquals(expectedDagerTil.map(LocalDate::toString), endringer["dager"]["til"].map(JsonNode::asText))
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
