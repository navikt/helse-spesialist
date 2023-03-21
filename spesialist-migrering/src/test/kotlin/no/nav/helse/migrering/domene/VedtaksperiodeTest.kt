package no.nav.helse.migrering.domene

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class VedtaksperiodeTest{

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

    private val observer = object : IPersonObserver {
        val opprettedeVedtaksperioder = mutableListOf<UUID>()
        override fun vedtaksperiodeOpprettet(
            id: UUID,
            opprettet: LocalDateTime,
            fom: LocalDate,
            tom: LocalDate,
            skjæringstidspunkt: LocalDate,
            fødselsnummer: String,
            organisasjonsnummer: String,
            forkastet: Boolean
        ) {
            opprettedeVedtaksperioder.add(id)
        }
    }
}
