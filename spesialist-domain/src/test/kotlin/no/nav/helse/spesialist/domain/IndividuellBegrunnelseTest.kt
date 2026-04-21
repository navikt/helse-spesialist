package no.nav.helse.spesialist.domain

import no.nav.helse.modell.vedtak.Utfall
import java.util.UUID
import kotlin.random.Random.Default.nextLong
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IndividuellBegrunnelseTest {
    @Test
    fun `erForskjelligFra gir riktig svar`() {
        val tekst = "Lorem ipsum dolor sit amet"
        val individuellBegrunnelse =
            IndividuellBegrunnelse.fraLagring(
                id = VedtakBegrunnelseId(nextLong()),
                spleisBehandlingId = SpleisBehandlingId(UUID.randomUUID()),
                tekst = tekst,
                utfall = Utfall.INNVILGELSE,
                invalidert = false,
                saksbehandlerOid = SaksbehandlerOid(UUID.randomUUID()),
            )
        assertFalse(individuellBegrunnelse.erForskjelligFra(tekst, Utfall.INNVILGELSE))
        assertTrue(individuellBegrunnelse.erForskjelligFra("En ulik tekst.", Utfall.INNVILGELSE))
        assertTrue(individuellBegrunnelse.erForskjelligFra(tekst, Utfall.AVSLAG))
        assertTrue(individuellBegrunnelse.erForskjelligFra("En ulik tekst.", Utfall.AVSLAG))
    }

    @Test
    fun `kan invalidere vedtakbegrunnelse`() {
        val individuellBegrunnelse =
            IndividuellBegrunnelse.fraLagring(
                id = VedtakBegrunnelseId(nextLong()),
                spleisBehandlingId = SpleisBehandlingId(UUID.randomUUID()),
                tekst = "Lorem ipsum dolor sit amet",
                utfall = Utfall.INNVILGELSE,
                invalidert = false,
                saksbehandlerOid = SaksbehandlerOid(UUID.randomUUID()),
            )
        assertFalse(individuellBegrunnelse.invalidert)
        individuellBegrunnelse.invalider()
        assertTrue(individuellBegrunnelse.invalidert)
    }
}
