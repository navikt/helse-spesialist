package no.nav.helse.migrering.domene

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal class Person(
    private val aktørId: String,
    private val fødselsnummer: String
) {

    private val observers = mutableSetOf<IPersonObserver>()
    private val arbeidsgivere = mutableListOf<Arbeidsgiver>()
    internal fun register(observer: IPersonObserver) {
        observers.add(observer)
    }

    internal fun opprett() {
        observers.forEach { it.personOpprettet( aktørId, fødselsnummer) }
    }

    internal fun håndterNyArbeidsgiver(organisasjonsnummer: String): Arbeidsgiver {
        val arbeidsgiver = Arbeidsgiver(organisasjonsnummer)
        arbeidsgivere.add(arbeidsgiver)
        arbeidsgiver.register(observer = observers.toTypedArray())
        arbeidsgiver.opprett()
        return arbeidsgiver
    }
}

internal interface IPersonObserver{

    fun personOpprettet(aktørId: String, fødselsnummer: String) {}

    fun arbeidsgiverOpprettet(organisasjonsnummer: String) {}
    fun vedtaksperiodeOpprettet(
        id: UUID,
        opprettet: LocalDateTime,
        fom: LocalDate,
        tom: LocalDate,
        skjæringstidspunkt: LocalDate,
        fødselsnummer: String,
        organisasjonsnummer: String,
        forkastet: Boolean,
    ) {}
}