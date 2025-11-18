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
)

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
    søknadIder: Set<UUID>,
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

    private val søknadIder = søknadIder.toMutableSet()

    fun søknadIder() = søknadIder.toSet()

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

    fun kobleSøknader(eksterneSøknadIder: Set<UUID>) {
        søknadIder += eksterneSøknadIder
    }

    fun overlapperMedInfotrygd(): Boolean = tags.any { it == "OverlapperMedInfotrygd" }

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
            søknadIder: Set<UUID>,
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
            søknadIder = søknadIder,
        )
    }
}
