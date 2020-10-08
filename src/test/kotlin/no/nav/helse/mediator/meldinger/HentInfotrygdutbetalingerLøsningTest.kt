package no.nav.helse.mediator.meldinger

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.modell.person.PersonDao
import org.junit.jupiter.api.Test

internal class HentInfotrygdutbetalingerLøsningTest {
    private companion object {
        private const val FNR = "12345678911"
        private val objectMapper = jacksonObjectMapper()
    }

    private val dao = mockk<PersonDao>(relaxed = true)

    @Test
    fun `lagre infotrygdutbetalinger`() {
        val json = objectMapper.createObjectNode()
        val utbetalinger = HentInfotrygdutbetalingerLøsning(json)
        utbetalinger.lagre(dao)
        verify(exactly = 1) { dao.insertInfotrygdutbetalinger(json) }
    }

    @Test
    fun `oppdater infotrygdutbetalinger til person`() {
        every { dao.findInfotrygdutbetalinger(FNR) } returns "{}"
        val json = objectMapper.createObjectNode()
        val utbetalinger = HentInfotrygdutbetalingerLøsning(json)
        utbetalinger.oppdater(dao, FNR)
        verify(exactly = 1) { dao.updateInfotrygdutbetalinger(FNR, json) }
    }

    @Test
    fun `lagre ny infotrygdutbetalinger når person ikke har fra før`() {
        val utbetalingerRefId = 1
        every { dao.findInfotrygdutbetalinger(FNR) } returns null
        every { dao.insertInfotrygdutbetalinger(any()) } returns utbetalingerRefId
        val json = objectMapper.createObjectNode()
        val utbetalinger = HentInfotrygdutbetalingerLøsning(json)
        utbetalinger.oppdater(dao, FNR)
        verify(exactly = 1) { dao.insertInfotrygdutbetalinger(json) }
        verify(exactly = 1) { dao.updateInfotrygdutbetalingerRef(FNR, utbetalingerRefId) }
    }
}
