package no.nav.helse.spesialist.db.repository

import no.nav.helse.modell.saksbehandler.handlinger.Overstyring
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtTidslinje
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingTilstand.AVVENTER_SAKSBEHANDLER
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


class PgTotrinnsvurderingRepositoryTest : AbstractDBIntegrationTest() {

    @BeforeEach
    fun setup() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        opprettSaksbehandler()
    }

    @Test
    fun `hvis det ikke finnes totrinnsvurdering, returnerer null`() {
        val totrinnsvurdering = totrinnsvurderingRepository.finn(FNR)

        assertNull(totrinnsvurdering)
    }

    @Test
    fun `lagre totrinnsvurdering`() {
        val totrinnsvurdering = nyTotrinnsvurdering()

        totrinnsvurderingRepository.lagre(totrinnsvurdering)
        assertTrue(totrinnsvurdering.harFåttTildeltId())

        val hentetTotrinnsvurdering = totrinnsvurderingRepository.finn(FNR)

        assertEquals(totrinnsvurdering, hentetTotrinnsvurdering)
    }

    @Test
    fun `oppdater totrinnsvurdering`() {
        totrinnsvurderingRepository.lagre(nyTotrinnsvurdering())
        val hentetTotrinnsvurdering = totrinnsvurderingRepository.finn(FNR)
        checkNotNull(hentetTotrinnsvurdering)
        hentetTotrinnsvurdering.nyOverstyring(nyOverstyring())
        hentetTotrinnsvurdering.settBeslutter(SaksbehandlerOid(SAKSBEHANDLER_OID))
        hentetTotrinnsvurdering.settAvventerSaksbehandler()
        totrinnsvurderingRepository.lagre(hentetTotrinnsvurdering)
        val oppdatertTotrinnsvurdering = totrinnsvurderingRepository.finn(FNR)
        checkNotNull(oppdatertTotrinnsvurdering)

        assertEquals(1, oppdatertTotrinnsvurdering.overstyringer.size)
        assertNotNull(oppdatertTotrinnsvurdering.beslutter)
        assertEquals(AVVENTER_SAKSBEHANDLER, oppdatertTotrinnsvurdering.tilstand)
        assertNotNull(oppdatertTotrinnsvurdering.oppdatert)
    }

    @Test
    fun `finner ikke frem totrinnsvurdering hvor vdtaksperiode er forkastet`() {
        totrinnsvurderingRepository.lagre(nyTotrinnsvurdering())
        val hentetTotrinnsvurdering = totrinnsvurderingRepository.finn(FNR)
        checkNotNull(hentetTotrinnsvurdering)
        hentetTotrinnsvurdering.vedtaksperiodeForkastet()
        totrinnsvurderingRepository.lagre(hentetTotrinnsvurdering)
        val oppdatertTotrinnsvurdering = totrinnsvurderingRepository.finn(FNR)

        assertNull(oppdatertTotrinnsvurdering)
    }

    private fun nyTotrinnsvurdering(): Totrinnsvurdering = Totrinnsvurdering.ny(fødselsnummer = FNR)

    private fun nyOverstyring(): Overstyring =
        OverstyrtTidslinje.ny(
            vedtaksperiodeId = VEDTAKSPERIODE,
            saksbehandlerOid = SaksbehandlerOid(SAKSBEHANDLER_OID),
            fødselsnummer = FNR,
            aktørId = AKTØR,
            organisasjonsnummer = ORGNUMMER,
            begrunnelse = "begrunnelse",
            dager = listOf(nyOverstyrtTidslinjedag())
        )

}
