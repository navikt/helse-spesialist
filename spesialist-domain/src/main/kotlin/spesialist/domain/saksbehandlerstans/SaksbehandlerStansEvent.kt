package no.nav.helse.spesialist.domain.saksbehandlerstans

import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.NAVIdent
import java.time.Instant

sealed interface SaksbehandlerStansEvent {
    val metadata: Metadata

    data class Metadata(
        val sekvensnummer: Int,
        val utførtAvSaksbehandlerIdent: NAVIdent,
        val tidspunkt: Instant,
        val identitetsnummer: Identitetsnummer,
        val begrunnelse: String,
    )
}

data class SaksbehandlerStansOpprettetEvent(
    override val metadata: SaksbehandlerStansEvent.Metadata,
) : SaksbehandlerStansEvent

data class SaksbehandlerStansOpphevetEvent(
    override val metadata: SaksbehandlerStansEvent.Metadata,
) : SaksbehandlerStansEvent
