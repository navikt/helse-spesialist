package no.nav.helse.spesialist.domain.andreytelser

import no.nav.helse.spesialist.domain.NAVIdent
import no.nav.helse.spesialist.domain.TotrinnsvurderingId
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelserEvent.Metadata
import java.time.Instant

sealed interface GraderteAndreYtelserEvent {
    val metadata: Metadata

    data class Metadata(
        val graderteAndreYtelserId: GraderteAndreYtelserId,
        val sekvensnummer: Int,
        val tidspunkt: Instant,
        val utførtAvSaksbehandlerIdent: NAVIdent,
        val notatTilBeslutter: String,
        val totrinnsvurderingId: TotrinnsvurderingId,
    )
}

data class GraderteAndreYtelserOpprettetEvent(
    override val metadata: Metadata,
    val fødselsnummer: String,
    val graderteAndreYtelserType: GraderteAndreYtelserType,
) : GraderteAndreYtelserEvent
