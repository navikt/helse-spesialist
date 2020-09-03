package no.nav.helse.modell.command.nyny

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.modell.person.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class OpprettPersonCommandTest {
    private companion object {
        private const val FNR = "12345678911"
        private const val AKTØR = "4321098765432"
        private const val FORNAVN = "KARI"
        private const val MELLOMNAVN = "Mellomnavn"
        private const val ETTERNAVN = "Nordmann"
        private const val ENHET_OSLO = "0301"
        private val FØDSELSDATO = LocalDate.EPOCH
        private val KJØNN = Kjønn.Kvinne

        private val objectMapper = jacksonObjectMapper()
    }

    private val dao = mockk<PersonDao>(relaxed = true)
    private val command = OpprettPersonCommand(FNR, AKTØR, dao)
    private lateinit var context: CommandContext

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
        clearMocks(dao)
    }

    @Test
    fun `oppretter ikke person når person finnes fra før`() {
        personFinnes()
        assertTrue(command.execute(context))
        verify(exactly = 0) { dao.insertPerson(FNR, any(), any(), any(), any()) }
    }

    @Test
    fun `oppretter person`() {
        personFinnesIkke()
        context.add(HentPersoninfoLøsning(FORNAVN, MELLOMNAVN, ETTERNAVN, FØDSELSDATO, KJØNN))
        context.add(HentEnhetLøsning(ENHET_OSLO))
        context.add(HentInfotrygdutbetalingerLøsning(objectMapper.createObjectNode()))
        assertTrue(command.execute(context))
        assertFalse(context.harBehov())
        verify(exactly = 1) { dao.insertPerson(FNR, AKTØR.toLong(), any(), any(), any()) }
    }

    @Test
    fun `ber om manglende informasjon`() {
        personFinnesIkke()
        assertFalse(command.execute(context))
        assertHarBehov()
    }

    @Test
    fun `kan ikke opprette person med bare personinfo`() {
        personFinnesIkke()
        context.add(HentPersoninfoLøsning(FORNAVN, MELLOMNAVN, ETTERNAVN, FØDSELSDATO, Kjønn.Kvinne))
        assertFalse(command.execute(context))
        assertHarBehov()
    }

    @Test
    fun `kan ikke opprette person med bare enhet`() {
        personFinnesIkke()
        context.add(HentEnhetLøsning(ENHET_OSLO))
        assertFalse(command.execute(context))
        assertHarBehov()
    }

    @Test
    fun `kan ikke opprette person med bare infotrygdutbetalinger`() {
        personFinnesIkke()
        context.add(HentInfotrygdutbetalingerLøsning(objectMapper.createObjectNode()))
        assertFalse(command.execute(context))
        assertHarBehov()
    }

    private fun assertHarBehov() {
        assertTrue(context.harBehov())
        assertEquals(listOf("HentPersoninfo", "HentEnhet", "HentInfotrygdutbetalinger"), context.behov().keys.toList())
        verify(exactly = 0) { dao.insertPerson(FNR, any(), any(), any(), any()) }
    }

    private fun personFinnes() {
        every { dao.findPersonByFødselsnummer(FNR) } returns 1
    }
    private fun personFinnesIkke() {
        every { dao.findPersonByFødselsnummer(FNR) } returns null
    }

}
