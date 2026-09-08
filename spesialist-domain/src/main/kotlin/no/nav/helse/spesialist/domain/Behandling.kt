package no.nav.helse.spesialist.domain

import no.nav.helse.modell.vedtak.Utfall
import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.spesialist.domain.ddd.AggregateRoot
import no.nav.helse.spesialist.domain.ddd.ValueObject
import java.time.LocalDate
import java.util.UUID

@JvmInline
value class SpleisBehandlingId(
    val value: UUID,
) : ValueObject

@JvmInline
value class BehandlingUnikId(
    val value: UUID,
) : ValueObject

enum class Tag {
    Innvilget,
    DelvisInnvilget,
    Avslag,
}

class Behandling private constructor(
    id: BehandlingUnikId,
    val spleisBehandlingId: SpleisBehandlingId?,
    val vedtaksperiodeId: VedtaksperiodeId,
    utbetalingId: UtbetalingId?,
    tags: Set<String>,
    tilstand: Tilstand,
    fom: LocalDate,
    tom: LocalDate,
    skjæringstidspunkt: LocalDate,
    yrkesaktivitetstype: Yrkesaktivitetstype,
) : AggregateRoot<BehandlingUnikId>(id) {
    enum class Tilstand {
        VedtakFattet,
        VidereBehandlingAvklares,
        AvsluttetUtenVedtak,
        AvsluttetUtenVedtakMedVarsler,
        KlarTilBehandling,
    }

    var utbetalingId: UtbetalingId? = utbetalingId
        private set
    var tags: Set<String> = tags
        private set
    var tilstand: Tilstand = tilstand
        private set
    var fom: LocalDate = fom
        private set
    var tom: LocalDate = tom
        private set
    var skjæringstidspunkt: LocalDate = skjæringstidspunkt
        private set
    var yrkesaktivitetstype: Yrkesaktivitetstype = yrkesaktivitetstype
        private set

    fun håndterGodkjentAvSaksbehandler() {
        if (tilstand == Tilstand.AvsluttetUtenVedtakMedVarsler) {
            tilstand = Tilstand.AvsluttetUtenVedtak
        }
    }

    fun utfall(): Utfall {
        val tags =
            tags
                .mapNotNull { tagString -> Tag.entries.find { it.name == tagString } }
                .map {
                    when (it) {
                        Tag.Innvilget -> Utfall.INNVILGELSE
                        Tag.DelvisInnvilget -> Utfall.DELVIS_INNVILGELSE
                        Tag.Avslag -> Utfall.AVSLAG
                    }
                }
        return tags.singleOrNull() ?: error("Mangler utfall-tag eller har flere utfall-tags")
    }

    fun overlapperMedInfotrygd(): Boolean = tags.any { it == "OverlapperMedInfotrygd" }

    fun avsluttetUtenVedtak() {
        tilstand = Tilstand.AvsluttetUtenVedtak
    }

    fun avsluttetUtenVedtakMedVarsler() {
        tilstand = Tilstand.AvsluttetUtenVedtakMedVarsler
    }

    fun overlapperMed(perioder: List<Periode>): Boolean = perioder.any { it.overlapper(Periode(fom = fom, tom = tom)) }

    fun oppdaterDatoer(
        fom: LocalDate,
        tom: LocalDate,
        skjæringstidspunkt: LocalDate,
    ) {
        this.fom = fom
        this.tom = tom
        this.skjæringstidspunkt = skjæringstidspunkt
    }

    fun oppdaterTags(tags: List<String>) {
        this.tags = tags.toSet()
    }

    fun oppdaterUtbetalingId(utbetalingId: UtbetalingId) {
        this.utbetalingId = utbetalingId
    }

    fun forkastUtbetaling() {
        if (tilstand == Tilstand.KlarTilBehandling) {
            this.utbetalingId = null
            tilstand = Tilstand.VidereBehandlingAvklares
        }
    }

    fun nyUtbetaling(utbetalingId: UtbetalingId) {
        this.utbetalingId = utbetalingId
        tilstand = Tilstand.KlarTilBehandling
    }

    companion object {
        fun fraLagring(
            id: BehandlingUnikId,
            spleisBehandlingId: SpleisBehandlingId?,
            vedtaksperiodeId: VedtaksperiodeId,
            utbetalingId: UtbetalingId?,
            tags: Set<String>,
            tilstand: Tilstand,
            fom: LocalDate,
            tom: LocalDate,
            skjæringstidspunkt: LocalDate,
            yrkesaktivitetstype: Yrkesaktivitetstype,
        ) = Behandling(
            id = id,
            spleisBehandlingId = spleisBehandlingId,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId,
            tags = tags,
            tilstand = tilstand,
            fom = fom,
            tom = tom,
            skjæringstidspunkt = skjæringstidspunkt,
            yrkesaktivitetstype = yrkesaktivitetstype,
        )

        private fun ny(
            spleisBehandlingId: SpleisBehandlingId,
            vedtaksperiodeId: VedtaksperiodeId,
            fom: LocalDate,
            tom: LocalDate,
            skjæringstidspunkt: LocalDate,
            yrkesaktivitetstype: Yrkesaktivitetstype,
        ) = Behandling(
            id = BehandlingUnikId(UUID.randomUUID()),
            spleisBehandlingId = spleisBehandlingId,
            vedtaksperiodeId = vedtaksperiodeId,
            fom = fom,
            tom = tom,
            skjæringstidspunkt = skjæringstidspunkt,
            yrkesaktivitetstype = yrkesaktivitetstype,
            utbetalingId = null,
            tags = emptySet(),
            tilstand = Tilstand.VidereBehandlingAvklares,
        )

        fun ny(
            spleisBehandlingId: SpleisBehandlingId,
            vedtaksperiodeId: VedtaksperiodeId,
            fom: LocalDate,
            tom: LocalDate,
            yrkesaktivitetstype: Yrkesaktivitetstype,
        ) = ny(
            spleisBehandlingId = spleisBehandlingId,
            vedtaksperiodeId = vedtaksperiodeId,
            fom = fom,
            tom = tom,
            skjæringstidspunkt = fom,
            yrkesaktivitetstype = yrkesaktivitetstype,
        )

        fun nyBasertPåTidligereBehandling(
            spleisBehandlingId: SpleisBehandlingId,
            fom: LocalDate,
            tom: LocalDate,
            yrkesaktivitetstype: Yrkesaktivitetstype,
            tidligereBehandling: Behandling,
        ) = ny(
            spleisBehandlingId = spleisBehandlingId,
            vedtaksperiodeId = tidligereBehandling.vedtaksperiodeId,
            fom = fom,
            tom = tom,
            skjæringstidspunkt = tidligereBehandling.skjæringstidspunkt,
            yrkesaktivitetstype = yrkesaktivitetstype,
        )

        // Alle behandlinger som må sees i sammenheng når saksbehandler behandler saken. Dvs. alle behandlinger som overlapper i tid eller ligger før og har samme skjæringstidspunkt som behandlingen som er til godkjenning.
        fun Collection<Behandling>.behandlingspakke(behandling: Behandling): Set<Behandling> = this.filter { it.fom <= behandling.tom && it.skjæringstidspunkt == behandling.skjæringstidspunkt }.toSet() + behandling
    }
}
