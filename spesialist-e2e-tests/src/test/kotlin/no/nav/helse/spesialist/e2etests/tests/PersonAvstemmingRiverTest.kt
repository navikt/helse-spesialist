package no.nav.helse.spesialist.e2etests.tests

import no.nav.helse.spesialist.e2etests.AbstractE2EIntegrationTest
import org.junit.jupiter.api.Test

class PersonAvstemmingRiverTest : AbstractE2EIntegrationTest() {

    @Test
    fun `Leser inn person_avstemt event`() {
        søknadOgGodkjenningbehovKommerInn()
        avstemming()
    }
}
