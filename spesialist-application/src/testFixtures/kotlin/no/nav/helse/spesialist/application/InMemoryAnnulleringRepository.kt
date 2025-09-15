package no.nav.helse.spesialist.application

import no.nav.helse.db.AnnulleringRepository
import no.nav.helse.modell.Annullering
import no.nav.helse.modell.saksbehandler.handlinger.AnnulleringDto
import no.nav.helse.spesialist.domain.legacy.SaksbehandlerWrapper
import java.time.LocalDateTime

class InMemoryAnnulleringRepository : AnnulleringRepository {
    private data class Key(
        val arbeidsgiverFagsystemId: String,
        val personFagsystemId: String,
    )

    private val data = mutableMapOf<Key, Annullering>()

    override fun lagreAnnullering(
        annulleringDto: AnnulleringDto,
        saksbehandlerWrapper: SaksbehandlerWrapper
    ) {
        data[
            Key(
                arbeidsgiverFagsystemId = annulleringDto.arbeidsgiverFagsystemId,
                personFagsystemId = annulleringDto.personFagsystemId
            )
        ] = Annullering(
            saksbehandlerIdent = saksbehandlerWrapper.saksbehandler.ident,
            arbeidsgiverFagsystemId = annulleringDto.arbeidsgiverFagsystemId,
            personFagsystemId = annulleringDto.personFagsystemId,
            tidspunkt = LocalDateTime.now(),
            arsaker = annulleringDto.årsaker.map { it.arsak },
            begrunnelse = annulleringDto.kommentar,
            vedtaksperiodeId = annulleringDto.vedtaksperiodeId
        )
    }

    override fun finnAnnullering(arbeidsgiverFagsystemId: String, personFagsystemId: String): Annullering? =
        data[Key(arbeidsgiverFagsystemId = arbeidsgiverFagsystemId, personFagsystemId = personFagsystemId)]
}
