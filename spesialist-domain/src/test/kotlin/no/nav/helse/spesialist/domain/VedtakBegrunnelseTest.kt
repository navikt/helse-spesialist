package no.nav.helse.spesialist.domain

import no.nav.helse.modell.vedtak.Utfall
import java.util.UUID
import kotlin.random.Random.Default.nextLong
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue


class VedtakBegrunnelseTest {

    @Test
    fun `erForskjelligFra gir riktig svar`() {
        val tekst = "Lorem ipsum dolor sit amet"
        val vedtakBegrunnelse = VedtakBegrunnelse.fraLagring(
            id = VedtakBegrunnelseId(nextLong()),
            spleisBehandlingId = SpleisBehandlingId(UUID.randomUUID()),
            tekst = tekst,
            utfall = Utfall.INNVILGELSE,
            invalidert = false,
        )
        assertFalse(vedtakBegrunnelse.erForskjelligFra(tekst, Utfall.INNVILGELSE))
        assertTrue(vedtakBegrunnelse.erForskjelligFra("En ulik tekst.", Utfall.INNVILGELSE))
        assertTrue(vedtakBegrunnelse.erForskjelligFra(tekst, Utfall.AVSLAG))
        assertTrue(vedtakBegrunnelse.erForskjelligFra("En ulik tekst.", Utfall.AVSLAG))
    }

    @Test
    fun `kan invalidere vedtakbegrunnelse`() {
        val vedtakBegrunnelse = VedtakBegrunnelse.fraLagring(
            id = VedtakBegrunnelseId(nextLong()),
            spleisBehandlingId = SpleisBehandlingId(UUID.randomUUID()),
            tekst = "Lorem ipsum dolor sit amet",
            utfall = Utfall.INNVILGELSE,
            invalidert = false,
        )
        assertFalse(vedtakBegrunnelse.invalidert)
        vedtakBegrunnelse.invalider()
        assertTrue(vedtakBegrunnelse.invalidert)
    }
}
