package no.nav.helse.modell

import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.ddd.AggregateRoot
import java.time.LocalDateTime
import java.util.UUID

@JvmInline
value class AnnulleringId(
    val value: Int,
)

class Annullering private constructor(
    id: AnnulleringId?,
    val saksbehandlerOid: SaksbehandlerOid,
    val arbeidsgiverFagsystemId: String?,
    val personFagsystemId: String?,
    val tidspunkt: LocalDateTime,
    val årsaker: List<String>,
    val kommentar: String?,
    val vedtaksperiodeId: UUID,
) : AggregateRoot<AnnulleringId>(id) {
    object Factory {
        fun ny(
            arbeidsgiverFagsystemId: String?,
            personFagsystemId: String?,
            saksbehandlerOid: SaksbehandlerOid,
            vedtaksperiodeId: UUID,
            årsaker: List<String>,
            kommentar: String?,
        ) = Annullering(
            id = null,
            saksbehandlerOid = saksbehandlerOid,
            arbeidsgiverFagsystemId = arbeidsgiverFagsystemId,
            personFagsystemId = personFagsystemId,
            tidspunkt = LocalDateTime.now(),
            årsaker = årsaker,
            kommentar = kommentar,
            vedtaksperiodeId = vedtaksperiodeId,
        )

        fun fraLagring(
            id: AnnulleringId,
            arbeidsgiverFagsystemId: String?,
            personFagsystemId: String?,
            saksbehandlerOid: SaksbehandlerOid,
            vedtaksperiodeId: UUID,
            tidspunkt: LocalDateTime,
            årsaker: List<String>,
            kommentar: String?,
        ) = Annullering(
            id = id,
            saksbehandlerOid = saksbehandlerOid,
            arbeidsgiverFagsystemId = arbeidsgiverFagsystemId,
            personFagsystemId = personFagsystemId,
            tidspunkt = tidspunkt,
            årsaker = årsaker,
            kommentar = kommentar,
            vedtaksperiodeId = vedtaksperiodeId,
        )
    }
}
