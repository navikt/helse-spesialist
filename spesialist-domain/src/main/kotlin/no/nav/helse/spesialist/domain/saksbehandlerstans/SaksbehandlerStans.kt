package no.nav.helse.spesialist.domain.saksbehandlerstans

import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.NAVIdent
import no.nav.helse.spesialist.domain.ddd.AggregateRoot
import no.nav.helse.spesialist.domain.ddd.ValueObject
import java.time.Instant
import java.util.UUID

@JvmInline
value class SaksbehandlerStansId(
    val value: UUID,
) : ValueObject

class SaksbehandlerStans private constructor(
    stansOpprettetEvent: SaksbehandlerStansOpprettetEvent,
) : AggregateRoot<SaksbehandlerStansId>(stansOpprettetEvent.metadata.saksbehandlerStansId) {
    private val _events: MutableList<SaksbehandlerStansEvent> = mutableListOf(stansOpprettetEvent)
    val events: List<SaksbehandlerStansEvent> get() = _events

    val identitetsnummer: Identitetsnummer = stansOpprettetEvent.identitetsnummer
    var versjon: Int = stansOpprettetEvent.metadata.sekvensnummer
        private set
    var erStanset: Boolean = true
        private set

    fun opphevStans(
        utførtAvSaksbehandlerIdent: NAVIdent,
        begrunnelse: String,
    ) {
        val saksbehandlerStansOpphevetEvent =
            SaksbehandlerStansOpphevetEvent(
                metadata =
                    SaksbehandlerStansEvent.Metadata(
                        saksbehandlerStansId = id,
                        sekvensnummer = versjon + 1,
                        utførtAvSaksbehandlerIdent = utførtAvSaksbehandlerIdent,
                        tidspunkt = Instant.now(),
                    ),
                identitetsnummer = identitetsnummer,
                begrunnelse = begrunnelse,
            )
        versjon = saksbehandlerStansOpphevetEvent.metadata.sekvensnummer
        erStanset = false
        _events.add(saksbehandlerStansOpphevetEvent)
    }

    fun opprettStans(
        utførtAvSaksbehandlerIdent: NAVIdent,
        begrunnelse: String,
    ) {
        val saksbehandlerStansOpprettetEvent =
            SaksbehandlerStansOpprettetEvent(
                metadata =
                    SaksbehandlerStansEvent.Metadata(
                        saksbehandlerStansId = id,
                        sekvensnummer = versjon + 1,
                        utførtAvSaksbehandlerIdent = utførtAvSaksbehandlerIdent,
                        tidspunkt = Instant.now(),
                    ),
                identitetsnummer = identitetsnummer,
                begrunnelse = begrunnelse,
            )
        versjon = saksbehandlerStansOpprettetEvent.metadata.sekvensnummer
        erStanset = true
        _events.add(saksbehandlerStansOpprettetEvent)
    }

    companion object {
        fun ny(
            utførtAvSaksbehandlerIdent: NAVIdent,
            begrunnelse: String,
            identitetsnummer: Identitetsnummer,
        ) = SaksbehandlerStans(
            SaksbehandlerStansOpprettetEvent(
                metadata =
                    SaksbehandlerStansEvent.Metadata(
                        saksbehandlerStansId = SaksbehandlerStansId(UUID.randomUUID()),
                        sekvensnummer = 1,
                        utførtAvSaksbehandlerIdent = utførtAvSaksbehandlerIdent,
                        tidspunkt = Instant.now(),
                    ),
                begrunnelse = begrunnelse,
                identitetsnummer = identitetsnummer,
            ),
        )
    }
}
