package no.nav.helse.db

import no.nav.helse.modell.Annullering
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.saksbehandler.handlinger.AnnulleringDto

interface AnnulleringRepository {
    fun lagreAnnullering(
        annulleringDto: AnnulleringDto,
        saksbehandler: Saksbehandler,
    )

    fun finnAnnullering(
        arbeidsgiverFagsystemId: String,
        personFagsystemId: String,
    ): Annullering?

    fun finnAnnullering(annulleringDto: AnnulleringDto): Annullering?
}
