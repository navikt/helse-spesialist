package no.nav.helse.spesialist.domain

import no.nav.helse.modell.vedtak.Utfall
import no.nav.helse.spesialist.domain.ddd.Entity
import java.time.LocalDate
import java.util.UUID

@JvmInline
value class SpleisBehandlingId(
    val value: UUID,
)

@JvmInline
value class BehandlingUnikId(
    val value: UUID,
)

enum class Tag {
    Innvilget,
    DelvisInnvilget,
    Avslag,
}

class Behandling private constructor(
    id: BehandlingUnikId,
    val spleisBehandlingId: SpleisBehandlingId?,
    val vedtaksperiodeId: VedtaksperiodeId,
    val tags: Set<String>,
    val fom: LocalDate,
    val tom: LocalDate,
    val skjæringstidspunkt: LocalDate,
    søknadIder: Set<UUID>,
) : Entity<BehandlingUnikId>(id) {
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
            tags: Set<String>,
            fom: LocalDate,
            tom: LocalDate,
            skjæringstidspunkt: LocalDate,
            søknadIder: Set<UUID>,
        ) = Behandling(
            id = id,
            spleisBehandlingId = spleisBehandlingId,
            vedtaksperiodeId = vedtaksperiodeId,
            tags = tags,
            fom = fom,
            tom = tom,
            skjæringstidspunkt = skjæringstidspunkt,
            søknadIder = søknadIder,
        )
    }
}
