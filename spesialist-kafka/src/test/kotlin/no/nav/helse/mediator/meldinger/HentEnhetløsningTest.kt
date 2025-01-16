package no.nav.helse.mediator.meldinger

import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.modell.person.HentEnhetløsning
import no.nav.helse.modell.person.PersonDao
import org.junit.jupiter.api.Test

internal class HentEnhetløsningTest {
    private companion object {
        private const val FNR = "12345678911"
        private const val AKTØR = "4321098765432"
        private const val ENHET_OSLO = "0301"
        private const val ENHET_SVALBARD = "2100"
        private const val NAVN_REF = 1L
        private const val INFOTRYGDUTBETALINGER_REF = 2L
    }

    private val personDao = mockk<PersonDao>(relaxed = true)

    @Test
    fun `lagre person`() {
        val enhet = HentEnhetløsning(ENHET_OSLO)
        enhet.lagrePerson(personDao, FNR, AKTØR, NAVN_REF, INFOTRYGDUTBETALINGER_REF)
        verify(exactly = 1) { personDao.insertPerson(FNR, AKTØR, NAVN_REF, ENHET_OSLO.toInt(), INFOTRYGDUTBETALINGER_REF) }
    }

    @Test
    fun `oppdatere enhet`() {
        val enhet = HentEnhetløsning(ENHET_SVALBARD)
        enhet.oppdater(personDao, FNR)
        verify(exactly = 1) { personDao.oppdaterEnhet(FNR, ENHET_SVALBARD.toInt()) }
    }
}
