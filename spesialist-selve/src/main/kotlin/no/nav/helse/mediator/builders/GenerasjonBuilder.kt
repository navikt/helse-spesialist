package no.nav.helse.mediator.builders

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.modell.varsel.ActualVarselRepository
import no.nav.helse.modell.varsel.Varsel
import no.nav.helse.modell.vedtaksperiode.ActualGenerasjonRepository
import no.nav.helse.modell.vedtaksperiode.Generasjon
import no.nav.helse.modell.vedtaksperiode.IVedtaksperiodeObserver

class GenerasjonBuilder(
    private val vedtaksperiodeId: UUID
) {
    private lateinit var generasjonId: UUID
    private lateinit var fom: LocalDate
    private lateinit var tom: LocalDate
    private lateinit var skjæringstidspunkt: LocalDate
    private lateinit var tilstand: Generasjon.Tilstand
    private var utbetalingId: UUID? = null
    private val varsler = mutableListOf<Varsel>()

    internal fun buildFirst(
        generasjonId: UUID = UUID.randomUUID(),
        fom: LocalDate,
        tom: LocalDate,
        skjæringstidspunkt: LocalDate,
        vararg observers: IVedtaksperiodeObserver,
    ): Generasjon {
        return Generasjon.nyVedtaksperiode(generasjonId, vedtaksperiodeId, fom, tom, skjæringstidspunkt).also {
            it.registrer(*observers)
        }
    }

    internal fun build(
        generasjonRepository: ActualGenerasjonRepository,
        varselRepository: ActualVarselRepository,
    ): Generasjon {
        generasjonRepository.byggGenerasjon(vedtaksperiodeId, this)
        varselRepository.byggGenerasjon(generasjonId, this)
        return Generasjon.fraLagring(
            id = generasjonId,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId,
            skjæringstidspunkt = skjæringstidspunkt,
            fom = fom,
            tom = tom,
            tilstand = tilstand,
            varsler = varsler.toSet()
        ).also {
            it.registrer(generasjonRepository, varselRepository)
        }
    }

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

    internal fun tilstand(tilstand: Generasjon.Tilstand) {
        this.tilstand = tilstand
    }

    internal fun varsler(varsler: List<Varsel>) {
        this.varsler.addAll(varsler)
    }
}