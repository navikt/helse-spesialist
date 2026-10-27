package no.nav.helse.spesialist.e2etests.tests

import no.nav.helse.mediator.asLocalDate
import no.nav.helse.mediator.asUUID
import no.nav.helse.spesialist.api.rest.ApiGraderteAndreYtelserDag
import no.nav.helse.spesialist.api.rest.ApiGraderteAndreYtelserType
import no.nav.helse.spesialist.api.rest.ApiYrkesaktivitetstype
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.e2etests.AbstractE2EIntegrationTest
import org.junit.jupiter.api.Test
import tools.jackson.databind.JsonNode
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class GraderteAndreYtelserE2ETest : AbstractE2EIntegrationTest() {
    @Test
    fun `saksbehandler legger til og endrer graderte andre ytelser`() {
        // Given:
        førsteVedtaksperiode().apply {
            fom = 1 jan 2021
            tom = 31 jan 2021
        }
        val dager =
            listOf(
                dag(2 jan 2021, ApiGraderteAndreYtelserType.FORELDREPENGER to 50, sykdomsgrad = 50),
                dag(3 jan 2021, ApiGraderteAndreYtelserType.FORELDREPENGER to 80, sykdomsgrad = 20),
            )
        val endretDag = dag(3 jan 2021, ApiGraderteAndreYtelserType.FORELDREPENGER to 30, sykdomsgrad = 70)
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        søknadOgGodkjenningbehovKommerInn()

        medPersonISpeil {
            // When:
            val førsteVersjon =
                saksbehandlerEndrerGraderteAndreYtelser(
                    versjon = null,
                    dager = dager,
                    yrkesaktivitet = ApiYrkesaktivitetstype.ARBEIDSTAKER,
                    organisasjonsnummer = "123456789",
                    notatTilBeslutter = "notat",
                )

            // Then:
            assertEquals(1, graderteAndreYtelser.size())
            assertGraderteAndreYtelser(graderteAndreYtelser[0], førsteVersjon, dager)
            assertEquals(2 jan 2021, sisteSendteMeldingMedEventName("graderte_andre_ytelser_endret")["fom"].asLocalDate())

            // When:
            val andreVersjon =
                saksbehandlerEndrerGraderteAndreYtelser(
                    versjon = førsteVersjon,
                    dager = listOf(endretDag),
                    yrkesaktivitet = ApiYrkesaktivitetstype.ARBEIDSTAKER,
                    organisasjonsnummer = "123456789",
                    notatTilBeslutter = "endret notat",
                )

            // Then:
            assertNotEquals(førsteVersjon, andreVersjon)
            assertEquals(1, graderteAndreYtelser.size())
            assertGraderteAndreYtelser(graderteAndreYtelser[0], andreVersjon, listOf(dager[0], endretDag))
            assertEquals(2, graderteAndreYtelser[0]["events"].size())
        }
        val endringsmelding = sisteSendteMeldingMedEventName("graderte_andre_ytelser_endret")
        assertEquals(fødselsnummer(), endringsmelding["fødselsnummer"].asString())
        assertEquals(3 jan 2021, endringsmelding["fom"].asLocalDate())
    }

    @Test
    fun `saksbehandler henter graderte andre ytelser for flere yrkesaktiviteter`() {
        // Given:
        førsteVedtaksperiode().apply {
            fom = 1 jan 2021
            tom = 31 jan 2021
        }
        val arbeidstakerDager = listOf(dag(2 jan 2021, ApiGraderteAndreYtelserType.PLEIEPENGER to 50, sykdomsgrad = 50))
        val frilansDager = listOf(dag(16 jan 2021, ApiGraderteAndreYtelserType.OMSORGSPENGER to 99, sykdomsgrad = 1))
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        søknadOgGodkjenningbehovKommerInn()

        // When:
        medPersonISpeil {
            val arbeidstakerVersjon =
                saksbehandlerEndrerGraderteAndreYtelser(
                    versjon = null,
                    dager = arbeidstakerDager,
                    yrkesaktivitet = ApiYrkesaktivitetstype.ARBEIDSTAKER,
                    organisasjonsnummer = "123456789",
                    notatTilBeslutter = "første notat",
                )
            val frilansVersjon =
                saksbehandlerEndrerGraderteAndreYtelser(
                    versjon = null,
                    dager = frilansDager,
                    yrkesaktivitet = ApiYrkesaktivitetstype.FRILANS,
                    organisasjonsnummer = null,
                    notatTilBeslutter = "andre notat",
                )

            // Then:
            assertEquals(2, graderteAndreYtelser.size())
            val perYrkesaktivitet = graderteAndreYtelser.toList().associateBy { it["yrkesaktivitet"].asString() }
            assertGraderteAndreYtelser(requireNotNull(perYrkesaktivitet["ARBEIDSTAKER"]), arbeidstakerVersjon, arbeidstakerDager)
            assertGraderteAndreYtelser(requireNotNull(perYrkesaktivitet["FRILANS"]), frilansVersjon, frilansDager)
        }
    }

    private fun dag(
        dato: LocalDate,
        annenYtelse: Pair<ApiGraderteAndreYtelserType, Int>,
        sykdomsgrad: Int,
    ) = ApiGraderteAndreYtelserDag(
        dato = dato,
        andreYtelserGrader = mapOf(annenYtelse),
        sykdomsgrad = sykdomsgrad,
        arbeidsgrad = 0,
    )

    private fun assertGraderteAndreYtelser(
        graderteAndreYtelser: JsonNode,
        expectedVersjon: UUID,
        expectedDager: List<ApiGraderteAndreYtelserDag>,
    ) {
        assertEquals(expectedVersjon, graderteAndreYtelser["versjon"].asUUID())
        assertEquals(expectedDager.size, graderteAndreYtelser["dager"].size())
        graderteAndreYtelser["dager"].forEachIndexed { index, dag ->
            val expected = expectedDager[index]
            assertEquals(expected.dato.toString(), dag["dato"].asString())
            assertEquals(expected.sykdomsgrad, dag["sykdomsgrad"].asInt())
            assertEquals(expected.arbeidsgrad, dag["arbeidsgrad"].asInt())
            expected.andreYtelserGrader.forEach { (type, grad) ->
                assertEquals(grad, dag["andreYtelserGrader"][type.name].asInt())
            }
        }
    }
}
