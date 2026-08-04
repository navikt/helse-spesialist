package no.nav.helse.spesialist.e2etests.tests

import no.nav.helse.spesialist.domain.Periode.Companion.tilOgMed
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer
import no.nav.helse.spesialist.e2etests.AbstractE2EIntegrationTest
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertEquals

class TotrinnsvurderingGraphQLApiE2ETest : AbstractE2EIntegrationTest() {
    @Test
    fun `sendTilGodkjenningV2 returnerer true og setter oppgaven til beslutteroppgave`() {
        // Given:
        førsteVedtaksperiode().apply {
            fom = 1 jan 2021
            tom = 31 jan 2021
        }
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        søknadOgGodkjenningbehovKommerInn()

        // When:
        medPersonISpeil {
            saksbehandlerTildelerSegSaken()
            saksbehandlerLeggerTilTilkommenInntekt(
                organisasjonsnummer = lagOrganisasjonsnummer(),
                periode = (2 jan 2021) tilOgMed (31 jan 2021),
                periodebeløp = BigDecimal("1111.11"),
                ekskluderteUkedager = emptyList(),
                notatTilBeslutter = "notat",
            )
            saksbehandlerGodkjennerAlleVarsler()
            val response = saksbehandlerSenderTilGodkjenning()

            // Then:
            assertEquals(true, response["data"]["sendTilGodkjenningV2"].asBoolean())
            assertHarOppgaveegenskap("BESLUTTER")
            assertHarIkkeOppgaveegenskap("RETUR")
        }
    }

    @Test
    fun `sendIRetur returnerer true og setter oppgaven til returoppgave`() {
        // Given:
        førsteVedtaksperiode().apply {
            fom = 1 jan 2021
            tom = 31 jan 2021
        }
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        søknadOgGodkjenningbehovKommerInn()

        medPersonISpeil {
            saksbehandlerTildelerSegSaken()
            saksbehandlerLeggerTilTilkommenInntekt(
                organisasjonsnummer = lagOrganisasjonsnummer(),
                periode = (2 jan 2021) tilOgMed (31 jan 2021),
                periodebeløp = BigDecimal("1111.11"),
                ekskluderteUkedager = emptyList(),
                notatTilBeslutter = "notat",
            )
            saksbehandlerGodkjennerAlleVarsler()
            saksbehandlerSenderTilGodkjenning()
        }

        // When:
        beslutterMedPersonISpeil {
            val response = saksbehandlerSenderIRetur("Trenger en ny vurdering")

            // Then:
            assertEquals(true, response["data"]["sendIRetur"].asBoolean())
            assertHarOppgaveegenskap("RETUR")
            assertHarIkkeOppgaveegenskap("BESLUTTER")
        }
    }
}
