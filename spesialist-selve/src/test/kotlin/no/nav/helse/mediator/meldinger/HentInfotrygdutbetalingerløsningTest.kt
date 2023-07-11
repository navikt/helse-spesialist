package no.nav.helse.mediator.meldinger

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.modell.person.HentInfotrygdutbetalingerløsning
import no.nav.helse.modell.person.PersonDao
import org.junit.jupiter.api.Test

internal class HentInfotrygdutbetalingerløsningTest {
    private companion object {
        private const val FNR = "12345678911"
        private val objectMapper = jacksonObjectMapper()
    }

    private val dao = mockk<PersonDao>(relaxed = true)

    @Test
    fun `lagre infotrygdutbetalinger`() {
        val json = objectMapper.createObjectNode()
        val utbetalinger = HentInfotrygdutbetalingerløsning(json)
        utbetalinger.lagre(dao)
        verify(exactly = 1) { dao.insertInfotrygdutbetalinger(json) }
    }

    @Test
    fun `oppdater infotrygdutbetalinger til person`() {
        every { dao.findInfotrygdutbetalinger(FNR) } returns "{}"
        val json = objectMapper.createObjectNode()
        val utbetalinger = HentInfotrygdutbetalingerløsning(json)
        utbetalinger.oppdater(dao, FNR)
        verify(exactly = 1) { dao.upsertInfotrygdutbetalinger(FNR, json) }
    }

    @Test
    fun `lagre ny infotrygdutbetalinger når person ikke har fra før`() {
        val json = objectMapper.createObjectNode()
        val utbetalinger = HentInfotrygdutbetalingerløsning(json)
        utbetalinger.oppdater(dao, FNR)
        verify(exactly = 1) { dao.upsertInfotrygdutbetalinger(FNR, json) }
    }
}
