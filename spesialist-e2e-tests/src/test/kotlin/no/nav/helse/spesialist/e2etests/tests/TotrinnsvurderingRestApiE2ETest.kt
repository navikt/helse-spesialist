package no.nav.helse.spesialist.e2etests.tests

import no.nav.helse.spesialist.domain.Periode.Companion.tilOgMed
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer
import no.nav.helse.spesialist.e2etests.AbstractE2EIntegrationTest
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class TotrinnsvurderingRestApiE2ETest : AbstractE2EIntegrationTest() {
    @Test
    fun `send til godkjenning via REST gir oppgaven til beslutteregenskap`() {
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
            saksbehandlerSenderTilGodkjenningMedRest()

            // Then:
            assertHarOppgaveegenskap("BESLUTTER")
            assertHarIkkeOppgaveegenskap("RETUR")
        }
    }
}
