package no.nav.helse.spesialist.db.repository

import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.Vedtak
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import org.postgresql.util.PSQLException
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class PgVedtakRepositoryTest : AbstractDBIntegrationTest() {
    private val repository = sessionContext.vedtakRepository

    @Test
    fun `lagre og finn behandling`() {
        // given
        val vedtak = etVedtak()

        // when
        repository.lagre(vedtak)

        // then
        val funnet = repository.finn(vedtak.id)
        assertNotNull(funnet)
        assertEquals(vedtak.id, funnet.id)
        assertEquals(vedtak.saksbehandlerIdent, funnet.saksbehandlerIdent)
        assertEquals(vedtak.beslutterIdent, funnet.beslutterIdent)
        assertEquals(vedtak.automatiskFattet, funnet.automatiskFattet)
        assertEquals(vedtak.tidspunkt.truncatedTo(ChronoUnit.MILLIS), funnet.tidspunkt.truncatedTo(ChronoUnit.MILLIS))
    }

    @Test
    fun `forsøk på å lagre vedtak dobbelt medfører exception`() {
        // given
        val vedtak = etVedtak()
        repository.lagre(vedtak)

        // then
        assertFailsWith<PSQLException> {
            // when
            repository.lagre(vedtak)
        }
    }

    private fun etVedtak() =
        Vedtak.fraLagring(
            SpleisBehandlingId(UUID.randomUUID()),
            automatiskFattet = false,
            saksbehandlerIdent = lagSaksbehandler().ident,
            beslutterIdent = lagSaksbehandler().ident,
            tidspunkt = Instant.now(),
        )
}
