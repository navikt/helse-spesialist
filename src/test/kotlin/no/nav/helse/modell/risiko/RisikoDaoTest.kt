package no.nav.helse.modell.risiko

import no.nav.helse.TestPerson
import no.nav.helse.setupDataSourceMedFlyway
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

internal class RisikoDaoTest {
    private val dataSource = setupDataSourceMedFlyway()
    private val risikoDao = RisikoDao(dataSource)
    private val testperson = TestPerson(dataSource)

    @Test
    fun `Henter ut persisterte risikovurderinger`() {
        val vedtaksperiodeId1 = UUID.randomUUID()
        val vedtaksperiodeId2 = UUID.randomUUID()

        testperson.tilSaksbehandlerGodkjenning(vedtaksperiodeId = vedtaksperiodeId1)
        testperson.tilSaksbehandlerGodkjenning(vedtaksperiodeId = vedtaksperiodeId2)

        val expectedRisikovurdering1 = RisikovurderingDto(
            vedtaksperiodeId = vedtaksperiodeId1,
            samletScore = 10,
            begrunnelser = listOf("har begrunnelse", "har enda en begrunnelse", "det var alle begrunnelsene"),
            ufullstendig = false
        )

        val expectedRisikovurdering2 = RisikovurderingDto(
            vedtaksperiodeId = vedtaksperiodeId2,
            samletScore = 1,
            begrunnelser = listOf("har begrunnelse", "har enda en begrunnelse"),
            ufullstendig = false
        )

        risikoDao.persisterRisikovurdering(
            vedtaksperiodeId = vedtaksperiodeId1,
            samletScore = 10,
            begrunnelser = listOf("har begrunnelse", "har enda en begrunnelse"),
            ufullstendig = true
        )
        risikoDao.persisterRisikovurdering(
            vedtaksperiodeId = expectedRisikovurdering1.vedtaksperiodeId,
            samletScore = expectedRisikovurdering1.samletScore,
            begrunnelser = expectedRisikovurdering1.begrunnelser,
            ufullstendig = expectedRisikovurdering1.ufullstendig
        )
        risikoDao.persisterRisikovurdering(
            vedtaksperiodeId = expectedRisikovurdering2.vedtaksperiodeId,
            samletScore = expectedRisikovurdering2.samletScore,
            begrunnelser = expectedRisikovurdering2.begrunnelser,
            ufullstendig = expectedRisikovurdering2.ufullstendig
        )

        // ArbeidsgiverRef er 1 siden vi inserter i tom database
        val risikovurderinger = risikoDao.hentRisikovurderingerForArbeidsgiver(arbeidsgiverRef = 1)

        assertEquals(2, risikovurderinger.size)
        assertEquals(expectedRisikovurdering1, risikovurderinger.find { it.vedtaksperiodeId == vedtaksperiodeId1 })
        assertEquals(expectedRisikovurdering2, risikovurderinger.find { it.vedtaksperiodeId == vedtaksperiodeId2 })
    }

    @Test
    fun `Kan hente tom risikovurdering`() {
        val vedtaksperiodeId1 = UUID.randomUUID()
        val vedtaksperiodeId2 = UUID.randomUUID()

        testperson.tilSaksbehandlerGodkjenning(vedtaksperiodeId = vedtaksperiodeId1)
        testperson.tilSaksbehandlerGodkjenning(vedtaksperiodeId = vedtaksperiodeId2)

        val expectedRisikovurdering1 = RisikovurderingDto(
            vedtaksperiodeId = vedtaksperiodeId1,
            samletScore = 10,
            begrunnelser = listOf("har begrunnelse", "har enda en begrunnelse", "det var alle begrunnelsene"),
            ufullstendig = false
        )

        val expectedRisikovurdering2 = RisikovurderingDto(
            vedtaksperiodeId = vedtaksperiodeId2,
            samletScore = 1,
            begrunnelser = listOf("har begrunnelse", "har enda en begrunnelse"),
            ufullstendig = false
        )

        // ArbeidsgiverRef er 1 siden vi inserter i tom database
        val risikovurderinger = risikoDao.hentRisikovurderingerForArbeidsgiver(arbeidsgiverRef = 1)

        assertEquals(0, risikovurderinger.size)
    }
}
