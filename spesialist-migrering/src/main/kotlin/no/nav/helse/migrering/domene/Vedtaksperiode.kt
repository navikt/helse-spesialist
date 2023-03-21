package no.nav.helse.migrering.domene

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.kv
import org.slf4j.LoggerFactory

internal class Vedtaksperiode(
    private val id: UUID,
    private val opprettet: LocalDateTime,
    private val fom: LocalDate,
    private val tom: LocalDate,
    private val skjæringstidspunkt: LocalDate,
    private val fødselsnummer: String,
    private val organisasjonsnummer: String,
    private val forkastet: Boolean
) {

    private val observers = mutableListOf<IPersonObserver>()

    internal fun erForkastet() = forkastet

    internal fun register(vararg observer: IPersonObserver) {
        observers.addAll(observer)
    }

    internal fun oppdaterForkastet() {
        observers.forEach { it.vedtaksperiodeOppdaterForkastet(id, forkastet) }
    }

    internal fun opprett() {
        if (forkastet) return sikkerlogg.info(
            "Oppretter ikke vedtaksperiode for {}, {} med {} da den er forkastet",
            kv("fødselsnummer", fødselsnummer),
            kv("organisasjonsnummer", organisasjonsnummer),
            kv("vedtaksperiodeId", id),
        )
        observers.forEach {
            it.vedtaksperiodeOpprettet(id, opprettet, fom, tom, skjæringstidspunkt, fødselsnummer, organisasjonsnummer)
        }
    }

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
}