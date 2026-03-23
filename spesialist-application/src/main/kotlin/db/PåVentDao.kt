package no.nav.helse.db

import no.nav.helse.modell.saksbehandler.handlinger.PåVentÅrsak
import java.time.LocalDate
import java.util.UUID

interface PåVentDao {
    fun erPåVent(vedtaksperiodeId: UUID): Boolean

    fun lagrePåVent(
        oppgaveId: Long,
        saksbehandlerOid: UUID,
        frist: LocalDate?,
        årsaker: List<PåVentÅrsak>,
        notatTekst: String? = null,
        dialogRef: Long,
    )

    fun slettPåVent(oppgaveId: Long): Int?

    fun erPåVent(oppgaveId: Long): Boolean

    fun oppdaterPåVent(
        oppgaveId: Long,
        saksbehandlerOid: UUID,
        frist: LocalDate?,
        årsaker: List<PåVentÅrsak>,
        notatTekst: String? = null,
        dialogRef: Long,
    )
}
