package no.nav.helse.db

import no.nav.helse.modell.Annullering
import no.nav.helse.modell.saksbehandler.handlinger.AnnulleringDto
import no.nav.helse.spesialist.domain.legacy.LegacySaksbehandler

interface AnnulleringRepository {
    fun lagreAnnullering(
        annulleringDto: AnnulleringDto,
        legacySaksbehandler: LegacySaksbehandler,
    )

    fun finnAnnullering(
        arbeidsgiverFagsystemId: String,
        personFagsystemId: String,
    ): Annullering?

    fun finnAnnullering(annulleringDto: AnnulleringDto): Annullering?
}
