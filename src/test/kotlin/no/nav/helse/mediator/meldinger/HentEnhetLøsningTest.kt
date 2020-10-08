package no.nav.helse.mediator.meldinger

import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.modell.person.PersonDao
import org.junit.jupiter.api.Test

internal class HentEnhetLøsningTest {
    private companion object {
        private const val FNR = "12345678911"
        private const val AKTØR = "4321098765432"
        private const val ENHET_OSLO = "0301"
        private const val ENHET_SVALBARD = "2100"
        private const val NAVN_REF = 1
        private const val INFOTRYGDUTBETALINGER_REF = 2
    }

    private val dao = mockk<PersonDao>(relaxed = true)

    @Test
    fun `lagre person`() {
        val enhet = HentEnhetLøsning(ENHET_OSLO)
        enhet.lagrePerson(dao, FNR, AKTØR, NAVN_REF, INFOTRYGDUTBETALINGER_REF)
        verify(exactly = 1) { dao.insertPerson(FNR, AKTØR, NAVN_REF, ENHET_OSLO.toInt(), INFOTRYGDUTBETALINGER_REF) }
    }

    @Test
    fun `oppdatere enhet`() {
        val enhet = HentEnhetLøsning(ENHET_SVALBARD)
        enhet.oppdater(dao, FNR)
        verify(exactly = 1) { dao.updateEnhet(FNR, ENHET_SVALBARD.toInt()) }
    }
}
