package no.nav.helse.mediator.builders

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.modell.varsel.Varsel
import no.nav.helse.modell.vedtaksperiode.Generasjon
import kotlin.properties.Delegates

class GenerasjonBuilder(
    private val vedtaksperiodeId: UUID
) {
    private lateinit var generasjonId: UUID
    private lateinit var fom: LocalDate
    private lateinit var tom: LocalDate
    private lateinit var skjæringstidspunkt: LocalDate
    private var låst by Delegates.notNull<Boolean>()
    private var utbetalingId: UUID? = null
    private val varsler = mutableListOf<Varsel>()

    internal fun build(): Generasjon {
        return Generasjon.fraLagring(
            id = generasjonId,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId,
            låst = låst,
            skjæringstidspunkt = skjæringstidspunkt,
            fom = fom,
            tom = tom,
            varsler = varsler.toSet()
        )
    }

    internal fun generasjonId(): UUID = generasjonId

    internal fun generasjonId(generasjonId: UUID) {
        this.generasjonId = generasjonId
    }

    internal fun utbetalingId(utbetalingId: UUID) {
        this.utbetalingId = utbetalingId
    }

    internal fun periode(fom: LocalDate, tom: LocalDate) {
        this.fom = fom
        this.tom = tom
    }

    internal fun skjæringstidspunkt(skjæringstidspunkt: LocalDate) {
        this.skjæringstidspunkt = skjæringstidspunkt
    }

    internal fun låst(låst: Boolean) {
        this.låst = låst
    }

    internal fun varsler(varsler: List<Varsel>) {
        this.varsler.addAll(varsler)
    }
}