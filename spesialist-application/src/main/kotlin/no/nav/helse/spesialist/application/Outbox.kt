package no.nav.helse.spesialist.application

import no.nav.helse.MeldingPubliserer
import no.nav.helse.mediator.KommandokjedeEndretEvent
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.melding.SubsumsjonEvent
import no.nav.helse.modell.melding.UtgåendeHendelse
import java.util.UUID

class Outbox {
    private val outbox = mutableListOf<OutboxMelding>()

    private sealed interface OutboxMelding

    private data class OutboxBehovListe(
        val hendelseId: UUID,
        val commandContextId: UUID,
        val fødselsnummer: String,
        val behov: List<Behov>,
    ) : OutboxMelding

    fun leggTil(
        hendelseId: UUID,
        commandContextId: UUID,
        fødselsnummer: String,
        behov: List<Behov>,
    ) {
        outbox.add(
            OutboxBehovListe(
                hendelseId = hendelseId,
                commandContextId = commandContextId,
                fødselsnummer = fødselsnummer,
                behov = behov,
            ),
        )
    }

    private data class OutboxKommandokjedeEndretEvent(
        val fødselsnummer: String,
        val event: KommandokjedeEndretEvent,
        val hendelseNavn: String,
    ) : OutboxMelding

    fun leggTil(
        fødselsnummer: String,
        event: KommandokjedeEndretEvent,
        hendelseNavn: String,
    ) {
        outbox.add(
            OutboxKommandokjedeEndretEvent(
                fødselsnummer = fødselsnummer,
                event = event,
                hendelseNavn = hendelseNavn,
            ),
        )
    }

    private data class OutboxSubsumsjon(
        val fødselsnummer: String,
        val subsumsjonEvent: SubsumsjonEvent,
        val versjonAvKode: String,
    ) : OutboxMelding

    fun leggTil(
        fødselsnummer: String,
        subsumsjonEvent: SubsumsjonEvent,
        versjonAvKode: String,
    ) {
        outbox.add(
            OutboxSubsumsjon(
                fødselsnummer = fødselsnummer,
                subsumsjonEvent = subsumsjonEvent,
                versjonAvKode = versjonAvKode,
            ),
        )
    }

    private data class OutboxUtgåendeHendelse(
        val fødselsnummer: String,
        val hendelse: UtgåendeHendelse,
        val årsak: String,
    ) : OutboxMelding

    fun leggTil(
        fødselsnummer: String,
        hendelse: UtgåendeHendelse,
        årsak: String,
    ) {
        outbox.add(
            OutboxUtgåendeHendelse(
                fødselsnummer = fødselsnummer,
                hendelse = hendelse,
                årsak = årsak,
            ),
        )
    }

    fun sendAlle(meldingPubliserer: MeldingPubliserer) {
        outbox.forEach {
            when (it) {
                is OutboxBehovListe ->
                    meldingPubliserer.publiser(
                        hendelseId = it.hendelseId,
                        commandContextId = it.commandContextId,
                        fødselsnummer = it.fødselsnummer,
                        behov = it.behov,
                    )

                is OutboxKommandokjedeEndretEvent ->
                    meldingPubliserer.publiser(
                        fødselsnummer = it.fødselsnummer,
                        event = it.event,
                        hendelseNavn = it.hendelseNavn,
                    )

                is OutboxSubsumsjon ->
                    meldingPubliserer.publiser(
                        fødselsnummer = it.fødselsnummer,
                        subsumsjonEvent = it.subsumsjonEvent,
                        versjonAvKode = it.versjonAvKode,
                    )

                is OutboxUtgåendeHendelse ->
                    meldingPubliserer.publiser(
                        fødselsnummer = it.fødselsnummer,
                        hendelse = it.hendelse,
                        årsak = it.årsak,
                    )
            }
        }
    }
}
