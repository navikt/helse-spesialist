package no.nav.helse.spesialist.application

import no.nav.helse.db.AnnulleringRepository
import no.nav.helse.spesialist.domain.Annullering
import no.nav.helse.spesialist.domain.AnnulleringId
import java.util.UUID

class InMemoryAnnulleringRepository : AnnulleringRepository, AbstractLateIdInMemoryRepository<AnnulleringId, Annullering>() {
    override fun lagreAnnullering(annullering: Annullering) {
        lagre(annullering)
    }

    override fun finnAnnullering(id: AnnulleringId): Annullering? = finn(id)

    override fun finnAnnullering(vedtaksperiodeId: UUID): Annullering? =
        alle().find { it.vedtaksperiodeId == vedtaksperiodeId }

    override fun finnAnnulleringMedEnAv(
        arbeidsgiverFagsystemId: String,
        personFagsystemId: String
    ): Annullering? =
        alle().find { it.arbeidsgiverFagsystemId == arbeidsgiverFagsystemId || it.personFagsystemId == personFagsystemId }

    override fun tildelIderSomMangler(root: Annullering) {
        if (!root.harFåttTildeltId())
            root.tildelId(AnnulleringId((alle().maxOfOrNull { it.id().value } ?: 0) + 1))
    }

    override fun deepCopy(original: Annullering): Annullering = Annullering.Factory.fraLagring(
        id = original.id(),
        arbeidsgiverFagsystemId = original.arbeidsgiverFagsystemId,
        personFagsystemId = original.personFagsystemId,
        saksbehandlerOid = original.saksbehandlerOid,
        vedtaksperiodeId = original.vedtaksperiodeId,
        tidspunkt = original.tidspunkt,
        årsaker = original.årsaker.toList(),
        kommentar = original.kommentar,
    )
}
