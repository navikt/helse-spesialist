package no.nav.helse.migrering.domene

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ArbeidsgiverTest{

    @Test
    fun `Kan opprette arbeidsgiver`() {
        val arbeidsgiver = Arbeidsgiver("123")
        arbeidsgiver.register(observer)
        arbeidsgiver.opprett()

        assertEquals(listOf("123"), observer.opprettedeArbeidsgivere)
    }

    private val observer = object : IPersonObserver {
        val opprettedeArbeidsgivere = mutableListOf<String>()
        override fun arbeidsgiverOpprettet(organisasjonsnummer: String) {
            opprettedeArbeidsgivere.add(organisasjonsnummer)
        }
    }
}