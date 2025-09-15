package no.nav.helse.spesialist.application

import no.nav.helse.db.AnnulleringRepository
import no.nav.helse.modell.Annullering
import no.nav.helse.modell.AnnulleringId

class InMemoryAnnulleringRepository : AnnulleringRepository {
    private val data = mutableMapOf<AnnulleringId, Annullering>()

    override fun lagreAnnullering(annullering: Annullering) {
        if (!annullering.harFåttTildeltId()) {
            annullering.tildelId(AnnulleringId((data.keys.maxOfOrNull { it.value } ?: 0) + 1))
        }
        data[annullering.id()] = annullering
    }

    override fun finnAnnullering(id: AnnulleringId): Annullering? =
        data[id]

    override fun finnAnnulleringMedEnAv(
        arbeidsgiverFagsystemId: String,
        personFagsystemId: String
    ): Annullering? =
        data.values.firstOrNull {
            it.arbeidsgiverFagsystemId == arbeidsgiverFagsystemId || it.personFagsystemId == personFagsystemId
        }
}
