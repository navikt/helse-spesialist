package no.nav.helse.mediator.meldinger

import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.person.Adressebeskyttelse
import no.nav.helse.person.Kjønn
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class HentPersoninfoløsningTest {
    private companion object {
        private const val FNR = "123456789011"
        private const val FORNAVN = "KARI"
        private const val MELLOMNAVN = "Mellomnavn"
        private const val ETTERNAVN = "Nordmann"
        private val FØDSELSDATO = LocalDate.EPOCH
        private val KJØNN = Kjønn.Kvinne
        private val ADRESSEBESKYTTELSE = Adressebeskyttelse.Ugradert
    }

    private val dao = mockk<PersonDao>(relaxed = true)

    @Test
    fun `lagre personinfo`() {
        val info = HentPersoninfoløsning(FORNAVN, MELLOMNAVN, ETTERNAVN, FØDSELSDATO, KJØNN, ADRESSEBESKYTTELSE)
        info.lagre(dao)
        verify(exactly = 1) { dao.insertPersoninfo(FORNAVN, MELLOMNAVN, ETTERNAVN, FØDSELSDATO, KJØNN, ADRESSEBESKYTTELSE) }
    }

    @Test
    fun `oppdater personinfo`() {
        val info = HentPersoninfoløsning(FORNAVN, MELLOMNAVN, ETTERNAVN, FØDSELSDATO, KJØNN, ADRESSEBESKYTTELSE)
        info.oppdater(dao, FNR)
        verify(exactly = 1) { dao.updatePersoninfo(FNR, FORNAVN, MELLOMNAVN, ETTERNAVN, FØDSELSDATO, KJØNN, ADRESSEBESKYTTELSE) }
    }
}
