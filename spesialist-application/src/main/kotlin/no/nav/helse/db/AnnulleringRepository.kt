package no.nav.helse.db

import no.nav.helse.spesialist.domain.Annullering
import no.nav.helse.spesialist.domain.AnnulleringId
import java.util.UUID

interface AnnulleringRepository {
    fun lagreAnnullering(annullering: Annullering)

    fun finnOrNull(id: AnnulleringId): Annullering?

    fun finnOrNull(vedtaksperiodeId: UUID): Annullering?

    fun finnMedEnAvOrNull(
        arbeidsgiverFagsystemId: String,
        personFagsystemId: String,
    ): Annullering?
}
