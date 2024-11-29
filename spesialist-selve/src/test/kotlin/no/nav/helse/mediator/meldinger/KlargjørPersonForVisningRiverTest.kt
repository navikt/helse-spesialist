package no.nav.helse.mediator.meldinger

import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.Meldingssender
import no.nav.helse.kafka.KlargjørPersonForVisningRiver
import no.nav.helse.medRivers
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.modell.person.KlargjørTilgangsrelaterteData
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import no.nav.helse.spesialist.test.lagAktørId
import no.nav.helse.spesialist.test.lagFødselsnummer
import org.junit.jupiter.api.Test

class KlargjørPersonForVisningRiverTest {

    @Test
    fun `leser melding`() {
        val mediator = mockk<MeldingMediator>(relaxed = true)
        val rapid = TestRapid().medRivers(KlargjørPersonForVisningRiver(mediator))
        Meldingssender(rapid).sendKlargjørPersonForVisning(lagAktørId(), lagFødselsnummer())
        verify(exactly = 1) { mediator.mottaMelding(any<KlargjørTilgangsrelaterteData>(), any()) }
    }
}
