package no.nav.helse.modell

import no.nav.helse.objectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

internal class BehovTest {

    internal val behov = Behov(
        typer = listOf(
            Behovtype.HentEnhet,
            Behovtype.HentPersoninfo,
            Behovtype.HentInfotrygdutbetalinger()
        ),
        fødselsnummer = "12345679101",
        orgnummer = "123456789",
        spleisBehovId = UUID.randomUUID(),
        vedtaksperiodeId = UUID.randomUUID()
    )

    @Test
    fun `ekstrafelter legges på behov-json`() {
        val behov = objectMapper.readTree(behov.toJson())
        assertNotNull(behov["HentInfotrygdutbetalinger.historikkFom"])
        assertNotNull(behov["HentInfotrygdutbetalinger.historikkTom"])
    }

    @Test
    fun `@behov-nøkkel mappes riktig`() {
        val behov = objectMapper.readTree(behov.toJson())
        assertEquals("HentEnhet", behov["@behov"][0].textValue())
        assertEquals("HentPersoninfo", behov["@behov"][1].textValue())
        assertEquals("HentInfotrygdutbetalinger", behov["@behov"][2].textValue())
    }
}
