package no.nav.helse.migrering.domene

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class VedtaksperiodeTest {

    @Test
    fun `Kan opprette vedtaksperiode`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val vedtaksperiode = Vedtaksperiode(
            id = vedtaksperiodeId,
            opprettet = LocalDateTime.now(),
            fom = LocalDate.now(),
            tom = LocalDate.now(),
            skjæringstidspunkt = LocalDate.now(),
            fødselsnummer = "123",
            organisasjonsnummer = "1234",
            forkastet = false
        )
        vedtaksperiode.register(observer)
        vedtaksperiode.opprett()

        assertEquals(listOf(vedtaksperiodeId), observer.opprettedeVedtaksperioder)
    }

    @Test
    fun `Oppretter ikke vedtaksperiode hvis den er forkastet`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val vedtaksperiode = Vedtaksperiode(
            id = vedtaksperiodeId,
            opprettet = LocalDateTime.now(),
            fom = LocalDate.now(),
            tom = LocalDate.now(),
            skjæringstidspunkt = LocalDate.now(),
            fødselsnummer = "123",
            organisasjonsnummer = "1234",
            forkastet = true
        )
        vedtaksperiode.register(observer)
        vedtaksperiode.opprett()

        assertEquals(emptyList<UUID>(), observer.opprettedeVedtaksperioder)
    }

    @Test
    fun `Oppdaterer forkastet-flagg for vedtaksperiode`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val vedtaksperiode = Vedtaksperiode(
            id = vedtaksperiodeId,
            opprettet = LocalDateTime.now(),
            fom = LocalDate.now(),
            tom = LocalDate.now(),
            skjæringstidspunkt = LocalDate.now(),
            fødselsnummer = "123",
            organisasjonsnummer = "1234",
            forkastet = true
        )
        vedtaksperiode.register(observer)
        vedtaksperiode.oppdaterForkastet()

        assertEquals(listOf(vedtaksperiodeId), observer.oppdaterteVedtaksperioder)
    }

    private val observer = object : IPersonObserver {
        val opprettedeVedtaksperioder = mutableListOf<UUID>()
        val oppdaterteVedtaksperioder = mutableListOf<UUID>()
        override fun vedtaksperiodeOpprettet(
            id: UUID,
            opprettet: LocalDateTime,
            fom: LocalDate,
            tom: LocalDate,
            skjæringstidspunkt: LocalDate,
            fødselsnummer: String,
            organisasjonsnummer: String
        ) {
            opprettedeVedtaksperioder.add(id)
        }

        override fun vedtaksperiodeOppdaterForkastet(id: UUID, forkastet: Boolean) {
            oppdaterteVedtaksperioder.add(id)
        }
    }
}
