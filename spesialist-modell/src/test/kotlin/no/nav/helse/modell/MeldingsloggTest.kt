package no.nav.helse.modell

import no.nav.helse.modell.melding.VedtaksperiodeGodkjentAutomatisk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class MeldingsloggTest {

    @Test
    fun `kan legge til og hente ut meldinger fra meldingsloggen`() {
        val logg = Meldingslogg()
        val hendelse = VedtaksperiodeGodkjentAutomatisk(
            fødselsnummer = lagFødselsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
            behandlingId = UUID.randomUUID(),
            periodetype = "FORLENGELSE"
        )
        logg.nyHendelse(hendelse)
        assertEquals(1, logg.hendelser().size)
        assertEquals(hendelse, logg.hendelser().single())
    }
}
