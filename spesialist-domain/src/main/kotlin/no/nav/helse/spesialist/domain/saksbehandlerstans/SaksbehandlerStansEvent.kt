package no.nav.helse.spesialist.domain.saksbehandlerstans

import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.NAVIdent
import java.time.Instant

sealed interface SaksbehandlerStansEvent {
    val metadata: Metadata
    val identitetsnummer: Identitetsnummer
    val begrunnelse: String

    data class Metadata(
        val saksbehandlerStansId: SaksbehandlerStansId,
        val sekvensnummer: Int,
        val utførtAvSaksbehandlerIdent: NAVIdent,
        val tidspunkt: Instant,
    )
}

data class SaksbehandlerStansOpprettetEvent(
    override val metadata: SaksbehandlerStansEvent.Metadata,
    override val identitetsnummer: Identitetsnummer,
    override val begrunnelse: String,
) : SaksbehandlerStansEvent

data class SaksbehandlerStansOpphevetEvent(
    override val metadata: SaksbehandlerStansEvent.Metadata,
    override val identitetsnummer: Identitetsnummer,
    override val begrunnelse: String,
) : SaksbehandlerStansEvent
