package no.nav.helse.spesialist.application

import no.nav.helse.MeldingPubliserer
import no.nav.helse.mediator.KommandokjedeEndretEvent
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.melding.SubsumsjonEvent
import no.nav.helse.modell.melding.UtgåendeHendelse
import java.util.UUID

class KøetMeldingPubliserer(
    private val meldingPubliserer: MeldingPubliserer,
) : MeldingPubliserer {
    private val kø = mutableListOf<KøetMelding>()

    private sealed interface KøetMelding

    private data class KøetBehovListe(
        val hendelseId: UUID,
        val commandContextId: UUID,
        val fødselsnummer: String,
        val behov: List<Behov>,
    ) : KøetMelding

    override fun publiser(
        hendelseId: UUID,
        commandContextId: UUID,
        fødselsnummer: String,
        behov: List<Behov>,
    ) {
        kø.add(
            KøetBehovListe(
                hendelseId = hendelseId,
                commandContextId = commandContextId,
                fødselsnummer = fødselsnummer,
                behov = behov,
            ),
        )
    }

    private data class KøetKommandokjedeEndretEvent(
        val fødselsnummer: String,
        val event: KommandokjedeEndretEvent,
        val hendelseNavn: String,
    ) : KøetMelding

    override fun publiser(
        fødselsnummer: String,
        event: KommandokjedeEndretEvent,
        hendelseNavn: String,
    ) {
        kø.add(
            KøetKommandokjedeEndretEvent(
                fødselsnummer = fødselsnummer,
                event = event,
                hendelseNavn = hendelseNavn,
            ),
        )
    }

    private data class KøetSubsumsjon(
        val fødselsnummer: String,
        val subsumsjonEvent: SubsumsjonEvent,
        val versjonAvKode: String,
    ) : KøetMelding

    override fun publiser(
        fødselsnummer: String,
        subsumsjonEvent: SubsumsjonEvent,
        versjonAvKode: String,
    ) {
        kø.add(
            KøetSubsumsjon(
                fødselsnummer = fødselsnummer,
                subsumsjonEvent = subsumsjonEvent,
                versjonAvKode = versjonAvKode,
            ),
        )
    }

    private data class KøetUtgåendeHendelse(
        val fødselsnummer: String,
        val hendelse: UtgåendeHendelse,
        val årsak: String,
    ) : KøetMelding

    override fun publiser(
        fødselsnummer: String,
        hendelse: UtgåendeHendelse,
        årsak: String,
    ) {
        kø.add(
            KøetUtgåendeHendelse(
                fødselsnummer = fødselsnummer,
                hendelse = hendelse,
                årsak = årsak,
            ),
        )
    }

    fun flush() {
        kø.forEach {
            when (it) {
                is KøetBehovListe ->
                    meldingPubliserer.publiser(
                        hendelseId = it.hendelseId,
                        commandContextId = it.commandContextId,
                        fødselsnummer = it.fødselsnummer,
                        behov = it.behov,
                    )

                is KøetKommandokjedeEndretEvent ->
                    meldingPubliserer.publiser(
                        fødselsnummer = it.fødselsnummer,
                        event = it.event,
                        hendelseNavn = it.hendelseNavn,
                    )

                is KøetSubsumsjon ->
                    meldingPubliserer.publiser(
                        fødselsnummer = it.fødselsnummer,
                        subsumsjonEvent = it.subsumsjonEvent,
                        versjonAvKode = it.versjonAvKode,
                    )

                is KøetUtgåendeHendelse ->
                    meldingPubliserer.publiser(
                        fødselsnummer = it.fødselsnummer,
                        hendelse = it.hendelse,
                        årsak = it.årsak,
                    )
            }
        }
    }
}
