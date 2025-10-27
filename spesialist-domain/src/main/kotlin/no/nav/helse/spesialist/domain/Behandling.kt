package no.nav.helse.spesialist.domain

import no.nav.helse.modell.vedtak.Utfall
import no.nav.helse.spesialist.domain.ddd.Entity
import java.time.LocalDate
import java.util.UUID

@JvmInline
value class SpleisBehandlingId(
    val value: UUID,
)

enum class Tag {
    Innvilget,
    DelvisInnvilget,
    Avslag,
}

class Behandling private constructor(
    val id: SpleisBehandlingId,
    val tags: Set<String>,
    val fødselsnummer: String,
    val fom: LocalDate,
    val tom: LocalDate,
    søknadIder: Set<UUID>,
) : Entity<SpleisBehandlingId>(id) {
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

    companion object {
        fun fraLagring(
            id: SpleisBehandlingId,
            tags: Set<String>,
            fødselsnummer: String,
            søknadIder: Set<UUID>,
            fom: LocalDate,
            tom: LocalDate,
        ) = Behandling(
            id = id,
            tags = tags,
            fødselsnummer = fødselsnummer,
            fom = fom,
            tom = tom,
            søknadIder = søknadIder,
        )
    }
}
