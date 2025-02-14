package no.nav.helse.spesialist.modell

import no.nav.helse.spesialist.modell.ddd.Entity
import java.time.LocalDateTime

@JvmInline
value class KommentarId(val value: Int)

class Kommentar private constructor(
    id: KommentarId?,
    val tekst: String,
    val saksbehandlerident: String,
    val opprettetTidspunkt: LocalDateTime,
    feilregistrertTidspunkt: LocalDateTime?,
) : Entity<KommentarId>(id) {
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
            id: KommentarId,
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
