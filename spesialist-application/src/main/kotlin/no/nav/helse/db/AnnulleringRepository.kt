package no.nav.helse.db

import no.nav.helse.modell.Annullering
import no.nav.helse.modell.saksbehandler.handlinger.AnnulleringDto
import no.nav.helse.spesialist.domain.legacy.SaksbehandlerWrapper

interface AnnulleringRepository {
    fun lagreAnnullering(
        annulleringDto: AnnulleringDto,
        saksbehandlerWrapper: SaksbehandlerWrapper,
    )

    fun finnAnnullering(
        arbeidsgiverFagsystemId: String,
        personFagsystemId: String,
    ): Annullering?

    fun finnAnnullering(annulleringDto: AnnulleringDto): Annullering?
}
