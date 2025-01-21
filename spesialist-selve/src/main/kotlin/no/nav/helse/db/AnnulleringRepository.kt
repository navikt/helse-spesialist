package no.nav.helse.db

import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.saksbehandler.handlinger.AnnulleringDto
import no.nav.helse.spesialist.api.graphql.schema.Annullering

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
