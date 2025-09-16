package no.nav.helse.db

import no.nav.helse.modell.Annullering
import no.nav.helse.modell.AnnulleringId
import java.util.UUID

interface AnnulleringRepository {
    fun lagreAnnullering(annullering: Annullering)

    fun finnAnnullering(id: AnnulleringId): Annullering?

    fun finnAnnullering(vedtaksperiodeId: UUID): Annullering?

    fun finnAnnulleringMedEnAv(
        arbeidsgiverFagsystemId: String,
        personFagsystemId: String,
    ): Annullering?
}
