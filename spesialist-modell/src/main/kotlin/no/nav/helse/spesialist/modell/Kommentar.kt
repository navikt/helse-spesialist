package no.nav.helse.spesialist.modell

import no.nav.helse.spesialist.modell.ddd.Entity
import java.time.LocalDateTime

class Kommentar private constructor(
    id: Int?,
    val tekst: String,
    val saksbehandlerident: String,
    val opprettetTidspunkt: LocalDateTime,
    feilregistrertTidspunkt: LocalDateTime?,
) : Entity<Int>(id) {
    var feilregistrertTidspunkt: LocalDateTime? = feilregistrertTidspunkt
        private set

    internal fun feilregistrer() {
        feilregistrertTidspunkt = LocalDateTime.now()
    }

    object Factory {
        fun ny(
            tekst: String,
            saksbehandlerident: String,
        ) = Kommentar(
            id = null,
            tekst = tekst,
            saksbehandlerident = saksbehandlerident,
            opprettetTidspunkt = LocalDateTime.now(),
            feilregistrertTidspunkt = null,
        )

        fun fraLagring(
            id: Int,
            tekst: String,
            saksbehandlerident: String,
            opprettetTidspunkt: LocalDateTime,
            feilregistrertTidspunkt: LocalDateTime?,
        ) = Kommentar(
            id = id,
            tekst = tekst,
            saksbehandlerident = saksbehandlerident,
            opprettetTidspunkt = opprettetTidspunkt,
            feilregistrertTidspunkt = feilregistrertTidspunkt,
        )
    }
}
