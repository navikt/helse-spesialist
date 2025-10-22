package no.nav.helse.spesialist.domain

import no.nav.helse.modell.vedtak.Utfall
import no.nav.helse.spesialist.domain.ddd.Entity
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
) : Entity<SpleisBehandlingId>(id) {
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

    companion object {
        fun fraLagring(
            id: SpleisBehandlingId,
            tags: Set<String>,
            fødselsnummer: String,
        ) = Behandling(id, tags, fødselsnummer)
    }
}
