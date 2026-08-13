package no.nav.helse.spesialist.e2etests.tests

import no.nav.helse.mediator.asUUID
import no.nav.helse.spesialist.api.rest.ApiGraderteAndreYtelseType
import no.nav.helse.spesialist.api.rest.ApiGraderteAndreYtelserPeriode
import no.nav.helse.spesialist.domain.Periode.Companion.tilOgMed
import no.nav.helse.spesialist.domain.andreytelser.AndreYtelserPeriode.GraderteAndreYtelserPeriode
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
        val ApiPerioder =
            listOf(
                ApiGraderteAndreYtelserPeriode(
                    fom = 2 jan 2021,
                    tom = 20 jan 2021,
                    grad = 50,
                ),
                ApiGraderteAndreYtelserPeriode(
                    fom = 21 jan 2021,
                    tom = 31 jan 2021,
                    grad = 80,
                ),
            )
        val domainPerioder =
            ApiPerioder.map { apiPeriode ->
                GraderteAndreYtelserPeriode(
                    periode = apiPeriode.fom tilOgMed apiPeriode.tom,
                    grad = apiPeriode.grad,
                )
            }
        val apiAndreYtelseType = ApiGraderteAndreYtelseType.FORELDREPENGER
        val domainAndreYtelseType = GraderteAndreYtelserType.FORELDREPENGER
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        søknadOgGodkjenningbehovKommerInn()

        // When:
        medPersonISpeil {
            val andreYtelserId =
                saksbehandlerLeggerTilGraderteAndreYtelser(
                    perioder = ApiPerioder,
                    andreYtelseType = apiAndreYtelseType,
                    notatTilBeslutter = "notat",
                )

            // Then:
            assertEquals(1, graderteAndreYtelser.size())
            assertGraderteAndreYtelser(
                graderteAndreYtelser = graderteAndreYtelser[0],
                expectedAndreYtelserId = andreYtelserId,
                expectedPerioder = domainPerioder,
                expectedAndreYtelseType = domainAndreYtelseType,
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
        val apiFørstePeriode =
            listOf(
                ApiGraderteAndreYtelserPeriode(
                    fom = 2 jan 2021,
                    tom = 15 jan 2021,
                    grad = 50,
                ),
            )
        val apiAndrePerioder =
            listOf(
                ApiGraderteAndreYtelserPeriode(
                    fom = 16 jan 2021,
                    tom = 31 jan 2021,
                    grad = 100,
                ),
            )
        val domainFørstePerioder =
            apiFørstePeriode.map { apiPeriode ->
                GraderteAndreYtelserPeriode(
                    periode = apiPeriode.fom tilOgMed apiPeriode.tom,
                    grad = apiPeriode.grad,
                )
            }
        val domainAndrePerioder =
            apiAndrePerioder.map { apiPeriode ->
                GraderteAndreYtelserPeriode(
                    periode = apiPeriode.fom tilOgMed apiPeriode.tom,
                    grad = apiPeriode.grad,
                )
            }
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        søknadOgGodkjenningbehovKommerInn()

        // When:
        medPersonISpeil {
            val førsteAndreYtelserId =
                saksbehandlerLeggerTilGraderteAndreYtelser(
                    perioder = apiFørstePeriode,
                    andreYtelseType = ApiGraderteAndreYtelseType.PLEIEPENGER,
                    notatTilBeslutter = "første notat",
                )
            val andreAndreYtelserId =
                saksbehandlerLeggerTilGraderteAndreYtelser(
                    perioder = apiAndrePerioder,
                    andreYtelseType = ApiGraderteAndreYtelseType.OMSORGSPENGER,
                    notatTilBeslutter = "andre notat",
                )

            // Then:
            assertEquals(2, graderteAndreYtelser.size())
            val graderteAndreYtelserPerId = graderteAndreYtelser.toList().associateBy { it["andreYtelserId"].asUUID() }
            assertGraderteAndreYtelser(
                graderteAndreYtelser = requireNotNull(graderteAndreYtelserPerId[førsteAndreYtelserId]),
                expectedAndreYtelserId = førsteAndreYtelserId,
                expectedPerioder = domainFørstePerioder,
                expectedAndreYtelseType = GraderteAndreYtelserType.PLEIEPENGER,
            )
            assertGraderteAndreYtelser(
                graderteAndreYtelser = requireNotNull(graderteAndreYtelserPerId[andreAndreYtelserId]),
                expectedAndreYtelserId = andreAndreYtelserId,
                expectedPerioder = domainAndrePerioder,
                expectedAndreYtelseType = GraderteAndreYtelserType.OMSORGSPENGER,
            )
        }
    }

    private fun assertGraderteAndreYtelser(
        graderteAndreYtelser: JsonNode,
        expectedAndreYtelserId: UUID,
        expectedPerioder: List<GraderteAndreYtelserPeriode>,
        expectedAndreYtelseType: GraderteAndreYtelserType,
    ) {
        assertEquals(expectedAndreYtelserId, graderteAndreYtelser["andreYtelserId"].asUUID())
        assertEquals(expectedAndreYtelseType.name, graderteAndreYtelser["andreYtelseType"].asString())
        assertEquals(expectedPerioder.size, graderteAndreYtelser["perioder"].size())
        graderteAndreYtelser["perioder"].forEachIndexed { index, periode ->
            assertEquals(expectedPerioder[index].periode.fom.toString(), periode["fom"].asString())
            assertEquals(expectedPerioder[index].periode.tom.toString(), periode["tom"].asString())
            assertEquals(expectedPerioder[index].grad, periode["grad"].asInt())
        }
    }
}
