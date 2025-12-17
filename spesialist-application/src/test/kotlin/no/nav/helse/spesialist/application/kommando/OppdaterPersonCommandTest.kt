package no.nav.helse.spesialist.application.kommando

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.db.PersonDao
import no.nav.helse.mediator.CommandContextObserver
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.OppdaterPersonCommand
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

    private val personDao = mockk<PersonDao>(relaxed = true)

    val førsteKjenteDagFinner = { LocalDate.now() }
    private val command = OppdaterPersonCommand(FNR, førsteKjenteDagFinner, personDao)
    private lateinit var context: CommandContext

    private val observer =
        object : CommandContextObserver {
            val behov = mutableListOf<Behov>()

            override fun behov(
                behov: Behov,
                commandContextId: UUID,
            ) {
                this.behov.add(behov)
            }
        }

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
        context.nyObserver(observer)
        clearMocks(personDao)
    }

    @Test
    fun `oppdaterer ingenting når informasjonen er ny nok`() =
        testMedSessionContext {
            every { personDao.finnPersoninfoSistOppdatert(FNR) } returns LocalDate.now()
            every { personDao.finnEnhetSistOppdatert(FNR) } returns LocalDate.now()
            every { personDao.finnITUtbetalingsperioderSistOppdatert(FNR) } returns LocalDate.now()
            assertTrue(command.execute(context, it))
            verify(exactly = 0) { personDao.oppdaterEnhet(any(), any()) }
            verify(exactly = 0) { personDao.upsertPersoninfo(any(), any(), any(), any(), any(), any(), any()) }
            verify(exactly = 0) { personDao.upsertInfotrygdutbetalinger(any(), any()) }
        }

    @Test
    fun `trenger enhet`() =
        testMedSessionContext {
            utdatertEnhet()
            assertFalse(command.execute(context, it))
            assertTrue(observer.behov.isNotEmpty())
            assertEquals(listOf(Behov.Enhet), observer.behov.toList())
        }

    @Test
    fun `oppdatere enhet`() =
        testMedSessionContext {
            utdatertEnhet()
            val løsning = mockk<HentEnhetløsning>(relaxed = true)
            context.add(løsning)
            assertTrue(command.execute(context, it))
            verify(exactly = 1) { løsning.oppdater(personDao, FNR) }
        }

    @Test
    fun `trenger infotrygdutbetalinger`() =
        testMedSessionContext {
            utdatertUtbetalinger()
            assertFalse(command.execute(context, it))
            assertTrue(observer.behov.isNotEmpty())
            assertEquals(
                listOf(
                    Behov.Infotrygdutbetalinger(
                        fom = førsteKjenteDagFinner().minusYears(3),
                        tom = LocalDate.now(),
                    ),
                ),
                observer.behov.toList(),
            )
        }

    @Test
    fun `oppdatere infotrygdutbetalinger`() =
        testMedSessionContext {
            utdatertUtbetalinger()
            val løsning = mockk<HentInfotrygdutbetalingerløsning>(relaxed = true)
            context.add(løsning)
            assertTrue(command.execute(context, it))
            verify(exactly = 1) { løsning.oppdater(personDao, FNR) }
        }

    @Test
    fun `oppdatere alt`() =
        testMedSessionContext {
            altUtdatert()
            val personinfo = mockk<HentPersoninfoløsning>(relaxed = true)
            val enhet = mockk<HentEnhetløsning>(relaxed = true)
            val utbetalinger = mockk<HentInfotrygdutbetalingerløsning>(relaxed = true)
            context.add(personinfo)
            context.add(enhet)
            context.add(utbetalinger)
            assertTrue(command.execute(context, it))
            verify(exactly = 1) { personinfo.oppdater(personDao, FNR) }
            verify(exactly = 1) { enhet.oppdater(personDao, FNR) }
            verify(exactly = 1) { utbetalinger.oppdater(personDao, FNR) }
        }

    private fun utdatertEnhet() {
        every { personDao.finnPersoninfoSistOppdatert(FNR) } returns LocalDate.now()
        every { personDao.finnEnhetSistOppdatert(FNR) } returns LocalDate.now().minusYears(1)
        every { personDao.finnITUtbetalingsperioderSistOppdatert(FNR) } returns LocalDate.now()
    }

    private fun utdatertUtbetalinger() {
        every { personDao.finnPersoninfoSistOppdatert(FNR) } returns LocalDate.now()
        every { personDao.finnEnhetSistOppdatert(FNR) } returns LocalDate.now()
        every { personDao.finnITUtbetalingsperioderSistOppdatert(FNR) } returns LocalDate.now().minusYears(1)
    }

    private fun altUtdatert() {
        every { personDao.finnPersoninfoSistOppdatert(FNR) } returns LocalDate.now().minusYears(1)
        every { personDao.finnEnhetSistOppdatert(FNR) } returns LocalDate.now().minusYears(1)
        every { personDao.finnITUtbetalingsperioderSistOppdatert(FNR) } returns LocalDate.now().minusYears(1)
    }
}
