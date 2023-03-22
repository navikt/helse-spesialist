package no.nav.helse.migrering.domene

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class PersonTest{

    @Test
    fun `Kan opprette person`() {
        val person = Person("123", "1234")
        person.register(observer)
        val arbeidsgiver = person.håndterNyArbeidsgiver("123456789")
        arbeidsgiver.håndterNyVedtaksperiode(vedtaksperiode())
        person.opprett()

        assertEquals(listOf("123"), observer.opprettedePersoner)
    }

    @Test
    fun `Oppretter ikke person hvis person ikke har arbeidsgivere`() {
        val person = Person("123", "1234")
        person.register(observer)
        person.opprett()

        assertEquals(emptyList<String>(), observer.opprettedePersoner)
    }

    @Test
    fun `Oppretter ikke person hvis person har arbeidsgivere som kun har forkastede vedtaksperioder`() {
        val person = Person("123", "1234")
        person.register(observer)
        val arbeidsgiver = person.håndterNyArbeidsgiver("123456789")
        arbeidsgiver.håndterNyVedtaksperiode(vedtaksperiode(forkastet = true))
        person.opprett()

        assertEquals(emptyList<String>(), observer.opprettedePersoner)
    }

    @Test
    fun `Oppretter person hvis person har arbeidsgivere som både har aktive og forkastede vedtaksperioder`() {
        val person = Person("123", "1234")
        person.register(observer)
        val arbeidsgiver1 = person.håndterNyArbeidsgiver("123456789")
        val arbeidsgiver2 = person.håndterNyArbeidsgiver("987654321")
        arbeidsgiver1.håndterNyVedtaksperiode(vedtaksperiode(forkastet = true))
        arbeidsgiver2.håndterNyVedtaksperiode(vedtaksperiode(forkastet = true))
        arbeidsgiver2.håndterNyVedtaksperiode(vedtaksperiode(forkastet = false))
        person.opprett()

        assertEquals(listOf("123"), observer.opprettedePersoner)
    }

    @Test
    fun `oppdaterer forkastet-flagg selv om person kun har arbeidsgivere med forkastede perioder`() {
        val person = Person("123", "1234")
        person.register(observer)
        val arbeidsgiver1 = person.håndterNyArbeidsgiver("123456789")
        val arbeidsgiver2 = person.håndterNyArbeidsgiver("987654321")
        arbeidsgiver1.håndterNyVedtaksperiode(vedtaksperiode(forkastet = true))
        arbeidsgiver2.håndterNyVedtaksperiode(vedtaksperiode(forkastet = true))
        person.opprett()

        assertEquals(0, observer.opprettedePersoner.size)
        assertEquals(0, observer.opprettedeArbeidsgivere.size)
        assertEquals(0, observer.opprettedeVedtaksperioder.size)
        assertEquals(2, observer.vedtaksperioderOppdatert.size)
    }

    private fun vedtaksperiode(forkastet: Boolean = false) = Vedtaksperiode(
        UUID.randomUUID(),
        LocalDateTime.now(),
        LocalDate.now(),
        LocalDate.now(),
        LocalDate.now(),
        "1234",
        "123",
        forkastet
    )

    private val observer = object : IPersonObserver {
        val opprettedePersoner = mutableListOf<String>()
        val opprettedeArbeidsgivere = mutableListOf<String>()
        val opprettedeVedtaksperioder = mutableListOf<UUID>()
        val vedtaksperioderOppdatert = mutableListOf<UUID>()
        override fun personOpprettet(aktørId: String, fødselsnummer: String) {
            opprettedePersoner.add(aktørId)
        }

        override fun vedtaksperiodeOppdaterForkastet(id: UUID, forkastet: Boolean) {
            vedtaksperioderOppdatert.add(id)
        }

        override fun arbeidsgiverOpprettet(organisasjonsnummer: String) {
            opprettedeArbeidsgivere.add(organisasjonsnummer)
        }

        override fun vedtaksperiodeOpprettet(id: UUID, opprettet: LocalDateTime, fom: LocalDate, tom: LocalDate, skjæringstidspunkt: LocalDate, fødselsnummer: String, organisasjonsnummer: String) {
            opprettedeVedtaksperioder.add(id)
        }
    }
}