package no.nav.helse.db

import io.mockk.every
import io.mockk.mockk
import no.nav.helse.modell.NyId
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.spesialist.test.lagFødselsnummer
import no.nav.helse.spesialist.test.lagSaksbehandlerident
import no.nav.helse.spesialist.test.lagSaksbehandlernavn
import no.nav.helse.spesialist.test.lagTilfeldigSaksbehandlerepost
import no.nav.helse.util.TilgangskontrollForTestHarIkkeTilgang
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID
import kotlin.random.Random.Default.nextLong


class PgTotrinnsvurderingRepositoryTest {

    private val FNR = lagFødselsnummer()
    private val VEDTAKSPERIODE = UUID.randomUUID()
    private val SAKSBEHANDLER = Saksbehandler(
        oid = UUID.randomUUID(),
        navn = lagSaksbehandlernavn(),
        ident = lagSaksbehandlerident(),
        epostadresse = lagTilfeldigSaksbehandlerepost(),
        tilgangskontroll = TilgangskontrollForTestHarIkkeTilgang
    )

    private val overstyringDao = mockk<PgOverstyringDao>(relaxed = true)
    private val saksbehandlerDao = mockk<PgSaksbehandlerDao>(relaxed = true)
    private val totrinnsvurderingDao = mockk<PgTotrinnsvurderingDao>(relaxed = true)
    private val repository = PgTotrinnsvurderingRepository(
        overstyringDao,
        saksbehandlerDao,
        totrinnsvurderingDao,
    )

    @Test
    fun `hvis det ikke finnes totrinnsvurdering, returnerer null`() {
        every { totrinnsvurderingDao.hentAktivTotrinnsvurdering(FNR) } returns null

        val result: Totrinnsvurdering? = repository.finnTotrinnsvurdering(FNR, mockk(relaxed = true))

        assertNull(result)
    }

    @Test
    fun `finn en totrinnsvurdering`() {
        every { totrinnsvurderingDao.hentAktivTotrinnsvurdering(FNR) } returns (nextLong() to mockk<TotrinnsvurderingFraDatabase>(
            relaxed = true
        ))
        every { overstyringDao.finnOverstyringer(FNR) } returns listOf(mockk(relaxed = true))

        val result = repository.finnTotrinnsvurdering(FNR, mockk(relaxed = true))

        assertNotNull(result)
        assertEquals(1, result?.overstyringer()?.size)
    }

    @Test
    fun `lagre totrinsvurdering`() {
        val totrinnsvurdering = Totrinnsvurdering(
            id = NyId,
            vedtaksperiodeId = VEDTAKSPERIODE,
            erRetur = false,
            saksbehandler = SAKSBEHANDLER,
            beslutter = null,
            utbetalingId = null,
            opprettet = LocalDateTime.now(),
            oppdatert = LocalDateTime.now(),
            overstyringer = emptyList(),
            ferdigstilt = false,
        )

        every { totrinnsvurderingDao.opprett(totrinnsvurdering, FNR) } returns (1L to TotrinnsvurderingFraDatabase(
            vedtaksperiodeId = totrinnsvurdering.vedtaksperiodeId,
            erRetur = totrinnsvurdering.erRetur,
            saksbehandler = totrinnsvurdering.saksbehandler?.oid,
            beslutter = null,
            utbetalingId = totrinnsvurdering.utbetalingId,
            opprettet = totrinnsvurdering.opprettet,
            oppdatert = totrinnsvurdering.oppdatert,
        ))

        every { saksbehandlerDao.finnSaksbehandler(SAKSBEHANDLER.oid, any()) } returns SAKSBEHANDLER

        val lagret = repository.lagre(totrinnsvurdering, FNR, mockk(relaxed = true))
        assertEquals(totrinnsvurdering, lagret)
    }
}