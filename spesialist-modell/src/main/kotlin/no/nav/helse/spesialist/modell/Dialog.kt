package no.nav.helse.spesialist.modell

import no.nav.helse.spesialist.modell.ddd.AggregateRoot
import java.time.LocalDateTime

class Dialog private constructor(
    id: Long?,
    val opprettetTidspunkt: LocalDateTime,
    kommentarer: List<Kommentar>,
) : AggregateRoot<Long>(id) {
    private val _kommentarer: MutableList<Kommentar> = kommentarer.toMutableList()
    val kommentarer: List<Kommentar>
        get() = _kommentarer

    fun leggTilKommentar(
        tekst: String,
        saksbehandlerident: String,
    ): Kommentar =
        Kommentar.Factory.ny(
            tekst = tekst,
            saksbehandlerident = saksbehandlerident,
        ).also(_kommentarer::add)

    fun finnKommentar(kommentarId: Int): Kommentar? = kommentarer.firstOrNull { it.harFåttTildeltId() && it.id() == kommentarId }

    fun feilregistrerKommentar(kommentarId: Int) {
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
            id: Long,
            opprettetTidspunkt: LocalDateTime,
            kommentarer: List<Kommentar>,
        ) = Dialog(
            id = id,
            opprettetTidspunkt = opprettetTidspunkt,
            kommentarer = kommentarer,
        )
    }
}
