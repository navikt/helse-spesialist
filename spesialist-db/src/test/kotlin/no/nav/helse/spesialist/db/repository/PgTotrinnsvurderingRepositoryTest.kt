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
    private val person = opprettPerson()

    @BeforeEach
    fun setup() {
        opprettArbeidsgiver()
        opprettVedtaksperiode(fødselsnummer = person.id.value)
        opprettSaksbehandler()
    }

    @Test
    fun `hvis det ikke finnes totrinnsvurdering, returnerer null`() {
        val totrinnsvurdering = totrinnsvurderingRepository.finnAktivForPerson(person.id.value)

        assertNull(totrinnsvurdering)
    }

    @Test
    fun `lagre totrinnsvurdering`() {
        val totrinnsvurdering = nyTotrinnsvurdering()

        totrinnsvurderingRepository.lagre(totrinnsvurdering)
        assertTrue(totrinnsvurdering.harFåttTildeltId())

        val hentetTotrinnsvurdering = totrinnsvurderingRepository.finnAktivForPerson(person.id.value)

        assertEquals(totrinnsvurdering, hentetTotrinnsvurdering)
    }

    @Test
    fun `oppdater totrinnsvurdering`() {
        totrinnsvurderingRepository.lagre(nyTotrinnsvurdering())
        val hentetTotrinnsvurdering = totrinnsvurderingRepository.finnAktivForPerson(person.id.value)
        checkNotNull(hentetTotrinnsvurdering)
        hentetTotrinnsvurdering.nyOverstyring(nyOverstyring())
        hentetTotrinnsvurdering.settBeslutter(SaksbehandlerOid(SAKSBEHANDLER_OID))
        hentetTotrinnsvurdering.settAvventerSaksbehandler()
        totrinnsvurderingRepository.lagre(hentetTotrinnsvurdering)
        val oppdatertTotrinnsvurdering = totrinnsvurderingRepository.finnAktivForPerson(person.id.value)
        checkNotNull(oppdatertTotrinnsvurdering)

        assertEquals(1, oppdatertTotrinnsvurdering.overstyringer.size)
        assertNotNull(oppdatertTotrinnsvurdering.beslutter)
        assertEquals(AVVENTER_SAKSBEHANDLER, oppdatertTotrinnsvurdering.tilstand)
        assertNotNull(oppdatertTotrinnsvurdering.oppdatert)
    }

    @Test
    fun `finner ikke frem totrinnsvurdering hvor vdtaksperiode er forkastet`() {
        val totrinnsvurdering = nyTotrinnsvurdering()
        totrinnsvurdering.nyOverstyring(nyOverstyring())
        totrinnsvurderingRepository.lagre(totrinnsvurdering)
        val hentetTotrinnsvurdering = totrinnsvurderingRepository.finnAktivForPerson(person.id.value)
        checkNotNull(hentetTotrinnsvurdering)
        hentetTotrinnsvurdering.vedtaksperiodeForkastet(listOf(VEDTAKSPERIODE))
        totrinnsvurderingRepository.lagre(hentetTotrinnsvurdering)
        val oppdatertTotrinnsvurdering = totrinnsvurderingRepository.finnAktivForPerson(person.id.value)

        assertNull(oppdatertTotrinnsvurdering)
    }

    private fun nyTotrinnsvurdering(): Totrinnsvurdering = Totrinnsvurdering.ny(fødselsnummer = person.id.value)

    private fun nyOverstyring(): Overstyring =
        OverstyrtTidslinje.ny(
            vedtaksperiodeId = VEDTAKSPERIODE,
            saksbehandlerOid = SaksbehandlerOid(SAKSBEHANDLER_OID),
            fødselsnummer = person.id.value,
            aktørId = AKTØR,
            organisasjonsnummer = ORGNUMMER,
            begrunnelse = "begrunnelse",
            dager = listOf(nyOverstyrtTidslinjedag()),
        )
}
