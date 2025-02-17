package no.nav.helse.spesialist.db.repository

import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.saksbehandler.handlinger.Overstyring
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtTidslinje
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.spesialist.db.DatabaseIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


class PgTotrinnsvurderingRepositoryTest: DatabaseIntegrationTest() {

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
    fun `lagre totrinsvurdering`() {
        val totrinnsvurdering = nyTotrinnsvurdering()

        totrinnsvurderingRepository.lagre(totrinnsvurdering, FNR)
        assertTrue(totrinnsvurdering.harFåttTildeltId())

        val hentetTotrinnsvurdering = totrinnsvurderingRepository.finn(FNR)

        assertEquals(totrinnsvurdering, hentetTotrinnsvurdering)
    }

    @Test
    fun `oppdater totrinsvurdering`() {
        totrinnsvurderingRepository.lagre(nyTotrinnsvurdering(), FNR)
        val hentetTotrinnsvurdering = totrinnsvurderingRepository.finn(FNR)
        checkNotNull(hentetTotrinnsvurdering)
        hentetTotrinnsvurdering.nyOverstyring(nyOverstyring())
        hentetTotrinnsvurdering.settBeslutter(nySaksbehandler())
        hentetTotrinnsvurdering.settRetur()
        totrinnsvurderingRepository.lagre(hentetTotrinnsvurdering, FNR)
        val oppdatertTotrinnsvurdering = totrinnsvurderingRepository.finn(FNR)
        checkNotNull(oppdatertTotrinnsvurdering)

        assertEquals(1, oppdatertTotrinnsvurdering.overstyringer.size)
        assertNotNull(oppdatertTotrinnsvurdering.beslutter)
        assertTrue(oppdatertTotrinnsvurdering.erRetur)
        assertNotNull(oppdatertTotrinnsvurdering.oppdatert)
    }

    private fun nyTotrinnsvurdering(): Totrinnsvurdering =
        Totrinnsvurdering.ny(vedtaksperiodeId = VEDTAKSPERIODE)

    private fun nyOverstyring(): Overstyring =
        OverstyrtTidslinje.ny(
            vedtaksperiodeId = VEDTAKSPERIODE,
            saksbehandlerOid = SAKSBEHANDLER_OID,
            fødselsnummer = FNR,
            aktørId = AKTØR,
            organisasjonsnummer = ORGNUMMER,
            begrunnelse = "begrunnelse",
            dager = listOf(nyOverstyrtTidslinjedag())
        )

    private fun nySaksbehandler(): no.nav.helse.modell.saksbehandler.Saksbehandler =
        Saksbehandler(
            epostadresse = SAKSBEHANDLER_EPOST,
            oid = SAKSBEHANDLER_OID,
            navn = SAKSBEHANDLER_NAVN,
            ident = SAKSBEHANDLER_IDENT,
            tilgangskontroll = { _, _ -> false },
        )
}
