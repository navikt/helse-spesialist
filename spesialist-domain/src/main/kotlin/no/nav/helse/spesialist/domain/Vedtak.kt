package no.nav.helse.spesialist.domain

import no.nav.helse.spesialist.domain.ddd.AggregateRoot
import java.time.Instant

sealed class Vedtak private constructor(
    id: SpleisBehandlingId,
    val tidspunkt: Instant,
) : AggregateRoot<SpleisBehandlingId>(id) {
    class Automatisk(
        id: SpleisBehandlingId,
        tidspunkt: Instant,
    ) : Vedtak(id, tidspunkt)

    class ManueltUtenTotrinnskontroll(
        id: SpleisBehandlingId,
        tidspunkt: Instant,
        val saksbehandlerIdent: String,
    ) : Vedtak(id, tidspunkt)

    class ManueltMedTotrinnskontroll(
        id: SpleisBehandlingId,
        tidspunkt: Instant,
        val saksbehandlerIdent: String,
        val beslutterIdent: String,
    ) : Vedtak(id, tidspunkt)

    companion object {
        fun automatisk(id: SpleisBehandlingId) =
            Automatisk(
                id = id,
                tidspunkt = Instant.now(),
            )

        fun manueltUtenTotrinnskontroll(
            id: SpleisBehandlingId,
            saksbehandlerIdent: String,
        ) = ManueltUtenTotrinnskontroll(
            id = id,
            saksbehandlerIdent = saksbehandlerIdent,
            tidspunkt = Instant.now(),
        )

        fun manueltMedTotrinnskontroll(
            id: SpleisBehandlingId,
            saksbehandlerIdent: String,
            beslutterIdent: String,
        ) = ManueltMedTotrinnskontroll(
            id = id,
            saksbehandlerIdent = saksbehandlerIdent,
            beslutterIdent = beslutterIdent,
            tidspunkt = Instant.now(),
        )
    }
}
