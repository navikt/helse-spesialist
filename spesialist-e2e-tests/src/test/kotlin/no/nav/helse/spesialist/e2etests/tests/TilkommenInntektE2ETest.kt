package no.nav.helse.spesialist.e2etests.tests

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.spesialist.domain.Periode.Companion.tilOgMed
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
        val periode = 2 jan 2021 tilOgMed (31 jan 2021)
        val ekskluderteUkedager = listOf(12 jan 2021, 21 jan 2021, 25 jan 2021)
        val periodebeløp = BigDecimal("1111.11")
        val notatTilBeslutter = "notat"
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        søknadOgGodkjenningbehovKommerInn()

        // When:
        medPersonISpeil {
            saksbehandlerLeggerTilTilkommenInntekt(
                organisasjonsnummer = organisasjonsnummer,
                periode = periode,
                periodebeløp = periodebeløp,
                ekskluderteUkedager = ekskluderteUkedager,
                notatTilBeslutter = notatTilBeslutter
            )

            // Then:
            assertEquals(1, tilkomneInntektskilder.size())
            assertApiInntektskilde(
                tilkommenInntektskilde = tilkomneInntektskilder[0],
                expectedOrganisasjonsnummer = organisasjonsnummer,
                expectedFom = periode.fom,
                expectedTom = periode.tom,
                expectedPeriodebeløp = periodebeløp,
                expectedEkskluderteUkedager = ekskluderteUkedager,
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
            assertEquals(periode.fom.toString(), opprettetEvent["periode"]["fom"].asText())
            assertEquals(periode.tom.toString(), opprettetEvent["periode"]["tom"].asText())
            assertEquals(periodebeløp.toString(), opprettetEvent["periodebelop"].asText())
            assertEquals(ekskluderteUkedager.size, opprettetEvent["ekskluderteUkedager"].size())
            assertEquals(
                ekskluderteUkedager.map(LocalDate::toString),
                opprettetEvent["ekskluderteUkedager"].map(JsonNode::asText)
            )
        }

        val inntektsendringerMelding = meldinger().last { it["@event_name"].asText() == "inntektsendringer" }
        assertEquals(fødselsnummer(), inntektsendringerMelding["fødselsnummer"].asText())
        assertEquals(1, inntektsendringerMelding["inntektsendringer"].size())
        assertInntektsendringerInntektskilde(
            inntektskilde = inntektsendringerMelding["inntektsendringer"][0],
            expectedOrganisasjonsnummer = organisasjonsnummer,
            expectedInntekter = listOf(
                Triple("2021-01-04", "2021-01-08", 65.3594),
                Triple("2021-01-11", "2021-01-11", 65.3594),
                Triple("2021-01-13", "2021-01-15", 65.3594),
                Triple("2021-01-18", "2021-01-20", 65.3594),
                Triple("2021-01-22", "2021-01-22", 65.3594),
                Triple("2021-01-26", "2021-01-29", 65.3594)
            ),
            expectedNullstillinger = emptyList()
        )
    }

    @Test
    fun `saksbehandler endrer tilkommen inntekt`() {
        // Given:
        val opprinneligOrganisasjonsnummer = lagOrganisasjonsnummer()
        val opprinneligPeriode = (2 jan 2021) tilOgMed (31 jan 2021)
        val opprinneligPeriodebeløp = BigDecimal("1111.11")
        val opprinneligeEkskluderteUkedager = listOf(12 jan 2021, 21 jan 2021, 25 jan 2021)

        val endringOrganisasjonsnummer = lagOrganisasjonsnummer()
        val endringPeriode = (3 jan 2021) tilOgMed (30 jan 2021)
        val endringPeriodebeløp = BigDecimal("2222.22")
        val endringEkskluderteUkedager = listOf(12 jan 2021, 21 jan 2021, 26 jan 2021)
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
                periode = opprinneligPeriode,
                periodebeløp = opprinneligPeriodebeløp,
                ekskluderteUkedager = opprinneligeEkskluderteUkedager,
                notatTilBeslutter = "notat"
            )

            // When:
            saksbehandlerEndrerTilkommenInntekt(
                tilkommenInntektId = tilkommenInntektId,
                organisasjonsnummerEndring = opprinneligOrganisasjonsnummer to endringOrganisasjonsnummer,
                periodeEndring = opprinneligPeriode to endringPeriode,
                periodebeløpEndring = opprinneligPeriodebeløp to endringPeriodebeløp,
                ekskluderteUkedagerEndring = opprinneligeEkskluderteUkedager to endringEkskluderteUkedager,
                notatTilBeslutter = endringNotatTilBeslutter
            )

            // Then:
            assertEquals(1, tilkomneInntektskilder.size())
            assertApiInntektskilde(
                tilkommenInntektskilde = tilkomneInntektskilder[0],
                expectedOrganisasjonsnummer = endringOrganisasjonsnummer,
                expectedFom = endringPeriode.fom,
                expectedTom = endringPeriode.tom,
                expectedPeriodebeløp = endringPeriodebeløp,
                expectedEkskluderteUkedager = endringEkskluderteUkedager,
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
                expectedFraFom = opprinneligPeriode.fom,
                expectedFraTom = opprinneligPeriode.tom,
                expectedTilFom = endringPeriode.fom,
                expectedTilTom = endringPeriode.tom,
                expectedPeriodebeløpFra = opprinneligPeriodebeløp,
                expectedPeriodebeløpTil = endringPeriodebeløp,
                expectedEkskluderteUkedagerFra = opprinneligeEkskluderteUkedager,
                expectedEkskluderteUkedagerTil = endringEkskluderteUkedager
            )
        }
        val inntektsendringerMelding = meldinger().last { it["@event_name"].asText() == "inntektsendringer" }
        assertEquals(fødselsnummer(), inntektsendringerMelding["fødselsnummer"].asText())
        assertEquals(2, inntektsendringerMelding["inntektsendringer"].size())
        assertInntektsendringerInntektskilde(
            inntektskilde = inntektsendringerMelding["inntektsendringer"][0],
            expectedOrganisasjonsnummer = opprinneligOrganisasjonsnummer,
            expectedInntekter = emptyList(),
            expectedNullstillinger = listOf(
                Pair("2021-01-04", "2021-01-08"),
                Pair("2021-01-11", "2021-01-11"),
                Pair("2021-01-13", "2021-01-15"),
                Pair("2021-01-18", "2021-01-20"),
                Pair("2021-01-22", "2021-01-22"),
                Pair("2021-01-26", "2021-01-29")
            )
        )
        assertInntektsendringerInntektskilde(
            inntektskilde = inntektsendringerMelding["inntektsendringer"][1],
            expectedOrganisasjonsnummer = endringOrganisasjonsnummer,
            expectedInntekter = listOf(
                Triple("2021-01-04", "2021-01-08", 130.7188),
                Triple("2021-01-11", "2021-01-11", 130.7188),
                Triple("2021-01-13", "2021-01-15", 130.7188),
                Triple("2021-01-18", "2021-01-20", 130.7188),
                Triple("2021-01-22", "2021-01-22", 130.7188),
                Triple("2021-01-25", "2021-01-25", 130.7188),
                Triple("2021-01-27", "2021-01-29", 130.7188)
            ),
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
        val opprinneligPeriode = 2 jan 2021 tilOgMed (31 jan 2021)
        val opprinneligPeriodebeløp = BigDecimal("1111.11")
        val opprinneligeEkskluderteUkedager = listOf(12 jan 2021, 21 jan 2021, 25 jan 2021)
        medPersonISpeil {
            val tilkommenInntektId = saksbehandlerLeggerTilTilkommenInntekt(
                organisasjonsnummer = opprinneligOrganisasjonsnummer,
                periode = opprinneligPeriode,
                periodebeløp = opprinneligPeriodebeløp,
                ekskluderteUkedager = opprinneligeEkskluderteUkedager,
                notatTilBeslutter = "notat"
            )

            // When:
            val fjerningNotatTilBeslutter = "fjerner inntekten"
            saksbehandlerFjernerTilkommenInntekt(
                tilkommenInntektId = tilkommenInntektId,
                notatTilBeslutter = fjerningNotatTilBeslutter
            )

            // Then:
            assertEquals(1, tilkomneInntektskilder.size())
            assertApiInntektskilde(
                tilkommenInntektskilde = tilkomneInntektskilder[0],
                expectedOrganisasjonsnummer = opprinneligOrganisasjonsnummer,
                expectedFom = opprinneligPeriode.fom,
                expectedTom = opprinneligPeriode.tom,
                expectedPeriodebeløp = opprinneligPeriodebeløp,
                expectedEkskluderteUkedager = opprinneligeEkskluderteUkedager,
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
            expectedNullstillinger = listOf(
                Pair("2021-01-04", "2021-01-08"),
                Pair("2021-01-11", "2021-01-11"),
                Pair("2021-01-13", "2021-01-15"),
                Pair("2021-01-18", "2021-01-20"),
                Pair("2021-01-22", "2021-01-22"),
                Pair("2021-01-26", "2021-01-29")
            ),
        )
    }

    @Test
    fun `saksbehandler gjenoppretter tilkommen inntekt`() {
        // Given:
        val opprinneligOrganisasjonsnummer = lagOrganisasjonsnummer()
        val opprinneligPeriode = (2 jan 2021) tilOgMed (31 jan 2021)
        val opprinneligPeriodebeløp = BigDecimal("1111.11")
        val opprinneligeEkskluderteUkedager = listOf(12 jan 2021, 21 jan 2021, 25 jan 2021)

        val gjenopprettingOrganisasjonsnummer = lagOrganisasjonsnummer()
        val gjenopprettingPeriode = (3 jan 2021) tilOgMed (30 jan 2021)
        val gjenopprettingPeriodebeløp = BigDecimal("2222.22")
        val gjenopprettingEkskluderteUkedager = listOf(12 jan 2021, 21 jan 2021, 26 jan 2021)
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
                periode = opprinneligPeriode,
                periodebeløp = opprinneligPeriodebeløp,
                ekskluderteUkedager = opprinneligeEkskluderteUkedager,
                notatTilBeslutter = "notat"
            )

            saksbehandlerFjernerTilkommenInntekt(
                tilkommenInntektId = tilkommenInntektId,
                notatTilBeslutter = "fjerner inntekten"
            )

            // When:
            saksbehandlerGjenoppretterTilkommenInntekt(
                tilkommenInntektId = tilkommenInntektId,
                organisasjonsnummerEndring = opprinneligOrganisasjonsnummer to gjenopprettingOrganisasjonsnummer,
                periodeEndring = opprinneligPeriode to gjenopprettingPeriode,
                periodebeløpEndring = opprinneligPeriodebeløp to gjenopprettingPeriodebeløp,
                ekskluderteUkedagerEndring = opprinneligeEkskluderteUkedager to gjenopprettingEkskluderteUkedager,
                notatTilBeslutter = gjenopprettingNotatTilBeslutter
            )

            // Then:
            assertEquals(1, tilkomneInntektskilder.size())
            assertApiInntektskilde(
                tilkommenInntektskilde = tilkomneInntektskilder[0],
                expectedOrganisasjonsnummer = gjenopprettingOrganisasjonsnummer,
                expectedFom = gjenopprettingPeriode.fom,
                expectedTom = gjenopprettingPeriode.tom,
                expectedPeriodebeløp = gjenopprettingPeriodebeløp,
                expectedEkskluderteUkedager = gjenopprettingEkskluderteUkedager,
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
                expectedFraFom = opprinneligPeriode.fom,
                expectedFraTom = opprinneligPeriode.tom,
                expectedTilFom = gjenopprettingPeriode.fom,
                expectedTilTom = gjenopprettingPeriode.tom,
                expectedPeriodebeløpFra = opprinneligPeriodebeløp,
                expectedPeriodebeløpTil = gjenopprettingPeriodebeløp,
                expectedEkskluderteUkedagerFra = opprinneligeEkskluderteUkedager,
                expectedEkskluderteUkedagerTil = gjenopprettingEkskluderteUkedager
            )
        }
        val inntektsendringerMelding = meldinger().last { it["@event_name"].asText() == "inntektsendringer" }
        assertEquals(fødselsnummer(), inntektsendringerMelding["fødselsnummer"].asText())
        assertEquals(1, inntektsendringerMelding["inntektsendringer"].size())
        assertInntektsendringerInntektskilde(
            inntektskilde = inntektsendringerMelding["inntektsendringer"][0],
            expectedOrganisasjonsnummer = gjenopprettingOrganisasjonsnummer,
            expectedInntekter = listOf(
                Triple("2021-01-04", "2021-01-08", 130.7188),
                Triple("2021-01-11", "2021-01-11", 130.7188),
                Triple("2021-01-13", "2021-01-15", 130.7188),
                Triple("2021-01-18", "2021-01-20", 130.7188),
                Triple("2021-01-22", "2021-01-22", 130.7188),
                Triple("2021-01-25", "2021-01-25", 130.7188),
                Triple("2021-01-27", "2021-01-29", 130.7188)
            ),
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
        val organisasjonsnummer1 = lagOrganisasjonsnummer()
        val organisasjonsnummer2 = lagOrganisasjonsnummer()
        val organisasjonsnummer3 = lagOrganisasjonsnummer()
        val organisasjonsnummer4 = lagOrganisasjonsnummer()
        val organisasjonsnummer5 = lagOrganisasjonsnummer()
        val periode1 = (2 jan 2021) tilOgMed (31 jan 2021)
        val periode2 = (3 jan 2021) tilOgMed (30 jan 2021)
        val periode3 = (4 jan 2021) tilOgMed (29 jan 2021)
        val periode4 = (5 jan 2021) tilOgMed (28 jan 2021)
        val periode5 = (14 jan 2021) tilOgMed (21 jan 2021)
        val periodebeløp1 = BigDecimal("1111.11")
        val periodebeløp2 = BigDecimal("2222.22")
        val periodebeløp3 = BigDecimal("3333.33")
        val periodebeløp4 = BigDecimal("4444.44")
        val periodebeløp5 = BigDecimal("5555.55")
        val ekskluderteUkedager1 = listOf(12 jan 2021, 21 jan 2021, 25 jan 2021)
        val ekskluderteUkedager2 = listOf(12 jan 2021, 21 jan 2021, 26 jan 2021)
        val ekskluderteUkedager3 = listOf(11 jan 2021)
        val ekskluderteUkedager4 = listOf(6 jan 2021, 13 jan 2021, 20 jan 2021, 27 jan 2021)
        val ekskluderteUkedager5 = listOf(15 jan 2021, 19 jan 2021)

        medPersonISpeil {
            val tilkommenInntektId = saksbehandlerLeggerTilTilkommenInntekt(
                organisasjonsnummer = organisasjonsnummer1,
                periode = periode1,
                periodebeløp = periodebeløp1,
                ekskluderteUkedager = ekskluderteUkedager1,
                notatTilBeslutter = "legger til tilkommen inntekt her"
            )

            saksbehandlerEndrerTilkommenInntekt(
                tilkommenInntektId = tilkommenInntektId,
                organisasjonsnummerEndring = organisasjonsnummer1 to organisasjonsnummer2,
                periodeEndring = periode1 to periode2,
                periodebeløpEndring = periodebeløp1 to periodebeløp2,
                ekskluderteUkedagerEndring = ekskluderteUkedager1 to ekskluderteUkedager2,
                notatTilBeslutter = "endring nummer 1"
            )

            saksbehandlerEndrerTilkommenInntekt(
                tilkommenInntektId = tilkommenInntektId,
                organisasjonsnummerEndring = organisasjonsnummer2 to organisasjonsnummer3,
                periodeEndring = periode2 to periode3,
                periodebeløpEndring = periodebeløp2 to periodebeløp3,
                ekskluderteUkedagerEndring = ekskluderteUkedager2 to ekskluderteUkedager3,
                notatTilBeslutter = "endring nummer 2"
            )
            saksbehandlerFjernerTilkommenInntekt(
                tilkommenInntektId = tilkommenInntektId,
                notatTilBeslutter = "fjerning"
            )

            saksbehandlerGjenoppretterTilkommenInntekt(
                tilkommenInntektId = tilkommenInntektId,
                organisasjonsnummerEndring = organisasjonsnummer3 to organisasjonsnummer4,
                periodeEndring = periode3 to periode4,
                periodebeløpEndring = periodebeløp3 to periodebeløp4,
                ekskluderteUkedagerEndring = ekskluderteUkedager3 to ekskluderteUkedager4,
                notatTilBeslutter = "gjenoppretter etter feilaktig fjerning"
            )

            saksbehandlerEndrerTilkommenInntekt(
                tilkommenInntektId = tilkommenInntektId,
                organisasjonsnummerEndring = organisasjonsnummer4 to organisasjonsnummer5,
                periodeEndring = periode4 to periode5,
                periodebeløpEndring = periodebeløp4 to periodebeløp5,
                ekskluderteUkedagerEndring = ekskluderteUkedager4 to ekskluderteUkedager5,
                notatTilBeslutter = "endring nummer 3"
            )

            // Then:
            assertEquals(1, tilkomneInntektskilder.size())
            assertApiInntektskilde(
                tilkommenInntektskilde = tilkomneInntektskilder[0],
                expectedOrganisasjonsnummer = organisasjonsnummer5,
                expectedFom = periode5.fom,
                expectedTom = periode5.tom,
                expectedPeriodebeløp = periodebeløp5,
                expectedEkskluderteUkedager = ekskluderteUkedager5,
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
            expectedOrganisasjonsnummer = organisasjonsnummer4,
            expectedInntekter = emptyList(),
            expectedNullstillinger = listOf(
                Pair("2021-01-05", "2021-01-05"),
                Pair("2021-01-07", "2021-01-08"),
                Pair("2021-01-11", "2021-01-12"),
                Pair("2021-01-14", "2021-01-15"),
                Pair("2021-01-18", "2021-01-19"),
                Pair("2021-01-21", "2021-01-22"),
                Pair("2021-01-25", "2021-01-26"),
                Pair("2021-01-28", "2021-01-28"),
            ),
        )
        assertInntektsendringerInntektskilde(
            inntektskilde = inntektsendringerMelding["inntektsendringer"][1],
            expectedOrganisasjonsnummer = organisasjonsnummer5,
            expectedInntekter = listOf(
                Triple("2021-01-14", "2021-01-14", 1388.8875),
                Triple("2021-01-18", "2021-01-18", 1388.8875),
                Triple("2021-01-20", "2021-01-21", 1388.8875),
            ),
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
                periode = (2 jan 2021) tilOgMed (15 jan 2021),
                periodebeløp = BigDecimal("1111.11"),
                ekskluderteUkedager = listOf(7 jan 2021, 8 jan 2021),
                notatTilBeslutter = "legger til første tilkommen inntekt her"
            )

            saksbehandlerEndrerTilkommenInntekt(
                tilkommenInntektId = tilkommenInntektId1,
                organisasjonsnummerEndring = null,
                periodeEndring = null,
                periodebeløpEndring = BigDecimal("1111.11") to BigDecimal("2222.22"),
                ekskluderteUkedagerEndring = null,
                notatTilBeslutter = "endret periodebeløp på første tilkomne inntekt"
            )

            val tilkommenInntektId2 = saksbehandlerLeggerTilTilkommenInntekt(
                organisasjonsnummer = organisasjonsnummer,
                periode = (16 jan 2021) tilOgMed (31 jan 2021),
                periodebeløp = BigDecimal("1111.11"),
                ekskluderteUkedager = listOf(18 jan 2021, 19 jan 2021, 20 jan 2021, 21 jan 2021),
                notatTilBeslutter = "legger til enda en tilkommen inntekt her"
            )

            saksbehandlerFjernerTilkommenInntekt(
                tilkommenInntektId = tilkommenInntektId1,
                notatTilBeslutter = "fjerner første tilkommen inntekt"
            )

            saksbehandlerEndrerTilkommenInntekt(
                tilkommenInntektId = tilkommenInntektId2,
                organisasjonsnummerEndring = null,
                periodeEndring = null,
                periodebeløpEndring = BigDecimal("1111.11") to BigDecimal("2222.22"),
                ekskluderteUkedagerEndring = null,
                notatTilBeslutter = "endret periodebeløp på andre tilkomne inntekt"
            )

            // Then:
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
                    expectedOrganisasjonsnummer = organisasjonsnummer,
                    expectedFom = 2 jan 2021,
                    expectedTom = 15 jan 2021,
                    expectedPeriodebeløp = BigDecimal("2222.22"),
                    expectedEkskluderteUkedager = listOf(7 jan 2021, 8 jan 2021),
                    expectedFjernet = true,
                    expectedErDelAvAktivTotrinnsvurdering = true
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
                    expectedOrganisasjonsnummer = organisasjonsnummer,
                    expectedFom = 16 jan 2021,
                    expectedTom = 31 jan 2021,
                    expectedPeriodebeløp = BigDecimal("2222.22"),
                    expectedEkskluderteUkedager = listOf(18 jan 2021, 19 jan 2021, 20 jan 2021, 21 jan 2021),
                    expectedFjernet = false,
                    expectedErDelAvAktivTotrinnsvurdering = true
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
        expectedEkskluderteUkedager: List<LocalDate>,
        expectedFjernet: Boolean
    ) {
        assertEquals(expectedOrganisasjonsnummer, tilkommenInntektskilde["organisasjonsnummer"].asText())
        tilkommenInntektskilde["inntekter"].let { inntekter ->
            assertEquals(1, inntekter.size())
            assertApiInntekt(
                inntekt = inntekter[0],
                expectedOrganisasjonsnummer = expectedOrganisasjonsnummer,
                expectedFom = expectedFom,
                expectedTom = expectedTom,
                expectedPeriodebeløp = expectedPeriodebeløp,
                expectedEkskluderteUkedager = expectedEkskluderteUkedager,
                expectedFjernet = expectedFjernet,
                expectedErDelAvAktivTotrinnsvurdering = true
            )
        }
    }

    private fun assertApiInntekt(
        inntekt: JsonNode,
        expectedOrganisasjonsnummer: String,
        expectedFom: LocalDate,
        expectedTom: LocalDate,
        expectedPeriodebeløp: BigDecimal,
        expectedEkskluderteUkedager: List<LocalDate>,
        expectedFjernet: Boolean,
        expectedErDelAvAktivTotrinnsvurdering: Boolean
    ) {
        assertEquals(expectedOrganisasjonsnummer, inntekt["organisasjonsnummer"].asText())
        assertEquals(expectedFom.toString(), inntekt["periode"]["fom"].asText())
        assertEquals(expectedTom.toString(), inntekt["periode"]["tom"].asText())
        assertEquals(expectedPeriodebeløp.toString(), inntekt["periodebelop"].asText())
        assertEquals(expectedEkskluderteUkedager.size, inntekt["ekskluderteUkedager"].size())
        assertEquals(
            expectedEkskluderteUkedager.map(LocalDate::toString),
            inntekt["ekskluderteUkedager"].map(JsonNode::asText)
        )
        assertEquals(expectedFjernet, inntekt["fjernet"].asBoolean())
        assertEquals(expectedErDelAvAktivTotrinnsvurdering, inntekt["erDelAvAktivTotrinnsvurdering"].asBoolean())
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
        expectedEkskluderteUkedagerFra: List<LocalDate>,
        expectedEkskluderteUkedagerTil: List<LocalDate>
    ) {
        assertEquals(expectedOrganisasjonsnummerFra, endringer["organisasjonsnummer"]["fra"].asText())
        assertEquals(expectedOrganisasjonsnummerTil, endringer["organisasjonsnummer"]["til"].asText())
        assertEquals(expectedFraFom.toString(), endringer["periode"]["fra"]["fom"].asText())
        assertEquals(expectedFraTom.toString(), endringer["periode"]["fra"]["tom"].asText())
        assertEquals(expectedTilFom.toString(), endringer["periode"]["til"]["fom"].asText())
        assertEquals(expectedTilTom.toString(), endringer["periode"]["til"]["tom"].asText())
        assertEquals(expectedPeriodebeløpFra.toString(), endringer["periodebelop"]["fra"].asText())
        assertEquals(expectedPeriodebeløpTil.toString(), endringer["periodebelop"]["til"].asText())
        assertEquals(expectedEkskluderteUkedagerFra.size, endringer["ekskluderteUkedager"]["fra"].size())
        assertEquals(expectedEkskluderteUkedagerTil.size, endringer["ekskluderteUkedager"]["til"].size())
        assertEquals(
            expectedEkskluderteUkedagerFra.map(LocalDate::toString),
            endringer["ekskluderteUkedager"]["fra"].map(JsonNode::asText)
        )
        assertEquals(
            expectedEkskluderteUkedagerTil.map(LocalDate::toString),
            endringer["ekskluderteUkedager"]["til"].map(JsonNode::asText)
        )
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
        assertEquals("Api$expectedTypename", event["type"].asText())

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
