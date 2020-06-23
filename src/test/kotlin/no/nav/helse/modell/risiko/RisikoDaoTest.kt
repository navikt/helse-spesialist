package no.nav.helse.modell.risiko

import kotliquery.sessionOf
import no.nav.helse.TestPerson
import no.nav.helse.setupDataSourceMedFlyway
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDateTime
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RisikoDaoTest {
    private val dataSource = setupDataSourceMedFlyway()
    private val session = sessionOf(dataSource, returnGeneratedKey = true)

    @AfterAll
    fun cleanup() {
        session.close()
    }

    @Test
    fun `Henter ut persisterte risikovurderinger`() {
        val testperson = TestPerson(dataSource)

        val vedtaksperiodeId1 = UUID.randomUUID()
        val vedtaksperiodeId2 = UUID.randomUUID()

        val eventId1 = UUID.randomUUID()
        testperson.sendGodkjenningMessage(eventId = eventId1, vedtaksperiodeId = vedtaksperiodeId1)
        testperson.sendPersoninfo(eventId = eventId1)
        testperson.sendGodkjenningMessage(eventId = UUID.randomUUID(), vedtaksperiodeId = vedtaksperiodeId2)

        val expectedRisikovurdering1 = RisikovurderingDto(
            vedtaksperiodeId = vedtaksperiodeId1,
            opprettet = LocalDateTime.of(2020, 1, 1, 1, 1),
            samletScore = 10,
            faresignaler = listOf("har begrunnelse", "har enda en begrunnelse", "det var alle begrunnelsene"),
            arbeidsuførhetvurdering = listOf("arbeidsuførhet"),
            ufullstendig = false
        )

        val expectedRisikovurdering2 = RisikovurderingDto(
            vedtaksperiodeId = vedtaksperiodeId2,
            opprettet = LocalDateTime.of(2020, 2, 2, 2, 2),
            samletScore = 1,
            faresignaler = listOf("har begrunnelse", "har enda en begrunnelse"),
            arbeidsuførhetvurdering = listOf("arbeidsuførhet"),
            ufullstendig = false
        )

        session.persisterRisikovurdering(
            RisikovurderingDto(
                vedtaksperiodeId = vedtaksperiodeId1,
                opprettet = LocalDateTime.of(2020, 1, 3, 3, 7),
                samletScore = 10,
                faresignaler = listOf("har begrunnelse", "har enda en begrunnelse"),
                arbeidsuførhetvurdering = emptyList(),
                ufullstendig = true
            )
        )
        session.persisterRisikovurdering(
            RisikovurderingDto(
                vedtaksperiodeId = expectedRisikovurdering1.vedtaksperiodeId,
                opprettet = expectedRisikovurdering1.opprettet,
                samletScore = expectedRisikovurdering1.samletScore,
                faresignaler = expectedRisikovurdering1.faresignaler,
                arbeidsuførhetvurdering = expectedRisikovurdering1.arbeidsuførhetvurdering,
                ufullstendig = expectedRisikovurdering1.ufullstendig
            )
        )
        session.persisterRisikovurdering(
            RisikovurderingDto(
                vedtaksperiodeId = expectedRisikovurdering2.vedtaksperiodeId,
                opprettet = expectedRisikovurdering2.opprettet,
                samletScore = expectedRisikovurdering2.samletScore,
                faresignaler = expectedRisikovurdering2.faresignaler,
                arbeidsuførhetvurdering = expectedRisikovurdering2.arbeidsuførhetvurdering,
                ufullstendig = expectedRisikovurdering2.ufullstendig
            )
        )

        val risikovurderinger1 =
            requireNotNull(session.hentRisikovurderingForVedtaksperiode(expectedRisikovurdering1.vedtaksperiodeId))
        val risikovurderinger2 =
            requireNotNull(session.hentRisikovurderingForVedtaksperiode(expectedRisikovurdering2.vedtaksperiodeId))

        assertEquals(expectedRisikovurdering1.faresignaler.sorted(), risikovurderinger1.faresignaler.sorted())
        assertEquals(
            expectedRisikovurdering1.arbeidsuførhetvurdering.sorted(),
            risikovurderinger1.arbeidsuførhetvurdering.sorted()
        )
        assertEquals(expectedRisikovurdering2.faresignaler.sorted(), risikovurderinger2.faresignaler.sorted())
        assertEquals(
            expectedRisikovurdering2.arbeidsuførhetvurdering.sorted(),
            risikovurderinger2.arbeidsuførhetvurdering.sorted()
        )
    }

    @Test
    fun `Kan hente tom risikovurdering`() {
        val testperson = TestPerson(dataSource)

        val vedtaksperiodeId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        testperson.sendGodkjenningMessage(eventId = eventId, vedtaksperiodeId = vedtaksperiodeId)
        testperson.sendPersoninfo(eventId = eventId)

        val risikovurdering = session.hentRisikovurderingForVedtaksperiode(vedtaksperiodeId)
        assertNull(risikovurdering)
    }
}
