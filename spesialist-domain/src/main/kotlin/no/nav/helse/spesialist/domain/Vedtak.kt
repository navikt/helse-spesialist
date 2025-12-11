package no.nav.helse.spesialist.domain

import no.nav.helse.spesialist.domain.ddd.AggregateRoot
import java.time.Instant

class Vedtak private constructor(
    id: SpleisBehandlingId,
    val automatiskFattet: Boolean,
    val saksbehandlerIdent: String?,
    val beslutterIdent: String?,
    val tidspunkt: Instant,
) : AggregateRoot<SpleisBehandlingId>(id) {
    companion object {
        fun fraLagring(
            id: SpleisBehandlingId,
            automatiskFattet: Boolean,
            saksbehandlerIdent: String?,
            beslutterIdent: String?,
            tidspunkt: Instant,
        ) = Vedtak(
            id = id,
            automatiskFattet = automatiskFattet,
            saksbehandlerIdent = saksbehandlerIdent,
            beslutterIdent = beslutterIdent,
            tidspunkt = tidspunkt,
        )

        fun automatisk(id: SpleisBehandlingId) =
            Vedtak(
                id = id,
                automatiskFattet = true,
                saksbehandlerIdent = null,
                beslutterIdent = null,
                tidspunkt = Instant.now(),
            )

        fun manueltUtenTotrinnskontroll(
            id: SpleisBehandlingId,
            saksbehandlerIdent: String,
        ) = Vedtak(
            id = id,
            automatiskFattet = false,
            saksbehandlerIdent = saksbehandlerIdent,
            beslutterIdent = null,
            tidspunkt = Instant.now(),
        )

        fun manueltMedTotrinnskontroll(
            id: SpleisBehandlingId,
            saksbehandlerIdent: String,
            beslutterIdent: String,
        ) = Vedtak(
            id = id,
            automatiskFattet = false,
            saksbehandlerIdent = saksbehandlerIdent,
            beslutterIdent = beslutterIdent,
            tidspunkt = Instant.now(),
        )
    }
}
