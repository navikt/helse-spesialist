package no.nav.helse.mediator.meldinger

import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.modell.person.HentPersoninfoløsning
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import no.nav.helse.spesialist.typer.Kjønn
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

    private val personDao = mockk<PersonDao>(relaxed = true)

    @Test
    fun `lagre personinfo`() {
        val info = HentPersoninfoløsning(FNR, FORNAVN, MELLOMNAVN, ETTERNAVN, FØDSELSDATO, KJØNN, ADRESSEBESKYTTELSE)
        info.lagre(personDao)
        verify(exactly = 1) { personDao.insertPersoninfo(FORNAVN, MELLOMNAVN, ETTERNAVN, FØDSELSDATO, KJØNN, ADRESSEBESKYTTELSE) }
    }

    @Test
    fun `oppdater personinfo`() {
        val info = HentPersoninfoløsning(FNR, FORNAVN, MELLOMNAVN, ETTERNAVN, FØDSELSDATO, KJØNN, ADRESSEBESKYTTELSE)
        info.oppdater(personDao, FNR)
        verify(exactly = 1) { personDao.upsertPersoninfo(FNR, FORNAVN, MELLOMNAVN, ETTERNAVN, FØDSELSDATO, KJØNN, ADRESSEBESKYTTELSE) }
    }
}
