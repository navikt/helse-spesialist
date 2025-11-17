package no.nav.helse.spesialist.domain

import no.nav.helse.spesialist.domain.ddd.LateIdAggregateRoot
import no.nav.helse.spesialist.domain.ddd.ValueObject
import java.time.LocalDateTime

@JvmInline
value class DialogId(
    val value: Long,
) : ValueObject

class Dialog private constructor(
    id: DialogId?,
    val opprettetTidspunkt: LocalDateTime,
    kommentarer: List<Kommentar>,
) : LateIdAggregateRoot<DialogId>(id) {
    private val _kommentarer: MutableList<Kommentar> = kommentarer.toMutableList()
    val kommentarer: List<Kommentar>
        get() = _kommentarer

    fun leggTilKommentar(
        tekst: String,
        saksbehandlerident: String,
    ): Kommentar =
        Kommentar.Factory
            .ny(
                tekst = tekst,
                saksbehandlerident = saksbehandlerident,
            ).also(_kommentarer::add)

    fun finnKommentar(kommentarId: KommentarId): Kommentar? = kommentarer.firstOrNull { it.harFåttTildeltId() && it.id() == kommentarId }

    fun feilregistrerKommentar(kommentarId: KommentarId) {
        kommentarer.first { it.harFåttTildeltId() && it.id() == kommentarId }.feilregistrer()
    }

    object Factory {
        fun ny() =
            Dialog(
                id = null,
                opprettetTidspunkt = LocalDateTime.now(),
                kommentarer = emptyList(),
            )

        fun fraLagring(
            id: DialogId,
            opprettetTidspunkt: LocalDateTime,
            kommentarer: List<Kommentar>,
        ) = Dialog(
            id = id,
            opprettetTidspunkt = opprettetTidspunkt,
            kommentarer = kommentarer,
        )
    }
}
