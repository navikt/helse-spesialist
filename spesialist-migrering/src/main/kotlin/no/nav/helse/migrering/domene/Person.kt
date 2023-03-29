package no.nav.helse.migrering.domene

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.migrering.domene.Arbeidsgiver.Companion.harKunForkastedeVedtaksperioder
import org.slf4j.LoggerFactory

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
        if (arbeidsgivere.isEmpty())
            return sikkerlogg.info(
                "Oppretter ikke person med {}, {} da den ikke har noen arbeidsgivere",
                kv("aktørId", aktørId),
                kv("fødselsnummer", fødselsnummer)
            )
        if (arbeidsgivere.harKunForkastedeVedtaksperioder()) {
            sikkerlogg.info(
                "Oppretter ikke person med {}, {} da den ikke har noen aktive vedtaksperioder",
                kv("aktørId", aktørId),
                kv("fødselsnummer", fødselsnummer)
            )
        } else {
            observers.forEach { it.personOpprettet(aktørId, fødselsnummer) }
        }
        arbeidsgivere.forEach { it.opprett() }
    }

    internal fun håndterNyArbeidsgiver(organisasjonsnummer: String): Arbeidsgiver {
        val arbeidsgiver = Arbeidsgiver(organisasjonsnummer)
        arbeidsgivere.add(arbeidsgiver)
        arbeidsgiver.register(observer = observers.toTypedArray())
        return arbeidsgiver
    }

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
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
    ) {}
    fun vedtaksperiodeOppdaterForkastet(id: UUID, forkastet: Boolean) {}
    fun generasjonerOppdatert(id: UUID, fom: LocalDate, tom: LocalDate, skjæringstidspunkt: LocalDate) {}
}