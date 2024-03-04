package no.nav.helse.modell.kommando

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import no.nav.helse.mediator.UtgåendeMeldingerObserver
import no.nav.helse.mediator.meldinger.løsninger.Inntekter
import no.nav.helse.mediator.meldinger.løsninger.Inntektløsning
import no.nav.helse.modell.person.PersonDao
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class PersisterInntektCommandTest {
    private companion object {
        private const val FNR = "12345678910"
    }

    private val personDao = mockk<PersonDao>(relaxed = true)
    private lateinit var context: CommandContext

    private val observer = object : UtgåendeMeldingerObserver {
        val behov = mutableListOf<String>()
        override fun behov(behov: String, ekstraKontekst: Map<String, Any>, detaljer: Map<String, Any>) {
            this.behov.add(behov)
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
    fun `Sender behov om inntekt ikke er lagret fra før`() {
        every { personDao.findInntekter(any(), any()) } returns null

        val command = PersisterInntektCommand(FNR, LocalDate.now(), personDao)

        assertFalse(command.execute(context))
        assertTrue(observer.behov.isNotEmpty())
    }

    @Test
    fun `Fullfører dersom inntekt er lagret fra før`() {
        every { personDao.findInntekter(any(), any()) } returns inntekter()

        val command = PersisterInntektCommand(FNR, LocalDate.now(), personDao)

        assertTrue(command.execute(context))
        assertTrue(observer.behov.isEmpty())
    }

    @Test
    fun `Lagrer inntekter dersom det ikke finnes på skjæringstidspunkt for person`() {
        val skjæringtidspunkt = LocalDate.now()

        every { personDao.findInntekter(FNR, skjæringtidspunkt) } returns null

        val command = PersisterInntektCommand(FNR, skjæringtidspunkt, personDao)

        assertFalse(command.execute(context))
        assertTrue(observer.behov.isNotEmpty())

        context.add(løsning())
        assertTrue(command.resume(context))
        verify(exactly = 1) { personDao.insertInntekter(FNR, skjæringtidspunkt, inntekter()) }
    }

    private fun løsning(inntekter: List<Inntekter> = inntekter()) = Inntektløsning(inntekter)

    private fun inntekter() = listOf(
        Inntekter(
            årMåned = YearMonth.parse("2022-11"),
            inntektsliste = listOf(Inntekter.Inntekt(beløp = 20000.0, orgnummer = "123456789"))
        )
    )
}
