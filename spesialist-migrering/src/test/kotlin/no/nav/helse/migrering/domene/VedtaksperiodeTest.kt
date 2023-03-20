package no.nav.helse.migrering.domene

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class VedtaksperiodeTest{

    @Test
    fun `Kan opprette vedtaksperiode`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val vedtaksperiode = Vedtaksperiode(vedtaksperiodeId, LocalDateTime.now(), LocalDate.now(), LocalDate.now(), LocalDate.now(), "123", "1234")
        vedtaksperiode.register(observer)
        vedtaksperiode.opprett()

        assertEquals(listOf(vedtaksperiodeId), observer.opprettedeVedtaksperioder)
    }

    private val observer = object : IPersonObserver{
        val opprettedeVedtaksperioder = mutableListOf<UUID>()
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
    }
}
