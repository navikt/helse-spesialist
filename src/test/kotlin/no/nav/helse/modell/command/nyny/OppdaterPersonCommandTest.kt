package no.nav.helse.modell.command.nyny

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.modell.person.HentEnhetLøsning
import no.nav.helse.modell.person.HentInfotrygdutbetalingerLøsning
import no.nav.helse.modell.person.HentPersoninfoLøsning
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.rapids_rivers.JsonMessage
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class OppdaterPersonCommandTest {
    private companion object {
        private const val FNR = "12345678911"
    }

    private val dao = mockk<PersonDao>(relaxed = true)
    private val command = OppdaterPersonCommand(FNR, dao)
    private lateinit var context: CommandContext

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
        clearMocks(dao)
    }

    @Test
    fun `oppdaterer ingenting når informasjonen er ny nok`() {
        every { dao.findPersoninfoSistOppdatert(FNR) } returns LocalDate.now()
        every { dao.findEnhetSistOppdatert(FNR) } returns LocalDate.now()
        every { dao.findITUtbetalingsperioderSistOppdatert(FNR) } returns LocalDate.now()
        assertTrue(command.execute(context))
        verify(exactly = 0) { dao.updateEnhet(any(), any()) }
        verify(exactly = 0) { dao.updatePersoninfo(any(), any(), any(), any(), any(), any()) }
        verify(exactly = 0) { dao.updateInfotrygdutbetalinger(any(), any()) }
        verify(exactly = 0) { dao.updateInfotrygdutbetalingerRef(any(), any()) }
    }

    @Test
    fun `trenger personinfo`() {
        utdatertPersoninfo()
        assertFalse(command.execute(context))
        assertTrue(context.harBehov())
        assertEquals(listOf("HentPersoninfo"), context.behov().keys.toList())
    }

    @Test
    fun `oppdatere personinfo`() {
        utdatertPersoninfo()
        val løsning = mockk<HentPersoninfoLøsning>(relaxed = true)
        context.add(løsning)
        assertTrue(command.execute(context))
        verify(exactly = 1) { løsning.oppdater(dao, FNR) }
    }

    @Test
    fun `trenger enhet`() {
        utdatertEnhet()
        assertFalse(command.execute(context))
        assertTrue(context.harBehov())
        assertEquals(listOf("HentEnhet"), context.behov().keys.toList())
        println(JsonMessage.newMessage(context.behov()).toJson())
    }

    @Test
    fun `oppdatere enhet`() {
        utdatertEnhet()
        val løsning = mockk<HentEnhetLøsning>(relaxed = true)
        context.add(løsning)
        assertTrue(command.execute(context))
        verify(exactly = 1) { løsning.oppdater(dao, FNR) }
    }

    @Test
    fun `trenger infotrygdutbetalinger`() {
        utdatertUtbetalinger()
        assertFalse(command.execute(context))
        assertTrue(context.harBehov())
        assertEquals(listOf("HentInfotrygdutbetalinger"), context.behov().keys.toList())
        assertTrue(context.behov().values.any { it["historikkFom"] != null && it["historikkTom"] != null})
    }

    @Test
    fun `oppdatere infotrygdutbetalinger`() {
        utdatertUtbetalinger()
        val løsning = mockk<HentInfotrygdutbetalingerLøsning>(relaxed = true)
        context.add(løsning)
        assertTrue(command.execute(context))
        verify(exactly = 1) { løsning.oppdater(dao, FNR) }
    }

    @Test
    fun `oppdatere alt`() {
        altUtdatert()
        val personinfo = mockk<HentPersoninfoLøsning>(relaxed = true)
        val enhet = mockk<HentEnhetLøsning>(relaxed = true)
        val utbetalinger = mockk<HentInfotrygdutbetalingerLøsning>(relaxed = true)
        context.add(personinfo)
        context.add(enhet)
        context.add(utbetalinger)
        assertTrue(command.execute(context))
        verify(exactly = 1) { personinfo.oppdater(dao, FNR) }
        verify(exactly = 1) { enhet.oppdater(dao, FNR) }
        verify(exactly = 1) { utbetalinger.oppdater(dao, FNR) }
    }

    private fun utdatertPersoninfo() {
        every { dao.findPersoninfoSistOppdatert(FNR) } returns LocalDate.now().minusYears(1)
        every { dao.findEnhetSistOppdatert(FNR) } returns LocalDate.now()
        every { dao.findITUtbetalingsperioderSistOppdatert(FNR) } returns LocalDate.now()
    }

    private fun utdatertEnhet() {
        every { dao.findPersoninfoSistOppdatert(FNR) } returns LocalDate.now()
        every { dao.findEnhetSistOppdatert(FNR) } returns LocalDate.now().minusYears(1)
        every { dao.findITUtbetalingsperioderSistOppdatert(FNR) } returns LocalDate.now()
    }

    private fun utdatertUtbetalinger() {
        every { dao.findPersoninfoSistOppdatert(FNR) } returns LocalDate.now()
        every { dao.findEnhetSistOppdatert(FNR) } returns LocalDate.now()
        every { dao.findITUtbetalingsperioderSistOppdatert(FNR) } returns LocalDate.now().minusYears(1)
    }

    private fun altUtdatert() {
        every { dao.findPersoninfoSistOppdatert(FNR) } returns LocalDate.now().minusYears(1)
        every { dao.findEnhetSistOppdatert(FNR) } returns LocalDate.now().minusYears(1)
        every { dao.findITUtbetalingsperioderSistOppdatert(FNR) } returns LocalDate.now().minusYears(1)
    }
}
