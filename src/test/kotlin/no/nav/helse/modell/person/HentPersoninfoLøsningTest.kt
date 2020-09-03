package no.nav.helse.modell.person

import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class HentPersoninfoLøsningTest {
    private companion object {
        private const val FNR = "123456789011"
        private const val FORNAVN = "KARI"
        private const val MELLOMNAVN = "Mellomnavn"
        private const val ETTERNAVN = "Nordmann"
        private val FØDSELSDATO = LocalDate.EPOCH
        private val KJØNN = Kjønn.Kvinne
    }

    private val dao = mockk<PersonDao>(relaxed = true)

    @Test
    fun `lagre personinfo`() {
        val info = HentPersoninfoLøsning(FORNAVN, MELLOMNAVN, ETTERNAVN, FØDSELSDATO, KJØNN)
        info.lagre(dao)
        verify(exactly = 1) { dao.insertPersoninfo(FORNAVN, MELLOMNAVN, ETTERNAVN, FØDSELSDATO, KJØNN) }
    }

    @Test
    fun `oppdater personinfo`() {
        val info = HentPersoninfoLøsning(FORNAVN, MELLOMNAVN, ETTERNAVN, FØDSELSDATO, KJØNN)
        info.oppdater(dao, FNR)
        verify(exactly = 1) { dao.updatePersoninfo(FNR, FORNAVN, MELLOMNAVN, ETTERNAVN, FØDSELSDATO, KJØNN) }
    }
}
