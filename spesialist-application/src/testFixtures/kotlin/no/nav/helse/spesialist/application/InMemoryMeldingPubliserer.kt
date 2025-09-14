package no.nav.helse.spesialist.application

import no.nav.helse.MeldingPubliserer
import no.nav.helse.mediator.KommandokjedeEndretEvent
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.melding.SubsumsjonEvent
import no.nav.helse.modell.melding.UtgåendeHendelse
import java.util.UUID

class InMemoryMeldingPubliserer : MeldingPubliserer {
    data class PublisertUtgåendeHendelse(
        val fødselsnummer: String,
        val hendelse: UtgåendeHendelse,
        val årsak: String
    )

    val publiserteUtgåendeHendelser = mutableListOf<PublisertUtgåendeHendelse>()

    override fun publiser(
        fødselsnummer: String,
        hendelse: UtgåendeHendelse,
        årsak: String
    ) {
        publiserteUtgåendeHendelser.add(
            PublisertUtgåendeHendelse(
                fødselsnummer = fødselsnummer,
                hendelse = hendelse,
                årsak = årsak
            )
        )
    }

    data class PublisertSubsumsjon(
        val fødselsnummer: String,
        val subsumsjonEvent: SubsumsjonEvent,
        val versjonAvKode: String
    )

    val publiserteSubsumsjoner = mutableListOf<PublisertSubsumsjon>()

    override fun publiser(
        fødselsnummer: String,
        subsumsjonEvent: SubsumsjonEvent,
        versjonAvKode: String
    ) {
        publiserteSubsumsjoner.add(
            PublisertSubsumsjon(
                fødselsnummer = fødselsnummer,
                subsumsjonEvent = subsumsjonEvent,
                versjonAvKode = versjonAvKode
            )
        )
    }

    data class PublisertBehovListe(
        val hendelseId: UUID,
        val commandContextId: UUID,
        val fødselsnummer: String,
        val behov: List<Behov>
    )

    val publiserteBehovLister = mutableListOf<PublisertBehovListe>()

    override fun publiser(
        hendelseId: UUID,
        commandContextId: UUID,
        fødselsnummer: String,
        behov: List<Behov>
    ) {
        publiserteBehovLister.add(
            PublisertBehovListe(
                hendelseId = hendelseId,
                commandContextId = commandContextId,
                fødselsnummer = fødselsnummer,
                behov = behov
            )
        )
    }

    data class PublisertKommandokjedeEndretEvent(
        val fødselsnummer: String,
        val event: KommandokjedeEndretEvent,
        val hendelseNavn: String
    )

    val publiserteKommandokjedeEndretEvents = mutableListOf<PublisertKommandokjedeEndretEvent>()

    override fun publiser(
        fødselsnummer: String,
        event: KommandokjedeEndretEvent,
        hendelseNavn: String
    ) {
        publiserteKommandokjedeEndretEvents.add(
            PublisertKommandokjedeEndretEvent(
                fødselsnummer = fødselsnummer,
                event = event,
                hendelseNavn = hendelseNavn
            )
        )
    }
}
