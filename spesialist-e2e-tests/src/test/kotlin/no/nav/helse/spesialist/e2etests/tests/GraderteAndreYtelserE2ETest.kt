package no.nav.helse.spesialist.e2etests.tests

import no.nav.helse.mediator.asUUID
import no.nav.helse.spesialist.api.rest.ApiGraderteAndreYtelserType
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelserType
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.e2etests.AbstractE2EIntegrationTest
import org.junit.jupiter.api.Test
import tools.jackson.databind.JsonNode
import java.util.*
import kotlin.test.assertEquals

class GraderteAndreYtelserE2ETest : AbstractE2EIntegrationTest() {
    @Test
    fun `saksbehandler legger til og henter graderte andre ytelser`() {
        // Given:
        førsteVedtaksperiode().apply {
            fom = 1 jan 2021
            tom = 31 jan 2021
        }
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        søknadOgGodkjenningbehovKommerInn()

        // When:
        medPersonISpeil {
            val andreYtelserId =
                saksbehandlerLeggerTilGraderteAndreYtelser(
                    andreYtelserType = ApiGraderteAndreYtelserType.FORELDREPENGER,
                    notatTilBeslutter = "notat",
                )

            // Then:
            assertEquals(1, graderteAndreYtelser.size())
            assertGraderteAndreYtelser(
                graderteAndreYtelser = graderteAndreYtelser[0],
                expectedAndreYtelserId = andreYtelserId,
                expectedAndreYtelserType = GraderteAndreYtelserType.FORELDREPENGER,
            )
        }
    }

    @Test
    fun `saksbehandler henter flere graderte andre ytelser for samme person`() {
        // Given:
        førsteVedtaksperiode().apply {
            fom = 1 jan 2021
            tom = 31 jan 2021
        }
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        søknadOgGodkjenningbehovKommerInn()

        // When:
        medPersonISpeil {
            val førsteAndreYtelserId =
                saksbehandlerLeggerTilGraderteAndreYtelser(
                    andreYtelserType = ApiGraderteAndreYtelserType.PLEIEPENGER,
                    notatTilBeslutter = "første notat",
                )
            val andreAndreYtelserId =
                saksbehandlerLeggerTilGraderteAndreYtelser(
                    andreYtelserType = ApiGraderteAndreYtelserType.OMSORGSPENGER,
                    notatTilBeslutter = "andre notat",
                )

            // Then:
            assertEquals(2, graderteAndreYtelser.size())
            val graderteAndreYtelserPerId = graderteAndreYtelser.toList().associateBy { it["andreYtelserId"].asUUID() }
            assertGraderteAndreYtelser(
                graderteAndreYtelser = requireNotNull(graderteAndreYtelserPerId[førsteAndreYtelserId]),
                expectedAndreYtelserId = førsteAndreYtelserId,
                expectedAndreYtelserType = GraderteAndreYtelserType.PLEIEPENGER,
            )
            assertGraderteAndreYtelser(
                graderteAndreYtelser = requireNotNull(graderteAndreYtelserPerId[andreAndreYtelserId]),
                expectedAndreYtelserId = andreAndreYtelserId,
                expectedAndreYtelserType = GraderteAndreYtelserType.OMSORGSPENGER,
            )
        }
    }

    private fun assertGraderteAndreYtelser(
        graderteAndreYtelser: JsonNode,
        expectedAndreYtelserId: UUID,
        expectedAndreYtelserType: GraderteAndreYtelserType,
    ) {
        assertEquals(expectedAndreYtelserId, graderteAndreYtelser["andreYtelserId"].asUUID())
        assertEquals(expectedAndreYtelserType.name, graderteAndreYtelser["andreYtelserType"].asString())
    }
}
