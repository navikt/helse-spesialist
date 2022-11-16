package no.nav.helse.mediator.meldinger

import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import no.nav.helse.mediator.meldinger.løsninger.HentPersoninfoløsning
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import no.nav.helse.spesialist.api.person.Kjønn
import org.junit.jupiter.api.Test

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
        val info = HentPersoninfoløsning(FNR, FORNAVN, MELLOMNAVN, ETTERNAVN, FØDSELSDATO, KJØNN, ADRESSEBESKYTTELSE)
        info.lagre(dao)
        verify(exactly = 1) { dao.insertPersoninfo(FORNAVN, MELLOMNAVN, ETTERNAVN, FØDSELSDATO, KJØNN, ADRESSEBESKYTTELSE) }
    }

    @Test
    fun `oppdater personinfo`() {
        val info = HentPersoninfoløsning(FNR, FORNAVN, MELLOMNAVN, ETTERNAVN, FØDSELSDATO, KJØNN, ADRESSEBESKYTTELSE)
        info.oppdater(dao, FNR)
        verify(exactly = 1) { dao.upsertPersoninfo(FNR, FORNAVN, MELLOMNAVN, ETTERNAVN, FØDSELSDATO, KJØNN, ADRESSEBESKYTTELSE) }
    }
}
