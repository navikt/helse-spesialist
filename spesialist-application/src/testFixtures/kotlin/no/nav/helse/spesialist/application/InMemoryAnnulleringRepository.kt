package no.nav.helse.spesialist.application

import no.nav.helse.db.AnnulleringRepository
import no.nav.helse.modell.Annullering
import no.nav.helse.modell.AnnulleringId
import java.util.UUID

class InMemoryAnnulleringRepository : AnnulleringRepository, AbstractInMemoryRepository<AnnulleringId, Annullering>() {
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

    override fun generateId(): AnnulleringId = AnnulleringId((alle().maxOfOrNull { it.id().value } ?: 0) + 1)
}
