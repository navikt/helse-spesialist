package no.nav.helse.migrering.domene

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal class Vedtaksperiode(
    private val id: UUID,
    private val opprettet: LocalDateTime,
    private val fom: LocalDate,
    private val tom: LocalDate,
    private val skjæringstidspunkt: LocalDate,
    private val fødselsnummer: String,
    private val organisasjonsnummer: String
) {

    private val observers = mutableListOf<IPersonObserver>()

    internal fun register(vararg observer: IPersonObserver) {
        observers.addAll(observer)
    }

    internal fun opprett() {
        observers.forEach { it.vedtaksperiodeOpprettet(id, opprettet, fom, tom, skjæringstidspunkt, fødselsnummer, organisasjonsnummer) }
    }
}