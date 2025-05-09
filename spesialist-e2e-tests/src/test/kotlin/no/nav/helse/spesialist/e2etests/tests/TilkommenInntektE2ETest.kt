package no.nav.helse.spesialist.e2etests.tests

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.spesialist.domain.Periode.Companion.tilPerioder
import no.nav.helse.spesialist.domain.testfixtures.feb
import no.nav.helse.spesialist.domain.testfixtures.jan
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
        førsteVedtaksperiode().apply {
            fom = 1 jan 2021
            tom = 31 jan 2021
        }
        val organisasjonsnummer = lagOrganisasjonsnummer()
        val fom = 2 jan 2021
        val tom = 31 jan 2021
        val dager = (2 jan 2021).datesUntil(1 feb 2021).toList()
        val periodebeløp = BigDecimal("1111.11")
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

            // Then:
            val tilkomneInntektskilder = hentTilkomneInntektskilder()
            assertEquals(1, tilkomneInntektskilder.size())
            assertApiInntektskilde(
                tilkommenInntektskilde = tilkomneInntektskilder[0],
                expectedOrganisasjonsnummer = organisasjonsnummer,
                expectedFom = fom,
                expectedTom = tom,
                expectedPeriodebeløp = periodebeløp,
                expectedDager = dager,
                expectedFjernet = false
            )
            val events = tilkomneInntektskilder[0]["inntekter"][0]["events"]
            assertEventTypenamesAndMetadata(
                events = events,
                expectedTypenameNotatTilBeslutterPairs = listOf(
                    "TilkommenInntektOpprettetEvent" to notatTilBeslutter,
                )
            )
            val opprettetEvent = events.last()
            assertEquals(organisasjonsnummer, opprettetEvent["organisasjonsnummer"].asText())
            assertEquals(fom.toString(), opprettetEvent["periode"]["fom"].asText())
            assertEquals(tom.toString(), opprettetEvent["periode"]["tom"].asText())
            assertEquals(periodebeløp.toString(), opprettetEvent["periodebelop"].asText())
            assertEquals(dager.size, opprettetEvent["dager"].size())
            assertEquals(dager.map(LocalDate::toString), opprettetEvent["dager"].map(JsonNode::asText))
        }

        val inntektsendringerMelding = meldinger().last { it["@event_name"].asText() == "inntektsendringer" }
        assertEquals(fødselsnummer(), inntektsendringerMelding["fødselsnummer"].asText())
        assertEquals(1, inntektsendringerMelding["inntektsendringer"].size())
        assertInntektsendringerInntektskilde(
            inntektskilde = inntektsendringerMelding["inntektsendringer"][0],
            expectedOrganisasjonsnummer = organisasjonsnummer,
            expectedInntekter = listOf(Triple(fom.toString(), tom.toString(), 37.037)),
            expectedNullstillinger = emptyList()
        )
    }

    @Test
    fun `saksbehandler endrer tilkommen inntekt`() {
        // Given:
        val opprinneligOrganisasjonsnummer = lagOrganisasjonsnummer()
        val opprinneligFom = 2 jan 2021
        val opprinneligTom = 31 jan 2021
        val opprinneligPeriodebeløp = BigDecimal("1111.11")
        val opprinneligeDager = (2 jan 2021).datesUntil(1 feb 2021).toList()

        val endringOrganisasjonsnummer = lagOrganisasjonsnummer()
        val endringFom = 3 jan 2021
        val endringTom = 30 jan 2021
        val endringPeriodebeløp = BigDecimal("2222.22")
        val endringDager = (3 jan 2021).datesUntil(31 jan 2021)
            .filter { it.toEpochDay() % 2 == 0L }.toList()
        val endringNotatTilBeslutter = "endring i gang"

        førsteVedtaksperiode().apply {
            fom = 1 jan 2021
            tom = 31 jan 2021
        }
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        søknadOgGodkjenningbehovKommerInn()

        medPersonISpeil {
            val tilkommenInntektId = saksbehandlerLeggerTilTilkommenInntekt(
                organisasjonsnummer = opprinneligOrganisasjonsnummer,
                fom = opprinneligFom,
                tom = opprinneligTom,
                periodebeløp = opprinneligPeriodebeløp,
                dager = opprinneligeDager,
                notatTilBeslutter = "notat"
            )

            // When:
            saksbehandlerEndrerTilkommenInntekt(
                tilkommenInntektId = tilkommenInntektId,
                organisasjonsnummer = endringOrganisasjonsnummer,
                fom = endringFom,
                tom = endringTom,
                periodebeløp = endringPeriodebeløp,
                dager = endringDager,
                notatTilBeslutter = endringNotatTilBeslutter
            )

            // Then:
            val tilkomneInntektskilder = hentTilkomneInntektskilder()
            assertEquals(1, tilkomneInntektskilder.size())
            assertApiInntektskilde(
                tilkommenInntektskilde = tilkomneInntektskilder[0],
                expectedOrganisasjonsnummer = endringOrganisasjonsnummer,
                expectedFom = endringFom,
                expectedTom = endringTom,
                expectedPeriodebeløp = endringPeriodebeløp,
                expectedDager = endringDager,
                expectedFjernet = false
            )
            val events = tilkomneInntektskilder[0]["inntekter"][0]["events"]
            assertEventTypenamesAndMetadata(
                events = events,
                expectedTypenameNotatTilBeslutterPairs = listOf(
                    "TilkommenInntektOpprettetEvent" to "notat",
                    "TilkommenInntektEndretEvent" to endringNotatTilBeslutter,
                )
            )
            assertEventEndringer(
                endringer = events.last()["endringer"],
                expectedOrganisasjonsnummerFra = opprinneligOrganisasjonsnummer,
                expectedOrganisasjonsnummerTil = endringOrganisasjonsnummer,
                expectedFraFom = opprinneligFom,
                expectedFraTom = opprinneligTom,
                expectedTilFom = endringFom,
                expectedTilTom = endringTom,
                expectedPeriodebeløpFra = opprinneligPeriodebeløp,
                expectedPeriodebeløpTil = endringPeriodebeløp,
                expectedDagerFra = opprinneligeDager,
                expectedDagerTil = endringDager
            )
        }
        val inntektsendringerMelding = meldinger().last { it["@event_name"].asText() == "inntektsendringer" }
        assertEquals(fødselsnummer(), inntektsendringerMelding["fødselsnummer"].asText())
        assertEquals(2, inntektsendringerMelding["inntektsendringer"].size())
        assertInntektsendringerInntektskilde(
            inntektskilde = inntektsendringerMelding["inntektsendringer"][0],
            expectedOrganisasjonsnummer = opprinneligOrganisasjonsnummer,
            expectedInntekter = emptyList(),
            expectedNullstillinger = listOf(Pair(opprinneligFom.toString(), opprinneligTom.toString())),
        )
        assertInntektsendringerInntektskilde(
            inntektskilde = inntektsendringerMelding["inntektsendringer"][1],
            expectedOrganisasjonsnummer = endringOrganisasjonsnummer,
            expectedInntekter = endringDager.tilPerioder().map { Triple(it.fom.toString(), it.tom.toString(), 158.73) },
            expectedNullstillinger = emptyList()
        )
    }

    @Test
    fun `saksbehandler fjerner tilkommen inntekt`() {
        // Given:
        førsteVedtaksperiode().apply {
            fom = 1 jan 2021
            tom = 31 jan 2021
        }
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        søknadOgGodkjenningbehovKommerInn()
        val opprinneligOrganisasjonsnummer = lagOrganisasjonsnummer()
        val opprinneligFom = 2 jan 2021
        val opprinneligTom = 31 jan 2021
        val opprinneligPeriodebeløp = BigDecimal("1111.11")
        val opprinneligeDager = (2 jan 2021).datesUntil(1 feb 2021).toList()
        medPersonISpeil {
            val tilkommenInntektId = saksbehandlerLeggerTilTilkommenInntekt(
                organisasjonsnummer = opprinneligOrganisasjonsnummer,
                fom = opprinneligFom,
                tom = opprinneligTom,
                periodebeløp = opprinneligPeriodebeløp,
                dager = opprinneligeDager,
                notatTilBeslutter = "notat"
            )

            // When:
            val fjerningNotatTilBeslutter = "fjerner inntekten"
            saksbehandlerFjernerTilkommenInntekt(
                tilkommenInntektId = tilkommenInntektId,
                notatTilBeslutter = fjerningNotatTilBeslutter
            )

            // Then:
            val tilkomneInntektskilder = hentTilkomneInntektskilder()
            assertEquals(1, tilkomneInntektskilder.size())
            assertApiInntektskilde(
                tilkommenInntektskilde = tilkomneInntektskilder[0],
                expectedOrganisasjonsnummer = opprinneligOrganisasjonsnummer,
                expectedFom = opprinneligFom,
                expectedTom = opprinneligTom,
                expectedPeriodebeløp = opprinneligPeriodebeløp,
                expectedDager = opprinneligeDager,
                expectedFjernet = true
            )
            assertEventTypenamesAndMetadata(
                events = tilkomneInntektskilder[0]["inntekter"][0]["events"],
                expectedTypenameNotatTilBeslutterPairs = listOf(
                    "TilkommenInntektOpprettetEvent" to "notat",
                    "TilkommenInntektFjernetEvent" to fjerningNotatTilBeslutter,
                )
            )
        }
        val inntektsendringerMelding = meldinger().last { it["@event_name"].asText() == "inntektsendringer" }
        assertEquals(fødselsnummer(), inntektsendringerMelding["fødselsnummer"].asText())
        assertEquals(1, inntektsendringerMelding["inntektsendringer"].size())
        assertInntektsendringerInntektskilde(
            inntektskilde = inntektsendringerMelding["inntektsendringer"][0],
            expectedOrganisasjonsnummer = opprinneligOrganisasjonsnummer,
            expectedInntekter = emptyList(),
            expectedNullstillinger = listOf(Pair(opprinneligFom.toString(), opprinneligTom.toString())),
        )
    }

    @Test
    fun `saksbehandler gjenoppretter tilkommen inntekt`() {
        // Given:
        val opprinneligOrganisasjonsnummer = lagOrganisasjonsnummer()
        val opprinneligFom = 2 jan 2021
        val opprinneligTom = 31 jan 2021
        val opprinneligPeriodebeløp = BigDecimal("1111.11")
        val opprinneligeDager = (2 jan 2021).datesUntil(1 feb 2021).toList()

        val gjenopprettingOrganisasjonsnummer = lagOrganisasjonsnummer()
        val gjenopprettingFom = 3 jan 2021
        val gjenopprettingTom = 30 jan 2021
        val gjenopprettingPeriodebeløp = BigDecimal("2222.22")
        val gjenopprettingDager = (3 jan 2021).datesUntil(31 jan 2021)
            .filter { it.toEpochDay() % 2 == 0L }.toList()
        val gjenopprettingNotatTilBeslutter = "gjenoppretter etter feilaktig fjerning"

        førsteVedtaksperiode().apply {
            fom = 1 jan 2021
            tom = 31 jan 2021
        }
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        søknadOgGodkjenningbehovKommerInn()

        medPersonISpeil {
            val tilkommenInntektId = saksbehandlerLeggerTilTilkommenInntekt(
                organisasjonsnummer = opprinneligOrganisasjonsnummer,
                fom = opprinneligFom,
                tom = opprinneligTom,
                periodebeløp = opprinneligPeriodebeløp,
                dager = opprinneligeDager,
                notatTilBeslutter = "notat"
            )

            saksbehandlerFjernerTilkommenInntekt(
                tilkommenInntektId = tilkommenInntektId,
                notatTilBeslutter = "fjerner inntekten"
            )

            // When:
            saksbehandlerGjenoppretterTilkommenInntekt(
                tilkommenInntektId = tilkommenInntektId,
                organisasjonsnummer = gjenopprettingOrganisasjonsnummer,
                fom = gjenopprettingFom,
                tom = gjenopprettingTom,
                periodebeløp = gjenopprettingPeriodebeløp,
                dager = gjenopprettingDager,
                notatTilBeslutter = gjenopprettingNotatTilBeslutter
            )

            // Then:
            val tilkomneInntektskilder = hentTilkomneInntektskilder()
            assertEquals(1, tilkomneInntektskilder.size())
            assertApiInntektskilde(
                tilkommenInntektskilde = tilkomneInntektskilder[0],
                expectedOrganisasjonsnummer = gjenopprettingOrganisasjonsnummer,
                expectedFom = gjenopprettingFom,
                expectedTom = gjenopprettingTom,
                expectedPeriodebeløp = gjenopprettingPeriodebeløp,
                expectedDager = gjenopprettingDager,
                expectedFjernet = false
            )
            val events = tilkomneInntektskilder[0]["inntekter"][0]["events"]
            assertEventTypenamesAndMetadata(
                events = events,
                expectedTypenameNotatTilBeslutterPairs = listOf(
                    "TilkommenInntektOpprettetEvent" to "notat",
                    "TilkommenInntektFjernetEvent" to "fjerner inntekten",
                    "TilkommenInntektGjenopprettetEvent" to "gjenoppretter etter feilaktig fjerning",
                )
            )
            assertEventEndringer(
                endringer = events[2]["endringer"],
                expectedOrganisasjonsnummerFra = opprinneligOrganisasjonsnummer,
                expectedOrganisasjonsnummerTil = gjenopprettingOrganisasjonsnummer,
                expectedFraFom = opprinneligFom,
                expectedFraTom = opprinneligTom,
                expectedTilFom = gjenopprettingFom,
                expectedTilTom = gjenopprettingTom,
                expectedPeriodebeløpFra = opprinneligPeriodebeløp,
                expectedPeriodebeløpTil = gjenopprettingPeriodebeløp,
                expectedDagerFra = opprinneligeDager,
                expectedDagerTil = gjenopprettingDager
            )
        }
        val inntektsendringerMelding = meldinger().last { it["@event_name"].asText() == "inntektsendringer" }
        assertEquals(fødselsnummer(), inntektsendringerMelding["fødselsnummer"].asText())
        assertEquals(1, inntektsendringerMelding["inntektsendringer"].size())
        assertInntektsendringerInntektskilde(
            inntektskilde = inntektsendringerMelding["inntektsendringer"][0],
            expectedOrganisasjonsnummer = gjenopprettingOrganisasjonsnummer,
            expectedInntekter = gjenopprettingDager.tilPerioder()
                .map { Triple(it.fom.toString(), it.tom.toString(), 158.73) },
            expectedNullstillinger = emptyList(),
        )
    }

    @Test
    fun `en tilkommen inntekt gjennom en del ledd ender opp med riktig tilstand og historikk`() {
        // Given:
        førsteVedtaksperiode().apply {
            fom = 1 jan 2021
            tom = 31 jan 2021
        }
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        søknadOgGodkjenningbehovKommerInn()

        // When:
        val nestSisteEndretOrganisasjonsnummer = lagOrganisasjonsnummer()
        val nestSisteFom = 5 jan 2021
        val nestSisteTom = 28 jan 2021

        val sisteEndretOrganisasjonesnummer = lagOrganisasjonsnummer()
        val sisteFom = 14 jan 2021
        val sisteTom = 21 jan 2021
        val sisteDager = listOf(17 jan 2021, 19 jan 2021)

        medPersonISpeil {
            val tilkommenInntektId = saksbehandlerLeggerTilTilkommenInntekt(
                organisasjonsnummer = lagOrganisasjonsnummer(),
                fom = 2 jan 2021,
                tom = 31 jan 2021,
                periodebeløp = BigDecimal("1111.11"),
                dager = (2 jan 2021).datesUntil(1 feb 2021).toList(),
                notatTilBeslutter = "legger til tilkommen inntekt her"
            )

            saksbehandlerEndrerTilkommenInntekt(
                tilkommenInntektId = tilkommenInntektId,
                organisasjonsnummer = lagOrganisasjonsnummer(),
                fom = 3 jan 2021,
                tom = 30 jan 2021,
                periodebeløp = BigDecimal("2222.22"),
                dager = (3 jan 2021).datesUntil(31 jan 2021).toList()
                    .filter { it.toEpochDay() % 2 == 0L },
                notatTilBeslutter = "endring nummer 1"
            )
            saksbehandlerEndrerTilkommenInntekt(
                tilkommenInntektId = tilkommenInntektId,
                organisasjonsnummer = lagOrganisasjonsnummer(),
                fom = 4 jan 2021,
                tom = 29 jan 2021,
                periodebeløp = BigDecimal("3333.33"),
                dager = (4 jan 2021).datesUntil(30 jan 2021).toList()
                    .filter { it.toEpochDay() % 2 == 1L },
                notatTilBeslutter = "endring nummer 2"
            )
            saksbehandlerFjernerTilkommenInntekt(
                tilkommenInntektId = tilkommenInntektId,
                notatTilBeslutter = "fjerning"
            )
            saksbehandlerGjenoppretterTilkommenInntekt(
                tilkommenInntektId = tilkommenInntektId,
                organisasjonsnummer = nestSisteEndretOrganisasjonsnummer,
                fom = nestSisteFom,
                tom = nestSisteTom,
                periodebeløp = BigDecimal("4444.44"),
                dager = nestSisteFom.datesUntil(nestSisteTom.plusDays(1)).toList(),
                notatTilBeslutter = "gjenoppretter etter feilaktig fjerning"
            )
            saksbehandlerEndrerTilkommenInntekt(
                tilkommenInntektId = tilkommenInntektId,
                organisasjonsnummer = sisteEndretOrganisasjonesnummer,
                fom = sisteFom,
                tom = sisteTom,
                periodebeløp = BigDecimal("5555.55"),
                dager = sisteDager,
                notatTilBeslutter = "endring nummer 3"
            )

            // Then:
            val tilkomneInntektskilder = hentTilkomneInntektskilder()
            assertEquals(1, tilkomneInntektskilder.size())
            assertApiInntektskilde(
                tilkommenInntektskilde = tilkomneInntektskilder[0],
                expectedOrganisasjonsnummer = sisteEndretOrganisasjonesnummer,
                expectedFom = 14 jan 2021,
                expectedTom = 21 jan 2021,
                expectedPeriodebeløp = BigDecimal("5555.55"),
                expectedDager = listOf(17 jan 2021, 19 jan 2021),
                expectedFjernet = false
            )
            assertEventTypenamesAndMetadata(
                events = tilkomneInntektskilder[0]["inntekter"][0]["events"],
                expectedTypenameNotatTilBeslutterPairs = listOf(
                    "TilkommenInntektOpprettetEvent" to "legger til tilkommen inntekt her",
                    "TilkommenInntektEndretEvent" to "endring nummer 1",
                    "TilkommenInntektEndretEvent" to "endring nummer 2",
                    "TilkommenInntektFjernetEvent" to "fjerning",
                    "TilkommenInntektGjenopprettetEvent" to "gjenoppretter etter feilaktig fjerning",
                    "TilkommenInntektEndretEvent" to "endring nummer 3",
                )
            )
        }
        val inntektsendringerMelding = meldinger().last { it["@event_name"].asText() == "inntektsendringer" }
        assertEquals(fødselsnummer(), inntektsendringerMelding["fødselsnummer"].asText())
        assertEquals(2, inntektsendringerMelding["inntektsendringer"].size())
        assertInntektsendringerInntektskilde(
            inntektskilde = inntektsendringerMelding["inntektsendringer"][0],
            expectedOrganisasjonsnummer = nestSisteEndretOrganisasjonsnummer,
            expectedInntekter = emptyList(),
            expectedNullstillinger = listOf(Pair(nestSisteFom.toString(), nestSisteTom.toString())),
        )
        assertInntektsendringerInntektskilde(
            inntektskilde = inntektsendringerMelding["inntektsendringer"][1],
            expectedOrganisasjonsnummer = sisteEndretOrganisasjonesnummer,
            expectedInntekter = sisteDager.tilPerioder().map { Triple(it.fom.toString(), it.tom.toString(), 2777.775) },
            expectedNullstillinger = emptyList()
        )
    }

    @Test
    fun `to tilkomne inntekter går ikke i beina på hverandre`() {
        // Given:
        førsteVedtaksperiode().apply {
            fom = 1 jan 2021
            tom = 31 jan 2021
        }
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        søknadOgGodkjenningbehovKommerInn()

        // When:
        val organisasjonsnummer = lagOrganisasjonsnummer()
        medPersonISpeil {
            val tilkommenInntektId1 = saksbehandlerLeggerTilTilkommenInntekt(
                organisasjonsnummer = organisasjonsnummer,
                fom = 2 jan 2021,
                tom = 15 jan 2021,
                periodebeløp = BigDecimal("1111.11"),
                dager = (2 jan 2021).datesUntil(16 jan 2021).toList(),
                notatTilBeslutter = "legger til første tilkommen inntekt her"
            )

            saksbehandlerEndrerTilkommenInntekt(
                tilkommenInntektId = tilkommenInntektId1,
                organisasjonsnummer = organisasjonsnummer,
                fom = 2 jan 2021,
                tom = 15 jan 2021,
                periodebeløp = BigDecimal("2222.22"),
                dager = (2 jan 2021).datesUntil(16 jan 2021).toList(),
                notatTilBeslutter = "endret periodebeløp på første tilkomne inntekt"
            )

            val tilkommenInntektId2 = saksbehandlerLeggerTilTilkommenInntekt(
                organisasjonsnummer = organisasjonsnummer,
                fom = 16 jan 2021,
                tom = 31 jan 2021,
                periodebeløp = BigDecimal("1111.11"),
                dager = (16 jan 2021).datesUntil(1 feb 2021).toList(),
                notatTilBeslutter = "legger til enda en tilkommen inntekt her"
            )

            saksbehandlerFjernerTilkommenInntekt(
                tilkommenInntektId = tilkommenInntektId1,
                notatTilBeslutter = "fjerner første tilkommen inntekt"
            )

            saksbehandlerEndrerTilkommenInntekt(
                tilkommenInntektId = tilkommenInntektId2,
                organisasjonsnummer = organisasjonsnummer,
                fom = 16 jan 2021,
                tom = 31 jan 2021,
                periodebeløp = BigDecimal("2222.22"),
                dager = (16 jan 2021).datesUntil(1 feb 2021).toList(),
                notatTilBeslutter = "endret periodebeløp på andre tilkomne inntekt"
            )

            // Then:
            val tilkomneInntektskilder = hentTilkomneInntektskilder()
            assertEquals(1, tilkomneInntektskilder.size())
            val tilkommenInntektskilde = tilkomneInntektskilder[0]
            assertEquals(
                expected = organisasjonsnummer,
                actual = tilkommenInntektskilde["organisasjonsnummer"].asText()
            )
            tilkommenInntektskilde["inntekter"].let { inntekter ->
                assertEquals(2, inntekter.size())
                assertApiInntekt(
                    inntekt = inntekter[0],
                    expectedFom = 2 jan 2021,
                    expectedTom = 15 jan 2021,
                    expectedPeriodebeløp = BigDecimal("2222.22"),
                    expectedDager = (2 jan 2021).datesUntil(16 jan 2021).toList(),
                    expectedFjernet = true
                )
                assertEventTypenamesAndMetadata(
                    events = inntekter[0]["events"],
                    expectedTypenameNotatTilBeslutterPairs = listOf(
                        "TilkommenInntektOpprettetEvent" to "legger til første tilkommen inntekt her",
                        "TilkommenInntektEndretEvent" to "endret periodebeløp på første tilkomne inntekt",
                        "TilkommenInntektFjernetEvent" to "fjerner første tilkommen inntekt",
                    )
                )
                assertApiInntekt(
                    inntekt = inntekter[1],
                    expectedFom = 16 jan 2021,
                    expectedTom = 31 jan 2021,
                    expectedPeriodebeløp = BigDecimal("2222.22"),
                    expectedDager = (16 jan 2021).datesUntil(1 feb 2021).toList(),
                    expectedFjernet = false
                )
                assertEventTypenamesAndMetadata(
                    events = inntekter[1]["events"],
                    expectedTypenameNotatTilBeslutterPairs = listOf(
                        "TilkommenInntektOpprettetEvent" to "legger til enda en tilkommen inntekt her",
                        "TilkommenInntektEndretEvent" to "endret periodebeløp på andre tilkomne inntekt",
                    )
                )
            }
        }
    }

    private fun assertApiInntektskilde(
        tilkommenInntektskilde: JsonNode,
        expectedOrganisasjonsnummer: String,
        expectedFom: LocalDate,
        expectedTom: LocalDate,
        expectedPeriodebeløp: BigDecimal,
        expectedDager: List<LocalDate>,
        expectedFjernet: Boolean
    ) {
        assertEquals(expectedOrganisasjonsnummer, tilkommenInntektskilde["organisasjonsnummer"].asText())
        tilkommenInntektskilde["inntekter"].let { inntekter ->
            assertEquals(1, inntekter.size())
            assertApiInntekt(
                inntekt = inntekter[0],
                expectedFom = expectedFom,
                expectedTom = expectedTom,
                expectedPeriodebeløp = expectedPeriodebeløp,
                expectedDager = expectedDager,
                expectedFjernet = expectedFjernet
            )
        }
    }

    private fun assertApiInntekt(
        inntekt: JsonNode,
        expectedFom: LocalDate,
        expectedTom: LocalDate,
        expectedPeriodebeløp: BigDecimal,
        expectedDager: List<LocalDate>,
        expectedFjernet: Boolean
    ) {
        assertEquals(expectedFom.toString(), inntekt["periode"]["fom"].asText())
        assertEquals(expectedTom.toString(), inntekt["periode"]["tom"].asText())
        assertEquals(expectedPeriodebeløp.toString(), inntekt["periodebelop"].asText())
        assertEquals(expectedDager.size, inntekt["dager"].size())
        assertEquals(expectedDager.map(LocalDate::toString), inntekt["dager"].map(JsonNode::asText))
        assertEquals(expectedFjernet, inntekt["fjernet"].asBoolean())
    }

    private fun assertEventEndringer(
        endringer: JsonNode,
        expectedOrganisasjonsnummerFra: String,
        expectedOrganisasjonsnummerTil: String,
        expectedFraFom: LocalDate,
        expectedFraTom: LocalDate,
        expectedTilFom: LocalDate,
        expectedTilTom: LocalDate,
        expectedPeriodebeløpFra: BigDecimal,
        expectedPeriodebeløpTil: BigDecimal,
        expectedDagerFra: List<LocalDate>,
        expectedDagerTil: List<LocalDate>
    ) {
        assertEquals(expectedOrganisasjonsnummerFra, endringer["organisasjonsnummer"]["fra"].asText())
        assertEquals(expectedOrganisasjonsnummerTil, endringer["organisasjonsnummer"]["til"].asText())
        assertEquals(expectedFraFom.toString(), endringer["periode"]["fra"]["fom"].asText())
        assertEquals(expectedFraTom.toString(), endringer["periode"]["fra"]["tom"].asText())
        assertEquals(expectedTilFom.toString(), endringer["periode"]["til"]["fom"].asText())
        assertEquals(expectedTilTom.toString(), endringer["periode"]["til"]["tom"].asText())
        assertEquals(expectedPeriodebeløpFra.toString(), endringer["periodebelop"]["fra"].asText())
        assertEquals(expectedPeriodebeløpTil.toString(), endringer["periodebelop"]["til"].asText())
        assertEquals(expectedDagerFra.size, endringer["dager"]["fra"].size())
        assertEquals(expectedDagerTil.size, endringer["dager"]["til"].size())
        assertEquals(expectedDagerFra.map(LocalDate::toString), endringer["dager"]["fra"].map(JsonNode::asText))
        assertEquals(expectedDagerTil.map(LocalDate::toString), endringer["dager"]["til"].map(JsonNode::asText))
    }

    private fun assertEventTypenamesAndMetadata(
        events: JsonNode,
        expectedTypenameNotatTilBeslutterPairs: List<Pair<String, String>>
    ) {
        assertEquals(expectedTypenameNotatTilBeslutterPairs.size, events.size())
        expectedTypenameNotatTilBeslutterPairs.forEachIndexed { index, (type, notatTilBeslutter) ->
            assertEventTypenameAndMetadata(
                event = events[index],
                expectedTypename = type,
                expectedSekvensnummer = index + 1,
                expectedNotatTilBeslutter = notatTilBeslutter
            )
        }
    }

    private fun assertEventTypenameAndMetadata(
        event: JsonNode,
        expectedTypename: String,
        expectedSekvensnummer: Int,
        expectedNotatTilBeslutter: String
    ) {
        assertEquals(expectedTypename, event["__typename"].asText())

        val metadata = event["metadata"]
        assertEquals(expectedSekvensnummer, metadata["sekvensnummer"].asInt())
        val eventTidspunkt = metadata["tidspunkt"].asText().let(LocalDateTime::parse).somInstantIOslo()
        val now = Instant.now()
        assertTrue(eventTidspunkt.isBefore(now)) {
            "Forventet at tidspunkt på event ($eventTidspunkt) var før nå ($now)"
        }
        assertTrue(eventTidspunkt.isAfter(now.minusSeconds(5))) {
            "Forventet at tidspunkt på event ($eventTidspunkt) var senere enn fem sekunder før nå ($now)"
        }
        assertEquals(saksbehandlerIdent(), metadata["utfortAvSaksbehandlerIdent"].asText())
        assertEquals(expectedNotatTilBeslutter, metadata["notatTilBeslutter"].asText())
    }

    private fun assertInntektsendringerInntektskilde(
        inntektskilde: JsonNode,
        expectedOrganisasjonsnummer: String,
        expectedInntekter: List<Triple<String, String, Double>>,
        expectedNullstillinger: List<Pair<String, String>>
    ) {
        assertEquals(expectedOrganisasjonsnummer, inntektskilde["inntektskilde"].asText())
        assertInntektsendringerInntekter(expectedInntekter, inntektskilde["inntekter"])
        assertInntektsendringerNullstillinger(expectedNullstillinger, inntektskilde["nullstill"])
    }

    private fun assertInntektsendringerInntekter(
        expectedInntekter: List<Triple<String, String, Double>>,
        inntekter: JsonNode
    ) {
        assertEquals(expectedInntekter.size, inntekter.size())
        expectedInntekter.forEachIndexed { index, (fom, tom, dagsbeløp) ->
            val inntekt = inntekter[index]
            assertEquals(fom, inntekt["fom"].asText())
            assertEquals(tom, inntekt["tom"].asText())
            assertEquals(dagsbeløp, inntekt["dagsbeløp"].asDouble())
        }
    }

    private fun assertInntektsendringerNullstillinger(
        expectedNullstillinger: List<Pair<String, String>>,
        nullstillinger: JsonNode
    ) {
        assertEquals(expectedNullstillinger.size, nullstillinger.size())
        expectedNullstillinger.forEachIndexed { index, (fom, tom) ->
            val nullstilling = nullstillinger[index]
            assertEquals(fom, nullstilling["fom"].asText())
            assertEquals(tom, nullstilling["tom"].asText())
        }
    }

    private fun LocalDateTime.somInstantIOslo(): Instant =
        atZone(ZoneId.of("Europe/Oslo")).toInstant()
}
