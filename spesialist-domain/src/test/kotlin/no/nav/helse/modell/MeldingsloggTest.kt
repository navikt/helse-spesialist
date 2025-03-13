package no.nav.helse.modell

import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.melding.VedtaksperiodeGodkjentAutomatisk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class MeldingsloggTest {

    @Test
    fun `kan legge til og hente ut hendelser fra meldingsloggen`() {
        val logg = Meldingslogg()
        val hendelse = VedtaksperiodeGodkjentAutomatisk(
            fødselsnummer = no.nav.helse.spesialist.testhjelp.lagFødselsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
            behandlingId = UUID.randomUUID(),
            periodetype = "FORLENGELSE"
        )
        logg.nyMelding(hendelse)
        assertEquals(0, logg.behov().size)
        assertEquals(1, logg.hendelser().size)
        assertEquals(hendelse, logg.hendelser().single())
    }

    @Test
    fun `kan legge til og hente ut behov fra meldingsloggen`() {
        val logg = Meldingslogg()
        val behov = Behov.Vergemål
        logg.nyMelding(behov)
        assertEquals(0, logg.hendelser().size)
        assertEquals(1, logg.behov().size)
        assertEquals(behov, logg.behov().single())
    }
}
