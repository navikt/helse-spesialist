package no.nav.helse.modell.kommando

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import java.time.LocalDate
import java.util.UUID
import no.nav.helse.mediator.meldinger.HentPersoninfoløsning
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import no.nav.helse.spesialist.api.person.Kjønn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class OppdaterPersoninfoCommandTest {
    private companion object {
        private const val FNR = "12345678911"
        private const val FORNAVN = "LITEN"
        private const val MELLOMNAVN = "STOR"
        private const val ETTERNAVN = "TRANFLASKE"
        private val FØDSELSDATO = LocalDate.EPOCH
        private val KJØNN = Kjønn.Ukjent
        private val ADRESSEBESKYTTELSE = Adressebeskyttelse.StrengtFortrolig
    }

    private val dao = mockk<PersonDao>(relaxed = true)


    @Test
    fun `trenger personinfo`() {
        val context = CommandContext(UUID.randomUUID())
        val command = OppdaterPersoninfoCommand(FNR, dao, force = false)
        utdatertPersoninfo()
        assertFalse(command.execute(context))
        assertTrue(context.harBehov())
        assertEquals(listOf("HentPersoninfoV2"), context.behov().keys.toList())
    }

    @Test
    fun `oppdatere personinfo`() {
        val context = CommandContext(UUID.randomUUID())
        val command = OppdaterPersoninfoCommand(FNR, dao, force = false)
        utdatertPersoninfo()
        val løsning = spyk(HentPersoninfoløsning(FNR, FORNAVN, MELLOMNAVN, ETTERNAVN, FØDSELSDATO, KJØNN, ADRESSEBESKYTTELSE))
        context.add(løsning)
        assertTrue(command.execute(context))
        verify(exactly = 1) { løsning.oppdater(dao, FNR) }
        verify(exactly = 1) { dao.upsertPersoninfo(FNR, FORNAVN, MELLOMNAVN, ETTERNAVN, FØDSELSDATO, KJØNN, ADRESSEBESKYTTELSE) }

    }

    @Test
    fun `oppdaterer ingenting når informasjonen er ny nok`() {
        val context = CommandContext(UUID.randomUUID())
        val command = OppdaterPersoninfoCommand(FNR, dao, force = false)
        every { dao.findPersoninfoSistOppdatert(FNR) } returns LocalDate.now()
        assertTrue(command.execute(context))
        verify(exactly = 0) { dao.upsertPersoninfo(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `oppdaterer personinfo dersom force er satt til true`() {
        val context = CommandContext(UUID.randomUUID())
        val command = OppdaterPersoninfoCommand(FNR, dao, force = true)
        val løsning = spyk(HentPersoninfoløsning(FNR, FORNAVN, MELLOMNAVN, ETTERNAVN, FØDSELSDATO, KJØNN, ADRESSEBESKYTTELSE))
        context.add(løsning)
        every { dao.findPersoninfoSistOppdatert(FNR) } returns LocalDate.now()
        assertTrue(command.execute(context))
        verify(exactly = 1) { løsning.oppdater(dao, FNR) }
        verify(exactly = 1) { dao.upsertPersoninfo(any(), any(), any(), any(), any(), any(), any()) }
    }


    private fun utdatertPersoninfo() {
        every { dao.findPersoninfoSistOppdatert(FNR) } returns LocalDate.now().minusYears(1)
        every { dao.findEnhetSistOppdatert(FNR) } returns LocalDate.now()
        every { dao.findITUtbetalingsperioderSistOppdatert(FNR) } returns LocalDate.now()
    }
}
