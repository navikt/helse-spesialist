package no.nav.helse.modell.kommando

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.mediator.meldinger.HentEnhetløsning
import no.nav.helse.mediator.meldinger.HentInfotrygdutbetalingerløsning
import no.nav.helse.mediator.meldinger.HentPersoninfoløsning
import no.nav.helse.mediator.meldinger.Kjønn
import no.nav.helse.modell.person.PersonDao
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class KlargjørPersonCommandTest {
    private companion object {
        private const val FNR = "12345678911"
        private const val AKTØR = "4321098765432"
        private const val FORNAVN = "KARI"
        private const val MELLOMNAVN = "Mellomnavn"
        private const val ETTERNAVN = "Nordmann"
        private const val ENHET_OSLO = "0301"
        private const val ENHET_UTLAND = "2101"
        private val FØDSELSDATO = LocalDate.EPOCH
        private val KJØNN = Kjønn.Kvinne

        private val objectMapper = jacksonObjectMapper()
    }

    private val dao = mockk<PersonDao>(relaxed = true)
    private val command = KlargjørPersonCommand(FNR, AKTØR, dao, """{"@event_name": "behov"}""", UUID.randomUUID())
    private lateinit var context: CommandContext

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
        clearMocks(dao)
    }

    @Test
    fun `oppretter person`() {
        personFinnesIkke()
        context.add(HentPersoninfoløsning(FORNAVN, MELLOMNAVN, ETTERNAVN, FØDSELSDATO, KJØNN))
        context.add(HentEnhetløsning(ENHET_OSLO))
        context.add(HentInfotrygdutbetalingerløsning(objectMapper.createObjectNode()))
        assertTrue(command.execute(context))
        assertFalse(context.harBehov())
        verify(exactly = 1) { dao.insertPerson(FNR, AKTØR, any(), any(), any()) }
    }

    @Test
    fun `person finnes, men personinfo er utdatert`() {
        personFinnes()
        altUtdatert()
        assertFalse(command.execute(context))
        assertHarBehov(listOf("HentPersoninfo"))
    }

    @Test
    fun `person finnes, men enhet og utbetalinger utdatert`() {
        personFinnes()
        personinfoOppdatert()
        assertFalse(command.execute(context))
        assertHarBehov(listOf("HentEnhet"))
    }

    @Test
    fun `person finnes, men utbetalinger utdatert`() {
        personFinnes()
        utdatertUtbetalinger()
        assertFalse(command.execute(context))
        assertHarBehov(listOf("HentInfotrygdutbetalinger"))
    }

    @Test
    fun `oppdaterer utdatert person`() {
        personFinnes()
        altUtdatert()
        val personinfo = mockk<HentPersoninfoløsning>(relaxed = true)
        val enhet = mockk<HentEnhetløsning>(relaxed = true)
        val utbetalinger = mockk<HentInfotrygdutbetalingerløsning>(relaxed = true)
        context.add(personinfo)
        context.add(enhet)
        context.add(utbetalinger)
        assertTrue(command.execute(context))
        verify(exactly = 1) { personinfo.oppdater(dao, FNR) }
        verify(exactly = 1) { enhet.oppdater(dao, FNR) }
        verify(exactly = 1) { utbetalinger.oppdater(dao, FNR) }
    }

    @Test
    fun `person finnes, alt er oppdatert`() {
        personFinnes()
        altOppdatert()
        assertTrue(command.execute(context))
    }

    @Test
    fun `sender løsning på godkjenning hvis bruker er utdatert og er tilknyttet utlandsenhet`() {
        personFinnes()
        altUtdatert()
        context.add(HentEnhetløsning(ENHET_UTLAND))
        context.add(mockk<HentPersoninfoløsning>(relaxed = true))
        context.add(mockk<HentInfotrygdutbetalingerløsning>(relaxed = true))
        assertTrue(command.execute(context))
        assertEquals(1, context.meldinger().size)
        assertFalse(
            no.nav.helse.objectMapper.readTree(context.meldinger().first())
                .path("@løsning")
                .path("Godkjenning")
                .path("godkjent")
                .booleanValue()
        )
    }

    @Test
    fun `sender ikke løsning på godkjenning hvis bruker er utdatert og ikke er tilknyttet utlandsenhet`() {
        personFinnes()
        altUtdatert()
        context.add(HentEnhetløsning(ENHET_OSLO))
        context.add(mockk<HentPersoninfoløsning>(relaxed = true))
        context.add(mockk<HentInfotrygdutbetalingerløsning>(relaxed = true))
        assertTrue(command.execute(context))
        assertEquals(0, context.meldinger().size)
    }

    @Test
    fun `sender løsning på godkjenning hvis bruker er tilknyttet utlandsenhet`() {
        context.add(HentEnhetløsning(ENHET_UTLAND))
        context.add(mockk<HentPersoninfoløsning>(relaxed = true))
        context.add(mockk<HentInfotrygdutbetalingerløsning>(relaxed = true))
        assertTrue(command.execute(context))
        assertEquals(1, context.meldinger().size)
        assertFalse(
            no.nav.helse.objectMapper.readTree(context.meldinger().first())
                .path("@løsning")
                .path("Godkjenning")
                .path("godkjent")
                .booleanValue()
        )
    }

    @Test
    fun `sender ikke løsning på godkjenning hvis bruker ikke er tilknyttet utlandsenhet`() {
        context.add(HentEnhetløsning(ENHET_OSLO))
        context.add(mockk<HentPersoninfoløsning>(relaxed = true))
        context.add(mockk<HentInfotrygdutbetalingerløsning>(relaxed = true))
        assertTrue(command.execute(context))
        assertEquals(0, context.meldinger().size)
    }

    private fun assertHarBehov(forventetBehov: List<String>) {
        assertTrue(context.harBehov())
        assertEquals(forventetBehov, context.behov().keys.toList())
        verify(exactly = 0) { dao.insertPerson(FNR, any(), any(), any(), any()) }
    }

    private fun personFinnes() {
        every { dao.findPersonByFødselsnummer(FNR) } returns 1
    }

    private fun personFinnesIkke() {
        every { dao.findPersonByFødselsnummer(FNR) } returns null
    }

    private fun altOppdatert() {
        every { dao.findPersoninfoSistOppdatert(FNR) } returns LocalDate.now()
        every { dao.findEnhetSistOppdatert(FNR) } returns LocalDate.now()
        every { dao.findITUtbetalingsperioderSistOppdatert(FNR) } returns LocalDate.now()
    }

    private fun altUtdatert() {
        every { dao.findPersoninfoSistOppdatert(FNR) } returns LocalDate.now().minusYears(1)
        every { dao.findEnhetSistOppdatert(FNR) } returns LocalDate.now().minusYears(1)
        every { dao.findITUtbetalingsperioderSistOppdatert(FNR) } returns LocalDate.now().minusYears(1)
    }

    private fun personinfoOppdatert() {
        every { dao.findPersoninfoSistOppdatert(FNR) } returns LocalDate.now()
        every { dao.findEnhetSistOppdatert(FNR) } returns LocalDate.now().minusYears(1)
        every { dao.findITUtbetalingsperioderSistOppdatert(FNR) } returns LocalDate.now().minusYears(1)
    }

    private fun utdatertUtbetalinger() {
        every { dao.findPersoninfoSistOppdatert(FNR) } returns LocalDate.now()
        every { dao.findEnhetSistOppdatert(FNR) } returns LocalDate.now()
        every { dao.findITUtbetalingsperioderSistOppdatert(FNR) } returns LocalDate.now().minusYears(1)
    }
}
