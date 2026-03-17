package no.nav.helse.spesialist.domain.saksbehandlerstans

import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.NAVIdent
import no.nav.helse.spesialist.domain.ddd.AggregateRoot
import java.time.Instant

class SaksbehandlerStans private constructor(
    stansOpprettetEvent: SaksbehandlerStansOpprettetEvent,
) : AggregateRoot<Identitetsnummer>(stansOpprettetEvent.metadata.identitetsnummer) {
    private val _events: MutableList<SaksbehandlerStansEvent> = mutableListOf(stansOpprettetEvent)
    val events: List<SaksbehandlerStansEvent> get() = _events

    val identitetsnummer: Identitetsnummer = stansOpprettetEvent.metadata.identitetsnummer
    var versjon: Int = stansOpprettetEvent.metadata.sekvensnummer
        private set
    var erStanset: Boolean = true
        private set

    fun opphevStans(
        utførtAvSaksbehandlerIdent: NAVIdent,
        begrunnelse: String,
    ) {
        apply(
            SaksbehandlerStansOpphevetEvent(
                metadata =
                    SaksbehandlerStansEvent.Metadata(
                        sekvensnummer = versjon + 1,
                        utførtAvSaksbehandlerIdent = utførtAvSaksbehandlerIdent,
                        tidspunkt = Instant.now(),
                        identitetsnummer = identitetsnummer,
                        begrunnelse = begrunnelse,
                    ),
            ),
        )
    }

    fun opprettStans(
        utførtAvSaksbehandlerIdent: NAVIdent,
        begrunnelse: String,
    ) {
        apply(
            SaksbehandlerStansOpprettetEvent(
                metadata =
                    SaksbehandlerStansEvent.Metadata(
                        sekvensnummer = versjon + 1,
                        utførtAvSaksbehandlerIdent = utførtAvSaksbehandlerIdent,
                        tidspunkt = Instant.now(),
                        identitetsnummer = identitetsnummer,
                        begrunnelse = begrunnelse,
                    ),
            ),
        )
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
                        sekvensnummer = 1,
                        utførtAvSaksbehandlerIdent = utførtAvSaksbehandlerIdent,
                        tidspunkt = Instant.now(),
                        begrunnelse = begrunnelse,
                        identitetsnummer = identitetsnummer,
                    ),
            ),
        )

        fun fraLagring(events: List<SaksbehandlerStansEvent>): SaksbehandlerStans =
            SaksbehandlerStans(events.first() as SaksbehandlerStansOpprettetEvent)
                .also { stans -> events.drop(1).forEach(stans::apply) }
    }

    private fun apply(event: SaksbehandlerStansEvent) {
        håndterEvent(event)
        when (event) {
            is SaksbehandlerStansOpprettetEvent -> {
                if (erStanset) error("Prøvde å opprette stans som allerede er stanset!")
                erStanset = true
            }

            is SaksbehandlerStansOpphevetEvent -> {
                if (!erStanset) error("Prøvde å oppheve stans som ikke er stanset!")
                erStanset = false
            }
        }
    }

    private fun håndterEvent(event: SaksbehandlerStansEvent) {
        if (event.metadata.sekvensnummer != versjon + 1) { // sekvensnummer i riktig rekkefølge
            error("Fikk events ute av rekkefølge: $versjon -> ${event.metadata.sekvensnummer}")
        }
        versjon = event.metadata.sekvensnummer
        _events.add(event)
    }
}
