package no.nav.helse.mediator.meldinger

import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.db.PersonDao
import no.nav.helse.modell.person.HentEnhetløsning
import org.junit.jupiter.api.Test

internal class HentEnhetløsningTest {
    private companion object {
        private const val FNR = "12345678911"
        private const val ENHET_SVALBARD = "2100"
    }

    private val personDao = mockk<PersonDao>(relaxed = true)

    @Test
    fun `oppdatere enhet`() {
        val enhet = HentEnhetløsning(ENHET_SVALBARD)
        enhet.oppdater(personDao, FNR)
        verify(exactly = 1) { personDao.oppdaterEnhet(FNR, ENHET_SVALBARD.toInt()) }
    }
}
