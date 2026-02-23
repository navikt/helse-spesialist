package no.nav.helse.spesialist.domain

import no.nav.helse.spesialist.domain.ddd.AggregateRoot
import java.time.Instant

sealed class Vedtak private constructor(
    id: SpleisBehandlingId,
    val tidspunkt: Instant,
    behandletAvSpleis: Boolean,
) : AggregateRoot<SpleisBehandlingId>(id) {
    var behandletAvSpleis = behandletAvSpleis
        private set

    fun markerSomBehandletAvSpleis() {
        behandletAvSpleis = true
    }

    class Automatisk(
        id: SpleisBehandlingId,
        tidspunkt: Instant,
        behandletAvSpleis: Boolean,
    ) : Vedtak(id, tidspunkt, behandletAvSpleis)

    class ManueltUtenTotrinnskontroll(
        id: SpleisBehandlingId,
        tidspunkt: Instant,
        val saksbehandlerIdent: NAVIdent,
        behandletAvSpleis: Boolean,
    ) : Vedtak(id, tidspunkt, behandletAvSpleis)

    class ManueltMedTotrinnskontroll(
        id: SpleisBehandlingId,
        tidspunkt: Instant,
        val saksbehandlerIdent: NAVIdent,
        val beslutterIdent: NAVIdent,
        behandletAvSpleis: Boolean,
    ) : Vedtak(id, tidspunkt, behandletAvSpleis)

    companion object {
        fun automatisk(id: SpleisBehandlingId) =
            Automatisk(
                id = id,
                tidspunkt = Instant.now(),
                behandletAvSpleis = false,
            )

        fun manueltUtenTotrinnskontroll(
            id: SpleisBehandlingId,
            saksbehandlerIdent: NAVIdent,
        ) = ManueltUtenTotrinnskontroll(
            id = id,
            saksbehandlerIdent = saksbehandlerIdent,
            tidspunkt = Instant.now(),
            behandletAvSpleis = false,
        )

        fun manueltMedTotrinnskontroll(
            id: SpleisBehandlingId,
            saksbehandlerIdent: NAVIdent,
            beslutterIdent: NAVIdent,
        ) = ManueltMedTotrinnskontroll(
            id = id,
            saksbehandlerIdent = saksbehandlerIdent,
            beslutterIdent = beslutterIdent,
            tidspunkt = Instant.now(),
            behandletAvSpleis = false,
        )
    }
}
