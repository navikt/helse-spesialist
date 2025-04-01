package no.nav.helse.spesialist.e2etests.tests

import no.nav.helse.spesialist.e2etests.AbstractE2EIntegrationTest
import org.junit.jupiter.api.Test

class ManglerInntektsmeldingE2ETest : AbstractE2EIntegrationTest() {
    @Test
    fun `legger til egenskap på oppgaven når varsel RV_IV_10 er til stede`() {
        // Given:
        lagreVarseldefinisjon("RV_IV_10")

        // When:
        søknadOgGodkjenningbehovKommerInn(
            tilleggsmeldinger = {
                aktivitetsloggNyAktivitet(varselkoder = listOf("RV_IV_10"))
            }
        )

        // Then:
        medPersonISpeil {
            assertHarOppgaveegenskap("MANGLER_IM")
        }
    }
}
