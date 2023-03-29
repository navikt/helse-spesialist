package no.nav.helse.migrering.domene

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ArbeidsgiverTest {

    @Test
    fun `Kan opprette arbeidsgiver`() {
        val arbeidsgiver = Arbeidsgiver("123")
        arbeidsgiver.register(observer)
        arbeidsgiver.håndterNyVedtaksperiode(vedtaksperiode())
        arbeidsgiver.opprett(emptyList())

        assertEquals(listOf("123"), observer.opprettedeArbeidsgivere)
    }

    @Test
    fun `Oppretter arbeidsgiver selv om den kun har forkastede perioder dersom den har perioder i vedtakSomMangler`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val arbeidsgiver = Arbeidsgiver("123")
        arbeidsgiver.register(observer)
        arbeidsgiver.håndterNyVedtaksperiode(vedtaksperiode(vedtaksperiodeId, forkastet = true))
        arbeidsgiver.opprett(listOf(vedtaksperiodeId))

        assertEquals(listOf("123"), observer.opprettedeArbeidsgivere)
    }

    @Test
    fun `Oppretter ikke arbeidsgiver dersom arbeidsgiver kun har forkastede perioder`() {
        val arbeidsgiver = Arbeidsgiver("123")
        arbeidsgiver.register(observer)
        arbeidsgiver.håndterNyVedtaksperiode(vedtaksperiode(forkastet = true))
        arbeidsgiver.opprett(emptyList())

        assertEquals(emptyList<String>(), observer.opprettedeArbeidsgivere)
    }

    @Test
    fun `Oppretter arbeidsgiver dersom arbeidsgiver har både vanlige og forkastede perioder`() {
        val arbeidsgiver = Arbeidsgiver("123")
        arbeidsgiver.register(observer)
        arbeidsgiver.håndterNyVedtaksperiode(vedtaksperiode(forkastet = true))
        arbeidsgiver.håndterNyVedtaksperiode(vedtaksperiode(forkastet = false))
        arbeidsgiver.opprett(emptyList())

        assertEquals(listOf("123"), observer.opprettedeArbeidsgivere)
    }

    private fun vedtaksperiode(vedtaksperiodeId: UUID = UUID.randomUUID(), forkastet: Boolean = false) = Vedtaksperiode(
        vedtaksperiodeId,
        LocalDateTime.now(),
        LocalDate.now(),
        LocalDate.now(),
        LocalDate.now(),
        "1234",
        "123",
        forkastet
    )

    private val observer = object : IPersonObserver {
        val opprettedeArbeidsgivere = mutableListOf<String>()
        override fun arbeidsgiverOpprettet(organisasjonsnummer: String) {
            opprettedeArbeidsgivere.add(organisasjonsnummer)
        }
    }
}