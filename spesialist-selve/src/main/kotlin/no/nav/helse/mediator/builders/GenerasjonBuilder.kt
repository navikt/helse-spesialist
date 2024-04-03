package no.nav.helse.mediator.builders

import no.nav.helse.modell.varsel.Varsel
import no.nav.helse.modell.varsel.VarselRepository
import no.nav.helse.modell.vedtaksperiode.Generasjon
import no.nav.helse.modell.vedtaksperiode.GenerasjonRepository
import java.time.LocalDate
import java.util.UUID

class GenerasjonBuilder(
    private val vedtaksperiodeId: UUID,
) {
    private lateinit var generasjonId: UUID
    private lateinit var fom: LocalDate
    private lateinit var tom: LocalDate
    private lateinit var skjæringstidspunkt: LocalDate
    private lateinit var tilstand: Generasjon.Tilstand
    private var utbetalingId: UUID? = null
    private var spleisBehandlingId: UUID? = null
    private val tags = mutableListOf<String>()
    private val varsler = mutableListOf<Varsel>()

    internal fun build(
        generasjonRepository: GenerasjonRepository,
        varselRepository: VarselRepository,
    ): Generasjon {
        generasjonRepository.byggGenerasjon(vedtaksperiodeId, this)
        varselRepository.byggGenerasjon(generasjonId, this)
        return Generasjon.fraLagring(
            id = generasjonId,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId,
            spleisBehandlingId = spleisBehandlingId,
            skjæringstidspunkt = skjæringstidspunkt,
            fom = fom,
            tom = tom,
            tilstand = tilstand,
            tags = tags,
            varsler = varsler.toSet(),
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

    internal fun spleisBehandlingId(spleisBehandlingId: UUID) {
        this.spleisBehandlingId = spleisBehandlingId
    }

    internal fun periode(
        fom: LocalDate,
        tom: LocalDate,
    ) {
        this.fom = fom
        this.tom = tom
    }

    internal fun skjæringstidspunkt(skjæringstidspunkt: LocalDate) {
        this.skjæringstidspunkt = skjæringstidspunkt
    }

    internal fun tags(tags: List<String>) {
        this.tags.apply {
            clear()
            addAll(tags)
        }
    }

    internal fun tilstand(tilstand: Generasjon.Tilstand) {
        this.tilstand = tilstand
    }

    internal fun varsler(varsler: List<Varsel>) {
        this.varsler.addAll(varsler)
    }
}
