package no.nav.helse.db

import io.mockk.every
import io.mockk.mockk
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import kotlin.random.Random.Default.nextLong


class PgTotrinnsvurderingRepositoryTest {

    val overstyringDao = mockk<PgOverstyringDao>(relaxed = true)
    val saksbehandlerDao = mockk<PgSaksbehandlerDao>(relaxed = true)
    val totrinnsvurderingDao = mockk<PgTotrinnsvurderingDao>(relaxed = true)
    val repository = PgTotrinnsvurderingRepository(
        overstyringDao,
        saksbehandlerDao,
        totrinnsvurderingDao,
    )

    @Test
    fun `hvis det ikke finnes totrinnsvurdering, returnerer null`() {
        val fødselsnummer = "12345678910"

        every { totrinnsvurderingDao.hentAktivTotrinnsvurdering(fødselsnummer) } returns null

        val result: Totrinnsvurdering? = repository.finnTotrinnsvurdering(fødselsnummer, mockk(relaxed = true))

        assertNull(result)
    }

    @Test
    fun `finn en totrinnsvurdering`() {
        val fødselsnummer = "12345678910"

        every { totrinnsvurderingDao.hentAktivTotrinnsvurdering(fødselsnummer) } returns (nextLong() to mockk<TotrinnsvurderingFraDatabase>(
            relaxed = true
        ))
        every { overstyringDao.finnOverstyringer(fødselsnummer) } returns listOf(mockk(relaxed = true))

        val result = repository.finnTotrinnsvurdering(fødselsnummer, mockk(relaxed = true))

        assertNotNull(result)
        assertEquals(1, result?.overstyringer()?.size)
    }
}