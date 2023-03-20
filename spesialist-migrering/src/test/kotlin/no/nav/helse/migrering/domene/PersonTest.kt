package no.nav.helse.migrering.domene

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class PersonTest{

    @Test
    fun `Kan opprette person`() {
        val person = Person("123", "1234")
        person.register(observer)
        person.opprett()

        assertEquals(listOf("123"), observer.opprettedePersoner)
    }

    private val observer = object : IPersonObserver{
        val opprettedePersoner = mutableListOf<String>()
        override fun personOpprettet(aktørId: String, fødselsnummer: String) {
            opprettedePersoner.add(aktørId)
        }
    }
}