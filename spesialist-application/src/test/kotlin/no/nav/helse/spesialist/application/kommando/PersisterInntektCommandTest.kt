package no.nav.helse.spesialist.application.kommando

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.db.PersonDao
import no.nav.helse.mediator.CommandContextObserver
import no.nav.helse.mediator.meldinger.løsninger.Inntekter
import no.nav.helse.mediator.meldinger.løsninger.Inntektløsning
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.PersisterInntektCommand
import no.nav.helse.modell.melding.Behov
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

internal class PersisterInntektCommandTest : ApplicationTest() {
    private companion object {
        private const val FNR = "12345678910"
    }

    private val personDao = mockk<PersonDao>(relaxed = true)
    private val observer =
        object : CommandContextObserver {
            val behov = mutableListOf<Behov>()

            override fun behov(
                behov: Behov,
                commandContextId: UUID,
                sti: List<Int>,
            ) {
                this.behov.add(behov)
            }
        }
    private val commandContext: CommandContext = CommandContext(UUID.randomUUID()).also { it.nyObserver(observer) }

    @BeforeEach
    fun setup() {
        clearMocks(personDao)
    }

    @Test
    fun `Sender behov om inntekt ikke er lagret fra før`() {
        every { personDao.finnInntekter(any(), any()) } returns null

        val command = PersisterInntektCommand(FNR, LocalDate.now(), personDao)

        assertFalse(command.execute(commandContext, sessionContext, outbox))
        assertTrue(observer.behov.isNotEmpty())
    }

    @Test
    fun `Fullfører dersom inntekt er lagret fra før`() {
        every { personDao.finnInntekter(any(), any()) } returns inntekter()

        val command = PersisterInntektCommand(FNR, LocalDate.now(), personDao)

        assertTrue(command.execute(commandContext, sessionContext, outbox))
        assertTrue(observer.behov.isEmpty())
    }

    @Test
    fun `Lagrer inntekter dersom det ikke finnes på skjæringstidspunkt for person`() {
        val skjæringtidspunkt = LocalDate.now()

        every { personDao.finnInntekter(FNR, skjæringtidspunkt) } returns null

        val command = PersisterInntektCommand(FNR, skjæringtidspunkt, personDao)

        assertFalse(command.execute(commandContext, sessionContext, outbox))
        assertTrue(observer.behov.isNotEmpty())

        commandContext.add(løsning())
        assertTrue(command.resume(commandContext, sessionContext, outbox))
        verify(exactly = 1) { personDao.lagreInntekter(FNR, skjæringtidspunkt, inntekter()) }
    }

    @Test
    fun `Bryr oss ikke om løsning dersom vi har inntekter alt`() {
        val skjæringtidspunkt = LocalDate.now()
        every { personDao.finnInntekter(FNR, skjæringtidspunkt) } returns inntekter()

        val command = PersisterInntektCommand(FNR, skjæringtidspunkt, personDao)

        assertTrue(command.resume(commandContext, sessionContext, outbox))
        verify(exactly = 0) { personDao.lagreInntekter(any(), any(), any()) }
    }

    private fun løsning(inntekter: List<Inntekter> = inntekter()) = Inntektløsning(inntekter)

    private fun inntekter() =
        listOf(
            Inntekter(
                årMåned = YearMonth.parse("2022-11"),
                inntektsliste = listOf(Inntekter.Inntekt(beløp = 20000.0, orgnummer = "123456789")),
            ),
        )
}
