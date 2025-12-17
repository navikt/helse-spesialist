package no.nav.helse.spesialist.db.repository

import no.nav.helse.modell.vedtak.Utfall
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.VedtakBegrunnelse
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PgVedtakBegrunnelseRepositoryTest : AbstractDBIntegrationTest() {
    private val repository = sessionContext.vedtakBegrunnelseRepository

    @Test
    fun `lagre og finn vedtaksbegrunnelse`() {
        // given
        val vedtaksperiodeId = UUID.randomUUID()
        val fødselsnummer = opprettPerson().id.value
        val spleisBehandlingId = SpleisBehandlingId(UUID.randomUUID())
        opprettArbeidsgiver()
        opprettBehandling(
            vedtaksperiodeId = vedtaksperiodeId,
            fødselsnummer = fødselsnummer,
            spleisBehandlingId = spleisBehandlingId.value,
        )

        // when
        val tekst = "Lorem ipsum dolor sit amet"
        val saksbehandlerOid = SaksbehandlerOid(UUID.randomUUID())
        repository.lagre(
            VedtakBegrunnelse.ny(
                spleisBehandlingId = spleisBehandlingId,
                tekst = tekst,
                utfall = Utfall.INNVILGELSE,
                saksbehandlerOid = saksbehandlerOid,
            ),
        )
        val vedtakBegrunnelse = repository.finn(spleisBehandlingId)

        // then
        assertNotNull(vedtakBegrunnelse)
        assertTrue(vedtakBegrunnelse.harFåttTildeltId())
        assertEquals(spleisBehandlingId, vedtakBegrunnelse.spleisBehandlingId)
        assertEquals(tekst, vedtakBegrunnelse.tekst)
        assertEquals(Utfall.INNVILGELSE, vedtakBegrunnelse.utfall)
        assertFalse(vedtakBegrunnelse.invalidert)
        assertEquals(saksbehandlerOid, vedtakBegrunnelse.saksbehandlerOid)
    }

    @Test
    fun `henter ikke vedtakbegrunnelse som er invalidert`() {
        // given
        val vedtaksperiodeId = UUID.randomUUID()
        val fødselsnummer = opprettPerson().id.value
        val spleisBehandlingId = SpleisBehandlingId(UUID.randomUUID())
        opprettArbeidsgiver()
        opprettBehandling(
            vedtaksperiodeId = vedtaksperiodeId,
            fødselsnummer = fødselsnummer,
            spleisBehandlingId = spleisBehandlingId.value,
        )

        // when
        repository.lagre(
            VedtakBegrunnelse.ny(
                spleisBehandlingId = spleisBehandlingId,
                tekst = "Lorem ipsum dolor sit amet",
                utfall = Utfall.INNVILGELSE,
                saksbehandlerOid = SaksbehandlerOid(UUID.randomUUID()),
            ),
        )
        val vedtakBegrunnelse = repository.finn(spleisBehandlingId)
        checkNotNull(vedtakBegrunnelse)
        vedtakBegrunnelse.invalider()
        repository.lagre(vedtakBegrunnelse)
        val oppdatertVedtakBegrunnelse = repository.finn(spleisBehandlingId)

        // then
        assertNull(oppdatertVedtakBegrunnelse)
    }
}
