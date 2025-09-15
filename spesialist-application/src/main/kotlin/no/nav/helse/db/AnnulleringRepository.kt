package no.nav.helse.db

import no.nav.helse.modell.Annullering
import no.nav.helse.modell.AnnulleringId

interface AnnulleringRepository {
    fun lagreAnnullering(annullering: Annullering)

    fun finnAnnullering(id: AnnulleringId): Annullering?

    fun finnAnnulleringMedEnAv(
        arbeidsgiverFagsystemId: String,
        personFagsystemId: String,
    ): Annullering?
}
