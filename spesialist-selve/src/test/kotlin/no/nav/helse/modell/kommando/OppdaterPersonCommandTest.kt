package no.nav.helse.modell.kommando

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import java.util.UUID
import no.nav.helse.mediator.CommandContextObserver
import no.nav.helse.modell.person.HentEnhetløsning
import no.nav.helse.modell.person.HentInfotrygdutbetalingerløsning
import no.nav.helse.modell.person.HentPersoninfoløsning
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.rapids_rivers.JsonMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class OppdaterPersonCommandTest {
    private companion object {
        private const val FNR = "12345678911"
    }

    private val personDao = mockk<PersonDao>(relaxed = true)

    private val command = OppdaterPersonCommand(FNR, { LocalDate.now() }, personDao)
    private lateinit var context: CommandContext

    private val observer = object : CommandContextObserver {
        val behov = mutableMapOf<String, Map<String, Any>>()
        override fun behov(behov: String, ekstraKontekst: Map<String, Any>, detaljer: Map<String, Any>) {
            this.behov[behov] = detaljer
        }

        override fun hendelse(hendelse: String) {}
    }

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
        context.nyObserver(observer)
        clearMocks(personDao)
    }

    @Test
    fun `oppdaterer ingenting når informasjonen er ny nok`() {
        every { personDao.findPersoninfoSistOppdatert(FNR) } returns LocalDate.now()
        every { personDao.findEnhetSistOppdatert(FNR) } returns LocalDate.now()
        every { personDao.findITUtbetalingsperioderSistOppdatert(FNR) } returns LocalDate.now()
        assertTrue(command.execute(context))
        verify(exactly = 0) { personDao.updateEnhet(any(), any()) }
        verify(exactly = 0) { personDao.upsertPersoninfo(any(), any(), any(), any(), any(), any(), any()) }
        verify(exactly = 0) { personDao.upsertInfotrygdutbetalinger(any(), any()) }
    }

    @Test
    fun `trenger enhet`() {
        utdatertEnhet()
        assertFalse(command.execute(context))
        assertTrue(observer.behov.isNotEmpty())
        assertEquals(listOf("HentEnhet"), observer.behov.keys.toList())
        println(JsonMessage.newMessage(observer.behov).toJson())
    }

    @Test
    fun `oppdatere enhet`() {
        utdatertEnhet()
        val løsning = mockk<HentEnhetløsning>(relaxed = true)
        context.add(løsning)
        assertTrue(command.execute(context))
        verify(exactly = 1) { løsning.oppdater(personDao, FNR) }
    }

    @Test
    fun `trenger infotrygdutbetalinger`() {
        utdatertUtbetalinger()
        assertFalse(command.execute(context))
        assertTrue(observer.behov.isNotEmpty())
        assertEquals(listOf("HentInfotrygdutbetalinger"), observer.behov.keys.toList())
        assertTrue(observer.behov.values.any { it["historikkFom"] != null && it["historikkTom"] != null})
    }

    @Test
    fun `oppdatere infotrygdutbetalinger`() {
        utdatertUtbetalinger()
        val løsning = mockk<HentInfotrygdutbetalingerløsning>(relaxed = true)
        context.add(løsning)
        assertTrue(command.execute(context))
        verify(exactly = 1) { løsning.oppdater(personDao, FNR) }
    }

    @Test
    fun `oppdatere alt`() {
        altUtdatert()
        val personinfo = mockk<HentPersoninfoløsning>(relaxed = true)
        val enhet = mockk<HentEnhetløsning>(relaxed = true)
        val utbetalinger = mockk<HentInfotrygdutbetalingerløsning>(relaxed = true)
        context.add(personinfo)
        context.add(enhet)
        context.add(utbetalinger)
        assertTrue(command.execute(context))
        verify(exactly = 1) { personinfo.oppdater(personDao, FNR) }
        verify(exactly = 1) { enhet.oppdater(personDao, FNR) }
        verify(exactly = 1) { utbetalinger.oppdater(personDao, FNR) }
    }

    private fun utdatertEnhet() {
        every { personDao.findPersoninfoSistOppdatert(FNR) } returns LocalDate.now()
        every { personDao.findEnhetSistOppdatert(FNR) } returns LocalDate.now().minusYears(1)
        every { personDao.findITUtbetalingsperioderSistOppdatert(FNR) } returns LocalDate.now()
    }

    private fun utdatertUtbetalinger() {
        every { personDao.findPersoninfoSistOppdatert(FNR) } returns LocalDate.now()
        every { personDao.findEnhetSistOppdatert(FNR) } returns LocalDate.now()
        every { personDao.findITUtbetalingsperioderSistOppdatert(FNR) } returns LocalDate.now().minusYears(1)
    }

    private fun altUtdatert() {
        every { personDao.findPersoninfoSistOppdatert(FNR) } returns LocalDate.now().minusYears(1)
        every { personDao.findEnhetSistOppdatert(FNR) } returns LocalDate.now().minusYears(1)
        every { personDao.findITUtbetalingsperioderSistOppdatert(FNR) } returns LocalDate.now().minusYears(1)
    }
}
