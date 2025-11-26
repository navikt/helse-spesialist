package no.nav.helse.mediator.meldinger

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.kafka.KlargjørPersonForVisningRiver
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.modell.person.KlargjørTilgangsrelaterteData
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagAktørId
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFødselsnummer
import no.nav.helse.spesialist.kafka.medRivers
import no.nav.helse.spesialist.kafka.testfixtures.Testmeldingfabrikk
import org.junit.jupiter.api.Test
import java.util.UUID

class KlargjørPersonForVisningRiverTest {

    @Test
    fun `leser melding`() {
        val mediator = mockk<MeldingMediator>(relaxed = true)
        val rapid = TestRapid().medRivers(KlargjørPersonForVisningRiver(mediator))
        rapid.sendTestMessage(Testmeldingfabrikk.lagKlargjørPersonForVisning(lagAktørId(), lagFødselsnummer(), UUID.randomUUID()))
        verify(exactly = 1) { mediator.mottaMelding(any<KlargjørTilgangsrelaterteData>(), any()) }
    }
}
