package no.nav.helse.spesialist.db.repository

import no.nav.helse.spesialist.application.testing.assertEqualsByMicrosecond
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.Vedtak
import no.nav.helse.spesialist.domain.testfixtures.lagSpleisBehandlingId
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import org.postgresql.util.PSQLException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class PgVedtakRepositoryTest : AbstractDBIntegrationTest() {
    private val repository = sessionContext.vedtakRepository

    @Test
    fun `lagre automatisk vedtak og finn vedtak`() {
        // given
        val lagret = Vedtak.automatisk(lagSpleisBehandlingId())

        // when
        repository.lagre(lagret)

        // then
        val funnet = repository.finn(lagret.id)
        assertIs<Vedtak.Automatisk>(funnet)
        assertEquals(lagret.id, funnet.id)
        assertEqualsByMicrosecond(lagret.tidspunkt, funnet.tidspunkt)
    }

    @Test
    fun `lagre vedtak _med_ totrinnskontroll og finn vedtak`() {
        // given
        val lagret =
            Vedtak.manueltMedTotrinnskontroll(
                id = lagSpleisBehandlingId(),
                saksbehandlerIdent = lagSaksbehandler().ident,
                beslutterIdent = lagSaksbehandler().ident,
            )

        // when
        repository.lagre(lagret)

        // then
        val funnet = repository.finn(lagret.id)
        assertIs<Vedtak.ManueltMedTotrinnskontroll>(funnet)
        assertEquals(lagret.id, funnet.id)
        assertEquals(lagret.saksbehandlerIdent, funnet.saksbehandlerIdent)
        assertEquals(lagret.beslutterIdent, funnet.beslutterIdent)
        assertEqualsByMicrosecond(lagret.tidspunkt, funnet.tidspunkt)
    }

    @Test
    fun `lagre vedtak _uten_ totrinnskontroll og finn vedtak`() {
        // given
        val lagret =
            Vedtak.manueltUtenTotrinnskontroll(
                id = lagSpleisBehandlingId(),
                saksbehandlerIdent = lagSaksbehandler().ident,
            )

        // when
        repository.lagre(lagret)

        // then
        val funnet = repository.finn(lagret.id)
        assertIs<Vedtak.ManueltUtenTotrinnskontroll>(funnet)
        assertEquals(lagret.id, funnet.id)
        assertEquals(lagret.saksbehandlerIdent, funnet.saksbehandlerIdent)
        assertEqualsByMicrosecond(lagret.tidspunkt, funnet.tidspunkt)
    }

    @Test
    fun `forsøk på å lagre vedtak dobbelt medfører exception`() {
        // given
        val vedtak = Vedtak.automatisk(lagSpleisBehandlingId())
        repository.lagre(vedtak)

        // then
        assertFailsWith<PSQLException> {
            // when
            repository.lagre(vedtak)
        }
    }
}
