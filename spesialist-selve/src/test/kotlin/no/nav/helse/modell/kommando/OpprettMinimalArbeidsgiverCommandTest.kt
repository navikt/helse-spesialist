package no.nav.helse.modell.kommando

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import java.util.UUID
import no.nav.helse.mediator.meldinger.HentPersoninfoløsning
import no.nav.helse.mediator.meldinger.HentPersoninfoløsninger
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import no.nav.helse.spesialist.api.person.Kjønn
import org.junit.Ignore
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class OpprettMinimalArbeidsgiverCommandTest {
    private companion object {
        private const val ORGNR = "123456789"
    }

    private val dao = mockk<ArbeidsgiverDao>(relaxed = true)

    private lateinit var context: CommandContext
    private val command = OpprettMinimalArbeidsgiverCommand(ORGNR, dao)

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
        clearMocks(dao)
    }

    @Test
    fun `opprett arbeidsgiver`() {
        arbeidsgiverFinnesIkke()
        assertTrue(command.execute(context))
        verify(exactly = 1) { dao.insertArbeidsgiver(ORGNR) }
    }

    @Ignore
    @Test
    fun `opprett person-arbeidsgiver`() {
        val fnr = "12345678911"
        arbeidsgiverFinnesIkke(fnr)
        context.add(HentPersoninfoløsninger(listOf(
            HentPersoninfoløsning(fnr, "LITEN", null, "TRANFLASKE", LocalDate.of(1970, 1, 1), Kjønn.Kvinne, Adressebeskyttelse.Ugradert)
        )))
        val command = OpprettArbeidsgiverCommand(listOf(fnr), dao)
        assertTrue(command.execute(context))
        verify(exactly = 1) { dao.insertArbeidsgiver(fnr, "LITEN TRANFLASKE", listOf("Privatperson")) }
    }

    @Test
    fun `oppretter ikke arbeidsgiver når den finnes`() {
        arbeidsgiverFinnes()
        assertTrue(command.execute(context))
        verify(exactly = 0) { dao.insertArbeidsgiver(any()) }
    }

    private fun arbeidsgiverFinnes(orgnr: String = ORGNR) {
        every { dao.findArbeidsgiverByOrgnummer(orgnr) } returns 1
    }

    private fun arbeidsgiverFinnesIkke(orgnr: String = ORGNR) {
        every { dao.findArbeidsgiverByOrgnummer(orgnr) } returns null
    }
}
