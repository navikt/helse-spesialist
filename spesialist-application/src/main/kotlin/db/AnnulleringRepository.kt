package no.nav.helse.db

import no.nav.helse.spesialist.domain.Annullering
import no.nav.helse.spesialist.domain.AnnulleringId
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
