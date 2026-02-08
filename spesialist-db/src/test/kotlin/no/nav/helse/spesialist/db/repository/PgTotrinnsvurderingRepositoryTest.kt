package no.nav.helse.spesialist.db.repository

import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.ArbeidsgiverIdentifikator
import no.nav.helse.spesialist.domain.Totrinnsvurdering
import no.nav.helse.spesialist.domain.TotrinnsvurderingTilstand.AVVENTER_SAKSBEHANDLER
import no.nav.helse.spesialist.domain.overstyringer.Overstyring
import no.nav.helse.spesialist.domain.overstyringer.OverstyrtTidslinje
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PgTotrinnsvurderingRepositoryTest : AbstractDBIntegrationTest() {
    private val person = opprettPerson()
    private val arbeidsgiver = opprettArbeidsgiver()
    private val organisasjonsnummer =
        when (val id = arbeidsgiver.id) {
            is ArbeidsgiverIdentifikator.Fødselsnummer -> id.fødselsnummer
            is ArbeidsgiverIdentifikator.Organisasjonsnummer -> id.organisasjonsnummer
        }
    private val vedtaksperiode =
        opprettVedtaksperiode(person, arbeidsgiver).also {
            opprettBehandling(it)
        }
    private val saksbehandler = opprettSaksbehandler()
    private val beslutter = opprettSaksbehandler()

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
        hentetTotrinnsvurdering.settBeslutter(beslutter.id)
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
        hentetTotrinnsvurdering.vedtaksperiodeForkastet(listOf(vedtaksperiode.id.value))
        totrinnsvurderingRepository.lagre(hentetTotrinnsvurdering)
        val oppdatertTotrinnsvurdering = totrinnsvurderingRepository.finnAktivForPerson(person.id.value)

        assertNull(oppdatertTotrinnsvurdering)
    }

    private fun nyTotrinnsvurdering(): Totrinnsvurdering = Totrinnsvurdering.ny(fødselsnummer = person.id.value)

    private fun nyOverstyring(): Overstyring =
        OverstyrtTidslinje.ny(
            vedtaksperiodeId = vedtaksperiode.id.value,
            saksbehandlerOid = saksbehandler.id,
            fødselsnummer = person.id.value,
            aktørId = person.aktørId,
            organisasjonsnummer = organisasjonsnummer,
            begrunnelse = "begrunnelse",
            dager = listOf(nyOverstyrtTidslinjedag()),
        )
}
