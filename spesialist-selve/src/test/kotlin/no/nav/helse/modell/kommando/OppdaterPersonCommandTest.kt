package no.nav.helse.modell.kommando

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.db.PersonRepository
import no.nav.helse.mediator.CommandContextObserver
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.person.HentEnhetløsning
import no.nav.helse.modell.person.HentInfotrygdutbetalingerløsning
import no.nav.helse.modell.person.HentPersoninfoløsning
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

internal class OppdaterPersonCommandTest {
    private companion object {
        private const val FNR = "12345678911"
    }

    private val personRepository = mockk<PersonRepository>(relaxed = true)

    val førsteKjenteDagFinner = { LocalDate.now() }
    private val command = OppdaterPersonCommand(FNR, førsteKjenteDagFinner, personRepository)
    private lateinit var context: CommandContext

    private val observer = object : CommandContextObserver {
        val behov = mutableListOf<Behov>()

        override fun behov(behov: Behov, commandContextId: UUID) {
            this.behov.add(behov)
        }
    }

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
        context.nyObserver(observer)
        clearMocks(personRepository)
    }

    @Test
    fun `oppdaterer ingenting når informasjonen er ny nok`() {
        every { personRepository.finnPersoninfoSistOppdatert(FNR) } returns LocalDate.now()
        every { personRepository.finnEnhetSistOppdatert(FNR) } returns LocalDate.now()
        every { personRepository.finnITUtbetalingsperioderSistOppdatert(FNR) } returns LocalDate.now()
        assertTrue(command.execute(context))
        verify(exactly = 0) { personRepository.oppdaterEnhet(any(), any()) }
        verify(exactly = 0) { personRepository.upsertPersoninfo(any(), any(), any(), any(), any(), any(), any()) }
        verify(exactly = 0) { personRepository.upsertInfotrygdutbetalinger(any(), any()) }
    }

    @Test
    fun `trenger enhet`() {
        utdatertEnhet()
        assertFalse(command.execute(context))
        assertTrue(observer.behov.isNotEmpty())
        assertEquals(listOf(Behov.Enhet), observer.behov.toList())
    }

    @Test
    fun `oppdatere enhet`() {
        utdatertEnhet()
        val løsning = mockk<HentEnhetløsning>(relaxed = true)
        context.add(løsning)
        assertTrue(command.execute(context))
        verify(exactly = 1) { løsning.oppdater(personRepository, FNR) }
    }

    @Test
    fun `trenger infotrygdutbetalinger`() {
        utdatertUtbetalinger()
        assertFalse(command.execute(context))
        assertTrue(observer.behov.isNotEmpty())
        assertEquals(listOf(
            Behov.Infotrygdutbetalinger(
            fom = førsteKjenteDagFinner().minusYears(3),
            tom = LocalDate.now()
        )), observer.behov.toList())
    }

    @Test
    fun `oppdatere infotrygdutbetalinger`() {
        utdatertUtbetalinger()
        val løsning = mockk<HentInfotrygdutbetalingerløsning>(relaxed = true)
        context.add(løsning)
        assertTrue(command.execute(context))
        verify(exactly = 1) { løsning.oppdater(personRepository, FNR) }
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
        verify(exactly = 1) { personinfo.oppdater(personRepository, FNR) }
        verify(exactly = 1) { enhet.oppdater(personRepository, FNR) }
        verify(exactly = 1) { utbetalinger.oppdater(personRepository, FNR) }
    }

    private fun utdatertEnhet() {
        every { personRepository.finnPersoninfoSistOppdatert(FNR) } returns LocalDate.now()
        every { personRepository.finnEnhetSistOppdatert(FNR) } returns LocalDate.now().minusYears(1)
        every { personRepository.finnITUtbetalingsperioderSistOppdatert(FNR) } returns LocalDate.now()
    }

    private fun utdatertUtbetalinger() {
        every { personRepository.finnPersoninfoSistOppdatert(FNR) } returns LocalDate.now()
        every { personRepository.finnEnhetSistOppdatert(FNR) } returns LocalDate.now()
        every { personRepository.finnITUtbetalingsperioderSistOppdatert(FNR) } returns LocalDate.now().minusYears(1)
    }

    private fun altUtdatert() {
        every { personRepository.finnPersoninfoSistOppdatert(FNR) } returns LocalDate.now().minusYears(1)
        every { personRepository.finnEnhetSistOppdatert(FNR) } returns LocalDate.now().minusYears(1)
        every { personRepository.finnITUtbetalingsperioderSistOppdatert(FNR) } returns LocalDate.now().minusYears(1)
    }
}
